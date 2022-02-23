package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import dk.alexandra.fresco.outsourcing.server.ServerSession;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PestoOutputServer<ResourcePoolT extends NumericResourcePool> implements
    OutputServer<BigInteger> {

  private static final Logger logger = LoggerFactory.getLogger(PestoOutputServer.class);
  private final ClientSessionProducer<JnoClientSession> clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;
  private final List<SInt> hiddenOutputs;
  private final List<BigInteger> publicOutput;

  public PestoOutputServer(ClientSessionProducer<JnoClientSession> clientSessionProducer,
      ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.serverSessionProducer = Objects.requireNonNull(serverSessionProducer);
    this.hiddenOutputs = new ArrayList<>();
    this.publicOutput = new ArrayList<>();
  }

  private void runOutputSession() {
    logger.info("Running output session");
    ServerSession<ResourcePoolT> serverOutputSession = serverSessionProducer.next();
    Network network = serverOutputSession.getNetwork();
    ResourcePoolT resourcePool = serverOutputSession.getResourcePool();
    ExecutorService es = Executors.newCachedThreadPool();
    while (clientSessionProducer.hasNext()) {
      JnoClientSession clientSession = clientSessionProducer.next();
      byte[] result = computeClientToken(clientSession);
      logger.info("Running client output session for C{}", clientSession.getClientId());
      es.submit(new ClientCommunication(clientSession, result));
    }
    es.shutdown();
  }

  private byte[] computeClientToken(JnoClientSession session) {
    // TODO
    return new byte[32];
  }

  @Override
  public void putClientOutputs(int clientId, List<BigInteger> outputs) {
    if (!this.publicOutput.isEmpty()) {
      throw new UnsupportedOperationException("Currently only support output to at most one party");
    }
    this.publicOutput.addAll(publicOutput);
    this.runOutputSession();
  }

  private static class ClientCommunication implements Runnable {

    private final JnoClientSession session;
    private byte[] output;

    ClientCommunication(JnoClientSession session, byte[] output) {
      this.output = output;
      this.session = session;
    }

    @Override
    public void run() {
      TwoPartyNetwork net = session.getNetwork();
      // send number of outputs to client
      net.send(output);
      logger.info("Sent shares to C{}", session.getClientId());
    }
  }
}
