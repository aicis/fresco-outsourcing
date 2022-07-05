package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
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

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public abstract class GenericOutputServerTest {

  protected static int NUMBER_OF_SERVERS;
  protected static int NUMBER_OF_CLIENTS;
  protected static int NUMBER_OF_OUTPUTS;

  protected abstract SpdzWithIO.Protocol getProtocol();

  protected abstract OutputClient getOutputClient(int id, List<Party> servers);

  @Test
  public void testManyInputs() throws Exception {
    NUMBER_OF_OUTPUTS = 100;
    NUMBER_OF_SERVERS = 3;
    NUMBER_OF_CLIENTS = 10;
    testClientOutput();
  }

  @Test
  public void testManyClients() throws Exception {
    NUMBER_OF_OUTPUTS = 10;
    NUMBER_OF_SERVERS = 3;
    NUMBER_OF_CLIENTS = 30;
    testClientOutput();
  }

  @Test
  public void testManyServers() throws Exception {
    NUMBER_OF_OUTPUTS = 10;
    NUMBER_OF_SERVERS = 10;
    NUMBER_OF_CLIENTS = 10;
    testClientOutput();
  }

  public void testClientOutput() throws InterruptedException, ExecutionException {
    int numClients = NUMBER_OF_CLIENTS;
    int numServers = NUMBER_OF_SERVERS;
    Map<Integer, List<BigInteger>> expectedOutputs = new HashMap<>();
    List<Integer> outputIds = IntStream
            .range(1, 1 + numClients).boxed()
            .collect(Collectors.toList());
    for (int id : outputIds) {
      expectedOutputs.put(id, computeOutputs(id));
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

  /**
   * Let the output simply be the ID of the party
   */
  private List<BigInteger> computeOutputs(int id) {
    return IntStream.range(0, NUMBER_OF_OUTPUTS)
            .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
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
        OutputClient client = getOutputClient(clientId, servers);
        List<BigInteger> actual = client.getBigIntegerOutputs();
        assertEquals(expectedOutputs.get(clientId), actual);
        return null;
      });
      assertFutures.add(assertFuture);
    }
    es.shutdown();
    es.awaitTermination(5, TimeUnit.SECONDS);
    return assertFutures;
  }

  private void runServers(int numServers,
                          List<Integer> freePorts, Map<Integer, List<BigInteger>> toOutput) throws InterruptedException {
    ExecutorService es = Executors.newCachedThreadPool();
    List<Integer> serverIds = IntStream.rangeClosed(1, numServers).boxed()
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
                      Collections.emptyList(),
                      new ArrayList<>(toOutput.keySet()),
                      SpdzSetupUtils.getLocalhostMap(internalPorts),
                      SpdzSetupUtils.DEFAULT_BITLENGTH, true, getProtocol()));
      spdzServers.put(serverId, spdzServer);
    }

    for (int serverId : serverIds) {
      Future<SpdzWithIO> futureServer = spdzServers.get(serverId);
      es.submit(() -> serverSideProtocol(futureServer, toOutput));
    }

    es.shutdown();
    es.awaitTermination(5, TimeUnit.SECONDS);
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
