package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.CloseableNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;

/**
 * Represents a session between the servers where the servers process input delivered by a client
 * using the DDNNT protocol.
 *
 * @param <ResourcePoolT> the resource pool to use.
 */
public class ServerSessionImpl<ResourcePoolT extends NumericResourcePool>
    implements ServerSession<ResourcePoolT> {

  private final CloseableNetwork network;
  private final ResourcePoolT resourcePool;
  private final SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> sce;

  /**
   * Constructs a server session.
   *
   * @param network a network
   * @param resourcePool the resource pool
   * @param sce an SCE
   */
  public ServerSessionImpl(CloseableNetwork network, ResourcePoolT resourcePool,
      SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> sce) {
    this.network = network;
    this.resourcePool = resourcePool;
    this.sce = sce;
  }

  @Override
  public CloseableNetwork getNetwork() {
    return network;
  }

  @Override
  public ResourcePoolT getResourcePool() {
    return resourcePool;
  }

  @Override
  public SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> getSce() {
    return sce;
  }

}
