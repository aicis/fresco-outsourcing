package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.CloseableNetwork;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;

public class ServerInputSessionImpl<ResourcePoolT extends NumericResourcePool>
    implements ServerInputSession<ResourcePoolT> {

  private final CloseableNetwork network;
  private final ResourcePoolT resourcePool;
  private final SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> sce;

  public ServerInputSessionImpl(CloseableNetwork network, ResourcePoolT resourcePool,
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
