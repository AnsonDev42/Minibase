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

/**
     * Join two tuples
     *
     * @param tuple1 the first tuple
     * @param tuple2 the second tuple
     * @return a new tuple joined from tuple1 and tuple2
     */
    public static Tuple join(Tuple tuple1, Tuple tuple2) {
        Object[] fields1 = tuple1.getFields();
        Object[] fields2 = tuple2.getFields();
        Object[] joinedFields = new Object[fields1.length + fields2.length];
        System.arraycopy(fields1, 0, joinedFields, 0, fields1.length);
        System.arraycopy(fields2, 0, joinedFields, fields1.length, fields2.length);
        return new Tuple(joinedFields);
    }


    @Override
    public int hashCode() {
        int hash = Arrays.deepHashCode(this.fields);
        return hash;
    }
}
