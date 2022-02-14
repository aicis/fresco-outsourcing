package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import java.math.BigInteger;

public class SameValue implements Computation<BigInteger, ProtocolBuilderNumeric> {
  private DRes<SInt> clientsInput;

  public SameValue(DRes<SInt> clientsInput) {
    this.clientsInput = clientsInput;
  }

  @Override
  public DRes<BigInteger> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      return par.numeric().known(42);
      }).par( (par, currentKnown) -> {
      // TODO only works with half bitlength and requires at least 128 bits
      return Comparison.using(par).equals(clientsInput, currentKnown);
    }).par( (par, res) -> {
      return par.numeric().open(res);
    });
  }
}