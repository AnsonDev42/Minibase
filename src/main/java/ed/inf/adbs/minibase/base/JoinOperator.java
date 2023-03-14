package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static ed.inf.adbs.minibase.base.SelectOperator.createTermToIndexMap;
import static ed.inf.adbs.minibase.base.SelectOperator.passConditions;


public class JoinOperator extends Operator {

    private final Operator leftChild;
    private final Operator rightChild;
    private final List<Atom> variables;
    private final List<ComparisonAtom> joinConditions;
    private final HashMap<String, Integer> leftTermToIndexMap;
    private final HashMap<String, Integer> rightTermToIndexMap;

    public JoinOperator(Operator leftChild, Operator rightChild, List variables, List joinConditions) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.variables = variables;
        this.joinConditions = joinConditions;
        this.leftTermToIndexMap = createTermToIndexMap((RelationalAtom) variables.get(0));
        this.rightTermToIndexMap = createTermToIndexMap((RelationalAtom) variables.get(1));
    }

    @Override
    public Tuple getNextTuple() throws IOException {
        Tuple leftTuple;
        Tuple rightTuple;
        // outer loop iterates over left child
        while ((leftTuple = leftChild.getNextTuple()) != null) {
            rightChild.reset();
            while ((rightTuple = rightChild.getNextTuple()) != null) {
                if (passConditions(leftTuple, rightTuple, joinConditions, leftTermToIndexMap, rightTermToIndexMap)) {
                    return Tuple.join(leftTuple, rightTuple);
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
        if (!passConditions(leftTuple, rightTuple, joinConditions, leftTermToIndexMap, rightTermToIndexMap)) {
            return null;
        }
        return Tuple.join(leftTuple, rightTuple);
    }


    @Override
    public void reset() throws IOException {
        leftChild.reset();
        rightChild.reset();
    }
}