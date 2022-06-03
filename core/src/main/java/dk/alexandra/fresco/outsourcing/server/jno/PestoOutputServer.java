package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PestoOutputServer<ResourcePoolT extends NumericResourcePool> implements
    OutputServer<SInt> {

  private static final Logger logger = LoggerFactory.getLogger(PestoOutputServer.class);
  private final ClientSessionHandler<JnoClientSession> clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;
  private final Map<Integer, List<SInt>> idToOutputs;
  private Future<Map<Integer, List<BigInteger>>> paddedOutput;
  private final List<SInt> hiddenOutputs;
  private final List<BigInteger> publicOutput;
  private final List<JnoClientSession> sessions;

  public PestoOutputServer(ClientSessionHandler<JnoClientSession> clientSessionProducer,
                           ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);

    this.hiddenOutputs = new ArrayList<>();
    this.publicOutput = new ArrayList<>();
    this.idToOutputs = new HashMap<>();
    this.sessions = new ArrayList<>();
  }

  private Void runInputSession() throws Exception {
    logger.info("Running input session");
    SortedMap<Integer, ClientPayload<FieldElement>> clientPayload = getClientPayload();
    ServerSession<ResourcePoolT> serverInputSession = serverSessionProducer.next();
    Network network = serverInputSession.getNetwork();
    ResourcePoolT resourcePool = serverInputSession.getResourcePool();
    JnoClientOutputApp app = new JnoClientOutputApp(resourcePool.getMyId(),
            resourcePool.getNoOfParties(), clientPayload, idToOutputs);
    Map<Integer, List<BigInteger>>  res = serverInputSession.getSce().runApplication(app, resourcePool, network);
    ExecutorService es = Executors.newCachedThreadPool();
    for (JnoClientSession session : sessions) {
      es.submit(new ClientCommunication(session, res.get(session.getClientId()), serverInputSession.getResourcePool().getFieldDefinition()));
    }
    es.shutdown();
    return null;
  }

  private SortedMap<Integer, ClientPayload<FieldElement>> getClientPayload() throws Exception {
    ExecutorService es = Executors.newCachedThreadPool();
    HashMap<Integer, Future<ClientPayload<FieldElement>>> clientInputFutures = new HashMap<>();
    while (clientSessionProducer.hasNext()) {
      JnoClientSession clientSession = clientSessionProducer.next();
      sessions.add(clientSession);
      logger.info("Running client input session for C{}", clientSession.getClientId());
      Future<ClientPayload<FieldElement>> f = es.submit(new JnoInputServer.ClientCommunication(clientSession));
      clientInputFutures.put(clientSession.getClientId(), f);
    }
    SortedMap<Integer, ClientPayload<FieldElement>> clientPayloads = new TreeMap<>();
    for (Map.Entry<Integer, Future<ClientPayload<FieldElement>>> e : clientInputFutures.entrySet()) {
      ClientPayload<FieldElement> p = e.getValue().get();
      clientPayloads.put(e.getKey(), p);
      logger.info("Finished client input session for C{}", e.getKey());
    }
    es.shutdown();
    return clientPayloads;
  }


  private void runOutputSession() {
    if (idToOutputs.size() != clientSessionProducer.getExpectedClients()) {
      // All output has not currently been added, so we wait with distributing the data
      return;
    }
    FutureTask<Void> ft = new FutureTask<>(this::runInputSession);
//    this.paddedOutput = ft;
    Thread t = new Thread(ft);
    t.setName("JNO Input Server");
    t.start();

//    logger.info("Running output session");
//    ServerSession<ResourcePoolT> serverOutputSession = serverSessionProducer.next();
//    Network network = serverOutputSession.getNetwork();
//    ResourcePoolT resourcePool = serverOutputSession.getResourcePool();
//
//    // Run an input session to where the client inputs random paddings for the output
//    JnoInputServer inputServer = new JnoInputServer(clientSessionProducer, serverSessionProducer);
//    Map<Integer, List<SInt>> paddings = (Map<Integer, List<SInt>>) ExceptionConverter
//            .safe(() -> inputServer.getClientInputs().get(), "Input step failed");
//
//    ExecutorService es = Executors.newCachedThreadPool();
//    while (clientSessionProducer.hasNext()) {
//      JnoClientSession clientSession = clientSessionProducer.next();
////      // Send to the client how many outputs it should construct random values for
////      clientSession.getNetwork().send(ByteAndBitConverter.toByteArray(idToOutputs.get(clientSession.getClientId()).size()));
//
//      logger.info("Running client output session for C{}", clientSession.getClientId());
//      AuthenticateOutput app = new AuthenticateOutput(idToOutputs.get(clientSession.getClientId()), paddings.get(clientSession.getClientId()));
//      List<DRes<BigInteger>> result =
//              serverOutputSession.getSce().runApplication(app, resourcePool, network);
//      es.submit(new ClientCommunication(clientSession, result, serverOutputSession.getResourcePool().getFieldDefinition()));
//    }
//    es.shutdown();
  }

//  private static class AuthenticateOutput
//          implements Application<List<DRes<BigInteger>>, ProtocolBuilderNumeric> {
//
//    private final List<SInt> outputs;
//    private final List<SInt> paddings;
//
//    AuthenticateOutput(List<SInt> outputs, List<SInt> paddings) {
//      this.outputs = outputs;
//      this.paddings = paddings;
//    }
//
//    @Override
//    public DRes<List<DRes<BigInteger>>> buildComputation(ProtocolBuilderNumeric builder) {
//      return builder.par(par -> {
//        List<DRes<BigInteger>> result = new ArrayList<>();
//        for (int i = 0; i < outputs.size(); i++) {
//          result.add(par.numeric().open(par.numeric().sub(outputs.get(i), paddings.get(i))));
//        }
//        logger.info("Added output shares to result");
//        return () -> result;
//      });
//    }
//  }

  @Override
  public void putClientOutputs(int clientId, List<SInt> outputs) {
    if (this.idToOutputs.containsKey(clientId)) {
      throw new UnsupportedOperationException("Output has already been set for party " + clientId);
    }
    this.idToOutputs.put(clientId, outputs);
    this.runOutputSession();
  }

  private static class ClientCommunication implements Runnable {

    private final JnoClientSession session;
    private List<BigInteger> output;
    private final FieldDefinition definition;

    ClientCommunication(JnoClientSession session, List<BigInteger> output, FieldDefinition definition) {
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
