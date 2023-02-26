package ed.inf.adbs.minibase.base;

public class StringConstant extends Constant {
    private final String value;

    public StringConstant(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }

    public boolean isEqual(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof StringConstant)) {
            return false;
        }
        StringConstant v = (StringConstant) obj;
        return this.value.equals(v.value);
    }
}