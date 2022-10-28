package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericOutputServerTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.OutputServerProducer;
import java.util.List;

public class JnoOutputServerTest extends GenericOutputServerTest {

    @Override
    protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.JNO;
    }

    @Override
    protected OutputClient getOutputClient(int id, List<Party> servers) {
        return new JnoOutputClient(id, servers, new AesCtrDrbg(new byte[32]), testRunner.getOutputsPerClient());
    }

    @Override
    protected OutputServerProducer getOutputServerProducer() {
        return ((endpoint, sessionProducer) -> new JnoOutputServer(endpoint, sessionProducer));
    }
}
