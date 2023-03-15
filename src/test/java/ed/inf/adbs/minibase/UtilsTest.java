package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UtilsTest {
    @Test
    public void ComparingTwoConstantTerms() {
//        1. Test for comparing two StringConstants that are equal using the "EQ" operator:
        Term left = new StringConstant("hello");
        Term right = new StringConstant("hello");
//        create a new ComparisonOperator with the operator EQ
        boolean result = ComparisonOperator.EQ.compare(left, right);
        assertTrue(result);
//        2. Test for comparing two IntegerConstants that are not equal using the "NEQ" operator:
        left = new IntegerConstant(10);
        right = new IntegerConstant(5);
        result = ComparisonOperator.NEQ.compare(left, right);
        assertTrue(result);

//        3. Test for comparing a Variable and a StringConstant which should throw an IllegalArgumentException:
        Term left1 = new Variable("x");
        Term right1 = new StringConstant("hello");
        assertThrows(IllegalArgumentException.class, () -> {
            ComparisonOperator.EQ.compare(left1, right1);
        });

//        4. Test for comparing a StringConstant and an IntegerConstant which should throw an IllegalArgumentException:
//        should it throw an IllegalArgumentException?
        Term left2 = new StringConstant("hello");
        Term right2 = new IntegerConstant(5);
        assertThrows(IllegalArgumentException.class, () -> {
            ComparisonOperator.EQ.compare(left2, right2);
        });
    }

}