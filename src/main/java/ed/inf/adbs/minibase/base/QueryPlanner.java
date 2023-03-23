package ed.inf.adbs.minibase.base;

import java.io.IOException;
import java.util.*;

import static ed.inf.adbs.minibase.Utils.copyTerm;
import static ed.inf.adbs.minibase.Utils.swapCondition;

public class QueryPlanner {
    private static Operator operator;


    public QueryPlanner(Query query) throws Exception {
        operator = buildQueryPlan(query); // See algorithm inside this method

    }

    public Operator getOperator() {
        return operator;
    }


    /**
     * With pushdown optimization!
     *
     * @param query
     * @return
     * @throws Exception
     */
    public static Operator buildQueryPlan(Query query) throws Exception {
        Head head = query.getHead();
        List<Atom> body = query.getBody();
        if (body.size() == 0) {
            throw new Exception("Query body is empty");
        }
        // STEP1 : remove all always true condition, return dummy operator if any (Constant)condition is always False
        if (removeCondition(body)) { // remove all always true condition TODO: can add more optimization here
            return new dummyOperator();
        }

        // STEP2: handle hidden condition, by extracting them and add them to the body
        int conIdx = findAndUpdateCondition(body);

        if (conIdx == 0) { // try to optimize for following functions to iterate either relational Atoms or condition Atoms
            conIdx = body.size();
        }
        // STEP3 optimise: push down selection
        HashSet<String> requiredColumns = computeRequiredColumns(body, head, conIdx);
        // STEP4: create join tree for relational atoms, add create selection operator if needed
        HashMap<String, Integer> jointTupleVarToIdx = createJointTupleVarToIdx(body, conIdx, requiredColumns);
        Operator root = createDeepLeftJoinTree(body, conIdx, jointTupleVarToIdx, requiredColumns);
        // STEP4: create projected variables list( include vars in SumAgg if SumAgg existed)
        List<Variable> projectedVars = getProjectedVars(head, jointTupleVarToIdx);
        // STEP5: add project operator if needed

        if (needProject(head, jointTupleVarToIdx)) {
            if (head.getSumAggregate() != null) {
                root = new ProjectOperator(root, projectedVars, jointTupleVarToIdx, false);
            } else {
                root = new ProjectOperator(root, projectedVars, jointTupleVarToIdx, true);
            }
        }
        // STEP6: add sum operator if needed
        if (head.getSumAggregate() != null) {
            if (!(root instanceof ProjectOperator)) {
                throw new Exception("Sum operator must be after project operator");
            }
            root = new SumOperator(root, head);
        }
        return root;
    }

    /**
     * return a list of projected variables including variables in sum aggregate
     *
     * @param head
     * @param jointTupleVarToIdx
     * @return List<Term> projectedVars
     */
    public static List<Variable> getProjectedVars(Head head, HashMap<String, Integer> jointTupleVarToIdx) {
        List<Variable> projectedVars = new ArrayList<>(head.getVariables());
        if (head.getSumAggregate() != null) {
            for (Term term : head.getSumAggregate().getProductTerms()) {
                if (term instanceof Variable && !projectedVars.contains(term)) {
                    projectedVars.add((Variable) term);
                }
            }
        }
        return projectedVars;
    }

    /**
     * get requried coloumn variables
     *
     * @param body   preprocessed body
     * @param head   head of the query
     * @param conIdx index of the first condition atom
     * @return HashSet<String> requiredColumns
     */
    public static HashSet<String> computeRequiredColumns(List<Atom> body, Head head, int conIdx) {
        HashSet<String> requiredColumns = new HashSet<>();
        ArrayList<HashMap<Integer, HashSet<ComparisonAtom>>> conMaps = createTwoConMap(body, conIdx);
        HashMap<Integer, HashSet<ComparisonAtom>> selMap = conMaps.get(0);
        HashMap<Integer, HashSet<ComparisonAtom>> joinMap = conMaps.get(1);
//      Iterate over selMap and Joinmap to find all the columns that are required
        Arrays.asList(selMap, joinMap).forEach(map -> {
            map.forEach((k, v) -> {
                v.forEach(atom -> {
                    if (!atom.isTwoConstant()) {
                        if (atom.getTerm1() instanceof Variable) {
                            requiredColumns.add(((Variable) atom.getTerm1()).getName());
                        }
                        if (atom.getTerm2() instanceof Variable) {
                            requiredColumns.add(((Variable) atom.getTerm2()).getName());
                        }
                    }
                });
            });
        });

        for (Variable var : head.getVariables()) {
            requiredColumns.add(var.getName());
        }
        if (head.getSumAggregate() != null) {
            head.getSumAggregate().getProductTerms().forEach(term -> {
                if (term instanceof Variable) {
                    requiredColumns.add(((Variable) term).getName());
                }
            });
        }
        return requiredColumns;
    }


    /**
     * check if the query need to be projected
     *
     * @param head               the head of the query
     * @param jointTupleVarToIdx the mapping from variable to index in the joint tuple
     * @return true if the query need to be projected
     */
    public static boolean needProject(Head head, HashMap<String, Integer> jointTupleVarToIdx) {
        int headIdx = 0;
        if (jointTupleVarToIdx.size() != head.getVariables().size() || head.getSumAggregate() != null) {
            return true;
        }
        for (Variable var : head.getVariables()) {
            if (headIdx != jointTupleVarToIdx.get(var.toString())) {
                return true;
            }
        }
        return false;
    }


    /**
     * This method is used to build the query plan. See more details below.
     *
     * @param query the query to be executed
     * @return the root operator of the query plan
     */
//    public static Operator buildQueryPlan(Query query) throws Exception {
//        Head head = query.getHead();
//        List<Atom> body = query.getBody();
//        if (body.size() == 0) {
//            throw new Exception("Query body is empty");
//        }
//        // STEP1 : remove all always true condition, return dummy operator if any (Constant)condition is always False
//        if (removeCondition(body)) { // remove all always true condition TODO: can add more optimization here
//            return new dummyOperator();
//        }
//        // STEP2: handle hidden condition, by extracting them and add them to the body
//        int conIdx = findAndUpdateCondition(body);
//        if (conIdx == 0) { // try to optimize for following functions to iterate either relational Atoms or condition Atoms
//            conIdx = body.size();
//        }
//        // STEP3: create join tree for relational atoms, add create selection operator if needed
//        HashMap<String, Integer> jointTupleVarToIdx = createJointTupleVarToIdx(body, conIdx);
//        Operator root = createDeepLeftJoinTree(body, conIdx, jointTupleVarToIdx);
//        // STEP4: add project operator if needed
//        if (jointTupleVarToIdx.size() > head.getVariables().size() && head.getSumAggregate() == null) {
//            root = new ProjectOperator(root, head.getVariables(), jointTupleVarToIdx);
//        }
//        // STEP5: add sum operator if needed
//        if (head.getSumAggregate() != null) {
//            root = new SumOperator(root, head, jointTupleVarToIdx);
//        }
//        return root;
//    }


    /**
     * return a map of variable name to index in final join tuple
     *
     * @param body   final body that should not be modified
     * @param conIdx index of the first condition in the body
     * @return map of variable name to index in final join tuple
     */
    public static HashMap<String, Integer> createJointTupleVarToIdx(List<Atom> body, int conIdx, HashSet<String> requiredColumns) {
        HashMap<String, Integer> varIndexInJoinTupleMap = new HashMap<>();
        int index = 0;
        if (conIdx == 0) {
            conIdx = body.size();
        }
        for (int i = 0; i < conIdx; i++) {
            RelationalAtom atom = (RelationalAtom) body.get(i);
            String[] fieldNames = atom.getFieldsName();
            for (String fieldName : fieldNames) {
                // Only add the field to the map if it's in the requiredColumns set
                if (requiredColumns.contains(fieldName)) {
                    varIndexInJoinTupleMap.put(fieldName, index);
                    index++;
                }
            }
        }
        return varIndexInJoinTupleMap;
    }


    /**
     * create join tree for relational atoms, add create selection operator if needed
     *
     * @param body               prepossessed body contains no repeated variables
     * @param conIdx             index of first condition atom
     * @param jointTupleVarToIdx map from variable name to index in final join tuple(returned tuple from root join operator)
     * @return root of join tree
     * @throws IOException
     */
    public static Operator createDeepLeftJoinTree(List<Atom> body, int conIdx, HashMap<String, Integer> jointTupleVarToIdx, HashSet<String> requiredColumns) throws IOException {
        ArrayList<HashMap<Integer, HashSet<ComparisonAtom>>> conMaps = createTwoConMap(body, conIdx);
        HashMap<Integer, HashSet<ComparisonAtom>> selMap = conMaps.get(0);
        HashMap<Integer, HashSet<ComparisonAtom>> joinMap = conMaps.get(1);
        // Create initial operators for each relAtoms based on if they have selection conditions
        ArrayList<Operator> operators = new ArrayList<>();
        for (int i = 0; i < conIdx; i++) {
            if (!(body.get(i) instanceof RelationalAtom)) { // should not happen
                break;
            }
            RelationalAtom relAtom = (RelationalAtom) body.get(i);
            if (selMap.get(i) != null && selMap.get(i).isEmpty()) {
                operators.add(new ScanOperator(relAtom, requiredColumns));
            } else {
                operators.add(new SelectOperator(relAtom, new ArrayList<>(selMap.get(i)), requiredColumns));
            }
        }
        // Create deep left join tree
        Operator root = operators.get(0); // just pointer
        if (body.size() == 1) {
            return root; // no join needed
        }
        HashSet<ComparisonAtom> leftConditionPool = joinMap.get(0); // store all join conditions for left child
        for (int i = 1; i < conIdx; i++) {
            // create intersection of join conditions between left (joined) Tuple and right (to be joined) tuple
            HashSet<ComparisonAtom> rightChildConditions = joinMap.get(i);
            HashSet<ComparisonAtom> intersection = new HashSet<>(leftConditionPool);
            intersection.retainAll(rightChildConditions);
            Operator rightChild = operators.get(i);
            root = new JoinOperator(root, rightChild, jointTupleVarToIdx, (RelationalAtom) body.get(i), new ArrayList<>(intersection), requiredColumns);
            leftConditionPool.addAll(rightChildConditions); // Update leftConditionPool
        }
        return root;
    }


    /**
     * Find first index of ComparisonAtom in the body of the query (if it exists)
     * given that the comparison atoms are always after the relational atoms.
     *
     * @param body
     * @return the index of the first comparison atom if it exists, -1 otherwise
     */
    public static int findComparisonAtoms(List<Atom> body) {
        int i = 0;
        for (Atom atom : body) {
            if (atom instanceof ComparisonAtom) {
                return i;
            }
            i++;
        }
        return 0;
    }

    /**
     * remove always true condition from the body;
     * return true if contains always false condition (for now only 2 constants) so that the query is always false(no result)
     *
     * @param body body of the query
     */
    public static Boolean removeCondition(List<Atom> body) {
        int tmp_conidx = findComparisonAtoms(body);
        if (tmp_conidx != 0) {
            for (int i = tmp_conidx; i < body.size(); i++) {
                if (body.get(i) instanceof ComparisonAtom) {
                    ComparisonAtom atom = (ComparisonAtom) body.get(i);
                    if (atom.isTwoConstant()) {
                        if (atom.evalTwoConstant()) { // always true
                            body.remove(i);
                            i--;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


    /**
     * Create a hashMap to store all defined variables in the body, and set the value to 1,
     * for later to find repeated variables when iterate through body
     *
     * @param body body of the query
     * @return two maps for selection and join conditions
     */
    public static HashMap<String, Integer> getDefinedVariables(List<Atom> body) {
        HashMap<String, Integer> definedVariables = new HashMap<>();
        for (Atom atom : body) {
            if (atom instanceof RelationalAtom) {
                RelationalAtom relAtom = (RelationalAtom) atom;
                relAtom.getVarsNames().forEach(term -> definedVariables.put(term, 1));
            } else {
                break;
            }
        }
        return definedVariables;
    }


    /**
     * STEP2: Find the first condition in the body of the query and update the body, if it exists,
     * and modify the relational atom(if include constant) by creating new var name and add condition.
     * e.g. R(x,y) :- R(x, 5)    --->  R(x,y) :- R(x, y), y = 5 and return 1
     *
     * @param body query body to be modified
     * @return first condition in the body of the query, else -1
     */
    public static int findAndUpdateCondition(List<Atom> body) {
        /*  workflow
        potential optimization: build a table for each var e.g.  'x' : {R(0), S(1), T(2)}
            STEP2.1: extract hidden selection condition and add to the body, without touching join
                e.g. R(x, 5), S(x, b), ==> R(x, y), S(x, b), y=5
            STEP2.2: extract join condition and add to the body,
                e.g.  R(x, y), S(a, b), y=5 ==> R(x, y), S(a, b), y=5, x=a
            STEP2.3: create both selection and join condition map for EACH relational atom
         */
        HashMap<String, Integer> preDefVariables = getDefinedVariables(body);
        HashMap<String, HashSet> varToJoinVarIdx = initVarToJoinVarIdx(preDefVariables);
        Random ranObj = new Random();
        int conIdx = 0;
        // STEP2.1; 2.2: extract hidden SELECTION and JOIN conditions to the body
        for (int i = 0; i < body.size(); i++) {
            Atom atom = body.get(i);
            if (atom instanceof RelationalAtom) {
                if (addImpliedConditions(body, ranObj, preDefVariables, i, atom, varToJoinVarIdx) && conIdx == 0) {
                    conIdx = i;// prevent the below did not get any condition
                }
            }
            if (atom instanceof ComparisonAtom) {
                conIdx = i;
                break;
            }
        }
        return conIdx;
    }


    /**
     * STEP3: create both selection and join condition map for the query
     * // e.g. Q(...) :- R(x, 5), R(7, y)  --->  R(x, a), R(b, y), a = 5, b = 7
     * //      rToSelConIdx {0: [2], 1: [3] }, since  2 is "a=5", 3 is "b=7"
     *
     * @param body   query body
     * @param conIdx
     * @returna a list of two maps, map1 is the selection condition map, map2 is the join condition map
     */
    public static ArrayList<HashMap<Integer, HashSet<ComparisonAtom>>> createTwoConMap(List<Atom> body, int conIdx) {
        HashMap<Integer, HashSet<ComparisonAtom>> selMap = new HashMap<>();
        HashMap<Integer, HashSet<ComparisonAtom>> joinMap = new HashMap<>();

        for (int i = 0; i < conIdx; i++) {
            selMap.put(i, new HashSet<>());
            joinMap.put(i, new HashSet<>());
        }

        for (int i = conIdx; i < body.size(); i++) {
            if (!(body.get(i) instanceof ComparisonAtom)) {
                break;
            }
            ComparisonAtom condition = (ComparisonAtom) body.get(i);

            int leftIdx = -1;
            int rightIdx = -1;
            for (int j = 0; j < conIdx; j++) {
                RelationalAtom atom = (RelationalAtom) body.get(j);
                if (condition.getTerm1() instanceof Variable && atom.getVarsNames().contains(((Variable) condition.getTerm1()).getName())) {
                    leftIdx = j;
                }
                if (condition.getTerm2() instanceof Variable && atom.getVarsNames().contains(((Variable) condition.getTerm2()).getName())) {
                    rightIdx = j;
                }
            }
            if (leftIdx == -1 && rightIdx == -1) { // would not happen, since we have already checked
                throw new RuntimeException("Error: condition contains variable not in the body:" + condition);
            }
            if (leftIdx != -1 && rightIdx != -1) {
                if (leftIdx != rightIdx) { // join condition( selection not in the same atom)
                    if (leftIdx > rightIdx) {
                        condition = swapCondition(condition); // swap the two terms, can be delete later
                        body.set(i, condition); // always put the smaller index in the left
                    }
                    joinMap.get(leftIdx).add(condition);
                    joinMap.get(rightIdx).add(condition);
                }
            } else { // selection condition (one of the term is constant)
                selMap.get(leftIdx).add(condition);
            }
        }
        ArrayList<HashMap<Integer, HashSet<ComparisonAtom>>> conMaps = new ArrayList<>();
        conMaps.add(selMap);
        conMaps.add(joinMap);
        return conMaps;
    }


    /**
     * Initialize the varToJoinVarIdx hashmap
     * by iterate through all the str(vars) into the hashmap, and maps to an empty list
     */
    public static HashMap<String, HashSet> initVarToJoinVarIdx(HashMap<String, Integer> definedVariables) {
        HashMap<String, HashSet> varToJoinVarIdx = new HashMap<>();
        for (String key : definedVariables.keySet()) {
            varToJoinVarIdx.put(key, new HashSet<>());
        }
        return varToJoinVarIdx;
    }


    /**
     * Update body if there is hidden condition in the relational atom (e.g. R(x, 5))
     * by replacing new variables to Constants (e.g. 5-> y);
     * and adding new condition to the body (e.g. y = 5);
     * and updating the varToJoinVarIdx hashmap
     * e.g. Q() :- R(x, 5)    --->  Q() :- R(x, y), y = 5      and return true
     *
     * @param body       query body to be modified
     * @param ranObj     random object
     * @param preDefVars all pre-defined variables (in the query) by user
     * @param i          index of the relational atom in the body
     * @param atom       the relational atom to be modified
     * @return true if the relational atom is modified, false otherwise
     */
    public static Boolean addImpliedConditions(List<Atom> body, Random ranObj,
                                               HashMap<String, Integer> preDefVars, int i, Atom atom,
                                               HashMap<String, HashSet> preDefVarToJoinedVarsMap) {
        RelationalAtom relationalAtom = (RelationalAtom) atom;
        Boolean added = false;
        List<Term> terms = relationalAtom.getTerms();
        for (int j = 0; j < terms.size(); j++) {
            String newVar = createNewVar(preDefVars, ranObj);
            Term term = terms.get(j);
            if (term instanceof Constant) { // if the term is a constant
                preDefVars.put(newVar, 0);// it will never be used again since new var is unique across the query
                // update the term in relational atom e.g. (x, 5) ---> (x, y)
                relationalAtom.setTerm(j, new Variable(newVar));
                // add the hidden con to body e.g.  "y = 5"
                body.add(new ComparisonAtom(new Variable(newVar), copyTerm(term), ComparisonOperator.EQ));
                added = true;          // update the changed status
            } else if (preDefVars.get(term.toString()) != null) { // if the term is repeated so far
                // update the term in the relational atom e.g. (x,z) ---> (x,z'), z=z'
                if (preDefVars.get(term.toString()) == 0) {
                    preDefVarToJoinedVarsMap.get(term.toString()).add(new Variable(newVar));
                    relationalAtom.setTerm(j, new Variable(newVar));
                    body.add(new ComparisonAtom(term, new Variable(newVar), ComparisonOperator.EQ));
                    added = true;          // update the changed status
                } else {
                    preDefVars.remove(term.toString());
                    preDefVars.put(term.toString(), 0);
                }
            }
        }
        if (added) {
            body.set(i, relationalAtom); // update in the body e.g. R(x, 5) ---> R(x, y)
        }
        return added;
    }

    /**
     * Create a new variable that is not in the pre-defined variables
     *
     * @param preDefVars pre-defined variables i.e. to create a unique variable name
     * @param ranObj     random object
     * @return a new unique(so far) and unseen variable name
     */
    public static String createNewVar(HashMap<String, Integer> preDefVars, Random ranObj) {
        String newVar = "";
        do {
            newVar = ranObj.nextInt() + "";
        } while (preDefVars.get(newVar) != null);
        return newVar;
    }
}

