package ed.inf.adbs.minibase.base;

import java.util.ArrayList;
import java.util.List;

import static ed.inf.adbs.minibase.Minibase.findComparisonAtoms;

public class QueryPlanner {
    public static Operator buildQueryPlan(Query query) throws Exception {
        // Step 1: create list of variables in the head
        Head head = query.getHead();
        List<Variable> variables = head.getVariables();
        if (variables.isEmpty()) {
            throw new Exception("Query should have at least one variable in the head");
        }

        // Step 2: create list of tables in the body
        List<Atom> body = query.getBody();
        ArrayList<Operator> tableScans = new ArrayList<>();
        int index = findComparisonAtoms(body);
        if (index != -1) {
            List condition = body.subList(index, body.size());
            for (Atom atom : body) {
                if (atom instanceof RelationalAtom) {
                    tableScans.add(new SelectOperator((RelationalAtom) atom, condition));
                }
            }
        } else {
            for (Atom atom : body) {
                if (atom instanceof RelationalAtom) {
                    String tableName = ((RelationalAtom) atom).getName();
                    tableScans.add(new ScanOperator(tableName));
                }
            }
        }
        // Step 4: create project operator
//        TODO

        Operator operator = null;

        return operator;
    }
}

