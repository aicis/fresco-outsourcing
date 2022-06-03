package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.ComputationParallel;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.outsourcing.client.ClientBase;

import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
        clientInput.put(clientId, par.par(combineClientShares(clientInputShares.get(clientId))));
      }
      return () -> clientInput;
    }).seq( (seq, clientInput) -> {
      Map<Integer, List<DRes<SInt>>> unopenedResult = new HashMap<>();
      Map<Integer, List<SInt>> finalResult = new HashMap<>();
      List<DRes<SInt>> refValues = new ArrayList<>();
      for (Integer clientId : clientInput.keySet()) {
        unopenedResult.put(clientId, clientInput.get(clientId).out().getX());
        DRes<BigInteger> openK = seq.numeric().open(clientInput.get(clientId).out().getK());
        DRes<SInt> referenceTag = seq.seq(computeTag(clientInput.get(clientId).out().getX(),
            openK, clientInput.get(clientId).out().getR()));
        refValues.add(seq.numeric().sub(referenceTag, clientInput.get(clientId).out().getT()));
        finalResult.put(clientId,
            unopenedResult.get(clientId).stream().map(cur -> cur.out())
                .collect(Collectors.toList()));
      }
      DRes<SInt> sum = AdvancedNumeric.using(seq).sum(refValues);
      DRes<SInt> rand = seq.numeric().randomElement();
      DRes<SInt> randomProduct = seq.numeric().mult(sum, rand);
      DRes<BigInteger> res = seq.numeric().open(randomProduct);
      return () -> new Pair<DRes<BigInteger>, Map<Integer, List<SInt>>>(res, finalResult);
    }).par( (par, pair) -> { //TODO is par a problem here?
      if (!pair.getFirst().out().equals(BigInteger.ZERO)) {
        throw new IllegalArgumentException("The client tags did no verify");
      }
      Map<Integer, List<DRes<BigInteger>>> unopenedRes = new HashMap<>();
      for (Integer clientId : pair.getSecond().keySet()) {
        List<DRes<BigInteger>> currentClientList = new ArrayList<>();
        for (int i = 0; i < pair.getSecond().get(clientId).size(); i++) {
          currentClientList.add(par.numeric().open(par.numeric().add(pair.getSecond().get(clientId).get(i),
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

  private ComputationParallel<ClientPayload<DRes<SInt>>, ProtocolBuilderNumeric> combineClientShares(List<ClientPayload<DRes<SInt>>> sharedPayloads) {
    return builder -> {
      List<DRes<SInt>> tShares = sharedPayloads.stream().map(cur -> cur.getT()).collect(Collectors.toList());
      List<DRes<SInt>> kShares = sharedPayloads.stream().map(cur -> cur.getK()).collect(Collectors.toList());
      List<DRes<SInt>> rShares = sharedPayloads.stream().map(cur -> cur.getR()).collect(Collectors.toList());
      List<List<DRes<SInt>>> xShares = ClientBase.transpose(sharedPayloads.stream().map(cur -> cur.getX()).collect(Collectors.toList()));
      DRes<SInt> t = AdvancedNumeric.using(builder).sum(tShares);
      DRes<SInt> k = AdvancedNumeric.using(builder).sum(kShares);
      DRes<SInt> r = AdvancedNumeric.using(builder).sum(rShares);
      List<DRes<SInt>> xValues = xShares.stream().map(cur ->
          AdvancedNumeric.using(builder).sum(cur)).collect(Collectors.toList());
      return ()-> new ClientPayload<DRes<SInt>>(t, k, r, xValues);
    };
  }

  private ComputationParallel<SInt, ProtocolBuilderNumeric> computeTag(List<DRes<SInt>> inputs, DRes<BigInteger> key, DRes<SInt> randomness) {
    return builder -> {
      DRes<SInt> tag = builder.numeric().known(0);
      BigInteger currentKeyPower = key.out();
      for (DRes<SInt> current : inputs) {
        tag = builder.numeric().add(tag, builder.numeric().mult(currentKeyPower, current));
        currentKeyPower = currentKeyPower.multiply(key.out()).mod(builder.getBasicNumericContext().getModulus());;
      }
      tag = builder.numeric().add(tag, builder.numeric().mult(currentKeyPower, randomness));
      return builder.numeric().add(currentKeyPower.multiply(key.out()).multiply(key.out()), tag);
    };
  }
}
