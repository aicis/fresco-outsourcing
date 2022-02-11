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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Age extends ServerPPP  {
  private Map<Integer, List<SInt>> clientsInputs;

  public Age(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
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
        DRes<SInt> lower = par.numeric().known(BigInteger.valueOf(18));
        DRes<SInt> upper = par.numeric().known(BigInteger.valueOf(60));
        // TODO no specific bitlength
        DRes<SInt> first = par.comparison().compareLEQ(lower, clientsInputs.get(ClientPPP.CLIENT_ID).get(0));
        DRes<SInt> second = par.comparison().compareLEQ(clientsInputs.get(ClientPPP.CLIENT_ID).get(0), upper);
        return () -> Arrays.asList(first.out(), second.out());
      }).par( (par, comparisons) -> {
        DRes<SInt> res = par.numeric().mult(comparisons.get(0), comparisons.get(1));
        return () ->  Collections.singletonList(res.out());
      });
    };
    spdz.sendOutputsTo(ClientPPP.CLIENT_ID+1, spdz.run(app));
  }
}
