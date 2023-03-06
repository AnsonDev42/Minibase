package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.Catalog;
import org.junit.Test;

import java.util.Arrays;

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
    public void testScanOperator() {

    }
}

