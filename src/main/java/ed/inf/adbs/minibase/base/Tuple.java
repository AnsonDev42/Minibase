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


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Tuple.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final Tuple other = (Tuple) obj;
        if (this.fields.length != other.fields.length) {
            return false;
        }
        for (int i = 0; i < this.fields.length; i++) {
            if (!this.fields[i].equals(other.fields[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = Arrays.deepHashCode(this.fields);
        return hash;
    }
}
