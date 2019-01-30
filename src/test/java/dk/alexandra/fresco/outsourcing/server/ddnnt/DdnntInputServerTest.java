package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.network.AsyncNetwork;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.SpdzTestSetup;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
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

public class DdnntInputServerTest {

  private static final int INPUTS_PER_CLIENT = 1000;
  private static final int NUMBER_OF_SERVERS = 3;
  private static final int NUMBER_OF_CLIENTS = 100;

  @Test
  public void test() throws InterruptedException, ExecutionException {
    int numClients = NUMBER_OF_CLIENTS;
    int numServers = NUMBER_OF_SERVERS;
    List<Integer> clientFacingPorts = SpdzTestSetup.Builder.getFreePorts(numServers);
    System.out.println("Run servers");
    List<Future<Object>> assertFutures = runServers(numClients, clientFacingPorts);
    System.out.println("Run clients");
    runClients(numClients, clientFacingPorts);
    for (Future<Object> assertFuture : assertFutures) {
      assertFuture.get();
    }
  }

  private void runClients(int numClients, List<Integer> clientFacingPorts)
      throws InterruptedException {
    List<Party> servers = new ArrayList<>(clientFacingPorts.size());
    for (int i = 0; i < clientFacingPorts.size(); i++) {
      servers.add(new Party(i + 1, "localhost", clientFacingPorts.get(i)));
    }
    ExecutorService es = Executors.newFixedThreadPool(8);
    for (int i = 0; i < numClients; i++) {
      final int id = i;
      es.submit(() -> {
        DemoDdnntInputClient client = new DemoDdnntInputClient(INPUTS_PER_CLIENT, id, servers);
        List<BigInteger> inputs = IntStream.range(0, INPUTS_PER_CLIENT)
            .mapToObj(num -> BigInteger.valueOf(id)).collect(Collectors.toList());
        client.putBigIntegerInputs(inputs);
      });
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.HOURS);
  }

  private List<Future<Object>> runServers(int numClients, List<Integer> clientFacingPorts)
      throws InterruptedException, ExecutionException {
    Map<Integer, SpdzTestSetup> setup = SpdzTestSetup.builder(clientFacingPorts.size()).build();
    ExecutorService es = Executors.newCachedThreadPool();
    Map<Integer, Future<ClientSessionProducer>> clientSessionProducers =
        getClientSessionProducers(numClients, clientFacingPorts, setup, es);
    Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers =
        getServerSessionProducers(setup, es);
    Map<Integer, Future<InputServer>> inputServers =
        getInputServers(setup, es, clientSessionProducers, serverSessionProducers);
    List<Future<Object>> assertFutures = getFutureAsserts(setup, es, inputServers);
    es.shutdown();
    return assertFutures;
  }

  private List<Future<Object>> getFutureAsserts(Map<Integer, SpdzTestSetup> setup,
      ExecutorService es, Map<Integer, Future<InputServer>> inputServers) {
    List<Future<Object>> assertFutures = new ArrayList<>(setup.size());
    for (SpdzTestSetup s : setup.values()) {
      Future<InputServer> futureServer = inputServers.get(s.getRp().getMyId());
      Future<Object> assertFuture = es.submit(() -> {
        Map<Integer, List<BigInteger>> result = openInputs(futureServer, s);
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

  private Map<Integer, Future<InputServer>> getInputServers(Map<Integer, SpdzTestSetup> setup,
      ExecutorService es, Map<Integer, Future<ClientSessionProducer>> clientSessionProducers,
      Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers) {
    Map<Integer, Future<InputServer>> inputServers = new HashMap<>(setup.size());
    for (SpdzTestSetup s : setup.values()) {
      int id = s.getRp().getMyId();
      Future<InputServer> server = es.submit(() -> {
        return new DdnntInputServer<>(clientSessionProducers.get(id).get(),
            serverSessionProducers.get(id).get());
      });
      inputServers.put(id, server);
    }
    return inputServers;
  }

  private Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> getServerSessionProducers(
      Map<Integer, SpdzTestSetup> setup, ExecutorService es) {
    Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers =
        new HashMap<>(setup.size());
    for (SpdzTestSetup s : setup.values()) {
      Future<ServerSessionProducer<SpdzResourcePool>> producer = es.submit(() -> {
        return new DemoServerSessionProducer(s.getRp(), s.getNetConf());
      });
      serverSessionProducers.put(s.getRp().getMyId(), producer);
    }
    return serverSessionProducers;
  }

  private Map<Integer, Future<ClientSessionProducer>> getClientSessionProducers(int numClients,
      List<Integer> clientFacingPorts, Map<Integer, SpdzTestSetup> setup, ExecutorService es) {
    Map<Integer, Future<ClientSessionProducer>> clientSessionProducers =
        new HashMap<>(clientFacingPorts.size());
    for (SpdzTestSetup s : setup.values()) {
      Future<ClientSessionProducer> producer = es.submit(() -> {
        int port = clientFacingPorts.get(s.getRp().getMyId() - 1);
        return new DemoClientSessionProducer(s.getRp(), port, numClients);
      });
      clientSessionProducers.put(s.getRp().getMyId(), producer);
    }
    return clientSessionProducers;
  }

  private Map<Integer, List<BigInteger>> openInputs(Future<InputServer> futureServer,
      SpdzTestSetup setup) {
    Map<Integer, List<SInt>> inputs;
    try {
      InputServer server = futureServer.get();
      inputs = server.getClientInputs().get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return null;
    }
    AsyncNetwork net = new AsyncNetwork(setup.getNetConf());
    Map<Integer, DRes<List<DRes<BigInteger>>>> wrapped =
        setup.getSce().runApplication((builder) -> {
          Map<Integer, DRes<List<DRes<BigInteger>>>> openInputs =
              new HashMap<>(inputs.keySet().size());
          for (Entry<Integer, List<SInt>> e : inputs.entrySet()) {
            DRes<List<DRes<BigInteger>>> partyInputs =
                builder.collections().openList(() -> e.getValue());
            openInputs.put(e.getKey(), partyInputs);
          }
          return () -> openInputs;
        }, setup.getRp(), net);
    Map<Integer, List<BigInteger>> openedInputs =
        wrapped.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            e -> e.getValue().out().stream().map(DRes::out).collect(Collectors.toList())));
    return openedInputs;
  }

}
