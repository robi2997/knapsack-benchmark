package knapsack;

/**
 * A 0/1 knapsack instance: n items, each with a weight and a value, and a bag
 * that can carry at most {@code capacity} in total weight.
 *
 * <p>Note that records compare arrays by reference, so two instances holding
 * equal contents are not {@code equals}. That is fine here, because instances
 * are never used as map keys or placed in sets.
 */
public record Instance(int capacity, int[] weights, int[] values) {

    public Instance {
        if (weights == null || values == null) {
            throw new IllegalArgumentException("weights and values must not be null");
        }
        if (weights.length != values.length) {
            throw new IllegalArgumentException(
                    "weights and values must have the same length, got "
                            + weights.length + " and " + values.length);
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must not be negative, got " + capacity);
        }
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < 0) {
                throw new IllegalArgumentException("weight " + i + " is negative: " + weights[i]);
            }
        }
    }

    /** The number of items in this instance. */
    public int size() {
        return weights.length;
    }

    public int weight(int i) {
        return weights[i];
    }

    public int value(int i) {
        return values[i];
    }
}
