package ed.inf.adbs.minibase;

import org.junit.Test;

import static ed.inf.adbs.minibase.Minibase.evaluateCQ;

public class Task3Test {

    @Test
    public void testIntepreterALL() throws Exception {
        for (int i = 1; i <= 9; i++) {
            String databaseDir = "data/evaluation/db";
            String inputFile = "data/evaluation/input/query" + i + ".txt";
            String outputFile = "data/evaluation/test_db/test_output/query" + i + ".csv";
            evaluateCQ(databaseDir, inputFile, outputFile);
        }
    }

    @Test
    public void testSingleQuery1() throws Exception {
        int i = 8;
        String databaseDir = "data/evaluation/db";
        String inputFile = "data/evaluation/input/query" + i + ".txt";
        String outputFile = "data/evaluation/test_db/test_output/query" + i + ".csv";
        evaluateCQ(databaseDir, inputFile, outputFile);
    }
}
