package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.compare.Comparison;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.outsourcing.benchmark.ClientPPP;
import dk.alexandra.fresco.outsourcing.benchmark.Hole;
import dk.alexandra.fresco.outsourcing.benchmark.ServerPPP;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SetMembership extends ServerPPP {
  private final int amount;
  private Map<Integer, List<SInt>> clientsInputs;

  public SetMembership(int amount, int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
    super(myId, serverIdIpMap, bitLength, basePort);
    this.amount = amount;
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
        for (int i = 0; i < amount; i++) {
          comparisons.add(par.numeric().sub(clientsInputs.get(ClientPPP.CLIENT_ID).get(0), i));
        }
        DRes<SInt> res = AdvancedNumeric.using(par).product(comparisons);
        return () -> res;
      }).par( (par, res) -> {
        // TODO should be full domain comparison
        DRes<SInt> zeroChecked = Comparison.using(par).compareZero(res, bitLength);
        return () -> Collections.singletonList(zeroChecked.out());
      });
    };
    spdz.sendOutputsTo(ClientPPP.CLIENT_ID+1, spdz.run(app));
  }
}
