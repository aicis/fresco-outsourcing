package dk.alexandra.fresco.outsourcing.server;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public abstract class GenericInputServerTest {
  protected abstract SpdzWithIO.Protocol getProtocol();

  protected abstract InputClient getInputClient(int inputsPerClient, int id, List<Party> servers);

  protected static GenericTestRunner testRunner;

  private void setTestRunner(int inputsPerClient, int numberOfInputClients, int numberOfServers) {
    testRunner = new GenericTestRunner(
            getProtocol(),
            inputsPerClient,
            numberOfInputClients,
            0,
            0,
            numberOfServers, (futureServer) -> {
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
    });
  }

  @Test
  public void testManyInputs() throws Exception {
    setTestRunner(100, 10, 3);
    testInputsOnly();
  }

  @Test
  @Ignore // Can't run in the suite since too many threads open up
  public void testManyClients() throws Exception {
    setTestRunner(10, 20, 3);
    testInputsOnly();
  }

  @Test
  @Ignore // Can't run in the suite since too many threads open up
  public void testManyServers() throws Exception {
    setTestRunner(10, 10, 9);
    testInputsOnly();
  }

  public void testInputsOnly() throws InterruptedException, ExecutionException {
    List<Integer> freePorts = SpdzSetup.getFreePorts(testRunner.getNumberOfServers() * 3);
    List<Future<Object>> assertFutures = testRunner.runServers(
            freePorts);
    GenericTestRunner.InputClientFunction inputClientFunction = (inputsPerClient, id, servers) -> {
      return getInputClient(inputsPerClient, id, servers);
    };
    testRunner.runInputClients(testRunner.getNumberOfInputClients(), SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()), inputClientFunction);

    for (Future<Object> assertFuture : assertFutures) {
      Map<Integer, List<BigInteger>> currentServerRes = (Map<Integer, List<BigInteger>>) assertFuture.get();
      for (int clientId : currentServerRes.keySet()) {
        List<BigInteger> actual = currentServerRes.get(clientId);
        assertEquals(testRunner.computeInputs(clientId), actual);
      }
      assertEquals(testRunner.getNumberOfInputClients(), currentServerRes.keySet().size());
    }
    assertEquals(testRunner.getNumberOfServers(), assertFutures.size());
  }
}
