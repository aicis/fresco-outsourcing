package dk.alexandra.fresco.outsourcing.network;

import java.io.Closeable;
import java.io.IOException;

/**
 * A network connection between two parties.
 */
public interface TwoPartyNetwork extends Closeable {

  /**
   * Sends a message to the opposing party.
   *
   * @param msg the message
   */
  void send(byte[] msg);

  /**
   * Receives a message from an opposing party.
   * @return received date
   */
  byte[] receive();

  /**
   * Closes the connection to the other party.
   * @throws IOException if an exception happens during close.
   */
  @Override
  void close() throws IOException;

}
