package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;

/**
 * Produces server sessions. I.e., connects the servers for a session processing clients input in
 * the DDNNT procotol.
 *
 * @param <ResourcePoolT> the resource pool.
 */
public interface ServerSessionProducer<ResourcePoolT extends NumericResourcePool> {

  ServerInputSession<ResourcePoolT> next();

}
