package dk.alexandra.fresco.outsourcing.jno;

import dk.alexandra.fresco.framework.network.serializers.ByteSerializer;
import java.util.List;

public class ClientPayload<T> {
  private final T t;
  private final T k;
  private final T r;
  private final List<T> x;

  /**
   * Creates a new triple.
   *
   * @param t Tag share
   * @param k Key share
   * @param x Input share
   */
  public ClientPayload(T t, T k, T r, List<T> x) {
    this.t = t;
    this.k = k;
    this.r = r;
    this.x = x;
  }

  public T getT() {
    return t;
  }

  public T getK() {
    return k;
  }

  public T getR() {
    return r;
  }

  public List<T> getX() {
    return x;
  }

  public static <T> ClientPayload deserialize(ByteSerializer<T> serializer, byte[] t, byte[] k, byte[] r, byte[] xList) {
    T deserializedT = serializer.deserialize(t);
    T deserializedK = serializer.deserialize(k);
    T deserializedR = serializer.deserialize(r);
    List<T> deserializedX = serializer.deserializeList(xList);
    return new ClientPayload(deserializedT, deserializedK, deserializedR, deserializedX);
  }


}
