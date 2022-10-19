package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericOutputServerTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import java.util.List;

public class DdnntOutputServerTest extends GenericOutputServerTest {
    @Override
    protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.DDNNT;
    }

    @Override
    protected OutputClient getOutputClient(int id, List<Party> servers) {
        return new DdnntOutputClient(id, servers);
    }
}
