package dk.alexandra.fresco.outsourcing.network;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.socket.NetworkConnector;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for clients (providing in/output) to actively make connections to the mpc servers.
 */
public class ClientSideNetworkFactory {

  public enum Parties {
    SERVER(2), CLIENT(1);

    private final int id;

    Parties(int id) {
      this.id = id;
    }

    public int id() {
      return id;
    }
  }

  /**
   * Connects to a specified server and returns a network using the connection.
   *
   * @param serverHost hostname of the server
   * @param serverPort the port number used by the server
   * @return a network connected to the given server
   */
  public static TwoPartyNetwork getNetwork(String serverHost, int serverPort) {
    return getNetwork(serverHost, serverPort, Parties.CLIENT.id());
  }

  private static TwoPartyNetwork getNetwork(String serverHost, int serverPort, int id) {
    Party client = new Party(Parties.CLIENT.id(), "", 0); // Note port and host irrelevant
    Party server = new Party(Parties.SERVER.id(), serverHost, serverPort);
    Map<Integer, Party> parties = new HashMap<>(2);
    parties.put(Parties.CLIENT.id(), client);
    parties.put(Parties.SERVER.id(), server);
    NetworkConfiguration conf = new NetworkConfigurationImpl(id, parties);
    NetworkConnector connector = new OutsourcingConnector(conf, Duration.ofDays(1));
    SocketNetwork network = new SocketNetwork(conf, connector.getSocketMap());
    return new TwoPartyNetworkImpl(network, conf.getMyId());
  }

}
