package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.network.serializers.BigIntegerWithFixedLengthSerializer;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.network.ClientSideNetworkFactory;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple demo client for the Ddnnt input protocol.
 *
 * <p>
 * The ddnnt client will proceed as follows to provide input:
 * <ol>
 * <li>Connect to server 1 and send an introductory message including the unique id of the client,
 * and the number of inputs the client will give.
 * <li>Receive a response from server 1 giving the client a priority.
 * <li>The client will then connect to all other servers and send an introductory message including
 * the unique id of the priority, the unique id and the number of inputs of the client.
 * <li>From the servers the client receives shares of a triple for each input.
 * <li>The client checks if the shares reconstructs to a correct triple.
 * <li>If so, the client uses the reconstructed first value of the triple to mask the input and
 * sends this value to each servers.
 * </ol>
 * </p>
 */
public class DemoDdnntInputClient implements InputClient {

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntInputClient.class);

  private int numInputs;
  private int clientId;
  private BigInteger modulus;
  private List<Party> servers;
  private Map<Integer, TwoPartyNetwork> serverNetworks;

  public DemoDdnntInputClient(int numInputs, int clientId, List<Party> servers) {
    this.numInputs = numInputs;
    this.clientId = clientId;
    this.servers = servers;
    ExceptionConverter.safe(() -> {
      this.handshake();
      return null;
    }, "Failed client handshake");
  }

  private void handshake() throws InterruptedException, ExecutionException {
    logger.info("C{}: Starting handshake", clientId);
    try {
      ExecutorService es = Executors.newFixedThreadPool(servers.size() - 1);
      Party serverOne = servers.stream().filter(p -> p.getPartyId() == 1).findFirst().get();
      logger.info("C{}: connecting to master server {}", clientId, serverOne);
      TwoPartyNetwork masterNetwork = es.submit(connect(serverOne, 0)).get();
      logger.info("C{}: Connected to master server", clientId);
      byte[] response = masterNetwork.receive();
      int priority = intFromBytes(response);
      logger.info("C{}: Received priotity {}", clientId, priority);
      Map<Integer, Future<TwoPartyNetwork>> futureNetworks = new HashMap<>(servers.size() - 1);
      for (Party s : servers.stream().filter(p -> p.getPartyId() != 1)
          .collect(Collectors.toList())) {
        Future<TwoPartyNetwork> futureNetwork = es.submit(connect(s, priority));
        futureNetworks.put(s.getPartyId(), futureNetwork);
      }
      serverNetworks = new HashMap<>(servers.size());
      serverNetworks.put(1, masterNetwork);
      for (Entry<Integer, Future<TwoPartyNetwork>> f : futureNetworks.entrySet()) {
        serverNetworks.put(f.getKey(), f.getValue().get());
      }
      es.shutdown();
      byte[] modResponse = masterNetwork.receive();
      this.modulus = new BigInteger(modResponse);
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

  private Callable<TwoPartyNetwork> connect(Party server, int priority) {
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

  @Override
  public void putBigIntegerInputs(List<BigInteger> inputs) {
    if (inputs.size() != numInputs) {
      throw new IllegalArgumentException("Number of inputs does match");
    }
    List<BigInteger> accA =
        IntStream.range(0, numInputs).mapToObj(i -> BigInteger.ZERO).collect(Collectors.toList());
    List<BigInteger> accB =
        IntStream.range(0, numInputs).mapToObj(i -> BigInteger.ZERO).collect(Collectors.toList());
    List<BigInteger> accC =
        IntStream.range(0, numInputs).mapToObj(i -> BigInteger.ZERO).collect(Collectors.toList());
    BigIntegerWithFixedLengthSerializer serializer =
        new BigIntegerWithFixedLengthSerializer(modulus.toByteArray().length);
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      List<BigInteger> tmpA = serializer.deserializeList(network.receive());
      accA = sumLists(accA, tmpA);
      List<BigInteger> tmpB = serializer.deserializeList(network.receive());
      accB = sumLists(accB, tmpB);
      List<BigInteger> tmpC = serializer.deserializeList(network.receive());
      accC = sumLists(accC, tmpC);
      if (!(tmpA.size() == numInputs && tmpB.size() == numInputs && tmpC.size() == numInputs)) {
        throw new MaliciousException(
            "Number of input tuple shares received not matching the number of inputs");
      }
      logger.info("C{}: Received input tuples from server {}", clientId, s);
    }
    accA = accA.stream().map(i -> i.mod(modulus)).collect(Collectors.toList());
    accB = accB.stream().map(i -> i.mod(modulus)).collect(Collectors.toList());
    accC = accC.stream().map(i -> i.mod(modulus)).collect(Collectors.toList());
    for (int i = 0; i < accA.size(); i++) {
      if (!accA.get(i).multiply(accB.get(i)).mod(modulus).equals(accC.get(i))) {
        logger.debug("Product was {} but shoudl be {}",
            accA.get(i).multiply(accB.get(i)).mod(modulus), accC.get(i));
        throw new MaliciousException("Mac for input " + i + " did not pass check");
      }
    }
    List<BigInteger> maskedInputs = new ArrayList<>(numInputs);
    for (int i = 0; i < inputs.size(); i++) {
      maskedInputs.add(inputs.get(i).subtract(accA.get(i)).mod(modulus));
    }
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      network.send(serializer.serialize(maskedInputs));
      logger.info("C{}: Send masked input to {}", clientId, s);
    }
  }

  private List<BigInteger> sumLists(List<BigInteger> left, List<BigInteger> right) {
    List<BigInteger> res = new ArrayList<>(left.size());
    for (int i = 0; i < left.size(); i++) {
      BigInteger b = left.get(i).add(right.get(i));
      res.add(b);
    }
    return res;
  }

  @Override
  public void putLongInputs(List<Long> inputs) {
    // TODO Auto-generated method stub

  }

  @Override
  public void putIntInputs(List<Integer> inputs) {
    // TODO Auto-generated method stub

  }

}
