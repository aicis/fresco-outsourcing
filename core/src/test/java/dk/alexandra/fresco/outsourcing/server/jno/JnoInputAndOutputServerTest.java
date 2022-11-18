package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.outsourcing.client.InputClient;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoInputClient;
import dk.alexandra.fresco.outsourcing.client.jno.JnoOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericInputOutputTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.InputServerProducer;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils.OutputServerProducer;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JnoInputAndOutputServerTest extends GenericInputOutputTest {
    @Override
    protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.JNO;
    }

    @Override
    protected InputClient getInputClient(int inputsPerClient, int id, List<Party> servers) {
        return new JnoInputClient(inputsPerClient, id, servers, BigIntegerFieldDefinition::new, new AesCtrDrbg(new byte[32]));
    }

    @Override
    protected OutputClient getOutputClient(int id, List<Party> servers) {
        return new JnoOutputClient(id, servers, new AesCtrDrbg(new byte[32]), testRunner.getOutputsPerClient());
    }

    @Override
    protected InputServerProducer getInputServerProducer() {
        return ((endpoint, sessionProducer) -> new JnoInputServer<>(endpoint, sessionProducer));
    }

    @Override
    protected OutputServerProducer getOutputServerProducer() {
        return ((endpoint, sessionProducer) -> new JnoOutputServer(endpoint, sessionProducer));
    }

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
        setTestRunner(3, 1, 3, 1, 5);
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
