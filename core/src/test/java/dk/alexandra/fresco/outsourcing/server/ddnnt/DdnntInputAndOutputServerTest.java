package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputOutputTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.OutputServerProducer;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DdnntInputAndOutputServerTest extends GenericInputOutputTest {
  @Override
  protected SpdzWithIO.Protocol getProtocol() {
    return SpdzWithIO.Protocol.DDNNT;
  }

  @Override
  protected InputClient getInputClient(int inputsPerClient, int id, List<Party> servers) {
    return new DdnntInputClient(inputsPerClient, id, servers);
  }

  @Override
  protected OutputClient getOutputClient(int id, List<Party> servers) {
    return new DdnntOutputClient(id, servers);
  }

  @Override
  protected InputServerProducer getInputServerProducer() {
    return ((endpoint, sessionProducer) -> new DdnntInputServer<>(endpoint, sessionProducer));
  }

  @Override
  protected OutputServerProducer getOutputServerProducer() {
    return ((endpoint, sessionProducer) -> new DdnntOutputServer(endpoint, sessionProducer));
  }

  /**
   * Test the protocol by simply outputting the inputs
   */
  @Test
  public void testMoreInputClientsThanOutputClients() throws Exception {
    setTestRunner(10, 2, 10, 1, 3);
    testInputsAndOutput();
  }

  @Test
  public void testMoreOutputClientsThanInputClients() throws Exception {
    setTestRunner(10, 1, 10, 2, 3);
    testInputsAndOutput();
  }

  @Test
  public void testManyServers() throws Exception {
    setTestRunner(3, 1, 3, 1, 10);
    testInputsAndOutput();
  }

  @Test
  public void moreOutputsPerClient() throws Exception {
    setTestRunner(3, 2, 5, 2, 3);
    testInputsAndOutput();
  }

  @Test
  public void moreInputsPerClient() throws Exception {
    setTestRunner(5, 1, 3, 2, 3);
    testInputsAndOutput();
  }
}
