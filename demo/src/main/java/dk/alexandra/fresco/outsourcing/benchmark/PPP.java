package dk.alexandra.fresco.outsourcing.benchmark;

import java.util.Map;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

public class PPP {
  public static int MYID;
  public static final int CLIENT_ID = 1;
  public static final int BASE_PORT = 8042;
  public static int MAX_SERVER;
  public static Map<Integer, String> SERVERID_IP_MAP;

  //  // Define benchmarks parameters with @State
  @State(Scope.Benchmark)
  public static class Params {

    @Param({"2", "3"})
    public int amount;

    @Param({"5", "10"})
    public int inputs;
  }
}
