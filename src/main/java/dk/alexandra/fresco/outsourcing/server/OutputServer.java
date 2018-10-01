package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.value.SInt;
import java.util.List;

public interface OutputServer {

  void putClientOutputs(int clientId, List<SInt> outputs);

}
