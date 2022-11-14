package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import java.util.ArrayList;
import java.util.List;

public class SameObject implements Computation<SInt, ProtocolBuilderNumeric> {

  private final List<DRes<SInt>> clientsInputs;
  private final List<DRes<SInt>> referenceVals;
  private final int bitlength;

  public SameObject(List<DRes<SInt>> referenceVals, List<DRes<SInt>> clientsInputs, int bitlength) {
    this.clientsInputs = clientsInputs;
    this.referenceVals = referenceVals;
    this.bitlength = bitlength;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    if (clientsInputs.size() != referenceVals.size()) {
      throw new IllegalArgumentException("Reference list and input are not the same size");
    }
    return builder.par((par) -> {
      List<DRes<SInt>> comparisons = new ArrayList<>();
      for (int i = 0; i < referenceVals.size(); i++) {
        DRes<SInt> res = Comparison.using(par).equals(clientsInputs.get(i), referenceVals.get(i), bitlength);
        comparisons.add(res);
      }
      return () -> comparisons;
    }).par((par, comparisons) -> {
      return AdvancedNumeric.using(par).product(comparisons);
    });
  }
}
