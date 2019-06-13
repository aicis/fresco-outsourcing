package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdnntOutputServer<ResourcePoolT extends NumericResourcePool> implements OutputServer {

  private static final Logger logger = LoggerFactory.getLogger(DdnntOutputServer.class);
  private final ClientSessionProducer clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;
  private final Map<Integer, List<SInt>> outputs = new HashMap<>();

  public DdnntOutputServer(ClientSessionProducer clientSessionProducer,
      ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
  }

  private void runOutputSession() {
    ExecutorService es = Executors.newCachedThreadPool();
    logger.info("Running output session");
    ServerSession<ResourcePoolT> serverOutputSession = serverSessionProducer.next();
    Network network = serverOutputSession.getNetwork();
    ResourcePoolT resourcePool = serverOutputSession.getResourcePool();
    AuthenticateOutput app = new AuthenticateOutput(outputs);
    List<Map<String, DRes<SInt>>> result =
        serverOutputSession.getSce().runApplication(app, resourcePool, network);
    while (clientSessionProducer.hasNextOutput()) {
      DdnntClientInputSession clientSession = clientSessionProducer.nextOutput();
      logger.info("Running client output session for C{}", clientSession.getClientId());
      es.submit(new ClientCommunication(clientSession, result));
    }
  }

  private static class AuthenticateOutput
      implements Application<List<Map<String, DRes<SInt>>>, ProtocolBuilderNumeric> {

    private final Map<Integer, List<SInt>> outputs;

    AuthenticateOutput(Map<Integer, List<SInt>> outputs) {
      this.outputs = outputs;
    }

    @Override
    public DRes<List<Map<String, DRes<SInt>>>> buildComputation(ProtocolBuilderNumeric builder) {
      return builder.seq(seq -> {
        Numeric numeric = seq.numeric();
        List<Map<String, DRes<SInt>>> result = new ArrayList<>();
        for (SInt output : outputs.get(1)) {
          DRes<SInt> maskR = numeric.randomElement();
          DRes<SInt> maskV = numeric.randomElement();
          DRes<SInt> productW = numeric.mult(output, maskR);
          DRes<SInt> productU = numeric.mult(maskV, maskR);
          Map<String, DRes<SInt>> resultShares = new HashMap<>();
          resultShares.put("r", maskR);
          resultShares.put("v", maskV);
          resultShares.put("w", productW);
          resultShares.put("u", productU);
          resultShares.put("y", output);
          result.add(resultShares);
          logger.info("Added outputs shares to result");
        }
        return () -> result;
      });
    }
  }

  @Override
  public void putClientOutputs(int clientId, List<SInt> outputs) {
    this.outputs.put(clientId, outputs);
    this.runOutputSession();
  }

  private static class ClientCommunication implements Runnable {

    private final DdnntClientInputSession session;
    private List<Map<String, DRes<SInt>>> outputs;

    ClientCommunication(DdnntClientInputSession session, List<Map<String, DRes<SInt>>> outputs) {
      this.outputs = outputs;
      this.session = session;
    }

    @Override
    public void run() {
      TwoPartyNetwork net = session.getNetwork();
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
        logger.info("Sent shares to C{}", session.getClientId());
      }
    }
  }

}
