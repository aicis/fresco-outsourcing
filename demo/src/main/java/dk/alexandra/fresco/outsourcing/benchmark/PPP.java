package dk.alexandra.fresco.outsourcing.benchmark;

import java.util.Map;

public abstract class PPP implements Benchmarkable {
  public static final int BASE_PORT = 8042;
  public final int maxServers;
  public final Map<Integer, String> serverIdIpMap;

  public PPP(int maxServers,  Map<Integer, String> serverIdIpMap) {
    this.maxServers = maxServers;
    this.serverIdIpMap = serverIdIpMap;
  }

  @Override
  public void setup() {
    // nop
  }

  @Override
  public void afterEach() {
    // nop
  }

  @Override
  public void beforeEach() {
    // nop
  }


  public static class Params {
    public static int amount;

    public static int inputs;
  }
}
