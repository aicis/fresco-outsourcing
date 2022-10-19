package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.compare.Comparison.Algorithm;
import java.util.Arrays;

public class Range implements Computation<SInt, ProtocolBuilderNumeric> {
  private final int maxBitlength;
  private final DRes<SInt> clientVal;
  private final DRes<SInt> lower;
  private final DRes<SInt> upper;

  public Range(DRes<SInt> lower, DRes<SInt> upper, DRes<SInt> clientVal, int maxBitLength) {
    this.clientVal = clientVal;
    this.lower = lower;
    this.upper = upper;
    this.maxBitlength = maxBitLength;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par(par -> {
      DRes<SInt> first = Comparison.using(par).compareLT(lower, clientVal);
      DRes<SInt> second = Comparison.using(par).compareLT(clientVal, upper);
      return () -> Arrays.asList(first.out(), second.out());
    }).par( (par, comparisons) -> {
      return par.numeric().mult(comparisons.get(0), comparisons.get(1));
    });
  }
}
