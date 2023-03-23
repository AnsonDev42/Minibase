package ed.inf.adbs.minibase.base;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Operator {
    private String outputPath;

    public Tuple getNextTuple() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Dump the tuples in the relation to the console
     */
    public void dump() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            Tuple tuple;
            while ((tuple = getNextTuple()) != null) {
//                System.out.println(tuple);
//                remove brackets at the beginning and end of the tuple
                writer.write(tuple.toString().trim().substring(1, tuple.toString().trim().length() - 1));
                writer.newLine();
            }
        }
    }

    /**
     * set dump path for dumping to a file
     *
     * @param outputPath
     */
    public void setDumpPath(String outputPath) {
        this.outputPath = outputPath;
    }


    protected String getRelationName() {
        return null;
    }

    protected HashMap<String, Integer> getReturnedTermToIndexMap() {
        return null;
    }
}
