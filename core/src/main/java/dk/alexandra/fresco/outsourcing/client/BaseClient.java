package dk.alexandra.fresco.outsourcing.client;

import dk.alexandra.fresco.framework.Party;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.outsourcing.network.TwoPartyNetwork;
import java.util.List;
import java.util.Map;

public interface BaseClient {
  Map<Integer, TwoPartyNetwork> getServerNetworks();
  List<Party> getServers();
  FieldDefinition getDefinition();
  int getClientId();
}
