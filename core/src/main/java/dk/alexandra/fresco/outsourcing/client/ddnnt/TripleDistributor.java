package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntInputTuple;
import java.util.List;

/**
 * Distributes secret shared multiplication triples used for the DDNNT input protocol.
 *
 * <p>
 * The DDNNT protocol relies on the servers reconstructing towards the client secret shared
 * preprocessed random values <i>a, b</i> and <i>c</i> so that <i>ab=c</i>. For each such triple the
 * servers must also hold an SInt representing <i>a</i> in order to unmask the clients input in MPC.
 * </p>
 *
 * <p>
 * TripleDistributor is responsible for distributing - from some source - the triples used in a
 * single input session with a single client in such a way that all servers agree on which triples
 * will be served to which client input.
 * </p>
 */
public interface TripleDistributor {

  /**
   * Get a batch of triples along with an SInt representing the first element of the triple.
   *
   * @param amount the amount of triples in the batch
   * @return a batch of triples.
   */
  List<DdnntInputTuple> getTriples(int amount);

}
