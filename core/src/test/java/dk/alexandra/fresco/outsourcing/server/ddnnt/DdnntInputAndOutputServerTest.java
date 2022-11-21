package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputOutputTest;
import dk.alexandra.fresco.outsourcing.server.TestDataGenerator;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO.Protocol;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.OutputServerProducer;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DdnntInputAndOutputServerTest extends GenericInputOutputTest {

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
    setTestRunner(new TestDataGenerator(Protocol.DDNNT, 5, 2, 4, 1, 3));
    testInputsAndOutput();
  }

  @Test
  public void testMoreOutputClientsThanInputClients() throws Exception {
    setTestRunner(new TestDataGenerator(Protocol.DDNNT, 4, 1, 5, 2, 3));
    testInputsAndOutput();
  }

  @Test
  public void testManyServers() throws Exception {
    setTestRunner(new TestDataGenerator(Protocol.DDNNT, 4, 1, 4, 1, 5));
    testInputsAndOutput();
  }
}
