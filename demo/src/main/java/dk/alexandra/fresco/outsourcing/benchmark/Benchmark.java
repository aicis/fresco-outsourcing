package dk.alexandra.fresco.outsourcing.benchmark;

import java.util.ArrayList;
import java.util.List;

public class Benchmark {
  public static final int ITERATIONS = 20;
  public static final int WARMUP = 10;

  public static List<Long> runBenchmark(Benchmarkable toRun) {
    try {
      List<Long> times = new ArrayList<>(ITERATIONS);
      toRun.setup();
      for (int i = 0; i < ITERATIONS + WARMUP; i++) {
        toRun.beforeEach();
        Thread.sleep(1);
        long startTime = System.currentTimeMillis();
        toRun.run(Hole.getInstance());
        long endTime = System.currentTimeMillis();
        if (i >= WARMUP) {
          times.add(endTime - startTime);
        }
        toRun.afterEach();
      }
      return times;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
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




