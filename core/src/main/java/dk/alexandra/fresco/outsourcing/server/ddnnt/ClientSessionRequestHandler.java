package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Produces new client sessions for each registered request.
 *
 * @param <SessionT> the session type (input or output) this handler can process
 */
public interface ClientSessionRequestHandler<SessionT extends DdnntClientSession> {

  /**
   * Gets the next fresh {@link SessionT} produced by this producer.
   *
   * <p>
   * This should block until a new session is available.
   * </p>
   *
   * @return a new session
   */
  SessionT next();

  /**
   * Tells if there producers will produce more sessions or if all expected sessions have been
   * handed off.
   *
   * @return true if there are more sessions waiting, false otherwise.
   */
  boolean hasNext();

  /**
   * Adds request to produce another session for given client.
   *
   * @param handshakeMessage the handshake message received from the client
   * @return priority assigned to client (may be same as suggested priority)
   */
  int registerNewSessionRequest(byte[] handshakeMessage, TwoPartyNetwork network);

}
