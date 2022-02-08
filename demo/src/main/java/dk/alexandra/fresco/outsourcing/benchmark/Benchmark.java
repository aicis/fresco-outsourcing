package dk.alexandra.fresco.outsourcing.benchmark;

import java.util.ArrayList;
import java.util.List;

public class Benchmark {
  private static final int ITERATIONS = 50;
  private static final int WARMUP = 30;

  public static List<Long> runBenchmark(Benchmarkable toRun) {
    List<Long> times = new ArrayList<>(ITERATIONS);
    System.out.println("Running setup");
    toRun.setup();
    for (int i = 0; i < ITERATIONS + WARMUP; i++) {
      System.out.println("Starting iteration " + i);
      toRun.beforeEach();
      System.out.println("Starting benchmark " + i);
      long startTime = System.currentTimeMillis();
      toRun.run(Hole.getInstance());
      long endTime = System.currentTimeMillis();
      if (i >= WARMUP) {
        times.add(endTime - startTime);
      }
      System.out.println("Closing connection");
      toRun.afterEach();
    }
    return times;
  }

  public static String parseTimes(String name, List<Long> times) {
    return name + ", " + mean(times) + ", " + std(times) + "\n";
  }

  private static double mean(List<Long> times) {
    return ((double) times.stream().mapToInt(t -> t.intValue()).sum())/ITERATIONS;
  }

  private static double std(List<Long> times ) {
    double mean = mean(times);
    double temp = 0.0;
    for (long current : times) {
      temp += (((double)current) - mean)*(((double)current) - mean);
    }
    return Math.sqrt(temp/((double)(ITERATIONS - 1)));
  }
}




