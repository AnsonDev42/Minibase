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
        Variable rhs = (Variable) obj;
        return name.equals(rhs.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
}
