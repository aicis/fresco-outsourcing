package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.builder.ComputationParallel;
import dk.alexandra.fresco.framework.builder.numeric.NumericResourcePool;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.server.ClientSessionProducer;
import dk.alexandra.fresco.outsourcing.server.InputServer;
import dk.alexandra.fresco.outsourcing.server.ServerSessionProducer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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
import java.util.stream.Collectors;

/**
 * Input server using the DDNNT input protocol to provide input.
 *
 * @param <ResourcePoolT> type of resource pool used to run the protocol
 * @see <a href="https://eprint.iacr.org/2015/1006">Protocol Description on ePrint</a>
 */
public class DdnntInputServer<ResourcePoolT extends NumericResourcePool> implements InputServer {

  private static final String HASH_ALGO = "SHA-256";
  private final Future<Map<Integer, List<SInt>>> clientInputs;
  private final ClientSessionProducer clientSessionProducer;
  private final ServerSessionProducer<ResourcePoolT> serverSessionProducer;

  /**
   * Creates a new server to handle an input session.
   *
   * <p>
   * The server will use session producers to set up client and server facing parts of the protocol
   * respectively. Note that, the parameters of the protocol are implicitly defined by these
   * producers, e.g., the order in which clients give inputs, the number of MPC parties and so on.
   * </p>
   *
   * @param clientSessionProducer producer of client sessions
   * @param serverSessionProducer producer of server sessions
   */
  public DdnntInputServer(ClientSessionProducer clientSessionProducer,
      ServerSessionProducer<ResourcePoolT> serverSessionProducer) {
    Objects.requireNonNull(clientSessionProducer);
    Objects.requireNonNull(serverSessionProducer);
    this.clientSessionProducer = clientSessionProducer;
    this.serverSessionProducer = serverSessionProducer;
    FutureTask<Map<Integer, List<SInt>>> ft = new FutureTask<>(this::runInputSession);
    this.clientInputs = ft;
    Thread t = new Thread(ft);
    t.setName("DDNNT Input Server");
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
    SortedMap<Integer, Pair<List<SInt>, byte[]>> maskPairs = getMaskPairs();
    ServerInputSession<ResourcePoolT> serverInputSession = serverSessionProducer.next();
    Network network = serverInputSession.getNetwork();
    broadcastMaskedInput(maskPairs, network);
    ResourcePoolT resourcePool = serverInputSession.getResourcePool();
    UnMaskingApp app = new UnMaskingApp(maskPairs, resourcePool.getSerializer());
    Map<Integer, List<SInt>> result =
        serverInputSession.getSce().runApplication(app, resourcePool, network);
    return result;
  }

  /**
   * Does client interaction part of the protocol.
   *
   * <p>
   * For each client session and each element to be input by the client <i>x</i> this will retrieve
   * secret shares of a multiplication triple <i>(a, b, c)</i> which will be sent to the client.
   * Once having received all shares of the triple the client should then send back <i>x - a</i>,
   * i.e., a masking of the input element using <i>a</i> as the mask (or OTP key).
   *
   * This method returns when all such client sessions are done. It returns for each client a list
   * of masked inputs <i>x - a </i> (serialized as a byte array) and a list of SInts representing
   * the masks <i>a</i>. The server part of the protocol will then work to unmask the input elements
   * securely.
   * </p>
   *
   * @return a map from client ids to a pair representing the mask and masked input respectively
   * @throws Exception if exceptions are thrown during the protocol execution.
   */
  private SortedMap<Integer, Pair<List<SInt>, byte[]>> getMaskPairs() throws Exception {
    ExecutorService es = Executors.newCachedThreadPool();
    HashMap<Integer, Future<Pair<List<SInt>, byte[]>>> maskPairsFuture = new HashMap<>();
    while (clientSessionProducer.hasNext()) {
      ClientInputSession clientSession = clientSessionProducer.next();
      Future<Pair<List<SInt>, byte[]>> f = es.submit(new ClientCommunication(clientSession));
      maskPairsFuture.put(clientSession.getClientId(), f);
    }
    SortedMap<Integer, Pair<List<SInt>, byte[]>> maskPairs = new TreeMap<>();
    for (Entry<Integer, Future<Pair<List<SInt>, byte[]>>> e : maskPairsFuture.entrySet()) {
      Pair<List<SInt>, byte[]> p = null;
      p = e.getValue().get();
      maskPairs.put(e.getKey(), p);
    }
    es.shutdown();
    return maskPairs;
  }

  private void broadcastMaskedInput(SortedMap<Integer, Pair<List<SInt>, byte[]>> maskPairs,
      Network network) {
    MessageDigest digest = ExceptionConverter.safe(() -> MessageDigest.getInstance(HASH_ALGO),
        "Unable to instantiate " + HASH_ALGO);
    for (Pair<List<SInt>, byte[]> p : maskPairs.values()) {
      digest.update(p.getSecond());
    }
    byte[] hash = digest.digest();
    network.sendToAll(hash);
    List<byte[]> hashes = network.receiveFromAll();
    for (byte[] peerHash : hashes) {
      if (!Arrays.equals(hash, peerHash)) {
        throw new MaliciousException("Broadcast validation hash not matching.");
      }
    }
  }

  private static class UnMaskingApp
      implements Application<Map<Integer, List<SInt>>, ProtocolBuilderNumeric> {

    private final SortedMap<Integer, Pair<List<SInt>, byte[]>> maskPairs;
    private final ByteSerializer<BigInteger> serializer;

    public UnMaskingApp(SortedMap<Integer, Pair<List<SInt>, byte[]>> maskPairs,
        ByteSerializer<BigInteger> serializer) {
      this.maskPairs = maskPairs;
      this.serializer = serializer;
    }

    @Override
    public DRes<Map<Integer, List<SInt>>> buildComputation(ProtocolBuilderNumeric builder) {
      return builder.par(this::unMaskAllInputs);
    }

    private DRes<Map<Integer, List<SInt>>> unMaskAllInputs(ProtocolBuilderNumeric par) {
      Map<Integer, DRes<List<SInt>>> inputMap = new HashMap<>(maskPairs.size());
      for (Entry<Integer, Pair<List<SInt>, byte[]>> e : maskPairs.entrySet()) {
        Pair<List<SInt>, byte[]> maskPair = e.getValue();
        List<SInt> masks = maskPair.getFirst();
        List<BigInteger> masked = serializer.deserializeList(maskPair.getSecond());
        DRes<List<SInt>> unMasked = par.par(unMaskClientInputs(masks, masked));
        inputMap.put(e.getKey(), unMasked);
      }
      return () -> inputMap.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().out()));
    }

    private ComputationParallel<List<SInt>, ProtocolBuilderNumeric> unMaskClientInputs(
        List<SInt> masks, List<BigInteger> masked) {
      return (builder) -> {
        Iterator<SInt> maskIt = masks.iterator();
        Iterator<BigInteger> maskedIt = masked.iterator();
        List<DRes<SInt>> unMaskedInputs = new ArrayList<>(masks.size());
        while (maskedIt.hasNext() && maskIt.hasNext()) {
          unMaskedInputs.add(builder.numeric().add(maskedIt.next(), maskIt.next()));
        }
        return () -> unMaskedInputs.stream().map(DRes::out).collect(Collectors.toList());
      };
    }
  }



  private static class ClientCommunication implements Callable<Pair<List<SInt>, byte[]>> {

    private final ClientInputSession session;

    public ClientCommunication(ClientInputSession session) {
      this.session = session;
    }

    @Override
    public Pair<List<SInt>, byte[]> call() throws Exception {
      TwoPartyNetwork net = session.getNetwork();
      byte[] msg = null;
      List<Pair<SInt, Triple<BigInteger>>> triples =
          session.getTripledistributor().getTriples(session.getAmountOfInputs());
      List<BigInteger> flatTriples = new ArrayList<>(triples.size() * 3);
      for (Pair<SInt, Triple<BigInteger>> pair : triples) {
        Triple<BigInteger> t = pair.getSecond();
        flatTriples.add(t.getFirst());
        flatTriples.add(t.getSecond());
        flatTriples.add(t.getThird());
      }
      net.send(session.getSerializer().serialize(flatTriples));
      msg = net.receive();
      return new Pair<>(triples.stream().map(Pair::getFirst).collect(Collectors.toList()), msg);
    }

  }

  @Override
  public Future<Map<Integer, List<SInt>>> getClientInputs() {
    return clientInputs;
  }

}
