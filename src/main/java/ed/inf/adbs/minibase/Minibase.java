package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.util.List;

import static ed.inf.adbs.minibase.base.QueryPlanner.findComparisonAtoms;

/**
 * In-memory database system
 */
public class Minibase {

    public static void main(String[] args) {
        try {
            if (args.length != 3) {
                System.err.println("Usage: Minibase database_dir input_file output_file");
                return;
            }
            String databaseDir = args[0];
            String inputFile = args[1];
            String outputFile = args[2];
            evaluateCQ(databaseDir, inputFile, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) throws Exception {
        Catalog catalog = Catalog.getInstance(databaseDir); // setup singleton catalog
        Interpreter interpreter = new Interpreter(inputFile, outputFile);
        interpreter.dump(); // dump the result to output file
    }

}
