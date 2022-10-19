package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.benchmark.applications.Interpolate;
import dk.alexandra.fresco.outsourcing.benchmark.applications.Range;
import dk.alexandra.fresco.outsourcing.benchmark.applications.SameObject;
import dk.alexandra.fresco.outsourcing.benchmark.applications.SameValue;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
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
            DRes<SInt> comparisonRes = builder.seq(new SameValue(x, x));
            DRes<BigInteger> res = builder.numeric().open(comparisonRes);
            return () -> res.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(BigInteger.ONE, output);
        }
      };
    }
  }

  public static class SameObjectTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x1 = input.input(BigInteger.valueOf(42), 1);
            DRes<SInt> x2 = input.input(BigInteger.valueOf(43), 1);
            DRes<SInt> comparisonRes = builder.seq(
                new SameObject(Arrays.asList(x1, x2), Arrays.asList(x1, x2), 16));
            DRes<BigInteger> res = builder.numeric().open(comparisonRes);
            return () -> res.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(BigInteger.ONE, output);
        }
      };
    }
  }

  public static class RangeTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x = input.input(BigInteger.valueOf(42), 1);
            DRes<SInt> lower = input.input(BigInteger.valueOf(18), 1);
            DRes<SInt> upper = input.input(BigInteger.valueOf(60), 1);
            DRes<SInt> comparisonRes = builder.seq(
                new Range(lower, upper, x, 7));
            DRes<BigInteger> res = builder.numeric().open(comparisonRes);
            return () -> res.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(BigInteger.ONE, output);
        }
      };
    }
  }

  public static class LargeRangeTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x = input.input(BigInteger.valueOf(420000), 1);
            DRes<SInt> lower = input.input(BigInteger.valueOf(18), 1);
            DRes<SInt> upper = input.input(BigInteger.valueOf(600000), 1);
            DRes<SInt> comparisonRes = builder.seq(
                new Range(lower, upper, x, 20));
            DRes<BigInteger> res = builder.numeric().open(comparisonRes);
            return () -> res.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(BigInteger.ONE, output);
        }
      };
    }
  }

  public static class InterpolateTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x1 = input.known(1494);
            DRes<SInt> x2 = input.known(1942);
            DRes<SInt> x3 = input.known(2578);
            List<DRes<SInt>> list = Arrays.asList(x1, x2, x3);
            DRes<SInt> interpolateRes = builder.seq(new Interpolate(list));
            DRes<BigInteger> res = builder.numeric().open(interpolateRes);
            return () -> res.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(BigInteger.valueOf(1234), output);
        }
      };
    }
  }
}
