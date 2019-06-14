package dk.alexandra.fresco.outsourcing.demo;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import java.util.Collections;
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
public class Spdz {

  private static final int DEFAULT_FRESCO_BASE_PORT = 8042;

  private final int basePort;
  private final SpdzSetup spdzSetup;
  private final InputServer inputServer;
  private final OutputServer outputServer;

  /**
   * Construct new SPDZ server.
   *
   * @param serverId Id of this server
   * @param numServers total number of servers
   * @param basePort the base FRESCO port, if running on same machine all ports in range {@code
   * basePort} to {@code basePort} + 2 * {@code numServers} must be available
   * @param inputParties all client parties that will contribute input
   * @param outputParties all client parties that will receive outputs
   */
  public Spdz(
      int serverId,
      int numServers,
      int basePort,
      List<Integer> inputParties,
      List<Integer> outputParties) {
    // TODO check that all required ports are available
    // TODO handle non-localhost addresses
    this.basePort = basePort;
    this.spdzSetup = SpdzSetupUtils.getSetup(serverId, numServers, basePort);
    Pair<InputServer, OutputServer> io = SpdzSetupUtils
        .initIOServers(spdzSetup, inputParties, outputParties, basePort);
    this.inputServer = io.getFirst();
    this.outputServer = io.getSecond();
  }

  /**
   * Default constructor for {@link #Spdz(int, int, int, List, List)} that sets up two servers
   * and uses {@link #DEFAULT_FRESCO_BASE_PORT} base port.
   */
  public Spdz(int serverId, List<Integer> inputParties,
      List<Integer> outputParties) {
    this(serverId, 2, DEFAULT_FRESCO_BASE_PORT, inputParties,
        outputParties);
  }

  /**
   * Default constructor for {@link #Spdz(int, int, int, List, List)} that sets up two servers
   * and uses {@link #DEFAULT_FRESCO_BASE_PORT} base port and no input or output parties.
   */
  public Spdz(int serverId) {
    this(serverId, 2, DEFAULT_FRESCO_BASE_PORT, Collections.emptyList(), Collections.emptyList());
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
    final int applicationBasePort = this.basePort + 2 * getNumServers();
    return spdzSetup
        .getSce()
        .runApplication(
            app,
            spdzSetup.getRp(),
            new SocketNetwork(
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
