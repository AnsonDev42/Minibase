package ed.inf.adbs.minibase.base;

public class Variable extends Term {
    private final String name;

    public Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isEqual(Term obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Variable)) {
            return false;
        }
        Variable v = (Variable) obj;
        return this.name.equals(v.name);
    }
}
