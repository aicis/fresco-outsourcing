package dk.alexandra.fresco.outsourcing.client.ddnnt;

import static dk.alexandra.fresco.outsourcing.utils.ByteConversionUtils.intFromBytes;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple demo client for the DDNNT input protocol.
 *
 * <p>
 * The DDNNT client will proceed as follows to provide input:
 * <ol>
 * <li>Connect to server 1 and send an introductory message including the unique id of the client,
 * and the number of inputs the client will give.
 * <li>Receive a response from server 1 giving the client a priority. This priority will define the
 * order in which servers should handle client inputs to make it possible for the servers to
 * coordinate on the preprocessed material to use for each client.
 * <li>The client will then connect to all other servers and send an introductory message including
 * it's priority, the unique id and the number of inputs of the client.
 * <li>From the servers the client receives shares of a triple for each input.
 * <li>The client checks if the shares reconstructs to a correct triple.
 * <li>If so, the client uses the reconstructed first value of the triple to mask the input and
 * sends this value to each servers.
 * </ol>
 * </p>
 */
public class DemoDdnntInputClient extends DemoDdnntClientBase implements InputClient {

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntInputClient.class);

  private final int numInputs;

  /**
   * Constructs a new input client delivering a given number of values to a given set of servers.
   *
   * <p>
   * Note, that on construction the client will start the protocol by connecting to the servers as
   * described above and perform the handshake, the servers may then start transferring the
   * preprocessed material to the client, even before input is received from the client
   * application.
   * </p>
   *
   * @param numInputs number of input values to deliver
   * @param clientId the unique id of the client (should be unique among all clients)
   * @param servers a list of servers to deliver input to
   */
  public DemoDdnntInputClient(int numInputs, int clientId, List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    super(clientId, servers);
    this.numInputs = numInputs;
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, numInputs);
      return null;
    }, "Failed client handshake");
  }

  @Override
  public void putBigIntegerInputs(List<BigInteger> inputs) {
    if (inputs.size() != numInputs) {
      throw new IllegalArgumentException("Number of inputs does match");
    }
    List<FieldElement> accA =
        IntStream.range(0, numInputs).mapToObj(i -> definition.createElement(0))
            .collect(Collectors.toList());
    List<FieldElement> accB =
        IntStream.range(0, numInputs).mapToObj(i -> definition.createElement(0))
            .collect(Collectors.toList());
    List<FieldElement> accC =
        IntStream.range(0, numInputs).mapToObj(i -> definition.createElement(0))
            .collect(Collectors.toList());
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      List<FieldElement> tmpA = definition.deserializeList(network.receive());
      accA = sumLists(accA, tmpA);
      List<FieldElement> tmpB = definition.deserializeList(network.receive());
      accB = sumLists(accB, tmpB);
      List<FieldElement> tmpC = definition.deserializeList(network.receive());
      accC = sumLists(accC, tmpC);
      if (!(tmpA.size() == numInputs && tmpB.size() == numInputs && tmpC.size() == numInputs)) {
        throw new MaliciousException(
            "Number of input tuple shares received not matching the number of inputs");
      }
      logger.info("C{}: Received input tuples from server {}", clientId, s);
    }
    for (int i = 0; i < accA.size(); i++) {
      FieldElement a = accA.get(i);
      FieldElement b = accB.get(i);
      FieldElement c = accC.get(i);
      if (!productCheck(a, b, c)) {
        logger.debug("Product was {} but should be {}", a.multiply(b), c);
        throw new MaliciousException("Mac for input " + i + " did not pass check");
      }
    }
    List<FieldElement> maskedInputs = new ArrayList<>(numInputs);
    for (int i = 0; i < inputs.size(); i++) {
      maskedInputs.add(definition.createElement(inputs.get(i)).subtract(accA.get(i)));
    }
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      network.send(definition.serialize(maskedInputs));
      logger.info("C{}: Send masked input to {}", clientId, s);
    }
  }

  @Override
  public void putLongInputs(List<Long> inputs) {
    putBigIntegerInputs(inputs.stream().map(BigInteger::valueOf).collect(Collectors.toList()));
  }

  @Override
  public void putIntInputs(List<Integer> inputs) {
    putBigIntegerInputs(inputs.stream().map(BigInteger::valueOf).collect(Collectors.toList()));
  }

  /**
   * Default constructor that uses {@link BigIntegerFieldDefinition} for the default field
   * definition.
   */
  public DemoDdnntInputClient(int numInputs, int clientId, List<Party> servers) {
    this(numInputs, clientId, servers, BigIntegerFieldDefinition::new);
  }

  private void handshake(Function<BigInteger, FieldDefinition> definitionSupplier,
      int numInputs) {
    logger.info("C{}: Starting handshake", clientId);
    try {
      ExecutorService es = Executors.newFixedThreadPool(servers.size() - 1);

      Party serverOne = servers.stream().filter(p -> p.getPartyId() == 1).findFirst().get();
      logger.info("C{}: connecting to master server {}", clientId, serverOne);
      TwoPartyNetwork masterNetwork = es
          .submit(connect(serverOne, getHandShakeMessage(0, numInputs))).get();
      logger.info("C{}: Connected to master server", clientId);
      byte[] response = masterNetwork.receive();

      int priority = intFromBytes(response);
      logger.info("C{}: Received priority {}", clientId, priority);

      initServerNetworks(es, masterNetwork, getHandShakeMessage(priority, numInputs));

      es.shutdown();

      initFieldDefinition(definitionSupplier, masterNetwork);
    } catch (Exception e) {
      logger.error("Error during handshake", e);
      e.printStackTrace();
    }
  }

  private byte[] getHandShakeMessage(int priority, int numInputs) {
    byte[] msg = new byte[Integer.BYTES * 3];
    System.arraycopy(ByteAndBitConverter.toByteArray(priority), 0, msg, 0, Integer.BYTES);
    System.arraycopy(ByteAndBitConverter.toByteArray(clientId), 0, msg, Integer.BYTES,
        Integer.BYTES);
    System.arraycopy(ByteAndBitConverter.toByteArray(numInputs), 0, msg, Integer.BYTES * 2,
        Integer.BYTES);
    return msg;
  }

}
