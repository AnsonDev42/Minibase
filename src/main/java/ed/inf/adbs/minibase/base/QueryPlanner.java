package ed.inf.adbs.minibase.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        int index = findComparisonAtoms(body);
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


    /**
     * Modify the hidden condition from the relational atom by replacing the constants with the variables
     * and add the hidden condition to the body of the query in the end
     *
     * @param body query body to be modified
     * @return body modified with the hidden condition
     */
    public List<Term> showHiddenCondition(List<Atom> body) {
        int i = 0;
        for (Atom atom : body) {
            if (atom instanceof RelationalAtom) {
                for (Term term : ((RelationalAtom) atom).getTerms()) {
                    if (term instanceof Constant) {
//                        ...
                    }
                }
            }
            i++;
        }
        return null;
    }
}

