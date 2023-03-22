package ed.inf.adbs.minibase.base;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Operator {


    public Tuple getNextTuple() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void reset() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Dump the tuples in the relation to the console
     */
    public void dump(String outputFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            Tuple tuple;
            while ((tuple = getNextTuple()) != null) {
//                System.out.println(tuple);
//                remove brackets at the beginning and end of the tuple
                writer.write(tuple.toString().trim().substring(1, tuple.toString().trim().length() - 1));
                writer.newLine();
            }
        }
    }


    protected String getRelationName() {
        return null;
    }

    protected HashMap<String, Integer> getReturnedTermToIndexMap() {
        return null;
    }
}
