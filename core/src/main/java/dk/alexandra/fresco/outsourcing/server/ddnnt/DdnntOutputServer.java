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

/**
 * Output server using the DDNNT output protocol to deliver output to clients.
 *
 * <p>Parts of the code contributed by Mathias Rahbek.</p>
 *
 * @param <ResourcePoolT> type of resource pool used to run the protocol
 * @see <a href="https://eprint.iacr.org/2015/1006">Protocol Description on ePrint</a>
 */
public class DdnntOutputServer<ResourcePoolT extends NumericResourcePool> implements OutputServer {

  private static final Logger logger = LoggerFactory.getLogger(DdnntOutputServer.class);
  private final DdnntClientSessionProducer clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;
  private final List<SInt> outputs;

  public DdnntOutputServer(DdnntClientSessionProducer clientSessionProducer,
      ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
    this.outputs = new ArrayList<>();
  }

  private void runOutputSession() {
    logger.info("Running output session");
    ServerSession<ResourcePoolT> serverOutputSession = serverSessionProducer.next();
    Network network = serverOutputSession.getNetwork();
    ResourcePoolT resourcePool = serverOutputSession.getResourcePool();
    AuthenticateOutput app = new AuthenticateOutput(outputs);
    List<Map<String, DRes<SInt>>> result =
        serverOutputSession.getSce().runApplication(app, resourcePool, network);
    ExecutorService es = Executors.newCachedThreadPool();
    while (clientSessionProducer.hasNextOutput()) {
      DdnntClientOutputSession clientSession = clientSessionProducer.nextOutput();
      logger.info("Running client output session for C{}", clientSession.getClientId());
      es.submit(new ClientCommunication(clientSession, result));
    }
    es.shutdown();
  }

  private static class AuthenticateOutput
      implements Application<List<Map<String, DRes<SInt>>>, ProtocolBuilderNumeric> {

    private final List<SInt> outputs;

    AuthenticateOutput(List<SInt> outputs) {
      this.outputs = outputs;
    }

    @Override
    public DRes<List<Map<String, DRes<SInt>>>> buildComputation(ProtocolBuilderNumeric builder) {
      // TODO outer par scope
      return builder.seq(seq -> {
        Numeric numeric = seq.numeric();
        List<Map<String, DRes<SInt>>> result = new ArrayList<>();
        for (SInt output : outputs) {
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
    if (!this.outputs.isEmpty()) {
      throw new UnsupportedOperationException("Currently only support output to at most one party");
    }
    this.outputs.addAll(outputs);
    this.runOutputSession();
  }

  private static class ClientCommunication implements Runnable {

    private final DdnntClientOutputSession session;
    private List<Map<String, DRes<SInt>>> outputs;

    ClientCommunication(DdnntClientOutputSession session, List<Map<String, DRes<SInt>>> outputs) {
      this.outputs = outputs;
      this.session = session;
    }

    @Override
    public void run() {
      TwoPartyNetwork net = session.getNetwork();
      // send number of outputs to client
      net.send(ByteAndBitConverter.toByteArray(outputs.size()));
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
