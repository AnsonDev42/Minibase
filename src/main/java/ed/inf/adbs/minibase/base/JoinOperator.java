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
    private Tuple currentLeftTuple = null;


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
        System.out.println("HERE in the end");
        while (true) {
            // If the current left tuple is null, move to the next left tuple
            if (currentLeftTuple == null) {
                currentLeftTuple = leftChild.getNextTuple();

                // If there are no more left tuples, reset and return null
                if (currentLeftTuple == null) {
                    reset();
                    return null;
                }
                rightChild.reset(); // reset right child every time we move to the next left tuple
            }

            while ((rightTuple = rightChild.getNextTuple()) != null) {
                if (passConditions(currentLeftTuple, rightTuple, joinConditions, leftTermToIndexMap, rightTermToIndexMap)) {
                    return Tuple.join(currentLeftTuple, rightTuple);
                }
            }

            // If there are no more right tuples, set the current left tuple to null
            // This will cause the outer loop to move to the next left tuple
            currentLeftTuple = null;
        }
    }


    @Override
    public void reset() throws IOException {
        leftChild.reset();
        rightChild.reset();
    }
}