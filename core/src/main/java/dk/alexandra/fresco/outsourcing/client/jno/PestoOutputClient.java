package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.ClientBase;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A simple demo client for the DDNNT output protocol.
 *
 * <p>Parts of the code contributed by Mathias Rahbek.</p>
 */
public class PestoOutputClient extends ClientBase implements OutputClient {
  private static final Logger logger = LoggerFactory.getLogger(PestoOutputClient.class);
  private final Function<BigInteger, FieldDefinition> definitionSupplier;
  private final Drbg drbg;
  private final int amountOfOutputs;

  public PestoOutputClient(int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier, Drbg drbg, int amountOfOutputs) {
    super(clientId, servers);
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, amountOfOutputs);
      return null;
    }, "Failed client handshake");
    this.definitionSupplier = definitionSupplier;
    this.drbg = drbg;
    this.amountOfOutputs = amountOfOutputs;
  }

  public PestoOutputClient(int clientId, List<Party> servers, Drbg drbg, int amountOfOutputs) {
    this(clientId, servers, BigIntegerFieldDefinition::new, drbg, amountOfOutputs);
  }

  private List<FieldElement> additivelyShare(FieldElement element, int amount) {
    List<FieldElement> randomSharing = randomSharing(amount-1);
    FieldElement sum = definition.createElement(0);
    for (FieldElement cur : randomSharing) {
      sum = sum.add(cur);
    }
    // Make an additive sharing of the input by summing the random shares
    randomSharing.add(element.subtract(sum));
    return randomSharing;
  }

  private List<FieldElement> randomSharing(int amount) {
    // Pick random sharing
    List<FieldElement> shares = IntStream.range(0, amount)
            .mapToObj(cur -> randomElement()).collect(Collectors.toList());
    return shares;
  }

  FieldElement randomElement() {
    // Amount of bytes is 2*modulus size to ensure that there is no bias for any statistical sec par up to the size of the modulus.
    int bytesToSample = 1+((2*definition.getBitLength()) / Byte.SIZE);
    byte[] bytes = new byte[bytesToSample];
    drbg.nextBytes(bytes);
    return definition.createElement(new BigInteger(1, bytes));
  }

  private FieldElement computeTag(List<FieldElement> inputs, FieldElement key, FieldElement randomness) {
    FieldElement tag = definition.createElement(0);
    FieldElement currentKeyPower = key;
    for (FieldElement current : inputs) {
      tag = tag.add(current.multiply(currentKeyPower));
      currentKeyPower = currentKeyPower.multiply(key);
    }
    tag = tag.add(randomness.multiply(currentKeyPower));
    return tag.add(currentKeyPower.multiply(key).multiply(key));
  }

  void putBigIntegerInputs(List<BigInteger> inputs) {
    List<FieldElement> inputsInField = inputs.stream().map(cur -> definition.createElement(cur)).collect(Collectors.toList());
    List<List<FieldElement>> sharedInputs = transpose(inputsInField.stream()
            .map(input -> additivelyShare(input, servers.size()))
            .collect(Collectors.toList()));
    FieldElement key = randomElement();
    List<FieldElement> sharedKey = additivelyShare(key, servers.size());
    FieldElement randomness = randomElement();
    List<FieldElement> sharedRandomness = additivelyShare(randomness, servers.size());
    FieldElement tag = computeTag(inputsInField, key, randomness);
    List<FieldElement> sharedTag = additivelyShare(tag, servers.size());
    for (int i = 0; i < servers.size(); i++) {
      TwoPartyNetwork network = serverNetworks.get(servers.get(i).getPartyId());
      network.send(definition.serialize(sharedTag.get(i)));
      network.send(definition.serialize(sharedKey.get(i)));
      network.send(definition.serialize(sharedRandomness.get(i)));
      network.send(definition.serialize(sharedInputs.get(i)));
      logger.info("C{}: Send shared and tagged input to {}", clientId, servers.get(i));
    }
  }
  @Override
  public List<BigInteger> getBigIntegerOutputs() {
//    JnoInputClient randomInputClient = new JnoInputClient(amountOfOutputs, clientId, servers, definitionSupplier, drbg);
    List<FieldElement> randomPaddings = IntStream.range(0, amountOfOutputs)
            .mapToObj(num -> randomElement()).collect(Collectors.toList());
    List<BigInteger> inputs = IntStream.range(0, amountOfOutputs)
        .mapToObj(num -> randomElement().toBigInteger()).collect(Collectors.toList());
    putBigIntegerInputs(inputs);

    List<BigInteger> defaultReply = null;
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      if (defaultReply == null) {
        defaultReply = definition.deserializeList(network.receive()).stream().map(cur -> cur.toBigInteger()).collect(Collectors.toList());
      } else {
        // check consistency of server replies
        List<BigInteger> current = definition.deserializeList(network.receive()).stream().map(cur -> cur.toBigInteger()).collect(Collectors.toList());
        for (int i = 0; i < current.size(); i++) {
          if (!current.get(i).equals(defaultReply.get(i))) {
            throw new MaliciousException("The server output is not consistent");
          }
        }
      }
      logger.info("C{}: Received output shares from server {}", clientId, s);
    }

    List<BigInteger> results = new ArrayList<>();
    for (int i = 0; i < amountOfOutputs; i++) {
      results.add(defaultReply.get(i).subtract(inputs.get(i)));
    }
    return results;
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
