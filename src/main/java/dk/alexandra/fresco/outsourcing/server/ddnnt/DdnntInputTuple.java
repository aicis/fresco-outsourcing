package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;

public interface DdnntInputTuple {

  BigInteger getShareA();

  BigInteger getShareB();

  BigInteger getShareC();

  SInt getA();

}
