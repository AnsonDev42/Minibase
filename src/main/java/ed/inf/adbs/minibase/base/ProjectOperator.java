package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;

public class ProjectOperator extends Operator {

    private static Operator childOperator;
    private final List<Variable> projectedVars;
    private final Map<Term, Integer> varIndexMap;
    private final Set<Tuple> projectedTuples;
    private final RelationalAtom relationalAtom;

    public ProjectOperator(Operator childOperator, List<Variable> projectedVars, Atom relationalAtom) {
        ProjectOperator.childOperator = childOperator;
        this.projectedVars = projectedVars;
        this.varIndexMap = new HashMap<>();
        this.projectedTuples = new HashSet<>();
        this.relationalAtom = (RelationalAtom) relationalAtom;

        // Map each projected variable to its corresponding index in the original tuples

        //1. build a map of var to index in the table
        HashMap relationMap = new HashMap<>();
        List<Term> terms = this.relationalAtom.getTerms();
        for (int i = 0; i < terms.size(); i++) {
            if (terms.get(i) instanceof Variable) {
                relationMap.put(terms.get(i), i);
            }
        }
        //2. build a map of var to index in the projected tuple
        for (int i = 0; i < projectedVars.size(); i++) {
            if (projectedVars.get(i) instanceof Variable) {
                Integer tmp = (Integer) relationMap.get(projectedVars.get(i));
                if (tmp == null) {
                    throw new RuntimeException("Error: field value is null, probably bcz an unseen var in Q");
                }
                varIndexMap.put(projectedVars.get(i), tmp);
            }
        }


//        int i = 0;
//        for (Term var : this.projectedVars) {
//            if (var instanceof Variable) {
////                map all var to indx in the table e.g. Q(y,x) -: R(x,y,z) => y:1, x:0
//                for (int j = 0; j < (this.relationalAtom.getTerms().size(); j++) {
//                    if (this.relationalAtom.getTerms().get(j).toString().equals(var.toString())) {
//                        varIndexMap.put(var, j);
//                    }
//                }
//                varIndexMap.put(var, i);
//            } else {
////                varIndexMap.put(var, i);
//            }
//            i++;
//        }
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