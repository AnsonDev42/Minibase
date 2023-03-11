package ed.inf.adbs.minibase.base;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SelectOperator extends Operator {
    private final RelationalAtom relationalAtom;
    private static ScanOperator childScanOperator;
    private final List<ComparisonAtom> condition;

    private final HashMap<String, Integer> termToIndexMap;


    public SelectOperator(RelationalAtom relationalAtom, List<ComparisonAtom> condition) throws FileNotFoundException {
        this.relationalAtom = relationalAtom;
        childScanOperator = new ScanOperator(relationalAtom.getName());
        this.condition = condition;
        this.termToIndexMap = createTermToIndexMap(relationalAtom);
    }

    @Override
    public Tuple getNextTuple() {
        Tuple tuple = childScanOperator.getNextTuple();
        while (tuple != null) {
            if (passCondition(tuple, condition)) {
                return tuple;
            }
            tuple = childScanOperator.getNextTuple();
        }
        return null;
    }

    /**
     * Check if the tuple satisfies the condition
     *
     * @param tuple     the tuple to be checked
     * @param condition the condition(may contain multiple ComparsionAtoms) to be checked e.g. x < y and z! = ’adbs’
     * @return true if the tuple satisfies the condition, false otherwise
     */
    public Boolean passCondition(Tuple tuple, List<ComparisonAtom> condition) {
        for (ComparisonAtom comparisonAtom : condition) {
            Term left = getFieldValue(tuple, comparisonAtom.getTerm1());
            Term right = getFieldValue(tuple, comparisonAtom.getTerm2());
            if (left == null || right == null) {
                return true;
            }
            if (!comparisonAtom.getOp().compare(left, right)) {
                return false;
            }
        }

        return true;
    }

    public List<ComparisonAtom> addHiddenCondition() {
        List<ComparisonAtom> condition = this.condition;
        for (Term term : relationalAtom.getTerms()) {
            if (term instanceof Constant) {
                condition.add(new ComparisonAtom(term, new Variable("hidden"), ComparisonOperator.EQ));
            }
        }
        return condition;
    }


    /**
     * Create variable to index map e.g. R(x,y,z) -> x:0, y:1, z:2, or R(x,y,4) -> x:0, y:1, (IntCons)4:0
     * So that we can get the index of a variable in a tuple
     *
     * @param relationalAtom the relational atom this operator
     * @return a map from variable name to index
     */
    public static HashMap createTermToIndexMap(RelationalAtom relationalAtom) {
//        System.out.println("Relation :" + relationalAtom.getName() + Catalog.getInstance(null).getSchema(relationalAtom.getName()).length);
        if (relationalAtom.getTerms().size() != Catalog.getInstance(null).getSchema(relationalAtom.getName()).length) {
            throw new IllegalArgumentException("The number of terms in the relational atom does not match the schema");
        }
        HashMap<Term, Integer> map = new HashMap<>();
        for (int i = 0; i < relationalAtom.getTerms().size(); i++) {
            map.put(relationalAtom.getTerms().get(i), i);
        }
        return map;
    }

    public static HashMap createIndexToTerm(RelationalAtom relationalAtom) {
//        System.out.println("Relation :" + relationalAtom.getName() + Catalog.getInstance(null).getSchema(relationalAtom.getName()).length);
        if (relationalAtom.getTerms().size() != Catalog.getInstance(null).getSchema(relationalAtom.getName()).length) {
            throw new IllegalArgumentException("The number of terms in the relational atom does not match the schema");
        }
        HashMap<Integer, Term> map = new HashMap<>();
        for (int i = 0; i < relationalAtom.getTerms().size(); i++) {
            map.put(i, relationalAtom.getTerms().get(i));
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
    private Term getFieldValue(Tuple tuple, Term term) {
        if (term instanceof Variable) {
            if (termToIndexMap.containsKey(term.toString())) {
                return (Term) tuple.getField(termToIndexMap.get(term.toString()));
            }
            return null;
        }

        return term;
    }

    @Override
    public void reset() throws IOException {
        childScanOperator.reset();
    }

}
