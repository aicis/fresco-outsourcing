package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSession;

/**
 * Represents a session between a client and the server where the client provides an MPC input to
 * the servers using the DDNNT protocol.
 */
public interface DdnntClientInputSession extends ClientSession {

  /**
   * Gives the id of the client giving id.
   *
   * @return the client id
   */
  int getClientId();

  /**
   * Gives the number of inputs to be given by the client.
   *
   * @return the number of input elements
   */
  int getAmountOfInputs();

  /**
   * Gives the network connected to the client.
   *
   * @return a network connected to the client
   */
  TwoPartyNetwork getNetwork();

  /**
   * Gives the triple distributor distributing triples for the given client and session.
   *
   * @return a triple distributor
   */
  TripleDistributor getTripleDistributor();

  /**
   * Gets the serializer used to serialize messages to the client.
   *
   * @return a byte serializer
   */
  ByteSerializer<FieldElement> getSerializer();

}
