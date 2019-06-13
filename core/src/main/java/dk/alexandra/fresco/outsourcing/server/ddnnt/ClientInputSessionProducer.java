package dk.alexandra.fresco.outsourcing.server.ddnnt;

/**
 * Produces new client session (e.g., by listening for new connections from clients)
 *
 * <p>
 * The client session producer is responsible for setting up the session with the client. This
 * includes any rules about which connections to accept, how many client should give input, when to
 * stop accepting sessions, in which order to serve the clients and so on.
 * </p>
 */
public interface ClientInputSessionProducer {

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
   * Tells if there producers will produce mere sessions or if all expected sessions have handed
   * off.
   *
   * @return true if there are more sessions waiting, false otherwise.
   */
  boolean hasNext();

}
