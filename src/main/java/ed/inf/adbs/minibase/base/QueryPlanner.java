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
        int conIdx = -1;
        findAndUpdateCondition(body, conIdx);
        // 1. create the scan operator
        Atom relationalAtom = body.get(0);
        if (relationalAtom instanceof RelationalAtom) {
            checkQueryMatchSchema((RelationalAtom) relationalAtom);
            String tableName = ((RelationalAtom) relationalAtom).getName();
            results.put("scan", new ScanOperator(tableName));
        } else
            throw new Exception("The first atom in the query body is not a relational atom");

//        2. check if need to select
        if (conIdx != -1) {
            List condition = body.subList(conIdx, body.size());
            checkQueryMatchSchema((RelationalAtom) relationalAtom);
            results.put("select", new SelectOperator((RelationalAtom) relationalAtom, condition));
        }


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
        if (needProject && conIdx != -1) {
            results.put("project", new ProjectOperator(results.get("select"), variables, relationalAtom));
        } else if (needProject && conIdx == -1) {
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
        return -1;
    }


    public static HashMap<String, Integer> getDefinedVariables(List<Atom> body) {
        HashMap<String, Integer> definedVariables = new HashMap<>();
        // find all variable names in case of clash
        for (Atom atom : body) {
            if (atom instanceof RelationalAtom) {
                for (Term term : ((RelationalAtom) atom).getTerms()) {
                    if (term instanceof Variable) {
                        definedVariables.put(term.toString(), 1);
                    }
                }
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
    public static int findAndUpdateCondition(List<Atom> body, int conIdx) {
        HashMap<String, Integer> preDefVariables = getDefinedVariables(body);
        HashMap<String, HashSet> varToJoinVarIdx = initVarToJoinVarIdx(preDefVariables);
        Random ranObj = new Random();
        /*  workflow
        potential optimization: build a table for each var e.g.  'x' : {R(0), S(1), T(2)}
            STEP1: extract hidden selection condition and add to the body, without touching join
                e.g. R(x, 5), S(x, b), ==> R(x, y), S(x, b), y=5
            STEP2: extract join condition and add to the body,
                e.g.  R(x, y), S(a, b), y=5 ==> R(x, y), S(a, b), y=5, x=a

            STEP3: create a hashmap to store the indices of relational atom TO a list of ONLY selection condition
            so that the selection condition can be handle first before join( by creating a child selection operator)

            STEP4: create a hashmap to store the indices of relational atom TO a list of ONLY join condition
            the size of hashmap is num(RelationalAtoms) - 1, because the last relational atom does join with right
         */

        // STEP1 AND 2: extract hidden SELECTION and JOIN conditions to the body
        for (int i = 0; i < body.size(); i++) {
            Atom atom = body.get(i);
            if (atom instanceof RelationalAtom) {
                // STEP1 extract selection condition; STEP2 extract join condition
                if (addImpliedConditions(body, ranObj, preDefVariables, i, atom, varToJoinVarIdx) && conIdx == 0) {
                    conIdx = i;
                }
            }
            if (atom instanceof ComparisonAtom) {
                conIdx = i;
                break;
            }
        }

        // STEP3: create a hashmap to store the indices of relational atom TO a list of ONLY Selection condition
        // so that the selection condition can be handle first before join( by creating a child selection operator)
        // e.g. Q(...) :- R(x, 5), R(7, y)  --->  R(x, a), R(b, y), a = 5, b = 7
        //      return  {x: [2, 3], 1: [2,3] }, since  2 is "a=5", 3 is "b=7"

        createSelectionConditionMap(body, conIdx, varToJoinVarIdx);

        // STEP4: create a hashmap to store the indices of relational atom TO a list of ONLY join condition
        createJoinConditionMap(body, conIdx, varToJoinVarIdx);

        return -1;
    }

    private static void createJoinConditionMap(List<Atom> body, int conIdx, HashMap<String, HashSet> varToJoinVarIdx) {
    }

    private static void createSelectionConditionMap(List<Atom> body, int conIdx, HashMap<String, HashSet> varToJoinVarIdx) {
    }

    /**
     * Initialize the varToJoinVarIdx hashmap
     * by adding all the variables into the hashmap, which mapping to an empty list
     */
    public static HashMap<String, HashSet> initVarToJoinVarIdx(HashMap<String, Integer> definedVariables) {
        HashMap<String, HashSet> varToJoinVarIdx = new HashMap<>();
        // Iterate through all the keys in definedVariables
        for (String key : definedVariables.keySet()) {
            // Add an empty ArrayList<Integer> for each key
            varToJoinVarIdx.put(key, new HashSet<>());
        }
        return varToJoinVarIdx;
    }


    /**
     * Update body if there is hidden condition in the relational atom (e.g. R(x, 5))
     * by replacing new variables to Constants (e.g. 5-> y);
     * and adding new condition to the body (e.g. y = 5)
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
                                               HashMap<String, HashSet> preDefVarToJoinedVarMap) {
        RelationalAtom relationalAtom = (RelationalAtom) atom;
        Boolean added = false;
        List<Term> terms = relationalAtom.getTerms();
        for (int j = 0; j < terms.size(); j++) {
            Term term = terms.get(j);
            //either constant or variable that has been seen before
//            if (term instanceof Constant) {
            if ((term instanceof Constant) || preDefVars.get(term.toString()) == 0) {
                // create a new var ( unseen in preDefVars)
                String newVar = "";
                do {
                    newVar = ranObj.nextInt() + "";
                } while (preDefVars.get(newVar) != null);
                // add the new var to the preDefVarToJoinedVarMap since we need this for adding join condition later
                if (preDefVars.get(term.toString()) == 0) { //0 == has been seen, which is implies a joint condition
                    preDefVarToJoinedVarMap.get(term.toString()).add(i); // TODO:check missing key
                }
                preDefVars.put(newVar, 0);
                // update the term in the relational atom e.g. (x, 5) ---> (x, y) or (x,z) ---> (x,z')
                relationalAtom.setTerm(j, new Variable(newVar));
                Term copiedTerm = copyTerm(term);
                // add the new condition to the body e.g. R(x, 5) ---> R(x, y), y = 5
                body.add(new ComparisonAtom(new Variable(newVar), copiedTerm, ComparisonOperator.EQ));
                added = true;          // update the changed status
            }
        }
        if (added) {
            body.set(i, relationalAtom); // update in the body e.g. R(x, 5) ---> R(x, y)
        }
        return added;
    }


}

