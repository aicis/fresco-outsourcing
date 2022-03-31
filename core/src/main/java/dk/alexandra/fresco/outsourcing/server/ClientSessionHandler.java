package dk.alexandra.fresco.outsourcing.server;

public interface ClientSessionHandler<T extends ClientSession> extends ClientSessionRegistration<T>,
        ClientSessionProducer<T> {
}
