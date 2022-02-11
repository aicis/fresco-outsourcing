package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.util.Objects;

/**
 * A SPDZ instantiation of the DDNNT tuple which simply wraps a {@link SpdzTriple}.
 */
public class SpdzDdnntTuple implements DdnntInputTuple {

  private final FieldElement shareA;
  private final FieldElement shareB;
  private final FieldElement shareC;
  private final SInt sintA;

  /**
   * Constructs DDNNT tuple from a SPDZ triple in the natrual way.
   *
   * @param triple a SPDZ triple
   */
  public SpdzDdnntTuple(SpdzTriple triple, FieldDefinition fieldDefinition) {
    Objects.requireNonNull(triple);
    this.sintA = Objects.requireNonNull(triple.getA());
    this.shareA = fieldDefinition.createElement(Objects.requireNonNull(triple.getA().getShare()));
    this.shareB = fieldDefinition.createElement(Objects.requireNonNull(triple.getB().getShare()));
    this.shareC = fieldDefinition.createElement(Objects.requireNonNull(triple.getC().getShare()));
  }

  @Override
  public FieldElement getShareA() {
    return shareA;
  }

  @Override
  public FieldElement getShareB() {
    return shareB;
  }

  @Override
  public FieldElement getShareC() {
    return shareC;
  }

  @Override
  public SInt getA() {
    return sintA;
  }

}
