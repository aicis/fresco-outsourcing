package dk.alexandra.fresco.outsourcing.server.ddnnt;

/**
 * Handles input and output client session requests for the DDNNT protocol.
 */
public interface DdnntClientSessionRequestHandler {

  void setInputRegistrationHandler(ClientSessionRegistration<DdnntClientInputSession> handler);

  void setOutputRegistrationHandler(ClientSessionRegistration<DdnntClientOutputSession> handler);

  void launch();

}
