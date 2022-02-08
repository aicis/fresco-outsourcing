package dk.alexandra.fresco.outsourcing.benchmark;

public interface Benchmarkable {
  public void setup();
  public void beforeEach();
  public void run(Hole blackhole);
  public void afterEach();
}
