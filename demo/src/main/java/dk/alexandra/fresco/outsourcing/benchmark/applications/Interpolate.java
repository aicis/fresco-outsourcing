package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Interpolate implements Computation<SInt, ProtocolBuilderNumeric> {

  private List<DRes<SInt>> points;

  public Interpolate(List<DRes<SInt>> points) {
    this.points = points;
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      BigInteger modulus = builder.getBasicNumericContext().getFieldDefinition().getModulus();
      List<DRes<SInt>> terms = new ArrayList<>();
      for (int i = 1; i <= points.size(); i++) {
        BigInteger coef = BigInteger.ONE;
        for (int j = 1; j <= points.size(); j++) {
          if (i != j) {
            BigInteger denominator = BigInteger.valueOf(j).subtract(BigInteger.valueOf(i));
            BigInteger factor = BigInteger.valueOf(j)
                .multiply(denominator.modPow(modulus.subtract(BigInteger.valueOf(2)), modulus));
            coef = coef.multiply(factor);
          }
        }
        terms.add(par.numeric().mult(coef.mod(modulus), points.get(i - 1)));
      }
      return AdvancedNumeric.using(par).sum(terms);
    });
  }
}
