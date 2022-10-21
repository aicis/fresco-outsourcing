package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.GenericClientSession;
import dk.alexandra.fresco.outsourcing.client.jno.ClientPayload;
import dk.alexandra.fresco.outsourcing.client.jno.ReconstructClientInput;
import dk.alexandra.fresco.outsourcing.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class JnoInputServer<ResourcePoolT extends NumericResourcePool, ClientSessionT extends ClientSession> extends JnoCommonServer implements InputServer {
  private static final Logger logger = LoggerFactory.getLogger(JnoInputServer.class);
  private final Future<Map<Integer, List<SInt>>> clientInputs;
  private ServerSession<ResourcePoolT> serverInputSession;

  public JnoInputServer(ClientSessionHandler<ClientSessionT> clientSessionProducer,
                        ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    super(clientSessionProducer, serverSessionProducer);
    serverInputSession = getServerSessionProducer().next();
    FutureTask<Map<Integer, List<SInt>>> ft = new FutureTask<>(this::runInputProtocol);
    this.clientInputs = ft;
    Thread t = new Thread(ft);
    t.setName("JNO input Server");
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
  protected Map<Integer, List<SInt>> runInputProtocol() throws Exception {
    logger.info("Running input session");
    Pair<SortedMap<Integer, ClientPayload<FieldElement>>, List<GenericClientSession>> clientPayload = getClientPayload();
    ServerSession<ResourcePoolT> serverInputSession = getServerSessionProducer().next();
    Network network = serverInputSession.getNetwork();
    ResourcePoolT resourcePool = serverInputSession.getResourcePool();
    ReconstructClientInputApp app = new ReconstructClientInputApp(resourcePool.getMyId(),
            resourcePool.getNoOfParties(), clientPayload.getFirst());
    return serverInputSession.getSce().runApplication(app, resourcePool, network);
  }
  @Override
  public Future<Map<Integer, List<SInt>>> getClientInputs() {
    return clientInputs;
  }

  @Override
  public ServerSession<ResourcePoolT> getSession() {
    return serverInputSession;
  }

  private static class ReconstructClientInputApp implements
          Application<Map<Integer, List<SInt>>, ProtocolBuilderNumeric> {

    private final SortedMap<Integer, ClientPayload<FieldElement>> clientPayload;
    private final int myId;
    private final int amountOfServers;

    public ReconstructClientInputApp(int myId, int amountOfServer, SortedMap<Integer, ClientPayload<FieldElement>> clientPayload) {
      this.myId = myId;
      this.amountOfServers = amountOfServer;
      this.clientPayload = clientPayload;
    }

    @Override
    public DRes<Map<Integer, List<SInt>>> buildComputation(ProtocolBuilderNumeric builder) {
      return builder.seq(new ReconstructClientInput(myId, amountOfServers, clientPayload));
    }
  }
}
