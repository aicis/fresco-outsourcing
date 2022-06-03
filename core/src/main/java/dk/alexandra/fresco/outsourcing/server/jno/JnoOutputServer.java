package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.jno.ClientPayload;
import dk.alexandra.fresco.outsourcing.client.jno.JnoClientOutputApp;
import dk.alexandra.fresco.outsourcing.client.jno.JnoClientSession;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSessionHandler;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.server.ServerSession;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JnoOutputServer<ResourcePoolT extends NumericResourcePool> extends JnoCommonServer implements
    OutputServer<SInt> {

  private static final Logger logger = LoggerFactory.getLogger(JnoOutputServer.class);
  private final Map<Integer, List<SInt>> idToOutputs = new HashMap<>();

  public JnoOutputServer(ClientSessionHandler<JnoClientSession> clientSessionProducer,
                         ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    super(clientSessionProducer, serverSessionProducer);
  }

  private void runSession() {
    ExceptionConverter.safe(()-> {
      Pair<SortedMap<Integer, ClientPayload<FieldElement>>, List<JnoClientSession>> clientPayload = getClientPayload();
      ServerSession<ResourcePoolT> serverInputSession = getServerSessionProducer().next();
      Network network = serverInputSession.getNetwork();
      ResourcePoolT resourcePool = serverInputSession.getResourcePool();
      JnoClientOutputApp app = new JnoClientOutputApp(resourcePool.getMyId(),
              resourcePool.getNoOfParties(), clientPayload.getFirst(), idToOutputs);
      Map<Integer, List<BigInteger>> res = serverInputSession.getSce().runApplication(app, resourcePool, network);
      ExecutorService es = Executors.newCachedThreadPool();
      for (JnoClientSession session : clientPayload.getSecond()) {
        es.submit(new ClientOutputCommunication(session, res.get(session.getClientId()), serverInputSession.getResourcePool().getFieldDefinition()));
      }
      es.awaitTermination(1, TimeUnit.SECONDS);
      return null;
    }, "Running server output session failed");
  }

  @Override
  public void putClientOutputs(int clientId, List<SInt> outputs) {
    if (this.idToOutputs.containsKey(clientId)) {
      throw new UnsupportedOperationException("Output has already been set for party " + clientId);
    }
    this.idToOutputs.put(clientId, outputs);
    if (idToOutputs.size() != getClientSessionProducer().getExpectedClients()) {
      // All output has not currently been added, so we wait with distributing the data
      return;
    }
    runSession();
  }

  private static class ClientOutputCommunication implements Runnable {

    private final JnoClientSession session;
    private List<BigInteger> output;
    private final FieldDefinition definition;

    ClientOutputCommunication(JnoClientSession session, List<BigInteger> output, FieldDefinition definition) {
      this.output = output;
      this.session = session;
      this.definition = definition;
    }

    @Override
    public void run() {
      TwoPartyNetwork net = session.getNetwork();
      // get paddings from input protocol
      List<FieldElement> res = output.stream().map(cur -> definition.createElement(cur)).collect(Collectors.toList());
      // send number of outputs to client
      net.send(session.getSerializer().serialize(res));
      logger.info("Sent shares to C{}", session.getClientId());
    }
  }
}
