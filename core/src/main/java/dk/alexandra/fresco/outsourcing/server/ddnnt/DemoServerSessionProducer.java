package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.network.AsyncNetwork;
import dk.alexandra.fresco.framework.network.CloseableNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.SecureComputationEngineImpl;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedProtocolEvaluator;
import dk.alexandra.fresco.framework.sce.evaluator.BatchedStrategy;
import dk.alexandra.fresco.suite.spdz.SpdzProtocolSuite;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;

/**
 * A simple demo server session producer based on SPDZ.
 *
 * <p>
 * Note this implementation is very simple and mainly suitable for demos. For each call to the
 * <code>next()</code> method it will set up a new session by connecting a new network between the
 * servers and creating a new SCE. All sessions will be using the same resource pool though, so the
 * sessions should not run concurrently.
 * </p>
 */
public class DemoServerSessionProducer implements ServerSessionProducer<SpdzResourcePool> {

  public static final int DEFAULT_BATCH_SIZE = 10000;
  private final int batchSize;
  SpdzResourcePool resourcePool;
  NetworkConfiguration conf;

  /**
   * Creates a new server session producer with a given batch size, resource pool and network
   * configuration.
   *
   * <p>
   * Note, the resource pool will be reused for all sessions. I.e., running sessions in parallel
   * will not be safe.
   * </p>
   *
   * @param batchSize the batch size for the MPC evaluator
   * @param resourcePool the resource pool.
   * @param conf the configuration for the network connecting the servers.
   */
  public DemoServerSessionProducer(int batchSize, SpdzResourcePool resourcePool,
      NetworkConfiguration conf) {
    this.batchSize = batchSize;
    this.resourcePool = resourcePool;
    this.conf = conf;
  }

  /**
   * Convenience constructor for
   * {@link #DemoServerSessionProducer(int, SpdzResourcePool, NetworkConfiguration)} with the
   * batch size set to {@value #DEFAULT_BATCH_SIZE}.
   *
   * @param resourcePool the resource pool
   * @param conf the network configuration
   */
  public DemoServerSessionProducer(SpdzResourcePool resourcePool, NetworkConfiguration conf) {
    this(DEFAULT_BATCH_SIZE, resourcePool, conf);
  }

  @Override
  public ServerInputSession<SpdzResourcePool> next() {
    CloseableNetwork net = new AsyncNetwork(conf);
    SpdzProtocolSuite suite = new SpdzProtocolSuite(resourcePool.getModulus().bitLength());
    SecureComputationEngine<SpdzResourcePool, ProtocolBuilderNumeric> sce =
        new SecureComputationEngineImpl<>(suite,
            new BatchedProtocolEvaluator<>(new BatchedStrategy<>(), suite, batchSize));
    return new ServerInputSessionImpl<>(net, resourcePool, sce);
  }

}
