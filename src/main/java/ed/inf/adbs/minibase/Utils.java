package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;

import java.util.Collection;
import java.util.stream.Collectors;

public class Utils {

    public static String join(Collection<?> c, String delimiter) {
        return c.stream()
                .map(x -> x.toString())
                .collect(Collectors.joining(delimiter));
    }


    /**
     *  create a new copied term copy from the given term
     * @param term the term to be copied
     * @return the copied term
     */
    public static Term copyTerm(Term term){
        Term newTerm;
        if (term instanceof Constant) {// create a new constant to put into the added condition
            newTerm = term instanceof StringConstant
                    ? new StringConstant(((StringConstant) term).getValue())
                    : new IntegerConstant(((IntegerConstant) term).getValue());
        } else {//if (term instanceof Variable) {
            newTerm = new Variable(term.toString());// create a new constant to put into the added condition
        }
        return newTerm;
    }


}


