# Minibase

## 0. Run the programme.

TL,DL: Default setting for running CQMinimiser and Minibase(evaluation) should be fine.

You shouldn't test out of the range from PDF, if so, the following error will be thrown(and more undocumented, please
check thrown Error message if available):
`Special case: when a string and an integer is compared, it will throw a comparsion error, since the PDF only indicated
that two string are compared in lexcial order, but not mentioned about between string and integer.(e.g. throw exception
for Q(x)-: R(x,y,z),
z="test string" ,where R is (int,int,int) )`

`Special case: if condition contains unseen Variable, it will throw a variable not found error. (e.g. throw exception`

## TASK2: Extracting join conditions from the body of a query explaination

*you can also find in QueryPlanner code comment, but they are separated out with corresponding functions*

0. Prepocess a bit for the body, by calling **removeCondition**() method. This method is responsible for removing
   conditions always True; return dummyOperator if any condition is False; and redundant-conditions(WIP).

1. First, the **findAndUpdateCondition**() method is called. This method is responsible for finding and updating
   hidden
   conditions in the query body, both selection and join conditions, by replacing repeated variables to new variables(
   guaranteed no repeated var).
   This method calls **addImpliedConditions**()
   to achieve above goal and handle hidden join conditions that are implicitly embedded within the relational atoms.

    1. In the **addImpliedConditions**() method, the code iterates through each term of a relational atom. If a term is
       a
       repeated
       variable (i.e., the variable is used in multiple relational atoms), this indicates a potential join condition.
       The
       method adds a new join condition with a fresh variable and updates the relational atom accordingly.
2. After processing hidden conditions, the **createTwoConMap**() method is called. This method creates two maps: one for
   selection conditions and one for join conditions. These maps are used to efficiently construct the operator tree
   later
   on. And this is how the join conditions are extracted from the body of a query.

    1. In the **createTwoConMap**() method, the code iterates through all conditions in the query body. For each
       condition, it
       determines which relational atoms the variables involved in the condition belong to. If both variables belong to
       different relational atoms, this indicates a join condition. In this case, the join condition is added to the
       join
       map
       for both relational atoms involved.

3. With the constructing the LEFT DEEP JOIN tree, it creates a joined condition pool for left side(LHS pool), and find
   the
   intersection of this left pool with the to be joint relationAtom's join condition pool(RHS pool). Only the
   intersected
   condition would be used for the join operator. After the joint, the LHS pool updates by adding the RHS pool.

## TASK3: optimisation rules

0. Reduce conditions. The removeCondition() finds
    1. always false condition and return a dummy operator immediately,
       since false condition returns nothing.
    2. always true condition since it is useless and therefore selection operator can be reduced, since removing
       always
       true condition does not affect comparison results.
1. Choose only required column. The computeRequiredColumn() scans and stores all required variables in both the body and
   the head. The hashSet requiredColumn is passed into each scanOperator so that it only returns required column, which
   reduced the size of intermediate results. They are correct because the requiredColumn contains everything needed to
   be compared so that in any operator it can perform checking condition correctly.


