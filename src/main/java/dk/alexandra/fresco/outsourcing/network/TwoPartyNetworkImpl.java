package dk.alexandra.fresco.outsourcing.network;

import dk.alexandra.fresco.framework.network.CloseableNetwork;
import java.io.IOException;
import java.util.Objects;

/**
 * An implementation of a two party network that simply wraps a {@link CloseableNetwork}.
 */
public class TwoPartyNetworkImpl implements TwoPartyNetwork {

  private final CloseableNetwork network;
  private final int myId;

  public static enum Parties {
    SERVER(1), CLIENT(2);

    private final int id;

    Parties(int id) {
      this.id = id;
    }

    public int id() {
      return id;
    }

  }

  /**
   * Creates a TwoPartyNetwork from a regular network with just two parties.
   * @param network a closable network
   * @param myId the id of this party
   */
  public TwoPartyNetworkImpl(CloseableNetwork network, int myId) {
    Objects.requireNonNull(network);
    if (network.getNoOfParties() != 2) {
      throw new IllegalArgumentException("Network should be for two parties only");
    }
    this.myId = myId;
    this.network = network;
  }

  @Override
  public void send(byte[] msg) {
    network.send(3 - myId, msg);

  }

  @Override
  public byte[] receive() {
    return network.receive(3 - myId);
  }

  @Override
  public void close() throws IOException {
    network.close();
  }

}
