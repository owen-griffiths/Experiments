package com.omgcodes;

/**
 * Created by owen.griffiths on 02-May-17.
 * Container class to hold output of Floyd Warshall Calculation. No logic.
 */
public class FloydWarshallOutput {
    public static final int nullNodeIndex = -1;

    public final String[] indexToId;

    // previousNodeIndex[i][j] Stores index of last node on best path from i to j.
    // nullNodeIndex if there is a direct link, or if no path exists.
    public final int[][] previousNodeIndex;

    // bestDistances[i][j] stores the length of the best path from i to j.
    public final double[][] bestDistances;

    public FloydWarshallOutput(
            String[] indexToId,
            int[][] previousNodeIndex,
            double[][] bestDistances) {
        this.indexToId = indexToId;
        this.previousNodeIndex = previousNodeIndex;
        this.bestDistances = bestDistances;
    }
}
