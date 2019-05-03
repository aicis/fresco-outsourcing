package dk.alexandra.fresco.outsourcing.demo;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.ModulusFinder;
import dk.alexandra.fresco.framework.util.OpenedValueStoreImpl;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntInputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoServerSessionProducer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.ServerSessionProducer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDummyDataSupplier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpdzSetupUtils {

  private SpdzSetupUtils() {
  }

  static NetworkConfiguration getNetConf(int serverId, int numServers, int basePort) {
    Map<Integer, Party> partyMap = new HashMap<>();
    for (int i = 1; i <= numServers; i++) {
      partyMap.put(i, new Party(i, "localhost", basePort + i));
    }
    return new NetworkConfigurationImpl(serverId, partyMap);
  }

  static List<Party> getServerParties(int basePort, int numServers) {
    List<Party> servers = new ArrayList<>(numServers);
    for (int i = 1; i <= numServers; i++) {
      servers.add(new Party(i, "localhost", basePort + i));
    }
    return servers;
  }

  static SpdzSetup getSetup(int serverId, int numServers, int basePort) {
    NetworkConfiguration netConf = getNetConf(serverId, numServers, basePort);
    SpdzDataSupplier supplier =
        new SpdzDummyDataSupplier(
            serverId,
            numServers,
            ModulusFinder.findSuitableModulus(64));
    SpdzResourcePool rp = new SpdzResourcePoolImpl(serverId, numServers,
        new OpenedValueStoreImpl<>(),
        supplier, new AesCtrDrbg(new byte[32]));
    SpdzProtocolSuite suite = new SpdzProtocolSuite(64);
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
        new SecureComputationEngineImpl<>(suite,
            new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite));
    return new SpdzSetup(netConf, rp, sce);
  }

  static InputServer initInputServer(SpdzSetup spdzSetup, List<Integer> clientIds,
      int basePort) {
    final ClientSessionProducer clientSessionProducer = new DemoClientSessionProducer(
        spdzSetup.getRp(),
        spdzSetup
            .getNetConf()
            .getMe()
            .getPort(),
        clientIds.size()
    );
    final int numServers = spdzSetup.getNetConf().noOfParties();
    final ServerSessionProducer<SpdzResourcePool> serverSessionProducer = new DemoServerSessionProducer(
        spdzSetup.getRp(),
        getNetConf(
            spdzSetup.getNetConf().getMyId(),
            numServers,
            basePort + numServers));
    return new DdnntInputServer<>(
        clientSessionProducer,
        serverSessionProducer
    );
  }
}
