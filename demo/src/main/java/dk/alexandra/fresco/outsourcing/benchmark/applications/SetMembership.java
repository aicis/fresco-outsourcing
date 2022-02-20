package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import java.util.ArrayList;
import java.util.List;

public class SetMembership implements Computation<SInt, ProtocolBuilderNumeric> {

  private DRes<SInt> clientInput;
  private List<DRes<SInt>> set;

  public SetMembership(List<DRes<SInt>> set, DRes<SInt> clientInput) {
    this.clientInput = clientInput;
    this.set = set;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      List<DRes<SInt>> comparisons = new ArrayList<>();
      for (int i = 0; i < set.size(); i++) {
        comparisons.add(par.numeric().sub(set.get(i), clientInput));
      }
      DRes<SInt> res = AdvancedNumeric.using(par).product(comparisons);
      return () -> res;
    }).par( (par, res) -> {
      DRes<SInt> zeroChecked = Comparison.using(par).compareZero(res,
          builder.getBasicNumericContext().getMaxBitLength());
      return () -> zeroChecked.out();
    });
  }
}
