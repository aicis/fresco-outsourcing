package dk.alexandra.fresco.outsourcing.server;

import static dk.alexandra.fresco.outsourcing.utils.GenericUtils.intFromBytes;

import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.outsourcing.network.ServerSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import javax.net.ServerSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO update A demo client session producer based on SPDZ.
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
public class DemoClientSessionRequestHandler<T extends ClientSession, K extends ClientSession> implements ClientSessionRequestHandler<T, K> {

  private static final Logger logger = LoggerFactory
      .getLogger(DemoClientSessionRequestHandler.class);
  private final SpdzResourcePool resourcePool;
  private final int port;
  private final int expectedClients;
  private final Predicate<Integer> isInputClient;
  private ClientSessionRegistration<T> inputSessionRequestHandler;
  private ClientSessionRegistration<K> outputSessionRequestHandler;

  /**
   * Constructs a new client session producer.
   *
   * <p>
   * At construction this will start a thread that starts listening a specified number for client
   * connections and perform the handshake protocol described above with the clients.
   * </p>
   *
   * @param resourcePool a spdz resource pool to use for the input protocol
   * @param port a port to listen for incoming sessions on
   * TODO
   */
  public DemoClientSessionRequestHandler(SpdzResourcePool resourcePool,
      int port, int expectedClients, Predicate<Integer> isInputClient) {
    if (port < 0) {
      throw new IllegalArgumentException("Port number cannot be negative, but was: " + port);
    }
    this.resourcePool = Objects.requireNonNull(resourcePool);
    this.port = port;
    this.expectedClients = expectedClients;
    this.isInputClient = isInputClient;
  }

  private void listenForClients() {
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

  private void handshake(TwoPartyNetwork network) {
    // The introduction message from the client is expected to be the following 12 bytes
    // Bytes 0-3: client priority, assigned by server 1 (big endian int)
    // Bytes 4-7: unique id for client (big endian int)
    byte[] introBytes = network.receive();
    int clientId = getClientId(introBytes);

    int assignedPriority;
    if (this.isInputClient.test(clientId)) {
      // forward request to input session request handler
      assignedPriority = inputSessionRequestHandler.registerNewSessionRequest(
          introBytes.clone(),
          network);
    } else {
      // forward request to output session request handler
      assignedPriority = outputSessionRequestHandler.registerNewSessionRequest(
          introBytes.clone(),
          network);
    }

    // send updated priority to client if this is main server
    if (resourcePool.getMyId() == 1) {
      byte[] priorityBytes = ByteAndBitConverter.toByteArray(assignedPriority);
      network.send(priorityBytes);
      network.send(resourcePool.getModulus().toByteArray());
    }
    logger.info("S{}: Finished handskake for client {} with priority {}.",
        resourcePool.getMyId(), clientId, assignedPriority);
  }

  /**
   * Gets client ID from handshake.
   */
  private int getClientId(byte[] introBytes) {
    return intFromBytes(Arrays.copyOfRange(introBytes, Integer.BYTES, Integer.BYTES * 2));
  }

  @Override
  public void setInputRegistrationHandler(ClientSessionRegistration<T> handler) {
    if (this.inputSessionRequestHandler != null) {
      throw new IllegalStateException("Input handler already set");
    }
    this.inputSessionRequestHandler = handler;
  }

  @Override
  public void setOutputRegistrationHandler(ClientSessionRegistration<K> handler) {
    if (this.outputSessionRequestHandler != null) {
      throw new IllegalStateException("Output handler already set");
    }
    this.outputSessionRequestHandler = handler;
  }

  @Override
  public void launch() {
    Thread t = new Thread(this::listenForClients);
    t.setDaemon(true);
    t.setName("DemoClientSessionRequestHandler Listener");
    t.start();
  }

  public static class QueuedClient {

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

    public int getClientId() {
      return clientId;
    }

    public int getInputAmount() {
      return inputAmount;
    }

    public TwoPartyNetwork getNetwork() {
      return network;
    }

    public int getPriority() {
      return priority;
    }
  }

}
