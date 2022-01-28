package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.outsourcing.benchmark.PPP.Params;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ServerPPP extends PPP {
  private static List<Integer> serverIdsWOMe;

  @Setup
  public void setupServer(Params param) {
    List<Integer> serverIds = IntStream.range(1, param.amount+1).boxed().collect(Collectors.toList());
    List<Integer> serverIdsWOMe = serverIds.stream().filter((current) -> MYID != current).collect(
          Collectors.toList());
  }

  @Benchmark
  public void serverExecute() {
    SpdzWithIO spdz = new SpdzWithIO(MYID, Collections.singletonList(CLIENT_ID), serverIdsWOMe);
    Map<Integer, List<SInt>> clientsInputs = spdz.receiveInputs();

    // Example MPC application
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
      List<DRes<SInt>> clientOneInputs = new ArrayList<>(clientsInputs.get(1));
      DRes<SInt> res = AdvancedNumeric.using(builder).sum(clientOneInputs);
      return () -> Collections.singletonList(res.out());
    };
    serverIdsWOMe.stream().forEach((id) -> spdz.sendOutputsTo(id, spdz.run(app)));
  }
}
