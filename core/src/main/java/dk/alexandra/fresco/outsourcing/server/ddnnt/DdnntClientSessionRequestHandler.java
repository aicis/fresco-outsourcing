package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.outsourcing.server.ClientSessionRegistration;

/**
 * Handles input and output client session requests for the DDNNT protocol.
 */
public interface DdnntClientSessionRequestHandler {

  void setInputRegistrationHandler(ClientSessionRegistration<DdnntClientInputSession> handler);

  void setOutputRegistrationHandler(ClientSessionRegistration<DdnntClientOutputSession> handler);

  void launch();

}
