package com.omgcodes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        try {
            List<CSVRecord> input = loadInput(args[0]);

            Graph graph = buildGraph(input);
            List<Pair<Integer, Integer>> pathsToSave = loadPathsToOutput(args[1], graph);

            FloydWarshallOutput output = graph.floydWarshall();

            long startTimeMs = System.currentTimeMillis();
            saveBestPaths(pathsToSave, output, args[2]);
            long takenMs = System.currentTimeMillis() - startTimeMs;
            System.out.printf("Took %,d[ms] to save output\n", takenMs);
        } catch (Exception e) {
            System.out.printf("Error: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<CSVRecord> loadInput(String arg) throws IOException {
        File source = new File(arg);
        System.out.printf("Loading Links for %s\n", source.getAbsolutePath());

        CSVFormat format = CSVFormat.DEFAULT.withSkipHeaderRecord();
        CSVParser rd = CSVParser.parse(source, Charset.defaultCharset(), format);
        List<CSVRecord> records = rd.getRecords();
        rd.close();
        System.out.printf("%,d records\n", records.size());

        return records;
    }

    private static Graph buildGraph(List<CSVRecord> links) {
        Arc[] arcs = new Arc[links.size()];
        Map<String, Integer> idToIndex = new HashMap<String, Integer>();
        int arcIndex = 0;
        for (CSVRecord current : links) {
            String origin = current.get(0);
            int originIndex = index(idToIndex, origin);

            String destination = current.get(1);
            int destinationIndex = index(idToIndex, destination);

            double length = Double.parseDouble(current.get(2));

            arcs[arcIndex] = new Arc(origin, originIndex, destination, destinationIndex, length);
            arcIndex++;
        }

        assert arcIndex == arcs.length;

        System.out.printf("%,d Ids indexed, %,d arcs", idToIndex.size(), arcs.length);

        Graph result = new Graph(idToIndex, arcs);
        return result;
    }

    private static int index(Map<String, Integer> idToIndex, String id) {
        if (!idToIndex.containsKey(id)) {
            int result = idToIndex.size();
            idToIndex.put(id, result);
            return result;
        }

        return idToIndex.get(id);
    }

    private static List<Pair<Integer, Integer>> loadPathsToOutput(String filename, Graph graph) throws IOException {
        File source = new File(filename);
        System.out.printf("Saving best paths for OD Pairs defined in %s\n", source.getAbsolutePath());

        CSVFormat format = CSVFormat.DEFAULT.withSkipHeaderRecord();
        CSVParser rd = CSVParser.parse(source, Charset.defaultCharset(), format);
        List<CSVRecord> records = rd.getRecords();
        rd.close();
        System.out.printf("%,d OD Pairs defined\n", records.size());

        Set<String> unknownIds = new HashSet<String>();

        List<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
        for (CSVRecord current : records) {
            String fromId = current.get(0);
            Integer fromIndex = graph.lookupId(fromId);

            String toId = current.get(1);
            Integer toIndex = graph.lookupId(toId);

            if (fromIndex == null) {
                unknownIds.add(fromId);
            }
            if (toIndex == null) {
                unknownIds.add(toId);
            }

            if ((fromIndex != null) && (toIndex != null)) {
                result.add(Pair.of(fromIndex, toIndex));
            }
        }
        System.out.printf("Saving best paths for %,d OD Pairs\n", result.size());
        System.out.printf("%,d unknown ids\n", unknownIds.size());
        System.out.println(String.join(",", unknownIds));

        return result;
    }

    private static void saveBestPaths(
            List<Pair<Integer, Integer>> pathsToSave,
            FloydWarshallOutput output,
            String outputPath) throws IOException {
        long startTimeMs = System.currentTimeMillis();
        System.out.printf("Saving shortest paths to %s\n", outputPath);
        PrintStream wr = new PrintStream(new FileOutputStream(outputPath));

        final int size = output.indexToId.length;
        int countLinesWritten = 0;

        for (Pair<Integer, Integer> odPair : pathsToSave) {
            int fromIndex = odPair.getLeft();
            String originId = output.indexToId[fromIndex];

            int toIndex = odPair.getRight();
            String destinationId = output.indexToId[toIndex];

            double distance = output.bestDistances[fromIndex][toIndex];

            if (distance != Double.POSITIVE_INFINITY) {
                wr.printf("%s,%s,%f,", originId, destinationId, distance);
                outputBestPath(fromIndex, toIndex, output, wr);
                wr.println();

                countLinesWritten++;
            }
        }
        wr.close();

        long takenMs = System.currentTimeMillis() - startTimeMs;
        System.out.printf("Wrote %,d output records in %,d[ms]\n", countLinesWritten, takenMs);
    }

    // Recursive function. Output node in the order they need to be passed to create the best path.
    private static void outputBestPath(
            int originIndex, int destinationIndex,
            FloydWarshallOutput output,
            PrintStream wr) {
        int viaNode = output.previousNodeIndex[originIndex][destinationIndex];
        if (viaNode == FloydWarshallOutput.nullNodeIndex) {
            return;
        }

        // There is a predecessor path from originIndex to previous - output that first.
        outputBestPath(originIndex, viaNode, output, wr);

        // output the current node id on the path.
        String viaNodeId = output.indexToId[viaNode];
        wr.printf("%s;", viaNodeId);
    }
}
