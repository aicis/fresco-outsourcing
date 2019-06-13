package dk.alexandra.fresco.outsourcing.utils;

/**
 * Helper class for byte conversions.
 */
public class ByteConversionUtils {

  private ByteConversionUtils() {
  }

  /**
   * Converts big-endian byte array to int.
   */
  public static int intFromBytes(byte[] bytes) {
    int res = 0;
    int topByteIndex = Byte.SIZE * (Integer.BYTES - 1);
    for (int i = 3; i >= 0; i--) {
      res ^= (bytes[i] & 0xFF) << (topByteIndex - i * Byte.SIZE);
    }
    return res;
  }

}
