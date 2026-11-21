package knapsack;

/**
 * A strategy for solving 0/1 knapsack instances.
 *
 * <p>Every algorithm implements this, so the benchmark can loop over a list of
 * solvers instead of repeating the same timing code once per algorithm.
 */
public interface Solver {

    /** Solve the instance and return the value together with the chosen items. */
    Solution solve(Instance instance);

    /** Short name used in result files and charts. */
    String name();

    /**
     * Whether this solver always returns the optimum. The benchmark uses this
     * to pick a reference value when measuring the error of approximate ones.
     */
    default boolean isExact() {
        return true;
    }

    /**
     * How many search nodes the last call to {@link #solve} visited, or -1 for
     * solvers that do not search. Comparing this between brute force and
     * branch and bound is how the effect of pruning is measured.
     */
    default long nodesExplored() {
        return -1;
    }
}
