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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SameObjectServer extends ServerPPP {

  private Map<Integer, List<SInt>> clientsInputs;
  public final List<BigInteger> REF_VALUES;
  public static final BigInteger UID = BigInteger.valueOf(10);
  public static final BigInteger DELTA_SHARE = BigInteger.valueOf(100);
  public final List<BigInteger> BETA_SHARE;// = Arrays.asList(BigInteger.valueOf(101), BigInteger.valueOf(102));

  private final int amountOfElements;

  public SameObjectServer(int myId, Map<Integer, String> serverIdIpMap, int bitLength,
      int basePort, int amountOfElements) {
    super(myId, serverIdIpMap, bitLength, basePort);
    this.amountOfElements = amountOfElements;
    this.REF_VALUES = IntStream.range(1, amountOfElements + 1).mapToObj(i -> BigInteger.valueOf(i))
        .collect(
            Collectors.toList());
    this.BETA_SHARE = IntStream.range(1, amountOfElements + 2)
        .mapToObj(i -> BigInteger.valueOf(100 + i)).collect(
            Collectors.toList());
  }

  @Override
  public void beforeEach() {
    spdz = new SpdzWithIO(myId, maxServers, currentBasePort,
        Collections.singletonList(ClientPPP.CLIENT_ID),
        Collections.singletonList(ClientPPP.CLIENT_ID + 1), serverIdIpMap, bitLength);
    clientsInputs = spdz.receiveInputs();
  }

  @Override
  public void run(Hole hole) {
    Application<List<SInt>, ProtocolBuilderNumeric> app = builder -> {
      Numeric input = builder.numeric();
      List<DRes<SInt>> refValues = new ArrayList<>();
      DRes<SInt> uid = input.known(UID);
      List<DRes<SInt>> attributes = new ArrayList<>();
      for (int i = 0; i < amountOfElements; i++) {
        attributes.add(clientsInputs.get(ClientPPP.CLIENT_ID).get(i));
        refValues.add(input.known(REF_VALUES.get(i)));
      }
      attributes.add(uid);
      List<DRes<SInt>> macs = new ArrayList<>();
      // MACs are stored in the list after attributes
      // First MAC is value MAC, second MAC is UID MAC
      for (int i = 0; i < amountOfElements + 1; i++) {
        macs.add(clientsInputs.get(ClientPPP.CLIENT_ID).get(amountOfElements + i));
      }
      int servers = serverIdIpMap.keySet().size();
      DRes<ServerInputModel> serverInput = builder.par(
          new ServerInputs(myId, DELTA_SHARE, BETA_SHARE, servers));
      return builder.seq((seq) -> {
        DRes<Boolean> checkAtt = seq.seq(
            new CheckAtt(attributes, macs, serverInput.out().getBetas(), serverInput.out()
                .getDelta()));
        // Compare, but excluding the uid
        DRes<SInt> comparisonRes = seq.seq(
            new SameObject(refValues, attributes.subList(0, amountOfElements)));
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
