package dk.alexandra.fresco.outsourcing.server;

import static org.junit.Assert.assertEquals;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.setup.SpdzSetup;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public abstract class GenericOutputServerTest {

  protected abstract SpdzWithIO.Protocol getProtocol();

  protected abstract OutputClient getOutputClient(int id, List<Party> servers);

  protected static GenericTestRunner testRunner;

  protected void setTestRunner(int outputsPerClient, int numberOfOutputClients, int numberOfServers) {
    testRunner = new GenericTestRunner(
            getProtocol(),
            0,
            0,
            outputsPerClient,
            numberOfOutputClients,
            numberOfServers, (futureServer) -> {
      try {
        SpdzWithIO server = futureServer.get();
        for (int clientId = 1; clientId < 1 + numberOfOutputClients; clientId++) {
          int finalClientId = clientId;
          List<SInt> out = server.run((builder) -> {
            DRes<List<DRes<SInt>>> secretShares;
            if (server.getServerId() == 1) {
              secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder)
                      .closeList(computeOutputs(finalClientId, outputsPerClient), server.getServerId());
            } else {
              secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder)
                      .closeList(computeOutputs(finalClientId, outputsPerClient).size(), 1);
            }
            return () -> secretShares.out().stream().map(DRes::out).collect(Collectors.toList());
          });
          server.sendOutputsTo(clientId, out);
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
      return null;
    });
  }

  // The output will be the input of the first party
  private List<BigInteger> computeOutputs(int id, int numberOfOutputs) {
    return IntStream.range(0, numberOfOutputs)
            .mapToObj(num -> BigInteger.valueOf(1)).collect(Collectors.toList());
  }

  @Test
  public void testManyInputs() throws Exception {
    setTestRunner(100, 3, 3);
    testClientOutput();
  }

  @Test
  public void testManyClients() throws Exception {
    setTestRunner(10, 7, 3);
    testClientOutput();
  }

  @Test
  public void testManyServers() throws Exception {
    setTestRunner(10, 3, 10);
    testClientOutput();
  }

  public void testClientOutput() throws InterruptedException, ExecutionException {
    List<Integer> freePorts = SpdzSetup.getFreePorts(testRunner.getNumberOfServers() * 3);
    testRunner.runServers(freePorts);

    GenericTestRunner.OutputClientFunction outputClientFunction = (id, servers) -> {
      return getOutputClient(id, servers);
    };
    Map<Integer, Future<Object>> assertFutures = testRunner.runOutputClients(
            SpdzSetup.getClientFacingPorts(freePorts, testRunner.getNumberOfServers()),
            outputClientFunction);
    for (int clientId : assertFutures.keySet()) {
      List<BigInteger> actual = (List<BigInteger>) assertFutures.get(clientId).get();
      assertEquals(testRunner.computeOutputs(clientId), actual);
    }
    assertEquals(testRunner.getNumberOfOutputClients(), assertFutures.size());
  }
}
