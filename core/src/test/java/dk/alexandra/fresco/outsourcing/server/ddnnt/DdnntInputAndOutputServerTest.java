package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.setup.Spdz;
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
  private static final int BASE_PORT = 8042;

  @Test
  public void testInputsAndOutput() throws InterruptedException, ExecutionException {
    int numInputClients = NUMBER_OF_INPUT_CLIENTS;
    int numOutputClients = NUMBER_OF_OUTPUT_CLIENTS;
    List<Integer> clientFacingPorts = IntStream
        .range(BASE_PORT + 1, BASE_PORT + 1 + NUMBER_OF_SERVERS).boxed()
        .collect(Collectors.toList());
    runServers(numInputClients, numOutputClients,
        clientFacingPorts);
    runInputClients(numInputClients, clientFacingPorts);

    List<Future<Object>> assertFutures = runOutputClients(numOutputClients, clientFacingPorts,
        computeInputs(1));
    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private void runInputClients(int numClients, List<Integer> clientFacingPorts) {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 0; i < clientFacingPorts.size(); i++) {
      servers.add(new Party(i + 1, "localhost", clientFacingPorts.get(i)));
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

  private List<Future<Object>> runOutputClients(int numClients, List<Integer> clientFacingPorts,
      List<BigInteger> expectedOutputs) throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 0; i < clientFacingPorts.size(); i++) {
      servers.add(new Party(i + 1, "localhost", clientFacingPorts.get(i)));
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
      List<Integer> clientFacingPorts) throws InterruptedException {

    ExecutorService es = Executors.newCachedThreadPool();
    List<Integer> serverIds = IntStream.rangeClosed(1, clientFacingPorts.size()).boxed()
        .collect(Collectors.toList());

    List<Integer> inputIds = IntStream.rangeClosed(1, numInputClients).boxed()
        .collect(Collectors.toList());

    List<Integer> outputIds = IntStream
        .range(OUTPUT_CLIENT_ID, OUTPUT_CLIENT_ID + numOutputClients).boxed()
        .collect(Collectors.toList());

    Map<Integer, Future<Spdz>> spdzServers = new HashMap<>(clientFacingPorts.size());
    for (int serverId : serverIds) {
      Future<Spdz> spdzServer = es
          .submit(() -> {
            return new Spdz(
                serverId,
                clientFacingPorts.size(),
                BASE_PORT,
                inputIds,
                outputIds);
          });
      spdzServers.put(serverId, spdzServer);
    }

    for (int serverId : serverIds) {
      Future<Spdz> futureServer = spdzServers.get(serverId);
      es.submit(() -> sendOutputs(futureServer));
    }

    es.shutdown();
  }

  private void sendOutputs(Future<Spdz> futureServer) {
    try {
      Spdz spdz = futureServer.get();
      Map<Integer, List<SInt>> clientInputs = spdz.receiveInputs();
      spdz.sendOutputsTo(OUTPUT_CLIENT_ID, clientInputs.get(1));
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

}
