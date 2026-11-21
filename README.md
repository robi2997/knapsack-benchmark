# knapsack-benchmark

An empirical comparison of exact and approximate algorithms for the **0/1 knapsack problem**, run on
generated instance classes from the knapsack literature.

> Work in progress.

## The question

Given `n` items, each with a weight and a value, and a bag of limited capacity: which items do you take
to maximise total value? The problem is NP-hard, so the practical question is not *can we solve it* but
**which algorithm to use when** — and how much accuracy a fast approximation costs you.

This project implements four algorithms, runs them on instance classes of increasing difficulty, and
measures both running time and the error of the approximate one.

## Algorithms

| Algorithm | Exact | Complexity | Notes |
|---|---|---|---|
| Brute force | yes | O(2^n) | baseline, include/exclude recursion |
| Branch and bound | yes | O(2^n) worst case | prunes with a fractional upper bound |
| Dynamic programming | yes | O(n·W) | 1-D array, O(W) memory |
| Greedy | no | O(n log n) | sorts by value/weight ratio |

## Instance classes

Instances are generated following the classes used in Pisinger's knapsack benchmarks: uncorrelated,
weakly correlated, strongly correlated, inverse strongly correlated, almost strongly correlated, and
subset sum. Generation uses a fixed seed, recorded with the data, so every run is reproducible.

<!-- TODO: table with the exact generation rule per class -->

## Results

<!-- TODO: charts and the main findings -->

## Running it

<!-- TODO: build, generate, benchmark, plot -->

## Repository layout

```
data/        generated instances
results/     benchmark output and charts
scripts/     plotting
src/main/java/knapsack/
    solvers/ the four algorithms
    gen/     instance generation
    io/      CSV reading and writing
    bench/   timing harness
src/test/java/knapsack/
```

## References

Pisinger, D. *Where are the hard knapsack problems?* Computers & Operations Research, 2005.

## License

MIT
