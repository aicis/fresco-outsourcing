package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.outsourcing.client.ClientBase;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Forms base for {@link DemoDdnntInputClient} and {@link DemoDdnntOutputClient}.
 */
public abstract class DemoDdnntClientBase extends ClientBase {

  /**
   * Creates new {@link ClientBase}.
   *
   * @param clientId client ID
   * @param servers servers to connect to
   */
  DemoDdnntClientBase(int clientId, List<Party> servers) {
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
    BigInteger actualProdConverted = definition.convertToUnsigned(actualProd);
    BigInteger expected = definition.convertToUnsigned(c);
    return actualProdConverted.equals(expected);
  }

}
