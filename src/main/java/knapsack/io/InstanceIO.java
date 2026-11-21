package knapsack.io;

import knapsack.Instance;
import knapsack.gen.InstanceType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes instance files.
 *
 * <p>One instance per line. Items are packed into a single field as
 * {@code weight:value} pairs separated by {@code |}, which keeps a whole
 * instance on one line and the file readable:
 *
 * <pre>
 * id,type,n,capacity,seed,items
 * 0,UNCORRELATED,3,900,42,131:764|249:885|971:526
 * </pre>
 */
public final class InstanceIO {

    private static final String HEADER = "id,type,n,capacity,seed,items";

    private InstanceIO() {
    }

    public static void write(Path file, List<LabelledInstance> instances) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.println(HEADER);

            for (LabelledInstance labelled : instances) {
                Instance instance = labelled.instance();

                StringBuilder items = new StringBuilder();
                for (int i = 0; i < instance.size(); i++) {
                    if (i > 0) {
                        items.append('|');
                    }
                    items.append(instance.weight(i)).append(':').append(instance.value(i));
                }

                out.printf("%d,%s,%d,%d,%d,%s%n",
                        labelled.id(),
                        labelled.type(),
                        instance.size(),
                        instance.capacity(),
                        labelled.seed(),
                        items);
            }
        }
    }

    public static List<LabelledInstance> read(Path file) throws IOException {
        List<LabelledInstance> instances = new ArrayList<>();

        try (BufferedReader in = Files.newBufferedReader(file)) {
            String line = in.readLine();   // header
            int lineNumber = 1;

            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                // limit 6 so the items field keeps any colons and pipes intact
                String[] fields = line.split(",", 6);
                if (fields.length < 6) {
                    throw new IOException(file + " line " + lineNumber
                            + ": expected 6 fields, got " + fields.length);
                }

                int id = Integer.parseInt(fields[0].trim());
                InstanceType type = InstanceType.valueOf(fields[1].trim());
                int n = Integer.parseInt(fields[2].trim());
                int capacity = Integer.parseInt(fields[3].trim());
                long seed = Long.parseLong(fields[4].trim());

                String[] pairs = fields[5].trim().split("\\|");
                if (pairs.length != n) {
                    throw new IOException(file + " line " + lineNumber
                            + ": header says " + n + " items but " + pairs.length + " are listed");
                }

                int[] weights = new int[n];
                int[] values = new int[n];
                for (int i = 0; i < n; i++) {
                    String[] pair = pairs[i].split(":");
                    weights[i] = Integer.parseInt(pair[0]);
                    values[i] = Integer.parseInt(pair[1]);
                }

                instances.add(new LabelledInstance(id, type, seed,
                        new Instance(capacity, weights, values)));
            }
        }

        return instances;
    }
}
