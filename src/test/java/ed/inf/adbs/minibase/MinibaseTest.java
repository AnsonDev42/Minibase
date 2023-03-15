package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static ed.inf.adbs.minibase.CQMinimizer.isSameTerm;
import static ed.inf.adbs.minibase.base.QueryPlanner.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThrows;

/**
 * Unit test for Minibase.
 */

public class MinibaseTest {

    /**
     * Rigorous Test :-)
     */

    @Test
    public void testTerm() {
        IntegerConstant term = new IntegerConstant(4);
        HashMap<Term, Integer> map = new HashMap<Term, Integer>();
        map.put(term, 1);
        IntegerConstant term2 = new IntegerConstant(4);
        assertEquals(term.hashCode(), term2.hashCode());
        assertEquals(1, (int) map.get(term2));

        StringConstant term3 = new StringConstant("adbs");
        Variable term4 = new Variable("adbs");
        assertEquals(term3.hashCode(), term4.hashCode()); // hashed based on the string value, should never be the key
        map.put(term3, 33);
        map.put(term4, 44);
        assertEquals(33, (int) map.get(term3));
        assertEquals(44, (int) map.get(term4));

    }

    @Test
    public void cataglogTest() {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        assertEquals("data/evaluation/test_db/files/R.csv", catalog.getDataFileName("R"));
        String[] R_schema = new String[]{"int", "int", "string"};
        assertEquals(R_schema, catalog.getSchema("R"));
    }

    @Test
    public void testScanOperator() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        ScanOperator scanOperator = new ScanOperator("R");
        Tuple tuple = scanOperator.getNextTuple();

        String s = "[1, 9, 'adbs']";
        assertEquals(s, tuple.toString());
        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
        Tuple tuple2 = scanOperator.getNextTuple();
        String s2 = "[2, 7, 'anlp']";
        assertEquals(s2, tuple2.toString());
    }

    @Test
    public void testScanOperatorRESET() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        ScanOperator scanOperator = new ScanOperator("R");
        Tuple tuple = scanOperator.getNextTuple();
        String s = "[1, 9, 'adbs']";
        assertEquals(s, tuple.toString());
        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
        Tuple tuple2 = scanOperator.getNextTuple();
        String s2 = "[2, 7, 'anlp']";
        assertEquals(s2, tuple2.toString());
        scanOperator.reset();
        Tuple tuple3 = scanOperator.getNextTuple();
        assertEquals(s, tuple3.toString());
    }

    @Test
    public void testScanOperatorDUMP() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        ScanOperator scanOperator = new ScanOperator("R");
        Tuple tuple = scanOperator.getNextTuple();
        String s = "[1, 9, 'adbs']";
        assertEquals(s, tuple.toString());
        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
        scanOperator.dump();
        Tuple tuple3 = scanOperator.getNextTuple();
        assertNull(tuple3);
    }




    @Test
    public void testPassCondition() throws FileNotFoundException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), 1>2");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        List condition = body.subList(index, body.size());
//        assertFalse(new SelectOperator((RelationalAtom) body.get(index - 1),condition).passCondition(null, condition));

        Query query2 = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), 3=3");
        List<Atom> body2 = query2.getBody();
        int index2 = findComparisonAtoms(body2);
        List condition2 = body2.subList(index2, body2.size());
//        assertTrue(new SelectOperator((RelationalAtom) body.get(1), condition).passCondition(null, condition2));


    }


    @Test
    public void testSelectOperatorNoMatch() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), y > 100");
        List<Atom> body = query.getBody();
        int index = findAndUpdateCondition(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        assertNull(selectOperator.getNextTuple());
    }



    @Test
    public void testQueryPlanner() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        // TEST SCAN => scan
        Query query = QueryParser.parse("Q(x, z, y) :- R(x, z, y)");
        HashMap<String, Operator> plan = buildQueryPlan(query);
        assertTrue(plan.get("scan") instanceof ScanOperator);
        assertNotNull(plan.get("scan"));
        assertNull(plan.get("select"));
        assertNull(plan.get("project"));
        assertEquals("[1, 9, 'adbs']", plan.get("scan").getNextTuple().toString());

        // TEST SELECT => scan + select
        assertTrue(buildQueryPlan(QueryParser.parse("Q(x, z, y) :- R(x, z, y)")).get("scan")
                instanceof ScanOperator);
        assertTrue(buildQueryPlan(QueryParser.parse("Q(x, z) :- R(x, z, 'anlp')")).get("select") //2, 7, 'anlp'
                instanceof SelectOperator);
        assertEquals("[2, 7, 'anlp']", buildQueryPlan(QueryParser.parse("Q(x, z) :- R(x, z, 'anlp')")).get("select").
                getNextTuple().toString());


        // TEST PROJECT => scan + project
        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z)")).get("scan")
                instanceof ScanOperator);
        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z)")).get("project")
                instanceof ProjectOperator);
        assertEquals("[9, 1, 'adbs']", buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z)")).get("project").
                getNextTuple().toString());


        // TEST SELECT + PROJECT => scan + select + project
        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z), y > 3")).get("scan")
                instanceof ScanOperator);
        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z), y > 3")).get("select")
                instanceof SelectOperator);
        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z), y > 3")).get("project")
                instanceof ProjectOperator);
        assertEquals("['adbs', 1]", buildQueryPlan(QueryParser.parse("Q(z,x) :- R(x, y,z), y > 3")).get("project").
                getNextTuple().toString());

        assertEquals("['ids']", buildQueryPlan(QueryParser.parse("Q(z) :- R(4, y,z), y =2")).get("project").
                getNextTuple().toString());


//        check when the query contains table info that does not match to the catalog schema
        assertThrows(Exception.class, () -> {
            buildQueryPlan(QueryParser.parse("Q(x, y) :- R(x) "));
        });
    }

    @Test
    public void testProjectOperatorWithoutPlanner() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, z) :- R(x, 9, z)");
        List<Atom> body = query.getBody();
        int index = findAndUpdateCondition(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        assertEquals(1, index);
        assertEquals(ComparisonOperator.EQ, ((ComparisonAtom) body.get(index)).getOp());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        ProjectOperator projectOperator = new ProjectOperator(selectOperator, query.getHead().getVariables(), body.get(0));
        assertEquals("[1, 'adbs']", projectOperator.getNextTuple().toString()); //        8, 9, 'rl'
        assertEquals("[8, 'rl']", projectOperator.getNextTuple().toString()); //        8, 9, 'rl'
        assertEquals("[8, 'ppls']", projectOperator.getNextTuple().toString()); //     8, 9, 'ppls'

        assertNull(projectOperator.getNextTuple());
    }

    @Test
    public void testProjectSeenTuple() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x) :- T(x,z), z>0");
        HashMap map = buildQueryPlan(query);
        ProjectOperator projectOperator = (ProjectOperator) map.get("project");
        assertEquals("[1]", projectOperator.getNextTuple().toString());
        assertEquals("[2]", projectOperator.getNextTuple().toString());
        assertEquals("[4]", projectOperator.getNextTuple().toString());
//        in here, it should skip [1] since it has been seen
        assertEquals("[8]", projectOperator.getNextTuple().toString());
//        1, 1
//        2, 3
//        4, 5
//        1, 3
    }

    @Test
    public void testSelectionDump() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x) :- T(x,z), z=3");
        HashMap map = buildQueryPlan(query);
        SelectOperator selectOperator = (SelectOperator) map.get("select");
        selectOperator.dump();
    }



    @Test
    public void testJoinOperatorSimple() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(a, b, c), S(x, y, z), a=z");
        List<Atom> body = query.getBody();
        ScanOperator leftChild = new ScanOperator(((RelationalAtom) body.get(0)).getName());
        ScanOperator rightChild = new ScanOperator(((RelationalAtom) body.get(1)).getName());
        List<RelationalAtom> variables = Arrays.asList((RelationalAtom) body.get(0), (RelationalAtom) body.get(1));
        List<ComparisonAtom> joinConditions = Collections.singletonList((ComparisonAtom) body.get(2));
        JoinOperator jOp = new JoinOperator(leftChild, rightChild, variables, joinConditions);
        assertEquals("[2, 7, 'anlp', 2, 'anka', 2]", jOp.getNextTuple().toString());

    }

}

