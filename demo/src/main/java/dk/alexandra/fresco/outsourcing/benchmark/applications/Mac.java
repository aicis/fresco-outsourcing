package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;

public class Mac implements Computation<SInt, ProtocolBuilderNumeric> {
  private final DRes<SInt> x;
  private final DRes<SInt> delta;
  private final DRes<SInt> beta;

  public Mac(DRes<SInt> x, DRes<SInt> delta, DRes<SInt> beta) {
    this.x = x;
    this.delta = delta;
    this.beta = beta;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.seq((seq) -> {
      DRes<SInt> partial = seq.numeric().mult(x, delta);
      return seq.numeric().add(partial, beta);
    });
  }
}
