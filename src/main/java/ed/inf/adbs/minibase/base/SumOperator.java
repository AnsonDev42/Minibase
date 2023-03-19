package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SumOperator extends Operator {
    private final Operator child;
    private final List<String> groupByVars;
    private final HashMap<List<Object>, Integer> groups;
    private final SumAggregate sumTerm;
    private boolean sumComputed = false;
    private Iterator<Map.Entry<List<Object>, Integer>> groupsIterator;
    private final HashMap<String, Integer> varToIndexMap;

    /**
     * Constructor
     *
     * @param child         child operator
     * @param head          head of the query
     * @param varToIndexMap map of complete tuple from query to index
     */
    public SumOperator(Operator child, Head head, HashMap<String, Integer> varToIndexMap) {
        this.child = child;
        this.groupByVars = head.getVariables().stream()
                .map(Variable::getName)
                .collect(Collectors.toList());
        this.sumTerm = head.getSumAggregate();
        this.varToIndexMap = varToIndexMap;
        this.groups = new HashMap<>();
    }


    /**
     * Compute sum for each group  by iterating over all tuples for the first time
     * Then iterate over each group and return the group with sum when getNextTuple is called
     *
     * @return Tuple with group and sum
     * @throws IOException
     */
    @Override
    public Tuple getNextTuple() throws IOException {
        //STEP1 : compute sum for each group
        if (!sumComputed) {
            computeSum();
            groupsIterator = groups.entrySet().iterator();
            sumComputed = true;
        }
        // STEP2 : return each unique group with sum
        if (groupsIterator.hasNext()) {
            Map.Entry<List<Object>, Integer> entry = groupsIterator.next();
            List<Object> groupKey = entry.getKey();
            Integer sumValue = entry.getValue();
            List<Object> tupleValues = new ArrayList<>(groupKey);
            tupleValues.add(sumValue);
            return new Tuple(tupleValues.toArray());
        } else {
            return null;
        }
    }


    /**
     * getter for groups
     *
     * @return HashMap of groups and sum
     */
    public HashMap getGroups() {
        return groups;
    }

    /**
     * Compute sum for each group
     *
     * @return HashMap of groups and sum
     * @throws IOException
     */
    public void computeSum() throws IOException {
        Tuple tuple = child.getNextTuple();
        while (tuple != null) {
            // create group key for unique groups
            List<Object> groupKey = new ArrayList<>();
            for (String var : groupByVars) {
                groupKey.add(tuple.getField(varToIndexMap.get(var)));
            }
            Integer sumValue = 1;
            Integer vIdx = 0;
            for (Term term : sumTerm.getProductTerms()) {
                if (term instanceof IntegerConstant) {
                    sumValue *= ((IntegerConstant) term).getValue();
                } else if (term instanceof Variable) {
                    vIdx = varToIndexMap.get(((Variable) term).getName());
                    if (vIdx != null) {
                        sumValue *= ((IntegerConstant) tuple.getField(vIdx)).getValue();
                    }
                }
            }
            groups.putIfAbsent(groupKey, 0); // solve null pointer exception
            groups.put(groupKey, groups.get(groupKey) + sumValue);
            tuple = child.getNextTuple();
        }
    }

    @Override
    public void reset() throws IOException {
        child.reset();
        sumComputed = false;
        groupsIterator = null;
    }
}

