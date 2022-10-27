package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.ConcreteClientBase;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.utils.GenericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JnoCommonClient extends ConcreteClientBase {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final int amount;
    private final Drbg drbg;

    public JnoCommonClient(int amount, int clientId, List<Party> servers,
                           Function<BigInteger, FieldDefinition> definitionSupplier, Drbg drbg) {
        super(clientId, servers);
        this.amount = amount;
        this.drbg = drbg;
        ExceptionConverter.safe(() -> {
            this.handshake(definitionSupplier, amount);
            return null;
        }, "Failed client handshake");
    }
    private List<FieldElement> additivelyShare(FieldElement element, int amount) {
        List<FieldElement> randomSharing = randomSharing(amount-1);
        FieldElement sum = getDefinition().createElement(0);
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
        int bytesToSample = 1+((2* getDefinition().getBitLength()) / Byte.SIZE);
        byte[] bytes = new byte[bytesToSample];
        drbg.nextBytes(bytes);
        return getDefinition().createElement(new BigInteger(1, bytes));
    }

    private FieldElement computeTag(List<FieldElement> inputs, FieldElement key, FieldElement randomness) {
        FieldElement tag = getDefinition().createElement(0);
        FieldElement currentKeyPower = key;
        for (FieldElement current : inputs) {
            tag = tag.add(current.multiply(currentKeyPower));
            currentKeyPower = currentKeyPower.multiply(key);
        }
        tag = tag.add(randomness.multiply(currentKeyPower));
        return tag.add(currentKeyPower.multiply(key).multiply(key));
    }

    public void constructAndSendInputs(List<BigInteger> inputs) {
        if (inputs.size() != amount) {
            throw new IllegalArgumentException("Number of inputs does match");
        }
        List<FieldElement> inputsInField = inputs.stream().map(cur -> getDefinition().createElement(cur)).collect(Collectors.toList());
        List<List<FieldElement>> sharedInputs = GenericUtils.transpose(inputsInField.stream()
                .map(input -> additivelyShare(input, getServers().size()))
                .collect(Collectors.toList()));
        FieldElement key = randomElement();
        List<FieldElement> sharedKey = additivelyShare(key, getServers().size());
        FieldElement randomness = randomElement();
        List<FieldElement> sharedRandomness = additivelyShare(randomness, getServers().size());
        FieldElement tag = computeTag(inputsInField, key, randomness);
        List<FieldElement> sharedTag = additivelyShare(tag, getServers().size());
        for (int i = 0; i < getServers().size(); i++) {
            TwoPartyNetwork network = getServerNetworks().get(getServers().get(i).getPartyId());
            network.send(getDefinition().serialize(sharedTag.get(i)));
            network.send(getDefinition().serialize(sharedKey.get(i)));
            network.send(getDefinition().serialize(sharedRandomness.get(i)));
            network.send(getDefinition().serialize(sharedInputs.get(i)));
            logger.info("C{}: Send shared and tagged input to {}", getClientId(), getServers().get(i));
        }
    }

    protected int getAmount() {
        return amount;
    }
}
