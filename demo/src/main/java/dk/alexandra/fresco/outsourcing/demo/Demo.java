package dk.alexandra.fresco.outsourcing.demo;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Demo {

  private static void runAsClient(int clientId, List<Party> servers) {
    final List<Integer> inputs = Arrays.asList(1, 2, 3, 4, 5);
    InputClient inputClient = new DemoDdnntInputClient(inputs.size(), clientId, servers);
    inputClient.putIntInputs(inputs);
    OutputClient outputClient = new DemoDdnntOutputClient(clientId + 1, servers);
    System.out.println("Outputs received " + outputClient.getBigIntegerOutputs());
  }

  private static void runAsServer(int serverId) {
    SpdzServer spdz = new SpdzServer(serverId, Collections.singletonList(1),
        Collections.singletonList(2));

    Map<Integer, List<SInt>> clientsInputs = spdz.receiveInputs();

    // Example MPC application
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
      List<DRes<SInt>> clientOneInputs = new ArrayList<>(clientsInputs.get(1));
      DRes<SInt> res = builder.advancedNumeric().sum(clientOneInputs);
      return () -> Collections.singletonList(res.out());
    };
    spdz.sendOutputsTo(2, spdz.run(app));
    // TODO shutdown servers
  }

  public static void main(String[] args) {
    // TODO proper command line parsing
    if (args.length != 2) {
      throw new IllegalArgumentException();
    }
    final String mode = args[0];
    final int id = Integer.parseInt(args[1]);

    if (mode.equals("c")) {
      // Make sure to update the port here if you change the port your SPDZ servers are running on
      runAsClient(id, SpdzSetupUtils.getServerParties(8042, 2));
    } else if (mode.equals("s")) {
      runAsServer(id);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
