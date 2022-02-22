package dk.alexandra.fresco.outsourcing.server;

/**
 * Handles input and output client session requests for the DDNNT protocol.
 */
public interface ClientSessionRequestHandler<T extends ClientSession, K extends ClientSession> {

  void setInputRegistrationHandler(ClientSessionRegistration<T> handler);

  void setOutputRegistrationHandler(ClientSessionRegistration<K> handler);

  void launch();

}
