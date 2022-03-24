package dk.alexandra.fresco.outsourcing.server.jno;

import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.client.jno.JnoClientSession;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.OutputServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
  public static final  String MSG = "test message";
  private final ClientSessionProducer<JnoClientSession> clientSessionProducer;
  private final List<SInt> hiddenOutputs;
  private final List<BigInteger> publicOutput;

  public PestoOutputServer(ClientSessionProducer<JnoClientSession> clientSessionProducer) {
    this.clientSessionProducer = Objects.requireNonNull(clientSessionProducer);
    this.hiddenOutputs = new ArrayList<>();
    this.publicOutput = new ArrayList<>();
  }

  private void runOutputSession() {
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
    try {
//      KeyShare keyShare = session.getKeyShare();
//      MessageDigest md = MessageDigest.getInstance("SHA-256");
//      byte[] msg = md.digest(MSG.getBytes(StandardCharsets.UTF_8));
//      SigShare sig =  keyShare.sign(msg);
      return null;//serializeObject(sig);
    } catch (Exception e) {
      throw new RuntimeException("Could not initialize hash digest", e);
    }
  }

  public static byte[] serializeObject(Serializable obj) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream out = null;
    try {
      out = new ObjectOutputStream(bos);
      out.writeObject(obj);
      out.flush();
      return bos.toByteArray();
    } finally {
      try {
        bos.close();
      } catch (IOException ex) {
        // ignore close exception
      }
    }
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
