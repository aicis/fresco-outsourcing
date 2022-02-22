package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO.Protocol;
import java.util.Collections;
import java.util.Map;

public class MascotServer extends ServerPPP {

  public MascotServer(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
    super(myId, serverIdIpMap, bitLength, basePort);
  }

  @Override
  public void beforeEach() {
  }

  @Override
  public void run(Hole blackhole) {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort,
        Collections.singletonList(ClientPPP.CLIENT_ID),
        Collections.singletonList(ClientPPP.CLIENT_ID + 1), serverIdIpMap, bitLength, false, Protocol.JNO);
  }
}
