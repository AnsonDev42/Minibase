package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static ed.inf.adbs.minibase.CQMinimizer.isSameTerm;
import static ed.inf.adbs.minibase.base.QueryPlanner.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;

public class QueryPlannerTest {


    @Test
    public void testFindComparisonAtomIdx() {
        Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), z < w");
        // Query query = QueryParser.parse("Q(SUM(x * 2 * x)) :- R(x, 'z'), S(4, z, w), 4 < 'test string' ");
        List<Atom> body = query.getBody();
        assertEquals(2, findComparisonAtoms(body));

        Query query1 = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w)");
        List<Atom> body1 = query1.getBody();
        assertEquals(0, findComparisonAtoms(body1));
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

//    new tests

    @Test
    public void testCreateTwoConMap() {
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), S(y, 5, w), z < w");
        List<Atom> body = query.getBody();
        int conIdx = findAndUpdateCondition(body);
        assertEquals(2, conIdx);
        System.out.println(body.toString());
//        should be R(x,y,z), S(newvar1, newvar2, w), z < w,y = newvar1, newvar2 = 5
        Variable r2 = (Variable) ((RelationalAtom) body.get(0)).getTerms().get(1);
        Variable s1 = (Variable) ((RelationalAtom) body.get(1)).getTerms().get(0);
        //check hidden select condition s2 is not 5 anymore
        assertNotEquals("5", ((RelationalAtom) body.get(1)).getTerms().get(1).toString());
        // check extracted condition is 5
        assertEquals("5", ((ComparisonAtom) body.get(4)).getTerm2().toString());
        // check hidden join condition are not the same anymore
        assertNotEquals(r2.getName(), s1.getName());
        // check extracted condition are the same
        Variable s2 = (Variable) ((RelationalAtom) body.get(1)).getTerms().get(1);
        assertEquals(((ComparisonAtom) body.get(4)).getTerm1().toString(), s2.getName());

        System.out.println("---------------------");
        ArrayList conMaps = createTwoConMap(body, conIdx);
        HashMap selMap = (HashMap) conMaps.get(0);
        HashMap joinMap = (HashMap) conMaps.get(1);
        System.out.println("selection map " + selMap.toString());
        System.out.println("join map " + joinMap.toString());
        assertEquals("[]", selMap.get(0).toString());
    }

    @Test
    public void testCreateTwoMaps() {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), S(a, b, c), T(xx, cc), x = xx, y = a, b = 5");
        System.out.println("testing query" + query.toString());
        List<Atom> body = query.getBody();
//        removeCondition(body); //already removed at first place
        assertEquals("testing body", "[R(x, y, z), S(a, b, c), T(xx, cc), x = xx, y = a, b = 5]", body.toString());
        int conIdx = findAndUpdateCondition(body);
        assertEquals(3, conIdx);
        ArrayList<HashMap<Integer, HashSet<ComparisonAtom>>> conMaps = createTwoConMap(body, conIdx);
        HashMap<Integer, HashSet<ComparisonAtom>> selMap = conMaps.get(0);
        HashMap<Integer, HashSet<ComparisonAtom>> joinMap = conMaps.get(1);
        System.out.println("selection map " + selMap.toString());
        System.out.println("join map " + joinMap.toString());
        assertEquals("[b = 5]", selMap.get(1).toString());
        assertEquals("[y = a, x = xx]", joinMap.get(0).toString());
        assertEquals("[y = a]", joinMap.get(1).toString());
    }


    @Test
    public void testCreateDeepLeftJoinTree() throws IOException { // depth 2
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), S(a, b, c), T(xx, cc), x = xx, y = a, b = 'rhcp'");
        System.out.println("testing query" + query.toString());
        List<Atom> body = query.getBody();
//        removeCondition(body); already removed at first place
        assertEquals("testing body", "[R(x, y, z), S(a, b, c), T(xx, cc), x = xx, y = a, b = 'rhcp']", body.toString());
        int conIdx = findAndUpdateCondition(body);
        assertEquals(3, conIdx);
        HashMap<String, Integer> jointTupleVarToIdx = createJointTupleVarToIdx(body, conIdx);
        Operator result = createDeepLeftJoinTree(body, conIdx, jointTupleVarToIdx);
        assertTrue(result instanceof JoinOperator);
        assertTrue("left child is not join", ((JoinOperator) result).getLeftChild() instanceof JoinOperator);
        assertFalse("right child CANT BE JoinOperator", ((JoinOperator) result).getRightChild() instanceof JoinOperator);
        assertTrue("right child is ScanOperator", ((JoinOperator) result).getRightChild() instanceof ScanOperator);
        assertTrue("left child's right child is selection", ((JoinOperator) ((JoinOperator) result).getLeftChild()).getRightChild() instanceof SelectOperator);
        //need to handle null output
    }

    @Test
    public void testQuery1() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z)");
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query1.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath);
    }

    @Test
    public void testQuery2() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, sname, z) :- S(x, sname, z)");
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query2.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath);
    }

    @Test
    public void testQuery3() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y) :- T(x, y)");
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query3.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath);
    }

    @Test
    public void testQuery4() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, sname, z) :- R(x, y, z), S(sname, ts, y)");
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query4.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath);
    }

    @Test
    public void testQuery5() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, sname, z) :- R(x, y, z), S(x, sname, y), T(x, z)");
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query5.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath);
    }

    @Test
    public void testScanOperatorDUMP() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        ScanOperator scanOperator = new ScanOperator("R", requiredColumns);
        Tuple tuple = scanOperator.getNextTuple();
        String s = "[1, 9, 'adbs']";
        assertEquals(s, tuple.toString());
        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
        scanOperator.dump("data/evaluation/test_db/test_output/query1.csv");
        Tuple tuple3 = scanOperator.getNextTuple();
        assertNull(tuple3);
    }

    @Test
    public void testAggregateExtraction() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q( SUM(x) ) :- R(x, y, z)");
//        Query query = QueryParser.parse("Q(x) :- R(x, y, z)");
        System.out.println("testing query head:" + query.getHead().toString());
        System.out.println("testing query head term:" + (query.getHead().getSumAggregate() instanceof Term));
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query_sum.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath); // 36
    }

    @Test
    public void testAggregateSumGROUP() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q( SUM(x) ) :- R(x, y, z)");
        // Set up the test data and expected results
        ScanOperator child = new ScanOperator("R", requiredColumns);
        HashMap<String, Integer> varToIndexMap = new HashMap<>();
        varToIndexMap.put("x", 0);
        varToIndexMap.put("y", 1);
        varToIndexMap.put("z", 2);

        SumOperator sumOperator = new SumOperator(child, query.getHead(), varToIndexMap);
        sumOperator.computeSum();
        int expectedResult = 36;
        System.out.println("sumOperator.getGroups().values()" + sumOperator.getGroups().values());
        assertEquals(expectedResult, sumOperator.getGroups().values().iterator().next());
    }


    @Test
    public void testAggregate1() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q( x, SUM(y * y) ) :- R(x, y, z)");
        System.out.println("testing query head:" + query.getHead().getSumAggregate().toString());
//        Query query = QueryParser.parse("Q(x) :- R(x, y, z)");
        System.out.println("testing query head:" + query.getHead().toString());
        System.out.println("testing query head term:" + (query.getHead().getSumAggregate() instanceof Term));
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query_sum.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath); // 36
    }

    @Test
    public void testAggregate2() throws Exception {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q( x, SUM(x) ) :- R(x, y, z)");
        System.out.println("testing query head:" + query.getHead().getSumAggregate().toString());
//        Query query = QueryParser.parse("Q(x) :- R(x, y, z)");
        System.out.println("testing query head:" + query.getHead().toString());
        System.out.println("testing query head term:" + (query.getHead().getSumAggregate() instanceof Term));
        Operator root = (new QueryPlanner(query)).getOperator();
        String outputFilePath = "data/evaluation/test_db/test_output/query_sum.csv";
        File file = new File(outputFilePath);
        root.dump(outputFilePath); // 36
    }

}