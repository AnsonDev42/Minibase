package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SelectOperator extends Operator {
    private final RelationalAtom relationalAtom;
    private final ScanOperator childScanOperator;
    private final List<ComparisonAtom> conditions;

    private final HashMap<String, Integer> termToIndexMap;


    public SelectOperator(RelationalAtom relationalAtom, List<ComparisonAtom> conditions, HashSet requiredColumns) throws IOException {
        this.relationalAtom = relationalAtom;
        this.childScanOperator = new ScanOperator(relationalAtom, requiredColumns);
        this.conditions = conditions;
        this.termToIndexMap = this.childScanOperator.getReturnedTermToIndexMap();
    }

    /**
     * returing the pushdown-ed mapping of the terms to the index of the tuple
     *
     * @return the pushdown-ed mapping of the terms to the index of the tuple
     */
    public HashMap<String, Integer> getReturnedTermToIndexMap() {
        return termToIndexMap;
    }


    /**
     * Get the relation name
     *
     * @return the relation name
     */
    public String getRelationName() {
        return this.relationalAtom.getName();
    }


    @Override
    public Tuple getNextTuple() {
        Tuple tuple = childScanOperator.getNextTuple();
        while (tuple != null) {
            if (passConditions(tuple, tuple, conditions, termToIndexMap, termToIndexMap)) {
                return tuple;
            }
            tuple = childScanOperator.getNextTuple();
        }
        return null;
    }

    /**
     * Check if the leftTuple satisfies the conditions
     *
     * @param leftTuple  the leftTuple to be checked
     * @param conditions the conditions(may contain multiple ComparisonAtoms) to be checked e.g. x < y and z! = ’adbs’
     * @return true if the leftTuple satisfies the conditions, false otherwise
     */
    public static Boolean passConditions(Tuple leftTuple, Tuple rightTuple, List<ComparisonAtom> conditions,
                                         HashMap<String, Integer> leftTermToIndexMap, HashMap<String, Integer> rightTermToIndexMap) {
        for (ComparisonAtom comparisonAtom : conditions) {
            Term left = getFieldValue(leftTuple, comparisonAtom.getTerm1(), leftTermToIndexMap);
            Term right = getFieldValue(rightTuple, comparisonAtom.getTerm2(), rightTermToIndexMap);
            if (left == null || right == null) {
//                System.out.println("Error: field value is null");
//                return false; ?true;
                throw new RuntimeException("Error: field value is null, probably bcz an unseen var in conditions");
            }
            if (!comparisonAtom.getOp().compare(left, right)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Create variable to index map e.g. R(x,y,z) -> x:0, y:1, z:2, or R(x,y,4) -> x:0, y:1, (IntCons)4:0
     * So that we can get the index of a variable in a tuple
     * It is guaranteed that the terms are unique, so we can use the term as the key
     *
     * @param relationalAtom the relational atom this operator
     * @return a map from variable name to index
     */
    public static HashMap<String, Integer> createTermToIndexMap(RelationalAtom relationalAtom, HashSet requiredColumns) {
        if (relationalAtom.getTerms().size() != Catalog.getInstance(null).getSchema(relationalAtom.getName()).length) {
            throw new IllegalArgumentException("The number of terms in the relational atom does not match the schema");
        }
        HashMap<String, Integer> map = new HashMap<>();
        int idx = 0;
        for (int i = 0; i < relationalAtom.getTerms().size(); i++) {
            if (requiredColumns.contains(relationalAtom.getTerms().get(i).toString())) {
                map.put(relationalAtom.getTerms().get(i).toString(), idx);
                idx++;
            }
        }
        return map;
    }


    /**
     * Get a term(has actual value) from the tuple, given a term which might be a variable,
     * for the comparison later.
     * e.g. R(x,y,z), x>1. This method returns the actual value of x in the tuple for comparison
     *
     * @param tuple the tuple to be checked
     * @param term  the term might be a variable
     * @return the actual field value of the term in the tuple
     */
    private static Term getFieldValue(Tuple tuple, Term term, HashMap<String, Integer> termToIndexMap) {
        if (tuple == null) {
            throw new RuntimeException("Error: tuple is null");
        }
        if (null != termToIndexMap.get(term.toString())) {
            return (Term) tuple.getField(termToIndexMap.get(term.toString()));
        }
        return term; // if the term is a constant, return it directly
    }

    @Override
    public void reset() throws IOException {
        childScanOperator.reset();
    }
}
