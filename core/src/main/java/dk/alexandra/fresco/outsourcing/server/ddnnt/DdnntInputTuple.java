package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.value.SInt;

/**
 * An input tuple as used in the DDNNT protocol.
 *
 * <p>
 * Not this is similar but slightly different than a regular Beaver multiplication triple. It
 * contains secret shares of values <i>a, b</i> and <i>c = ab</i> and a representation of <i>a</i>
 * in the internal representation of the MPC protocol. I.e., the representation may be different
 * than the secret sharing of <i>a, b, c</i> in the tuple. E.g., in SPDZ the internal representation
 * of <i>a</i> would include its MAC apart from the secret share.
 * </p>
 */
public interface DdnntInputTuple {

  FieldElement getShareA();

  FieldElement getShareB();

  FieldElement getShareC();

  SInt getA();

}
