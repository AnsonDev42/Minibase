package ed.inf.adbs.minibase.base;

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
    public void dump() throws IOException {
        Tuple tuple = getNextTuple();
        while (tuple != null) {
            System.out.println(tuple);
            tuple = getNextTuple();
        }
    }
}
