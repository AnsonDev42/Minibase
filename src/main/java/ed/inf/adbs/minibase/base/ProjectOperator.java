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
        this.projectedTuples = new HashSet<>();
        this.relationalAtom = (RelationalAtom) relationalAtom;
        this.varIndexMap = createProjectionMap();
    }


    /**
     * Create a map of projected variables to their corresponding index in the original tuples
     *
     * @return a map of projected variables to their corresponding index in the original tuples
     */
    public Map<Term, Integer> createProjectionMap() {
        //1. build a map of var to index in the table e.g. R(x,y,z)  x -> 0, y -> 1, z -> 2
        HashMap relationMap = new HashMap<>();
        List<Term> terms = this.relationalAtom.getTerms();
        for (int i = 0; i < terms.size(); i++) {
            if (terms.get(i) instanceof Variable) {
                relationMap.put(terms.get(i), i);
            }
        }

        //2. build a map of var to index in the query e.g. Q(y,x)  y -> 1, x -> 0
        HashMap<Term, Integer> termToIndexMap = new HashMap<>();
        for (int i = 0; i < projectedVars.size(); i++) {
            if (projectedVars.get(i) != null) {
                Integer tmp = (Integer) relationMap.get(projectedVars.get(i));
                if (tmp == null) {
                    throw new RuntimeException("Error: field value is null, probably bcz an unseen var in Q");
                }
                termToIndexMap.put(projectedVars.get(i), tmp);
            }
        }
        return termToIndexMap;
    }

    @Override
    public Tuple getNextTuple() throws IOException {
        Tuple tuple;
        Tuple projectedTuple;

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
                } else {// skip the tuple since it contains an unseen var
                    tupleSkipFlag = true;
                }
            }
            if (tupleSkipFlag) {
                continue; // skip the whole tuple
            }
            // Check if the projected tuple has already been seen before
            projectedTuple = new Tuple(fields);
            if (!projectedTuples.contains(projectedTuple)) {
                //TODO: check if this is correct since pigeonhole principle
                projectedTuples.add(projectedTuple);
                return projectedTuple;
            } else {
                System.out.println("Duplicate tuple: " + projectedTuple);
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