package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
 * clients.
 */
public class DdnntInputServerTest {

  private static final int INPUTS_PER_CLIENT = 100;
  private static final int NUMBER_OF_SERVERS = 3;
  private static final int NUMBER_OF_INPUT_CLIENTS = 100;
  private static final int NUMBER_OF_OUTPUT_CLIENTS = 1;
  private static final int OUTPUT_CLIENT_ID = NUMBER_OF_INPUT_CLIENTS + NUMBER_OF_OUTPUT_CLIENTS;

  @Test
  public void testInputsOnly() throws InterruptedException, ExecutionException {
    int numInputClients = NUMBER_OF_INPUT_CLIENTS;
    int numOutputClients = NUMBER_OF_OUTPUT_CLIENTS;
    int numServers = NUMBER_OF_SERVERS;
    List<Integer> freePorts = SpdzSetup.getFreePorts(NUMBER_OF_SERVERS * 3);
    List<Future<Object>> assertFutures = runServers(numInputClients, numOutputClients, numServers,
        freePorts);
    runInputClients(numInputClients, SpdzSetup.getClientFacingPorts(freePorts, numServers));

    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private Map<Integer, List<BigInteger>> serverSideProtocol(Future<SpdzWithIO> futureServer) {
    try {
      SpdzWithIO spdz = futureServer.get();
      Map<Integer, List<SInt>> inputs = spdz.receiveInputs();
      Map<Integer, DRes<List<DRes<BigInteger>>>> wrapped =
          spdz.run((builder) -> {
            Map<Integer, DRes<List<DRes<BigInteger>>>> openInputs =
                new HashMap<>(inputs.keySet().size());
            for (Entry<Integer, List<SInt>> e : inputs.entrySet()) {
              DRes<List<DRes<BigInteger>>> partyInputs =
                  dk.alexandra.fresco.lib.common.collections.Collections.using(builder).openList(() -> e.getValue());
              openInputs.put(e.getKey(), partyInputs);
            }
            return () -> openInputs;
          });
      return wrapped.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
          e -> e.getValue().out().stream().map(DRes::out).collect(Collectors.toList())));
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return null;
    }
  }

  private void runInputClients(int numClients, Map<Integer, Integer> clientFacingPorts)
      throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 1; i <= clientFacingPorts.size(); i++) {
      servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
    }
    ExecutorService es = Executors.newFixedThreadPool(8);
    for (int i = 1; i <= numClients; i++) {
      final int id = i;
      es.submit(() -> {
        DdnntInputClient client = new DdnntInputClient(INPUTS_PER_CLIENT, id, servers);
        List<BigInteger> inputs = computeInputs(id);
        client.putBigIntegerInputs(inputs);
      });
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.HOURS);
  }

  private List<BigInteger> computeInputs(int id) {
    return IntStream.range(0, INPUTS_PER_CLIENT)
        .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
  }

  private List<Future<Object>> getFutureAsserts(ExecutorService es,
      Map<Integer, Future<SpdzWithIO>> inputServers, List<Integer> serverIds) {
    List<Future<Object>> assertFutures = new ArrayList<>(inputServers.size());
    for (Integer serverId : serverIds) {
      Future<SpdzWithIO> futureSpdz = inputServers.get(serverId);
      Future<Object> assertFuture = es.submit(() -> {
        Map<Integer, List<BigInteger>> result = serverSideProtocol(futureSpdz);
        for (Entry<Integer, List<BigInteger>> e : result.entrySet()) {
          int clientId = e.getKey();
          for (BigInteger b : e.getValue()) {
            assertEquals(clientId, b.intValue());
          }
        }
        return null;
      });
      assertFutures.add(assertFuture);
    }
    return assertFutures;
  }

  private List<Future<Object>> runServers(int numInputClients,
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

    List<Future<Object>> assertFutures = getFutureAsserts(es, spdzServers, serverIds);

    es.shutdown();
    return assertFutures;
  }

}
