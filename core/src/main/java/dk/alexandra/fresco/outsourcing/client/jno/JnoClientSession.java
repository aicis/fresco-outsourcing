package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSession;

public class JnoClientSession implements ClientSession {
  private int clientId;
  private int amount;
  private TwoPartyNetwork network;
  private ByteSerializer<FieldElement> serializer;

  public JnoClientSession(int clientId, int amount, TwoPartyNetwork network,
      ByteSerializer<FieldElement> serializer) {
    this.clientId = clientId;
    this.amount = amount;
    this.network = network;
    this.serializer = serializer;
  }

  public int getAmount() {
    return amount;
  }

  @Override
  public int getClientId() {
    return clientId;
  }

  @Override
  public TwoPartyNetwork getNetwork() {
    return network;
  }

  @Override
  public ByteSerializer<FieldElement> getSerializer() {
    return serializer;
  }

}
