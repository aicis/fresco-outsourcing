package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JnoInputClient extends JnoCommonClient implements InputClient {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public JnoInputClient(int numInputs, int clientId, List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier, Drbg drbg) {
    super(numInputs, clientId, servers, definitionSupplier, drbg);
  }

  @Override
  public void putBigIntegerInputs(List<BigInteger> inputs) {
    constructAndSendInputs(inputs);
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
