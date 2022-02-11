package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * clients.
 */
public class DdnntOutputServerTest {

  private static final int NUMBER_OF_SERVERS = 3;
  private static final int NUMBER_OF_CLIENTS = 1;
  private static final int OUTPUT_CLIENT_ID = 1;

  @Test
  public void testClientOutput() throws InterruptedException, ExecutionException {
    int numClients = NUMBER_OF_CLIENTS;
    int numServers = NUMBER_OF_SERVERS;
    List<BigInteger> expectedOutputs = Arrays.asList(
        BigInteger.valueOf(0),
        BigInteger.valueOf(1),
        BigInteger.valueOf(42)
    );
    List<Integer> freePorts = SpdzSetup.getFreePorts(NUMBER_OF_SERVERS * 3);
    runServers(numClients, numServers, freePorts, expectedOutputs);
    List<Future<Object>> assertFutures = runOutputClients(numClients,
        SpdzSetup.getClientFacingPorts(freePorts, numServers),
        expectedOutputs);
    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private List<Future<Object>> runOutputClients(int numClients,
      Map<Integer, Integer> clientFacingPorts,
      List<BigInteger> expectedOutputs)
      throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 1; i <= clientFacingPorts.size(); i++) {
      servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
    }
    ExecutorService es = Executors.newFixedThreadPool(8);
    List<Future<Object>> assertFutures = new ArrayList<>(numClients);
    for (int i = 0; i < numClients; i++) {
      final int id = i + 1;
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

  private void runServers(int numClients, int numServers,
      List<Integer> freePorts,
      List<BigInteger> toOutput) {
    ExecutorService es = Executors.newCachedThreadPool();
    List<Integer> serverIds = IntStream.rangeClosed(1, numServers).boxed()
        .collect(Collectors.toList());

    List<Integer> outputIds = IntStream
        .range(OUTPUT_CLIENT_ID, OUTPUT_CLIENT_ID + numClients).boxed()
        .collect(Collectors.toList());

    Map<Integer, Future<SpdzWithIO>> spdzServers = new HashMap<>(numServers);
    for (int serverId : serverIds) {
      Future<SpdzWithIO> spdzServer = es
          .submit(() -> new SpdzWithIO(
              serverId,
              SpdzSetup.getClientFacingPorts(freePorts, numServers),
              SpdzSetup.getInternalPorts(freePorts, numServers),
              SpdzSetup.getApplicationPorts(freePorts, numServers),
              Collections.emptyList(),
              outputIds));
      spdzServers.put(serverId, spdzServer);
    }

    for (int serverId : serverIds) {
      Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
      es.submit(() -> serverSideProtocol(futureServer, toOutput));
    }

    es.shutdown();
  }

  private void serverSideProtocol(Future<SpdzWithIO> futureServer, List<BigInteger> toOutput) {
    try {
      SpdzWithIO server = futureServer.get();
      List<SInt> out = server.run((builder) -> {
        DRes<List<DRes<SInt>>> secretShares = builder.collections().closeList(toOutput, 1);
        return () -> secretShares.out().stream().map(DRes::out).collect(Collectors.toList());
      });
      server.sendOutputsTo(OUTPUT_CLIENT_ID, out);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}
