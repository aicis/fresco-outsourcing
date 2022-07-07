package dk.alexandra.fresco.outsourcing.client;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSession;

public class GenericClientSession implements ClientSession {
  private final int clientId;
  private final TwoPartyNetwork network;
  private final ByteSerializer<FieldElement> serializer;

  public GenericClientSession(int clientId, TwoPartyNetwork network,
                              ByteSerializer<FieldElement> serializer) {
    this.clientId = clientId;
    this.network = network;
    this.serializer = serializer;
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
