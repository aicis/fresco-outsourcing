package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.outsourcing.benchmark.applications.SetMembershipServer;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Code from https://developpaper.com/how-to-benchmark-using-jmh-in-java/
public class Main {

  //Generated file path: {project root} / {reportfiledir} / {XXX. Class. Getsimplename()}. JSON
  // e.g. jmh-reports/EmptyMethod.json
  private static final String reportFileDir = "jmh-reports/";

  /*
   * ============================== HOW TO RUN THIS TEST: ====================================
   *1. Modify class < integersumtests > targetclazz = integersumtests.class// Classes that need to run jmh tests
   *2. Run the main method in the IDE
   */

  // Arguments; c/s for client/ server, followed by id, then all IPs
  public static void main(String[] args) throws Exception {
    final String mode = args[0];
    final int myId = Integer.parseInt(args[1]);
    final int maxServers = args.length - 2;
    Map<Integer, String> serverIdIpMap = new HashMap<>();
    for (int id = 1; id <= maxServers; id++) {
      serverIdIpMap.put(id, args[id + 1]);
    }
    int bitLength = 64;
    Map<Integer, List<PPP>> amountOfServersToBenchmarks = setupBenchmark(myId, mode, serverIdIpMap,
        bitLength);
    List<String> results = runBenchmark(amountOfServersToBenchmarks);
    writeResults(reportFileDir + "/" + mode + "/" + myId, results);
  }

  private static Map<Integer, List<PPP>> setupBenchmark(int myId, String mode, Map<Integer, String> serverIdIpMap, int bitLength) {
    int basePort = PPP.BASE_PORT;
    Map<Integer, String> currentMap = serverIdIpMap;
    Map<Integer, List<PPP>> amountOfServersToBenchmarks = new HashMap<>();
//    while (currentMap.size() >= 2 && myId <= currentMap.size()) {
      List<PPP> currentList = new ArrayList<>();
      if (mode.equals("c")) {
//        currentList.add(new ClientPPP(currentMap,
//            // TODO mod fieldsize
//            Arrays.asList(BigInteger.ONE,
//                BigInteger.ONE.multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(101)),
//                ServerPPP.UID.multiply(ServerPPP.DELTA_SHARE)
//                    .add(BigInteger.valueOf(102))),
//            bitLength, basePort)); // 1 input and 2 MACs
//        basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);
//        currentList.add(new ClientPPP(currentMap,
//            // TODO mod fieldsize
//            Arrays.asList(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3),
//                BigInteger.valueOf(4),
//                BigInteger.ONE.multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(101)),
//                BigInteger.valueOf(2).multiply(ServerPPP.DELTA_SHARE)
//                    .add(BigInteger.valueOf(102)),
//                BigInteger.valueOf(3).multiply(ServerPPP.DELTA_SHARE)
//                    .add(BigInteger.valueOf(103)),
//                BigInteger.valueOf(4).multiply(ServerPPP.DELTA_SHARE)
//                    .add(BigInteger.valueOf(104)),
//                ServerPPP.UID.multiply(ServerPPP.DELTA_SHARE)
//                    .add(BigInteger.valueOf(105))),
//            bitLength, basePort)); // 1 input and 2 MACs
//        basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);
//        currentList.add(new ClientPPP(currentMap,
//            // TODO mod fieldsize
//            Arrays.asList(BigInteger.valueOf(42),
//                BigInteger.valueOf(42).multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(101)),
//                ServerPPP.UID.multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(102))),
//            bitLength, basePort)); // 1 input and 2 MACs
//        basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);
//        currentList.add(new ClientPPP(currentMap,
//            // TODO mod fieldsize
//            Arrays.asList(BigInteger.valueOf(420000),
//                BigInteger.valueOf(420000).multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(101)),
//                ServerPPP.UID.multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(102))),
//            bitLength, basePort)); // 1 input and 2 MACs
//        basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);
        currentList.add(new ClientPPP(currentMap,
            // TODO mod fieldsize
            Arrays.asList(BigInteger.valueOf(42),
                BigInteger.valueOf(42).multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(101)),
                ServerPPP.UID.multiply(ServerPPP.DELTA_SHARE).add(BigInteger.valueOf(102))),
            bitLength, basePort)); // 1 input and 2 MACs
      } else if (mode.equals("s")) {
//        currentList.add(new MascotServer(myId, currentMap, bitLength, basePort));
//        basePort += currentMap.size()*(Benchmark.WARMUP+Benchmark.ITERATIONS);

//        currentList.add(new SameObjectServer(myId, currentMap, bitLength, basePort, 1));
//        basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);
//        currentList.add(new SameObjectServer(myId, currentMap, bitLength, basePort, 4));
//        basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);
//        currentList.add(new RangeServer(myId, currentMap, bitLength, basePort, BigInteger.valueOf(18), BigInteger.valueOf(60), 7));
//        basePort += currentMap.size()* 3 * (Benchmark.WARMUP+Benchmark.ITERATIONS);
//        currentList.add(new RangeServer(myId, currentMap, bitLength, basePort, BigInteger.valueOf(18), BigInteger.valueOf(1000000), bitLength));
//        basePort += currentMap.size()* 3 * (Benchmark.WARMUP+Benchmark.ITERATIONS);
        currentList.add(new SetMembershipServer(myId, currentMap, bitLength, basePort, 200));
      } else {
        throw new IllegalArgumentException();
      }
    amountOfServersToBenchmarks.put(currentMap.size(), currentList);
    // Make a new map, excluding the last server
    currentMap = new HashMap<>(currentMap);
//      currentMap.remove(currentMap.size());
//    }
    return amountOfServersToBenchmarks;
  }

  private static List<String> runBenchmark(Map<Integer, List<PPP>> amountOfServersToBenchmarks) {
    List<String> results = new ArrayList<>();
    List<Integer> sortedAmountsList = amountOfServersToBenchmarks.keySet().stream().sorted().collect(Collectors.toList());
    Collections.reverse(sortedAmountsList);
    for (int servers: sortedAmountsList) {
      results.add("Testing " + servers + " servers");
      for (PPP currentBenchmark : amountOfServersToBenchmarks.get(servers)) {
        System.out.println("Starting new bench...");
        results.add(Benchmark.parseTimes(currentBenchmark.getClass().getName(),
            Benchmark.runBenchmark(currentBenchmark)));
      }
    }
    return results;
  }

  private static void writeResults(String directory, List<String> toWrite) throws IOException {
    if (!Files.exists(Paths.get(directory))) {
      Files.createDirectories(Paths.get(directory));
    }
    Path filePath = Paths.get(directory + "/benchmark.csv");
    BufferedWriter buffer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
    for (String currentLine : toWrite) {
      buffer.write(currentLine + "\n");
    }
    buffer.close();
  }


//  /**
//   *A standard configuration, which configures parameters such as preheating and iteration according to actual needs
//   *
//   *@ param targetclazz class to run jmh test
//   * @throws RunnerException See:{@link RunnerException}
//   */
//  private static String setupStandardOptions(Class<?> targetClazz) throws RunnerException {
//    String reportFilePath = resolvePath(targetClazz);
//    ChainedOptionsBuilder optionsBuilder =
//        new OptionsBuilder()
//            . include(targetClazz.getSimpleName())
//            . mode (Mode.AverageTime) // mode - throughput annotation method @ benchmarkmode
//            . forks (1) // number of forks @ fork
//            . warmupIterations (WARMUP) // number of preheating rounds ｜ annotation method @ warmup
//            . measurementIterations (ITERATIONS) // measurement rounds ｜ annotation method @ measurement
//            . timeUnit (TimeUnit.MILLISECONDS) // the time unit used in the result | annotation method @ outputtimeunit
//            . shouldFailOnError(true)
////            . jvmArgs("Xms512m", "Xmx512m", "Xnoclassgc", "Xint")
//            . result (reportFilePath) // output path of the result report file
//            . resultFormat(ResultFormatType.JSON);// Result report file output format JSON
//    new Runner(optionsBuilder.build()).run();
//    return reportFilePath;
//  }
//
//  private static String resolvePath(Class<?> targetClazz) {
//    return reportFileDir + targetClazz.getSimpleName() + ".json";
//  }
}
