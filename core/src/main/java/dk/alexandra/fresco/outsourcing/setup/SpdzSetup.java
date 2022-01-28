package dk.alexandra.fresco.outsourcing.setup;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.ModulusFinder;
import dk.alexandra.fresco.framework.util.OpenedValueStoreImpl;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDummyDataSupplier;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link SuiteSetup} using the SPDZ protocol suite to run MPC computations. This will use dummy
 * preprocessing.
 */
public class SpdzSetup implements SuiteSetup<SpdzResourcePool, ProtocolBuilderNumeric> {

  private final NetworkConfiguration netConf;
  private final SpdzResourcePool resourcePool;
  private final SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce;

  /**
   * Constructs a SpdzTestSetup given the required resources.
   *
   * @param netConf a network configuration
   * @param resourcePool a SpdzResourcePool
   * @param sce an sce
   */
  public SpdzSetup(NetworkConfiguration netConf, SpdzResourcePool resourcePool,
      SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce) {
    this.netConf = netConf;
    this.resourcePool = resourcePool;
    this.sce = sce;
  }

  /**
   * Returns a new {@link Builder} used to build tests setups for a given number of parties.
   *
   * @param parties the number of parties.
   * @return a new Builder
   */
  public static Builder builder(int parties) {
    return new Builder(parties);
  }

  @Override
  public NetworkConfiguration getNetConf() {
    return netConf;
  }

  @Override
  public SpdzResourcePool getRp() {
    return resourcePool;
  }

  @Override
  public SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> getSce() {
    return sce;
  }

  /**
   * Builder class used to configure and build test setups for a set of parties.
   */
  public static class Builder {

    private static final int DEFAULT_MOD_BIT_LENGTH = 128;
    private static final int DEFAULT_MAX_BIT_LENGTH = 64;

    private int maxLength = DEFAULT_MAX_BIT_LENGTH;
    private int modLength = DEFAULT_MOD_BIT_LENGTH;
    private int parties;

    Builder(int parties) {
      this.parties = parties;
    }

    public Builder modLength(int modLength) {
      this.modLength = modLength;
      return this;
    }

    public Builder maxLength(int maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    /**
     * Builds test setups for a number of parties using the specified parameters or default values
     * if none are given.
     *
     * @return a Map from party id to test setup
     */
    public Map<Integer, SpdzSetup> build() {
      Map<Integer, NetworkConfiguration> netConfMap = getNetConfs(parties);
      Map<Integer, SpdzSetup> setups = new HashMap<>(parties);
      FieldDefinition definition = new BigIntegerFieldDefinition(
          ModulusFinder.findSuitableModulus(modLength));
      for (int i = 1; i < parties + 1; i++) {
        BigInteger ssk = SpdzSetupUtils.insecureSampleSsk(i, definition.getModulus());
        SpdzDataSupplier supplier =
            new SpdzDummyDataSupplier(i, parties, definition, ssk);
        SpdzResourcePool rp = new SpdzResourcePoolImpl(i, parties, new OpenedValueStoreImpl<>(),
            supplier, AesCtrDrbg::new);
        SpdzProtocolSuite suite = new SpdzProtocolSuite(maxLength);
        SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
            new SecureComputationEngineImpl<>(suite,
                new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite));
        setups.put(i, new SpdzSetup(netConfMap.get(i), rp, sce));
      }
      return setups;
    }

    private Map<Integer, NetworkConfiguration> getNetConfs(int numParties) {
      Map<Integer, Party> parties = new HashMap<>(numParties);
      Map<Integer, NetworkConfiguration> confs = new HashMap<>(numParties);
      List<Integer> ports = getFreePorts(numParties);
      int id = 1;
      for (Integer port : ports) {
        parties.put(id, new Party(id, "localhost", port));
        id++;
      }
      for (int i = 1; i <= numParties; i++) {
        confs.put(i, new NetworkConfigurationImpl(i, parties));
      }
      return confs;
    }

  }

  /**
   * Finds {@code portsRequired} free ports and returns their port numbers.
   * <p>
   * NOTE: two subsequent calls to this method can return overlapping sets of free ports (same with
   * parallel calls).
   * </p>
   *
   * @param portsRequired number of free ports required
   * @return list of port numbers of free ports
   */
  public static List<Integer> getFreePorts(int portsRequired) {
    List<ServerSocket> sockets = new ArrayList<>(portsRequired);
    for (int i = 0; i < portsRequired; i++) {
      try {
        ServerSocket s = new ServerSocket(0);
        sockets.add(s);
        // we keep the socket open to ensure that the port is not re-used in a sub-sequent
        // iteration
      } catch (IOException e) {
        throw new RuntimeException("No free ports", e);
      }
    }
    return sockets.stream().map(socket -> {
      int portNumber = socket.getLocalPort();
      try {
        socket.close();
      } catch (IOException e) {
        throw new RuntimeException("No free ports", e);
      }
      return portNumber;
    }).collect(Collectors.toList());
  }

  /**
   * Returns map of party IDs to ports used for client-facing IO.
   *
   * <p>By convention these will be ports at 0 to numServers in {@code freePorts}.</p>
   */
  public static Map<Integer, Integer> getClientFacingPorts(List<Integer> freePorts,
      int numServers) {
    return getPortMap(freePorts, numServers, 0);
  }

  /**
   * Returns map of party IDs to ports used for running internal server functionality.
   *
   * <p>By convention these will be ports at numServers to 2 * numServers in {@code freePorts}.</p>
   */
  public static Map<Integer, Integer> getInternalPorts(List<Integer> freePorts, int numServers) {
    return getPortMap(freePorts, numServers, numServers);
  }

  /**
   * Returns map of party IDs to ports used for running MPC applications.
   *
   * <p>By convention these will be ports at 2 * numServers to 3 * numServers in {@code
   * freePorts}.</p>
   */
  public static Map<Integer, Integer> getApplicationPorts(List<Integer> freePorts, int numServers) {
    return getPortMap(freePorts, numServers, 2 * numServers);
  }

  private static Map<Integer, Integer> getPortMap(List<Integer> freePorts, int numServers,
      int offset) {
    if (!(freePorts.size() / numServers == 3 && freePorts.size() % numServers == 0)) {
      throw new IllegalArgumentException("Number of ports must be exactly 3 * number of servers.");
    }
    Map<Integer, Integer> ports = new HashMap<>(numServers);
    for (int i = 1; i <= numServers; i++) {
      ports.put(i, freePorts.get(i - 1 + offset));
    }
    return ports;
  }
}
