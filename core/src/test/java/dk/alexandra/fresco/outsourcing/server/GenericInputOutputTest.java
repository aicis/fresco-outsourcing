package dk.alexandra.fresco.outsourcing.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients, and send outputs to a single client.
 */
public abstract class GenericInputOutputTest {
    protected abstract InputClient getInputClient(int inputsPerClient, int id, List<Party> servers);

    protected abstract OutputClient getOutputClient(int id, List<Party> servers);

    protected abstract InputServerProducer getInputServerProducer();
    protected abstract OutputServerProducer getOutputServerProducer();
    protected static GenericTestRunner testRunner;

    protected void setTestRunner(TestDataGenerator testDataGenerator) {
        testRunner = new GenericTestRunner(testDataGenerator, (futureServer) -> {
            try {
                SpdzWithIO server = ((Future<SpdzWithIO>) futureServer).get();
                Map<Integer, List<SInt>> clientInputs = server.receiveInputs();
                // Derive the output from the input
                Map<Integer, List<SInt>> clientOutputs = mapToOutputs(clientInputs,
                    testDataGenerator.getNumberOfInputClients(),
                    testDataGenerator.getNumberOfOutputClients(),
                    testDataGenerator.getOutputsPerClient());
                IntStream.range(testDataGenerator.getNumberOfInputClients() + 1,
                    testDataGenerator.getNumberOfInputClients() + 1 + testDataGenerator.getNumberOfOutputClients()).forEach(clientId -> {
                    server.sendOutputsTo(clientId, clientOutputs.get(clientId));
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }, getInputServerProducer(), getOutputServerProducer());
    }

    /**
     * Map client input to oput
     * @param clientInput
     * @param numberOfInputClients
     * @param numberOfOutputClients
     * @param outputsPerClient
     * @return
     */
    protected Map<Integer, List<SInt>> mapToOutputs(Map<Integer, List<SInt>> clientInput,
        int numberOfInputClients, int numberOfOutputClients, int outputsPerClient) {
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
     * The actual test code, which contains futures for the code that must be executed by the
     * input and output clients and servers
     * @throws InterruptedException
     */
    public void testInputsAndOutput() throws InterruptedException {
        List<Integer> freePorts = SpdzSetup.getFreePorts(testRunner.getNumberOfServers() * 3);
        testRunner.runServers(freePorts, SpdzSetupUtils.DEFAULT_MPC_MODULUS);
        // Future for the input client code
        GenericTestRunner.InputClientFunction inputClientFunction = (inputsPerClient, id, servers) -> {
            InputClient inputClient =  getInputClient(inputsPerClient, id, servers);
            List<BigInteger> inputs = testRunner.getTestDataGenerator().computeInputs(id);
            inputClient.putBigIntegerInputs(inputs);
            return null;
        };
        testRunner.runInputClients(SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()), inputClientFunction);

        // Future for the output client code
        GenericTestRunner.OutputClientFunction outputClientFunction = (id, servers) -> {
            OutputClient outputClient = getOutputClient(id, servers);
            List<BigInteger> results = outputClient.getBigIntegerOutputs();
            return results;
        };
        Map<Integer, List<BigInteger>> results = testRunner.runOutputClients(
                SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()), outputClientFunction);
        // Check the result is as expected (i.e. as generated by the test setup)
        for (int clientId : results.keySet()) {
            List<BigInteger> actual = results.get(clientId);
            assertEquals(testRunner.getTestDataGenerator().computeOutputs(clientId), actual);
        }
        assertEquals(testRunner.getNumberOfOutputClients(), results.size());
    }
}
