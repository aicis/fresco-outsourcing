package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.jno.JnoInputClient;
import dk.alexandra.fresco.outsourcing.jno.PestoOutputClient;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ClientPPP extends PPP {

  public static final int CLIENT_ID = 1;
  private int currentBasePort;

  private List<Party> servers;
  private List<BigInteger> clientInputs;
  private OutputClient outputClient;
  private int amountOfServers;
  private final Drbg drbg;

  public ClientPPP(Map<Integer, String> serverIdIpMap, int inputs, int bitLength, int basePort) {
    this(serverIdIpMap,
        IntStream.range(42, 42 + inputs).mapToObj(i -> BigInteger.valueOf(i))
            .collect(Collectors.toList()),
        bitLength, basePort);
  }

  public ClientPPP(Map<Integer, String> serverIdIpMap, List<BigInteger> clientInputs, int bitLength,
      int basePort) {
    super(serverIdIpMap, bitLength);
    this.clientInputs = clientInputs;
    this.amountOfServers = serverIdIpMap.size();
    this.currentBasePort = basePort;
    // Static randomness for simplicity and debugging
    this.drbg = new AesCtrDrbg(new byte[32]);
  }

  private List<Party> getServers(int amount) {
    List<Party> servers = new ArrayList<>(serverIdIpMap.size());
    for (int id = 1; id <= amount; id++) {
      servers.add(new Party(id, serverIdIpMap.get(id), currentBasePort + id));
    }
    return servers;
  }

  @Override
  public void beforeEach() {
    servers = getServers(amountOfServers);
  }

  @Override
  public void run(Hole hole) {
    InputClient client = new JnoInputClient(clientInputs.size(), CLIENT_ID, servers,
        BigIntegerFieldDefinition::new, drbg);
    client.putBigIntegerInputs(clientInputs);
    outputClient = new PestoOutputClient(CLIENT_ID + 1, servers);
    List<Long> results = outputClient.getLongOutputs();
    for (long res : results) {
      // TODO output value
      if (0L != res) {
        throw new RuntimeException("Incorrect result");
      }
    }
  }

  @Override
  public void afterEach() {
    // Move base ports up
    currentBasePort += maxServers;
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
