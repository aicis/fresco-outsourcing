package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.ClientBase;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JnoInputClient extends ClientBase implements InputClient {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final int numInputs;
  private final Drbg drbg;

  public JnoInputClient(int numInputs, int clientId, List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier, Drbg drbg) {
    super(clientId, servers);
    this.numInputs = numInputs;
    this.drbg = drbg;
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, numInputs);
      return null;
    }, "Failed client handsha1ke");
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

  private FieldElement randomElement() {
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

  @Override
  public void putBigIntegerInputs(List<BigInteger> inputs) {
    if (inputs.size() != numInputs) {
      throw new IllegalArgumentException("Number of inputs does match");
    }
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
  public void putLongInputs(List<Long> inputs) {
    putBigIntegerInputs(inputs.stream().map(BigInteger::valueOf).collect(Collectors.toList()));
  }

  @Override
  public void putIntInputs(List<Integer> inputs) {
    putBigIntegerInputs(inputs.stream().map(BigInteger::valueOf).collect(Collectors.toList()));
  }
}
