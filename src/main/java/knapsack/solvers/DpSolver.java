package knapsack.solvers;

import knapsack.Instance;
import knapsack.Solution;
import knapsack.Solver;

/**
 * Dynamic programming over capacity.
 *
 * <p>{@code table[i][w]} is the best value reachable using only the first i
 * items with a bag of capacity w. Each entry is the better of two choices:
 * leave item i out, which is {@code table[i-1][w]}, or take it, which is
 * {@code table[i-1][w - weight] + value}.
 *
 * <p>Exact, in O(n * capacity) time. That is pseudo-polynomial, not polynomial:
 * it is linear in the capacity's <em>value</em>, which grows exponentially in the
 * number of digits needed to write it down. So the algorithm is fast on many
 * items with a small capacity, and hopeless on a capacity in the billions.
 *
 * <p>The full table is kept rather than a single rolling row, because the table
 * is what lets the chosen items be recovered afterwards. A rolling row would use
 * O(capacity) memory but only yield the optimal value.
 */
public final class DpSolver implements Solver {

    @Override
    public Solution solve(Instance instance) {
        int n = instance.size();
        int capacity = instance.capacity();

        int[][] table = new int[n + 1][capacity + 1];

        for (int i = 1; i <= n; i++) {
            int weight = instance.weight(i - 1);
            int value = instance.value(i - 1);

            for (int w = 0; w <= capacity; w++) {
                int withoutItem = table[i - 1][w];
                if (weight <= w) {
                    int withItem = table[i - 1][w - weight] + value;
                    table[i][w] = Math.max(withoutItem, withItem);
                } else {
                    table[i][w] = withoutItem;
                }
            }
        }

        return new Solution(table[n][capacity], recoverItems(instance, table));
    }

    /**
     * Walk the table backwards from the final cell. If the value changed when
     * item i became available, that item must have been taken to reach it.
     */
    private boolean[] recoverItems(Instance instance, int[][] table) {
        boolean[] taken = new boolean[instance.size()];
        int w = instance.capacity();

        for (int i = instance.size(); i > 0; i--) {
            if (table[i][w] != table[i - 1][w]) {
                taken[i - 1] = true;
                w -= instance.weight(i - 1);
            }
        }
        return taken;
    }

    @Override
    public String name() {
        return "dp";
    }
}
