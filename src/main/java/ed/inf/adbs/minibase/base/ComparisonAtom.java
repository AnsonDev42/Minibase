package ed.inf.adbs.minibase.base;

import java.util.Arrays;

public class ComparisonAtom extends Atom {

    private final Term term1;

    private final Term term2;

    private final ComparisonOperator op;

    public ComparisonAtom(Term term1, Term term2, ComparisonOperator op) {
        this.term1 = term1;
        this.term2 = term2;
        this.op = op;
    }

    public Term getTerm1() {
        return term1;
    }

    public Term getTerm2() {
        return term2;
    }

    public ComparisonOperator getOp() {
        return op;
    }

    @Override
    public String toString() {
        return term1 + " " + op + " " + term2;
    }

    public boolean isTwoVars() {
        return term1 instanceof Variable && term2 instanceof Variable;
    }

    public boolean isTwoConstant() {
        return (term1 instanceof Constant && term2 instanceof Constant);
    }

    /**
     * return true if the two terms are both constant and the comparison is true
     *
     * @return
     */
    public boolean evalTwoConstant() {
        return op.compare(term1, term2);
    }

}
