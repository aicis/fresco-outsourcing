package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.outsourcing.network.ClientSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.ServerSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.net.ServerSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A demo client session producer based on SPDZ.
 *
 * <p>
 * Defines the following protocol for clients to start the sessions:
 * <ol>
 * <li>The client must first connect to server 1.
 * <li>The client sends an introduction message to server 1 consisting of three integers: a zero,
 * the clients unique id, and the number of inputs the client will make.
 * <li>Server 1 assigns a priority to the client and sends this back to the client.
 * <li>The client can now connect to the other servers, and sends an introduction message of three
 * integers: priority, unique client id, and the number of inputs the client will make.
 * </ol>
 * The priority is used by the servers to be able to coordinate which triples to use for each
 * clients input.
 * </p>
 *
 */
public class DemoClientSessionProducer implements ClientSessionProducer {

  private static final Logger logger = LoggerFactory.getLogger(DemoClientSessionProducer.class);
  private final SpdzResourcePool resourcePool;
  private final PriorityQueue<QueuedClient> orderingQueue;
  private final BlockingQueue<QueuedClient> processingQueue;
  private final int port;
  private int clientsReady;
  private int expectedClients;
  private int sessionsProduced;

  public DemoClientSessionProducer(SpdzResourcePool resourcePool, int port, int expectedClients) {
    if (port < 0) {
      throw new IllegalArgumentException("Port number cannot be negative, but was: " + port);
    }
    if (expectedClients < 0) {
      throw new IllegalArgumentException(
          "Expected clients cannot be negative, but was: " + expectedClients);
    }
    this.resourcePool = Objects.requireNonNull(resourcePool);
    this.port = port;
    this.expectedClients = expectedClients;
    this.processingQueue = new ArrayBlockingQueue<>(expectedClients);
    this.orderingQueue = new PriorityQueue<>(expectedClients,
        (QueuedClient a, QueuedClient b) -> Integer.compare(a.getPriority(), b.getPriority()));
    this.clientsReady = 0;
    Thread t = new Thread(this::listenForClients);
    t.setDaemon(true);
    t.setName("DemoClientSessionProducer Listener");
    t.start();
  }

  void listenForClients() {
    logger.info("Started Listening for " + expectedClients + " client connections.");
    ServerSideNetworkFactory networkFactory =
        new ServerSideNetworkFactory(port, ServerSocketFactory.getDefault());
    for (int i = 0; i < expectedClients; i++) {
      logger.info("S{}: Waiting for next client.", resourcePool.getMyId());
      TwoPartyNetwork network = networkFactory.getNetwork();
      logger.info("S{}: Client connected. Starting handshake ... ", resourcePool.getMyId());
      handshake(network);
    }
    networkFactory.stopListening();
  }

  void handshake(TwoPartyNetwork network) {
    // The introduction message from the client is expected to be the following 12 bytes
    // Bytes 0-3: client priority, assigned by server 1 (big endian int)
    // Bytes 4-7: unique id for client (big endian int)
    // Bytes 8-11: number of inputs
    byte[] introBytes = network.receive();
    int priority = intFromBytes(Arrays.copyOfRange(introBytes, 0, Integer.BYTES * 1));
    int clientId =
        intFromBytes(Arrays.copyOfRange(introBytes, Integer.BYTES * 1, Integer.BYTES * 2));
    int inputAmount =
        intFromBytes(Arrays.copyOfRange(introBytes, Integer.BYTES * 2, Integer.BYTES * 3));
    if (resourcePool.getMyId() == 1) {
      priority = clientsReady++;
      byte[] priorityBytes = ByteAndBitConverter.toByteArray(priority);
      network.send(priorityBytes);
      network.send(resourcePool.getModulus().toByteArray());
      QueuedClient q = new QueuedClient(priority, clientId, inputAmount, network);
      processingQueue.add(q);
      logger.info("S{}: Finished handskake for client {} with priority {}. Expecting {} inputs.",
          resourcePool.getMyId(), q.clientId, q.priority, q.inputAmount);
    } else {
      QueuedClient q = new QueuedClient(priority, clientId, inputAmount, network);
      orderingQueue.add(q);
      while (!orderingQueue.isEmpty() && orderingQueue.peek().getPriority() == clientsReady) {
        clientsReady++;
        processingQueue.add(orderingQueue.remove());
      }
      logger.info("S{}: Finished handskake for client {} with priority {}. Expecting {} inputs.",
          resourcePool.getMyId(), q.clientId, q.priority, q.inputAmount);
    }
  }

  /**
   * Converts big-endian byte array to int.
   */
  private static int intFromBytes(byte[] bytes) {
    int res = 0;
    int topByteIndex = Byte.SIZE * (Integer.BYTES - 1);
    for (int i = 3; i >= 0; i--) {
      res ^= (bytes[i] & 0xFF) << (topByteIndex - i * Byte.SIZE);
    }
    return res;
  }

  private static class QueuedClient {

    int priority;
    int clientId;
    int inputAmount;
    TwoPartyNetwork network;

    public QueuedClient(int priority, int clientId, int inputAmount, TwoPartyNetwork network) {
      this.clientId = clientId;
      this.inputAmount = inputAmount;
      this.network = network;
      this.priority = priority;
    }

    private int getClientId() {
      return clientId;
    }

    private int getInputAmount() {
      return inputAmount;
    }

    private TwoPartyNetwork getNetwork() {
      return network;
    }

    int getPriority() {
      return priority;
    }
  }


  private int[] intsFromBytes(byte[] bytes) {
    int[] ints = new int[bytes.length / Integer.BYTES];
    for (int i = 0; i < bytes.length; i++) {
      ints[i / Integer.BYTES] <<= Byte.SIZE;
      ints[i / Integer.BYTES] ^= bytes[i];
    }
    return ints;
  }

  @Override
  public DdnntClientInputSession next() {
    try {
      QueuedClient client = processingQueue.take();
      List<DdnntInputTuple> tripList = new ArrayList<>(client.getInputAmount());
      for (int i = 0; i < client.getInputAmount(); i++) {
        SpdzTriple trip = resourcePool.getDataSupplier().getNextTriple();
        tripList.add(new SpdzDdnntTuple(trip));
      }
      TripleDistributor distributor = new PreLoadedTripleDistributor(tripList);
      DdnntClientInputSession session = new DdnntClientInputSessionImpl(client.getClientId(),
          client.getInputAmount(), client.getNetwork(), distributor, resourcePool.getSerializer());
      sessionsProduced++;
      return session;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean hasNext() {
    return expectedClients - sessionsProduced > 0;
  }

}
