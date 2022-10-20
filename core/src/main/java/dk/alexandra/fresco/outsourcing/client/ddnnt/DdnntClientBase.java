package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.outsourcing.client.AbstractClientBase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Forms base for {@link DdnntInputClient} and {@link DdnntOutputClient}.
 */
public abstract class DdnntClientBase extends AbstractClientBase {

  /**
   * Creates new {@link AbstractClientBase}.
   *
   * @param clientId client ID
   * @param servers  servers to connect to
   */
  DdnntClientBase(int clientId, List<Party> servers) {
    super(clientId, servers);
  }

  /**
   * Computes pairwise sum of left and right elements.
   */
  final List<FieldElement> sumLists(List<FieldElement> left, List<FieldElement> right) {
    if (left.size() != right.size()) {
      throw new IllegalArgumentException("Left and right should be same size");
    }
    List<FieldElement> res = new ArrayList<>(left.size());
    for (int i = 0; i < left.size(); i++) {
      FieldElement b = left.get(i).add(right.get(i));
      res.add(b);
    }
    return res;
  }

  /**
   * Returns true if a * b = c, false otherwise.
   */
  final boolean productCheck(FieldElement a, FieldElement b, FieldElement c) {
    FieldElement actualProd = a.multiply(b);
    BigInteger actualProdConverted = getDefinition().convertToUnsigned(actualProd);
    BigInteger expected = getDefinition().convertToUnsigned(c);
    return actualProdConverted.equals(expected);
  }

}
