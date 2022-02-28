package sweis.threshsig;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ThreshUtil {
  // Constants and variables
  //............................................................................
  protected final static BigInteger ZERO = BigInteger.ZERO;

  protected final static BigInteger ONE = BigInteger.ONE;

  protected final static BigInteger TWO = BigInteger.valueOf(2L);

  protected final static BigInteger FOUR = BigInteger.valueOf(4L);

  /** Fermat prime F4. */
  public final static BigInteger F4 = BigInteger.valueOf(0x10001L);

  /** An arbitrary security parameter for generating secret shares */
  protected final static int L1 = 128;
  static  SecureRandom random;
  static {
    try {
      random = SecureRandom.getInstance("SHA1PRNG");
      random.setSeed(42);
    } catch (Exception e) {
      throw new RuntimeException("Could not intialize static randomness");
    }
  }

  public static SecureRandom getRandom() {
    return random;
  }
}
