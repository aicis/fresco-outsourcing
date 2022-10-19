package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GenericTestRunner {

    private final int inputsPerClient;
    private final int numberOfServers;
    private final int numberOfInputClients;
    private final int outputsPerClient;
    private final int numberOfOutputClients;
    private final SpdzWithIO.Protocol protocol;
    private final Function<Future<SpdzWithIO>, Object> serverSideProtocol;

    public GenericTestRunner(SpdzWithIO.Protocol protocol, int inputsPerClient, int numberOfInputClients, int outputsPerClient, int numberOfOutputClients, int numberOfServers, Function<Future<SpdzWithIO>, Object> serverSideProtocol) {
        this.protocol = protocol;
        this.inputsPerClient = inputsPerClient;
        this.numberOfInputClients = numberOfInputClients;
        this.outputsPerClient = outputsPerClient;
        this.numberOfOutputClients = numberOfOutputClients;
        this.numberOfServers = numberOfServers;
        this.serverSideProtocol = serverSideProtocol;
    }

    public int getInputsPerClient() {
        return inputsPerClient;
    }

    public int getOutputsPerClient() {
        return outputsPerClient;
    }

    public int getNumberOfServers() {
        return numberOfServers;
    }

    public int getNumberOfInputClients() {
        return numberOfInputClients;
    }

    public int getNumberOfOutputClients() {
        return numberOfOutputClients;
    }

    // Input is just a list of clientID
    protected List<BigInteger> computeInputs(int id) {
        return IntStream.range(0, inputsPerClient)
                .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
    }

    // The output is the same as the input of the first party
    protected List<BigInteger> computeOutputs(int id) {
        // The output is the same as the input of the first party, i.e. a list of 1's
        return IntStream.range(0, outputsPerClient)
                .mapToObj(num -> BigInteger.valueOf(1)).collect(Collectors.toList());
    }

    protected Map<Integer, Future<Object>> runOutputClients(Map<Integer, Integer> clientFacingPorts, OutputClientFunction outputClientFunction) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        Map<Integer, Future<Object>> assertFutures = new HashMap<>();
        for (int i = 0; i < numberOfOutputClients; i++) {
            final int id = i + numberOfInputClients + 1;
            Future<Object> assertFuture = es.submit(() -> {
                OutputClient client = outputClientFunction.apply(id, servers);
                List<BigInteger> actual = client.getBigIntegerOutputs();
                return actual;
            });
            assertFutures.put(id, assertFuture);
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.SECONDS);
        return assertFutures;
    }

    protected void runInputClients(int numClients, Map<Integer, Integer> clientFacingPorts, InputClientFunction inputClientFunction) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        for (int i = 0; i < numClients; i++) {
            final int id = i + 1;
            es.submit(() -> {
                InputClient client = inputClientFunction.apply(inputsPerClient, id, servers);
                List<BigInteger> inputs = computeInputs(id);
                client.putBigIntegerInputs(inputs);
            });
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.SECONDS);
    }

    protected List<Future<Object>> runServers(List<Integer> freePorts) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        List<Integer> serverIds = IntStream.rangeClosed(1, numberOfServers).boxed()
                .collect(Collectors.toList());

        List<Integer> inputIds = IntStream.rangeClosed(1, numberOfInputClients).boxed()
                .collect(Collectors.toList());

        List<Integer> outputIds = IntStream
                .range(numberOfInputClients + 1, numberOfInputClients + 1 + numberOfOutputClients).boxed()
                .collect(Collectors.toList());

        Map<Integer, Future<SpdzWithIO>> spdzServers = new HashMap<>(numberOfServers);
        Map<Integer, Integer> internalPorts = SpdzSetup.getInternalPorts(freePorts, numberOfServers);
        for (int serverId : serverIds) {
            Future<SpdzWithIO> spdzServer = es
                    .submit(() -> new SpdzWithIO(
                            serverId,
                            SpdzSetup.getClientFacingPorts(freePorts, numberOfServers),
                            internalPorts,
                            SpdzSetup.getApplicationPorts(freePorts, numberOfServers),
                            inputIds,
                            outputIds,
                            SpdzSetupUtils.getLocalhostMap(internalPorts),
                            SpdzSetupUtils.DEFAULT_BITLENGTH, true, protocol));
            spdzServers.put(serverId, spdzServer);
        }

        List<Future<Object>> assertFutures = new ArrayList<>();
        for (int serverId : serverIds) {
            Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
            assertFutures.add(es.submit(() -> serverSideProtocol.apply(futureServer)));
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.SECONDS);
        return assertFutures;
    }

    @FunctionalInterface
    interface InputClientFunction<inputsPerClient, id, servers> {
        InputClient apply(int inputsPerClient, int id, List<Party> servers);
    }

    @FunctionalInterface
    interface OutputClientFunction<id, servers> {
        OutputClient apply(int id, List<Party> servers);
    }
}
