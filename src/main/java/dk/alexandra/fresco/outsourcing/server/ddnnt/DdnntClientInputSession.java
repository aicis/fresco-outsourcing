package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.math.BigInteger;

/**
 * Represents a session between a client and the server where the client provides an MPC input to
 * the servers.
 */
public interface DdnntClientInputSession {

  /**
   * Gives the id of the client giving id.
   *
   * @return the client id
   */
  public int getClientId();

  /**
   * Gives the number of inputs to be given by the client.
   *
   * @return the number of input elements
   */
  public int getAmountOfInputs();

  /**
   * Gives the network connected to the client.
   *
   * @return a network connected to the client
   */
  public TwoPartyNetwork getNetwork();


  /**
   * Gives the triple distributor distributing triples for the given client and session.
   *
   * @return a triple distributor
   */
  public TripleDistributor getTripledistributor();

  /**
   * Gets the serializer used to serialize messages to the client.
   *
   * @return a byte serializer
   */
  public ByteSerializer<BigInteger> getSerializer();



}
