package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.outsourcing.benchmark.applications.SameObjectServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO.Protocol;
import java.util.Collections;
import java.util.Map;
import sweis.threshsig.KeyShare;

public class MascotServer extends SameObjectServer {

  public MascotServer(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort, KeyShare keyShare) {
    super(myId, serverIdIpMap, bitLength, basePort, 1, keyShare);
  }

  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort,
        Collections.singletonList(ClientPPP.CLIENT_ID),
        Collections.singletonList(ClientPPP.CLIENT_ID + 1), serverIdIpMap, bitLength, false,
        Protocol.PESTO, keyShare);
    clientsInputs = spdz.receiveInputs();
  }

}
