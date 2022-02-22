package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.ServerSession;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JnoInputServer<ResourcePoolT extends NumericResourcePool> implements InputServer {
  private static final Logger logger = LoggerFactory.getLogger(JnoInputServer.class);
  private static final String HASH_ALGO = "SHA-256";
  private final Future<Map<Integer, List<SInt>>> clientInputs;
  private final ClientSessionProducer<JnoClientSession> clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;

  public JnoInputServer(ClientSessionProducer<JnoClientSession> clientSessionProducer,
      ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
    FutureTask<Map<Integer, List<SInt>>> ft = new FutureTask<>(this::runInputSession);
    this.clientInputs = ft;
    Thread t = new Thread(ft);
    t.setName("JNO Input Server");
    t.start();
  }

  /**
   * Runs the input session.
   *
   * <p>
   * Will return when the input of all clients in this session is ready.
   * </p>
   *
   * @return a map from client id's to a list of inputs given by the party
   * @throws Exception if exceptions a thrown
   */
  private Map<Integer, List<SInt>> runInputSession() throws Exception {
    logger.info("Running input session");
    SortedMap<Integer, ClientPayload<FieldElement>> clientPayload = getClientPayload();
    ServerSession<ResourcePoolT> serverInputSession = serverSessionProducer.next();
    Network network = serverInputSession.getNetwork();
    ResourcePoolT resourcePool = serverInputSession.getResourcePool();
    ReconstructClientInputApp app = new ReconstructClientInputApp(resourcePool.getMyId(),
        resourcePool.getNoOfParties(), clientPayload, resourcePool.getFieldDefinition());
    return serverInputSession.getSce().runApplication(app, resourcePool, network);
  }

  @Override
  public Future<Map<Integer, List<SInt>>> getClientInputs() {
    return clientInputs;
  }

  private SortedMap<Integer, ClientPayload<FieldElement>> getClientPayload() throws Exception {
    ExecutorService es = Executors.newCachedThreadPool();
    HashMap<Integer, Future<ClientPayload<FieldElement>>> clientInputFutures = new HashMap<>();
    while (clientSessionProducer.hasNext()) {
      JnoClientSession clientSession = clientSessionProducer.next();
      logger.info("Running client input session for C{}", clientSession.getClientId());
      Future<ClientPayload<FieldElement>> f = es.submit(new ClientCommunication(clientSession));
      clientInputFutures.put(clientSession.getClientId(), f);
    }
    SortedMap<Integer, ClientPayload<FieldElement>> clientPayloads = new TreeMap<>();
    for (Entry<Integer, Future<ClientPayload<FieldElement>>> e : clientInputFutures.entrySet()) {
      ClientPayload<FieldElement> p = e.getValue().get();
      clientPayloads.put(e.getKey(), p);
      logger.info("Finished client input session for C{}", e.getKey());
    }
    es.shutdown();
    return clientPayloads;
  }

  private static class ClientCommunication implements Callable<ClientPayload<FieldElement>> {

    private final JnoClientSession session;

    public ClientCommunication(JnoClientSession session) {
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
