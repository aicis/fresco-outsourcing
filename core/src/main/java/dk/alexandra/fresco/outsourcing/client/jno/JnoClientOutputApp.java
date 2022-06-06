package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.value.SInt;

import java.math.BigInteger;
import java.util.*;

public class JnoClientOutputApp implements
    Application<Map<Integer, List<BigInteger>>, ProtocolBuilderNumeric> {

  private final SortedMap<Integer, ClientPayload<FieldElement>> clientPayload;
  private final int myId;
  private final int amountOfServers;
  private final Map<Integer, List<SInt>> clientOutput;

  public JnoClientOutputApp(int myId, int amountOfServer, SortedMap<Integer, ClientPayload<FieldElement>> clientPayload, Map<Integer, List<SInt>> clientOutput) {
    this.myId = myId;
    this.amountOfServers = amountOfServer;
    this.clientPayload = clientPayload;
    this.clientOutput = clientOutput;
  }

  @Override
  public DRes<Map<Integer, List<BigInteger>>> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      return par.seq(new ReconstructClientInput(myId, amountOfServers, clientPayload));
      }).par( (par, clientInput) -> {
      Map<Integer, List<DRes<BigInteger>>> unopenedRes = new HashMap<>();
      for (Integer clientId : clientInput.keySet()) {
        List<DRes<BigInteger>> currentClientList = new ArrayList<>();
        for (int i = 0; i < clientInput.get(clientId).size(); i++) {
          currentClientList.add(par.numeric().open(par.numeric().add(clientInput.get(clientId).get(i),
                  clientOutput.get(clientId).get(i))));
        }
        unopenedRes.put(clientId, currentClientList);
      }
      return () -> unopenedRes;
    }).par((par, unopenedRes) -> {
      Map<Integer, List<BigInteger>> res = new HashMap<>();
      for (Integer clientId : unopenedRes.keySet()) {
        List<BigInteger> currentClientList = new ArrayList<>();
        for (int i = 0; i < unopenedRes.get(clientId).size(); i++) {
          currentClientList.add(unopenedRes.get(clientId).get(i).out());
        }
        res.put(clientId, currentClientList);
      }
      return ()-> res;
    });
  }
}
