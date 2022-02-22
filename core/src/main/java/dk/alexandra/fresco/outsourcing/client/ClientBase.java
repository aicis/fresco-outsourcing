package dk.alexandra.fresco.outsourcing.client;

import static dk.alexandra.fresco.outsourcing.utils.ByteConversionUtils.intFromBytes;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClientBase {
  private static final Logger logger = LoggerFactory.getLogger(ClientBase.class);

  protected FieldDefinition definition;
  protected List<Party> servers;
  protected Map<Integer, TwoPartyNetwork> serverNetworks;
  protected   int clientId;

  /**
   * Creates new {@link ClientBase}.
   *
   * @param clientId client ID
   * @param servers servers to connect to
   */
  protected ClientBase(int clientId, List<Party> servers) {
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
      logger.info("C{}: Connecting to server {} ... ", clientId, server);
      TwoPartyNetwork network =
          ClientSideNetworkFactory.getNetwork(server.getHostname(), server.getPort());
      network.send(handShakeMessage);
      logger.info("C{}: Connected to server {}", clientId, server);
      return network;
    };
  }

  protected void handshake(Function<BigInteger, FieldDefinition> definitionSupplier,
      int amount) {
    logger.info("C{}: Starting handshake", clientId);
    try {
      ExecutorService es = Executors.newFixedThreadPool(servers.size() - 1);

      Party serverOne = servers.stream().filter(p -> p.getPartyId() == 1).findFirst().get();
      logger.info("C{}: connecting to master server {}", clientId, serverOne);
      TwoPartyNetwork masterNetwork = es
          .submit(connect(serverOne, getHandShakeMessage(0, amount))).get();
      logger.info("C{}: Connected to master server", clientId);
      byte[] response = masterNetwork.receive();

      int priority = intFromBytes(response);
      logger.info("C{}: Received priority {}", clientId, priority);

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
    System.arraycopy(ByteAndBitConverter.toByteArray(clientId), 0, msg, Integer.BYTES,
        Integer.BYTES);
    System.arraycopy(ByteAndBitConverter.toByteArray(amount), 0, msg, Integer.BYTES * 2,
        Integer.BYTES);
    return msg;
  }

  public static <T> List<List<T>> transpose(List<List<T>> table) {
    List<List<T>> ret = new ArrayList<List<T>>();
    final int N = table.get(0).size();
    for (int i = 0; i < N; i++) {
      List<T> col = new ArrayList<T>();
      for (List<T> row : table) {
        col.add(row.get(i));
      }
      ret.add(col);
    }
    return ret;
  }
}
