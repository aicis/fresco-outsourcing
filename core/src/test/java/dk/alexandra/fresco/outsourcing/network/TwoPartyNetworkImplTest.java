package dk.alexandra.fresco.outsourcing.network;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.NetworkUtil;
import dk.alexandra.fresco.framework.network.CloseableNetwork;
import dk.alexandra.fresco.framework.network.socket.SocketNetwork;
import dk.alexandra.fresco.outsourcing.network.ClientSideNetworkFactory.Parties;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TwoPartyNetworkImplTest {

  private TwoPartyNetwork serverNetwork;
  private TwoPartyNetwork clientNetwork;

  /**
   * Sets up a network for a client and a server.
   *
   * @throws Exception if connecting the network fails.
   */
  @BeforeEach
  public void setUp() throws Exception {
    Map<Integer, NetworkConfiguration> conf = NetworkUtil.getNetworkConfigurations(2);
    ExecutorService es = Executors.newFixedThreadPool(2);
    Future<CloseableNetwork> n1 =
        es.submit(() -> new SocketNetwork(conf.get(Parties.SERVER.id())));
    Future<CloseableNetwork> n2 =
        es.submit(() -> new SocketNetwork(conf.get(Parties.CLIENT.id())));
    this.serverNetwork = new TwoPartyNetworkImpl(n1.get(), Parties.SERVER.id());
    this.clientNetwork = new TwoPartyNetworkImpl(n2.get(), Parties.CLIENT.id());
  }

  @AfterEach
  public void tearDown() throws Exception {
    this.serverNetwork.close();
    this.clientNetwork.close();
  }

  @SuppressWarnings("resource")
  @Test
  public void testTooManyParties() throws InterruptedException, ExecutionException {
    Map<Integer, NetworkConfiguration> conf = NetworkUtil.getNetworkConfigurations(3);
    ExecutorService es = Executors.newFixedThreadPool(3);
    Future<CloseableNetwork> n1 = es.submit(() -> new SocketNetwork(conf.get(1)));
    es.submit(() -> new SocketNetwork(conf.get(2)));
    es.submit(() -> new SocketNetwork(conf.get(3)));
    assertThrows(IllegalArgumentException.class, ()-> new TwoPartyNetworkImpl(n1.get(),
        Parties.SERVER.id()));
  }

  @Test
  public void testConnectAndClose() {
    // Just do the setup and tear down
  }

  @Test
  public void testCommunication() {
    byte[] msg1 = new byte[]{0x01, 0x02};
    clientNetwork.send(msg1);
    byte[] msg2 = serverNetwork.receive();
    assertArrayEquals(msg1, msg2);
    byte[] msg3 = new byte[]{0x03, 0x04, 0x05};
    serverNetwork.send(msg3);
    byte[] msg4 = clientNetwork.receive();
    assertArrayEquals(msg4, msg3);
  }

}
