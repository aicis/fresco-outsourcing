package dk.alexandra.fresco.outsourcing.utils;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkConfigurationImpl;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.framework.util.*;
import dk.alexandra.fresco.outsourcing.client.GenericClientSession;
import dk.alexandra.fresco.outsourcing.client.GenericClientSessionEndpoint;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntClientInputSessionEndpoint;
import dk.alexandra.fresco.outsourcing.server.*;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntInputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntOutputServer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoServerSessionProducer;
import dk.alexandra.fresco.outsourcing.server.jno.JnoInputServer;
import dk.alexandra.fresco.outsourcing.server.jno.JnoOutputServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePoolImpl;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzDummyDataSupplier;
import dk.alexandra.fresco.suite.spdz.storage.SpdzMascotDataSupplier;
import dk.alexandra.fresco.tools.ot.base.DummyOt;
import dk.alexandra.fresco.tools.ot.base.Ot;
import dk.alexandra.fresco.tools.ot.otextension.RotList;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;

public class SpdzSetupUtils {
  public static final int DEFAULT_BITLENGTH = 64;

  private SpdzSetupUtils() {
  }

  public static FieldDefinition getDefaultFieldDefinition(int bitLength) {
    return new BigIntegerFieldDefinition(
        ModulusFinder.findSuitableModulus(bitLength+40));
  }

  public static BigInteger insecureSampleSsk(int partyId, BigInteger modulus) {
    return new BigInteger(modulus.bitLength(), new Random(partyId)).mod(modulus);
  }

  public static Map<Integer, String> getLocalhostMap(Map<Integer, Integer> partiesToPorts) {
    Map<Integer, String> partiesToIp = new HashMap<>();
    for (int id: partiesToPorts.keySet()) {
      partiesToIp.put(id, "localhost");
    }
    return partiesToIp;
  }

  public static NetworkConfiguration getNetConf(int serverId,
      Map<Integer, Integer> partiesToPorts) {
    return getNetConf(serverId, partiesToPorts, getLocalhostMap(partiesToPorts));
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
    return getSetup(serverId, partiesToPorts, getLocalhostMap(partiesToPorts), DEFAULT_BITLENGTH);
  }

  static Map<Integer, RotList> getSeedOts(int myId, int parties, int prgSeedLength, Drbg drbg,
      Network network) {
    Map<Integer, RotList> seedOts = new HashMap<>();
    for (int otherId = 1; otherId <= parties; otherId++) {
      if (myId != otherId) {
        Ot ot = new DummyOt(otherId, network);
        RotList currentSeedOts = new RotList(drbg, prgSeedLength);
        if (myId < otherId) {
          currentSeedOts.send(ot);
          currentSeedOts.receive(ot);
        } else {
          currentSeedOts.receive(ot);
          currentSeedOts.send(ot);
        }
        seedOts.put(otherId, currentSeedOts);
      }
    }
    return seedOts;
  }

  static Drbg getDrbg(int myId, int prgSeedLength) {
    byte[] seed = new byte[prgSeedLength / 8];
    seed[0] = (byte) myId;
    return AesCtrDrbgFactory.fromDerivedSeed(seed);
  }

  public static SpdzSetup getMascotSetup(int serverId, Map<Integer, Integer> partiesToPorts,
      Map<Integer, String> partiesToIp, int bitLength) {
    NetworkConfiguration netConf = getNetConf(serverId, partiesToPorts, partiesToIp);
    FieldDefinition definition = getDefaultFieldDefinition(bitLength);
    FieldElement ssk = SpdzMascotDataSupplier.createRandomSsk(definition, 32);
    Drbg drbg = getDrbg(serverId, 32);
    Network net = new SocketNetwork(netConf);
    Map<Integer, RotList> seedOts =
        getSeedOts(serverId, partiesToIp.size(), 256, drbg, net);
    SpdzDataSupplier supplier = new SpdzMascotDataSupplier(serverId, partiesToIp.size(), 1,
        () -> net, definition, bitLength, null, 256, 1024, ssk, seedOts, drbg);

    SpdzResourcePool rp = new SpdzResourcePoolImpl(serverId, partiesToPorts.size(),
        new OpenedValueStoreImpl<>(),
        supplier, AesCtrDrbg::new);
    SpdzProtocolSuite suite = new SpdzProtocolSuite(bitLength);
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
        new SecureComputationEngineImpl<>(suite,
            new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite));
    return new SpdzSetup(netConf, rp, sce);
  }

  public static SpdzSetup getSetup(int serverId, Map<Integer, Integer> partiesToPorts,
      Map<Integer, String> partiesToIp, int bitLength) {
    NetworkConfiguration netConf = getNetConf(serverId, partiesToPorts, partiesToIp);
    FieldDefinition definition = getDefaultFieldDefinition(bitLength);
//    BigInteger ssk = SpdzSetupUtils.insecureSampleSsk(serverId, definition.getModulus());
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
  public static Pair<InputServer, OutputServer> initDdnntIOServers(SpdzSetup spdzSetup,
      List<Integer> inputClientIds, List<Integer> outputClientIds,
      Map<Integer, Integer> partiesToPorts) {
    return initDdnntIOServers(spdzSetup, inputClientIds, outputClientIds, partiesToPorts, getLocalhostMap(partiesToPorts));
  }

  public static Pair<InputServer, OutputServer> initDdnntIOServers(SpdzSetup spdzSetup,
      List<Integer> inputClientIds, List<Integer> outputClientIds,
      Map<Integer, Integer> partiesToPorts, Map<Integer, String> partiesToIp) {

    final ServerSessionProducer<SpdzResourcePool> serverSessionProducer = new DemoServerSessionProducer(
        spdzSetup.getRp(),
        getNetConf(
            spdzSetup
                .getNetConf()
                .getMyId(),
            partiesToPorts, partiesToIp));

    ClientSessionRequestHandler handler = new DemoClientSessionRequestHandler(
        spdzSetup.getRp(),
        spdzSetup
            .getNetConf()
            .getMe()
            .getPort(),
        inputClientIds.size() + outputClientIds.size(),
        id -> (inputClientIds.contains(id))
    );
    InputServer inputServer = null;
    OutputServer outputServer = null;

    if (!inputClientIds.isEmpty()) {
      DdnntClientInputSessionEndpoint inputSessionEndpoint =
          new DdnntClientInputSessionEndpoint(
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
      ClientSessionHandler<GenericClientSession> outputSessionEndpoint = new GenericClientSessionEndpoint(
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

  public static Pair<InputServer, OutputServer> initJnoIOServers(SpdzSetup spdzSetup,
      List<Integer> inputClientIds, List<Integer> outputClientIds,
      Map<Integer, Integer> partiesToPorts, Map<Integer, String> partiesToIp) {

    final ServerSessionProducer<SpdzResourcePool> serverSessionProducer = new DemoServerSessionProducer(
        spdzSetup.getRp(),
        getNetConf(
            spdzSetup
                .getNetConf()
                .getMyId(),
            partiesToPorts, partiesToIp));

    ClientSessionRequestHandler handler = new DemoClientSessionRequestHandler(
        spdzSetup.getRp(),
        spdzSetup
            .getNetConf()
            .getMe()
            .getPort(),
        inputClientIds.size() + outputClientIds.size(),
        id -> (inputClientIds.contains(id))
    );
    InputServer inputServer = null;
    OutputServer outputServer = null;

    if (!inputClientIds.isEmpty()) {
      GenericClientSessionEndpoint inputSessionEndpoint =
              new GenericClientSessionEndpoint(
                      spdzSetup.getRp(),
                      spdzSetup.getRp().getFieldDefinition(),
                      inputClientIds.size());
      handler.setInputRegistrationHandler(inputSessionEndpoint);
      inputServer = new JnoInputServer<>(
              inputSessionEndpoint,
              serverSessionProducer
      );
    }

    if (!outputClientIds.isEmpty()) {
      GenericClientSessionEndpoint outputSessionEndpoint = new GenericClientSessionEndpoint(
              spdzSetup.getRp(),
              spdzSetup.getRp().getFieldDefinition(),
              outputClientIds.size());
      handler.setOutputRegistrationHandler(outputSessionEndpoint);
      outputServer = new JnoOutputServer<>(outputSessionEndpoint, serverSessionProducer);
    }

    handler.launch();
    return new Pair<>(inputServer, outputServer);
  }
}
