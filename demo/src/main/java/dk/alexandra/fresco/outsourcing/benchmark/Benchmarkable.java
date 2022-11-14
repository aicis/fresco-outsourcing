package dk.alexandra.fresco.outsourcing.benchmark;

public interface Benchmarkable {
  void setup();
  void beforeEach();
  void run(Hole blackhole);
  void afterEach();
}
