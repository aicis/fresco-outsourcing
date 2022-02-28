package dk.alexandra.fresco.outsourcing.benchmark;


import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import sweis.threshsig.KeyShare;

public abstract class ServerPPP extends PPP {

  public static final BigInteger UID = BigInteger.valueOf(10);
  public static final BigInteger DELTA_SHARE = BigInteger.valueOf(100);
  protected final int myId;

  protected int currentBasePort;
  protected SpdzWithIO spdz;
  protected KeyShare keyShare;


  public ServerPPP(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort, KeyShare keyShare) {
    super(serverIdIpMap, bitLength);
    this.myId = myId;
    this.currentBasePort = basePort;
    this.keyShare = keyShare;
  }


  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort,
        Collections.singletonList(ClientPPP.CLIENT_ID),
        Collections.singletonList(ClientPPP.CLIENT_ID + 1), serverIdIpMap, bitLength, keyShare);
  }

  @Override
  public void afterEach() {
    // Move base ports up
    currentBasePort += maxServers;
    spdz.shutdown();
    System.gc();
  }
}
