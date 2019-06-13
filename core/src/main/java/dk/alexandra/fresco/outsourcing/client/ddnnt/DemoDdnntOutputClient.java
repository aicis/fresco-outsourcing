package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoDdnntOutputClient extends DemoDdnntClientBase implements OutputClient {

  private static final Logger logger = LoggerFactory.getLogger(DemoDdnntOutputClient.class);

  private final int numOutputs;

  DemoDdnntOutputClient(int numOutputs, int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier) {
    super(0, clientId, servers, definitionSupplier);
    this.numOutputs = numOutputs;
  }

  @Override
  public Future<List<BigInteger>> getBigIntegerOutputs() {
    return null;
  }

  @Override
  public Future<List<Long>> getLongOutputs() {
    return null;
  }

  @Override
  public Future<List<Integer>> getIntOutputs() {
    return null;
  }

}
