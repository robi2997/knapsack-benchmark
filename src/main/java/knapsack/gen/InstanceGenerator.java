package knapsack.gen;

import knapsack.Instance;

import java.util.Random;

/**
 * Builds instances of each {@link InstanceType}.
 *
 * <p>Generation is seeded, and the seed is written out with the data, so a
 * reported result can be reproduced exactly rather than approximately.
 *
 * <p>Capacity is set to half the total weight of the items. That is the
 * standard choice and it is the awkward one: a bag far too small or nearly
 * large enough for everything leaves little to decide.
 */
public final class InstanceGenerator {

    /** Largest weight drawn. The literature's usual value. */
    public static final int RANGE = 1000;

    private final Random random;

    public InstanceGenerator(long seed) {
        this.random = new Random(seed);
    }

    public Instance generate(InstanceType type, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive, got " + n);
        }

        int[] weights = new int[n];
        int[] values = new int[n];

        for (int i = 0; i < n; i++) {
            switch (type) {
                case UNCORRELATED -> {
                    weights[i] = uniform(1, RANGE);
                    values[i] = uniform(1, RANGE);
                }
                case WEAKLY_CORRELATED -> {
                    weights[i] = uniform(1, RANGE);
                    int spread = RANGE / 10;
                    values[i] = Math.max(1, uniform(weights[i] - spread, weights[i] + spread));
                }
                case STRONGLY_CORRELATED -> {
                    weights[i] = uniform(1, RANGE);
                    values[i] = weights[i] + RANGE / 10;
                }
                case INVERSE_STRONGLY_CORRELATED -> {
                    values[i] = uniform(1, RANGE);
                    weights[i] = values[i] + RANGE / 10;
                }
                case ALMOST_STRONGLY_CORRELATED -> {
                    weights[i] = uniform(1, RANGE);
                    int centre = weights[i] + RANGE / 10;
                    int noise = RANGE / 500;
                    values[i] = Math.max(1, uniform(centre - noise, centre + noise));
                }
                case SUBSET_SUM -> {
                    weights[i] = uniform(1, RANGE);
                    values[i] = weights[i];
                }
            }
        }

        return new Instance(halfTotalWeight(weights), weights, values);
    }

    private int uniform(int lowInclusive, int highInclusive) {
        return lowInclusive + random.nextInt(highInclusive - lowInclusive + 1);
    }

    private static int halfTotalWeight(int[] weights) {
        long total = 0;
        for (int w : weights) {
            total += w;
        }
        return (int) (total / 2);
    }
}
