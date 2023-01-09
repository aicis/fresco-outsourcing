package dk.alexandra.fresco.outsourcing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtil {
  public static final int TIMEOUT_IN_SEC = 500;
  private static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

  public static <T> T blockingEvaluation(Callable<T> callable) {
    try {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<T> result = executor.submit(() -> {
        return callable.call();
      });
      executor.shutdown();
      if (!executor.awaitTermination(TIMEOUT_IN_SEC, TimeUnit.SECONDS)) {
        logger.error("Callable task did not complete successfully!");
        throw new RuntimeException("Callable task did not complete successfully!");
      }
      return result.get();
    } catch (Exception e) {
      logger.error("Callable task threw an exception", e);
      throw new RuntimeException("Callable task threw an exception", e);
    }
  }

  public static <T> Future<T> nonblockingEvaluation(Callable<T> callable) {
    try {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<T> result = executor.submit(() -> {
        return callable.call();
      });
      executor.shutdown();
      return result;
    } catch (Exception e) {
      logger.error("Callable task threw an exception", e);
      throw new RuntimeException("Callable task threw an exception", e);
    }
  }

}
