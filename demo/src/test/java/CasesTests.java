import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.benchmark.applications.SameValue;
import java.math.BigInteger;
import org.junit.Assert;

public class CasesTests {
  public static class SameValueTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x = input.input(BigInteger.valueOf(42), 1);
            DRes<BigInteger> res = builder.seq(new SameValue(x));
            return () -> res.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(BigInteger.ONE, output);
        }
      };
    }
  }
}
