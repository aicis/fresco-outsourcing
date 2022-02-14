package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.outsourcing.benchmark.ClientPPP;
import dk.alexandra.fresco.outsourcing.benchmark.Hole;
import dk.alexandra.fresco.outsourcing.benchmark.ServerPPP;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SameValueServer extends ServerPPP {
  private Map<Integer, List<SInt>> clientsInputs;
  private static final int TOTAL_MACS = 2;
  private static final BigInteger DELTA_SHARE = BigInteger.valueOf(1);
  private static final List<BigInteger> BETA_SHARE = Arrays.asList( BigInteger.valueOf(1), BigInteger.valueOf(34));
//      IntStream.range(33, TOTAL_MACS+1).mapToObj(i -> BigInteger.valueOf(i)).collect(Collectors.toList());

  public SameValueServer(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
    super(myId, serverIdIpMap, bitLength, basePort);
  }

  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort, Collections.singletonList(ClientPPP.CLIENT_ID), Collections.singletonList(ClientPPP.CLIENT_ID+1), serverIdIpMap, bitLength);
    clientsInputs = spdz.receiveInputs();
  }

  @Override
  public void run(Hole hole) {
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
//      return builder.seq(seq -> {
        Numeric input = builder.numeric();
        DRes<SInt> refValue = input.known(42); // Value to verify against
        DRes<SInt> uid = input.known(10);
        List<DRes<SInt>> atts = Arrays.asList(clientsInputs.get(1).get(0), uid);
        // First MAC is value MAC, second MAC is UID MAC
        List<DRes<SInt>> macs = Arrays.asList(clientsInputs.get(1).get(1), clientsInputs.get(1).get(2));
        int servers = serverIdIpMap.keySet().size();
        List<DRes<SInt>> deltaShares = new ArrayList<>();
        List<List<DRes<SInt>>> betaShares = new ArrayList<>();
        for (int i = 1; i <= servers; i++) {
          DRes<SInt> deltaShare, betaShare;
          deltaShare = input.input(i == myId ? DELTA_SHARE : null, i);
//          deltaShare = input.known(DELTA_SHARE);
          List<DRes<SInt>> internalBetaShares = new ArrayList<>();
          for (int j = 0; j < TOTAL_MACS; j++) {
            betaShare = input.input(i == myId ? BETA_SHARE.get(j) : null, i);
//            betaShare = input.known(BETA_SHARE.get(j));
            internalBetaShares.add(betaShare);
          }
          deltaShares.add(deltaShare);
          betaShares.add(internalBetaShares);
        }
        return builder.seq((seq) -> {
          DRes<Boolean> checkAtt = seq.seq(new CheckAtt(atts, macs, betaShares.get(0), deltaShares.get(0)));
          return seq.seq( (seq2) -> {
            if (checkAtt.out() != true) {
              throw new IllegalArgumentException("Invalid user MAC");
            }
            // TODO only works with half bitlength and requires at least 128 bits
            DRes<SInt> res = Comparison.using(seq2).equals(clientsInputs.get(1).get(0), refValue);
            return () ->  Collections.singletonList(res.out());
          });

        });
      };
//    };
    spdz.sendOutputsTo(ClientPPP.CLIENT_ID+1, spdz.run(app));
  }
}
