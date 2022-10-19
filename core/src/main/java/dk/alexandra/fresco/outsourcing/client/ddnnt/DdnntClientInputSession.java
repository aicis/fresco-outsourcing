package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.client.GenericClientSession;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Represents a session between a client and the server where the client provides an MPC input to
 * the servers using the DDNNT protocol.
 */
public class DdnntClientInputSession extends GenericClientSession {

  private final TripleDistributor distributor;
  private final int inputAmount;

  /**
   * Construct a new session.
   *
   * @param clientId    the id of the client
   * @param inputAmount an amount of input to be given by the client
   * @param network     a network to communicate with the client over
   * @param distributor a distributor serving the triples needed for the protocol
   * @param serializer  a serializer for BigInteger's
   */
  public DdnntClientInputSession(int clientId, int inputAmount, TwoPartyNetwork network,
                                 TripleDistributor distributor, ByteSerializer<FieldElement> serializer) {
    super(clientId, network, serializer);
    this.distributor = distributor;
    this.inputAmount = inputAmount;
  }

  /**
   * Gives the number of inputs to be given by the client.
   *
   * @return the number of input elements
   */
  public int getAmountOfInputs() {
    return inputAmount;
  }

  public TripleDistributor getTripleDistributor() {
    return distributor;
  }

}
