package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static ed.inf.adbs.minibase.CQMinimizer.isSameTerm;
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
        Catalog catalog = Catalog.getInstance("data/evaluation/db");
        catalog.initialize();
        assertEquals("data/evaluation/db/files/R.csv", catalog.getDataFileName("R"));
        String[] R_schema = new String[]{"int", "int", "string"};
        assertEquals(R_schema, catalog.getSchema("R"));
    }

    @Test
    public void testScanOperator() throws IOException {
        Catalog catalog = Catalog.getInstance("data/evaluation/db");
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
        Catalog catalog = Catalog.getInstance("data/evaluation/db");
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
        Catalog catalog = Catalog.getInstance("data/evaluation/db");
        catalog.initialize();
        ScanOperator scanOperator = new ScanOperator("R");
        Tuple tuple = scanOperator.getNextTuple();
        String s = "[1, 9, 'adbs']";
        assertEquals(s, tuple.toString());
        assertTrue(isSameTerm((Term) tuple.getField(0), new IntegerConstant(1)));
        Tuple tuple2 = scanOperator.getNextTuple();
        String s2 = "[2, 7, 'anlp']";
        assertEquals(s2, tuple2.toString());
        scanOperator.dump();
        Tuple tuple3 = scanOperator.getNextTuple();
        assertNull(tuple3);
    }
}

