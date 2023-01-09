package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.Drbg;
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
 * A simple demo client for the JNO output protocol.
 */
public class JnoOutputClient extends JnoCommonClient implements OutputClient {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public JnoOutputClient(int clientId,
                         List<Party> servers,
                         Function<BigInteger, FieldDefinition> definitionSupplier, Drbg drbg, int amountOfOutputs) {
    super(amountOfOutputs, clientId, servers, definitionSupplier, drbg);
  }

  public JnoOutputClient(int clientId, List<Party> servers, Drbg drbg, int amountOfOutputs) {
    this(clientId, servers, BigIntegerFieldDefinition::new, drbg, amountOfOutputs);
  }

  @Override
  public List<BigInteger> getBigIntegerOutputs() {
    List<BigInteger> randomPaddings = IntStream.range(0, getAmount())
        .mapToObj(num -> randomElement().toBigInteger()).collect(Collectors.toList());
    constructAndSendInputs(randomPaddings);

    List<BigInteger> defaultReply = null;
    for (Party s : getServers()) {
      TwoPartyNetwork network = getServerNetworks().get(s.getPartyId());
      if (defaultReply == null) {
        defaultReply = getDefinition().deserializeList(network.receive()).stream().map(cur -> cur.toBigInteger()).collect(Collectors.toList());
      } else {
        // check consistency of server replies
        List<BigInteger> current = getDefinition().deserializeList(network.receive()).stream().map(cur -> cur.toBigInteger()).collect(Collectors.toList());
        for (int i = 0; i < current.size(); i++) {
          if (!current.get(i).equals(defaultReply.get(i))) {
            throw new MaliciousException("The server output is not consistent");
          }
        }
      }
      logger.info("C{}: Received output shares from server {}", getClientId(), s);
    }
    List<BigInteger> results = new ArrayList<>();
    for (int i = 0; i < getAmount(); i++) {
      results.add(defaultReply.get(i).subtract(randomPaddings.get(i)));
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
