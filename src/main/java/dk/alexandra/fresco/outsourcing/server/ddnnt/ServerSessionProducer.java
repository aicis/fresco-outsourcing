package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;

public interface ServerSessionProducer<ResourcePoolT extends NumericResourcePool> {

  ServerInputSession<ResourcePoolT> next();

}
