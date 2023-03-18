package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SumOperator extends Operator {
    private final Operator child;
    private final List<String> groupByVars;
    private final SumAggregate sumTerm;
    private final HashMap<String, Integer> varToIndexMap;
    private boolean groupsComputed = false;
    private Iterator<Map.Entry<List<Object>, Integer>> groupsIterator;
    private final HashMap<List<Object>, Integer> groups;


    public SumOperator(Operator child, Head head, HashMap<String, Integer> varToIndexMap) {
        this.child = child;
        this.groupByVars = head.getVariables().stream()
                .map(Variable::getName)
                .collect(Collectors.toList());
        this.sumTerm = head.getSumAggregate();
        this.varToIndexMap = varToIndexMap;
        this.groups = new HashMap<>();
    }

    @Override
    public Tuple getNextTuple() throws IOException {
        if (!groupsComputed) {
            computeGroups();
            groupsComputed = true;
            groupsIterator = groups.entrySet().iterator();
        }

        if (groupsIterator.hasNext()) {
            Map.Entry<List<Object>, Integer> entry = groupsIterator.next();
            List<Object> groupKey = entry.getKey();
            Integer sumValue = entry.getValue();
            List<Object> tupleValues = new ArrayList<>(groupKey);
            tupleValues.add(sumValue);
            return new Tuple(tupleValues);
        } else {
            return null;
        }
    }


    private void computeGroups() throws IOException {
        Tuple tuple = child.getNextTuple();
        while (tuple != null) {
            // create group key for unique groups
            List<Object> groupKey = new ArrayList<>();
            for (String var : groupByVars) {
                groupKey.add(tuple.getField(varToIndexMap.get(var)));
            }

            Integer sumValue = 0;
            Integer vIdx = 0;
            // TODO: solve the sum when y*y is in the sum term
            for (Term term : sumTerm.getProductTerms()) {
                if (term instanceof IntegerConstant) {
                    sumValue += ((IntegerConstant) term).getValue();
                } else if (term instanceof Variable) {
                    vIdx = varToIndexMap.get(((Variable) term).getName());
                    if (vIdx != null) {
                        sumValue += ((IntegerConstant) tuple.getField(vIdx)).getValue();
                    }
                }
            }
            groups.putIfAbsent(groupKey, 0);
            groups.put(groupKey, groups.get(groupKey) + sumValue);
            tuple = child.getNextTuple();
        }
    }
}

