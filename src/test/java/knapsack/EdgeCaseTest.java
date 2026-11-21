package knapsack;

import knapsack.solvers.BranchAndBoundSolver;
import knapsack.solvers.BruteForceSolver;
import knapsack.solvers.DpSolver;
import knapsack.solvers.GreedySolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cases random instances will not produce: empty bags, items that cannot
 * fit, and weightless items. Every solver is run through all of them, so a
 * solver added later cannot quietly skip them.
 */
class EdgeCaseTest {

    static Stream<Solver> solvers() {
        return Stream.of(
                new BruteForceSolver(),
                new BranchAndBoundSolver(),
                new DpSolver(),
                new GreedySolver());
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("a bag with no capacity holds nothing")
    void zeroCapacity(Solver solver) {
        Instance instance = new Instance(0, new int[]{3, 5}, new int[]{10, 20});
        Solution solution = solver.solve(instance);

        assertEquals(0, solution.value());
        assertEquals(0, solution.itemCount());
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("an instance with no items yields nothing")
    void noItems(Solver solver) {
        Solution solution = solver.solve(new Instance(100, new int[0], new int[0]));

        assertEquals(0, solution.value());
        assertEquals(0, solution.itemCount());
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("an item too heavy to fit is left behind")
    void singleItemTooHeavy(Solver solver) {
        Instance instance = new Instance(5, new int[]{10}, new int[]{100});
        Solution solution = solver.solve(instance);

        assertEquals(0, solution.value());
        assertFalse(solution.taken()[0]);
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("an item that exactly fills the bag is taken")
    void singleItemExactFit(Solver solver) {
        Instance instance = new Instance(5, new int[]{5}, new int[]{7});
        Solution solution = solver.solve(instance);

        assertEquals(7, solution.value());
        assertTrue(solution.taken()[0]);
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("a weightless item is always worth taking")
    void zeroWeightItemIsFree(Solver solver) {
        // The bag is full after the second item, but the first costs nothing.
        Instance instance = new Instance(4, new int[]{0, 4}, new int[]{9, 1});
        Solution solution = solver.solve(instance);

        assertEquals(10, solution.value(),
                solver.name() + " dropped the free item");
        assertTrue(solution.taken()[0]);
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("greedy's blind spot: the ratio favours the wrong item")
    void ratioOrderIsNotOptimal(Solver solver) {
        // Ratios: 6/5 = 1.2, then 10/9 and 10/9. Greedy takes the first and
        // then has 4 left over, reaching 6. The optimum leaves it and takes
        // one of the others, reaching 10.
        Instance instance = new Instance(9, new int[]{5, 9, 9}, new int[]{6, 10, 10});
        Solution solution = solver.solve(instance);

        int expected = solver.isExact() ? 10 : 6;
        assertEquals(expected, solution.value());
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("a negative capacity is rejected before any solving happens")
    void negativeCapacityRejected(Solver solver) {
        assertThrows(IllegalArgumentException.class,
                () -> new Instance(-1, new int[]{1}, new int[]{1}));
    }

    @ParameterizedTest
    @MethodSource("solvers")
    @DisplayName("mismatched weights and values are rejected")
    void mismatchedArraysRejected(Solver solver) {
        assertThrows(IllegalArgumentException.class,
                () -> new Instance(10, new int[]{1, 2}, new int[]{1}));
    }
}
