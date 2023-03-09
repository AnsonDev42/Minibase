package ed.inf.adbs.minibase.base;

import java.util.HashMap;

public class SelectOperator extends Operator {
    private static RelationalAtom relationalAtom;
    private static ScanOperator childScanOperator;
    private static ComparisonAtom comparisonAtom;


    public SelectOperator(RelationalAtom relationalAtom, ScanOperator operator, ComparisonAtom comparisonAtom) {
        SelectOperator.relationalAtom = relationalAtom;
        childScanOperator = operator;
        SelectOperator.comparisonAtom = comparisonAtom;

    }

    @Override
    public Tuple getNextTuple() {
        Tuple tuple = childScanOperator.getNextTuple();
        while (tuple != null) {
            if (passCondition(tuple, comparisonAtom, relationalAtom)) {
                return tuple;
            }
            tuple = childScanOperator.getNextTuple();
        }
        return null;
    }

    /**
     * Check if the tuple satisfies the condition
     *
     * @param tuple          the tuple to be checked
     * @param comparisonAtom the comparison atom
     * @return true if the tuple satisfies the condition, false otherwise
     */
    public static Boolean passCondition(Tuple tuple, ComparisonAtom comparisonAtom, RelationalAtom relationalAtom) {
//  TODO: implement this method by finding vars and testing them against the tuple

//        return comparisonAtom.evaluate(tuple);
        return true;
    }

    public static HashMap TupleToVarMap(Tuple tuple) {
        HashMap<String, Object> map = new HashMap<String, Object>();
//        TODO: create this
//        for (int i = 0; i < tuple.getFields().length; i++) {
//            map.put("X" + i, tuple.getField(i));
//        }
        return map;
    }

    @Override
    public void reset() {
        childScanOperator.reset();
    }

}
