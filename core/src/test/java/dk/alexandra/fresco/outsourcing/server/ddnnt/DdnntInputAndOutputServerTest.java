package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients, and send outputs to a single client.
 */
public class DdnntInputAndOutputServerTest {

  private static final int INPUTS_PER_CLIENT = 100;
  private static final int NUMBER_OF_SERVERS = 3;
  private static final int NUMBER_OF_INPUT_CLIENTS = 10;
  private static final int NUMBER_OF_OUTPUT_CLIENTS = 1;
  private static final int OUTPUT_CLIENT_ID = NUMBER_OF_INPUT_CLIENTS + NUMBER_OF_OUTPUT_CLIENTS;

  @Test
  public void testInputsAndOutput() throws InterruptedException, ExecutionException {
    int numInputClients = NUMBER_OF_INPUT_CLIENTS;
    int numOutputClients = NUMBER_OF_OUTPUT_CLIENTS;
    int numServers = NUMBER_OF_SERVERS;
    List<Integer> freePorts = SpdzSetup.getFreePorts(NUMBER_OF_SERVERS * 3);
    runServers(numInputClients, numOutputClients,
        numServers, freePorts);
    runInputClients(numInputClients, SpdzSetup.getClientFacingPorts(freePorts, numServers));

    List<Future<Object>> assertFutures = runOutputClients(numOutputClients,
        SpdzSetup.getClientFacingPorts(freePorts, numServers),
        computeInputs(1));
    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private void serverSideProtocol(Future<SpdzWithIO> futureServer) {
    try {
      SpdzWithIO spdz = futureServer.get();
      Map<Integer, List<SInt>> clientInputs = spdz.receiveInputs();
      spdz.sendOutputsTo(OUTPUT_CLIENT_ID, clientInputs.get(1));
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
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
        DemoDdnntInputClient client = new DemoDdnntInputClient(INPUTS_PER_CLIENT, id, servers);
        List<BigInteger> inputs = computeInputs(id);
        client.putBigIntegerInputs(inputs);
      });
    }
    es.shutdown();
  }

  private List<BigInteger> computeInputs(int id) {
    return IntStream.range(0, INPUTS_PER_CLIENT)
        .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
  }

  private List<Future<Object>> runOutputClients(int numClients,
      Map<Integer, Integer> clientFacingPorts,
      List<BigInteger> expectedOutputs) throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 1; i <= clientFacingPorts.size(); i++) {
      servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
    }
    ExecutorService es = Executors.newFixedThreadPool(8);
    List<Future<Object>> assertFutures = new ArrayList<>(numClients);
    for (int i = 0; i < numClients; i++) {
      final int id = i + OUTPUT_CLIENT_ID;
      Future<Object> assertFuture = es.submit(() -> {
        OutputClient client = new DemoDdnntOutputClient(id, servers);
        List<BigInteger> actual = client.getBigIntegerOutputs();
        assertEquals(expectedOutputs, actual);
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
        .range(OUTPUT_CLIENT_ID, OUTPUT_CLIENT_ID + numOutputClients).boxed()
        .collect(Collectors.toList());

    Map<Integer, Future<SpdzWithIO>> spdzServers = new HashMap<>(numServers);
    for (int serverId : serverIds) {
      Future<SpdzWithIO> spdzServer = es
          .submit(() -> new SpdzWithIO(
              serverId,
              SpdzSetup.getClientFacingPorts(freePorts, numServers),
              SpdzSetup.getInternalPorts(freePorts, numServers),
              SpdzSetup.getApplicationPorts(freePorts, numServers),
              inputIds,
              outputIds));
      spdzServers.put(serverId, spdzServer);
    }

    for (int serverId : serverIds) {
      Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
      es.submit(() -> serverSideProtocol(futureServer));
    }

    es.shutdown();
  }

}
