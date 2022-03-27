package dk.alexandra.fresco.outsourcing.client.jno;

import dk.alexandra.fresco.framework.MaliciousException;
import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.BigIntegerFieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.ExceptionConverter;
import dk.alexandra.fresco.outsourcing.client.ClientBase;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import dk.alexandra.fresco.outsourcing.utils.ByteConversionUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple demo client for the DDNNT output protocol.
 *
 * <p>Parts of the code contributed by Mathias Rahbek.</p>
 */
public class PestoOutputClient extends ClientBase implements OutputClient {
  private static final Logger logger = LoggerFactory.getLogger(PestoOutputClient.class);
  private final Function<BigInteger, FieldDefinition> definitionSupplier;
  private final Drbg drbg;

  public PestoOutputClient(int clientId,
      List<Party> servers,
      Function<BigInteger, FieldDefinition> definitionSupplier, Drbg drbg) {
    super(clientId, servers);
    ExceptionConverter.safe(() -> {
      this.handshake(definitionSupplier, 1);
      return null;
    }, "Failed client handshake");
    this.definitionSupplier = definitionSupplier;
    this.drbg = drbg;
  }

  public PestoOutputClient(int clientId, List<Party> servers, Drbg drbg) {
    this(clientId, servers, BigIntegerFieldDefinition::new, drbg);
  }

  @Override
  public List<BigInteger> getBigIntegerOutputs() {
    int numOutputs = receiveNumOutputs();
    JnoInputClient randomInputClient = new JnoInputClient(numOutputs, clientId, servers, definitionSupplier, drbg);
    List<BigInteger> inputs = IntStream.range(0, numOutputs)
        .mapToObj(num -> randomInputClient.randomElement().toBigInteger()).collect(Collectors.toList());
    randomInputClient.putBigIntegerInputs(inputs);

    List<byte[]> results = new ArrayList<>();
    for (Party s : servers) {
      TwoPartyNetwork network = serverNetworks.get(s.getPartyId());
      results.add(network.receive());
      logger.info("C{}: Received output shares from server {}", clientId, s);
    }
    // TODO restore result
    boolean res = false;
//    try {
//      SigShare shares[] = new SigShare[results.size()];
//      for (int i = 0; i < results.size(); i++) {
//        shares[i] = (SigShare) deserialize(results.get(i));
//      }
//      MessageDigest md = MessageDigest.getInstance("SHA-256");
//      byte[] msg = md.digest(PestoOutputServer.MSG.getBytes(StandardCharsets.UTF_8));
//      res = SigShare.verify(msg, shares, servers.size(), servers.size()+1, shares[0].getN(), ThreshUtil.F4);
//    } catch (Exception e) {
//      throw new RuntimeException("Could not decode signature", e);
//    }
    return Collections.singletonList(BigInteger.ONE);// TODO res == true ? BigInteger.ONE : BigInteger.ZERO);
  }

  public static Object deserialize(byte[] obj) throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream(obj);
    ObjectInput in = null;
    try {
      in = new ObjectInputStream(bis);
      return in.readObject();
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ex) {
        // ignore close exception
      }
    }
  }

  /**
   * Receives number of outputs from all servers and verifies that info is consistent.
   */
  private int receiveNumOutputs() {
    int numOutputs = getNumOutputsFrom(servers.get(0).getPartyId());
    for (Party s : servers.subList(1, servers.size())) {
      int newNumOutputs = getNumOutputsFrom(s.getPartyId());
      if (newNumOutputs != numOutputs) {
        throw new MaliciousException("Received incorrect number of outputs for servers");
      }
    }
    return numOutputs;
  }

  /**
   * Receives number of outputs from given party.
   */
  private int getNumOutputsFrom(int partyId) {
    TwoPartyNetwork network = serverNetworks.get(partyId);
    return ByteConversionUtils.intFromBytes(network.receive());
  }

  @Override
  public List<Long> getLongOutputs() {
    return getBigIntegerOutputs().stream().map(BigInteger::longValue).collect(Collectors.toList());
  }

  @Override
  public List<Integer> getIntOutputs() {
    return getBigIntegerOutputs().stream().map(BigInteger::intValue).collect(Collectors.toList());
  }

}