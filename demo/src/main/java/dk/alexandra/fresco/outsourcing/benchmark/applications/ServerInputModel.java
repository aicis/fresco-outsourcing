package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.value.SInt;
import java.util.List;

public class ServerInputModel {

  private final DRes<SInt> delta;
  private final List<DRes<SInt>> betas;

  public ServerInputModel(DRes<SInt> delta, List<DRes<SInt>> betas) {
    this.delta = delta;
    this.betas = betas;
  }

  public DRes<SInt> getDelta() {
    return delta;
  }

  public List<DRes<SInt>> getBetas() {
    return betas;
  }
}
