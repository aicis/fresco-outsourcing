package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import java.util.List;

/**
 * An output server to handle reconstructing a secret shared input towards given clients.
 */
public interface OutputServer<T> {

  /**
   * Hands a list of secret shared values to eventually be reconstructed towards a given client.
   *
   * @param clientId the id of the client to receive output
   * @param outputs the outputs to be reconstructed to the client
   */
  void putClientOutputs(int clientId, List<T> outputs);

  <ResourcePoolT extends NumericResourcePool> ServerSession<ResourcePoolT> getSession();



}
