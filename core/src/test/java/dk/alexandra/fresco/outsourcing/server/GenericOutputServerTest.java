package dk.alexandra.fresco.outsourcing.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.OutputServerProducer;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public abstract class GenericOutputServerTest {
  private static final Logger logger = LoggerFactory.getLogger(GenericOutputServerTest.class);
  protected abstract OutputClient getOutputClient(int id, List<Party> servers);
  protected abstract OutputServerProducer getOutputServerProducer();
  protected GenericTestRunner testRunner;

  protected void setTestRunner(TestDataGenerator testDataGenerator) {
    testRunner = new GenericTestRunner(testDataGenerator, (futureServer) -> {
      try {
        SpdzWithIO server = ((Future<SpdzWithIO>) futureServer).get();
        // Emulate the protocol by simply giving the data that should be output, as input into an
        // MPC computation
        for (int clientId = 1; clientId < 1 + testDataGenerator.getNumberOfOutputClients(); clientId++) {
          int finalClientId = clientId;
          List<SInt> out = server.run((builder) -> {
            DRes<List<DRes<SInt>>> secretShares;
            if (server.getServerId() == 1) {
              secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder)
                      .closeList(testRunner.getTestDataGenerator().computeOutputs(finalClientId), server.getServerId());
            } else {
              secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder)
                      .closeList(testRunner.getTestDataGenerator().computeOutputs(finalClientId).size(), 1);
            }
            return () -> secretShares.out().stream().map(DRes::out).collect(Collectors.toList());
          });
          server.sendOutputsTo(clientId, out);
        }
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Server evaluation failed!", e);
        e.printStackTrace();
      }
      return null;
    }, null,  getOutputServerProducer());
  }

  /**
   * Runs the actual test code through a future output client and check the result.
   * @throws InterruptedException
   */
  public void testClientOutput() throws Exception {
    List<Integer> freePorts = SpdzSetup.getFreePorts(testRunner.getNumberOfServers() * 3);
    testRunner.runServers(freePorts,testRunner.testDataGenerator.getModulus());

    // The future running the output client code
    GenericTestRunner.OutputClientFunction outputClientFunction = (id, servers) -> {
      OutputClient outputClient = getOutputClient(id, servers);
      List<BigInteger> results = outputClient.getBigIntegerOutputs();
      return results;
    };
    Map<Integer, List<BigInteger>> results = (Map<Integer, List<BigInteger>>) testRunner.runOutputClients(
            SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()),
            outputClientFunction).get();
    // Validate the data output from the computation against what is expected to be simulated
    for (int clientId : results.keySet()) {
      List<BigInteger> actual = results.get(clientId);
      assertEquals(testRunner.getTestDataGenerator().computeOutputs(clientId), actual);
    }
    assertEquals(testRunner.getNumberOfOutputClients(), results.size());
  }

}
