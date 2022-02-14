package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;

public class MacCheck implements Computation<Boolean, ProtocolBuilderNumeric> {
  private final DRes<SInt> mac;
  private final DRes<SInt> x;
  private final DRes<SInt> delta;
  private final DRes<SInt> beta;

  public MacCheck(DRes<SInt> mac, DRes<SInt> x, DRes<SInt> delta, DRes<SInt> beta) {
    this.mac = mac;
    this.x = x;
    this.delta = delta;
    this.beta = beta;
  }

  @Override
  public DRes<Boolean> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.seq((seq) -> {
          return seq.seq(new Mac(x, delta, beta));
        }).seq( (seq, res) -> {
          DRes<SInt> subRes = seq.numeric().sub(res, mac);
          DRes<SInt> randomizedRes = seq.numeric().mult(subRes, seq.numeric().randomElement());
          return seq.numeric().open(randomizedRes);
        }).seq( (seq, res) -> {
          if (res.equals(BigInteger.ZERO)) {
            return () -> true;
          } else {
            return () -> false;
          }
    });
  }
}
