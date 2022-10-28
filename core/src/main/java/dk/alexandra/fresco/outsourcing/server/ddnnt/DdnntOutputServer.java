package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.ByteAndBitConverter;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.*;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Output server using the DDNNT output protocol to deliver output to clients.
 *
 * <p>Parts of the code contributed by Mathias Rahbek.</p>
 *
 * @param <ResourcePoolT> type of resource pool used to run the protocol
 * @see <a href="https://eprint.iacr.org/2015/1006">Protocol Description on ePrint</a>
 */
public class DdnntOutputServer<ResourcePoolT extends NumericResourcePool, ClientSessionT extends ClientSession> implements
        OutputServer<SInt> {

  private static final Logger logger = LoggerFactory.getLogger(DdnntOutputServer.class);
  private final ClientSessionHandler<ClientSessionT> clientSessionHandler;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;
  private final Map<Integer, List<SInt>> idToOutputs;
  private ServerSession<ResourcePoolT> serverInputSession;

  public DdnntOutputServer(ClientSessionHandler<ClientSessionT> clientSessionHandler,
                           ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionHandler = Objects.requireNonNull(clientSessionHandler);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
    this.idToOutputs = new HashMap<>();
    this.serverInputSession = serverSessionProducer.next();
  }

  private void runOutputSession() {
    if (idToOutputs.size() != clientSessionHandler.getExpectedClients()) {
      // All output has not currently been added, so we wait with distributing the data
      return;
    }
    logger.info("Running output session");
    ServerSession<ResourcePoolT> serverOutputSession = serverInputSession;
    Network network = serverOutputSession.getNetwork();
    ResourcePoolT resourcePool = serverOutputSession.getResourcePool();

    ExecutorService es = Executors.newCachedThreadPool();
    while (clientSessionHandler.hasNext()) {
      ClientSession clientSession = clientSessionHandler.next();
      logger.info("Running client output session for C{}", clientSession.getClientId());
      AuthenticateOutput app = new AuthenticateOutput(idToOutputs.get(clientSession.getClientId()));
      List<Map<String, DRes<SInt>>> result =
              serverOutputSession.getSce().runApplication(app, resourcePool, network);
      es.submit(new ClientCommunication(clientSession, result));
    }
    es.shutdown();
  }

  @Override
  public ServerSession<ResourcePoolT> getSession() {
    return serverInputSession;
  }

  private static class AuthenticateOutput
      implements Application<List<Map<String, DRes<SInt>>>, ProtocolBuilderNumeric> {

    private final List<SInt> outputs;

    AuthenticateOutput(List<SInt> outputs) {
      this.outputs = outputs;
    }

    @Override
    public DRes<List<Map<String, DRes<SInt>>>> buildComputation(ProtocolBuilderNumeric builder) {
      return builder.par(par -> {
        List<Map<String, DRes<SInt>>> result = new ArrayList<>();
        for (SInt output : outputs) {
          DRes<SInt> maskR = par.numeric().randomElement();
          DRes<SInt> maskV = par.numeric().randomElement();
          par.seq(seq -> {
            Numeric numeric = seq.numeric();
            // TODO these could also happen in parallel
            DRes<SInt> productW = numeric.mult(output, maskR);
            DRes<SInt> productU = numeric.mult(maskV, maskR);
            Map<String, DRes<SInt>> resultShares = new HashMap<>();
            resultShares.put("r", maskR);
            resultShares.put("v", maskV);
            resultShares.put("w", productW);
            resultShares.put("u", productU);
            resultShares.put("y", output);
            result.add(resultShares);
            return () -> null;
          });
        }
        logger.info("Added output shares to result");
        return () -> result;
      });
    }
  }

  @Override
  public void putClientOutputs(int clientId, List<SInt> outputs) {
    if (this.idToOutputs.containsKey(clientId)) {
      throw new UnsupportedOperationException("Output has already been set for party " + clientId);
    }
    this.idToOutputs.put(clientId, outputs);
    this.runOutputSession();
  }

  private static class ClientCommunication implements Runnable {

    private final ClientSession session;
    private final List<Map<String, DRes<SInt>>> outputs;

    ClientCommunication(ClientSession session, List<Map<String, DRes<SInt>>> outputs) {
      this.outputs = outputs;
      this.session = session;
    }

    @Override
    public void run() {
      TwoPartyNetwork net = session.getNetwork();
      // send number of outputs to client
      net.send(ByteAndBitConverter.toByteArray(outputs.size()));
      // TODO these should all be batched
      for (Map<String, DRes<SInt>> e : outputs) {
        List<FieldElement> listOfOutputShares = new ArrayList<>();
        SpdzSInt r = (SpdzSInt) e.get("r").out();
        SpdzSInt v = (SpdzSInt) e.get("v").out();
        SpdzSInt w = (SpdzSInt) e.get("w").out();
        SpdzSInt u = (SpdzSInt) e.get("u").out();
        SpdzSInt y = (SpdzSInt) e.get("y").out();
        listOfOutputShares.add(r.getShare());
        listOfOutputShares.add(v.getShare());
        listOfOutputShares.add(w.getShare());
        listOfOutputShares.add(u.getShare());
        listOfOutputShares.add(y.getShare());
        net.send(session.getSerializer().serialize(listOfOutputShares));
      }
      logger.info("Sent shares to C{}", session.getClientId());
    }
  }

}
