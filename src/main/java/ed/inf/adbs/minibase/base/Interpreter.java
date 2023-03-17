package ed.inf.adbs.minibase.base;

import ed.inf.adbs.minibase.parser.QueryParser;

import java.nio.file.Paths;

public class Interpreter {
    private final String query_file;
    private static Query query;

    private static QueryPlanner queryPlanner;

    public Interpreter(String queryFilePath) throws Exception {
        this.query_file = queryFilePath;
        query = QueryParser.parse(Paths.get(query_file));
        queryPlanner = new QueryPlanner(query);
        Operator rootOperator = queryPlanner.getOperator();
        rootOperator.dump();

    }


}
