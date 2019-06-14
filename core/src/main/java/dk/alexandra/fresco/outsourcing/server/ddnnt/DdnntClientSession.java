package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Represents a generic session between a client and the server.
 */
public interface DdnntClientSession {

  /**
   * Gives the id of the client giving id.
   *
   * @return the client id
   */
  int getClientId();

  /**
   * Gives the network connected to the client.
   *
   * @return a network connected to the client
   */
  TwoPartyNetwork getNetwork();

  /**
   * Gets the serializer used to serialize messages to the client.
   *
   * @return a byte serializer
   */
  ByteSerializer<FieldElement> getSerializer();

}
