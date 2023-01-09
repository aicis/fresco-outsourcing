package dk.alexandra.fresco.outsourcing.server;

import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.outsourcing.setup.SpdzWithIO.Protocol;
import dk.alexandra.fresco.outsourcing.utils.SpdzSetupUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestDataGenerator {
  private final Protocol protocol;
  private final int inputsPerClient;
  private final int numberOfServers;
  private final int numberOfInputClients;
  private final int outputsPerClient;
  private final int numberOfOutputClients;
  private final BigInteger modulus;

  public TestDataGenerator(Protocol protocol, int inputsPerClient, int numberOfInputClients,
      int outputsPerClient,
      int numberOfOutputClients, int numberOfServers) {
    this(protocol, inputsPerClient, numberOfInputClients, outputsPerClient, numberOfOutputClients
        , numberOfServers, SpdzSetupUtils.DEFAULT_MPC_MODULUS);
  }

  public TestDataGenerator(Protocol protocol, int inputsPerClient, int numberOfInputClients,
      int outputsPerClient,
      int numberOfOutputClients, int numberOfServers, BigInteger mpcModulus) {
    this.protocol = protocol;
    this.inputsPerClient = inputsPerClient;
    this.numberOfInputClients = numberOfInputClients;
    this.outputsPerClient = outputsPerClient;
    this.numberOfOutputClients = numberOfOutputClients;
    this.numberOfServers = numberOfServers;
    this.modulus = mpcModulus;
  }

  public Protocol getProtocol() {
    return protocol;
  }
  public int getInputsPerClient() {
    return inputsPerClient;
  }

  public int getNumberOfServers() {
    return numberOfServers;
  }

  public int getNumberOfInputClients() {
    return numberOfInputClients;
  }

  public int getOutputsPerClient() {
    return outputsPerClient;
  }

  public int getNumberOfOutputClients() {
    return numberOfOutputClients;
  }

  public BigInteger getModulus() {
    return modulus;
  }

  /**
   * Input is a list of id
   * @param id The id of the client
   * @return A List of the value id
   */
  public List<BigInteger> computeInputs(int id) {
    return (List) IntStream.range(0, inputsPerClient).mapToObj((num) -> {
      return BigInteger.valueOf((long)id);
    }).collect(Collectors.toList());
  }

  /**
   * Output is a list of 1s
   * @param id ID of the output client
   * @return A list of 1s
   */
  public List<BigInteger> computeOutputs(int id) {
    return (List)IntStream.range(0, outputsPerClient).mapToObj((num) -> {
      return BigInteger.valueOf(1L);
    }).collect(Collectors.toList());
  }


  /**
   * Map client secret shared input to secret shared output, by letting the output of each
   * party being the first input of the first party.
   * @param clientInput map of the clients; input
   * @return map of the client ID to output
   */
  public Map<Integer, List<SInt>> mapToOutputs(Map<Integer, List<SInt>> clientInput) {
    Map<Integer, List<SInt>> clientOutput = new HashMap<>();
    for (int i = numberOfInputClients + 1; i < numberOfInputClients + 1 + numberOfOutputClients; i++) {
      List<SInt> outputs = new ArrayList<>();
      for (int j = 0; j < outputsPerClient; j++) {
        outputs.add(clientInput.get(1).get(0));
      }
      // The output is the input of the first party
      clientOutput.put(i, outputs);
    }
    return clientOutput;
  }
}
