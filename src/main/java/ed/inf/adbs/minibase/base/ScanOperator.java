package ed.inf.adbs.minibase.base;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ScanOperator extends Operator {
    private static String relationName;
    private static BufferedReader reader;
    private static int currentLocation;
    private final String[] fieldType;

    /**
     * Constructor for ScanOperator
     *
     * @param relationName the String name of the relation
     */
    public ScanOperator(String relationName) throws FileNotFoundException {
        ScanOperator.relationName = relationName;
        String filepath = Catalog.getInstance(null).getDataFileName(relationName);
        ScanOperator.reader = new BufferedReader(new FileReader(filepath));
        fieldType = Catalog.getInstance(null).getSchema(relationName);
        currentLocation = 0;

    }

    /**
     * given a string field and a string indicates its type , convert field to a term
     *
     * @param field     the string field to be converted, e.g. "1" or "abc"
     * @param fieldType The type of the field indicated by the schema
     * @return a term object converted from string
     */
    public static Term convertToTerm(String field, String fieldType) {
        if (fieldType.equals("int")) {
            return new IntegerConstant(Integer.parseInt(field.trim()));
        } else if (fieldType.equals("string")) {
//            trim the white space at the beginning and remove the outer single quotes e.g. ' 'abc'' -> 'abc'
            field = field.trim();
            field = field.substring(1, field.length() - 1);
            return new StringConstant(field);
        } else {
            throw new IllegalArgumentException("Invalid field type");
        }
    }

    /**
     * Get the tuple from the current location and set the location to the next(line)
     *
     * @return Tuple from the current line if available ; otherwise return null.
     * @throws IOException
     */
    public Tuple getNextTuple() {
        try {
            String line = ScanOperator.reader.readLine();
            if (line != null) {
//            split the line by comma and remove white space
                String[] data = line.split(",");
                Object[] fields = new Object[data.length];
                for (int i = 0; i < data.length; i++) {
                    fields[i] = convertToTerm(data[i], fieldType[i]);  // fieldType[i] is the type of the ith field
                }
                Tuple currentTuple = new Tuple(fields);
                return currentTuple;
            } else {
                ScanOperator.reader.close();
                return null;
            }
        } catch (IOException e) {
            System.out.println("Error reading file");
            return null;
        }

    }

    /**
     * Reset the current location to the beginning of the file
     */
    public void reset() throws IOException {
        currentLocation = 0;
//        reset the reader to the first line
        try {
            ScanOperator.reader.close();
        } catch (IOException e) {
            System.out.println("Error closing file");
        }
        String filepath = Catalog.getInstance(null).getDataFileName(relationName);
        ScanOperator.reader = new BufferedReader(new FileReader(filepath));

    }

    /**
     * Dump the tuples in the relation to the console
     *
     */
    public void dump() {
        Tuple tuple = getNextTuple();
        while (tuple != null) {
            System.out.println(tuple);
            tuple = getNextTuple();
        }
    }
}
