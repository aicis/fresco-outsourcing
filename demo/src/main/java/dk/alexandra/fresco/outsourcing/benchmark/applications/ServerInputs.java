package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.ComputationParallel;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    return builder.seq((seq) -> {
      Numeric input = seq.numeric();
      List<DRes<SInt>> deltaShares = new ArrayList<>();
      DRes<SInt>[][] betaShares = new DRes[amountOfAttr][amountOfServers];
      for (int i = 1; i <= amountOfServers; i++) {
        DRes<SInt> deltaShare, betaShare;
        deltaShare = input.input(i == myId ? myDelta : null, i);
        for (int j = 0; j < amountOfAttr; j++) {
          betaShare = input.input(i == myId ? myBetas.get(j) : null, i);
          betaShares[j][i - 1] = betaShare;
        }
        deltaShares.add(deltaShare);
      }
      return () -> new Pair<List<DRes<SInt>>, DRes<SInt>[][]>(deltaShares, betaShares);
    }).par((par, shares) -> {
      DRes<SInt> delta = par.seq(new Interpolate(shares.getFirst()));
      List<DRes<SInt>> betas = new ArrayList<>();
      for (int i = 0; i < shares.getSecond().length; i++) {
        betas.add(par.seq(new Interpolate(
            Arrays.stream(shares.getSecond()[i]).collect(Collectors.toList()))));
      }
      return () -> new ServerInputModel(delta, betas);
    });
  }
}