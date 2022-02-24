package dk.alexandra.fresco.outsourcing.demo;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ThresholdRSAGenerator {
  // Conservative since it really doesn't impact performance
  public static final int STAT_SEC = 80;
  private static final BigInteger DEFAULT_EXPONENT = BigInteger.valueOf(65537);

  private final int t;
  private final int n;
  private final BigInteger exponent;
  private final BigInteger delta;
  private final SecureRandom rand;
  private int modulusBits;

  @Deprecated
  public ThresholdRSAGenerator(int t, int n)  throws NoSuchAlgorithmException {
    // TODO Runs with static randomness, DON'T!!!!! use except for tests and benchmarking
    this(t, n, DEFAULT_EXPONENT, SecureRandom.getInstance("SHA1PRNG"));
  }

  public ThresholdRSAGenerator(int t, int n, BigInteger exponent, SecureRandom rand) {
    if (exponent.compareTo(BigInteger.valueOf(n)) <= 0) {
      throw new IllegalArgumentException("Exponent must be larger than the amount of parties");
    }
    this.t = t;
    this.n = n;
    this.exponent = exponent;
    this.delta = factorial(n);
    this.rand = rand;
  }

  public List<RSAShares> generateShares(int modulusBits) {
    this.modulusBits = modulusBits;
    BigInteger p = generateCandidate(modulusBits);
    BigInteger q = generateCandidate(modulusBits);
    BigInteger N = p.multiply(q);
    byte[] by = N.toByteArray();
    BigInteger d = computeD(p, q);
    System.out.println("D is " + d);
    List<BigInteger> additiveDShares = additiveSharing(d, t).stream().map(cur ->
        delta.multiply(delta).multiply(cur))
        .collect(Collectors.toList());
    List<BigInteger> coef =  new ArrayList<>();
    coef.add(additiveDShares.get(0));
    coef.addAll(sampleCoef(delta.pow(10).multiply(BigInteger.valueOf(n*n)), t));
    for (int i = 0; i < t; i++) {
      List<BigInteger> currentCoef = new ArrayList<>();
      currentCoef.add(additiveDShares.get(i));
      currentCoef.addAll(sampleCoef(delta.pow(10).multiply(BigInteger.valueOf(n*n)), t));
      for (int j = 0; j < coef.size(); j++) {
        coef.set(j, coef.get(j).add(currentCoef.get(j)));
      }
    }
    List<BigInteger> shares = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
      BigInteger currentShare = BigInteger.ZERO;
      for (int j =0; j < coef.size(); j++) {
        currentShare = currentShare.add(BigInteger.valueOf(i).pow(j).multiply(coef.get(j)));
      }
      shares.add(currentShare);
    }
    BigInteger v = getRandomNumber((modulusBits+STAT_SEC) / 8).mod(N);
    return shares.stream().map
        (cur -> new RSAShares(v, N, cur,
            v.modPow(cur.multiply(delta).multiply(delta), N), delta))
        .collect(Collectors.toList());
  }

  private static BigInteger factorial(int N)
  {
    BigInteger f = new BigInteger("1");
    for (int i = 2; i <= N; i++)
      f = f.multiply(BigInteger.valueOf(i));
    return f;
  }


  private BigInteger generateCandidate(int modulusBits) {
    BigInteger res = BigInteger.probablePrime(modulusBits/2, rand);
    while (!res.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2)).gcd(delta).equals(BigInteger.ONE)) {
      res = BigInteger.probablePrime(modulusBits/2, rand);
    }
    return res;
  }

  private BigInteger computeD(BigInteger p, BigInteger q) {
    BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
    if (!phi.gcd(exponent).equals(BigInteger.ONE)) {
      throw new IllegalArgumentException("GCD(phi, e) != 1");
    }
    return exponent.modPow(phi.subtract(BigInteger.ONE), phi);
  }

  private List<BigInteger> additiveSharing(BigInteger input, int amount) {
    List<BigInteger> res = new ArrayList<>();
    for (int i = 0; i < amount-1; i++) {
      int bit = rand.nextBoolean() == true ? 1 : -1;
      int size = (modulusBits+STAT_SEC) / 8;
      BigInteger randomNumber = getRandomNumber(size);
      res.add(randomNumber.multiply(BigInteger.valueOf(bit)));
    }
    BigInteger sum = res.stream().reduce(BigInteger::add).get();
    res.add(input.subtract(sum));
    return res;
  }

  private List<BigInteger> sampleCoef(BigInteger upperBound, int amount) {
    List<BigInteger> res = new ArrayList<>();
    int sampleBound = 1+ (upperBound.multiply(BigInteger.valueOf(STAT_SEC)).bitLength() / 8);
    for (int i = 0; i < amount; i++) {
      res.add(getRandomNumber(sampleBound).mod(upperBound));
    }
    return res;
  }

  private BigInteger getRandomNumber(int bytes) {
    byte[] randomBytes = new byte[bytes];
    rand.nextBytes(randomBytes);
    return new BigInteger(1, randomBytes);
  }

  public static class RSAShares {
    private final BigInteger v;
    private final BigInteger N;
    private final BigInteger share;
    private final BigInteger verification;
    private final BigInteger delta;

    public RSAShares(BigInteger v, BigInteger N, BigInteger share, BigInteger verification,
        BigInteger delta) {
      this.v = v;
      this.N = N;
      this.share = share;
      this.verification = verification;
      this.delta = delta;
    }

    public BigInteger getV() {
      return v;
    }

    public BigInteger getN() {
      return N;
    }

    public BigInteger getShare() {
      return share;
    }

    public BigInteger getVerification() {
      return verification;
    }

    public BigInteger getDelta() {
      return delta;
    }

    @Override
    public String toString() {
      return "N: " + N.toString() + "\nV: " + v.toString() + "\nVerification: " + verification.toString() + "\nDelta: " + delta.toString() + "\nShare: " + share.toString();
    }

  }

}
