package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JnoOutputServer<ResourcePoolT extends NumericResourcePool> implements OutputServer {
  private static final Logger logger = LoggerFactory.getLogger(JnoOutputServer.class);
  private final ClientSessionProducer<JnoClientSession> clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;
  private final List<SInt> outputs;

  public JnoOutputServer(ClientSessionProducer<JnoClientSession> clientSessionProducer,
      ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
    this.outputs = new ArrayList<>();
  }
  @Override
  public void putClientOutputs(int clientId, List<SInt> outputs) {

  }
}
