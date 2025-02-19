package ed.inf.adbs.minibase.base;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static ed.inf.adbs.minibase.base.SelectOperator.createTermToIndexMap;

public class ScanOperator extends Operator {
    private final String relationName;
    private final HashSet<String> requiredColumns;
    private BufferedReader reader;
    private final String[] fieldTypes;
    private final String[] fieldNames;
    private static FileReader fileReader;
    private final HashMap<String, Integer> returnedTermToIndexMap;

    /**
     * Constructor for ScanOperator
     *
     * @param relAtom         the relation atom
     * @param requiredColumns the required columns to avoid large tuple
     */
    public ScanOperator(RelationalAtom relAtom, HashSet<String> requiredColumns) throws IOException {
        this.relationName = relAtom.getName();
        this.fieldNames = relAtom.getFieldsName();
        // TODO: optimise the get FileReader by not retrieving from Catalog every time
        fileReader = new FileReader(Catalog.getInstance(null).getDataFileName(relationName));
        this.requiredColumns = requiredColumns;
        this.reader = new BufferedReader(fileReader);
        this.reader.mark(0);
        this.fieldTypes = Catalog.getInstance(null).getSchema(relationName);
        this.returnedTermToIndexMap = createTermToIndexMap(relAtom, requiredColumns);
    }

    /**
     * returing the pushdown-ed mapping of the terms to the index of the tuple
     *
     * @return the pushdown-ed mapping of the terms to the index of the tuple
     */
    public HashMap<String, Integer> getReturnedTermToIndexMap() {
        return returnedTermToIndexMap;
    }


    /**
     * Get the relation name
     *
     * @return the relation name
     */
    public String getRelationName() {
        return relationName;
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
            String line = this.reader.readLine();
            if (line != null) {
                // split the line by comma and remove white space
                String[] data = line.split(",");
                List<Object> fieldsList = new ArrayList<>();
                for (int i = 0; i < data.length; i++) {
                    // Check if the column is required before adding it to the fields list
                    if (requiredColumns.contains(this.fieldNames[i])) {
                        fieldsList.add(convertToTerm(data[i], this.fieldTypes[i])); // fieldType[i] is the type of the ith field
                    }
                }
                Object[] fields = fieldsList.toArray(new Object[0]);
                Tuple currentTuple = new Tuple(fields);
                return currentTuple;
            } else {
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
    @Override
    public void reset() throws IOException {
        try {
            this.reader.reset();
            this.reader.mark(0);
        } catch (IOException e) {
            fileReader = new FileReader(Catalog.getInstance(null).getDataFileName(relationName));
            this.reader = new BufferedReader(fileReader);
            this.reader.mark(0);
//            System.out.println(" Reset successfully");
        }
    }
}
