package knapsack.solvers;

import knapsack.Instance;
import knapsack.Solution;
import knapsack.Solver;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The same include/exclude search as {@link BruteForceSolver}, with two
 * additions that cut most of the tree away.
 *
 * <p><strong>Ordering.</strong> Items are visited in decreasing value per unit
 * of weight, so the search finds a good solution early. A high best-so-far is
 * what makes pruning effective, and arriving at it in the first few branches is
 * worth far more than arriving at it in the last.
 *
 * <p><strong>Bounding.</strong> At each node an upper bound is computed on
 * anything reachable below it, by solving the <em>fractional</em> relaxation of
 * what remains: take whole items in ratio order while they fit, then a fraction
 * of the next one to fill the bag exactly. Allowing a fraction can only help,
 * so the result is never below the true optimum of that subtree. When the bound
 * does not beat the best solution already found, nothing below can either, and
 * the whole subtree is skipped without being visited.
 *
 * <p>Both algorithms are O(2^n) in the worst case and both return the exact
 * optimum. The difference is how much of that worst case actually gets
 * explored, which {@link #nodesExplored()} reports.
 */
public final class BranchAndBoundSolver implements Solver {

    private int n;
    private int[] weight;          // sorted by ratio, descending
    private int[] value;
    private int[] originalIndex;   // originalIndex[i] = where item i came from

    private boolean[] current;
    private boolean[] best;
    private int bestValue;
    private long nodes;

    private final long nodeLimit;

    /** Searches without any limit. */
    public BranchAndBoundSolver() {
        this(Long.MAX_VALUE);
    }

    /**
     * Gives up after {@code nodeLimit} nodes by throwing {@link NodeLimitExceeded}.
     * Pruning helps enormously on some instance families and barely at all on
     * others, so a limit keeps one hard instance from stalling a whole run.
     */
    public BranchAndBoundSolver(long nodeLimit) {
        this.nodeLimit = nodeLimit;
    }

    /** Thrown when the search exceeds its node budget. */
    public static final class NodeLimitExceeded extends RuntimeException {
        NodeLimitExceeded(long limit) {
            // no stack trace: this is a control signal, not a fault
            super("search exceeded " + limit + " nodes", null, false, false);
        }
    }

    @Override
    public Solution solve(Instance instance) {
        n = instance.size();
        sortByRatio(instance);

        current = new boolean[n];
        best = new boolean[n];
        bestValue = 0;
        nodes = 0;

        search(0, instance.capacity(), 0);

        // map the answer back to the order the caller gave us
        boolean[] taken = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (best[i]) {
                taken[originalIndex[i]] = true;
            }
        }
        return new Solution(bestValue, taken);
    }

    private void sortByRatio(Instance instance) {
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(
                (Integer i) -> ratio(instance.weight(i), instance.value(i))).reversed());

        weight = new int[n];
        value = new int[n];
        originalIndex = new int[n];
        for (int i = 0; i < n; i++) {
            originalIndex[i] = order[i];
            weight[i] = instance.weight(order[i]);
            value[i] = instance.value(order[i]);
        }
    }

    /** Weightless items are worth taking before anything that costs capacity. */
    private static double ratio(int weight, int value) {
        if (weight == 0) {
            return value > 0 ? Double.POSITIVE_INFINITY : 0.0;
        }
        return (double) value / weight;
    }

    private void search(int i, int remaining, int valueSoFar) {
        if (++nodes > nodeLimit) {
            throw new NodeLimitExceeded(nodeLimit);
        }

        if (valueSoFar > bestValue) {
            bestValue = valueSoFar;
            best = current.clone();
        }

        if (i == n) {
            return;
        }

        // Nothing below this node can beat what we already have.
        if (upperBound(i, remaining, valueSoFar) <= bestValue) {
            return;
        }

        if (weight[i] <= remaining) {
            current[i] = true;
            search(i + 1, remaining - weight[i], valueSoFar + value[i]);
            current[i] = false;
        }

        search(i + 1, remaining, valueSoFar);
    }

    /**
     * Optimum of the fractional relaxation over items i..n-1, which is an upper
     * bound on the integral optimum of the same subproblem.
     */
    private double upperBound(int i, int remaining, int valueSoFar) {
        double bound = valueSoFar;
        int capacityLeft = remaining;
        int j = i;

        while (j < n && weight[j] <= capacityLeft) {
            capacityLeft -= weight[j];
            bound += value[j];
            j++;
        }

        // the item that does not fit whole contributes the fraction that does
        if (j < n && weight[j] > 0) {
            bound += (double) value[j] * capacityLeft / weight[j];
        }

        return bound;
    }

    @Override
    public String name() {
        return "branch-and-bound";
    }

    @Override
    public long nodesExplored() {
        return nodes;
    }
}
