package ed.inf.adbs.minibase.base;

import ed.inf.adbs.minibase.Utils;

import java.util.List;

public class SumAggregate extends Term {

    private final List<Term> productTerms;
    private List<String> varNames;

    public SumAggregate(List<Term> terms) {
        this.productTerms = terms;
    }

    public List<Term> getProductTerms() {
        return productTerms;
    }

    /**
     * Get the variable names in the product terms of the sum aggregate.
     *
     * @return
     */
    public List<String> getOnlyVarNames() {
        if (varNames == null) {
            varNames = new java.util.ArrayList<String>();
            for (Term term : productTerms) {
                if (term instanceof Variable) {
                    varNames.add(((Variable) term).getName());
                }
            }
        }
        return varNames;
    }

    @Override
    public String toString() {
        return "SUM(" + Utils.join(productTerms, " * ") + ")";
    }
}
