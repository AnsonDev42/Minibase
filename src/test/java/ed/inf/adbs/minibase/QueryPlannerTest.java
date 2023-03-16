package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
    public void testCreateDeepLeftJoinTree() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), S(a, b, c), T(xx, cc), x = xx, y = a, b = 5");
        System.out.println("testing query" + query.toString());
        List<Atom> body = query.getBody();
        assertEquals("testing body", "[R(x, y, z), S(a, b, c), T(xx, cc), x = xx, y = a, b = 5]", body.toString());
        int conIdx = findAndUpdateCondition(body);
        assertEquals(3, conIdx);
        Operator result = createDeepLeftJoinTree(body, conIdx);
        assertTrue(result instanceof JoinOperator);
        assertTrue("left child is not join", ((JoinOperator) result).getLeftChild() instanceof JoinOperator);
        assertTrue("right child is JoinOperator", ((JoinOperator) result).getRightChild() instanceof JoinOperator);


    }


}