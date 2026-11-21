package knapsack.bench;

import knapsack.Instance;
import knapsack.Solution;
import knapsack.Solver;
import knapsack.io.InstanceIO;
import knapsack.io.LabelledInstance;
import knapsack.solvers.BranchAndBoundSolver;
import knapsack.solvers.BruteForceSolver;
import knapsack.solvers.DpSolver;
import knapsack.solvers.GreedySolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Times every solver on every instance and writes one row per measurement.
 *
 * <h2>Why the timing is not just a call to nanoTime</h2>
 *
 * <p>A JVM does not run a method at full speed the first time. Classes load,
 * the interpreter runs the bytecode, and only after enough iterations does the
 * JIT compile it to machine code. Timing a single first call measures that
 * warm-up far more than it measures the algorithm, and the error is not small:
 * it can make a method look an order of magnitude slower than it is, and the
 * effect lands hardest on whichever solver happens to run first.
 *
 * <p>So each measurement here discards some runs to let the JIT settle, then
 * repeats the timed run and keeps the <em>median</em>, which a stray pause from
 * garbage collection or the operating system cannot drag around the way a mean
 * can. Repeats stop once a time budget is spent, so a slow instance costs a few
 * runs and a fast one costs many.
 *
 * <p>This is a reasonable harness, not a rigorous one: a serious measurement
 * would use JMH, which also guards against the JIT optimising away work whose
 * result is never used, and against the measurement loop itself being compiled
 * differently from real calls.
 *
 * <h2>Output</h2>
 *
 * <p>One row per solver per instance, in long form, which keeps the analysis
 * simple: no column has to be added when a solver is.
 *
 * <pre>
 * java -cp target/classes knapsack.bench.Benchmark data/small.csv results/small.csv
 * </pre>
 */
public final class Benchmark {

    private static final int WARMUP_RUNS = 3;
    private static final int MIN_RUNS = 3;
    private static final int MAX_RUNS = 15;

    /** Stop repeating once this much time has gone into one measurement. */
    private static final long TIME_BUDGET_NANOS = 200_000_000L;   // 200 ms

    /** Beyond this, 2^n brute force runs take longer than the whole benchmark. */
    private static final int MAX_BRUTE_FORCE_N = 24;

    /** The dynamic programming table is n * capacity cells. */
    private static final long MAX_DP_CELLS = 200_000_000L;

    /** Branch and bound gives up past this, rather than stalling the run. */
    private static final long MAX_BB_NODES = 50_000_000L;

    private record SolverSpec(String name, Supplier<Solver> factory) { }

    private static final List<SolverSpec> SOLVERS = List.of(
            new SolverSpec("brute-force", BruteForceSolver::new),
            new SolverSpec("branch-and-bound", () -> new BranchAndBoundSolver(MAX_BB_NODES)),
            new SolverSpec("dp", DpSolver::new),
            new SolverSpec("greedy", GreedySolver::new));

    public static void main(String[] args) throws IOException {
        Path input = Path.of(args.length > 0 ? args[0] : "data/small.csv");
        Path output = Path.of(args.length > 1 ? args[1] : "results/small.csv");

        List<LabelledInstance> instances = InstanceIO.read(input);
        System.out.println("read " + instances.size() + " instances from " + input);

        Files.createDirectories(output.toAbsolutePath().getParent());

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output))) {
            out.println("id,type,n,capacity,solver,status,time_ms,value,nodes");

            for (LabelledInstance labelled : instances) {
                for (SolverSpec spec : SOLVERS) {
                    Result result = measure(spec, labelled.instance());

                    out.printf("%d,%s,%d,%d,%s,%s,%s,%s,%s%n",
                            labelled.id(),
                            labelled.type(),
                            labelled.instance().size(),
                            labelled.instance().capacity(),
                            spec.name(),
                            result.status(),
                            format(result.millis()),
                            result.value() < 0 ? "" : String.valueOf(result.value()),
                            result.nodes() < 0 ? "" : String.valueOf(result.nodes()));
                }

                System.out.printf("instance %d (%s, n=%d) done%n",
                        labelled.id(), labelled.type(), labelled.instance().size());
            }
        }

        System.out.println("wrote " + output);
    }

    private record Result(String status, double millis, int value, long nodes) {

        static Result skipped() {
            return new Result("skipped", Double.NaN, -1, -1);
        }

        static Result aborted() {
            return new Result("node-limit", Double.NaN, -1, -1);
        }
    }

    private static Result measure(SolverSpec spec, Instance instance) {
        if (!applicable(spec.name(), instance)) {
            return Result.skipped();
        }

        try {
            // Warm-up: let class loading and the JIT happen before the clock starts.
            for (int i = 0; i < WARMUP_RUNS; i++) {
                consume(spec.factory().get().solve(instance));
            }

            List<Long> timings = new ArrayList<>();
            long spent = 0;
            Solution last = null;
            Solver solver = null;

            while (timings.size() < MAX_RUNS
                    && (timings.size() < MIN_RUNS || spent < TIME_BUDGET_NANOS)) {
                solver = spec.factory().get();

                long start = System.nanoTime();
                last = solver.solve(instance);
                long elapsed = System.nanoTime() - start;

                consume(last);
                timings.add(elapsed);
                spent += elapsed;
            }

            Collections.sort(timings);
            long medianNanos = timings.get(timings.size() / 2);

            return new Result("ok", medianNanos / 1e6, last.value(), solver.nodesExplored());

        } catch (BranchAndBoundSolver.NodeLimitExceeded e) {
            return Result.aborted();
        }
    }

    private static boolean applicable(String solverName, Instance instance) {
        return switch (solverName) {
            case "brute-force" -> instance.size() <= MAX_BRUTE_FORCE_N;
            case "dp" -> (long) instance.size() * (instance.capacity() + 1) <= MAX_DP_CELLS;
            default -> true;
        };
    }

    /**
     * Touches the result so the JIT cannot decide the whole call is dead code
     * and delete it. A blackhole in JMH terms, done by hand.
     */
    @SuppressWarnings("unused")
    private static void consume(Solution solution) {
        if (solution.value() == Integer.MIN_VALUE) {
            throw new AssertionError("unreachable, and the JIT cannot prove it");
        }
    }

    private static String format(double millis) {
        return Double.isNaN(millis) ? "" : String.format("%.5f", millis);
    }
}
