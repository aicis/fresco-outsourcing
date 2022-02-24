package dk.alexandra.fresco.outsourcing.demo;

import dk.alexandra.fresco.outsourcing.demo.ThresholdRSAGenerator.RSAShares;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ThresholdRSASigner {
  private final MessageDigest digest;
  private final RSAShares shares;

  public ThresholdRSASigner(RSAShares shares) throws NoSuchAlgorithmException {
    this.digest = MessageDigest.getInstance("SHA-256");
    this.shares = shares;
  }

  public BigInteger computePartialSig(String msg) {
    byte[] encodedhash = digest.digest(msg.getBytes(StandardCharsets.UTF_8));
    BigInteger integerMsg = new BigInteger(1, encodedhash);
    return integerMsg.modPow(BigInteger.valueOf(2).multiply(shares.getDelta()).multiply(shares.getDelta()).multiply(shares.getShare()), shares.getN());
  }
}
