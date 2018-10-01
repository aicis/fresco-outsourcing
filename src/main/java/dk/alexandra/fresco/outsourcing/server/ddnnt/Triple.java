package dk.alexandra.fresco.outsourcing.server.ddnnt;

public class Triple<T> {

  private final T first;
  private final T second;
  private final T third;

  /**
   * Creates a new triple.
   *
   * @param first first element
   * @param second second element
   * @param third third element
   */
  public Triple(T first, T second, T third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public T getFirst() {
    return first;
  }

  public T getSecond() {
    return second;
  }

  public T getThird() {
    return third;
  }




}
