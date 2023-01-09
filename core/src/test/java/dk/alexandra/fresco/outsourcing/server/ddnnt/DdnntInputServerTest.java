package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputServerTest;
import dk.alexandra.fresco.outsourcing.server.TestDataGenerator;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO.Protocol;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public class DdnntInputServerTest extends GenericInputServerTest {

  @Override
  protected InputClient getInputClient(int inputsPerClient, int id, List<Party> servers) {
        return new DdnntInputClient(inputsPerClient, id, servers);
  }

  @Override
  protected InputServerProducer getInputServerProducer() {
    return ((endpoint, sessionProducer) -> new DdnntInputServer<>(endpoint, sessionProducer));
  }

  @Test
  public void testManyInputs() throws Exception {
    setTestRunner(new TestDataGenerator(Protocol.DDNNT, 100, 2, 0, 0, 3));
    testInputsOnly();
  }

  @Test
  public void testManyClients() throws Exception {
    setTestRunner(new TestDataGenerator(Protocol.DDNNT, 3, 5, 0, 0, 3));
    testInputsOnly();
  }

  @Test
  public void testManyServers() throws Exception {
    setTestRunner(new TestDataGenerator(Protocol.DDNNT, 10, 2, 0,0, 5));
    testInputsOnly();
  }

}
