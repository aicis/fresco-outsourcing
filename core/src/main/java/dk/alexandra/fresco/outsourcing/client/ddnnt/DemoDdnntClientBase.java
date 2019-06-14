package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.outsourcing.network.ClientSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forms base for {@link DemoDdnntInputClient} and {@link DemoDdnntOutputClient}.
 */
public abstract class DemoDdnntClientBase {

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntClientBase.class);

  FieldDefinition definition;
  protected List<Party> servers;
  Map<Integer, TwoPartyNetwork> serverNetworks;
  int clientId;

  /**
   * Creates new {@link DemoDdnntClientBase}.
   *
   * @param clientId client ID
   * @param servers servers to connect to
   */
  DemoDdnntClientBase(int clientId, List<Party> servers) {
    if (clientId < 1) {
      throw new IllegalArgumentException("Client ID must be 1 or higher");
    }
    this.clientId = clientId;
    this.servers = servers;
  }

  /**
   * Connects to all worker server and initializes server map with all connected servers.
   */
  final void initServerNetworks(ExecutorService es, TwoPartyNetwork masterNetwork,
      byte[] handShakeMessage)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    Map<Integer, Future<TwoPartyNetwork>> futureNetworks = new HashMap<>(servers.size() - 1);
    for (Party s : servers.stream().filter(p -> p.getPartyId() != 1)
        .collect(Collectors.toList())) {
      Future<TwoPartyNetwork> futureNetwork = es.submit(connect(s, handShakeMessage));
      futureNetworks.put(s.getPartyId(), futureNetwork);
    }
    serverNetworks = new HashMap<>(servers.size());
    serverNetworks.put(1, masterNetwork);
    for (Entry<Integer, Future<TwoPartyNetwork>> f : futureNetworks.entrySet()) {
      serverNetworks.put(f.getKey(), f.getValue().get());
    }
  }

  final void initFieldDefinition(Function<BigInteger, FieldDefinition> definitionSupplier,
      TwoPartyNetwork masterNetwork) {
    byte[] modResponse = masterNetwork.receive();
    BigInteger modulus = new BigInteger(modResponse);
    this.definition = definitionSupplier.apply(modulus);
  }

  /**
   * Computes pairwise sum of left and right elements.
   */
  final List<FieldElement> sumLists(List<FieldElement> left, List<FieldElement> right) {
    if (left.size() != right.size()) {
      throw new IllegalArgumentException("Left and right should be same size");
    }
    List<FieldElement> res = new ArrayList<>(left.size());
    for (int i = 0; i < left.size(); i++) {
      FieldElement b = left.get(i).add(right.get(i));
      res.add(b);
    }
    return res;
  }

  /**
   * Returns true if a * b = c, false otherwise.
   */
  final boolean productCheck(FieldElement a, FieldElement b, FieldElement c) {
    FieldElement actualProd = a.multiply(b);
    BigInteger actualProdConverted = definition.convertToUnsigned(actualProd);
    BigInteger expected = definition.convertToUnsigned(c);
    return actualProdConverted.equals(expected);
  }

  /**
   * Connects to server with given handshake message.
   */
  final Callable<TwoPartyNetwork> connect(Party server, byte[] handShakeMessage) {
    return () -> {
      logger.info("C{}: Connecting to server {} ... ", clientId, server);
      TwoPartyNetwork network =
          ClientSideNetworkFactory.getNetwork(server.getHostname(), server.getPort());
      network.send(handShakeMessage);
      logger.info("C{}: Connected to server {}", clientId, server);
      return network;
    };
  }

}
