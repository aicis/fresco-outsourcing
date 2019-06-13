package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;

/**
 * Handles requests for client input sessions and produces new client input sessions.
 */
public interface ClientInputSessionRequestHandler {

  /**
   * Gets the next fresh {@link DdnntClientInputSession} produced by this producer.
   *
   * <p>
   * This should block until a new session is available.
   * </p>
   *
   * @return a new session
   */
  DdnntClientInputSession next();

  /**
   * Tells if there producers will produce more sessions or if all expected sessions have handed
   * off.
   *
   * @return true if there are more sessions waiting, false otherwise.
   */
  boolean hasNext();

  /**
   * Adds request to produce another session for given client.
   *
   * @param suggestedPriority suggested client priority (used for mapping pre-processing material to
   * client)
   * @param clientId ID of client
   * @param inputAmount number of inputs expected from client
   * @param network network to communicate with client
   * @return priority assigned to client (may be same as suggested priority)
   */
  int registerNewSessionRequest(int suggestedPriority, int clientId, int inputAmount,
      TwoPartyNetwork network);

}
