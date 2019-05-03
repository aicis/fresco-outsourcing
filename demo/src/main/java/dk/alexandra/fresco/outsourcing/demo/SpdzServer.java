package dk.alexandra.fresco.outsourcing.demo;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.AsyncNetwork;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import java.util.List;
import java.util.Map;

/**
 * This is a facade for the SPDZ suite functionality that allows receiving inputs from clients and
 * running FRESCO applications using SPDZ.
 *
 * <p>Note that this class has a purposefully limited interface and chooses a lot of "best-effort"
 * parameters. These might not work for your specific application in which case you should manually
 * instantiate SPDZ with the right parameters.</p>
 */
public class SpdzServer {

  private static final int DEFAULT_FRESCO_BASE_PORT = 8042;

  private final int basePort;
  private final SpdzSetup spdzSetup;

  /**
   * Construct new SPDZ server.
   *
   * @param serverId Id of this server
   * @param numServers total number of servers
   * @param basePort the base FRESCO port, if running on same machine all ports in range {@code
   * basePort} to {@code basePort} + 2 * {@code numServers} must be available
   */
  public SpdzServer(int serverId, int numServers, int basePort) {
    // TODO check that all required ports are available
    // TODO handle non-localhost addresses
    this.basePort = basePort;
    this.spdzSetup = SpdzSetupUtils.getSetup(serverId, numServers, basePort);
  }

  /**
   * Default constructor for {@link #SpdzServer(int, int, int)} that sets up two servers and uses
   * {@link #DEFAULT_FRESCO_BASE_PORT} base port.
   */
  public SpdzServer(int serverId) {
    this(serverId, 2, DEFAULT_FRESCO_BASE_PORT);
  }

  /**
   * Receives client inputs and returns this server's shares of the input.
   *
   * @param clientIds IDs of clients expected to submit inputs.
   * @return map of client ID to this server's shares of input
   */
  public Map<Integer, List<SInt>> receiveInputsFrom(List<Integer> clientIds) {
    InputServer server = SpdzSetupUtils.initInputServer(spdzSetup, clientIds, basePort);
    return ExceptionConverter
        .safe(() -> server.getClientInputs().get(), "Input step failed");
  }

  /**
   * Executes MPC application.
   *
   * @param app the application
   * @param <T> the return type of the application
   * @return the result
   */
  public <T> T run(Application<T, ProtocolBuilderNumeric> app) {
    final int applicationBasePort = this.basePort + 2 * getNumServers();
    return spdzSetup
        .getSce()
        .runApplication(
            app,
            spdzSetup.getRp(),
            new AsyncNetwork(
                SpdzSetupUtils
                    .getNetConf(getServerId(), getNumServers(), applicationBasePort)));
  }

  private int getServerId() {
    return spdzSetup
        .getNetConf()
        .getMyId();
  }

  private int getNumServers() {
    return spdzSetup
        .getNetConf()
        .noOfParties();
  }

}
