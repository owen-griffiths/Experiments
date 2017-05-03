package com.omgcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Created by owen.griffiths on 02-May-17.
 */
class Graph {
    private final Map<String, Integer> idToIndex;
    private final Arc[] arcs;

    public Graph(Map<String, Integer> idToIndex, Arc[] arcs) {
        this.idToIndex = idToIndex;
        this.arcs = arcs;
    }

    public Integer lookupId(String id) {
        return idToIndex.getOrDefault(id, null);
    }

    public FloydWarshallOutput floydWarshall() {
        final int size = idToIndex.size();
        final int elementCount = size * size;
        long startTimeMs = System.currentTimeMillis();
        System.out.printf("Calculating Floyd - Warshall with %,d elements in bestDistances array\n", elementCount);

        final String[] indexToId = new String[size];
        for (String id : idToIndex.keySet()) {
            int index = idToIndex.get(id);
            indexToId[index] = id;
        }

        // Seed array of best lengths. +Inf unless there is a direct arc from i to j.
        final double[][] bestDistances = new double[size][size];
        // Seed array of previous stops. nullNodeIndex for direct link / no path found.
        // This array should only be checked if corresponding element in bestDistances != +Inf
        final int[][] previousNodeIndex = new int[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                bestDistances[i][j] = Double.POSITIVE_INFINITY;
                previousNodeIndex[i][j] = FloydWarshallOutput.nullNodeIndex;
            }
        }

        for (Arc arc : arcs) {
            bestDistances[arc.originIndex][arc.destinationIndex] = arc.length;
        }

        // Find shortest paths. NB N^3 complexity.
        for (int k = 0; k < size; k++) {
            if ((k % 100) == 0) {
                long progressMs = System.currentTimeMillis() - startTimeMs;
                System.out.printf("%,8d[ms] : Processing Intermediate node %,d (%s)\n", progressMs, k, indexToId[k]);
            }

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    // Calculate path from (i -> k -> j), and compare to existing (i -> j) best length.
                    double distIToK = bestDistances[i][k];
                    double distKToJ = bestDistances[k][j];
                    if ((distIToK != Double.POSITIVE_INFINITY) && (distKToJ != Double.POSITIVE_INFINITY)) {
                        double distIToJViaK = distIToK + distKToJ;
                        if (distIToJViaK < bestDistances[i][j]) {
                            bestDistances[i][j] = distIToJViaK;
                            previousNodeIndex[i][j] = k;
                        }
                    }
                }
            }
        }

        long takenMs = System.currentTimeMillis() - startTimeMs;
        System.out.printf("Took %,d[ms] to calculate Floyd Warshall\n", takenMs);

        return new FloydWarshallOutput(indexToId, previousNodeIndex, bestDistances);
    }
}

class Arc {
    public final String origin;
    public final int originIndex;
    public final String destination;
    public final int destinationIndex;
    public final double length;

    public Arc(String origin, int originIndex,
               String destination, int destinationIndex,
               double length) {
        this.origin = origin;
        this.originIndex = originIndex;

        this.destination = destination;
        this.destinationIndex = destinationIndex;

        this.length = length;
    }
}
