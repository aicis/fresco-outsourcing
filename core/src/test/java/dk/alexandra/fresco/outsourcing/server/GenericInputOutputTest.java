package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients, and send outputs to a single client.
 */
public abstract class GenericInputOutputTest {
    protected static int INPUTS_PER_CLIENT;
    protected static int NUMBER_OF_SERVERS;
    protected static int NUMBER_OF_INPUT_CLIENTS;
    protected static int NUMBER_OF_OUTPUT_CLIENTS;

    protected abstract SpdzWithIO.Protocol getProtocol();

    protected abstract InputClient getInputClient(int inputsPerClient, int id, List<Party> servers);

    protected abstract OutputClient getOutputClient(int id, List<Party> servers);

    /**
     * Test the protocol by simply outputting the inputs
     */
    @Test
    public void testMoreInputClientsThanOutputClients() throws Exception {
        INPUTS_PER_CLIENT = 10;
        NUMBER_OF_SERVERS = 3;
        NUMBER_OF_INPUT_CLIENTS = 10;
        NUMBER_OF_OUTPUT_CLIENTS = 8; // todo more than 8 output clients will cause a stall for unknown reasons
        testInputsAndOutput();
    }

    @Test
    public void testMoreOutputClientsThanInputClients() throws Exception {
        INPUTS_PER_CLIENT = 10;
        NUMBER_OF_SERVERS = 3;
        NUMBER_OF_INPUT_CLIENTS = 5;
        NUMBER_OF_OUTPUT_CLIENTS = 8;
        testInputsAndOutput();
    }

    @Test
    public void testManyServers() throws Exception {
        INPUTS_PER_CLIENT = 3;
        NUMBER_OF_SERVERS = 10;
        NUMBER_OF_INPUT_CLIENTS = 5;
        NUMBER_OF_OUTPUT_CLIENTS = 5;
        testInputsAndOutput();
    }

    public void testInputsAndOutput() throws InterruptedException, ExecutionException {
        int numInputClients = NUMBER_OF_INPUT_CLIENTS;
        int numOutputClients = NUMBER_OF_OUTPUT_CLIENTS;
        int numServers = NUMBER_OF_SERVERS;
        List<Integer> freePorts = SpdzSetup.getFreePorts(NUMBER_OF_SERVERS * 3);
        runServers(numInputClients, numOutputClients,
                numServers, freePorts);
        runInputClients(numInputClients, SpdzSetup.getClientFacingPorts(freePorts, numServers));

        List<Future<Object>> assertFutures = runOutputClients(numOutputClients,
                SpdzSetup.getClientFacingPorts(freePorts, numServers));
        for (Future<Object> assertFuture : assertFutures) {
            assertFuture.get();
        }
        assertEquals(NUMBER_OF_OUTPUT_CLIENTS, assertFutures.size());
    }

    private Map<Integer, List<SInt>> mapToOutputs(Map<Integer, List<SInt>> clientInput) {
        Map<Integer, List<SInt>> clientOutput = new HashMap<>();
        for (int i = NUMBER_OF_INPUT_CLIENTS + 1; i < NUMBER_OF_INPUT_CLIENTS + 1 + NUMBER_OF_OUTPUT_CLIENTS; i++) {
            // The output is the input of the first party
            clientOutput.put(i, clientInput.get(1));
        }
        return clientOutput;
    }

    private void serverSideProtocol(Future<SpdzWithIO> futureServer) {
        try {
            SpdzWithIO server = futureServer.get();
            Map<Integer, List<SInt>> clientInputs = server.receiveInputs();
            // Derive the output from the input
            Map<Integer, List<SInt>> clientOutputs = mapToOutputs(clientInputs);
            IntStream.range(NUMBER_OF_INPUT_CLIENTS + 1, NUMBER_OF_INPUT_CLIENTS + 1 + NUMBER_OF_OUTPUT_CLIENTS).forEach(clientId -> {
                server.sendOutputsTo(clientId, clientOutputs.get(clientId));
            });
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void runInputClients(int numClients, Map<Integer, Integer> clientFacingPorts) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        for (int i = 0; i < numClients; i++) {
            final int id = i + 1;
            es.submit(() -> {
                InputClient client = getInputClient(INPUTS_PER_CLIENT, id, servers);
                List<BigInteger> inputs = computeInputs(id);
                client.putBigIntegerInputs(inputs);
            });
        }
        es.shutdown();
        es.awaitTermination(5, TimeUnit.SECONDS);
    }

    private List<BigInteger> computeInputs(int id) {
        return IntStream.range(0, INPUTS_PER_CLIENT)
                .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
    }

    private List<BigInteger> computeOutputs(int id) {
        // The output is the same as the input of the first party
        return computeInputs(1);
    }

    private List<Future<Object>> runOutputClients(int numClients,
                                                  Map<Integer, Integer> clientFacingPorts) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        List<Future<Object>> assertFutures = new ArrayList<>(numClients);
        for (int i = 0; i < numClients; i++) {
            final int id = i + NUMBER_OF_INPUT_CLIENTS + 1;
            Future<Object> assertFuture = es.submit(() -> {
                OutputClient client = getOutputClient(id, servers);
                List<BigInteger> actual = client.getBigIntegerOutputs();
                assertEquals(computeOutputs(id), actual);
                return null;
            });
            assertFutures.add(assertFuture);
        }
        es.shutdown();
        es.awaitTermination(5, TimeUnit.SECONDS);
        return assertFutures;
    }

    private void runServers(int numInputClients,
                            int numOutputClients,
                            int numServers,
                            List<Integer> freePorts) throws InterruptedException {
        ExecutorService es = Executors.newCachedThreadPool();
        List<Integer> serverIds = IntStream.rangeClosed(1, numServers).boxed()
                .collect(Collectors.toList());

        List<Integer> inputIds = IntStream.rangeClosed(1, numInputClients).boxed()
                .collect(Collectors.toList());

        List<Integer> outputIds = IntStream
                .range(NUMBER_OF_INPUT_CLIENTS + 1, NUMBER_OF_INPUT_CLIENTS + 1 + numOutputClients).boxed()
                .collect(Collectors.toList());

        Map<Integer, Future<SpdzWithIO>> spdzServers = new HashMap<>(numServers);
        Map<Integer, Integer> internalPorts = SpdzSetup.getInternalPorts(freePorts, numServers);
        for (int serverId : serverIds) {
            Future<SpdzWithIO> spdzServer = es
                    .submit(() -> new SpdzWithIO(
                            serverId,
                            SpdzSetup.getClientFacingPorts(freePorts, numServers),
                            internalPorts,
                            SpdzSetup.getApplicationPorts(freePorts, numServers),
                            inputIds,
                            outputIds,
                            SpdzSetupUtils.getLocalhostMap(internalPorts),
                            SpdzSetupUtils.DEFAULT_BITLENGTH, true, getProtocol()));
            spdzServers.put(serverId, spdzServer);
        }

        for (int serverId : serverIds) {
            Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
            es.submit(() -> serverSideProtocol(futureServer));
        }

        es.shutdown();
        es.awaitTermination(5, TimeUnit.SECONDS);
    }
}
