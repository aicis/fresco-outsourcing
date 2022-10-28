package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.GenericClientSession;
import dk.alexandra.fresco.outsourcing.client.jno.ClientPayload;
import dk.alexandra.fresco.outsourcing.client.jno.ReconstructClientInput;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JnoOutputServer<ResourcePoolT extends NumericResourcePool, ClientSessionT extends ClientSession> extends JnoCommonServer implements
        OutputServer<SInt> {

  private static final Logger logger = LoggerFactory.getLogger(JnoOutputServer.class);
  private final Map<Integer, List<SInt>> idToOutputs = new HashMap<>();
  private ServerSession<ResourcePoolT> serverInputSession;

  public JnoOutputServer(ClientSessionHandler<ClientSessionT> clientSessionProducer,
                         ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    super(clientSessionProducer, serverSessionProducer);
    serverInputSession = getServerSessionProducer().next();
  }

  private void runSession() {
    ExceptionConverter.safe(()-> {
      Pair<SortedMap<Integer, ClientPayload<FieldElement>>, List<GenericClientSession>> clientPayload = getClientPayload();
      Network network = serverInputSession.getNetwork();
      ResourcePoolT resourcePool = serverInputSession.getResourcePool();
      JnoClientOutputApp app = new JnoClientOutputApp(resourcePool.getMyId(),
              resourcePool.getNoOfParties(), clientPayload.getFirst(), idToOutputs);
      Map<Integer, List<BigInteger>> res = serverInputSession.getSce().runApplication(app, resourcePool, network);
      ExecutorService es = Executors.newCachedThreadPool();
      for (GenericClientSession session : clientPayload.getSecond()) {
        es.submit(new ClientOutputCommunication(session, res.get(session.getClientId()), serverInputSession.getResourcePool().getFieldDefinition()));
      }
      es.shutdown();
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

  @Override
  public ServerSession<ResourcePoolT> getSession() {
    return serverInputSession;
  }

  private static class ClientOutputCommunication implements Runnable {

    private final GenericClientSession session;
    private final List<BigInteger> output;
    private final FieldDefinition definition;

    ClientOutputCommunication(GenericClientSession session, List<BigInteger> output, FieldDefinition definition) {
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

  private static class JnoClientOutputApp implements
          Application<Map<Integer, List<BigInteger>>, ProtocolBuilderNumeric> {

    private final SortedMap<Integer, ClientPayload<FieldElement>> clientPayload;
    private final int myId;
    private final int amountOfServers;
    private final Map<Integer, List<SInt>> clientOutput;

    public JnoClientOutputApp(int myId, int amountOfServer, SortedMap<Integer, ClientPayload<FieldElement>> clientPayload, Map<Integer, List<SInt>> clientOutput) {
      this.myId = myId;
      this.amountOfServers = amountOfServer;
      this.clientPayload = clientPayload;
      this.clientOutput = clientOutput;
    }

    @Override
    public DRes<Map<Integer, List<BigInteger>>> buildComputation(ProtocolBuilderNumeric builder) {
      return builder.par((par) -> {
        return par.seq(new ReconstructClientInput(myId, amountOfServers, clientPayload));
      }).par( (par, clientInput) -> {
        // Compute and open the client's output after padding it with the random values the client gave as input
        Map<Integer, List<DRes<BigInteger>>> unopenedRes = new HashMap<>();
        for (Integer clientId : clientInput.keySet()) {
          List<DRes<BigInteger>> currentClientList = new ArrayList<>();
          for (int i = 0; i < clientInput.get(clientId).size(); i++) {
            currentClientList.add(par.numeric().open(par.numeric().add(clientInput.get(clientId).get(i),
                    clientOutput.get(clientId).get(i))));
          }
          unopenedRes.put(clientId, currentClientList);
        }
        return () -> unopenedRes;
      }).par((par, unopenedRes) -> {
        Map<Integer, List<BigInteger>> res = new HashMap<>();
        for (Integer clientId : unopenedRes.keySet()) {
          List<BigInteger> currentClientList = new ArrayList<>();
          for (int i = 0; i < unopenedRes.get(clientId).size(); i++) {
            currentClientList.add(unopenedRes.get(clientId).get(i).out());
          }
          res.put(clientId, currentClientList);
        }
        return ()-> res;
      });
    }
  }
}