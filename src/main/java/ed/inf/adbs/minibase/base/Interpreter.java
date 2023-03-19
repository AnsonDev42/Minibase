package ed.inf.adbs.minibase.base;

import ed.inf.adbs.minibase.parser.QueryParser;

import java.nio.file.Paths;

public class Interpreter {
    private final String query_file;
    private final String outputFile;
    private static Query query;
    private static QueryPlanner queryPlanner;
    private static Operator rootOperator;

    public Interpreter(String inputFile, String outputFile) throws Exception {
        this.query_file = inputFile;
        this.outputFile = outputFile;
        query = QueryParser.parse(Paths.get(query_file));
        queryPlanner = new QueryPlanner(query);
        Operator rootOperator = queryPlanner.getOperator();
        rootOperator.dump(outputFile);
    }

    public void dump() {
        try {
            rootOperator.dump(outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
