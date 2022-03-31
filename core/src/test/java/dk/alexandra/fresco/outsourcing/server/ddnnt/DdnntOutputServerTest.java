package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntOutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public class DdnntOutputServerTest {

  private static final int NUMBER_OF_SERVERS = 3;
  private static final int NUMBER_OF_CLIENTS = 10;
  private static final int FIRST_OUTPUT_CLIENT_ID = 1;

  @Test
  public void testClientOutput() throws InterruptedException, ExecutionException {
    int numClients = NUMBER_OF_CLIENTS;
    int numServers = NUMBER_OF_SERVERS;
    Map<Integer, List<BigInteger>> expectedOutputs = new HashMap<>();
    List<Integer> outputIds = IntStream
            .range(FIRST_OUTPUT_CLIENT_ID, FIRST_OUTPUT_CLIENT_ID + numClients).boxed()
            .collect(Collectors.toList());
    for (int id : outputIds) {
      expectedOutputs.put(id, Arrays.asList(
              BigInteger.valueOf(0),
              BigInteger.valueOf(id),
              BigInteger.valueOf(42))
      );
    }
    List<Integer> freePorts = SpdzSetup.getFreePorts(NUMBER_OF_SERVERS * 3);
    runServers(numServers, freePorts, expectedOutputs);
    List<Future<Object>> assertFutures = runOutputClients(
            SpdzSetup.getClientFacingPorts(freePorts, numServers),
        expectedOutputs);
    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private List<Future<Object>> runOutputClients(Map<Integer, Integer> clientFacingPorts, Map<Integer, List<BigInteger>> expectedOutputs)
      throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 1; i <= clientFacingPorts.size(); i++) {
      servers.add(new Party(i, "localhost", clientFacingPorts.get(i)));
    }
    ExecutorService es = Executors.newFixedThreadPool(8);
    List<Future<Object>> assertFutures = new ArrayList<>(expectedOutputs.size());
    for (int clientId : expectedOutputs.keySet()) {
      Future<Object> assertFuture = es.submit(() -> {
        OutputClient client = new DdnntOutputClient(clientId, servers);
        List<BigInteger> actual = client.getBigIntegerOutputs();
        assertEquals(expectedOutputs.get(clientId), actual);
        return null;
      });
      assertFutures.add(assertFuture);
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.HOURS);
    return assertFutures;
  }

  private void runServers(int numServers,
                          List<Integer> freePorts, Map<Integer, List<BigInteger>> toOutput) {
    ExecutorService es = Executors.newCachedThreadPool();
    List<Integer> serverIds = IntStream.rangeClosed(1, numServers).boxed()
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
              new ArrayList<>(toOutput.keySet())));
      spdzServers.put(serverId, spdzServer);
    }

    for (int serverId : serverIds) {
      Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
      es.submit(() -> serverSideProtocol(futureServer, toOutput));
    }

    es.shutdown();
  }

  private void serverSideProtocol(Future<SpdzWithIO> futureServer, Map<Integer, List<BigInteger>> toOutput) {
    try {
      SpdzWithIO server = futureServer.get();
      for (int clientId : toOutput.keySet()) {
        List<SInt> out = server.run((builder) -> {
          DRes<List<DRes<SInt>>> secretShares;
          if (server.getServerId() == 1) {
            secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder)
                    .closeList(toOutput.get(clientId), server.getServerId());
          } else {
            secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder)
                    .closeList(toOutput.get(clientId).size(), 1);
          }
          return () -> secretShares.out().stream().map(DRes::out).collect(Collectors.toList());
        });
        server.sendOutputsTo(clientId, out);
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}
