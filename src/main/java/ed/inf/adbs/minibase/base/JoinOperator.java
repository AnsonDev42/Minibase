package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static ed.inf.adbs.minibase.base.SelectOperator.createTermToIndexMap;
import static ed.inf.adbs.minibase.base.SelectOperator.passConditions;


public class JoinOperator extends Operator {

    private final Operator leftChild;
    private final Operator rightChild;
    private final List<ComparisonAtom> joinConditions;
    private final HashMap<String, Integer> leftTermToIndexMap;
    private final HashMap<String, Integer> rightTermToIndexMap;

    public JoinOperator(Operator leftChild, Operator rightChild, HashMap<String, Integer> leftTermToIndexMap,
                        RelationalAtom RightRelAtom, List joinConditions) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.joinConditions = joinConditions;
        this.leftTermToIndexMap = leftTermToIndexMap; // its global map already, no need to create again
        this.rightTermToIndexMap = createTermToIndexMap(RightRelAtom); // TODO: can be optimized using offset from leftTermToIndexMap
    }

    public Operator getLeftChild() {
        return leftChild;
    }

    public Operator getRightChild() {
        return rightChild;
    }

    @Override
    public Tuple getNextTuple() throws IOException {
        Tuple leftTuple;
        Tuple rightTuple;
        // outer loop iterates over left child
        while ((leftTuple = leftChild.getNextTuple()) != null) {
            rightChild.reset();
            while ((rightTuple = rightChild.getNextTuple()) != null) {
                System.out.println("before breaking..." + leftTuple + " " + rightTuple);
                if (passConditions(leftTuple, rightTuple, joinConditions, leftTermToIndexMap, rightTermToIndexMap)) {
                    return Tuple.join(leftTuple, rightTuple);
                }
            }
        }
        return null;
    }


    @Override
    public void reset() throws IOException {
        leftChild.reset();
        rightChild.reset();
    }
}