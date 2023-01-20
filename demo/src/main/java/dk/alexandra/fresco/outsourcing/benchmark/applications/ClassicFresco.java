package dk.alexandra.fresco.outsourcing.benchmark.applications;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.benchmark.ClientPPP;
import dk.alexandra.fresco.outsourcing.benchmark.Hole;
import dk.alexandra.fresco.outsourcing.benchmark.ServerPPP;
import dk.alexandra.fresco.outsourcing.server.jno.JnoInputServer;
import dk.alexandra.fresco.outsourcing.server.jno.JnoOutputServer;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO;

import java.math.BigInteger;
import java.util.*;

public class ClassicFresco extends ServerPPP {
    private List<BigInteger> res;

    public ClassicFresco(int myId, Map<Integer, String> serverIdIpMap, int bitLength, int basePort) {
        super(myId, serverIdIpMap, bitLength, basePort);
    }

    @Override
    public void run(Hole blackhole) {
        Application<BigInteger, ProtocolBuilderNumeric> app = builder -> {
            // TODO insert real application here
            return builder.seq((seq) -> {
                return ()-> BigInteger.valueOf(42);
            });
        };
        res = Collections.singletonList(spdz.run(app));
    }


    @Override
    public void beforeEach() {
        spdz = new SpdzWithIO(myId, maxServers, currentBasePort,
                Collections.singletonList(ClientPPP.CLIENT_ID),
                Collections.singletonList(ClientPPP.CLIENT_ID + 1), serverIdIpMap,
                ((endpoint, sessionProducer) -> null),
                ((endpoint, sessionProducer) -> null),
                bitLength, true,
                SpdzWithIO.Protocol.GENERIC);
    }
}
