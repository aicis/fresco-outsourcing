package dk.alexandra.fresco.outsourcing.demo;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Demo {

  private static final Logger logger = LoggerFactory.getLogger(Demo.class);

  private static void runAsClient(int clientId, List<Party> servers) {
    final List<Integer> inputs = Arrays.asList(1, 2, 3, 4, 5);
    InputClient client = new DemoDdnntInputClient(inputs.size(), clientId, servers);
    client.putIntInputs(inputs);
  }

  private static void runAsServer(int serverId) {
    SpdzServer spdz = new SpdzServer(serverId);

    Map<Integer, List<SInt>> clientsInputs = spdz.receiveInputsFrom(Collections.singletonList(1));

    // Example MPC application
    Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
      List<DRes<SInt>> clientOneInputs = new ArrayList<>(clientsInputs.get(1));
      DRes<SInt> res = builder.advancedNumeric().sum(clientOneInputs);
      return builder.numeric().open(res);
    };

    BigInteger res = spdz.run(app);
    logger.info("Sum of inputs is " + res);
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
