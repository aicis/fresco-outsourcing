package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.client.AbstractSessionEndPoint;
import dk.alexandra.fresco.outsourcing.client.GenericClientSession;
import dk.alexandra.fresco.outsourcing.server.ClientSessionHandler;
import dk.alexandra.fresco.outsourcing.server.DemoClientSessionRequestHandler.QueuedClient;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JnoClientSessionEndpoint extends AbstractSessionEndPoint implements ClientSessionHandler<GenericClientSession> {

  private static final Logger logger = LoggerFactory
          .getLogger(JnoClientSessionEndpoint.class);


  public JnoClientSessionEndpoint(SpdzResourcePool resourcePool,
                                  FieldDefinition definition, int expectedClients) {
    super(resourcePool, definition, expectedClients);
  }

  @Override
  public GenericClientSession next() {
    try {
      QueuedClient client = processingQueue.take();
      GenericClientSession session = new GenericClientSession(client.getClientId(),
              client.getNetwork(), definition);
      sessionsProduced++;
      return session;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
