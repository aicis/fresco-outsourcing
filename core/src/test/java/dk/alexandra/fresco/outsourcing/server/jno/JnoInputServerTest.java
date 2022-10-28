package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoInputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputServerTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import java.util.List;

public class JnoInputServerTest extends GenericInputServerTest {

    @Override
    protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.JNO;
    }

    @Override
    protected InputClient getInputClient(int inputsPerClient, int id, List<Party> servers) {
        return new JnoInputClient(inputsPerClient, id, servers, BigIntegerFieldDefinition::new, new AesCtrDrbg(new byte[32]));
    }

    @Override
    protected InputServerProducer getInputServerProducer() {
        return ((endpoint, sessionProducer) -> new JnoInputServer<>(endpoint, sessionProducer));
    }
}
