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

    public boolean isEqual(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IntegerConstant)) {
            return false;
        }
        IntegerConstant v = (IntegerConstant) obj;
        return this.value.equals(v.value);
    }
}
