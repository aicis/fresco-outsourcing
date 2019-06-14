package dk.alexandra.fresco.outsourcing.server.ddnnt;

/**
 * Produces sessions for input and output clients for the DDNNT protocol.
 */
public interface DdnntClientSessionProducer {

  /**
   * Produces session for next input client.
   */
  DdnntClientInputSession nextInput();

  /**
   * Returns true if there are more input client sessions left to produce.
   */
  boolean hasNextInput();

  /**
   * Produces session for next output client.
   */
  DdnntClientOutputSession nextOutput();

  /**
   * Returns true if there are more output client sessions left to produce.
   */
  boolean hasNextOutput();

}
