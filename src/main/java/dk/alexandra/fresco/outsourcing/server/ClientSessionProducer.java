package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.outsourcing.server.ddnnt.ClientInputSession;

/**
 * Produces new input sessions (e.g., by listen for new connections from from clients)
 */
public interface ClientSessionProducer {

  /**
   * Gets the next fresh {@link ClientInputSession} produced by this producer.
   *
   * <p>
   * This should block until a new session is available.
   * </p>
   *
   * @return a new input session
   */
  public ClientInputSession next();

  /**
   * Tells if there producers will produce mere sessions or if all expected sessions have handed
   * off.
   *
   * @return true if there are more sessions waiting, false otherwise.
   */
  public boolean hasNext();

}
