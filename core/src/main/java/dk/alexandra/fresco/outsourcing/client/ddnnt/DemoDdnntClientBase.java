package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.network.ClientSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DemoDdnntClientBase {

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntClientBase.class);

  FieldDefinition definition;
  protected List<Party> servers;
  Map<Integer, TwoPartyNetwork> serverNetworks;
  int clientId;

  DemoDdnntClientBase(int numInputs, int clientId, List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    this.clientId = clientId;
    this.servers = servers;
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, numInputs);
      return null;
    }, "Failed client handshake");
  }

  private void handshake(Function<BigInteger, FieldDefinition> definitionSupplier,
      int numInputs) {
    logger.info("C{}: Starting handshake", clientId);
    try {
      ExecutorService es = Executors.newFixedThreadPool(servers.size() - 1);
      Party serverOne = servers.stream().filter(p -> p.getPartyId() == 1).findFirst().get();
      logger.info("C{}: connecting to master server {}", clientId, serverOne);
      TwoPartyNetwork masterNetwork = es.submit(connect(serverOne, 0, numInputs)).get();
      logger.info("C{}: Connected to master server", clientId);
      byte[] response = masterNetwork.receive();
      int priority = intFromBytes(response);
      logger.info("C{}: Received priority {}", clientId, priority);
      Map<Integer, Future<TwoPartyNetwork>> futureNetworks = new HashMap<>(servers.size() - 1);
      for (Party s : servers.stream().filter(p -> p.getPartyId() != 1)
          .collect(Collectors.toList())) {
        Future<TwoPartyNetwork> futureNetwork = es.submit(connect(s, priority, numInputs));
        futureNetworks.put(s.getPartyId(), futureNetwork);
      }
      serverNetworks = new HashMap<>(servers.size());
      serverNetworks.put(1, masterNetwork);
      for (Entry<Integer, Future<TwoPartyNetwork>> f : futureNetworks.entrySet()) {
        serverNetworks.put(f.getKey(), f.getValue().get());
      }
      es.shutdown();
      byte[] modResponse = masterNetwork.receive();
      BigInteger modulus = new BigInteger(modResponse);
      this.definition = definitionSupplier.apply(modulus);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Converts big-endian byte array to int.
   */
  private static int intFromBytes(byte[] bytes) {
    int res = 0;
    int topByteIndex = Byte.SIZE * (Integer.BYTES - 1);
    for (int i = 3; i >= 0; i--) {
      res ^= (bytes[i] & 0xFF) << (topByteIndex - i * Byte.SIZE);
    }
    return res;
  }

  private Callable<TwoPartyNetwork> connect(Party server, int priority, int numInputs) {
    return () -> {
      byte[] msg = new byte[Integer.BYTES * 3];
      System.arraycopy(ByteAndBitConverter.toByteArray(priority), 0, msg, 0, Integer.BYTES);
      System.arraycopy(ByteAndBitConverter.toByteArray(clientId), 0, msg, Integer.BYTES,
          Integer.BYTES);
      System.arraycopy(ByteAndBitConverter.toByteArray(numInputs), 0, msg, Integer.BYTES * 2,
          Integer.BYTES);
      logger.info("C{}: Connecting to server {} ... ", clientId, server);
      TwoPartyNetwork network =
          ClientSideNetworkFactory.getNetwork(server.getHostname(), server.getPort());
      network.send(msg);
      logger.info("C{}: Connected to server {}", clientId, server);
      return network;
    };
  }

}
