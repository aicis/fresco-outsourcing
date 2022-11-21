package dk.alexandra.fresco.outsourcing.server;

import static java.lang.Thread.sleep;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.TestUtil;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test framework class that uses future to setup input and output clients, along with servers.
 * Note that this process spawns many threads, as is needed to allow for network communication
 * between all the different entities on the same machine.
 * @param <ClientInputT> The state that should be returned from the input client after ending the
 *                     input phase.
 * @param <ClientOutputT> The state that should be returned from the output client after ending the
 *  *                     output phase.
 * @param <ServerOutputT> The state that should be returned from the server after running the
 *                       entire protocol.
 */
public class GenericTestRunner<ClientInputT, ClientOutputT, ServerOutputT> {
    private final Map<Integer, Future<ClientInputT>> futureClientState = new TreeMap<>();
    protected final TestDataGenerator testDataGenerator;
    protected final Function<Future<SpdzWithIO>, ServerOutputT> serverSideProtocol;
    protected final SpdzSetupUtils.InputServerProducer inputServerProducer;
    protected final SpdzSetupUtils.OutputServerProducer outputServerProducer;

    /**
     * Sets up the test runner. NOTE that only a single test can be executed per instance of this
     * class.
     * @param testDataGenerator Class for generating the parameters for the test
     * @param serverSideProtocol The lambda that will be run by the server in order to execute
     *                           the MPC computation. It will use access to the lambdas
     *                           {@code inputServerProducer} and {@code outputServerProducer}
     *                           through a {@link SpdzWithIO} object.
     * @param inputServerProducer The lambda that the server must execute as part of the client
     *                            input phase.
     * @param outputServerProducer The lambda that the server must execute as part of the client
     *                             output phase.
     */
    public GenericTestRunner(TestDataGenerator testDataGenerator, Function<Future<SpdzWithIO>,
        ServerOutputT> serverSideProtocol, SpdzSetupUtils.InputServerProducer inputServerProducer,
        SpdzSetupUtils.OutputServerProducer outputServerProducer) {
        this.testDataGenerator = testDataGenerator;
        this.serverSideProtocol = serverSideProtocol;
        this.inputServerProducer = inputServerProducer;
        this.outputServerProducer = outputServerProducer;
    }

    public TestDataGenerator getTestDataGenerator() {
        return testDataGenerator;
    }

    public int getInputsPerClient() {
        return testDataGenerator.getInputsPerClient();
    }

    public int getOutputsPerClient() {
        return testDataGenerator.getOutputsPerClient();
    }

    public int getNumberOfServers() {
        return testDataGenerator.getNumberOfServers();
    }

    public int getNumberOfInputClients() {
        return testDataGenerator.getNumberOfInputClients();
    }

    public int getNumberOfOutputClients() {
        return testDataGenerator.getNumberOfOutputClients();
    }

    /**
     * Helper function to get all futures in the value position of a key/value map evaluated.
     * @param futureMap the map to evaluate
     * @return {@code futureMap} evaluated
     * @param <K> The key type of the map
     * @param <V> The value type of the map
     */
    private <K, V> Map<K, V> getFutures(Map<K, Future<V>> futureMap) {
        return ExceptionConverter.safe(() -> {
            Map<K, V> res = new TreeMap<>();
            for (Entry<K, Future<V>> cur : futureMap.entrySet() ) {
                res.put(cur.getKey(), cur.getValue().get());
            }
            return res;
        }, "Could not convert future map");
    }

    /**
     * Method for returning an optional client state as a result of evaluated the client input
     * protocol.
     * @return The state of the client after the input protocol.
     */
    public synchronized Map<Integer, Future<ClientInputT>> getFutureClientState() {
        return TestUtil.blockingEvaluation(() -> {
            while (futureClientState.size() < getNumberOfInputClients()) {
                // Wait 50 ms before trying again to ensure that thread scheduling can happen
                sleep(50);
            }
            return futureClientState;
        });
    }

    /**
     * Method for setting up and running the client code in {@code inputClientFunction}.
     * This is a non-blocking method.
     *
     * @param clientFacingPorts   Map of server ID to a port the client can use.
     * @param inputClientFunction The lambda to execute per server to do client input
     * @throws InterruptedException
     */
    public void runInputClients(Map<Integer, Integer> clientFacingPorts,
        InputClientFunction<ClientInputT> inputClientFunction) {
        // Run asynchronously
        TestUtil.nonblockingEvaluation( () -> {
            List<Party> servers = new ArrayList(clientFacingPorts.size());
            for (int i = 1; i <= clientFacingPorts.size(); ++i) {
                servers.add(new Party(i, "localhost", (Integer) clientFacingPorts.get(i)));
            }
            ExecutorService es = Executors.newFixedThreadPool(8);
            // Run each client in a new thread
            for (int i = 0; i < getNumberOfInputClients(); ++i) {
                int id = i + 1;
                Future<ClientInputT> inputRes = es.submit(() -> {
                    return inputClientFunction.apply(getInputsPerClient(), id, servers);
                });
                futureClientState.put(id, inputRes);
            }
            es.shutdown();
            if (!es.awaitTermination(TestUtil.TIMEOUT_IN_SEC, TimeUnit.SECONDS)) {
                throw new RuntimeException("Callable task did not complete successfully!");
            }
            return null;
        });
    }

    /**
     * Runs the output client code.
     * This is a non-blocking method.
     *
     * @param clientFacingPorts client id to port map for the output clients.
     * @param outputClientFunction the lambda to execute per output client
     * @return a map of client id to output
     * @throws InterruptedException
     */
    public Future<Map<Integer, ClientOutputT>> runOutputClients(Map<Integer, Integer> clientFacingPorts,
        OutputClientFunction<ClientOutputT> outputClientFunction) {
        // Run asynchronously
        return TestUtil.nonblockingEvaluation( () -> {
            List<Party> servers = new ArrayList(clientFacingPorts.size());

            for (int i = 1; i <= clientFacingPorts.size(); ++i) {
                servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
            }

            ExecutorService es = Executors.newFixedThreadPool(8);
            Map<Integer, Future<ClientOutputT>> outputFuture = new HashMap();
            for (int i = 0; i < this.getNumberOfOutputClients(); ++i) {
                int id = i + this.getNumberOfInputClients() + 1;
                Future<ClientOutputT> outputRes = es.submit(() -> {
                    return outputClientFunction.apply(id, servers);
                });
                outputFuture.put(id, outputRes);
            }
            es.shutdown();
            if (!es.awaitTermination(TestUtil.TIMEOUT_IN_SEC, TimeUnit.SECONDS)) {
                throw new RuntimeException("Callable task did not complete successfully!");
            }
            return getFutures(outputFuture);
        });
    }

    /**
     * Runs the MPC servers by using {@link SpdzWithIO} to setup a server.
     * This is a non-blocking method.
     *
     * @param freePorts List of free ports, to be used to run the MPC protocol. MUST be
     *                  3 * the amount of servers.
     * @param modulus   The modulus to use for the SPDZ MPC computation.
     * @return A map of server id to futures containing the server output of the computation.
     * @throws InterruptedException
     */
    public Future<Map<Integer, Future<ServerOutputT>>> runServers(List<Integer> freePorts,
        BigInteger modulus) {
        // Run asynchronously
        return TestUtil.nonblockingEvaluation( () -> {
            if (freePorts.size() < 3 * getNumberOfServers()) {
                throw new RuntimeException(
                    "There must be at least 3 times the amount of servers open"
                        + " ports");
            }
            ExecutorService es = Executors.newCachedThreadPool();
            // Server ID is contiguous and starts at 1
            List<Integer> serverIds = (List) IntStream.rangeClosed(1, this.getNumberOfServers())
                .boxed().collect(
                    Collectors.toList());
            // Input client ID is contiguous and starts at 1
            List<Integer> inputIds = (List) IntStream.rangeClosed(1, this.getNumberOfInputClients())
                .boxed().collect(Collectors.toList());
            // Output client ID is contiguous and starts at the amount of input clients + 1
            List<Integer> outputIds = (List) IntStream.range(this.getNumberOfInputClients() + 1,
                    this.getNumberOfInputClients() + 1 + this.getNumberOfOutputClients()).boxed()
                .collect(Collectors.toList());
            Map<Integer, Future<SpdzWithIO>> spdzServers = new HashMap(this.getNumberOfServers());
            Map<Integer, Integer> internalPorts = SpdzSetup.getInternalPorts(freePorts,
                this.getNumberOfServers());
            Iterator servers = serverIds.iterator();

            while (servers.hasNext()) {
                int serverId = (Integer) servers.next();
                // Setup servers using SpdzWithIO
                Future<SpdzWithIO> spdzServer = es.submit(() -> {
                    return new SpdzWithIO(serverId,
                        SpdzSetup.getClientFacingPorts(freePorts, this.getNumberOfServers()),
                        internalPorts,
                        SpdzSetup.getApplicationPorts(freePorts, this.getNumberOfServers()),
                        inputIds, outputIds, SpdzSetupUtils.getLocalhostMap(internalPorts),
                        this.inputServerProducer, this.outputServerProducer,
                        modulus, true,
                        testDataGenerator.getProtocol());
                });
                spdzServers.put(serverId, spdzServer);
            }

            Map<Integer, Future<ServerOutputT>> assertFutures = new HashMap<>();
            Iterator receivingServers = serverIds.iterator();

            // Run the server side protocol and get the future result
            while (receivingServers.hasNext()) {
                int serverId = (Integer) receivingServers.next();
                Future<SpdzWithIO> futureServer = (Future) spdzServers.get(serverId);
                assertFutures.put(serverId, es.submit(() -> {
                    return serverSideProtocol.apply(futureServer);
                }));
            }

            es.shutdown();
            es.awaitTermination(TestUtil.TIMEOUT_IN_SEC, TimeUnit.SECONDS);
            return assertFutures;
        });
    }

    /**
     * Functional interface the client output function.
     * @param <OutputT> The type of output the output client should return.
     */
    @FunctionalInterface
    public interface OutputClientFunction<OutputT> {
        OutputT apply(int id, List<Party> servers);
    }

    /**
     * Functional interface the client input lambda.
     * @param <InputT> The type of data that should be returned after running the input client.
     */
    @FunctionalInterface
    public interface InputClientFunction<InputT> {
        InputT apply(int inputsPerClient, int id, List<Party> servers);
    }
}
