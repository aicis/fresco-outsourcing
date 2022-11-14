package dk.alexandra.fresco.outsourcing.client;

import java.math.BigInteger;
import java.util.List;

/**
 * The client side of an output procedure.
 *
 * <p>
 * Performs the protocol for receiving output from the servers and hands clear text outputs to the
 * client application.
 * </p>
 */
public interface OutputClient {

  /**
   * Gets output as a list of BigIntegers.
   *
   * @return a future eventually holding the output
   */
  List<BigInteger> getBigIntegerOutputs();

  /**
   * Gets output as a list of Longs.
   *
   * @return a future eventually holding the output
   */
  List<Long> getLongOutputs();


  /**
   * Gets output as a list of Integers.
   *
   * @return a future eventually holding the output
   */
  List<Integer> getIntOutputs();
}
