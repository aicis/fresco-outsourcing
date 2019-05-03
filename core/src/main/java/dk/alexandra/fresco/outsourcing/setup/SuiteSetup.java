package dk.alexandra.fresco.outsourcing.setup;

import dk.alexandra.fresco.framework.builder.ProtocolBuilder;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;

/**
 * Interface for conveniently handling MPC suite setups that holds all the elements needed to run an
 * MPC application.
 *
 * @param <ResourcePoolT> the resource pool type to use in the MPC computation
 * @param <BuilderT> the builder type used
 */
public interface SuiteSetup<ResourcePoolT extends ResourcePool, BuilderT extends ProtocolBuilder> {

  /**
   * Returns a network configuration used in MPC computations.
   *
   * @return a network
   */
  NetworkConfiguration getNetConf();

  /**
   * Returns a resource pool to be used in MPC computations.
   *
   * @return a resource pool
   */
  ResourcePoolT getRp();

  /**
   * A SecureComputationEngine which can run an MPC computation.
   *
   * @return an sce
   */
  SecureComputationEngine<ResourcePoolT, BuilderT> getSce();

}
