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

        // handle hidden condition
        int conIdx = findAndUpdateCondition(body);
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
        HashSet<String> definedVariables = getDefinedVariables(body);
        Random ranObj = new Random();

        for (int i = 0; i < body.size(); i++) {
            Atom atom = body.get(i);
            if (atom instanceof RelationalAtom) {
                addVarCondition(body, ranObj, definedVariables, i, atom);
            }
            if (atom instanceof ComparisonAtom) {
                break;
            }
        }
        // find the first condition in the modified body
        for (int i = 0; i < body.size(); i++) {
            if (body.get(i) instanceof ComparisonAtom) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Update body if there is hidden condition in the relational atom (e.g. R(x, 5))
     * by replacing new variables to Constants (e.g. 5-> y);
     * and adding new condition to the body (e.g. y = 5)
     * e.g. Q() :- R(x, 5)    --->  Q() :- R(x, y), y = 5      and return true
     *
     * @param body             query body to be modified
     * @param ranObj           random object
     * @param definedVariables all defined variables by user
     * @param i                index of the relational atom in the body
     * @param atom             the relational atom to be modified
     * @return true if the relational atom is modified, false otherwise
     */
    public static Boolean addVarCondition(List<Atom> body, Random ranObj, HashSet<String> definedVariables, int i, Atom atom) {
        RelationalAtom relationalAtom = (RelationalAtom) atom;
        Boolean added = false;
        List<Term> terms = relationalAtom.getTerms();
        for (int j = 0; j < terms.size(); j++) {
            Term term = terms.get(j);
            if (term instanceof Constant) {
                // create a new var ( unseen in definedVariables)
                String newVar = "";
                do {
                    newVar = ranObj.nextInt() + "";
                } while (definedVariables.contains(newVar));
                definedVariables.add(newVar);
                // create a new constant to put into the added condition
                Constant newConstant = term instanceof StringConstant
                        ? new StringConstant(((StringConstant) term).getValue())
                        : new IntegerConstant(((IntegerConstant) term).getValue());
                // update the term in the relational atom e.g. (x, 5) ---> (x, y)
                relationalAtom.setTerm(j, new Variable(newVar));
                // add the new condition to the body e.g. R(x, 5) :- R(x, 5), y = 5
                body.add(new ComparisonAtom(new Variable(newVar), newConstant, ComparisonOperator.EQ));
                added = true;          // update the changed status

            }
        }
        body.set(i, relationalAtom); // update in the body e.g. R(x, 5) ---> R(x, y)
        return added;
    }
}

