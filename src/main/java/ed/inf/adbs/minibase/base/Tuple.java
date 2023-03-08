package ed.inf.adbs.minibase.base;

import java.util.Arrays;

public class Tuple {
    private final Object[] fields;

    /**
     * Constructor
     *
     * @param fields of the tuple，
     */
    public Tuple(Object... fields) {
        this.fields = fields;
    }

    /**
     * Get the field according to the given index
     *
     * @param idx index of the field
     * @return a single field object
     */
    public Object getField(int idx) {
        return fields[idx];
    }

    /**
     * Return all the fields in the Tuple
     *
     * @return A list of fields in the Tuple
     */
    public Object[] getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return Arrays.toString(fields);
    }
}
