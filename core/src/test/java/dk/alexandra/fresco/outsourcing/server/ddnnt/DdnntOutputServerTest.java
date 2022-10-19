package dk.alexandra.fresco.outsourcing.server.ddnnt;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.outsourcing.client.OutputClient;
import dk.alexandra.fresco.outsourcing.client.ddnnt.DdnntOutputClient;
import dk.alexandra.fresco.outsourcing.server.GenericOutputServerTest;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;

import java.util.List;

public class DdnntOutputServerTest extends GenericOutputServerTest {
    @Override
    protected SpdzWithIO.Protocol getProtocol() {
        return SpdzWithIO.Protocol.DDNNT;
    }

  // TODO handle thse methods
    @Override
    protected OutputClient getOutputClient(int id, List<Party> servers) {
        return new DdnntOutputClient(id, servers);
    }

  private void serverSideProtocol(Future<SpdzWithIO> futureServer, List<BigInteger> toOutput) {
    try {
      SpdzWithIO server = futureServer.get();
      List<SInt> out = server.run((builder) -> {
        DRes<List<DRes<SInt>>> secretShares = dk.alexandra.fresco.lib.common.collections.Collections.using(builder).closeList(toOutput, 1);
        return () -> secretShares.out().stream().map(DRes::out).collect(Collectors.toList());
      });
      server.sendOutputsTo(OUTPUT_CLIENT_ID, out);
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
}
