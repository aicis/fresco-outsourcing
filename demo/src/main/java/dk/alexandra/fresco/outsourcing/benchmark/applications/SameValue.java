package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;

public class SameValue implements Computation<SInt, ProtocolBuilderNumeric> {

  private final DRes<SInt> clientsInput;
  private final DRes<SInt> referenceVal;

  public SameValue(DRes<SInt> referenceVal, DRes<SInt> clientsInput) {
    this.clientsInput = clientsInput;
    this.referenceVal = referenceVal;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      return Comparison.using(par).equals(clientsInput, referenceVal);
    });
  }
}