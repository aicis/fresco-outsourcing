package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.math.BigInteger;
import java.util.Objects;

public class SpdzDdnntTuple implements DdnntInputTuple {

  private final BigInteger shareA;
  private final BigInteger shareB;
  private final BigInteger shareC;
  private final SInt sintA;

  public SpdzDdnntTuple(SpdzTriple triple) {
    Objects.requireNonNull(triple);
    this.sintA = Objects.requireNonNull(triple.getA());
    this.shareA = Objects.requireNonNull(triple.getA().getShare());
    this.shareB = Objects.requireNonNull(triple.getB().getShare());
    this.shareC = Objects.requireNonNull(triple.getC().getShare());
  }

  @Override
  public BigInteger getShareA() {
    return shareA;
  }

  @Override
  public BigInteger getShareB() {
    return shareB;
  }

  @Override
  public BigInteger getShareC() {
    return shareC;
  }

  @Override
  public SInt getA() {
    return sintA;
  }

}
