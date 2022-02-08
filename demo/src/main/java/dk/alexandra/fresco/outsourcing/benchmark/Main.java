package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.outsourcing.benchmark.PPP.Params;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

// Code from https://developpaper.com/how-to-benchmark-using-jmh-in-java/
public class Main {
  public static final int WARMUP = 5;
  public static final int ITERATIONS = 10;

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
    final int maxServers = args.length-2;
    final Map<Integer, String> serverIdIpMap = new HashMap<>();
    for (int id = 1; id <= maxServers; id++) {
      serverIdIpMap.put(id, args[id+1]);
    }
    PPP entity;
    if (mode.equals("c")) {
      entity = new ClientPPP(maxServers, serverIdIpMap);
      Params.amount = 2;
      Params.inputs = 1;
    } else if (mode.equals("s")) {
      entity = new ServerPPP(myId, maxServers, serverIdIpMap);
      Params.amount = 2;
      Params.inputs = 1;
    } else {
      throw new IllegalArgumentException();
    }

    if (!Files.exists(Paths.get(reportFileDir))) {
      Files.createDirectories(Paths.get(reportFileDir));
    }
    System.out.println(Benchmark.parseTimes("Test", Benchmark.runBenchmark(entity)));
  }


  /**
   *A standard configuration, which configures parameters such as preheating and iteration according to actual needs
   *
   *@ param targetclazz class to run jmh test
   * @throws RunnerException See:{@link RunnerException}
   */
  private static String setupStandardOptions(Class<?> targetClazz) throws RunnerException {
    String reportFilePath = resolvePath(targetClazz);
    ChainedOptionsBuilder optionsBuilder =
        new OptionsBuilder()
            . include(targetClazz.getSimpleName())
            . mode (Mode.AverageTime) // mode - throughput annotation method @ benchmarkmode
            . forks (1) // number of forks @ fork
            . warmupIterations (WARMUP) // number of preheating rounds ｜ annotation method @ warmup
            . measurementIterations (ITERATIONS) // measurement rounds ｜ annotation method @ measurement
            . timeUnit (TimeUnit.MILLISECONDS) // the time unit used in the result | annotation method @ outputtimeunit
            . shouldFailOnError(true)
//            . jvmArgs("Xms512m", "Xmx512m", "Xnoclassgc", "Xint")
            . result (reportFilePath) // output path of the result report file
            . resultFormat(ResultFormatType.JSON);// Result report file output format JSON
    new Runner(optionsBuilder.build()).run();
    return reportFilePath;
  }

  private static String resolvePath(Class<?> targetClazz) {
    return reportFileDir + targetClazz.getSimpleName() + ".json";
  }
}
