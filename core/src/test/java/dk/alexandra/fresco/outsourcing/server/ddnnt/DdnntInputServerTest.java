package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputServerTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A full functional test, that will set up a number of servers to accept inputs from some number of
 * clients.
 */
public class DdnntInputServerTest extends GenericInputServerTest {
  @Override
  protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.DDNNT;
  }

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
    setTestRunner(100, 2, 3);
    testInputsOnly();
  }

  @Test
  public void testManyClients() throws Exception {
    setTestRunner(3, 4, 3);
    testInputsOnly();
  }

  @Test
  public void testManyServers() throws Exception {
    setTestRunner(10, 2, 5);
    testInputsOnly();
  }

}
