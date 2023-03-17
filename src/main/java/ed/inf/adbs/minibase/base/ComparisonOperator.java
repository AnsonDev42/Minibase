package ed.inf.adbs.minibase.base;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum ComparisonOperator {
    EQ("="),
    NEQ("!="),
    GT(">"),
    GEQ(">="),
    LT("<"),
    LEQ("<=");

    private final String text;

    ComparisonOperator(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static ComparisonOperator fromString(String s) throws NoSuchElementException {
        return Arrays.stream(values())
                .filter(op -> op.text.equalsIgnoreCase(s))
                .findFirst().get();
    }

    public boolean compare(Term left, Term right) {
        if (left instanceof Variable || right instanceof Variable) {
            throw new IllegalArgumentException("Cannot compare a variable, only constants");
        }
        if (left == null || right == null) {
            throw new IllegalArgumentException("Cannot compare a null value");
//            return false;
        }
//        check if left and right are the same type of constant
        if (left.getClass() != right.getClass()) {
            throw new IllegalArgumentException("Cannot compare two different types of constants" + left.getClass() + right.getClass() + left + right);
        }

        if (left instanceof StringConstant && right instanceof StringConstant) {
            String left_str = left.toString();
            String right_str = right.toString();
            switch (this) {
                case EQ:
                    return left_str.equals(right_str);
                case NEQ:
                    return !left_str.equals(right_str);
                case GT:
                    return left_str.compareTo(right_str) > 0;
                case GEQ:
                    return left_str.compareTo(right_str) >= 0;
                case LT:
                    return left_str.compareTo(right_str) < 0;
                case LEQ:
                    return left_str.compareTo(right_str) <= 0;
                default:
                    throw new IllegalArgumentException("Unknown operator: " + this);
            }
        } else { // when left and right are both IntegerConstant
            Integer left_int = ((IntegerConstant) left).getValue();
            Integer right_int = ((IntegerConstant) right).getValue();
            switch (this) {
                case EQ:
                    return left_int.equals(right_int);
                case NEQ:
                    return !left_int.equals(right_int);
                case GT:
                    return left_int > right_int;
                case GEQ:
                    return left_int >= right_int;
                case LT:
                    return left_int < right_int;
                case LEQ:
                    return left_int <= right_int;
                default:
                    throw new IllegalArgumentException("Unknown operator: " + this);
            }

        }
    }

    public ComparisonOperator reverse() {
        switch (this) {
            case EQ:
                return EQ;
            case NEQ:
                return NEQ;
            case GT:
                return LT;
            case GEQ:
                return LEQ;
            case LT:
                return GT;
            case LEQ:
                return GEQ;
            default:
                throw new IllegalArgumentException("Unknown operator: " + this);
        }
    }


}
