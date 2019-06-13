package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 */
public class DemoDdnntOutputClient extends DemoDdnntClientBase implements OutputClient {

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntOutputClient.class);

  private final int numOutputs;

  public DemoDdnntOutputClient(int numOutputs, int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    super(0, clientId, servers, definitionSupplier);
    this.numOutputs = numOutputs;
  }

  public DemoDdnntOutputClient(int numOutputs, int clientId,
      List<Party> servers) {
    this(numOutputs, clientId, servers, BigIntegerFieldDefinition::new);
  }

  @Override
  public List<BigInteger> getBigIntegerOutputs() {
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

  @Override
  public List<Long> getLongOutputs() {
    return getBigIntegerOutputs().stream().map(BigInteger::longValue).collect(Collectors.toList());
  }

  @Override
  public List<Integer> getIntOutputs() {
    return getBigIntegerOutputs().stream().map(BigInteger::intValue).collect(Collectors.toList());
  }

}
