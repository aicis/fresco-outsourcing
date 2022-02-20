package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SetMembershipServer extends ServerPPP {
  private Map<Integer, List<SInt>> clientsInputs;
  public final Set<BigInteger> set;
  public final List<BigInteger> BETA_SHARE;

  public SetMembershipServer(int myId, Map<Integer, String> serverIdIpMap, int bitLength,
      int basePort, int setSize) {
    super(myId, serverIdIpMap, bitLength, basePort);
    this.set = IntStream.range(1, setSize + 1).mapToObj(i -> BigInteger.valueOf(i))
        .collect(Collectors.toSet());
    this.BETA_SHARE = IntStream.range(1, setSize + 2)
        .mapToObj(i -> BigInteger.valueOf(100 + i)).collect(
            Collectors.toList());
  }

  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort, Collections.singletonList(ClientPPP.CLIENT_ID), Collections.singletonList(ClientPPP.CLIENT_ID+1), serverIdIpMap, bitLength);
    clientsInputs = spdz.receiveInputs();
  }

  @Override
  public void run(Hole hole) {
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
      Numeric input = builder.numeric();
      Set<DRes<SInt>> hiddenSet = set.stream().map(i->input.known(i)).collect(Collectors.toSet());
      DRes<SInt> uid = input.known(UID);
      List<DRes<SInt>> attributes = Arrays.asList(clientsInputs.get(ClientPPP.CLIENT_ID).get(0), uid);
      // MACs are stored in the list after attributes
      // First MAC is value MAC, second MAC is UID MAC
      List<DRes<SInt>> macs = Arrays.asList(
          clientsInputs.get(ClientPPP.CLIENT_ID).get(1),
          clientsInputs.get(ClientPPP.CLIENT_ID).get(2));
      int servers = serverIdIpMap.keySet().size();
      DRes<ServerInputModel> serverInput = builder.par(
          new ServerInputs(myId, DELTA_SHARE, BETA_SHARE, servers));
      return builder.seq((seq) -> {
        DRes<Boolean> checkAtt = seq.seq(
            new CheckAtt(attributes, macs, serverInput.out().getBetas(), serverInput.out()
                .getDelta()));
        DRes<SInt> comparisonRes = seq.seq(
            new SetMembership(hiddenSet.stream().collect(Collectors.toList()), attributes.get(0)));
        return seq.seq((seq2) -> {
          if (checkAtt.out() != true) {
            throw new IllegalArgumentException("Invalid user MAC");
          }
          return () -> Collections.singletonList(comparisonRes.out());
        });
      });
    };
    spdz.sendOutputsTo(ClientPPP.CLIENT_ID+1, spdz.run(app));
  }
}
