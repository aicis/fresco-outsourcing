package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.outsourcing.client.jno.ClientPayload;
import dk.alexandra.fresco.outsourcing.client.jno.JnoClientSession;
import dk.alexandra.fresco.outsourcing.client.jno.JnoCommonClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSessionHandler;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class JnoCommonServer<ResourcePoolT extends NumericResourcePool> {
    private static final Logger logger = LoggerFactory.getLogger(JnoCommonClient.class);

    private final ClientSessionHandler<JnoClientSession> clientSessionProducer;
    private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;

    public JnoCommonServer(ClientSessionHandler<JnoClientSession> clientSessionProducer,
                          ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
        this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
        this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
    }

    protected Pair<SortedMap<Integer, ClientPayload<FieldElement>>, List<JnoClientSession>> getClientPayload() throws Exception {
        ExecutorService es = Executors.newCachedThreadPool();
        HashMap<Integer, Future<ClientPayload<FieldElement>>> clientInputFutures = new HashMap<>();
        List<JnoClientSession> sessions = new ArrayList<>();
        while (clientSessionProducer.hasNext()) {
            JnoClientSession clientSession = clientSessionProducer.next();
            sessions.add(clientSession);
            logger.info("Running client input session for C{}", clientSession.getClientId());
            Future<ClientPayload<FieldElement>> f = es.submit(new ClientInputCommunication(clientSession));
            clientInputFutures.put(clientSession.getClientId(), f);
        }
        SortedMap<Integer, ClientPayload<FieldElement>> clientPayloads = new TreeMap<>();
        for (Map.Entry<Integer, Future<ClientPayload<FieldElement>>> e : clientInputFutures.entrySet()) {
            ClientPayload<FieldElement> p = e.getValue().get();
            clientPayloads.put(e.getKey(), p);
            logger.info("Finished client input session for C{}", e.getKey());
        }
        es.shutdown();
        return new Pair<>(clientPayloads, sessions);
    }

    protected ClientSessionHandler<JnoClientSession> getClientSessionProducer() {
        return clientSessionProducer;
    }

    protected ServerSessionProducer<ResourcePoolT> getServerSessionProducer() {
        return serverSessionProducer;
    }

    static class ClientInputCommunication implements Callable<ClientPayload<FieldElement>> {

        private final JnoClientSession session;

        public ClientInputCommunication(JnoClientSession session) {
            this.session = session;
        }

        @Override
        public ClientPayload<FieldElement> call() {
            TwoPartyNetwork net = session.getNetwork();
            byte[] t = net.receive();
            byte[] k = net.receive();
            byte[] r = net.receive();
            byte[] xList = net.receive();
            logger.info("Received masked inputs from C{}", session.getClientId());
            return ClientPayload.deserialize(session.getSerializer(), t, k, r, xList);
        }

    }
}
