package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.value.SInt;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface InputServer {

  /**
   * A future map from clientId's to the inputs provided to the servers.
   *
   * @return a map from client id's to provided inputs
   */
  Future<Map<Integer, List<SInt>>> getClientInputs();

}
