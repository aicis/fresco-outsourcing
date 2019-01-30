package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;

public class DdnntClientInputSessionImpl implements DdnntClientInputSession {

  private int clientId;
  private int inputAmount;
  private TwoPartyNetwork network;
  private TripleDistributor distributor;
  private ByteSerializer<BigInteger> serializer;

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
      TripleDistributor distributor, ByteSerializer<BigInteger> serializer) {
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
  public TripleDistributor getTripledistributor() {
    return distributor;
  }

  @Override
  public ByteSerializer<BigInteger> getSerializer() {
    return serializer;
  }

}
