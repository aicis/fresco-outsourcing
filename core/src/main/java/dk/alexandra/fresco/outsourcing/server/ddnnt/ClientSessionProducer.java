package dk.alexandra.fresco.outsourcing.server.ddnnt;


public interface ClientSessionProducer {

  DdnntClientInputSession nextInput();

  boolean hasNextInput();

  DdnntClientInputSession nextOutput();

  boolean hasNextOutput();

}
