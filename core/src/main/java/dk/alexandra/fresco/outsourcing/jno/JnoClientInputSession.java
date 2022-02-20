package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSession;

public class JnoClientInputSession implements ClientSession {

  @Override
  public int getClientId() {
    return 0;
  }

  @Override
  public TwoPartyNetwork getNetwork() {
    return null;
  }

  @Override
  public ByteSerializer<FieldElement> getSerializer() {
    return null;
  }
}
