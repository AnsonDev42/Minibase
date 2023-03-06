package ed.inf.adbs.minibase.base;

import javax.management.relation.Relation;
import java.io.BufferedReader;
import java.util.ArrayList;

public class ScanOperator {
    private static String relationName;
    private static BufferedReader reader;
    private static int currentLocation;

    /**
     * Constructor for ScanOperator
     *
     * @param relationName the String name of the relation
     */
    public ScanOperator(String relationName) {
        ScanOperator.relationName = relationName;
        currentLocation = 0;
        reader = null;

    }

    public static ArrayList<String> getNextTuple() {
        ArrayList<String> tuple = new ArrayList<>();
        String filepath = Catalog.getInstance(null).getDataFileName(relationName);
//        load the currentLocation line of the file instead of the whole file

    }

    public void reset() {
        currentLocation = 0;
    }


}
