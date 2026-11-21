package knapsack.solvers;

import knapsack.Instance;
import knapsack.Solution;
import knapsack.Solver;

/**
 * Exhaustive search over every subset of items, written as an include/exclude
 * recursion: at item i, try taking it and try skipping it, keep the better.
 *
 * <p>Exact, and the simplest thing that is obviously correct, which is what
 * makes it useful as a reference the other solvers are checked against. It
 * explores 2^n leaves with no pruning whatsoever, so it is only usable for
 * small n. {@link BranchAndBoundSolver} is the same search with pruning added.
 */
public final class BruteForceSolver implements Solver {

    private int[] weights;
    private int[] values;
    private boolean[] current;
    private boolean[] best;
    private int bestValue;
    private long nodes;

    @Override
    public Solution solve(Instance instance) {
        weights = instance.weights();
        values = instance.values();
        current = new boolean[instance.size()];
        best = new boolean[instance.size()];
        bestValue = 0;
        nodes = 0;

        search(0, instance.capacity(), 0);

        return new Solution(bestValue, best);
    }

    /**
     * @param i             index of the item being decided
     * @param remaining     capacity still free
     * @param valueSoFar    value of the items already taken
     */
    private void search(int i, int remaining, int valueSoFar) {
        nodes++;

        if (i == weights.length) {
            if (valueSoFar > bestValue) {
                bestValue = valueSoFar;
                // clone, not assign: current keeps changing as the search backtracks
                best = current.clone();
            }
            return;
        }

        // branch 1: take item i, if it fits
        if (weights[i] <= remaining) {
            current[i] = true;
            search(i + 1, remaining - weights[i], valueSoFar + values[i]);
            current[i] = false;   // undo before trying the other branch
        }

        // branch 2: skip item i
        search(i + 1, remaining, valueSoFar);
    }

    @Override
    public String name() {
        return "brute-force";
    }

    @Override
    public long nodesExplored() {
        return nodes;
    }
}
