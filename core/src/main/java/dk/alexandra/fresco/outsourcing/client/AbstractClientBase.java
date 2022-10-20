package dk.alexandra.fresco.outsourcing.client;

import static dk.alexandra.fresco.outsourcing.utils.GenericUtils.intFromBytes;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
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

public abstract class AbstractClientBase {
  private static final Logger logger = LoggerFactory.getLogger(AbstractClientBase.class);

  private FieldDefinition definition;
  private List<Party> servers;
  private Map<Integer, TwoPartyNetwork> serverNetworks;
  private int clientId;

  /**
   * Creates new {@link AbstractClientBase}.
   *
   * @param clientId client ID
   * @param servers  servers to connect to
   */
  protected AbstractClientBase(int clientId, List<Party> servers) {
    if (clientId < 1) {
      throw new IllegalArgumentException("Client ID must be 1 or higher");
    }
    this.clientId = clientId;
    this.servers = servers;
  }

  /**
   * Connects to all worker server and initializes server map with all connected servers.
   */
  protected final void initServerNetworks(ExecutorService es, TwoPartyNetwork masterNetwork,
      byte[] handShakeMessage)
      throws InterruptedException, java.util.concurrent.ExecutionException {
    Map<Integer, Future<TwoPartyNetwork>> futureNetworks = new HashMap<>(getServers().size() - 1);
    for (Party s : getServers().stream().filter(p -> p.getPartyId() != 1)
        .collect(Collectors.toList())) {
      Future<TwoPartyNetwork> futureNetwork = es.submit(connect(s, handShakeMessage));
      futureNetworks.put(s.getPartyId(), futureNetwork);
    }
    serverNetworks = new HashMap<>(getServers().size());
    getServerNetworks().put(1, masterNetwork);
    for (Entry<Integer, Future<TwoPartyNetwork>> f : futureNetworks.entrySet()) {
      getServerNetworks().put(f.getKey(), f.getValue().get());
    }
  }

  protected final void initFieldDefinition(Function<BigInteger, FieldDefinition> definitionSupplier,
      TwoPartyNetwork masterNetwork) {
    byte[] modResponse = masterNetwork.receive();
    BigInteger modulus = new BigInteger(modResponse);
    this.definition = definitionSupplier.apply(modulus);
  }

  /**
   * Connects to server with given handshake message.
   */
  protected final Callable<TwoPartyNetwork> connect(Party server, byte[] handShakeMessage) {
    return () -> {
      logger.info("C{}: Connecting to server {} ... ", getClientId(), server);
      TwoPartyNetwork network =
          ClientSideNetworkFactory.getNetwork(server.getHostname(), server.getPort());
      network.send(handShakeMessage);
      logger.info("C{}: Connected to server {}", getClientId(), server);
      return network;
    };
  }

  protected void handshake(Function<BigInteger, FieldDefinition> definitionSupplier,
      int amount) {
    logger.info("C{}: Starting handshake", getClientId());
    try {
      ExecutorService es = Executors.newFixedThreadPool(getServers().size() - 1);

      Party serverOne = getServers().stream().filter(p -> p.getPartyId() == 1).findFirst().get();
      logger.info("C{}: connecting to master server {}", getClientId(), serverOne);
      TwoPartyNetwork masterNetwork = es
          .submit(connect(serverOne, getHandShakeMessage(0, amount))).get();
      logger.info("C{}: Connected to master server", getClientId());
      byte[] response = masterNetwork.receive();

      int priority = intFromBytes(response);
      logger.info("C{}: Received priority {}", getClientId(), priority);

      initServerNetworks(es, masterNetwork, getHandShakeMessage(priority, amount));

      es.shutdown();

      initFieldDefinition(definitionSupplier, masterNetwork);
    } catch (Exception e) {
      logger.error("Error during handshake", e);
      e.printStackTrace();
    }
  }

  protected byte[] getHandShakeMessage(int priority, int amount) {
    byte[] msg = new byte[Integer.BYTES * 3];
    System.arraycopy(ByteAndBitConverter.toByteArray(priority), 0, msg, 0, Integer.BYTES);
    System.arraycopy(ByteAndBitConverter.toByteArray(getClientId()), 0, msg, Integer.BYTES,
        Integer.BYTES);
    System.arraycopy(ByteAndBitConverter.toByteArray(amount), 0, msg, Integer.BYTES * 2,
        Integer.BYTES);
    return msg;
  }

  public FieldDefinition getDefinition() {
    return definition;
  }

  public List<Party> getServers() {
    return servers;
  }

  public Map<Integer, TwoPartyNetwork> getServerNetworks() {
    return serverNetworks;
  }

  public int getClientId() {
    return clientId;
  }
}
