package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputServerTest;
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
public class DdnntInputServerTest extends GenericInputServerTest {

  private static final int INPUTS_PER_CLIENT = 100;
  private static final int NUMBER_OF_SERVERS = 3;
  private static final int NUMBER_OF_INPUT_CLIENTS = 100;
  private static final int NUMBER_OF_OUTPUT_CLIENTS = 1;
  private static final int OUTPUT_CLIENT_ID = NUMBER_OF_INPUT_CLIENTS + NUMBER_OF_OUTPUT_CLIENTS;
  
  // TODO handle these methods
  @Override
  protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.DDNNT;
  }

  @Override
  protected InputClient getInputClient(int inputsPerClient, int id, List<Party> servers) {
        return new DdnntInputClient(inputsPerClient, id, servers);
  }
   
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
        DemoDdnntInputClient client = new DemoDdnntInputClient(INPUTS_PER_CLIENT, id, servers);
        List<BigInteger> inputs = computeInputs(id);
        client.putBigIntegerInputs(inputs);
      });
    }
    es.shutdown();
    es.awaitTermination(1, TimeUnit.HOURS);
  }
}
