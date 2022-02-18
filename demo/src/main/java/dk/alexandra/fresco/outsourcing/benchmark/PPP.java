package dk.alexandra.fresco.outsourcing.benchmark;

import java.util.Map;

public abstract class PPP implements Benchmarkable {
  public static final int BASE_PORT = 60000;
  public final int maxServers;
  public final Map<Integer, String> serverIdIpMap;
  public final int bitLength;

  public PPP(Map<Integer, String> serverIdIpMap, int bitLength) {
    this.maxServers = serverIdIpMap.size();
    this.serverIdIpMap = serverIdIpMap;
    this.bitLength = bitLength;
  }

//  protected List<Party> getServers(int amount, int basePort) {
//    List<Integer> freePorts = getPorts(amount * 3, basePort);
//    Map<Integer, Integer> clientFacingPorts = SpdzSetup.getClientFacingPorts(freePorts, amount);
//    List<Party> servers = new ArrayList<>(serverIdIpMap.size());
//    for (int id = 1; id <= clientFacingPorts.size(); id++) {
//      servers.add(new Party(id, serverIdIpMap.get(id), clientFacingPorts.get(id)));
//    }
//    return servers;
//  }
//
//  private List<Integer> getPorts(int total, int basePort) {
//    List<Integer> ports = new ArrayList<>();
//    for (int i = 1; i <= total; i++) {
//      ports.add(basePort + i);
//    }
//    return ports;
//  }
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
