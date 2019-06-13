package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;

/**
 * Defines a session between the servers where they process the input delivered by clients or output
 * delivered to clients.
 *
 * @param <ResourcePoolT> the resource pool used
 */
public interface ServerSession<ResourcePoolT extends NumericResourcePool> {

  /**
   * Gets a network connecting the servers.
   *
   * @return a network between the servers
   */
  Network getNetwork();

  /**
   * Gets the resource pool to use for processing.
   *
   * @return the resourcepool
   */
  ResourcePoolT getResourcePool();

  /**
   * Get the secure computation engine to use for the processing.
   *
   * @return the sce
   */
  SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> getSce();

}
