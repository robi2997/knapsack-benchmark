package knapsack;

import knapsack.gen.InstanceGenerator;
import knapsack.gen.InstanceType;
import knapsack.solvers.BranchAndBoundSolver;
import knapsack.solvers.BruteForceSolver;
import knapsack.solvers.DpSolver;
import knapsack.solvers.GreedySolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The core correctness tests.
 *
 * <p>There is no table of expected answers here, because writing one by hand
 * would only test the answers someone thought to write down. Instead the
 * solvers are checked against each other on many random instances: three
 * algorithms claim to be exact, so on every instance all three must agree.
 * Brute force is the one to trust if they ever disagree, since it is simple
 * enough to be obviously right, and that is exactly what makes it a good
 * reference for the two that are not.
 */
class SolverAgreementTest {

    private static final int INSTANCES_PER_TYPE = 60;
    private static final int MAX_ITEMS = 14;

    private final List<Solver> exactSolvers =
            List.of(new BruteForceSolver(), new BranchAndBoundSolver(), new DpSolver());

    @ParameterizedTest
    @EnumSource(InstanceType.class)
    @DisplayName("all exact solvers return the same optimum")
    void exactSolversAgree(InstanceType type) {
        Random random = new Random(type.ordinal() * 7919L);

        for (int run = 0; run < INSTANCES_PER_TYPE; run++) {
            int n = 1 + random.nextInt(MAX_ITEMS);
            Instance instance = new InstanceGenerator(random.nextLong()).generate(type, n);

            int reference = new BruteForceSolver().solve(instance).value();

            for (Solver solver : exactSolvers) {
                Solution solution = solver.solve(instance);

                assertEquals(reference, solution.value(),
                        () -> solver.name() + " disagrees on " + describe(instance));

                assertEquals(solution.value(), solution.recomputeValue(instance),
                        () -> solver.name() + " reports a value its own item set does not add up to, on "
                                + describe(instance));

                assertTrue(solution.totalWeight(instance) <= instance.capacity(),
                        () -> solver.name() + " returned a solution that does not fit, on "
                                + describe(instance));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(InstanceType.class)
    @DisplayName("greedy is feasible and never beats the optimum")
    void greedyStaysBelowOptimum(InstanceType type) {
        Random random = new Random(type.ordinal() * 104729L);
        Solver greedy = new GreedySolver();

        for (int run = 0; run < INSTANCES_PER_TYPE; run++) {
            int n = 1 + random.nextInt(MAX_ITEMS);
            Instance instance = new InstanceGenerator(random.nextLong()).generate(type, n);

            int optimum = new DpSolver().solve(instance).value();
            Solution solution = greedy.solve(instance);

            assertTrue(solution.value() <= optimum,
                    () -> "greedy claims " + solution.value() + " but the optimum is " + optimum
                            + ", on " + describe(instance));

            assertTrue(solution.totalWeight(instance) <= instance.capacity(),
                    () -> "greedy returned a solution that does not fit, on " + describe(instance));

            assertEquals(solution.value(), solution.recomputeValue(instance));
        }
    }

    @Test
    @DisplayName("pruning never changes the answer, only the work done")
    void branchAndBoundExploresFewerNodesThanBruteForce() {
        Random random = new Random(42);
        int comparisons = 0;

        for (int run = 0; run < 40; run++) {
            Instance instance = new InstanceGenerator(random.nextLong())
                    .generate(InstanceType.UNCORRELATED, 14);

            BruteForceSolver brute = new BruteForceSolver();
            BranchAndBoundSolver bb = new BranchAndBoundSolver();

            assertEquals(brute.solve(instance).value(), bb.solve(instance).value());

            assertTrue(bb.nodesExplored() <= brute.nodesExplored(),
                    "pruning should never add work: brute force visited "
                            + brute.nodesExplored() + ", branch and bound " + bb.nodesExplored());
            comparisons++;
        }

        assertEquals(40, comparisons);
    }

    private static String describe(Instance instance) {
        StringBuilder text = new StringBuilder("instance(capacity=")
                .append(instance.capacity()).append(", items=[");
        for (int i = 0; i < instance.size(); i++) {
            if (i > 0) {
                text.append(' ');
            }
            text.append(instance.weight(i)).append(':').append(instance.value(i));
        }
        return text.append("])").toString();
    }
}
