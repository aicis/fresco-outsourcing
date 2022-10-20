package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.utils.GenericUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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
public class DdnntOutputClient extends DdnntClientBase implements OutputClient {
  private static final Logger logger = LoggerFactory.getLogger(DdnntOutputClient.class);

  public DdnntOutputClient(int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    super(clientId, servers);
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, 1);
      return null;
    }, "Failed client handshake");
  }

  public DdnntOutputClient(int clientId, List<Party> servers) {
    this(clientId, servers, BigIntegerFieldDefinition::new);
  }

  @Override
  public List<BigInteger> getBigIntegerOutputs() {
    int numOutputs = receiveNumOutputs();

    List<List<FieldElement>> outputs = new ArrayList<>();
    List<BigInteger> finalResult = new ArrayList<>();

    for (int k = 0; k < numOutputs; k++) {
      List<FieldElement> el = IntStream.range(0, 5)
          .mapToObj(i -> getDefinition().createElement(0))
          .collect(Collectors.toList());
      outputs.add(el);
    }

    for (Party s : getServers()) {
      TwoPartyNetwork network = getServerNetworks().get(s.getPartyId());

      for (int i = 0; i < numOutputs; i++) {
        List<FieldElement> tmpList = getDefinition().deserializeList(network.receive());
        outputs.set(i, sumLists(outputs.get(i), tmpList));
      }

      logger.info("C{}: Received output shares from server {}", getClientId(), s);
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
        finalResult.add(getDefinition().convertToUnsigned(y));
      }
    }
    return finalResult;
  }

  /**
   * Receives number of outputs from all servers and verifies that info is consistent.
   */
  private int receiveNumOutputs() {
    int numOutputs = getNumOutputsFrom(getServers().get(0).getPartyId());
    for (Party s : getServers().subList(1, getServers().size())) {
      int newNumOutputs = getNumOutputsFrom(s.getPartyId());
      if (newNumOutputs != numOutputs) {
        throw new MaliciousException("Received incorrect number of outputs for servers");
      }
    }
    return numOutputs;
  }

  /**
   * Receives number of outputs from given party.
   */
  private int getNumOutputsFrom(int partyId) {
    TwoPartyNetwork network = getServerNetworks().get(partyId);
    return GenericUtils.intFromBytes(network.receive());
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
