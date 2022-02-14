import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.benchmark.applications.CheckAtt;
import dk.alexandra.fresco.outsourcing.benchmark.applications.Mac;
import dk.alexandra.fresco.outsourcing.benchmark.applications.MacCheck;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;

public class AttTests {
  public static class BasicMacTest<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x = input.input(BigInteger.valueOf(3), 1);
            DRes<SInt> delta = input.input(BigInteger.valueOf(5), 1);
            DRes<SInt> beta = input.input(BigInteger.valueOf(42), 1);
            DRes<SInt> res = builder.seq(new Mac(x, delta, beta));
            DRes<BigInteger> openRes = builder.numeric().open(res);
            return () -> openRes.out();
          };
          BigInteger output = runApplication(app);
          Assert.assertEquals(new BigInteger("57"), output);
        }
      };
    }
  }

  public static class BasicMacValidation<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<Pair<Boolean, Boolean>, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> x = input.input(BigInteger.valueOf(3), 1);
            DRes<SInt> delta = input.input(BigInteger.valueOf(5), 1);
            DRes<SInt> beta = input.input(BigInteger.valueOf(42), 1);
            DRes<SInt> mac = input.input(BigInteger.valueOf(57), 1);
            DRes<SInt> wrongMac = input.input(BigInteger.valueOf(0), 1);
            DRes<Boolean> correctRes = builder.seq(new MacCheck(mac, x, delta, beta));
            DRes<Boolean> wrongRes = builder.seq(new MacCheck(wrongMac, x, delta, beta));
            return () -> new Pair<>(correctRes.out(), wrongRes.out());
          };
          Pair<Boolean, Boolean> output = runApplication(app);
          Assert.assertTrue(output.getFirst());
          Assert.assertFalse(output.getSecond());
        }
      };
    }
  }

  public static class validateAttList<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {

        @Override
        public void test() throws Exception {
          Application<List<Boolean>, ProtocolBuilderNumeric> app = builder -> {
            Numeric input = builder.numeric();
            DRes<SInt> delta = input.input(BigInteger.valueOf(42), 1);
            DRes<SInt> att1 = input.input(BigInteger.valueOf(1337), 1);
            DRes<SInt> att2 = input.input(BigInteger.valueOf(666), 1);
            DRes<SInt> beta1 = input.input(BigInteger.valueOf(123), 1);
            DRes<SInt> beta2 = input.input(BigInteger.valueOf(456), 1);
            DRes<SInt> mac1 = input.input(BigInteger.valueOf(56277), 1);
            DRes<SInt> mac2 = input.input(BigInteger.valueOf(28428), 1);
            DRes<SInt> wrongMac = input.input(BigInteger.valueOf(789), 1);
            DRes<SInt> wrongAtt = input.input(BigInteger.valueOf(1000), 1);
            List<DRes<SInt>> atts = Arrays.asList(att1, att2);
            List<DRes<SInt>> betas = Arrays.asList(beta1, beta2);
            List<DRes<SInt>> macs = Arrays.asList(mac1, mac2);
            DRes<Boolean> correctRes = builder.seq(new CheckAtt(atts, macs, betas, delta));
            List<DRes<SInt>> wrongMacs = Arrays.asList(mac1, wrongMac);
            DRes<Boolean> wrongRes1 = builder.seq(new CheckAtt(atts, wrongMacs, betas, delta));
            List<DRes<SInt>> wrongAtts = Arrays.asList(wrongAtt, att2);
            DRes<Boolean> wrongRes2 = builder.seq(new CheckAtt(wrongAtts, macs, betas, delta));
            return () -> Arrays.asList(correctRes.out(), wrongRes1.out(), wrongRes2.out());
          };
          List<Boolean> output = runApplication(app);
          Assert.assertTrue(output.get(0));
          Assert.assertFalse(output.get(1));
          Assert.assertFalse(output.get(2));
        }
      };
    }
  }
}
