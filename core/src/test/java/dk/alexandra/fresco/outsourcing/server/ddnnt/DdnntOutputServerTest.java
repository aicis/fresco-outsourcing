package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public class DdnntOutputServerTest {

  private static final int NUM_OUTPUTS = 3;
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
        BigInteger.valueOf(32)
    );
    List<Integer> clientFacingPorts = SpdzSetup.Builder.getFreePorts(numServers);
    List<Future<Object>> assertFutures = runServers(numClients, clientFacingPorts, expectedOutputs);
    runClients(numClients, clientFacingPorts, expectedOutputs);
    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private void runClients(int numClients, List<Integer> clientFacingPorts,
      List<BigInteger> expectedOutputs)
      throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 0; i < clientFacingPorts.size(); i++) {
      servers.add(new Party(i + 1, "localhost", clientFacingPorts.get(i)));
    }
    ExecutorService es = Executors.newFixedThreadPool(8);
    for (int i = 0; i < numClients; i++) {
      final int id = i + 1;
      es.submit(() -> {
        OutputClient client = new DemoDdnntOutputClient(NUM_OUTPUTS, id, servers);
        List<BigInteger> actual = client.getBigIntegerOutputs();
        assertEquals(expectedOutputs, actual);
      });
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.HOURS);
  }

  private List<Future<Object>> runServers(int numClients, List<Integer> clientFacingPorts,
      List<BigInteger> toOutput) {
    Map<Integer, SpdzSetup> setup = SpdzSetup.builder(clientFacingPorts.size()).build();
    ExecutorService es = Executors.newCachedThreadPool();
    Map<Integer, Future<ClientSessionProducer>> clientSessionProducers =
        getClientSessionProducers(numClients, clientFacingPorts, setup, es);
    Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers =
        getServerSessionProducers(setup, es);
    Map<Integer, Future<OutputServer>> outputServers =
        getOutputServers(setup, es, clientSessionProducers, serverSessionProducers);
    List<Future<Object>> assertFutures = getFutureAsserts(setup, es, outputServers, toOutput);
    es.shutdown();
    return assertFutures;
  }

  private List<Future<Object>> getFutureAsserts(Map<Integer, SpdzSetup> setup,
      ExecutorService es, Map<Integer, Future<OutputServer>> outputServers,
      List<BigInteger> toOutput) {
    List<Future<Object>> assertFutures = new ArrayList<>(setup.size());
    for (SpdzSetup s : setup.values()) {
      Future<OutputServer> futureServer = outputServers.get(s.getRp().getMyId());
      Future<Object> assertFuture = es.submit(() -> {
        sendOutputs(futureServer, s, toOutput);
        return null;
      });
      assertFutures.add(assertFuture);
    }
    return assertFutures;
  }

  private Map<Integer, Future<OutputServer>> getOutputServers(Map<Integer, SpdzSetup> setup,
      ExecutorService es, Map<Integer, Future<ClientSessionProducer>> clientSessionProducers,
      Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers) {
    Map<Integer, Future<OutputServer>> inputServers = new HashMap<>(setup.size());
    for (SpdzSetup s : setup.values()) {
      int id = s.getRp().getMyId();
      Future<OutputServer> server = es
          .submit(() -> new DdnntOutputServer<>(clientSessionProducers.get(id).get(),
              serverSessionProducers.get(id).get()));
      inputServers.put(id, server);
    }
    return inputServers;
  }

  private Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> getServerSessionProducers(
      Map<Integer, SpdzSetup> setup, ExecutorService es) {
    Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers =
        new HashMap<>(setup.size());
    for (SpdzSetup s : setup.values()) {
      Future<ServerSessionProducer<SpdzResourcePool>> producer = es
          .submit(() -> new DemoServerSessionProducer(s.getRp(), s.getNetConf()));
      serverSessionProducers.put(s.getRp().getMyId(), producer);
    }
    return serverSessionProducers;
  }

  private Map<Integer, Future<ClientSessionProducer>> getClientSessionProducers(int numClients,
      List<Integer> clientFacingPorts, Map<Integer, SpdzSetup> setup, ExecutorService es) {
    Map<Integer, Future<ClientSessionProducer>> clientSessionProducers =
        new HashMap<>(clientFacingPorts.size());
    for (SpdzSetup s : setup.values()) {
      Future<ClientSessionProducer> producer = es.submit(() -> {
        int port = clientFacingPorts.get(s.getRp().getMyId() - 1);
        return new DemoClientSessionProducer(s.getRp(), SpdzSetupUtils.getDefaultFieldDefinition(),
            port, 0, numClients);
      });
      clientSessionProducers.put(s.getRp().getMyId(), producer);
    }
    return clientSessionProducers;
  }

  private void sendOutputs(Future<OutputServer> futureServer,
      SpdzSetup setup, List<BigInteger> toOutput) {
    try {
      OutputServer server = futureServer.get();
      List<SInt> out = runComputation(setup, toOutput);
      server.putClientOutputs(OUTPUT_CLIENT_ID, out);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  private List<SInt> runComputation(SpdzSetup setup, List<BigInteger> toOutput) {
    Network net = new SocketNetwork(setup.getNetConf());
    return setup.getSce().runApplication((builder) -> {
      DRes<List<DRes<SInt>>> secretShares = builder.collections().closeList(toOutput, 1);
      return () -> secretShares.out().stream().map(DRes::out).collect(Collectors.toList());
    }, setup.getRp(), net);
  }

}
