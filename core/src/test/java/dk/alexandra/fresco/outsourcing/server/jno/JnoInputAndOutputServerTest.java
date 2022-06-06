package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoInputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoOutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class JnoInputAndOutputServerTest {
    private static final int INPUTS_PER_CLIENT = 100;
    private static final int OUTPUTS_PER_CLIENT = 1;
    private static final int NUMBER_OF_SERVERS = 3;
    private static final int NUMBER_OF_INPUT_CLIENTS = 10;
    private static final int NUMBER_OF_OUTPUT_CLIENTS = 1;
    private static final int FIRST_OUTPUT_CLIENT_ID = NUMBER_OF_INPUT_CLIENTS + NUMBER_OF_OUTPUT_CLIENTS;

    @Test
    public void testInputsAndOutput() throws InterruptedException, ExecutionException {
        int numInputClients = NUMBER_OF_INPUT_CLIENTS;
        int numOutputClients = NUMBER_OF_OUTPUT_CLIENTS;
        int numServers = NUMBER_OF_SERVERS;
        Map<Integer, List<BigInteger>> expectedOutputs = new HashMap<>();
        List<Integer> outputIds = IntStream
                .range(FIRST_OUTPUT_CLIENT_ID, FIRST_OUTPUT_CLIENT_ID + numOutputClients).boxed()
                .collect(Collectors.toList());
        for (int id : outputIds) {
            // The output for the ith output client will be the input of input client ith input client, ie id i.
            expectedOutputs.put(id, IntStream.range(FIRST_OUTPUT_CLIENT_ID, FIRST_OUTPUT_CLIENT_ID+ OUTPUTS_PER_CLIENT).mapToObj(i-> BigInteger.valueOf(id - NUMBER_OF_INPUT_CLIENTS)).collect(Collectors.toList()));
        }
        List<Integer> freePorts = SpdzSetup.getFreePorts(NUMBER_OF_SERVERS * 3);
        runServers(numInputClients, numOutputClients,
                numServers, freePorts);
        runInputClients(numInputClients, SpdzSetup.getClientFacingPorts(freePorts, numServers));

        List<Future<Object>> assertFutures = runOutputClients(numOutputClients,
                SpdzSetup.getClientFacingPorts(freePorts, numServers),
                expectedOutputs);
        for (Future<Object> assertFuture : assertFutures) {
            assertFuture.get();
        }
    }

    private void serverSideProtocol(Future<SpdzWithIO> futureServer) {
        try {
            SpdzWithIO spdz = futureServer.get();
            Map<Integer, List<SInt>> clientInputs = spdz.receiveInputs();
            spdz.sendOutputsTo(FIRST_OUTPUT_CLIENT_ID, clientInputs.get(1));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<BigInteger> computeInputs(int id) {
        return IntStream.range(0, INPUTS_PER_CLIENT)
                .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
    }

    private void runInputClients(int numClients, Map<Integer, Integer> clientFacingPorts) {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newFixedThreadPool(8);
        for (int i = 0; i < numClients; i++) {
            final int id = i + 1;
            es.submit(() -> {
                // TODO make generic with factory
                InputClient client = new JnoInputClient(INPUTS_PER_CLIENT, id, servers,  BigIntegerFieldDefinition::new, new AesCtrDrbg(new byte[32]));
                List<BigInteger> inputs = computeInputs(id);
                client.putBigIntegerInputs(inputs);
            });
        }
        es.shutdown();
    }

    private List<Future<Object>> runOutputClients(int numClients,
                                                  Map<Integer, Integer> clientFacingPorts,
                                                  Map<Integer, List<BigInteger>> expectedOutputs) throws InterruptedException {
        List<Party> servers = new ArrayList<>(clientFacingPorts.size());
        for (int i = 1; i <= clientFacingPorts.size(); i++) {
            servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
        }
        ExecutorService es = Executors.newCachedThreadPool();
        List<Future<Object>> assertFutures = new ArrayList<>(numClients);
        for (int i = 0; i < numClients; i++) {
            final int id = i + FIRST_OUTPUT_CLIENT_ID;
            Future<Object> assertFuture = es.submit(() -> {
                OutputClient client = new JnoOutputClient(id, servers, BigIntegerFieldDefinition::new, new AesCtrDrbg(new byte[32]), OUTPUTS_PER_CLIENT);
                List<BigInteger> actual = client.getBigIntegerOutputs();
                assertEquals(expectedOutputs.get(id), actual);
                return null;
            });
            assertFutures.add(assertFuture);
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.HOURS);
        return assertFutures;
    }

    private void runServers(int numInputClients,
                            int numOutputClients,
                            int numServers,
                            List<Integer> freePorts) {
        ExecutorService es = Executors.newCachedThreadPool();
        List<Integer> serverIds = IntStream.rangeClosed(1, numServers).boxed()
                .collect(Collectors.toList());

        List<Integer> inputIds = IntStream.rangeClosed(1, numInputClients).boxed()
                .collect(Collectors.toList());

        List<Integer> outputIds = IntStream
                .range(FIRST_OUTPUT_CLIENT_ID, FIRST_OUTPUT_CLIENT_ID + numOutputClients).boxed()
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
                            SpdzSetupUtils.DEFAULT_BITLENGTH, true, SpdzWithIO.Protocol.JNO));
            spdzServers.put(serverId, spdzServer);
        }

        for (int serverId : serverIds) {
            Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
            es.submit(() -> serverSideProtocol(futureServer));
        }

        es.shutdown();
    }

}
