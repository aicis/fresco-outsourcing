package dk.alexandra.fresco.outsourcing.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class with different static methods.
 */
public class GenericUtils {

  private GenericUtils() {
  }

  public static <T> List<List<T>> transpose(List<List<T>> table) {
    List<List<T>> ret = new ArrayList<List<T>>();
    final int N = table.get(0).size();
    for (int i = 0; i < N; i++) {
      List<T> col = new ArrayList<T>();
      for (List<T> row : table) {
        col.add(row.get(i));
      }
      ret.add(col);
    }
    return ret;
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
