package dk.alexandra.fresco.outsourcing.benchmark;

public class Hole {
  private static final Hole hole = new Hole();

  public static Hole getInstance() {
    return hole;
  }

  private Hole() {}

  public void consume(Object x) {
    String something = "something " + x.toString();
    System.out.println(something);
  }
}
