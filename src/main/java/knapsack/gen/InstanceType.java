package knapsack.gen;

/**
 * The instance families used in the knapsack literature to probe where the
 * problem gets hard. They differ only in how an item's value relates to its
 * weight, and that relation is exactly what decides whether sorting by
 * value-per-weight carries any information.
 *
 * <p>R below is the generator's range parameter, the largest weight drawn.
 */
public enum InstanceType {

    /** Weight and value drawn independently. Ratios vary widely. */
    UNCORRELATED,

    /** Value near the weight, within a tenth of R either way. */
    WEAKLY_CORRELATED,

    /** Value is exactly weight + R/10, so every ratio is nearly the same. */
    STRONGLY_CORRELATED,

    /** Strongly correlated with the roles swapped: weight is value + R/10. */
    INVERSE_STRONGLY_CORRELATED,

    /** Strongly correlated, with a small amount of noise added back in. */
    ALMOST_STRONGLY_CORRELATED,

    /** Value equals weight, so the problem reduces to hitting a target sum. */
    SUBSET_SUM
}
