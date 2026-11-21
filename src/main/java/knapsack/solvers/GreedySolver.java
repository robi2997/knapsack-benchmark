package knapsack.solvers;

import knapsack.Instance;
import knapsack.Solution;
import knapsack.Solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts items by value per unit of weight and takes them in that order while
 * they still fit.
 *
 * <p>Fast, O(n log n), but <strong>not exact</strong>. The ratio is only a
 * heuristic for which items are worth their space, and a single greedy pass
 * cannot reconsider an earlier choice. There is no constant bound on how far
 * off it can be: a bag holding one item of value 1 instead of one of value
 * 1000 is a legal greedy outcome.
 *
 * <p>How badly it does in practice depends on the instance. When ratios vary a
 * lot the order carries real information; when every item has nearly the same
 * ratio the sort is close to arbitrary. That is the effect the benchmark
 * measures across instance classes.
 */
public final class GreedySolver implements Solver {

    /** An item together with the position it had in the original instance. */
    private record Item(int index, int weight, int value, double ratio) { }

    @Override
    public Solution solve(Instance instance) {
        boolean[] taken = new boolean[instance.size()];
        int totalValue = 0;
        int remaining = instance.capacity();

        List<Item> items = new ArrayList<>(instance.size());

        for (int i = 0; i < instance.size(); i++) {
            int weight = instance.weight(i);
            int value = instance.value(i);

            if (weight == 0) {
                // Free value: a weightless item costs no capacity, so take it
                // whenever it is worth anything. It also has no finite ratio,
                // which is why it is handled before the sort rather than in it.
                if (value > 0) {
                    taken[i] = true;
                    totalValue += value;
                }
            } else {
                items.add(new Item(i, weight, value, (double) value / weight));
            }
        }

        items.sort(Comparator.comparingDouble(Item::ratio).reversed());

        for (Item item : items) {
            if (item.weight() <= remaining) {
                taken[item.index()] = true;
                remaining -= item.weight();
                totalValue += item.value();
            }
        }

        return new Solution(totalValue, taken);
    }

    @Override
    public String name() {
        return "greedy";
    }

    @Override
    public boolean isExact() {
        return false;
    }
}
