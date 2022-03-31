package dk.alexandra.fresco.outsourcing.client;

import java.math.BigInteger;
import java.util.List;

/**
* The client side of an input procedure.
*
* <p>
* Performs the protocol for delivering input to the servers from the client application.
* </p>
*/
public interface InputClient {
  /**
   * Inputs a list of values given as BigIntegers.
   *
   * @param inputs a list of input values
   */
  void putBigIntegerInputs(List<BigInteger> inputs);

  /**
   * Inputs a list of values given as longs.
   *
   * @param inputs a list of input values
   */
  void putLongInputs(List<Long> inputs);

  /**
   * Inputs a list of values given as integers.
   *
   * @param inputs a list of input values
   */
  void  putIntInputs(List<Integer> inputs);

}
