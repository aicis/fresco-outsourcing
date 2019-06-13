package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoClientSessionProducer.QueuedClient;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 */
class DemoClientInputSessionProducer implements ClientInputSessionRequestHandler {

  private static final Logger logger = LoggerFactory
      .getLogger(DemoClientInputSessionProducer.class);

  private final SpdzResourcePool resourcePool;
  private int clientsReady;
  private int sessionsProduced;
  private final int expectedClients;
  private final PriorityQueue<QueuedClient> orderingQueue;
  private final BlockingQueue<QueuedClient> processingQueue;
  private final FieldDefinition definition;

  DemoClientInputSessionProducer(SpdzResourcePool resourcePool, FieldDefinition definition,
      int expectedInputClients) {
    this.resourcePool = resourcePool;
    this.definition = definition;
    this.expectedClients = expectedInputClients;
    this.processingQueue = new ArrayBlockingQueue<>(expectedInputClients);
    this.orderingQueue = new PriorityQueue<>(expectedInputClients,
        Comparator.comparingInt(QueuedClient::getPriority));
    this.clientsReady = 0;
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
      DdnntClientInputSession session = new DdnntClientInputSessionImpl(client.getClientId(),
          client.getInputAmount(), client.getNetwork(), distributor, definition);
      sessionsProduced++;
      return session;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean hasNext() {
    return expectedClients - sessionsProduced > 0;
  }

  @Override
  public int registerNewSessionRequest(int suggestedPriority, int clientId, int inputAmount,
      TwoPartyNetwork network) {
    if (resourcePool.getMyId() == 1) {
      int priority = clientsReady++;
      QueuedClient q = new QueuedClient(priority, clientId, inputAmount, network);
      processingQueue.add(q);
      return q.priority;
    } else {
      QueuedClient q = new QueuedClient(suggestedPriority, clientId, inputAmount, network);
      orderingQueue.add(q);
      while (!orderingQueue.isEmpty() && orderingQueue.peek().getPriority() == clientsReady) {
        clientsReady++;
        processingQueue.add(orderingQueue.remove());
      }
      logger.info("S{}: Finished handskake for client {} with priority {}. Expecting {} inputs.",
          resourcePool.getMyId(), q.clientId, q.priority, q.inputAmount);
      return q.priority;
    }
  }

}
