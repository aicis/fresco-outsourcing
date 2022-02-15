import static dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy.SEQUENTIAL_BATCHED;

import dk.alexandra.fresco.suite.dummy.arithmetic.AbstractDummyArithmeticTest;
import org.junit.Test;

public class ApplicationTests extends AbstractDummyArithmeticTest {

  private static TestParameters params = new TestParameters().
      numParties(3).
      maxBitLength(64).
      evaluationStrategy(SEQUENTIAL_BATCHED).
      performanceLogging(true);


  @Test
  public void basicMacCom() {
    runTest(new AttTests.BasicMacTest<>(), params);
  }

  @Test
  public void basicMacValidation() {
    runTest(new AttTests.BasicMacValidation<>(), params);
  }

  @Test
  public void validateAttList() {
    runTest(new AttTests.validateAttList<>(), params);
  }

  @Test
  public void sameValueTest() {
    runTest(new CasesTests.SameValueTest<>(), params);
  }

  @Test
  public void interpolateTest() {
    runTest(new CasesTests.InterpolateTest<>(), params);
  }
}
