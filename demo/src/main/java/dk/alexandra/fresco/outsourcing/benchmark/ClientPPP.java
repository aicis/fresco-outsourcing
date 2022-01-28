package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.demo.Demo;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class ClientPPP extends PPP {
  private static List<Integer> clientInputs;
  private static List<Party> servers;

  @Setup
  public void setupClient(Params param) {
    clientInputs = IntStream.range(0, param.inputs).boxed().collect(Collectors.toList());
    servers = getServers(param.amount);
  }

  private List<Party> getServers(int amount) {
    List<Party> servers = new ArrayList<>(SERVERID_IP_MAP.size());
    for (int id = 1; id <= amount; id++) {
      servers.add(new Party(id, SERVERID_IP_MAP.get(id), BASE_PORT + id));
    }
    return servers;
  }

  @Benchmark
  public void clientExecute() {
    InputClient client = new DemoDdnntInputClient(clientInputs.size(), CLIENT_ID, servers);
    client.putIntInputs(clientInputs);
    OutputClient outputClient = new DemoDdnntOutputClient(CLIENT_ID, servers);
    System.out.println("Outputs received " + outputClient.getBigIntegerOutputs());
  }

//  @Benchmark
//  @Fork(value = ITERATIONS, warmups = WARMUP)
//  @OutputTimeUnit(TimeUnit.MILLISECONDS)
//  @BenchmarkMode(Mode.AverageTime)
//  public static void demoWrapper(Params params) {
//    System.out.println("Running with parameters " + params.amount);
//    Runnable client = () -> {
//      main(new String[]{"c", "1"});
//    };
//    client.run();
//    for (int i = 1; i <= params.amount; i++) {
////    System.out.println("Running with parameters " + params.currentType + " and " + params.currentId);
//      final int id = i;
//      Runnable runner = () -> {
//        main(new String[]{"s", Integer.toString(id)});
//      };
//      runner.run();
//    }
//  }

//  // Define benchmarks parameters with @State
//  @State(Scope.Benchmark)
//  public static class Params {
//
//    @Param({"2", "3"})
//    public int amount;
//
//    @Param({"5", "10"})
//    public int inputs;
//    @Param({"c", "s", "s"})
//    public String type;
//
//    @Param({"1", "1", "2"})
//    public String id;
//
//    public String currentType;
//    public String currentId;
//
//    @Setup
//    public void setup () {
//      currentType = type;
//      currentId = id;
//    }
//  }
}
