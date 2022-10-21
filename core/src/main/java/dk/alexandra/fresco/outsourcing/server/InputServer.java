package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * An input server eventually supplying the inputs for some clients in a form that is ready for
 * further secure computation.
 */
public interface InputServer {

  /**
   * A future map from clientId's to the inputs provided to the servers.
   *
   * @return a map from client id's to provided inputs
   */
  Future<Map<Integer, List<SInt>>> getClientInputs();

  <ResourcePoolT extends NumericResourcePool> ServerSession<ResourcePoolT> getSession();

}
