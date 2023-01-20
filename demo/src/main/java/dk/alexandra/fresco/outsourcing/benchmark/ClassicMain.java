package dk.alexandra.fresco.outsourcing.benchmark;

import dk.alexandra.fresco.outsourcing.benchmark.applications.ClassicFresco;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassicMain {
    // e.g. jmh-reports/EmptyMethod.json
    private static final String reportFileDir = "jmh-reports/";

    // Arguments; c/s for client/ server, followed by id, then all IPs
    // Arguments: <bitlength> <id> <IPs>
    // Where bitlength is the size of the field to use
    //       id is the ID of a the current server which is a number monotonely increasing, starting from 1
    //       ip is the ip address of all servers, separated with space, e.g. 2 10.11.22.33 192.168.1.1
    //          the first IP is the IP of server with ID 1, the second is of the server with ID 2. Thus in the example
    //          the caller is server 2 and has IP 192.168.1.1
    public static void main(String[] args) throws Exception {
        final int bitlength =  Integer.parseInt(args[0]);
        final int myId = Integer.parseInt(args[1]);
        final int maxServers = args.length - 2;
        Map<Integer, String> serverIdIpMap = new HashMap<>();
        for (int id = 1; id <= maxServers; id++) {
            serverIdIpMap.put(id, args[id + 1]);
        }
        Map<Integer, List<PPP>> amountOfServersToBenchmarks = setupBenchmark(myId, serverIdIpMap, bitlength);
        List<String> results = Collections.singletonList("No results finished");
        try {
            results = runBenchmark(amountOfServersToBenchmarks);
        } finally {
            writeResults(reportFileDir + "/" + bitlength + "/" + myId, results);
        }

    }

    private static Map<Integer, List<PPP>> setupBenchmark(int myId, Map<Integer, String> serverIdIpMap, int bitLength) throws Exception {
        int basePort = PPP.BASE_PORT;
        Map<Integer, String> currentMap = serverIdIpMap;
        Map<Integer, List<PPP>> amountOfServersToBenchmarks = new HashMap<>();
        while (currentMap.size() >= 2 && myId <= currentMap.size()) {
            List<PPP> currentList = new ArrayList<>();

            // Add the different tests to run
            // TODO insert different programs to run
            currentList.add(new ClassicFresco(myId, currentMap, bitLength, basePort));
            basePort += currentMap.size() * 3 * (Benchmark.WARMUP + Benchmark.ITERATIONS);

            amountOfServersToBenchmarks.put(currentMap.size(), currentList);
            // Make a new map, excluding the last server
            currentMap = new HashMap<>(currentMap);
            currentMap.remove(currentMap.size());
        }
        return amountOfServersToBenchmarks;
    }

    private static List<String> runBenchmark(Map<Integer, List<PPP>> amountOfServersToBenchmarks) {
        List<String> results = new ArrayList<>();
        List<Integer> sortedAmountsList = amountOfServersToBenchmarks.keySet().stream().sorted().collect(Collectors.toList());
        Collections.reverse(sortedAmountsList);
        for (int servers: sortedAmountsList) {
            results.add("Testing " + servers + " servers");
            for (PPP currentBenchmark : amountOfServersToBenchmarks.get(servers)) {
                System.out.println("Starting new bench...");
                results.add(Benchmark.parseTimes(currentBenchmark.getClass().getName(),
                        Benchmark.runBenchmark(currentBenchmark)));
            }
        }
        return results;
    }

    private static void writeResults(String directory, List<String> toWrite) throws IOException {
        if (!Files.exists(Paths.get(directory))) {
            Files.createDirectories(Paths.get(directory));
        }
        Path filePath = Paths.get(directory + "/benchmark.csv");
        BufferedWriter buffer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
        for (String currentLine : toWrite) {
            buffer.write(currentLine + "\n");
        }
        buffer.close();
    }
}
