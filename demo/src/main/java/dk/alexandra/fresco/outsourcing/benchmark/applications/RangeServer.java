package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.benchmark.ClientPPP;
import dk.alexandra.fresco.outsourcing.benchmark.Hole;
import dk.alexandra.fresco.outsourcing.benchmark.ServerPPP;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RangeServer extends ServerPPP  {
  private Map<Integer, List<SInt>> clientsInputs;
  private final int maxBitlength;
  public final BigInteger lower;
  public final BigInteger upper;
  public final List<BigInteger> BETA_SHARE = Arrays.asList(BigInteger.valueOf(101), BigInteger.valueOf(102));

  public RangeServer(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort, BigInteger lower, BigInteger upper,  int maxBitlength) {
    super(myId, serverIdIpMap, bitLength, basePort);
    this.maxBitlength = maxBitlength;
    this.lower = lower;
    this.upper = upper;
  }

  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort, Collections.singletonList(ClientPPP.CLIENT_ID), Collections.singletonList(ClientPPP.CLIENT_ID+1), serverIdIpMap, bitLength);
    clientsInputs = spdz.receiveInputs();
  }

  @Override
  public void run(Hole hole) {
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
      Numeric input = builder.numeric();
      DRes<SInt> hiddenLower = input.known(lower);
      DRes<SInt> hiddenUpper = input.known(upper);
      DRes<SInt> uid = input.known(UID);
      List<DRes<SInt>> attributes = Arrays.asList(clientsInputs.get(ClientPPP.CLIENT_ID).get(0), uid);
      // MACs are stored in the list after attributes
      // First MAC is value MAC, second MAC is UID MAC
      List<DRes<SInt>> macs = Arrays.asList(
          clientsInputs.get(ClientPPP.CLIENT_ID).get(1),
          clientsInputs.get(ClientPPP.CLIENT_ID).get(2));
      int servers = serverIdIpMap.keySet().size();
      DRes<ServerInputModel> serverInput = builder.par(
          new ServerInputs(myId, DELTA_SHARE, BETA_SHARE, servers));
      return builder.seq((seq) -> {
        DRes<Boolean> checkAtt = seq.seq(
            new CheckAtt(attributes, macs, serverInput.out().getBetas(), serverInput.out()
                .getDelta()));
        DRes<SInt> comparisonRes = seq.seq(
            new Range(hiddenLower, hiddenUpper, attributes.get(0), maxBitlength));
        return seq.seq((seq2) -> {
          if (checkAtt.out() != true) {
            throw new IllegalArgumentException("Invalid user MAC");
          }
          return () -> Collections.singletonList(comparisonRes.out());
        });
      });
    };
    spdz.sendOutputsTo(ClientPPP.CLIENT_ID+1, spdz.run(app));
  }
}
