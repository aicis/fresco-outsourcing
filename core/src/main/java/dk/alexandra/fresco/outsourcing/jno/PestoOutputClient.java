package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.ClientBase;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple demo client for the DDNNT output protocol.
 *
 * <p>Parts of the code contributed by Mathias Rahbek.</p>
 */
public class PestoOutputClient extends ClientBase implements OutputClient {

  private static final int MASTER_SERVER_ID = 1;

  private static final Logger logger = LoggerFactory.getLogger(PestoOutputClient.class);

  public PestoOutputClient(int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    super(clientId, servers);
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, 1);
      return null;
    }, "Failed client handshake");
  }

  public PestoOutputClient(int clientId, List<Party> servers) {
    this(clientId, servers, BigIntegerFieldDefinition::new);
  }

  @Override
  public List<BigInteger> getBigIntegerOutputs() {
    List<byte[]> results = new ArrayList<>();
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      results.add(network.receive());
      logger.info("C{}: Received output shares from server {}", clientId, s);
    }
    // TODO restore result
    results.stream().map(cur -> {
      if (!cur.equals(results.get(0))) {
        throw new MaliciousException("Inconsistent output from servers");
      }
      return null;
    });
    // TODO encode the result as BigInteger to be compatible with interface
    return Collections.singletonList(new BigInteger(1, results.get(0)));
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
