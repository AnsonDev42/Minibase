package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class JoinOperator extends Operator {

    private final Operator leftChild;
    private final Operator rightChild;
    private final List<Atom> variables;
    private final List<Atom> joinConditions;

    public JoinOperator(Operator leftChild, Operator rightChild, List variables, List joinConditions) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.variables = variables;
        this.joinConditions = joinConditions;
    }

    @Override
    public Tuple getNextTuple() throws IOException {
        Tuple leftTuple;
        Tuple rightTuple;
        // outer loop iterates over left child
        while ((leftTuple = leftChild.getNextTuple()) != null) {
            rightChild.reset();
            while ((rightTuple = rightChild.getNextTuple()) != null) {
//                TODO:implement join function!
                Tuple joinedTuple = null;
                joinedTuple = join(leftTuple, rightTuple);
                if (joinedTuple != null) {
                    return joinedTuple;
                }
            }
        }
        return null;
    }

    /**
     * Join two tuples if they satisfy the join conditions
     *
     * @param leftTuple  the left tuple
     * @param rightTuple the right tuple
     * @return the joined tuple
     */
    public Tuple join(Tuple leftTuple, Tuple rightTuple) {
        //TODO: implement join function
        if (matchesJoinConditions(leftTuple, rightTuple)) {
            Tuple joinedTuple = new Tuple();
            // todo: join them
            return joinedTuple;
        }
        return null;
    }

    private boolean matchesJoinConditions(Tuple leftTuple, Tuple rightTuple) {
        if (joinConditions == null) {
            return true; // cross product
        }
        ComparisonAtom condition;
        for (Atom atom : joinConditions) {
            condition = (ComparisonAtom) atom;
            //TODO : FIX THIS  FOR NOW totally broken
            Tuple tuple = null;
            if (!evaluate(condition, tuple)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluate(ComparisonAtom condition, Tuple tuple) {
        // FOR NOW totally broken
//        TODO: MODIFY this function, for now is from ScanOperator

        Term left = getFieldValue(tuple, condition.getTerm1());
        Term right = getFieldValue(tuple, condition.getTerm2());
        if (left == null || right == null) {
            System.out.println("Error: field value is null");
            return false;
        }
        return condition.getOp().compare(left, right);
    }

    private Term getFieldValue(Tuple tuple, Term term) {
        // FOR NOW totally broken
//        TODO: MODIFY this function, for now is from ScanOperator
        HashMap termToIndexMap = null;
        if (tuple == null) {
            System.out.println("Error: tuple is null");
            throw new RuntimeException("Error: variable is null");
//            return null;
        }
        if (null != termToIndexMap.get(term.toString())) {
            String x = term.toString();
//            System.out.println("termToIndexMap.get(x)" + termToIndexMap.get(x));
//            return (Term) tuple.getField(termToIndexMap.get(x));
        }
//        System.out.println("current tuple" + tuple);
//        System.out.println("term" + term);
        return term; // if the term is a constant, return it directly
    }


    @Override
    public void reset() throws IOException {
        leftChild.reset();
        rightChild.reset();
    }
}