package dk.alexandra.fresco.outsourcing.client;

import static dk.alexandra.fresco.outsourcing.utils.GenericUtils.intFromBytes;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSession;
import dk.alexandra.fresco.outsourcing.server.ClientSessionHandler;
import dk.alexandra.fresco.outsourcing.server.DemoClientSessionRequestHandler;
import dk.alexandra.fresco.suite.spdz.SpdzResourcePool;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSessionEndPoint<T extends ClientSession> implements ClientSessionHandler<T> {
    private static final Logger logger = LoggerFactory
            .getLogger(AbstractSessionEndPoint.class);

    protected final SpdzResourcePool resourcePool;
    protected final int expectedClients;
    protected final PriorityQueue<DemoClientSessionRequestHandler.QueuedClient> orderingQueue;
    protected final BlockingQueue<DemoClientSessionRequestHandler.QueuedClient> processingQueue;
    protected final FieldDefinition definition;
    protected int clientsReady;
    protected int sessionsProduced;

    public AbstractSessionEndPoint(SpdzResourcePool resourcePool,
                                   FieldDefinition definition,
                                   int expectedClients) {
        if (expectedClients < 0) {
            throw new IllegalArgumentException(
                    "Expected input clients cannot be negative, but was: " + expectedClients);
        }
        this.resourcePool = resourcePool;
        this.definition = definition;
        this.expectedClients = expectedClients;
        this.processingQueue = new ArrayBlockingQueue<>(expectedClients);
        this.orderingQueue = new PriorityQueue<>(expectedClients,
                Comparator.comparingInt(DemoClientSessionRequestHandler.QueuedClient::getPriority));
        this.clientsReady = 0;
    }

    protected abstract T getClientSession(DemoClientSessionRequestHandler.QueuedClient client);

    @Override
    public T next() {
        try {
            DemoClientSessionRequestHandler.QueuedClient client = processingQueue.take();
            T session = getClientSession(client);
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
        // Bytes 8-11: number of inputs (big endian int)
        int priority = intFromBytes(Arrays.copyOfRange(handshakeMessage, 0, Integer.BYTES * 1));
        int clientId =
                intFromBytes(Arrays.copyOfRange(handshakeMessage, Integer.BYTES * 1, Integer.BYTES * 2));
        int numInputs =
                intFromBytes(Arrays.copyOfRange(handshakeMessage, Integer.BYTES * 2, Integer.BYTES * 3));
        return registerNewSessionRequest(priority, clientId, numInputs, network);
    }

    @Override
    public int getExpectedClients() {
        return expectedClients;
    }

    private int registerNewSessionRequest(int suggestedPriority, int clientId, int inputAmount,
                                          TwoPartyNetwork network) {
        if (resourcePool.getMyId() == 1) {
            int priority = clientsReady++;
            DemoClientSessionRequestHandler.QueuedClient q = new DemoClientSessionRequestHandler.QueuedClient(priority, clientId, inputAmount, network);
            processingQueue.add(q);
            return q.getPriority();
        } else {
            DemoClientSessionRequestHandler.QueuedClient q = new DemoClientSessionRequestHandler.QueuedClient(suggestedPriority, clientId, inputAmount, network);
            orderingQueue.add(q);
            while (!orderingQueue.isEmpty() && orderingQueue.peek().getPriority() == clientsReady) {
                clientsReady++;
                processingQueue.add(orderingQueue.remove());
            }
            logger.info(
                    "S{}: Finished handskake for input client {} with priority {}. Expecting {} inputs.",
                    resourcePool.getMyId(), q.getClientId(), q.getPriority(), q.getInputAmount());
            return q.getPriority();
        }
    }
}
