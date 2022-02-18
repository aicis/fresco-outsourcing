package dk.alexandra.fresco.outsourcing.benchmark;


import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

public abstract class ServerPPP extends PPP {

  public static final BigInteger UID = BigInteger.valueOf(10);
  public static final BigInteger DELTA_SHARE = BigInteger.valueOf(100);
  protected final int myId;

  protected int currentBasePort;
  protected SpdzWithIO spdz;


  public ServerPPP(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
    super(serverIdIpMap, bitLength);
    this.myId = myId;
    this.currentBasePort = basePort;
  }


  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort,
        Collections.singletonList(ClientPPP.CLIENT_ID),
        Collections.singletonList(ClientPPP.CLIENT_ID + 1), serverIdIpMap, bitLength);
  }

  @Override
  public void afterEach() {
    // Move base ports up
    currentBasePort += maxServers;
    spdz.shutdown();
  }
}
