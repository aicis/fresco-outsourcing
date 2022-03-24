package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Simple implementation of the DDNNT client session.
 */
public class DdnntClientInputSessionImpl implements DdnntClientInputSession {

  private int clientId;
  private int inputAmount;
  private TwoPartyNetwork network;
  private TripleDistributor distributor;
  private ByteSerializer<FieldElement> serializer;

  /**
   * Construct a new session.
   *
   * @param clientId the id of the client
   * @param inputAmount an amount of input to be given by the client
   * @param network a network to communicate with the client over
   * @param distributor a distributor serving the triples needed for the protocol
   * @param serializer a serializer for BigInteger's
   */
  public DdnntClientInputSessionImpl(int clientId, int inputAmount, TwoPartyNetwork network,
      TripleDistributor distributor, ByteSerializer<FieldElement> serializer) {
    this.clientId = clientId;
    this.inputAmount = inputAmount;
    this.network = network;
    this.distributor = distributor;
    this.serializer = serializer;
  }

  @Override
  public int getClientId() {
    return clientId;
  }

  @Override
  public int getAmountOfInputs() {
    return inputAmount;
  }

  @Override
  public TwoPartyNetwork getNetwork() {
    return network;
  }

  @Override
  public TripleDistributor getTripleDistributor() {
    return distributor;
  }

  @Override
  public ByteSerializer<FieldElement> getSerializer() {
    return serializer;
  }

}
