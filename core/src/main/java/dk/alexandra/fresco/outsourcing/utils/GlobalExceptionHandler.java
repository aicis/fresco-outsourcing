package dk.alexandra.fresco.outsourcing.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler  implements Thread.UncaughtExceptionHandler {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    logger.error("Thread \"" + thread.toString() + "\" threw an exception!", throwable);
  }
}

