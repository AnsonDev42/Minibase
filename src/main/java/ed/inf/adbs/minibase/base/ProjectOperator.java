package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;

public class ProjectOperator extends Operator {

    private static Operator childOperator;
    private final List<Variable> projectedVars;
    private static Map<Term, Integer> varIndexMap;
    private final Set<Tuple> projectedTuples;
    private final HashMap<String, Integer> jointTupleVarToIdx;
    private final boolean distinct;
//    private final RelationalAtom relationalAtom;

    public ProjectOperator(Operator childOperator, List<Variable> projectedVars, HashMap<String, Integer> jointTupleVarToIdx, boolean distinct) {
        ProjectOperator.childOperator = childOperator;
        this.projectedVars = projectedVars;
        this.projectedTuples = new HashSet<>();
        this.jointTupleVarToIdx = jointTupleVarToIdx;
        varIndexMap = createProjectionMap();
        this.distinct = distinct;
    }

    public Map<Term, Integer> getVarIndexMap() {
        return varIndexMap;
    }

    /*
    return the projected vars for creating the dictionary in SumOperator later
     */
    public List<Variable> getProjectedVars() {
        return projectedVars;
    }


    public static Operator getChildOperator() {
        return childOperator;
    }

    /**
     * Create a map of projected variables to their corresponding index in the original tuples
     *
     * @return a map of projected variables to their corresponding index in the original tuples
     */
    public Map<Term, Integer> createProjectionMap() {
//        THIS has been done in jointTupleVarToIdx, so no need to do it again
        //1. build a map of var to index in the table e.g. R(x,y,z)  x -> 0, y -> 1, z -> 2
//        HashMap jointTupleVarToIdx = new HashMap<>();
//        List<Term> terms = this.relationalAtom.getTerms();
//        for (int i = 0; i < terms.size(); i++) {
//            if (terms.get(i) instanceof Variable) {
//                jointTupleVarToIdx.put(terms.get(i), i);
//            }
//        }
        System.out.println("jointTupleVarToIdx: " + jointTupleVarToIdx);
        //2. build a map of var to index in the query e.g. Q(y,x)  y -> 1, x -> 0
        HashMap<Term, Integer> termToIndexMap = new HashMap<>();
        for (Term projectedVar : projectedVars) {

            if (jointTupleVarToIdx.get(projectedVar.toString()) == null) {
                throw new RuntimeException("Error: jointTupleVarToIdx does not contain the projectedVar");
            }
            Integer index = jointTupleVarToIdx.get(projectedVar.toString());
            if (index == null) {
                throw new RuntimeException("Error: field value is null, probably because an unseen variable is in the query");
            }
            termToIndexMap.put(projectedVar, index);
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
            if (distinct) {
                if (!projectedTuples.contains(projectedTuple)) {
                    projectedTuples.add(projectedTuple);
                    return projectedTuple;
                } else {
                    System.out.println("find Duplicate tuple, not output to file: " + projectedTuple);
                }
            } else {
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