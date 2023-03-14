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
    public void testFindComparisonAtomIdx() {
        Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), z < w");
        // Query query = QueryParser.parse("Q(SUM(x * 2 * x)) :- R(x, 'z'), S(4, z, w), 4 < 'test string' ");
        List<Atom> body = query.getBody();
        assertEquals(2, findComparisonAtoms(body));

        Query query1 = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w)");
        List<Atom> body1 = query1.getBody();
        assertEquals(-1, findComparisonAtoms(body1));

    }


    @Test
    public void ComparingTwoConstantTerms() {
//        1. Test for comparing two StringConstants that are equal using the "EQ" operator:
        Term left = new StringConstant("hello");
        Term right = new StringConstant("hello");
//        create a new ComparisonOperator with the operator EQ
        boolean result = ComparisonOperator.EQ.compare(left, right);
        assertTrue(result);
//        2. Test for comparing two IntegerConstants that are not equal using the "NEQ" operator:
        left = new IntegerConstant(10);
        right = new IntegerConstant(5);
        result = ComparisonOperator.NEQ.compare(left, right);
        assertTrue(result);

//        3. Test for comparing a Variable and a StringConstant which should throw an IllegalArgumentException:
        Term left1 = new Variable("x");
        Term right1 = new StringConstant("hello");
        assertThrows(IllegalArgumentException.class, () -> {
            ComparisonOperator.EQ.compare(left1, right1);
        });

//        4. Test for comparing a StringConstant and an IntegerConstant which should throw an IllegalArgumentException:
//        should it throw an IllegalArgumentException?
        Term left2 = new StringConstant("hello");
        Term right2 = new IntegerConstant(5);
        assertThrows(IllegalArgumentException.class, () -> {
            ComparisonOperator.EQ.compare(left2, right2);
        });
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
    public void testSelectOperatorSimple() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), y > 3");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        assertEquals("[1, 9, 'adbs']", selectOperator.getNextTuple().toString());
        assertEquals("[2, 7, 'anlp']", selectOperator.getNextTuple().toString());
//        skipped some tuples
        assertEquals("[8, 9, 'rl']", selectOperator.getNextTuple().toString());
    }

    @Test
    public void testSelectOperator2conditions() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), y > 3,x=8");
        List<Atom> body = query.getBody();
        int index = findAndUpdateCondition(body);
        assertEquals(1, index);
        System.out.println(index);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        assertEquals("[8, 9, 'rl']", selectOperator.getNextTuple().toString());
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
    public void testSelectOperatorImpossibleVar() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), c > 3");
        List<Atom> body = query.getBody();
        int index = findAndUpdateCondition(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
//        assertNull(selectOperator.getNextTuple()); // should be null
        assertThrows(RuntimeException.class, () -> {
            selectOperator.getNextTuple();
        });
//        assertEquals("[1, 9, 'adbs']", selectOperator.getNextTuple().toString());
    }

    @Test
    public void testSelectOperatorHiddenCondition() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y) :- S(x, y,4)");
        List<Atom> body = query.getBody();
        assertEquals("4", ((RelationalAtom) body.get(0)).getTerms().get(2).toString()); // check loc in 4

        int bodylength_before = body.size();
        int index = findAndUpdateCondition(body);
        assertEquals(1, index);  // check the returned condition index
        assertEquals(bodylength_before + 1, body.size()); // check body is updated in length
        assertSame(((ComparisonAtom) body.get(1)).getOp(), ComparisonOperator.EQ); // check body is updated in content
        assertNotEquals("4", ((RelationalAtom) body.get(0)).getTerms().get(2).toString()); // check loc in 4 is removed
        assertEquals("4", ((ComparisonAtom) body.get(1)).getTerm2().toString()); // z=4
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        assertEquals("[5, 'bowie', 4]", selectOperator.getNextTuple().toString());
    }


    @Test
    public void testSelectOperatorHiddenConditionMultiple() throws FileNotFoundException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x) :- S(5, 'bowie', x)");
        List<Atom> body = query.getBody();
        int bodylength_before = body.size();
        int index = findAndUpdateCondition(body);
        assertEquals(1, index);  // check the returned condition index
        assertEquals(bodylength_before + 2, body.size()); // check body is updated in length

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
    public void testTupleJoin() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), y > 3");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
//        assertEquals("[1, 9, 'adbs']", selectOperator.getNextTuple().toString());
//        assertEquals("[2, 7, 'anlp']", selectOperator.getNextTuple().toString());
        Tuple jointTuple = Tuple.join(selectOperator.getNextTuple(), selectOperator.getNextTuple());
        assertEquals("[1, 9, 'adbs', 2, 7, 'anlp']", jointTuple.toString());

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

