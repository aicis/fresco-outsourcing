package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.outsourcing.network.ServerSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * This producer will accept given number of expected sessions. It assumes that each client only
 * connects for a single input session and it will prioritize the sessions in the order the clients
 * connect to the server with id 1.
 *
 * Note, this producer will not attempt to authenticate the clients connecting for a session.
 * </p>
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
 */
public class DemoClientSessionProducer implements ClientSessionProducer {

  private static final Logger logger = LoggerFactory.getLogger(DemoClientSessionProducer.class);
  private final SpdzResourcePool resourcePool;
  private final FieldDefinition definition;
  private final PriorityQueue<QueuedClient> orderingQueue;
  private final BlockingQueue<QueuedClient> processingQueue;
  private final int port;
  private int clientsReady;
  private int expectedClients;
  private int sessionsProduced;

  /**
   * Constructs a new client session producer.
   *
   * <p>
   * At construction this will start a thread that starts listening a specified number for client
   * connections and perform the handshake protocol described above with the clients.
   * </p>
   *
   * @param resourcePool a spdz resource pool to use for the input protocol
   * @param port a port to listen for incomming sessions on
   * @param expectedInputClients the expected number of client sessions to produce
   */
  public DemoClientSessionProducer(SpdzResourcePool resourcePool, FieldDefinition definition,
      int port, int expectedInputClients) {
    if (port < 0) {
      throw new IllegalArgumentException("Port number cannot be negative, but was: " + port);
    }
    if (expectedInputClients < 0) {
      throw new IllegalArgumentException(
          "Expected clients cannot be negative, but was: " + expectedInputClients);
    }
    this.resourcePool = Objects.requireNonNull(resourcePool);
    this.definition = definition;
    this.port = port;
    this.expectedClients = expectedInputClients;
    this.processingQueue = new ArrayBlockingQueue<>(expectedInputClients);
    this.orderingQueue = new PriorityQueue<>(expectedInputClients,
        Comparator.comparingInt(QueuedClient::getPriority));
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
    // TODO check type, forward accordingly, network shouldn't be a problem because it's fresh for
    // every new connection
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

  @Override
  public DdnntClientInputSession nextInput() {
    try {
      QueuedClient client = processingQueue.take();
      List<DdnntInputTuple> tripList = new ArrayList<>(client.getInputAmount());
      for (int i = 0; i < client.getInputAmount(); i++) {
        SpdzTriple trip = resourcePool
            .getDataSupplier()
            .getNextTriple();
        tripList.add(new SpdzDdnntTuple(trip));
      }
      TripleDistributor distributor = new PreLoadedTripleDistributor(tripList);
      DdnntClientInputSession session = new DdnntClientInputSessionImpl(client.getClientId(),
          client.getInputAmount(), client.getNetwork(), distributor, definition);
      sessionsProduced++;
      return session;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean hasNextInput() {
    return expectedClients - sessionsProduced > 0;
  }

  @Override
  public DdnntClientOutputSession nextOutput() {
    return null;
  }

  @Override
  public boolean hasNextOutput() {
    return false;
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

}
