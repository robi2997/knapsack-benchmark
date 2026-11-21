package knapsack;

/**
 * The result of solving an instance: the total value achieved, and which items
 * were taken. {@code taken[i]} is true when item i is in the bag.
 */
public record Solution(int value, boolean[] taken) {

    public Solution {
        if (taken == null) {
            throw new IllegalArgumentException("taken must not be null");
        }
    }

    /**
     * Total weight of the chosen items, used to check that a solution really
     * fits in the bag.
     */
    public int totalWeight(Instance instance) {
        if (taken.length != instance.size()) {
            throw new IllegalArgumentException(
                    "solution covers " + taken.length + " items but the instance has "
                            + instance.size());
        }
        int total = 0;
        for (int i = 0; i < taken.length; i++) {
            if (taken[i]) {
                total += instance.weight(i);
            }
        }
        return total;
    }

    /** Total value of the chosen items, recomputed from the instance. */
    public int recomputeValue(Instance instance) {
        int total = 0;
        for (int i = 0; i < taken.length; i++) {
            if (taken[i]) {
                total += instance.value(i);
            }
        }
        return total;
    }

    /** How many items were taken. */
    public int itemCount() {
        int n = 0;
        for (boolean t : taken) {
            if (t) {
                n++;
            }
        }
        return n;
    }
}
