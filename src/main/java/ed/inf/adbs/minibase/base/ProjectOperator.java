package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;

public class ProjectOperator extends Operator {

    private static Operator childOperator;
    private final List<Variable> projectedVars;
    private final Map<Term, Integer> varIndexMap;
    private final Set<Tuple> projectedTuples;

    public ProjectOperator(Operator childOperator, List<Variable> projectedVars) {
        ProjectOperator.childOperator = childOperator;
        this.projectedVars = projectedVars;
        this.varIndexMap = new HashMap<>();
        this.projectedTuples = new HashSet<>();

        // Map each projected variable to its corresponding index in the original tuples
        int i = 0;
        for (Term var : this.projectedVars) {
            if (var instanceof Variable) {
                varIndexMap.put(var, i);
            }
            i++;
        }
    }

    @Override
    public Tuple getNextTuple() {
        Tuple tuple;
        Tuple projectedTuple = null;

        while ((tuple = childOperator.getNextTuple()) != null) {
//          Create a new tuple containing only the projected variables
            Object[] fields = new Term[projectedVars.size()];
            int i = 0;
            boolean tupleSkipFlag = false;
            for (Term var : projectedVars) {
//              requirement pdf assume projection cannot contain constants 
                Integer index = varIndexMap.get(var);
                if (index != null) {
                    fields[i] = tuple.getField(index);
                    i++;
                } else {
//                     skip the tuple since it contains an unseen var
                    tupleSkipFlag = true;
                }
            }

            if (tupleSkipFlag) {
                continue; // skip the whole tuple
            }

            // Check if the projected tuple has already been seen before
            projectedTuple = new Tuple(fields);
            if (!projectedTuples.contains(projectedTuple)) {
                projectedTuples.add(projectedTuple);
                return projectedTuple;
            }
        }
        return null;
    }

    @Override
    public void reset() throws IOException {
        childOperator.reset();
        projectedTuples.clear();
    }
}
//
//Here’s how the ProjectOperator works:
//
//        • When getNextTuple() is called, it retrieves tuples one-by-one from its child operator, either a ScanOperator or a SelectOperator.
//        • It creates a new tuple containing only the desired fields from the original tuple, based on the list of projected variables.
//        • If the new tuple has already been seen before, it tries the next tuple from the child operator, until it reaches a new one.
//        • If the new tuple is new, it adds it to the set of projected tuples and returns it as the next tuple.
//
//        You will need to make sure that this operator gets correctly instantiated and used in your code, depending on the query plan for a given query. It should be added as the parent of the ScanOperator or SelectOperator that will produce its input tuples.