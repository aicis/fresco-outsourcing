package dk.alexandra.fresco.outsourcing.client.ddnnt;

import static dk.alexandra.fresco.outsourcing.utils.ByteConversionUtils.intFromBytes;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.utils.ByteConversionUtils;
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
 * A simple demo client for the DDNNT output protocol.
 *
 * <p>Parts of the code contributed by Mathias Rahbek.</p>
 */
public class DemoDdnntOutputClient extends DemoDdnntClientBase implements OutputClient {

  private static final int MASTER_SERVER_ID = 1;

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntOutputClient.class);

  public DemoDdnntOutputClient(int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    super(clientId, servers);
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier);
      return null;
    }, "Failed client handshake");
  }

  public DemoDdnntOutputClient(int clientId, List<Party> servers) {
    this(clientId, servers, BigIntegerFieldDefinition::new);
  }

  private void handshake(Function<BigInteger, FieldDefinition> definitionSupplier) {
    logger.info("C{}: Starting handshake", clientId);
    try {
      ExecutorService es = Executors.newFixedThreadPool(servers.size() - 1);

      Party serverOne = servers.stream().filter(p -> p.getPartyId() == 1).findFirst().get();
      logger.info("C{}: connecting to master server {}", clientId, serverOne);
      TwoPartyNetwork masterNetwork = es
          .submit(connect(serverOne, getHandShakeMessage(0))).get();
      logger.info("C{}: Connected to master server", clientId);
      byte[] response = masterNetwork.receive();

      int priority = intFromBytes(response);
      logger.info("C{}: Received priority {}", clientId, priority);

      initServerNetworks(es, masterNetwork, getHandShakeMessage(priority));

      es.shutdown();

      initFieldDefinition(definitionSupplier, masterNetwork);
    } catch (Exception e) {
      logger.error("Error during handshake", e);
      e.printStackTrace();
    }
  }

  @Override
  public List<BigInteger> getBigIntegerOutputs() {
    int numOutputs = receiveNumOutputs();

    List<List<FieldElement>> outputs = new ArrayList<>();
    List<BigInteger> finalResult = new ArrayList<>();

    for (int k = 0; k < numOutputs; k++) {
      List<FieldElement> el = IntStream.range(0, 5)
          .mapToObj(i -> definition.createElement(0))
          .collect(Collectors.toList());
      outputs.add(el);
    }

    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());

      for (int i = 0; i < numOutputs; i++) {
        List<FieldElement> tmpList = definition.deserializeList(network.receive());
        outputs.set(i, sumLists(outputs.get(i), tmpList));
      }

      logger.info("C{}: Received output shares from server {}", clientId, s);
    }

    for (List<FieldElement> e : outputs) {
      FieldElement r = e.get(0);
      FieldElement v = e.get(1);
      FieldElement w = e.get(2);
      FieldElement u = e.get(3);
      FieldElement y = e.get(4);
      if (!productCheck(y, r, w)) {
        logger.debug("y * r was {} but should be {}", y.multiply(r), w);
        throw new MaliciousException("Authentication did not pass check");
      } else if (!productCheck(v, r, u)) {
        logger.debug("v * r was {} but should be {}", v.multiply(r), u);
        throw new MaliciousException("Authentication did not pass check");
      } else {
        finalResult.add(definition.convertToUnsigned(y));
      }
    }
    return finalResult;
  }

  private int receiveNumOutputs() {
    TwoPartyNetwork masterNetwork = serverNetworks.get(MASTER_SERVER_ID);
    return ByteConversionUtils.intFromBytes(masterNetwork.receive());
  }

  private byte[] getHandShakeMessage(int priority) {
    byte[] msg = new byte[Integer.BYTES * 2];
    System.arraycopy(ByteAndBitConverter.toByteArray(priority), 0, msg, 0, Integer.BYTES);
    System.arraycopy(ByteAndBitConverter.toByteArray(clientId), 0, msg, Integer.BYTES,
        Integer.BYTES);
    return msg;
  }

  @Override
  public List<Long> getLongOutputs() {
    return getBigIntegerOutputs().stream().map(BigInteger::longValue).collect(Collectors.toList());
  }

  @Override
  public List<Integer> getIntOutputs() {
    return getBigIntegerOutputs().stream().map(BigInteger::intValue).collect(Collectors.toList());
  }

}
