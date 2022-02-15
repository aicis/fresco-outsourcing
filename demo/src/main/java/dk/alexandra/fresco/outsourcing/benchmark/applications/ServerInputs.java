package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.ComputationParallel;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ServerInputs implements ComputationParallel<ServerInputModel, ProtocolBuilderNumeric> {

  private final int myId;
  private final int amountOfAttr;
  private final int amountOfServers;

  private final BigInteger myDelta;
  private final List<BigInteger> myBetas;

  public ServerInputs(int myId, BigInteger myDelta, List<BigInteger> myBetas, int amountOfServers) {
    this.myId = myId;
    this.myDelta = myDelta;
    this.myBetas = myBetas;
    this.amountOfAttr = myBetas.size();
    this.amountOfServers = amountOfServers;
  }

  @Override
  public DRes<ServerInputModel> buildComputation(ProtocolBuilderNumeric builder) {
    Numeric input = builder.numeric();
    List<DRes<SInt>> deltaShares = new ArrayList<>();
    List<List<DRes<SInt>>> betaShares = new ArrayList<>();
    for (int i = 1; i <= amountOfServers; i++) {
      DRes<SInt> deltaShare, betaShare;
      deltaShare = input.input(i == myId ? myDelta : null, i);
      List<DRes<SInt>> internalBetaShares = new ArrayList<>();
      for (int j = 0; j < amountOfAttr; j++) {
        betaShare = input.input(i == myId ? myBetas.get(j) : null, i);
        internalBetaShares.add(betaShare);
      }
      deltaShares.add(deltaShare);
      betaShares.add(internalBetaShares);
    }
    // TODO interpolate
    return () -> new ServerInputModel(deltaShares.get(0), betaShares.get(0));
  }
}
