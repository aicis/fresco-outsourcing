package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSession;
import sweis.threshsig.KeyShare;

public class JnoClientSession implements ClientSession {
  private int clientId;
  private int amount;
  private TwoPartyNetwork network;
  private ByteSerializer<FieldElement> serializer;
  private KeyShare keyShare;

  public JnoClientSession(int clientId, int amount, TwoPartyNetwork network,
      ByteSerializer<FieldElement> serializer, KeyShare keyShare) {
    this.clientId = clientId;
    this.amount = amount;
    this.network = network;
    this.serializer = serializer;
    this.keyShare = keyShare;
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

  public KeyShare getKeyShare() {
    return keyShare;
  }

}
