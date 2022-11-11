package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;

import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.OutputServerProducer;
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

    protected final int inputsPerClient;
    protected final int numberOfServers;
    protected final int numberOfInputClients;
    protected final int outputsPerClient;
    protected final int numberOfOutputClients;
    protected final SpdzWithIO.Protocol protocol;
    protected final Function<Future<SpdzWithIO>, Object> serverSideProtocol;
    protected final InputServerProducer inputServerProducer;
    protected final OutputServerProducer outputServerProducer;

    public GenericTestRunner(SpdzWithIO.Protocol protocol,
        int inputsPerClient, int numberOfInputClients, int outputsPerClient,
        int numberOfOutputClients,
        int numberOfServers, Function<Future<SpdzWithIO>, Object> serverSideProtocol,
        InputServerProducer inputServerProducer,
        OutputServerProducer outputServerProducer) {
        this.protocol = protocol;
        this.inputsPerClient = inputsPerClient;
        this.numberOfInputClients = numberOfInputClients;
        this.outputsPerClient = outputsPerClient;
        this.numberOfOutputClients = numberOfOutputClients;
        this.numberOfServers = numberOfServers;
        this.serverSideProtocol = serverSideProtocol;
        this.inputServerProducer = inputServerProducer;
        this.outputServerProducer = outputServerProducer;
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
    public List<BigInteger> computeInputs(int id) {
        return IntStream.range(0, getInputsPerClient())
                .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
    }

    // The output is the same as the input of the first party
    public List<BigInteger> computeOutputs(int id) {
        // The output is the same as the input of the first party, i.e. a list of 1's
        return IntStream.range(0, getOutputsPerClient())
                .mapToObj(num -> BigInteger.valueOf(1)).collect(Collectors.toList());
    }

    public Map<Integer, Future<Object>> runOutputClients(Map<Integer, Integer> clientFacingPorts, OutputClientFunction outputClientFunction) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        Map<Integer, Future<Object>> assertFutures = new HashMap<>();
        for (int i = 0; i < getNumberOfOutputClients(); i++) {
            final int id = i + getNumberOfInputClients() + 1;
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

    public void runInputClients(int numClients, Map<Integer, Integer> clientFacingPorts,
        InputClientFunction inputClientFunction) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        for (int i = 0; i < numClients; i++) {
            final int id = i + 1;
            es.submit(() -> {
                InputClient client = inputClientFunction.apply(getInputsPerClient(), id, servers);
                List<BigInteger> inputs = computeInputs(id);
                client.putBigIntegerInputs(inputs);
            });
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.SECONDS);
    }

    public List<Future<Object>> runServers(List<Integer> freePorts) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        List<Integer> serverIds = IntStream.rangeClosed(1, getNumberOfServers()).boxed()
                .collect(Collectors.toList());

        List<Integer> inputIds = IntStream.rangeClosed(1, getNumberOfInputClients()).boxed()
                .collect(Collectors.toList());

        List<Integer> outputIds = IntStream
                .range(getNumberOfInputClients() + 1,
                    getNumberOfInputClients() + 1 + getNumberOfOutputClients()).boxed()
                .collect(Collectors.toList());

        Map<Integer, Future<SpdzWithIO>> spdzServers = new HashMap<>(getNumberOfServers());
        Map<Integer, Integer> internalPorts = SpdzSetup.getInternalPorts(freePorts,
            getNumberOfServers());
        for (int serverId : serverIds) {
            Future<SpdzWithIO> spdzServer = es
                    .submit(() -> new SpdzWithIO(
                            serverId,
                            SpdzSetup.getClientFacingPorts(freePorts, getNumberOfServers()),
                            internalPorts,
                            SpdzSetup.getApplicationPorts(freePorts, getNumberOfServers()),
                            inputIds,
                            outputIds,
                            SpdzSetupUtils.getLocalhostMap(internalPorts),
                            inputServerProducer,
                            outputServerProducer,
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
    public interface InputClientFunction<InputClientT extends InputClient> {
        InputClientT apply(int inputsPerClient, int id, List<Party> servers);
    }

    @FunctionalInterface
    public interface OutputClientFunction<OutputClientT extends OutputClient> {
        OutputClientT apply(int id, List<Party> servers);
    }
}
