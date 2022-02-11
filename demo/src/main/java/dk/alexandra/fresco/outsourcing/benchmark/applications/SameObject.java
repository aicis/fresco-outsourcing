package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.benchmark.ClientPPP;
import dk.alexandra.fresco.outsourcing.benchmark.Hole;
import dk.alexandra.fresco.outsourcing.benchmark.ServerPPP;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SameObject extends ServerPPP {
  private Map<Integer, List<SInt>> clientsInputs;

  public SameObject(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
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
      return builder.par(par -> {
            List<DRes<SInt>> comparisons = new ArrayList<>();
            for (int i = 0; i < 256 / bitLength; i++) {
              DRes<SInt> currentKnown = par.numeric().known(BigInteger.valueOf(42+i));
              // TODO only works with half bitlength and requires at least 128 bits
              DRes<SInt> res = par.comparison().equals(clientsInputs.get(1).get(i), currentKnown, bitLength/2);
              comparisons.add(res);
            }
            return () -> comparisons;
        }).par( (par, comparisons) -> {
          DRes<SInt> res = par.advancedNumeric().product(comparisons);
          return () ->  Collections.singletonList(res.out());
      });
    };
    spdz.sendOutputsTo(ClientPPP.CLIENT_ID+1, spdz.run(app));
  }
}
