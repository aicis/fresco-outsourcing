package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DemoDdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
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
 * clients.
 *
 * TODO clean up
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
    List<Integer> clientFacingPorts = SpdzSetup.Builder.getFreePorts(numServers);
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

  private void runServers(int numInputClients, int numOutputClients,
      List<Integer> clientFacingPorts) {
    Map<Integer, SpdzSetup> setup = SpdzSetup.builder(clientFacingPorts.size()).build();
    ExecutorService es = Executors.newCachedThreadPool();
    Map<Integer, Future<DdnntClientSessionProducer>> clientSessionProducers =
        getClientSessionProducers(numInputClients, numOutputClients, clientFacingPorts, setup, es);
    Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers =
        getServerSessionProducers(setup, es);
    Map<Integer, Future<InputServer>> inputServers =
        getInputServers(setup, es, clientSessionProducers, serverSessionProducers);
    Map<Integer, Future<OutputServer>> outputServers =
        getOutputServers(setup, es, clientSessionProducers, serverSessionProducers);
    runServers(setup, es, inputServers, outputServers);
    es.shutdown();
  }

  private void runServers(Map<Integer, SpdzSetup> setup,
      ExecutorService es, Map<Integer, Future<InputServer>> inputServers,
      Map<Integer, Future<OutputServer>> outputServers) {
    for (SpdzSetup s : setup.values()) {
      Future<InputServer> futureInputServer = inputServers.get(s.getRp().getMyId());
      Future<OutputServer> futureOutputServer = outputServers.get(s.getRp().getMyId());
      es.submit(() -> {
        sendOutputs(futureInputServer, futureOutputServer);
      });
    }
  }

  private void sendOutputs(Future<InputServer> futureInputServer,
      Future<OutputServer> futureOutputServer) {
    try {
      InputServer inputServer = futureInputServer.get();
      OutputServer outputServer = futureOutputServer.get();
      Future<Map<Integer, List<SInt>>> futureClientInputs = inputServer
          .getClientInputs();

      outputServer.putClientOutputs(
          OUTPUT_CLIENT_ID,
          futureClientInputs
              .get()
              .get(1));
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  private Map<Integer, Future<InputServer>> getInputServers(Map<Integer, SpdzSetup> setup,
      ExecutorService es, Map<Integer, Future<DdnntClientSessionProducer>> clientSessionProducers,
      Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers) {
    Map<Integer, Future<InputServer>> inputServers = new HashMap<>(setup.size());
    for (SpdzSetup s : setup.values()) {
      int id = s.getRp().getMyId();
      Future<InputServer> server = es
          .submit(() -> new DdnntInputServer<>(clientSessionProducers.get(id).get(),
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

  private Map<Integer, Future<DdnntClientSessionProducer>> getClientSessionProducers(
      int numInputClients, int numOutputClients,
      List<Integer> clientFacingPorts, Map<Integer, SpdzSetup> setup, ExecutorService es) {
    Map<Integer, Future<DdnntClientSessionProducer>> clientSessionProducers =
        new HashMap<>(clientFacingPorts.size());
    for (SpdzSetup s : setup.values()) {
      Future<DdnntClientSessionProducer> producer = es.submit(() -> {
        int port = clientFacingPorts.get(s.getRp().getMyId() - 1);
        return new DemoClientSessionProducer(s.getRp(), SpdzSetupUtils.getDefaultFieldDefinition(),
            port, numInputClients, numOutputClients);
      });
      clientSessionProducers.put(s.getRp().getMyId(), producer);
    }
    return clientSessionProducers;
  }

  private Map<Integer, Future<OutputServer>> getOutputServers(Map<Integer, SpdzSetup> setup,
      ExecutorService es, Map<Integer, Future<DdnntClientSessionProducer>> clientSessionProducers,
      Map<Integer, Future<ServerSessionProducer<SpdzResourcePool>>> serverSessionProducers) {
    Map<Integer, Future<OutputServer>> outputServers = new HashMap<>(setup.size());
    for (SpdzSetup s : setup.values()) {
      int id = s.getRp().getMyId();
      Future<OutputServer> server = es
          .submit(() -> new DdnntOutputServer<>(clientSessionProducers.get(id).get(),
              serverSessionProducers.get(id).get()));
      outputServers.put(id, server);
    }
    return outputServers;
  }

}
