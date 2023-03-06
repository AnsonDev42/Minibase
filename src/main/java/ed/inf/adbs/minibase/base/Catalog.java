package ed.inf.adbs.minibase.base;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Catalog {
    private static Catalog instance = null;
    private final String schemaFile;
    private final String dataFolder;
    private Map<String, String> relationToFileMap;
    private Map<String, String[]> relationToSchemaMap;


    private Catalog(String rootFolder) {
        schemaFile = rootFolder + "/schema.txt";
        dataFolder = rootFolder + "/files/";
        relationToFileMap = new HashMap<>();
        relationToSchemaMap = new HashMap<>();
    }


    public static Catalog getInstance(String rootFolder) {
        if (instance == null) {
            instance = new Catalog(rootFolder);
        }
        return instance;
    }

    public void initialize() {
        relationToFileMap = new HashMap<>();
        relationToSchemaMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                String relationName = parts[0];
                String[] attributeTypes = Arrays.copyOfRange(parts, 1, parts.length);
                String fileName = dataFolder + relationName + ".csv";

                relationToFileMap.put(relationName, fileName);
                relationToSchemaMap.put(relationName, attributeTypes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDataFile(String relationName) {
        return relationToFileMap.get(relationName);
    }

    public String[] getSchema(String relationName) {
        return relationToSchemaMap.get(relationName);
    }
}