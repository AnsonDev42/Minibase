package ed.inf.adbs.minibase.base;

import java.util.*;

import static ed.inf.adbs.minibase.Utils.copyTerm;

public class QueryPlanner {
    private final SelectOperator selectOperator;
    private final ProjectOperator projectOperator;
    private final ScanOperator scanOperator;

    public QueryPlanner(Query query) throws Exception {
        HashMap<String, Operator> planOperatorMap = buildQueryPlan(query);
        this.scanOperator = (ScanOperator) planOperatorMap.get("scan");
        this.selectOperator = (SelectOperator) planOperatorMap.get("select");
        this.projectOperator = (ProjectOperator) planOperatorMap.get("project");
    }

    public Operator getRootOperator() {
        if (this.projectOperator != null) {
            return this.projectOperator;
        } else if (this.selectOperator != null) {
            return this.selectOperator;
        } else {
            return this.scanOperator;
        }
    }


    public static HashMap<String, Operator> buildQueryPlan_withJoin(Query query) throws Exception {
        HashMap<String, Operator> results = new HashMap<>();
        Head head = query.getHead();
        List<Atom> body = query.getBody();
        if (body.size() == 0) {
            throw new Exception("Query body is empty");
        }

        // handle hidden condition
        int conIdx = 0;
        conIdx = findAndUpdateCondition(body);
        ArrayList conMaps = createTwoConMap(body, conIdx);
        HashMap<Integer, HashSet> selMap = (HashMap) conMaps.get(0);
        HashMap<Integer, HashSet> joinMap = (HashMap) conMaps.get(1);

        // execute the body with deep left join
        Operator rootOperator = null;
        Operator leftChild = null;
        Operator rightChild = null;
        for (int i = 0; i < conIdx - 1; i++) {
            RelationalAtom currRelAtom = (RelationalAtom) body.get(i);
            RelationalAtom nextRelAtom = (RelationalAtom) body.get(i + 1);
            Operator parentOperator = null;
            if (i == 0) {
                if (selMap.get(i).size() > 0) {
                    leftChild = new SelectOperator(currRelAtom, (List<ComparisonAtom>) selMap.get(i)); // TODO: fix here
                } else {
                    leftChild = new ScanOperator(currRelAtom.getName());
                }
                rightChild = new ScanOperator(nextRelAtom.getName()); //TODO: might be SelectOperator
            } else {
                leftChild = new JoinOperator(leftChild, rightChild, Arrays.asList(currRelAtom, nextRelAtom), (List<ComparisonAtom>) joinMap.get(i));
                rightChild = new ScanOperator(nextRelAtom.getName());
                parentOperator = new JoinOperator(leftChild, rightChild, Arrays.asList(currRelAtom, nextRelAtom), (List<ComparisonAtom>) joinMap.get(i));


            }


            break;
        }

        return null;
    }


    /**
     * Build the query plan for the given query
     *
     * @param query the query to be executed
     * @return the query plan
     * @throws Exception
     */
    public static HashMap<String, Operator> buildQueryPlan(Query query) throws Exception {

        HashMap<String, Operator> results = new HashMap<>();
        Head head = query.getHead();
        List<Atom> body = query.getBody();
        if (body.size() == 0) {
            throw new Exception("Query body is empty");
        }

        // handle hidden condition
        int conIdx = 0;
        conIdx = findAndUpdateCondition(body);
//        return null;


        // 1. create the scan operator
        Atom relationalAtom = body.get(0);
        if (relationalAtom instanceof RelationalAtom) {
            checkQueryMatchSchema((RelationalAtom) relationalAtom);
            String tableName = ((RelationalAtom) relationalAtom).getName();
            results.put("scan", new ScanOperator(tableName));
        } else
            throw new Exception("The first atom in the query body is not a relational atom");

//        2. check if it needed to select
        if (conIdx != 0) {
            List condition = body.subList(conIdx, body.size());
            checkQueryMatchSchema((RelationalAtom) relationalAtom);
            results.put("select", new SelectOperator((RelationalAtom) relationalAtom, condition));
            System.out.println("select added");
        }
        System.out.println("select failed added: " + conIdx);


        // check if needed to project by checking length of vars in head
        Boolean needProject = false;
        List<Variable> variables = head.getVariables();
        if (variables.size() != ((RelationalAtom) relationalAtom).getTerms().size()) {
            needProject = true;
        }
        // check if  order of variables in the head is the same as the schema e.g .Q(y,x) :- R(x,y)
        if (!needProject) {
            for (int i = 0; i < variables.size(); i++) {
                if (!variables.get(i).getName().equals(((RelationalAtom) relationalAtom).getTerms().get(i).toString())) {
                    needProject = true;
                    break;
                }
            }
        }
        if (needProject && conIdx != 0) {
            results.put("project", new ProjectOperator(results.get("select"), variables, relationalAtom));
        } else if (needProject && conIdx == 0) {
            results.put("project", new ProjectOperator(results.get("scan"), variables, relationalAtom));
        }

        return results;
    }


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
//

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
    public static ArrayList createTwoConMap(List<Atom> body, int conIdx) {
        HashMap<Integer, HashSet> rToSelConIdx = new HashMap<>();
        HashMap<Integer, HashSet> rToJoinConIdx = new HashMap<>();
        ArrayList res = new ArrayList();
        res.add(rToSelConIdx);
        res.add(rToJoinConIdx);

        for (int ri = 0; ri < conIdx; ri++) {
            Atom atom = body.get(ri);
            RelationalAtom relAtom = (RelationalAtom) atom;
            HashSet<String> varInRelAtom = relAtom.getVarsNames();
            rToSelConIdx.put(ri, new HashSet<>());
            rToJoinConIdx.put(ri, new HashSet<>());
            // create a set store all the variables in the relational atom
            //find the variables in the relational atom that is also in the join condition
            for (int ci = conIdx; ci < body.size(); ci++) {
                ComparisonAtom innerCompAtom = (ComparisonAtom) body.get(ci);// if this wrong then conIdx wrong
                if (varInRelAtom.contains(innerCompAtom.getTerm1().toString())) {
                    if (innerCompAtom.getTerm2() instanceof Constant) {
                        rToSelConIdx.get(ri).add(ci);
                        System.out.println("selection condition: " + innerCompAtom);
                    } else if (innerCompAtom.getTerm2() instanceof Variable) { // if the second term is a variable
                        rToJoinConIdx.get(ri).add(ci);
                        System.out.println("join condition: " + innerCompAtom);
                    }
                }
            }
        }
        return res;
    }


    /**
     * STEP4: create a hashmap to store the indices of relational atom TO a list of ONLY join condition
     * this is fast since we already got varToJoinVarIdx in STEP2,
     *
     * @param body
     * @param conIdx
     * @param varToJoinVarIdx
     * @return
     */
    private static HashMap<Integer, HashSet> createJoinConditionMap(List<Atom> body, int conIdx, HashMap<String, HashSet> varToJoinVarIdx) {
        HashMap<Integer, HashSet> relAtomToJoinConIdx = new HashMap<>();
        for (int i = 0; i < conIdx; i++) {
            Atom atom = body.get(i);
            RelationalAtom relAtom = (RelationalAtom) atom;
            // create a set store all the variables in the relational atom
            HashSet<String> varInRelAtom = relAtom.getVarsNames();
            for (String var : varInRelAtom) {
                HashSet<Integer> joinVarIdx = varToJoinVarIdx.get(var);
                if (joinVarIdx.size() > 0) {
                    for (int joinIdx : joinVarIdx) {
                        relAtomToJoinConIdx.get(joinIdx).add(i);
                    }
                }
            }

        }
        return relAtomToJoinConIdx;
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

