package dk.alexandra.fresco.outsourcing.server.ddnnt;

import static dk.alexandra.fresco.outsourcing.utils.ByteConversionUtils.intFromBytes;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ddnnt.DemoClientSessionProducer.QueuedClient;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 */
class DemoClientOutputSessionRequestHandler implements
    ClientSessionRequestHandler<DdnntClientOutputSession> {

  private static final Logger logger = LoggerFactory
      .getLogger(DemoClientOutputSessionRequestHandler.class);

  private final SpdzResourcePool resourcePool;
  private int clientsReady;
  private int sessionsProduced;
  private final int expectedClients;
  private final PriorityQueue<QueuedClient> orderingQueue;
  private final BlockingQueue<QueuedClient> processingQueue;
  private final FieldDefinition definition;

  DemoClientOutputSessionRequestHandler(SpdzResourcePool resourcePool, FieldDefinition definition,
      int expectedClients) {
    this.resourcePool = resourcePool;
    this.definition = definition;
    this.expectedClients = expectedClients;
    this.processingQueue = new ArrayBlockingQueue<>(expectedClients);
    this.orderingQueue = new PriorityQueue<>(expectedClients,
        Comparator.comparingInt(QueuedClient::getPriority));
    this.clientsReady = 0;
  }

  @Override
  public DdnntClientOutputSession next() {
    try {
      QueuedClient client = processingQueue.take();
      DdnntClientOutputSession session = new DdnntClientOutputSessionImpl(client.getClientId(),
          client.getNetwork(), definition);
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
  public int registerNewSessionRequest(byte[] handshakeMessage, TwoPartyNetwork network) {
    // Bytes 0-3: client priority, assigned by server 1 (big endian int)
    // Bytes 4-7: unique id for client (big endian int)
    int priority = intFromBytes(Arrays.copyOfRange(handshakeMessage, 0, Integer.BYTES * 1));
    int clientId =
        intFromBytes(Arrays.copyOfRange(handshakeMessage, Integer.BYTES * 1, Integer.BYTES * 2));
    return registerNewSessionRequest(priority, clientId, network);
  }

  private int registerNewSessionRequest(int suggestedPriority, int clientId,
      TwoPartyNetwork network) {
    if (resourcePool.getMyId() == 1) {
      int priority = clientsReady++;
      QueuedClient q = new QueuedClient(priority, clientId, 0, network);
      processingQueue.add(q);
      return q.priority;
    } else {
      QueuedClient q = new QueuedClient(suggestedPriority, clientId, 0, network);
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
