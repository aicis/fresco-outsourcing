import dk.alexandra.fresco.outsourcing.demo.ThresholdRSAGenerator;
import dk.alexandra.fresco.outsourcing.demo.ThresholdRSAGenerator.RSAShares;
import java.util.List;
import org.junit.Test;

public class RSATest {
  @Test
  public void generationSunshine() throws Exception {
    ThresholdRSAGenerator gen = new ThresholdRSAGenerator(2, 3);
    List<RSAShares> shares = gen.generateShares(2048);
    shares.stream().forEach(cur -> System.out.println(cur.toString()));
  }

}
