package ed.inf.adbs.minibase.base;

import ed.inf.adbs.minibase.Utils;

import javax.management.RuntimeErrorException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RelationalAtom extends Atom {
    private final String name;

    private List<Term> terms;

    public RelationalAtom(String name, List<Term> terms) {
        this.name = name;
        this.terms = terms;
    }

    public String getName() {
        return name;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public void setTerms(List<Term> terms) {
        this.terms = terms;
    }

    public HashSet<String> getVarsNames() {
        HashSet<String> varNameInRelAtom = new HashSet<>();
        for (Term term : terms) {
            if (term instanceof Variable) {
                varNameInRelAtom.add(term.toString());
            } else {
                RuntimeErrorException e = new RuntimeErrorException(new Error("Constant in the join condition"));
            }
        }
        return varNameInRelAtom;
    }

    /**
     * get the field name in the relation atom
     *
     * @return the list of field name
     */
    public String[] getFieldsName() {
        String[] varNameInRelAtom = new String[terms.size()];
        int i = 0;
        for (Term term : terms) {
            if (term instanceof Variable) {
                varNameInRelAtom[i] = term.toString();
            } else {
                RuntimeErrorException e = new RuntimeErrorException(new Error("Constant in final scanner"));
            }
            i++;
        }
        return varNameInRelAtom;
    }


    public void setTerm(int index, Term term) {
        this.terms.set(index, term);
    }

    @Override
    public String toString() {
        return name + "(" + Utils.join(terms, ", ") + ")";
    }
}
