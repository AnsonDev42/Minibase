package ed.inf.adbs.minibase.base;

import javax.management.RuntimeErrorException;
import java.io.IOException;
import java.util.*;

import static ed.inf.adbs.minibase.Utils.copyTerm;
import static ed.inf.adbs.minibase.Utils.swapCondition;

public class QueryPlanner {
    private static Operator operator;

    public QueryPlanner(Query query) throws Exception {
        operator = buildQueryPlan_withJoin(query);
    }

    public Operator getOperator() {
        return operator;
    }

    public static HashMap<String, Integer> createJointTupleVarToIdx(List<Atom> body, int conIdx) {
        HashMap<String, Integer> varIndexInJoinTupleMap = new HashMap<>();
        int index = 0;
        if (conIdx == 0) {
            conIdx = body.size();
            System.out.println("conIdx: " + conIdx);
        }
        for (int i = 0; i < conIdx; i++) {
            RelationalAtom atom = (RelationalAtom) body.get(i);
            for (Term term : atom.getTerms()) {
                if (term instanceof Variable) {
                    varIndexInJoinTupleMap.put(((Variable) term).getName(), index);
                    index++;
                }
            }
        }
        return varIndexInJoinTupleMap;
    }


    public static Operator buildQueryPlan_withJoin(Query query) throws Exception {
        Head head = query.getHead();
        List<Atom> body = query.getBody();
        if (body.size() == 0) {
            throw new Exception("Query body is empty");
        }
        if (removeCondition(body)) { // remove all always true condition TODO: can add more optimization here
            body = new ArrayList<Atom>(); // clear all body since one condition is always false
            //TODO: do something faster?
        }
        // handle hidden condition
        int conIdx = findAndUpdateCondition(body);
        if (conIdx == 0) {
            conIdx = body.size();
        }
        HashMap<String, Integer> jointTupleVarToIdx = createJointTupleVarToIdx(body, conIdx);
        Operator root = createDeepLeftJoinTree(body, conIdx, jointTupleVarToIdx);
        System.out.println("jointTupleVarToIdx.size: " + jointTupleVarToIdx.size() + " head.getVariables().size(): " + head.getVariables().size());
        if (jointTupleVarToIdx.size() > head.getVariables().size()) {
            // add project operator
            System.out.println("head vars: " + head.getVariables());
            System.out.println("jointTupleVarToIdx: " + jointTupleVarToIdx);
            root = new ProjectOperator(root, head.getVariables(), jointTupleVarToIdx);
        }
        return root;
    }


    public static Operator createDeepLeftJoinTree(List<Atom> body, int conIdx, HashMap<String, Integer> jointTupleVarToIdx) throws IOException {
        ArrayList<HashMap<Integer, HashSet<ComparisonAtom>>> conMaps = createTwoConMap(body, conIdx);
        HashMap<Integer, HashSet<ComparisonAtom>> selMap = conMaps.get(0);
        HashMap<Integer, HashSet<ComparisonAtom>> joinMap = conMaps.get(1);
//        HashMap<String, Integer> jointTupleVarToIdx = createJointTupleVarToIdx(body, conIdx);

        // Create initial operators
        ArrayList<Operator> operators = new ArrayList<>();
        for (int i = 0; i < conIdx; i++) {
            if (!(body.get(i) instanceof RelationalAtom)) {
                break;
            }
            RelationalAtom relAtom = (RelationalAtom) body.get(i);
            if (selMap.get(i) != null && selMap.get(i).isEmpty()) {
                operators.add(new ScanOperator(relAtom.getName()));
            } else {
                operators.add(new SelectOperator(relAtom, new ArrayList<>(selMap.get(i))));
            }
        }


        // Create deep left join tree
        Operator root = operators.get(0); // just pointer
        if (body.size() == 1) {
            return root;
        }
        HashSet<ComparisonAtom> leftConditionPool = joinMap.get(0);
        for (int i = 1; i < conIdx; i++) {
            // create intersection of join conditions between left(joined)Tuple and right (to be joined) tuple
            HashSet<ComparisonAtom> rightChildConditions = joinMap.get(i);
            HashSet<ComparisonAtom> intersection = new HashSet<>(leftConditionPool);
            intersection.retainAll(rightChildConditions);
            Operator rightChild = operators.get(i);
            root = new JoinOperator(root, rightChild, jointTupleVarToIdx, (RelationalAtom) body.get(i), new ArrayList<>(intersection));
            if (rightChild instanceof JoinOperator) {
                throw new IllegalStateException("Right child should not be a JoinOperator");
            }
            JoinOperator jr_root = (JoinOperator) root;
            if (jr_root.getRightChild() instanceof JoinOperator) {
                throw new IllegalStateException("Right child should not be a JoinOperator");
            }
            // Update leftConditionPool
            leftConditionPool.addAll(rightChildConditions);
        }
        return root;
    }


    /**
     * Build the query plan for the given query
     *
     * @param query the query to be executed
     * @return the query plan
     * @throws Exception
     */
//    public static HashMap<String, Operator> buildQueryPlan(Query query) throws Exception {
//
//        HashMap<String, Operator> results = new HashMap<>();
//        Head head = query.getHead();
//        List<Atom> body = query.getBody();
//        if (body.size() == 0) {
//            throw new Exception("Query body is empty");
//        }
//
//        // handle hidden condition
//        int conIdx = 0;
//        conIdx = findAndUpdateCondition(body);
////        return null;
//
//
//        // 1. create the scan operator
//        Atom relationalAtom = body.get(0);
//        if (relationalAtom instanceof RelationalAtom) {
//            checkQueryMatchSchema((RelationalAtom) relationalAtom);
//            String tableName = ((RelationalAtom) relationalAtom).getName();
//            results.put("scan", new ScanOperator(tableName));
//        } else
//            throw new Exception("The first atom in the query body is not a relational atom");
//
////        2. check if it needed to select
//        if (conIdx != 0) {
//            List condition = body.subList(conIdx, body.size());
//            checkQueryMatchSchema((RelationalAtom) relationalAtom);
//            results.put("select", new SelectOperator((RelationalAtom) relationalAtom, condition));
//            System.out.println("select added");
//        }
//        System.out.println("select failed added: " + conIdx);
//
//
//        // check if needed to project by checking length of vars in head
//        Boolean needProject = false;
//        List<Variable> variables = head.getVariables();
//        if (variables.size() != ((RelationalAtom) relationalAtom).getTerms().size()) {
//            needProject = true;
//        }
//        // check if  order of variables in the head is the same as the schema e.g .Q(y,x) :- R(x,y)
//        if (!needProject) {
//            for (int i = 0; i < variables.size(); i++) {
//                if (!variables.get(i).getName().equals(((RelationalAtom) relationalAtom).getTerms().get(i).toString())) {
//                    needProject = true;
//                    break;
//                }
//            }
//        }
//        if (needProject && conIdx != 0) {
//            results.put("project", new ProjectOperator(results.get("select"), variables, relationalAtom));
//        } else if (needProject && conIdx == 0) {
//            results.put("project", new ProjectOperator(results.get("scan"), variables, relationalAtom));
//        }
//
//        return results;
//    }


    /**
     * This function checks if the query is correct with respect to the schema
     *
     * @param atom the relational atom (in the query) to be checked
     * @throws IllegalArgumentException if the number of terms in the atom does not match the schema
     */
    public static void checkQueryMatchSchema(RelationalAtom atom) {
        int numInSchema = Catalog.getInstance(null).getSchema(atom.getName()).length;
        int numInAtom = atom.getTerms().size();
        if (numInAtom != numInSchema) {
            throw new IllegalArgumentException("The number of terms  in the atom:" + atom.getName() + " does not match the schema");
        }
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
     * remove always true condition from the body; return true if contains always false condition (for now only 2 constants)
     *
     * @param body
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


    public static HashMap<String, Integer> getDefinedVariables(List<Atom> body) {
        HashMap<String, Integer> definedVariables = new HashMap<>();
        // find all variable names in case of clash
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
     * Find the first condition in the body of the query and update the body, if it exists,
     * and modify the relational atom(if include constant) by creating new var name and add condition.
     * e.g. R(x,y) :- R(x, 5)    --->  R(x,y) :- R(x, y), y = 5 and return 1
     *
     * @param body query body to be modified
     * @return first condition in the body of the query, else -1
     */
    public static int findAndUpdateCondition(List<Atom> body) {
        HashMap<String, Integer> preDefVariables = getDefinedVariables(body);
        HashMap<String, HashSet> varToJoinVarIdx = initVarToJoinVarIdx(preDefVariables);
        Random ranObj = new Random();
        int conIdx = 0;
        /*  workflow
        potential optimization: build a table for each var e.g.  'x' : {R(0), S(1), T(2)}
            STEP1: extract hidden selection condition and add to the body, without touching join
                e.g. R(x, 5), S(x, b), ==> R(x, y), S(x, b), y=5
            STEP2: extract join condition and add to the body,
                e.g.  R(x, y), S(a, b), y=5 ==> R(x, y), S(a, b), y=5, x=a

            STEP3: create both selection and join condition map for EACH relational atom

         */

        // STEP1 AND STEP2: extract hidden SELECTION and JOIN conditions to the body
        for (int i = 0; i < body.size(); i++) {
            Atom atom = body.get(i);
            if (atom instanceof RelationalAtom) {
                // STEP1 extract selection condition; STEP2 extract join condition
                if (addImpliedConditions(body, ranObj, preDefVariables, i, atom, varToJoinVarIdx) && conIdx == 0) {
                    conIdx = i;// prevent the below did not get any condition
                }
            }
            if (atom instanceof ComparisonAtom) {
                conIdx = i;
                break;
            }
        }

        // STEP3: create both selection and join condition map for EACH relational atom: in other functions
//        ArrayList twoConMap = createTwoConMap(body, conIdx);
//        HashMap<Integer, HashSet> rToSelConIdx = (HashMap<Integer, HashSet>) twoConMap.get(0);
//        HashMap<Integer, HashSet> rToJoinConIdx = (HashMap<Integer, HashSet>) twoConMap.get(1);
//         this step now done outside this function in the main planner

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
            System.out.println("handling condition:" + condition.toString());

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
                if (leftIdx != rightIdx) { // join condition
                    if (leftIdx > rightIdx) {
                        condition = swapCondition(condition);
                        body.set(i, condition); // always put the smaller index in the left
                    }
                    joinMap.get(leftIdx).add(condition);
                    joinMap.get(rightIdx).add(condition);
                }
            } else { // selection condition
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

    //TODO: create a new var ( unseen in preDefVars)
    public static String createNewVar(HashMap<String, Integer> preDefVars, Random ranObj) {
        String newVar = "";
        do {
            newVar = ranObj.nextInt() + "";
        } while (preDefVars.get(newVar) != null);
        return newVar;
    }
}

