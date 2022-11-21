package dk.alexandra.fresco.outsourcing;

import dk.alexandra.fresco.framework.util.ExceptionConverter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestUtil {
  public static final int TIMEOUT_IN_SEC = 5;
  public static <T> T lazyEvaluationWithTimeout(Callable<T> callable) {
    return ExceptionConverter.safe(() -> {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<T> result = executor.submit(() -> {
        return callable.call();
      });
      executor.shutdown();
      executor.awaitTermination(TIMEOUT_IN_SEC, TimeUnit.SECONDS);
      return result.get();
    }, "Could not complete lazy evaluation with timeout");
  }

}
