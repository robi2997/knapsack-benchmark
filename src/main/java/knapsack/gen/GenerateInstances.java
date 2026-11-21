package knapsack.gen;

import knapsack.io.InstanceIO;
import knapsack.io.LabelledInstance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the two instance files the benchmark runs on.
 *
 * <p>They are split because the algorithms live on different scales. Brute
 * force is only viable at small n, so comparing it against branch and bound
 * needs its own small set; dynamic programming and greedy stay usable far
 * beyond that, and the interesting question there is what greedy costs in
 * accuracy as instances grow.
 *
 * <pre>
 * java -cp target/classes knapsack.gen.GenerateInstances
 * </pre>
 */
public final class GenerateInstances {

    private static final long SEED = 20261121L;

    /** n values for the small set, where every solver runs. */
    private static final int[] SMALL_SIZES = {5, 8, 10, 12, 14, 16, 18, 20, 22, 24};

    /** n values for the large set, where brute force is out of the question. */
    private static final int[] LARGE_SIZES = {50, 100, 200, 400, 800, 1600, 3200, 6400};

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");

        write(dataDir.resolve("small.csv"), SMALL_SIZES);
        write(dataDir.resolve("large.csv"), LARGE_SIZES);
    }

    private static void write(Path file, int[] sizes) throws IOException {
        List<LabelledInstance> instances = new ArrayList<>();
        int id = 0;

        for (InstanceType type : InstanceType.values()) {
            for (int n : sizes) {
                // A seed per instance, derived from the run seed, so each one
                // can be regenerated on its own without replaying the rest.
                long seed = SEED + 1000L * type.ordinal() + n;
                instances.add(new LabelledInstance(
                        id++, type, seed, new InstanceGenerator(seed).generate(type, n)));
            }
        }

        InstanceIO.write(file, instances);
        System.out.println("wrote " + instances.size() + " instances to " + file);
    }
}
