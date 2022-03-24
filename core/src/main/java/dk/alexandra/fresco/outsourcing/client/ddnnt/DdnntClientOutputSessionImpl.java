package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Simple implementation of the DDNNT output client session.
 */
public class DdnntClientOutputSessionImpl implements DdnntClientOutputSession {

  private int clientId;
  private TwoPartyNetwork network;
  private ByteSerializer<FieldElement> serializer;

  /**
   * Construct a new session.
   *
   * @param clientId the id of the client
   * @param network a network to communicate with the client over
   * @param serializer a serializer for BigInteger's
   */
  public DdnntClientOutputSessionImpl(int clientId, TwoPartyNetwork network,
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
