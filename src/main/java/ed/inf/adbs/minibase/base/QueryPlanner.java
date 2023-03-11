package ed.inf.adbs.minibase.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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

    public static HashMap<String, Operator> buildQueryPlan(Query query) throws Exception {

        HashMap<String, Operator> results = new HashMap<>();

        Head head = query.getHead();
        List<Atom> body = query.getBody();
        if (body.size() == 0) {
            throw new Exception("Query body is empty");
        }

        List<Variable> variables = head.getVariables();
//        create the scan operator
        Atom relationalAtom = body.get(0);
        if (relationalAtom instanceof RelationalAtom) {
            checkQueryMatchSchema((RelationalAtom) relationalAtom);
            String tableName = ((RelationalAtom) relationalAtom).getName();
            results.put("scan", new ScanOperator(tableName));
        } else
            throw new Exception("The first atom in the query body is not a relational atom");

        //        check if needed to project by checking the number
        //        and checking order of variables in the head is the same as the schema
        Boolean needProject = false;
        if (variables.size() != ((RelationalAtom) relationalAtom).getTerms().size()) {
            needProject = true;
        }
        if (!needProject) { // continue checking
            for (int i = 0; i < variables.size(); i++) {
                if (!variables.get(i).getName().equals(((RelationalAtom) relationalAtom).getTerms().get(i).toString())) {
                    needProject = true;
                    break;
                }
            }
        }


//        check if needed to select
        int index = findAndUpdateCondition(body);
        if (index != -1) {
            List condition = body.subList(index, body.size());
            checkQueryMatchSchema((RelationalAtom) relationalAtom);
            results.put("select", new SelectOperator((RelationalAtom) relationalAtom, condition));
        }

        results.put("project", new ProjectOperator(results.get("scan"), variables));
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


    public static HashSet<String> getDefinedVariables(List<Atom> body) {
        HashSet<String> definedVariables = new HashSet<>();
        // find all variable names in case of clash
        for (Atom atom : body) {
            if (atom instanceof RelationalAtom) {
                for (Term term : ((RelationalAtom) atom).getTerms()) {
                    if (term instanceof Variable) {
                        definedVariables.add(term.toString());
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
    public static int findAndUpdateCondition(List<Atom> body) {
        int conditionIndex = -1;
        HashSet<String> definedVariables = getDefinedVariables(body);
        Random ranObj = new Random();

        for (int i = 0; i < body.size(); i++) {
            Atom atom = body.get(i);
            if (atom instanceof RelationalAtom) {
                for (int j = 0; j < ((RelationalAtom) atom).getTerms().size(); j++) {
                    if (((RelationalAtom) atom).getTerms().get(j) instanceof Constant) {
                        // create a new var ( unseen in definedVariables)
                        String newVar = ranObj.nextInt() + "";
                        while (definedVariables.contains(newVar)) {
                            newVar = ranObj.nextInt() + "";
                        }
                        definedVariables.add(newVar);
                        // create a new constant to put into the added condition
                        Constant newConstant = ((RelationalAtom) atom).getTerms().get(j) instanceof StringConstant ?
                                new StringConstant(((StringConstant) ((RelationalAtom) atom).getTerms().get(j)).getValue()) :
                                new IntegerConstant(((IntegerConstant) ((RelationalAtom) atom).getTerms().get(j)).getValue());
                        // update the term in the relational atom e.g. R(x, 5) ---> R(x, y)
                        ((RelationalAtom) atom).setTerm(j, new Variable(newVar));
                        // add the new condition to the body e.g. R(x, 5) :- R(x, y), y = 5
                        body.add(new ComparisonAtom(new Variable(newVar), newConstant, ComparisonOperator.EQ));
                        // update the condition index
                        if (conditionIndex == -1) {
                            conditionIndex = body.size() - 1;
                        } // since body does not change in the loop
                    }
                }
                // update the relational atom in the body
                body.set(i, atom);
            }
            if (atom instanceof ComparisonAtom) {
                if (conditionIndex == -1) {
                    conditionIndex = i;
                }
            }
        }
        return conditionIndex;
    }
}

