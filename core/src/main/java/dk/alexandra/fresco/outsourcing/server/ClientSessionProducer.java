package dk.alexandra.fresco.outsourcing.server;

/**
 * Produces new client sessions.
 *
 * @param <SessionT> the session type (input or output) this handler can process
 */
public interface ClientSessionProducer<SessionT extends ClientSession> {

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

}
