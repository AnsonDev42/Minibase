package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SelectOperator extends Operator {
    private static RelationalAtom relationalAtom;
    private static ScanOperator childScanOperator;
    private List<ComparisonAtom> condition;

    private static HashMap<String, Integer> termToIndexMap;


    public SelectOperator(RelationalAtom relationalAtom, ScanOperator operator, List<ComparisonAtom> condition) {
        SelectOperator.relationalAtom = relationalAtom;
        childScanOperator = operator;
        condition = condition;
        termToIndexMap = createTermToIndexMap(relationalAtom);

    }

    @Override
    public Tuple getNextTuple() {
        Tuple tuple = childScanOperator.getNextTuple();
        while (tuple != null) {
            if (passCondition(tuple, condition, relationalAtom)) {
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
    public static Boolean passCondition(Tuple tuple, List<ComparisonAtom> condition, RelationalAtom relationalAtom) {
//  TODO: implement this method by finding vars and testing them against the tuple
        HashMap<String, Integer> map = createTermToIndexMap(relationalAtom);
        for (ComparisonAtom comparisonAtom : condition) {
            Term left = comparisonAtom.getTerm1();
            Term right = comparisonAtom.getTerm2();
            if (left instanceof Variable) {
                int left_idx = map.get(left.toString());
                left = (Term) tuple.getField(left_idx);
            }
            if (right instanceof Variable) {
                int right_idx = map.get(right.toString());
                right = (Term) tuple.getField(right_idx);
            }
            if (!comparisonAtom.getOp().compare(left, right)) {
                return false;
            }
        }
//        return comparisonAtom.evaluate(tuple);
        return true;
    }

    /**
     * Create variable to index map e.g. R(x,y,z) -> x:0, y:1, z:2,
     * So that we can get the index of a variable in a tuple
     *
     * @param relationalAtom the relational atom this operator
     * @return a map from variable name to index
     */

    public static HashMap createTermToIndexMap(RelationalAtom relationalAtom) {
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < relationalAtom.getTerms().size(); i++) {
            map.put(relationalAtom.getTerms().get(i).toString(), i);
        }
        return map;
    }

    @Override
    public void reset() throws IOException {
        childScanOperator.reset();
    }

}
