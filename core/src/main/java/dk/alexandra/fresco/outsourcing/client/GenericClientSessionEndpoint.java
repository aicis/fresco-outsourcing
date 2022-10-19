package dk.alexandra.fresco.outsourcing.client;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.server.DemoClientSessionRequestHandler.QueuedClient;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericClientSessionEndpoint extends AbstractSessionEndPoint<GenericClientSession> {

  private static final Logger logger = LoggerFactory
          .getLogger(GenericClientSessionEndpoint.class);

  public GenericClientSessionEndpoint(SpdzResourcePool resourcePool,
                                      FieldDefinition definition,
                                      int expectedClients) {
    super(resourcePool, definition, expectedClients);
  }

  @Override
  protected GenericClientSession getClientSession(QueuedClient client) {
    return new GenericClientSession(client.getClientId(), client.getNetwork(), definition);
  }

}
