package dk.alexandra.fresco.outsourcing.utils;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
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
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.AesCtrDrbgFactory;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.ModulusFinder;
import dk.alexandra.fresco.framework.util.OpenedValueStoreImpl;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.outsourcing.client.AbstractSessionEndPoint;
import dk.alexandra.fresco.outsourcing.client.GenericClientSession;
import dk.alexandra.fresco.outsourcing.client.GenericClientSessionEndpoint;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntClientInputSessionEndpoint;
import dk.alexandra.fresco.outsourcing.server.ClientSessionRequestHandler;
import dk.alexandra.fresco.outsourcing.server.DemoClientSessionRequestHandler;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoServerSessionProducer;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpdzSetupUtils {
  private static final Logger logger = LoggerFactory.getLogger(SpdzSetupUtils.class);
  public static final int DEFAULT_BITLENGTH = 64;
  public static final int DEFAULT_STATPAR = 40;

  private SpdzSetupUtils() {
  }

  public static FieldDefinition getDefaultFieldDefinition(int bitLength) {
    return new BigIntegerFieldDefinition(
        ModulusFinder.findSuitableModulus(bitLength+DEFAULT_STATPAR));
  }

  public static FieldDefinition getDefaultFieldDefinition(BigInteger modulus) {
    if (!modulus.isProbablePrime(DEFAULT_STATPAR)) {
      // Very few operations can work in this case
      throw new IllegalArgumentException("Modulus is not prime");
    }
    if (!modulus.subtract(BigInteger.ONE).gcd(BigInteger.valueOf(3)).equals(BigInteger.ONE)) {
      // This can cause issues with some Fresco algorithms, but basic thing should still work.
      logger.error("Modulus-1 mod 3 != 1");
    }
    return new BigIntegerFieldDefinition(modulus);
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
      Map<Integer, String> partiesToIp, BigInteger modulus) {
    NetworkConfiguration netConf = getNetConf(serverId, partiesToPorts, partiesToIp);
    FieldDefinition definition = getDefaultFieldDefinition(modulus);
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
    // To ensure comparison protocols work, we have to subtract the statistical sec par to ensure
    // space enough to work
    SpdzProtocolSuite suite = new SpdzProtocolSuite(modulus.bitLength()-DEFAULT_STATPAR);
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
        new SecureComputationEngineImpl<>(suite,
            new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite));
    return new SpdzSetup(netConf, rp, sce);
  }

  public static SpdzSetup getSetup(int serverId, Map<Integer, Integer> partiesToPorts,
      Map<Integer, String> partiesToIp, int bitLength) {
    // To ensure comparison protocols work, we have to subtract the statistical sec par to ensure
    // space enough to work
    return getSetup(serverId, partiesToPorts, partiesToIp,
        ModulusFinder.findSuitableModulus(bitLength+DEFAULT_STATPAR));
  }

  // TODO currently we cannot make this generic due to the custom ddnt client input session
  //  endpoint, it could be given as optional argument though. The reason it is needed is that
  //  DDNNT is NOT black box in the underlying scheme
  public static Pair<InputServer, OutputServer> initDdnntIOServers(SpdzSetup spdzSetup,
      List<Integer> inputClientIds, List<Integer> outputClientIds,
      Map<Integer, Integer> partiesToPorts, Map<Integer, String> partiesToIp,
      InputServerProducer inputServerProducer, OutputServerProducer outputServerProducer) {

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
      inputServer = inputServerProducer.apply(
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
      outputServer = outputServerProducer.apply(
          outputSessionEndpoint,
          serverSessionProducer
      );
    }

    handler.launch();
    return new Pair<>(inputServer, outputServer);
  }

  public static Pair<InputServer, OutputServer> initIOServers(SpdzSetup spdzSetup,
      List<Integer> inputClientIds, List<Integer> outputClientIds,
      Map<Integer, Integer> partiesToPorts, Map<Integer, String> partiesToIp,
      InputServerProducer inputServerProducer, OutputServerProducer outputServerProducer) {

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
      inputServer = inputServerProducer.apply(
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
      outputServer = outputServerProducer.apply(
          outputSessionEndpoint,
          serverSessionProducer
      );
    }

    handler.launch();
    return new Pair<>(inputServer, outputServer);
  }

  @FunctionalInterface
  public interface InputServerProducer<
      T extends NumericResourcePool,
      EndPointT extends AbstractSessionEndPoint<GenericClientSession>,
      SessionT extends ServerSessionProducer<T>> {
    InputServer apply(EndPointT endpoint, SessionT sessionProducer);
  }

  @FunctionalInterface
  public interface OutputServerProducer<
      T extends NumericResourcePool,
      EndPointT extends AbstractSessionEndPoint<GenericClientSession>,
      SessionT extends ServerSessionProducer<T>> {
    OutputServer apply(EndPointT endpoint, SessionT sessionProducer);
  }

}
