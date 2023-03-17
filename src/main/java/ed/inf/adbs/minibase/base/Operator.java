package ed.inf.adbs.minibase.base;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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
            Tuple tuple = getNextTuple();
            while (tuple != null) {
                System.out.println(tuple);
//                remove brackets at the beginning and end of the tuple

                writer.write(tuple.toString().trim().substring(1, tuple.toString().trim().length() - 1));
                writer.newLine();
                tuple = getNextTuple();
            }
        }
    }


    protected String getRelationName() {
        return null;
    }
}
