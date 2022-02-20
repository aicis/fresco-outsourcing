package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.ComputationParallel;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.stream.Collectors;

public class ReconstructClientInputApp implements
    Application<Map<Integer, List<SInt>>, ProtocolBuilderNumeric> {

  private final SortedMap<Integer, ClientPayload<FieldElement>> clientPayload;
  private final FieldDefinition definition;
  private final int myId;
  private final int amountOfServers;

  public ReconstructClientInputApp(int myId, int amountOfServer, SortedMap<Integer, ClientPayload<FieldElement>> clientPayload, FieldDefinition definition) {
    this.myId = myId;
    this.amountOfServers = amountOfServer;
    this.clientPayload = clientPayload;
    this.definition = definition;
  }

  @Override
  public DRes<Map<Integer, List<SInt>>> buildComputation(ProtocolBuilderNumeric builder) {
    return builder.par((par) -> {
      Map<Integer, List<ClientPayload<DRes<SInt>>>> clientInputShares = new HashMap<>();
      for (Entry<Integer, ClientPayload<FieldElement>> entry : clientPayload.entrySet()) {
        ClientPayload<FieldElement> currentClientPayload = entry.getValue();
        List<ClientPayload<DRes<SInt>>> sharesOfInput = new ArrayList<>();
        for (int i = 1; i <= amountOfServers; i++) {
          DRes<SInt> t = par.numeric().input(i == myId ? currentClientPayload.getT().toBigInteger() : null, i);
          DRes<SInt> k = par.numeric().input(i == myId ? currentClientPayload.getK().toBigInteger() : null, i);
          DRes<SInt> r = par.numeric().input(i == myId ? currentClientPayload.getR().toBigInteger() : null, i);
          final int finalI = i;
          List<DRes<SInt>> xShareList = currentClientPayload.getX().stream().map(cur ->
              par.numeric().input(finalI == myId ? cur.toBigInteger() : null, finalI))
              .collect(Collectors.toList());
          ClientPayload<DRes<SInt>> currentClientShares = new ClientPayload<>(t, k, r, xShareList);
          sharesOfInput.add(currentClientShares);
        }
        clientInputShares.put(entry.getKey(), sharesOfInput);
      }
      return () -> clientInputShares;
    }).par( (par, clientInputShares) -> {
      Map<Integer, DRes<ClientPayload<DRes<SInt>>>> clientInput = new HashMap<>();
      for (Integer clientId : clientInputShares.keySet()) {
        clientInput.put(clientId, par.par(unMaskClientInputs(clientInputShares.get(clientId))));
      }
      return () -> clientInput;
    }).par( (par, clientInput) -> { //HERE
      return null;
    });
  }

  private ComputationParallel<ClientPayload<DRes<SInt>>, ProtocolBuilderNumeric> unMaskClientInputs(List<ClientPayload<DRes<SInt>>> sharedPayloads) {
    return builder -> {
      List<DRes<SInt>> tShares = sharedPayloads.stream().map(cur -> cur.getT()).collect(Collectors.toList());
      List<DRes<SInt>> kShares = sharedPayloads.stream().map(cur -> cur.getK()).collect(Collectors.toList());
      List<DRes<SInt>> rShares = sharedPayloads.stream().map(cur -> cur.getR()).collect(Collectors.toList());
      List<List<DRes<SInt>>> xShares = sharedPayloads.stream().map(cur -> cur.getX()).collect(Collectors.toList());
      DRes<SInt> t = AdvancedNumeric.using(builder).sum(tShares);
      DRes<SInt> k = AdvancedNumeric.using(builder).sum(kShares);
      DRes<SInt> r = AdvancedNumeric.using(builder).sum(rShares);
      List<DRes<SInt>> xValues = xShares.stream().map(cur ->
          AdvancedNumeric.using(builder).sum(cur)).collect(Collectors.toList());
      return ()-> new ClientPayload<DRes<SInt>>(t, k, r, xValues);
    };
  }
}
