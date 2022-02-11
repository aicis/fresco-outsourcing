package dk.alexandra.fresco.outsourcing.setup;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.socket.Connector;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is facade for running MPC applications using the SPDZ protocol suite.
 */
public class Spdz {

  private static final Logger logger = LoggerFactory.getLogger(Spdz.class);
  private static final int DEFAULT_FRESCO_BASE_PORT = 8042;

  private final SpdzSetup spdzSetup;

  /**
   * Constructs new {@link Spdz}.
   *
   * @param partyId id of this spdz party
   * @param applicationPorts ports of all servers
   */
  public Spdz(int partyId, Map<Integer, Integer> applicationPorts) {
    this.spdzSetup = SpdzSetupUtils.getSetup(partyId, applicationPorts);
  }

  /**
   * Constructs new {@link Spdz} using default FRESCO port range from {@link
   * #DEFAULT_FRESCO_BASE_PORT} to {@link #DEFAULT_FRESCO_BASE_PORT} + {@code numParties}.
   *
   * @param partyId id of this spdz party
   * @param numParties total number of parties
   */
  public Spdz(int partyId, int numParties) {
    this.spdzSetup = SpdzSetupUtils
        .getSetup(partyId, getDefaultPortMap(DEFAULT_FRESCO_BASE_PORT, numParties));
    logger.info("Created SPDZ instance Party {}", partyId);
  }

  /**
   * Executes MPC application.
   *
   * @param app the application
   * @param <T> the return type of the application
   * @return the result
   */
  public <T> T run(Application<T, ProtocolBuilderNumeric> app) {
    SocketNetwork network = new SocketNetwork(spdzSetup.getNetConf(),
        new Connector(spdzSetup.getNetConf(), Duration.of(1, ChronoUnit.MINUTES)).getSocketMap());
    T res = spdzSetup
        .getSce()
        .runApplication(
            app,
            spdzSetup.getRp(),
            network);
    network.close();
    return res;
  }

  /**
   * Shuts down all underlying resources.
   */
  public void shutdown() {
    spdzSetup.getSce().shutdownSCE();
  }

  /**
   * Creates default contiguous port map, where party IDs go from 1 to {@code numParties}, and ports
   * go from {@code basePort} to {@code basePort} + {@code numParties}.
   */
  private static Map<Integer, Integer> getDefaultPortMap(int basePort, int numServers) {
    Map<Integer, Integer> ports = new HashMap<>();
    for (int i = 0; i < numServers; i++) {
      ports.put(i + 1, basePort + i);
    }
    return ports;
  }

}
