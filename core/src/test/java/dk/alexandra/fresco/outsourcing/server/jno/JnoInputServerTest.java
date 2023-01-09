package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoInputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputServerTest;
import dk.alexandra.fresco.outsourcing.server.TestDataGenerator;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO.Protocol;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JnoInputServerTest extends GenericInputServerTest {

    @Override
    protected InputClient getInputClient(int inputsPerClient, int id, List<Party> servers) {
        return new JnoInputClient(inputsPerClient, id, servers, BigIntegerFieldDefinition::new, new AesCtrDrbg(new byte[32]));
    }

    @Override
    protected InputServerProducer getInputServerProducer() {
        return ((endpoint, sessionProducer) -> new JnoInputServer<>(endpoint, sessionProducer));
    }

    @Test
    public void testManyInputs() throws Exception {
        setTestRunner(new TestDataGenerator(Protocol.GENERIC, 100, 2, 0, 0, 3));
        testInputsOnly();
    }

    @Test
    public void testManyClients() throws Exception {
        setTestRunner(new TestDataGenerator(Protocol.GENERIC, 3, 5, 0, 0, 3));
        testInputsOnly();
    }

    @Test
    public void testManyServers() throws Exception {
        setTestRunner(new TestDataGenerator(Protocol.GENERIC, 10, 2, 0, 0, 5));
        testInputsOnly();
    }

}
