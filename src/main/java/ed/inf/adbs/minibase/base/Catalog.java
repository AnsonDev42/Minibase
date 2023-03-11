package ed.inf.adbs.minibase.base;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Catalog {
    private static Catalog instance = null;
    private final String schemaFile;
    private final String dataFolder;
    private final Map<String, String> relationToFileMap;
    private final Map<String, String[]> relationToSchemaMap;

    /*
     * Constructor
     *  @param rootFolder the root folder of the database(contains schema.txt and files folder)
     *  it construct a catalog object that contains all the necessary mappings
     */
    public Catalog(String rootFolder) {
        if (rootFolder == null) {
            throw new IllegalArgumentException("Root folder cannot be null");
        }
        schemaFile = rootFolder + "/schema.txt";
        dataFolder = rootFolder + "/files/";
        relationToFileMap = new HashMap<>();
        relationToSchemaMap = new HashMap<>();
        initialize();
    }

    /*
     * Initialize the catalog object by reading the schema file to create mappings for relation names to file names
     * and schema to attribute types
     *  @return void
     */
    public void initialize() {
        try (BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                String relationName = parts[0];
                String[] attributeTypes = Arrays.copyOfRange(parts, 1, parts.length);
                String fileName = dataFolder + relationName + ".csv";
                relationToFileMap.put(relationName, fileName);
                relationToSchemaMap.put(relationName, attributeTypes);
//
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Get the file path of the data file for a given relation
     *  @param relationName the name of the relation
     *  @return the file path of the data file for the given relation
     */
    public String getDataFileName(String relationName) {
        String fileName = relationToFileMap.get(relationName);
        if (fileName == null) {
            throw new IllegalArgumentException("Relation " + relationName + " does not exist");
        }
        return fileName;
    }

    /* Get the singleton instance of the catalog
     *  @param rootFolder the root folder of the database(contains schema.txt and files folder)
     *  @return the singleton instance of the catalog
     */
    public static Catalog getInstance(String rootFolder) {
        if (instance == null) {
            instance = new Catalog(rootFolder);
            instance.initialize();
        }
        return instance;
    }


    /*
     * Get the schema(a list of types for each field) of a given relation
     *  @param relationName the name of the relation
     *  @return the schema of the given relation
     */
    public String[] getSchema(String relationName) {
        if (relationName == null) {
            throw new IllegalArgumentException("Relation name cannot be null");
        }
        if (!relationToSchemaMap.containsKey(relationName)) {
            throw new IllegalArgumentException("Relation " + relationName + " does not exist");
        }
        return relationToSchemaMap.get(relationName);
    }

}