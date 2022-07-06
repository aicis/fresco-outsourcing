package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntInputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputOutputTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;

import java.util.List;

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
}
