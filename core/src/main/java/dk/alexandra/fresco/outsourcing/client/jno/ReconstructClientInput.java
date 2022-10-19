package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.ComputationParallel;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.common.math.AdvancedNumeric;
import dk.alexandra.fresco.outsourcing.utils.GenericUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

public class ReconstructClientInput implements Computation<Map<Integer, List<SInt>>, ProtocolBuilderNumeric> {
    private final SortedMap<Integer, ClientPayload<FieldElement>> clientPayload;
    private final int myId;
    private final int amountOfServers;

    public ReconstructClientInput(int myId, int amountOfServer, SortedMap<Integer, ClientPayload<FieldElement>> clientPayload) {
        this.myId = myId;
        this.amountOfServers = amountOfServer;
        this.clientPayload = clientPayload;
    }

    @Override
    public DRes<Map<Integer, List<SInt>>> buildComputation(ProtocolBuilderNumeric builder) {
        return builder.par((par) -> {
            // Recover the client input values, t, k, r for each server and each client outsourced
            Map<Integer, List<ClientPayload<DRes<SInt>>>> clientInputShares = new HashMap<>();
            for (Map.Entry<Integer, ClientPayload<FieldElement>> entry : clientPayload.entrySet()) {
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
            // Combine the shares of the different servers to get a ClientPayload element for each client
            Map<Integer, DRes<ClientPayload<DRes<SInt>>>> clientInput = new HashMap<>();
            for (Integer clientId : clientInputShares.keySet()) {
                clientInput.put(clientId, par.par(combineClientShares(clientInputShares.get(clientId))));
            }
            return () -> clientInput;
        }).par( (par, clientInput) -> {
            // According to the protocol, open the k value for each of the clients
            Map<Integer, DRes<BigInteger>> openedKs = new HashMap<>();
            for (Integer clientId : clientInput.keySet()) {
                DRes<BigInteger> openK = par.numeric().open(clientInput.get(clientId).out().getK());
                openedKs.put(clientId, openK);
            }
            return ()-> new Pair<>(openedKs, clientInput);
        }).par((par, pair) -> {
            // According to the protocol compute the tag on the client's input, for each client
            Map<Integer, DRes<SInt>> refTags = new HashMap<>();
            for (Integer clientId : pair.getSecond().keySet()) {
                DRes<SInt> referenceTag = par.seq(computeTag(pair.getSecond().get(clientId).out().getX(),
                        pair.getFirst().get(clientId), pair.getSecond().get(clientId).out().getR()));
                refTags.put(clientId, referenceTag);
            }
            return ()-> new Pair<>(refTags, pair.getSecond());
        }).par((par, pair) -> {
            // Recover the list of the client's secret shared input from ClientPayload, for each client.
            // That is, the list of x values. Furthermore, subtract the computed tag from the tag given as input.
            Map<Integer, List<DRes<SInt>>> unopenedResult = new HashMap<>();
            Map<Integer, List<SInt>> finalResult = new HashMap<>();
            List<DRes<SInt>> refValues = new ArrayList<>();
            for (Integer clientId : pair.getSecond().keySet()) {
                unopenedResult.put(clientId, pair.getSecond().get(clientId).out().getX());
                refValues.add(par.numeric().sub(pair.getFirst().get(clientId), pair.getSecond().get(clientId).out().getT()));
                finalResult.put(clientId, unopenedResult.get(clientId).stream().map(cur -> cur.out()).collect(Collectors.toList()));
            }
            return () -> new Pair<List<DRes<SInt>>, Map<Integer, List<SInt>>>(refValues, finalResult);
        }).seq( (par, pair) -> {
            // Multiply the sum of the reference values (difference between computed and supplied tag) with a random
            // element and open the result, in order to validate consistency.
            DRes<SInt> sum = AdvancedNumeric.using(par).sum(pair.getFirst());
            DRes<SInt> rand = par.numeric().randomElement();
            DRes<SInt> randomProduct = par.numeric().mult(sum, rand);
            DRes<BigInteger> res = par.numeric().open(randomProduct);
            return ()-> new Pair<>(res, pair.getSecond());
        }).par( (par, pair) -> {
            if (!pair.getFirst().out().equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("The client tags did no verify");
            }
            return ()-> pair.getSecond();
        });
    }

    private ComputationParallel<ClientPayload<DRes<SInt>>, ProtocolBuilderNumeric> combineClientShares(List<ClientPayload<DRes<SInt>>> sharedPayloads) {
        return builder -> {
            List<DRes<SInt>> tShares = sharedPayloads.stream().map(cur -> cur.getT()).collect(Collectors.toList());
            List<DRes<SInt>> kShares = sharedPayloads.stream().map(cur -> cur.getK()).collect(Collectors.toList());
            List<DRes<SInt>> rShares = sharedPayloads.stream().map(cur -> cur.getR()).collect(Collectors.toList());
            List<List<DRes<SInt>>> xShares =
                GenericUtils.transpose(sharedPayloads.stream().map(cur -> cur.getX()).collect(Collectors.toList()));
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
                currentKeyPower = currentKeyPower.multiply(key.out()).mod(builder.getBasicNumericContext().getModulus());
            }
            tag = builder.numeric().add(tag, builder.numeric().mult(currentKeyPower, randomness));
            return builder.numeric().add(currentKeyPower.multiply(key.out()).multiply(key.out()), tag);
        };
    }
}
