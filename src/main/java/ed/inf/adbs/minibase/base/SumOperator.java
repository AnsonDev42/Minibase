package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SumOperator extends Operator {
    private final Operator child;
    private final List<Variable> groupByVars;
    private final HashMap<List<Object>, Integer> groups = new HashMap<>();
    private final SumAggregate sumTerm;
    private boolean sumComputed = false;
    private Iterator<Map.Entry<List<Object>, Integer>> groupsIterator;
    private final Map<Term, Integer> projectedVarToIndexMap;


    /**
     * Constructor
     *
     * @param child child operator
     * @param head  head of the query
     */
    public SumOperator(Operator child, Head head) {
        this.child = child;
        this.groupByVars = head.getVariables();
        this.sumTerm = head.getSumAggregate();
        this.projectedVarToIndexMap = createProjectedVarToIndexMap(); // final projected var to index map
    }

    private Map<Term, Integer> createProjectedVarToIndexMap() {
        List<Variable> projectedVars = ((ProjectOperator) child).getProjectedVars();
        Map<Term, Integer> projectedVarToIndexMap = new HashMap<>();
        for (int i = 0; i < projectedVars.size(); i++) {
            projectedVarToIndexMap.put(projectedVars.get(i), i);
        }
        return projectedVarToIndexMap;
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
            List<Term> resultTerms = new ArrayList<>();
            for (Object keyElement : groupKey) {
                if (keyElement instanceof IntegerConstant) {
                    resultTerms.add((IntegerConstant) keyElement);
                } else if (keyElement instanceof Variable) {
                    throw new RuntimeException("Invalid key element type var");
                } else {
                    //skip since maybe empty(when the key is 'DEFAULT_GROUP') here
//                    throw new RuntimeException("Invalid key element type");
                }
            }
            resultTerms.add(new IntegerConstant(entry.getValue()));
            return new Tuple(resultTerms.toArray());
        }
        return null;
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
            if (groupByVars.isEmpty()) {
                // handle case when there is no group by clause. (since no clause, the group key is unique guaranteed.
                groupKey.add("EMPTY_GROUP");
            } else {
                for (Variable var : groupByVars) {
                    groupKey.add(tuple.getField(projectedVarToIndexMap.get(var)));
                }
            }
            int productValue = 1;
            Integer vIdx;
            Boolean changedProduct = false;
            for (Term term : sumTerm.getProductTerms()) {
//                System.out.println("v2 checking term class" + term.getClass());
                if (term instanceof IntegerConstant) {
                    productValue *= ((IntegerConstant) term).getValue();
                    changedProduct = true;
                } else if (term instanceof Variable) {
                    vIdx = projectedVarToIndexMap.get(term);
                    if (vIdx != null) {
                        productValue *= ((IntegerConstant) tuple.getField(vIdx)).getValue();
                        changedProduct = true;
                    }
                }
            }
            groups.putIfAbsent(groupKey, 0); // solve null pointer exception
            if (changedProduct) {
                groups.put(groupKey, groups.get(groupKey) + productValue);
            }
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

