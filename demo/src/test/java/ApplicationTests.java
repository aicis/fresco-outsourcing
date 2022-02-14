import dk.alexandra.fresco.suite.dummy.arithmetic.AbstractDummyArithmeticTest;
import org.junit.Test;

public class ApplicationTests extends AbstractDummyArithmeticTest {

  @Test
  public void basicMacCom() {
    runTest(new AttTests.BasicMacTest<>(), new TestParameters());
  }

  @Test
  public void basicMacValidation() {
    runTest(new AttTests.BasicMacValidation<>(), new TestParameters());
  }

  @Test
  public void validateAttList() {
    runTest(new AttTests.validateAttList<>(), new TestParameters());
  }

  @Test
  public void sameValueTest() {
    runTest(new CasesTests.SameValueTest<>(), new TestParameters());
  }
}
