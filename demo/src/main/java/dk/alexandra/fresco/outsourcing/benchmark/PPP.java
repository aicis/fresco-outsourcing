package dk.alexandra.fresco.outsourcing.benchmark;

import java.util.Map;

public abstract class PPP implements Benchmarkable {
  public static final int BASE_PORT = 8042;
  public final int maxServers;
  public final Map<Integer, String> serverIdIpMap;
  public final int bitLength;

  public PPP(Map<Integer, String> serverIdIpMap, int bitLength) {
    this.maxServers = serverIdIpMap.size();
    this.serverIdIpMap = serverIdIpMap;
    this.bitLength = bitLength;
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
}
