package dk.alexandra.fresco.outsourcing.utils;

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
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntClientSessionRequestHandler;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntInputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntOutputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoClientInputSessionEndpoint;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoClientOutputSessionEndpoint;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoClientSessionRequestHandler;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoServerSessionProducer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.ServerSessionProducer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDummyDataSupplier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class SpdzSetupUtils {

  private SpdzSetupUtils() {
  }

  public static FieldDefinition getDefaultFieldDefinition(int bitLength) {
    return new BigIntegerFieldDefinition(
        ModulusFinder.findSuitableModulus(bitLength+64));
  }

  public static BigInteger insecureSampleSsk(int partyId, BigInteger modulus) {
    return new BigInteger(modulus.bitLength(), new Random(partyId)).mod(modulus);
  }

  public static NetworkConfiguration getNetConf(int serverId,
      Map<Integer, Integer> partiesToPorts) {
    Map<Integer, String> partiesToIp = new HashMap<>();
    for (int id: partiesToPorts.keySet()) {
      partiesToIp.put(id, "localhost");
    }
    return getNetConf(serverId, partiesToPorts, partiesToIp);
  }

  public static NetworkConfiguration getNetConf(int serverId,
    Map<Integer, Integer> partiesToPorts, Map<Integer, String> partiesToIp) {
    Map<Integer, Party> partyMap = new HashMap<>();
    for (Entry<Integer, Integer> entry : partiesToPorts.entrySet()) {
      partyMap.put(entry.getKey(), new Party(entry.getKey(), partiesToIp.get(entry.getKey()), entry.getValue()));
    }
    return new NetworkConfigurationImpl(serverId, partyMap);
  }

  public static List<Party> getServerParties(int basePort, int numServers) {
    List<Party> servers = new ArrayList<>(numServers);
    for (int i = 1; i <= numServers; i++) {
      servers.add(new Party(i, "localhost", basePort + i));
    }
    return servers;
  }

  public static SpdzSetup getSetup(int serverId, Map<Integer, Integer> partiesToPorts) {
    Map<Integer, String> partiesToIp = new HashMap<>();
    for (int id: partiesToPorts.keySet()) {
      partiesToIp.put(id, "localhost");
    }
    return getSetup(serverId, partiesToPorts, partiesToIp, 64);
  }

  public static SpdzSetup getSetup(int serverId, Map<Integer, Integer> partiesToPorts, Map<Integer, String> partiesToIp, int bitLength) {
    NetworkConfiguration netConf = getNetConf(serverId, partiesToPorts, partiesToIp);
    FieldDefinition definition = getDefaultFieldDefinition(bitLength);
    SpdzDataSupplier supplier =
        new SpdzDummyDataSupplier(
            serverId,
            partiesToPorts.size(),
            definition,
            definition.getModulus()
        );
    SpdzResourcePool rp = new SpdzResourcePoolImpl(serverId, partiesToPorts.size(),
        new OpenedValueStoreImpl<>(),
        supplier, AesCtrDrbg::new);
    SpdzProtocolSuite suite = new SpdzProtocolSuite(bitLength);
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
        new SecureComputationEngineImpl<>(suite,
            new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite));
    return new SpdzSetup(netConf, rp, sce);
  }

  // TODO probably needs the real IPs
  public static Pair<InputServer, OutputServer> initIOServers(SpdzSetup spdzSetup,
      List<Integer> inputClientIds, List<Integer> outputClientIds,
      Map<Integer, Integer> internalPorts) {

    final ServerSessionProducer<SpdzResourcePool> serverSessionProducer = new DemoServerSessionProducer(
        spdzSetup.getRp(),
        getNetConf(
            spdzSetup
                .getNetConf()
                .getMyId(),
            internalPorts));

    DdnntClientSessionRequestHandler handler = new DemoClientSessionRequestHandler(
        spdzSetup.getRp(),
        spdzSetup
            .getNetConf()
            .getMe()
            .getPort(),
        inputClientIds.size() + outputClientIds.size(),
        id -> (id <= inputClientIds.size())
    );
    InputServer inputServer = null;
    OutputServer outputServer = null;

    if (!inputClientIds.isEmpty()) {
      DemoClientInputSessionEndpoint inputSessionEndpoint =
          new DemoClientInputSessionEndpoint(
              spdzSetup.getRp(),
              spdzSetup.getRp().getFieldDefinition(),
              inputClientIds.size());
      handler.setInputRegistrationHandler(inputSessionEndpoint);
      inputServer = new DdnntInputServer<>(
          inputSessionEndpoint,
          serverSessionProducer
      );
    }

    if (!outputClientIds.isEmpty()) {
      DemoClientOutputSessionEndpoint outputSessionEndpoint = new DemoClientOutputSessionEndpoint(
          spdzSetup.getRp(),
          spdzSetup.getRp().getFieldDefinition(),
          outputClientIds.size());
      handler.setOutputRegistrationHandler(outputSessionEndpoint);
      outputServer = new DdnntOutputServer<>(
          outputSessionEndpoint,
          serverSessionProducer
      );
    }

    handler.launch();
    return new Pair<>(inputServer, outputServer);
  }
}
