package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class CheckAtt implements Computation<Boolean, ProtocolBuilderNumeric> {
  private final List<DRes<SInt>> atts;
  private final List<DRes<SInt>> macs;
  private final List<DRes<SInt>> betas;
  private final DRes<SInt> delta;

  public CheckAtt(List<DRes<SInt>> atts, List<DRes<SInt>> macs, List<DRes<SInt>> betas, DRes<SInt> delta) {
    this.atts = atts;
    this.macs = macs;
    this.betas = betas;
    this.delta = delta;
  }

  @Override
  public DRes<Boolean> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      List<DRes<SInt>> macRes = new ArrayList<>();
      // Check MAC
      for (int i = 0; i < atts.size(); i++) {
        macRes.add(par.seq(new Mac(atts.get(i), delta, betas.get(i))));
      }
      return () -> macRes;
    }).par( (par, macRes) -> {
      List<DRes<SInt>> randProds = new ArrayList<>();
      for (int i = 0; i < atts.size(); i++) {
        DRes<SInt> subMac = par.numeric().sub(macs.get(i), macRes.get(i));
        randProds.add(par.numeric().mult(subMac, par.numeric().randomElement()));
      }
       return () -> randProds;
    }).par( (par, randProds) -> {
      List<DRes<BigInteger>> randMacs = new ArrayList<>();
      for (int i = 0; i < atts.size(); i++) {
        randMacs.add(par.numeric().open(randProds.get(i)));
      }
      return () -> randMacs;
    }).par( (par, randMacs) -> {
      for (DRes<BigInteger> currentRes : randMacs) {
        if (!currentRes.out().equals(BigInteger.ZERO)) {
          return ()-> false;
        }
      }
      return () -> true;
    });
  }
}
