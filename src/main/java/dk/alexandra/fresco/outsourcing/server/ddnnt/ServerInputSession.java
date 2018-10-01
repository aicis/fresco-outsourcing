package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.sce.SecureComputationEngine;

public interface ServerInputSession<ResourcePoolT extends NumericResourcePool> {

  Network getNetwork();

  ResourcePoolT getResourcePool();

  SecureComputationEngine<ResourcePoolT, ProtocolBuilderNumeric> getSce();

}
