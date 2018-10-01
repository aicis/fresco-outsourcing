package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.List;

/**
 * A simple implementation of the triple distributor, which is simply allocated some amount of
 * triples at construction, and will not be able to serve additional triples beyond this amount.
 */
class PreLoadedTripleDistributor implements TripleDistributor {

  private final List<Pair<SInt, Triple<BigInteger>>> preloaded;
  private int cursor = 0;

  /**
   * Constructs a new distributor serving triples from a given list.
   *
   * @param preloaded a list of preloaded triples.
   */
  public PreLoadedTripleDistributor(List<Pair<SInt, Triple<BigInteger>>> preloaded) {
    this.preloaded = preloaded;
  }

  /**
   * Will serve triples from directly from the list given at construction.
   *
   * @throws IndexOutOfBoundsException if more triples are requested than was preloaded in the
   *         constructor, or if a negative amount is requested.
   */
  @Override
  public List<Pair<SInt, Triple<BigInteger>>> getTriples(int amount) {
    this.cursor += amount;
    if (amount < 0 || this.cursor > preloaded.size()) {
      throw new IndexOutOfBoundsException("Can not get " + amount + " triples.");
    }
    return preloaded.subList(this.cursor - amount, this.cursor);
  }

}
