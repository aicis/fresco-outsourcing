package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.outsourcing.server.ddnnt.ServerInputSession;

public interface ServerSessionProducer<ResourcePoolT extends NumericResourcePool> {

  ServerInputSession<ResourcePoolT> next();

}
