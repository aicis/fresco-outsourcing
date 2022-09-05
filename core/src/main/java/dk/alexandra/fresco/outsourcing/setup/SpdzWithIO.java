package dk.alexandra.fresco.outsourcing.setup;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is a facade for the SPDZ suite functionality that allows receiving inputs from clients,
 * outputting results to clients, and running FRESCO applications using SPDZ.
 *
 * <p>Note that this class has a purposefully limited interface and chooses a lot of "best-effort"
 * parameters. These might not work for your specific application in which case you should manually
 * instantiate SPDZ with the right parameters.</p>
 */
public class SpdzWithIO {

  private static final int DEFAULT_FRESCO_BASE_PORT = 8042;

  private final Map<Integer, Integer> applicationPorts;
  private final SpdzSetup spdzSetup;
  private final InputServer inputServer;
  private final OutputServer outputServer;

  /**
   * Creates new {@link SpdzWithIO}.
   *
   * <p>Note that the plethora of ports is necessary because FRESCO doesn't have a channeled
   * network implementation. Once that is in place, a single port per server can be used.</p>
   *
   * @param serverId Id of this server
   * @param clientFacingPorts ports to be used by IO with clients
   * @param internalPorts ports to be used internally between servers for IO protocols
   * @param applicationPorts ports to be used for running MPC applications
   * @param inputParties IDs of parties contributing inputs (empty means no inputs)
   * @param outputParties IDs of parties receiving outputs (empty means no outputs)
   */
  public SpdzWithIO(
      int serverId,
      Map<Integer, Integer> clientFacingPorts,
      Map<Integer, Integer> internalPorts,
      Map<Integer, Integer> applicationPorts,
      List<Integer> inputParties,
      List<Integer> outputParties) {
    // TODO check that all required ports are available
    // TODO handle non-localhost addresses
    this.applicationPorts = applicationPorts;
    this.spdzSetup = SpdzSetupUtils.getSetup(serverId, clientFacingPorts);
    Pair<InputServer, OutputServer> io = SpdzSetupUtils
        .initIOServers(spdzSetup, inputParties, outputParties, internalPorts);
    this.inputServer = io.getFirst();
    this.outputServer = io.getSecond();
  }

  /**
   * Construct new SPDZ server.
   *
   * @param serverId Id of this server
   * @param numServers total number of servers
   * @param basePort the base FRESCO port, if running on same machine all ports in range {@code
   * basePort} to {@code basePort} + 3 * {@code numServers} must be available
   * @param inputParties all client parties that will contribute input
   * @param outputParties all client parties that will receive outputs
   */
  public SpdzWithIO(
      int serverId,
      int numServers,
      int basePort,
      List<Integer> inputParties,
      List<Integer> outputParties) {
    this(serverId,
        SpdzSetup.getClientFacingPorts(contiguousPorts(basePort, numServers), numServers),
        SpdzSetup.getInternalPorts(contiguousPorts(basePort, numServers), numServers),
        SpdzSetup.getApplicationPorts(contiguousPorts(basePort, numServers), numServers),
        inputParties,
        outputParties
    );
  }

  /**
   * Default constructor that will use {@link #DEFAULT_FRESCO_BASE_PORT} as base port and 2
   * servers.
   */
  public SpdzWithIO(int serverId, List<Integer> inputParties, List<Integer> outputParties) {
    this(serverId, 2, DEFAULT_FRESCO_BASE_PORT, inputParties, outputParties);
  }

  /**
   * Receives client inputs and returns this server's shares of the input.
   *
   * @return map of client ID to this server's shares of input
   */
  public Map<Integer, List<SInt>> receiveInputs() {
    return ExceptionConverter
        .safe(() -> inputServer.getClientInputs().get(), "Input step failed");
  }

  /**
   * Sends shares of secrets to specified output party.
   */
  public void sendOutputsTo(int receiverId, List<SInt> outputs) {
    outputServer.putClientOutputs(receiverId, outputs);
  }

  /**
   * Executes MPC application.
   *
   * @param app the application
   * @param <T> the return type of the application
   * @return the result
   */
  public <T> T run(Application<T, ProtocolBuilderNumeric> app) {
    SocketNetwork network = new SocketNetwork(
        SpdzSetupUtils
            .getNetConf(getServerId(), applicationPorts));
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
    spdzSetup.getSce().close();
  }

  private int getServerId() {
    return spdzSetup
        .getNetConf()
        .getMyId();
  }

  private static List<Integer> contiguousPorts(int basePort, int numServers) {
    return IntStream.range(basePort + 1, basePort + 1 + 3 * numServers).boxed()
        .collect(Collectors.toList());
  }

}
