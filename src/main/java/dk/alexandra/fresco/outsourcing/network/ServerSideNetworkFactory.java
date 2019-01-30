package dk.alexandra.fresco.outsourcing.network;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.network.ClientSideNetworkFactory.Parties;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.net.ServerSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerSideNetworkFactory {

  private static final int PARTY_ID_BYTES = 1;
  private static final Logger logger = LoggerFactory.getLogger(ServerSideNetworkFactory.class);

  private final int port;
  private final ServerSocketFactory serverFactory;
  private final BlockingQueue<TwoPartyNetwork> processingQueue;
  private final Thread thread;

  public ServerSideNetworkFactory(int port, ServerSocketFactory serverFactory) {
    this.port = port;
    this.serverFactory = serverFactory;
    this.processingQueue = new ArrayBlockingQueue<>(100);
    this.thread = new Thread(this::connectServer);
    this.thread.setDaemon(true);
    this.thread.start();
  }

  public TwoPartyNetwork getNetwork() {
    return ExceptionConverter.safe(() -> this.processingQueue.take(),
        "Interrupted waiting for the next client connection.");
  }

  public void stopListening() {
    if (!this.thread.isInterrupted()) {
      this.thread.interrupt();
    }
  }

  /**
   * Listens for connections from the opposing parties with lower id's.
   *
   * @throws IOException thrown if an {@link IOException} occurs while listening.
   * @throws InterruptedException thrown if exception occurs adding a new network to the processing
   *         queue.
   */
  private void connectServer() {
    try (ServerSocket serverSocket = serverFactory.createServerSocket(port)) {
      while (true) {
        logger.info("Listening for client connections on port {}", port);
        Socket sock = serverSocket.accept();
        int id = 0;
        for (int j = 0; j < PARTY_ID_BYTES; j++) {
          id ^= sock.getInputStream().read() << j * Byte.SIZE;
        }
        if (id != Parties.CLIENT.id()) {
          throw new RuntimeException(
              "Expexted connection from id " + Parties.CLIENT.id() + " but was " + id);
        }
        Party client = new Party(Parties.CLIENT.id(), "", 0); // Note port and host irrelevant
        Map<Integer, Socket> socketMap = new HashMap<>(1);
        socketMap.put(Parties.CLIENT.id(), sock);
        Party server = new Party(Parties.SERVER.id(), "localhost", port);
        Map<Integer, Party> parties = new HashMap<>(2);
        parties.put(Parties.CLIENT.id(), client);
        parties.put(Parties.SERVER.id(), server);
        NetworkConfiguration conf = new NetworkConfigurationImpl(Parties.SERVER.id(), parties);
        SocketNetwork network = new SocketNetwork(conf, socketMap);
        TwoPartyNetwork networkWrapper = new TwoPartyNetworkImpl(network, conf.getMyId());
        processingQueue.put(networkWrapper);
        logger.info("Accepted connection from client");
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
