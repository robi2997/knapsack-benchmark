package knapsack.io;

import knapsack.Instance;
import knapsack.gen.InstanceType;

/**
 * An instance together with the bookkeeping the benchmark reports on: which
 * family it came from, and the seed it was generated with.
 */
public record LabelledInstance(int id, InstanceType type, long seed, Instance instance) {
}
