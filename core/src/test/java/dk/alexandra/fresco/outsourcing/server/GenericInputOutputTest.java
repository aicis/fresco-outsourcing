package dk.alexandra.fresco.outsourcing.server;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients, and send outputs to a single client.
 */
public abstract class GenericInputOutputTest {
    protected abstract InputClient getInputClient(int inputsPerClient, int id, List<Party> servers);

    protected abstract OutputClient getOutputClient(int id, List<Party> servers);

    protected static GenericTestRunner testRunner;

    protected abstract SpdzWithIO.Protocol getProtocol();

    protected void setTestRunner(int inputsPerClient, int numberOfInputClients, int outputsPerClient,
        int numberOfOutputClients, int numberOfServers) {
        testRunner = new GenericTestRunner(
                getProtocol(),
                inputsPerClient,
                numberOfInputClients,
                outputsPerClient,
                numberOfOutputClients,
                numberOfServers, (futureServer) -> {
            try {
                SpdzWithIO server = futureServer.get();
                Map<Integer, List<SInt>> clientInputs = server.receiveInputs();
                // Derive the output from the input
                Map<Integer, List<SInt>> clientOutputs = mapToOutputs(clientInputs, numberOfInputClients, numberOfOutputClients, outputsPerClient);
                IntStream.range(numberOfInputClients + 1, numberOfInputClients + 1 + numberOfOutputClients).forEach(clientId -> {
                    server.sendOutputsTo(clientId, clientOutputs.get(clientId));
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private Map<Integer, List<SInt>> mapToOutputs(Map<Integer, List<SInt>> clientInput, int numberOfInputClients, int numberOfOutputClients, int outputsPerClient) {
        Map<Integer, List<SInt>> clientOutput = new HashMap<>();
        for (int i = numberOfInputClients + 1; i < numberOfInputClients + 1 + numberOfOutputClients; i++) {
            List<SInt> outputs = new ArrayList<>();
            for (int j = 0; j < outputsPerClient; j++) {
                outputs.add(clientInput.get(1).get(0));
            }
            // The output is the input of the first party
            clientOutput.put(i, outputs);
        }
        return clientOutput;
    }

    /**
     * Test the protocol by simply outputting the inputs
     */
    @Test
    public void testMoreInputClientsThanOutputClients() throws Exception {
        setTestRunner(10, 10, 10, 8, 3);
        testInputsAndOutput();
    }

    @Test
    public void testMoreOutputClientsThanInputClients() throws Exception {
        setTestRunner(10, 5, 10, 8, 3);
        testInputsAndOutput();
    }

    @Test
    public void testManyServers() throws Exception {
        setTestRunner(3, 5, 3, 5, 10);
        testInputsAndOutput();
    }

    @Test
    public void moreOutputsPerClient() throws Exception {
        setTestRunner(3, 3, 5, 4, 3);
        testInputsAndOutput();
    }

    @Test
    public void moreInputsPerClient() throws Exception {
        setTestRunner(5, 1, 3, 3, 3);
        testInputsAndOutput();
    }

    public void testInputsAndOutput() throws InterruptedException, ExecutionException {
        List<Integer> freePorts = SpdzSetup.getFreePorts(testRunner.getNumberOfServers() * 3);
        testRunner.runServers(freePorts);
        GenericTestRunner.InputClientFunction inputClientFunction = (inputsPerClient, id, servers) -> {
            return getInputClient(inputsPerClient, id, servers);
        };
        testRunner.runInputClients(testRunner.getNumberOfInputClients(), SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()), inputClientFunction);

        GenericTestRunner.OutputClientFunction outputClientFunction = (id, servers) -> {
            return getOutputClient(id, servers);
        };
        Map<Integer, Future<Object>> assertFutures = testRunner.runOutputClients(
                SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()), outputClientFunction);
        for (int clientId : assertFutures.keySet()) {
            List<BigInteger> actual = (List<BigInteger>) assertFutures.get(clientId).get();
            assertEquals(testRunner.computeOutputs(clientId), actual);
        }
        assertEquals(testRunner.getNumberOfOutputClients(), assertFutures.size());
    }
}
