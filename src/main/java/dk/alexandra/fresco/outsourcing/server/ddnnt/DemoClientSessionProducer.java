package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.socket.Connector;
import dk.alexandra.fresco.framework.network.socket.NetworkConnector;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetworkImpl;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetworkImpl.Parties;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A demo client session producer based on SPDZ.
 *
 * <p>
 *
 * </p>
 *
 */
public class DemoClientSessionProducer implements ClientSessionProducer {

  private SpdzResourcePool resourcePool;
  private PriorityQueue<QueuedClient> orderingQueue;
  private BlockingQueue<QueuedClient> processingQueue;
  private int clientsReady;
  private String host;
  private int port;
  private int expectedClients;

  public DemoClientSessionProducer(SpdzResourcePool resourcePool, String host, int port,
      int expectedClients) {
    this.resourcePool = resourcePool;
    this.host = host;
    this.port = port;
    this.expectedClients = expectedClients;
    this.processingQueue = new ArrayBlockingQueue<>(expectedClients);
    this.orderingQueue = new PriorityQueue<>(expectedClients);
    this.clientsReady = 0;
    Thread t = new Thread(this::listenForClients);
    t.setDaemon(true);
    t.setName("DemoClientSessionProducer Listener");
    t.start();
  }

  void listenForClients() {
    for (int i = 0; i < expectedClients; i++) {
      Party client = new Party(Parties.CLIENT.id(), null, -1); // Note port and host irrelevant
      Party server = new Party(Parties.SERVER.id(), host, port);
      Map<Integer, Party> parties = new HashMap<>(2);
      parties.put(Parties.CLIENT.id(), client);
      parties.put(Parties.SERVER.id(), server);
      NetworkConfiguration conf = new NetworkConfigurationImpl(resourcePool.getMyId(), parties);
      NetworkConnector connector = new Connector(conf, Duration.ofDays(1));
      SocketNetwork network = new SocketNetwork(conf, connector.getSocketMap());
      TwoPartyNetwork networkWrapper = new TwoPartyNetworkImpl(network, resourcePool.getMyId());
      handshake(networkWrapper);
    }
  }

  void handshake(TwoPartyNetwork network) {
    // The introduction message from the client is expected to be the following 12 bytes
    // Bytes 0-3: client priority, assigned by server 1 (big endian int)
    // Bytes 4-7: unique id for client (big endian int)
    // Bytes 8-11: number of inputs
    int[] introduction = intsFromBytes(network.receive());
    if (resourcePool.getMyId() == 1) {
      int priority = clientsReady++;
      byte[] priorityBytes = new byte[Integer.BYTES];
      priorityBytes[0] = (byte) ((priority >> (Integer.SIZE - Byte.SIZE * 1)) & 0xFF);
      priorityBytes[1] = (byte) ((priority >> (Integer.SIZE - Byte.SIZE * 2)) & 0xFF);
      priorityBytes[2] = (byte) ((priority >> (Integer.SIZE - Byte.SIZE * 3)) & 0xFF);
      priorityBytes[3] = (byte) ((priority >> (Integer.SIZE - Byte.SIZE * 4)) & 0xFF);
      network.send(priorityBytes);
      QueuedClient q = new QueuedClient(priority, introduction[1], introduction[2], network);
      processingQueue.add(q);
    } else {
      QueuedClient q = new QueuedClient(introduction[0], introduction[1], introduction[2], network);
      orderingQueue.add(q);
      while (!orderingQueue.isEmpty() && orderingQueue.peek().getPriority() == clientsReady) {
        clientsReady++;
        processingQueue.add(orderingQueue.element());
      }
    }
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

    private int getPriority() {
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
  public ClientInputSession next() {
    try {
      QueuedClient client = processingQueue.take();
      List<Pair<SInt, Triple<BigInteger>>> tripList = new ArrayList<>(client.getInputAmount());
      for (int i = 0; i < client.getInputAmount(); i++) {
        SpdzTriple trip = resourcePool.getDataSupplier().getNextTriple();
        Triple<BigInteger> shareTrip = new Triple<>(trip.getA().getShare(),
            trip.getB().getShare(), trip.getC().getShare());
        tripList.add(new Pair<>(trip.getA(), shareTrip));
      }
      TripleDistributor distributor = new PreLoadedTripleDistributor(tripList);
      ClientInputSession session = new DdnntClientInputSession(client.getClientId(),
          client.getInputAmount(), client.getNetwork(), distributor, resourcePool.getSerializer());
      expectedClients--;
      return session;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean hasNext() {
    return expectedClients > 0;
  }

}
