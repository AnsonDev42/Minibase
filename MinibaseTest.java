package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static ed.inf.adbs.minibase.CQMinimizer.isSameTerm;
import static ed.inf.adbs.minibase.Minibase.evaluateCQ;
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

//    @Test
//    public void testScanOperator() throws IOException {
//        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
//        catalog.initialize();
//        ScanOperator scanOperator = new ScanOperator("R", requiredColumns);
//        Tuple tuple = scanOperator.getNextTuple();
//
//        String s = "[1, 9, 'adbs']";
//        assertEquals(s, tuple.toString());
//        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
//        Tuple tuple2 = scanOperator.getNextTuple();
//        String s2 = "[2, 7, 'anlp']";
//        assertEquals(s2, tuple2.toString());
//    }
//
//    @Test
//    public void testScanOperatorRESET() throws IOException {
//        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
//        catalog.initialize();
//        ScanOperator scanOperator = new ScanOperator("R", requiredColumns);
//        Tuple tuple = scanOperator.getNextTuple();
//        String s = "[1, 9, 'adbs']";
//        assertEquals(s, tuple.toString());
//        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
//        Tuple tuple2 = scanOperator.getNextTuple();
//        String s2 = "[2, 7, 'anlp']";
//        assertEquals(s2, tuple2.toString());
//        scanOperator.reset();
//        Tuple tuple3 = scanOperator.getNextTuple();
//        assertEquals(s, tuple3.toString());
//    }


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
        Operator plan = (new QueryPlanner(query)).getOperator();
        assertEquals("[1, 9, 'adbs']", plan.getNextTuple().toString());

        // TEST SELECT => scan + select
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(x, z, y) :- R(x, z, y)")).get("scan")
//                instanceof ScanOperator);
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(x, z) :- R(x, z, 'anlp')")).get("select") //2, 7, 'anlp'
//                instanceof SelectOperator);
//        assertEquals("[2, 7, 'anlp']", buildQueryPlan(QueryParser.parse("Q(x, z) :- R(x, z, 'anlp')")).get("select").
//                getNextTuple().toString());
//
//
//        // TEST PROJECT => scan + project
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z)")).get("scan")
//                instanceof ScanOperator);
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z)")).get("project")
//                instanceof ProjectOperator);
//        assertEquals("[9, 1, 'adbs']", buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z)")).get("project").
//                getNextTuple().toString());
//
//
//        // TEST SELECT + PROJECT => scan + select + project
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z), y > 3")).get("scan")
//                instanceof ScanOperator);
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z), y > 3")).get("select")
//                instanceof SelectOperator);
//        assertTrue(buildQueryPlan(QueryParser.parse("Q(y, x,z) :- R(x, y,z), y > 3")).get("project")
//                instanceof ProjectOperator);
//        assertEquals("['adbs', 1]", buildQueryPlan(QueryParser.parse("Q(z,x) :- R(x, y,z), y > 3")).get("project").
//                getNextTuple().toString());
//
//        assertEquals("['ids']", buildQueryPlan(QueryParser.parse("Q(z) :- R(4, y,z), y =2")).get("project").
//                getNextTuple().toString());
//
//
////        check when the query contains table info that does not match to the catalog schema
//        assertThrows(Exception.class, () -> {
//            buildQueryPlan(QueryParser.parse("Q(x, y) :- R(x) "));
//        });
    }

//    @Test broken
//    public void testProjectOperatorWithoutPlanner() throws IOException {
//        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
//        Query query = QueryParser.parse("Q(x, z) :- R(x, 9, z)");
//        List<Atom> body = query.getBody();
//        int index = findAndUpdateCondition(body);
//        // get the terms starting from the index
//        List condition = body.subList(index, body.size());
//        assertEquals(1, index);
//        assertEquals(ComparisonOperator.EQ, ((ComparisonAtom) body.get(index)).getOp());
//        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
//        HashMap<String, Integer> map =createJointTupleVarToIdx(body, query.getHead().getVariables());
//        ProjectOperator projectOperator = new ProjectOperator(selectOperator, query.getHead().getVariables(),
//        assertEquals("[1, 'adbs']", projectOperator.getNextTuple().toString()); //        8, 9, 'rl'
//        assertEquals("[8, 'rl']", projectOperator.getNextTuple().toString()); //        8, 9, 'rl'
//        assertEquals("[8, 'ppls']", projectOperator.getNextTuple().toString()); //     8, 9, 'ppls'
//
//        assertNull(projectOperator.getNextTuple());
//    }

    @Test
    public void testProjectSeenTuple() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x) :- T(x,z), z>0");
        Operator projectOperator = (new QueryPlanner(query)).getOperator();
        assertTrue(projectOperator instanceof ProjectOperator);
        assertTrue(ProjectOperator.getChildOperator() instanceof SelectOperator);

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
//
//    @Test
//    public void testSelectionDump() throws Exception {
//        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
//        Query query = QueryParser.parse("Q(x) :- T(x,z), z=3");
//        Operator operator = QueryPlanner.buildQueryPlan(query);
//        operator.dump();
//    }


    @Test
    public void testJoinOperatorSimple() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(a, b, c), S(x, y, z), a=z");
        List<Atom> body = query.getBody();
        ScanOperator leftChild = new ScanOperator(((RelationalAtom) body.get(0)).getName(), requiredColumns);
        ScanOperator rightChild = new ScanOperator(((RelationalAtom) body.get(1)).getName(), requiredColumns);
        List<ComparisonAtom> joinConditions = Collections.singletonList((ComparisonAtom) body.get(2));
        HashMap<String, Integer> jointTupleVarToIdx = createJointTupleVarToIdx(body, 2);

        JoinOperator jOp = new JoinOperator(leftChild, rightChild, jointTupleVarToIdx, (RelationalAtom) body.get(1), joinConditions);
        assertEquals("[2, 7, 'anlp', 2, 'anka', 2]", jOp.getNextTuple().toString());

    }


    @Test
    public void testIntepreter6() throws Exception {
        String databaseDir = "data/evaluation/db";
        String inputFile = "data/evaluation/input/query6_debug.txt";
        String outputFile = "data/evaluation/test_db/test_output/query6.csv";

        Catalog catalog = Catalog.getInstance(databaseDir);
        Operator op = buildQueryPlan(QueryParser.parse("Q(y, r) :- R(x, y, z), S(x, w, t), T(x, r), x = 1"));
        op.dump(outputFile); // actually join now
        assertTrue(op instanceof ProjectOperator);
        assertTrue(ProjectOperator.getChildOperator() instanceof JoinOperator);
        JoinOperator joinOperator = (JoinOperator) ProjectOperator.getChildOperator();
        joinOperator.dump(outputFile);
        assertTrue(joinOperator.getRightChild() instanceof ScanOperator);
        ScanOperator r1 = (ScanOperator) joinOperator.getRightChild();
        assertEquals("T", r1.getRelationName());
        assertTrue(joinOperator.getLeftChild() instanceof JoinOperator);
        JoinOperator l1 = (JoinOperator) joinOperator.getLeftChild();
        assertTrue(l1.getRightChild() instanceof ScanOperator);
        ScanOperator l1_r2 = (ScanOperator) l1.getRightChild();
        assertEquals("S", l1_r2.getRelationName());
        assertTrue(l1.getLeftChild() instanceof SelectOperator);
        SelectOperator l1_l2 = (SelectOperator) l1.getLeftChild();

//        l1.dump(outputFile);


//        assertEquals("[1, 1]", scanOperator.getNextTuple().toString());
//        scanOperator.dump(outputFile);
//        joinOperator.dump(outputFile);
//        Operator root = op;
//        evaluateCQ(databaseDir, inputFile, outputFile);
    }


    @Test
    public void voidtestIntepreter61() throws Exception {
        String databaseDir = "data/evaluation/db";
        String inputFile = "data/evaluation/input/query6_debug.txt";
        String outputFile = "data/evaluation/test_db/test_output/query6.csv";

        Catalog catalog = Catalog.getInstance(databaseDir);
        Operator op = buildQueryPlan(QueryParser.parse("Q(y, w) :-S(xx, w, t), R(x, y, z), x = 1, x = xx"));
        //  check structure of tree
        assertTrue(op instanceof ProjectOperator);
        assertTrue(ProjectOperator.getChildOperator() instanceof JoinOperator);
        JoinOperator joinOperator = (JoinOperator) ProjectOperator.getChildOperator();
//        assertTrue(joinOperator.getRightChild() instanceof ScanOperator);
        joinOperator.dump(outputFile);
    }

    @Test
    public void testIntepreterALL() throws Exception {
        for (int i = 1; i <= 9; i++) {
            String databaseDir = "data/evaluation/db";
            String inputFile = "data/evaluation/input/query" + i + ".txt";
            String outputFile = "data/evaluation/test_db/test_output/query" + i + ".csv";
            evaluateCQ(databaseDir, inputFile, outputFile);
        }
    }

//    @Test
//    public void testIntepreter3() throws Exception {
//        String databaseDir = "data/evaluation/db";
//        String inputFile = "data/evaluation/input/query2.txt";
//        String outputFile = "data/evaluation/test_db/test_output/query2.csv";
//        evaluateCQ(databaseDir, inputFile, outputFile);
//    }
}

