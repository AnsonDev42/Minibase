package ed.inf.adbs.minibase.base;

public class IntegerConstant extends Constant {
    private final Integer value;

    public IntegerConstant(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        IntegerConstant rhs = (IntegerConstant) obj;
        return value.equals(rhs.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
