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

public class ServerPPP extends PPP {
  private static List<Integer> serverIdsWOMe;
  private final int myId;
  private int currentBasePort = BASE_PORT;
  private SpdzWithIO spdz;

  public ServerPPP(int myId, int maxServers,  Map<Integer, String> serverIdIpMap) {
    super(maxServers, serverIdIpMap);
    this.myId = myId;
  }

  @Override
  public void setup() {
    List<Integer> serverIds = IntStream.range(1, Params.amount+1).boxed().collect(Collectors.toList());
    serverIdsWOMe = serverIds.stream().filter((current) -> myId != current).collect(
          Collectors.toList());
  }


  @Override
  public void run(Hole hole) {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort, Collections.singletonList(ClientPPP.CLIENT_ID), Collections.singletonList(ClientPPP.CLIENT_ID+1));
    System.out.println("Setup spdz port " + currentBasePort);
    Map<Integer, List<SInt>> clientsInputs = spdz.receiveInputs();
    System.out.println("received input");
    // Example MPC application
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
      List<DRes<SInt>> clientOneInputs = new ArrayList<>(clientsInputs.get(1));
      DRes<SInt> res = AdvancedNumeric.using(builder).sum(clientOneInputs);
      return () -> Collections.singletonList(res.out());
    };
    serverIdsWOMe.stream().forEach((id) -> spdz.sendOutputsTo(id, spdz.run(app)));
  }

  @Override
  public void afterEach() {
    // Move base ports up
    currentBasePort += maxServers;
    spdz.shutdown();
  }
}
