package dk.alexandra.fresco.outsourcing.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public abstract class GenericInputServerTest {
  private static final Logger logger = LoggerFactory.getLogger(GenericInputServerTest.class);
  protected abstract InputClient getInputClient(int inputsPerClient, int id, List<Party> servers);
  protected abstract InputServerProducer getInputServerProducer();
  protected GenericTestRunner testRunner;

  protected void setTestRunner(TestDataGenerator testDataGenerator) {
    testRunner = new GenericTestRunner(testDataGenerator, (futureServer) -> {
      try {
        SpdzWithIO spdz = ((Future<SpdzWithIO>) futureServer).get();
        Map<Integer, List<SInt>> inputs = spdz.receiveInputs();
        // Wrapping is used here to make sure we can get the client input back out from the
        // server computation
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
        logger.error("Server evaluation failed!", e);
        e.printStackTrace();
        return null;
      }
    }, getInputServerProducer(), null);
  }

  /**
   * Run the actual test through futures of the input client and server. Output is not run
   * through a client but wrapped from the data from the input client
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void testInputsOnly() throws InterruptedException, ExecutionException {
    List<Integer> freePorts = SpdzSetup.getFreePorts(testRunner.getNumberOfServers() * 3);
    Future<Map<Integer, Future<List<BigInteger>>>> assertFutures = testRunner.runServers(
            freePorts, testRunner.getTestDataGenerator().getModulus());
    // Future input client code
    GenericTestRunner.InputClientFunction inputClientFunction = (inputsPerClient, id, servers) -> {
      InputClient inputClient =  getInputClient(inputsPerClient, id, servers);
      List<BigInteger> inputs = testRunner.getTestDataGenerator().computeInputs(id);
      inputClient.putBigIntegerInputs(inputs);
      return null;
    };
    testRunner.runInputClients(SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()), inputClientFunction);

    // Validate the result is consistent with how the test data is generated
    for (Future<List<BigInteger>> assertFuture : assertFutures.get().values()) {
      Map<Integer, List<BigInteger>> currentServerRes = (Map<Integer, List<BigInteger>>) assertFuture.get();
      for (int clientId : currentServerRes.keySet()) {
        List<BigInteger> actual = currentServerRes.get(clientId);
        assertEquals(testRunner.getTestDataGenerator().computeInputs(clientId), actual);
      }
      assertEquals(testRunner.getNumberOfInputClients(), currentServerRes.keySet().size());
    }
    assertEquals(testRunner.getNumberOfServers(), assertFutures.get().size());
  }
}
