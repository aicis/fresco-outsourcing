package dk.alexandra.fresco.outsourcing.client;

import java.math.BigInteger;
import java.util.List;

public interface InputClient {

  void putBigIntegerInputs(List<BigInteger> inputs);

  void putLongInputs(List<Long> inputs);

  void putIntInputs(List<Integer> inputs);

}
