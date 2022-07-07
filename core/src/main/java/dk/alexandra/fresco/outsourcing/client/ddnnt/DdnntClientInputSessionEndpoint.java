package dk.alexandra.fresco.outsourcing.client.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.client.AbstractSessionEndPoint;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.ClientSessionRegistration;
import dk.alexandra.fresco.outsourcing.server.DemoClientSessionRequestHandler.QueuedClient;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DdnntInputTuple;
import dk.alexandra.fresco.outsourcing.server.ddnnt.SpdzDdnntTuple;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DdnntClientInputSessionEndpoint extends AbstractSessionEndPoint implements
        ClientSessionRegistration<DdnntClientInputSession>,
        ClientSessionProducer<DdnntClientInputSession> {

  private static final Logger logger = LoggerFactory
          .getLogger(DdnntClientInputSessionEndpoint.class);

  public DdnntClientInputSessionEndpoint(SpdzResourcePool resourcePool,
                                         FieldDefinition definition,
                                         int expectedClients) {
    super(resourcePool, definition, expectedClients);
  }

  @Override
  public DdnntClientInputSession next() {
    try {
      QueuedClient client = processingQueue.take();
      List<DdnntInputTuple> tripList = new ArrayList<>(client.getInputAmount());
      for (int i = 0; i < client.getInputAmount(); i++) {
        SpdzTriple trip = resourcePool
            .getDataSupplier()
            .getNextTriple();
        tripList.add(new SpdzDdnntTuple(trip));
      }
      TripleDistributor distributor = new PreLoadedTripleDistributor(tripList);
      DdnntClientInputSession session = new DdnntClientInputSession(client.getClientId(),
              client.getInputAmount(), client.getNetwork(), distributor, definition);
      sessionsProduced++;
      return session;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
