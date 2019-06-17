package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Handles new client session requests for {@link SessionT} type.
 *
 * @param <SessionT> the session type (input or output) this handler can process
 */
public interface ClientSessionRegistration<SessionT extends DdnntClientSession> {

  /**
   * Adds request to produce another session for given client.
   *
   * @param handshakeMessage the handshake message received from the client
   * @return priority assigned to client (may be same as suggested priority)
   */
  int registerNewSessionRequest(byte[] handshakeMessage, TwoPartyNetwork network);

  /**
   * Returns number of clients expected to register with this handler.
   */
  int getExpectedClients();

}
