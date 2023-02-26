package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.nio.file.Paths;
import java.util.*;

/**
 * Minimization of conjunctive queries
 */
public class CQMinimizer {
    private static final HashMap<Variable, Term> homomorphism = new HashMap<>();// TODO: THIS one doesn't work for now, since static related issues

    private static final List<Integer> removed_indeces = new ArrayList<>();
    private static final Set<String> restricted_var_set = new HashSet<>();

    public static HashMap<Variable, Term> clearHomomorphism() {
        homomorphism.clear();
        return homomorphism;
    }


    public static Boolean isSameTerm(Term Term1, Term Term2) {
        if (Term1 instanceof StringConstant && Term2 instanceof StringConstant) {
            return ((StringConstant) Term1).getValue().equals(((StringConstant) Term2).getValue());
        } else if (Term1 instanceof IntegerConstant && Term2 instanceof IntegerConstant) {
            return ((IntegerConstant) Term1).getValue() == ((IntegerConstant) Term2).getValue();
        } else if (Term1 instanceof Variable && Term2 instanceof Variable) {
            return ((Variable) Term1).getName().equals(((Variable) Term2).getName());
        } else {
            return false;
        }
    }


    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: CQMinimizer input_file output_file");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];
        minimizeCQ(inputFile, outputFile);

//        parsingExample(inputFile);
    }

    /**
     * CQ minimization procedure
     * <p>
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        // TODO: add your implementation
        try {
//            Query query = QueryParser.parse(Paths.get(filename));
            Query query = QueryParser.parse("Q(x) :- R(x, y), R(x, 4), R('ADBS', 4), R(u, 12), R(u, v), S('ADBS', 12, 7)");
            // Query query = QueryParser.parse("Q(x) :- R(x, 'z'), S(4, z, w)");

            System.out.println("Entire query: " + query);
            Head head = query.getHead();
            List<Atom> body = query.getBody();


            // create a set for the restricted variable names  ( constant not included)
            List<Variable> restricted_var_list = head.getVariables();
            for (Variable var : restricted_var_list) {
                restricted_var_set.add(var.toString());
            }

            // create mappings
//            List<HashMap> mappings = getMappings(body);
            while (true) {
                // TODO: fix this: for now it can only perform one round of removal, due to global homo and local
                // homo issue, e.g. if find the first removal uses a-> b, it does not update all the a's in the body,
                // and therefore second round of removal will not work
                List<Atom> newBody = updateBody(body);
                if (newBody.size() == body.size()) { // equavlent to check len of removed_indeces
                    break;
                } else {
                    body = newBody;
                }
            }
            System.out.println(body);
        } catch (Exception e) {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }

    }

    private static List<Atom> updateBody(List<Atom> body) {
        if (body.size() <= 1) {
            return body;
        }

        for (int i = 0; i < body.size(); i++) { //iterate all the atoms
            String head1 = ((RelationalAtom) body.get(i)).getName();
            List<Term> terms1 = ((RelationalAtom) body.get(i)).getTerms();
            // check if the atom i is removable ( equals to atom j)
            for (int j = 0; j < body.size(); j++) {
                String head2 = ((RelationalAtom) body.get(j)).getName();
                List<Term> terms2 = ((RelationalAtom) body.get(j)).getTerms();
                if (i == j || !head1.equals(head2) || terms1.size() != terms2.size()) {
                    continue;
                }

                HashMap<String, Term> forward_tmp_homomorphism = new HashMap<String, Term>();
                for (int k = 0; k < terms1.size(); k++) {
                    // firstly check if term1 is (string) constant and if equivalent to term2
                    //      since constants and restricted vars are non-replaceable
                    if (terms1.get(k) instanceof Constant || restricted_var_set.contains(terms1.get(k).toString())) {
                        if (!terms1.get(k).toString().equals(terms2.get(k).toString())) {
                            break; // not removable
                        }
                    }
                    // secondly check term1 is equivalent to term2 (using homomorphism)
                    if (terms1.get(k) instanceof Variable && !restricted_var_set.contains(terms1.get(k).toString())) {
                        // check if existed in the current homomorphism
                        if (forward_tmp_homomorphism.containsKey(terms1.get(k).toString())) {
                            System.out.println("EXISTED KEY FROM forward_tmp_homomorphism: " + forward_tmp_homomorphism);
                            // use the mapping for term1 and compare
                            if (!isSameTerm(forward_tmp_homomorphism.get(terms1.get(k).toString()), terms2.get(k))) {
                                System.out.println("NOT EQUAL: " + forward_tmp_homomorphism.get(terms1.get(k).toString()) + " " + terms2.get(k));
                                break;
                            }
                        } else {      // add the mapping to the homomorphism if necessary
                            forward_tmp_homomorphism.put(terms1.get(k).toString(), terms2.get(k));
                        }
                    }

                    // matched all the terms, return the index of the atom
                    if (k == (terms1.size() - 1)) {
                        System.out.println("matched all the terms, return the index of the atom");
                        List<Atom> newBody = checkRemovable(body, i, forward_tmp_homomorphism);

                        if (newBody.size() != body.size()) {
                            return newBody;
                        }
                    }
                }
            }
        }
        return body;
    }

    private static List<Atom> checkRemovable(List<Atom> body, int removing_index, HashMap<String, Term> homomorphism) {

        // update the body
        System.out.println("updating the body: " + body);
        System.out.println("updating based on the homomorphism: " + homomorphism + " and removing index: " + removing_index);
        List<Atom> newBody = new ArrayList<Atom>(body);
        newBody.remove(removing_index);

        HashSet affectedAtoms = new HashSet();
        for (int i = 0; i < newBody.size(); i++) {
//          update each term in the atom if in the current homomorphism
            if (removing_index == i) { // impossible
                continue;
            }
            List<Term> terms = ((RelationalAtom) newBody.get(i)).getTerms();
            for (int j = 0; j < terms.size(); j++) {
                if (terms.get(j) instanceof Variable && homomorphism.containsKey(terms.get(j).toString())) {
                    System.out.println("updating the terms by: " + terms.get(j) + "-->" + homomorphism.get(terms.get(j).toString()));
                    Term testTerm = new Variable(terms.get(j).toString());
                    terms.set(j, homomorphism.get(terms.get(j).toString()));
                    affectedAtoms.add(i);
                }
//                System.out.println("NOT UPDATE the terms since the term " + terms.get(j));
            }
        }
        Iterator<Integer> it = affectedAtoms.iterator();
        // check if affected atoms are removable and keep removing
        Boolean removable = true;
        ArrayList removing_indices = new ArrayList(); // TODO: fix! inbewteen removal?
        while (it.hasNext() && removable) {
            removable = false;
            int i = it.next();
            String head1 = ((RelationalAtom) newBody.get(i)).getName();
            List<Term> terms1 = ((RelationalAtom) newBody.get(i)).getTerms();
            for (int j = 0; j < body.size(); j++) {
                String head2 = ((RelationalAtom) body.get(j)).getName();
                List<Term> terms2 = ((RelationalAtom) body.get(j)).getTerms();

                if (affectedAtoms.contains(j) || removing_index == j || !head1.equals(head2) || terms1.size() != terms2.size()) {
                    continue;
                }

                for (int k = 0; k < terms1.size(); k++) {
                    // firstly check if term1 is (string) constant and if equivalent to term2
                    //      since constants and restricted vars are non-replaceable
                    if (terms1.get(k) instanceof Constant) {
                        if (!terms1.get(k).toString().equals(terms2.get(k).toString())) {
                            break; // not removable
                        }
                    }
                    // secondly check term1 is equivalent to term2
                    if (terms1.get(k) instanceof Variable) {
                        if (!isSameTerm(terms1.get(k), terms2.get(k))) {
                            break;
                        }
                    }

                    // matched all the terms, return the index of the atom
                    if (k == (terms1.size() - 1)) {
                        System.out.println("matched all the terms, can remove this affected atom");
                        removable = true;
                    }
                }
            }
        }
        if (removable) {
            for (int i = 0; i < removing_indices.size(); i++) {
                if (i == 0) {
                    newBody.remove(removing_indices.get(i));
                } else {
                    newBody.remove(removing_indices.get(i - 1));
                }
            }
            System.out.println("updated newBody: " + newBody);
            return newBody;
        } else {
            return body;
        }
    }


//    public static List<HashMap> getMappings(List<Atom> body) {
//        // CREATE two mappings:
//        // 1."relation_to_indices":  create an empty set for the replaceable variables, which maps variable names to a
//        // list of indices that the variable appears in the body[index]
//        // 2."var_to_indices" : create a set for mapping from relation names to a list of indices
//        // that the relation appears in the body[index]
//        HashMap<String, HashSet> relation_to_indices = new HashMap<>();
//        HashMap<String, HashSet> var_to_indices = new HashMap<>();
//
//
//        //  add replaceable variables to the allowed variables list
//        int i = 0;
//        for (Atom atom : body) {
//            if (atom instanceof RelationalAtom) {
//                RelationalAtom relationalAtom = (RelationalAtom) atom;
//
//                // add for the mapping from relation names to a list of indices
//                String relation_name = relationalAtom.getName();
//                if (!relation_to_indices.containsKey(relation_name)) {
//                    // add relation to set with empty index list
//                    relation_to_indices.put(relation_name, new HashSet<Integer>());
//                }
//                // add index to index list
//                relation_to_indices.get(relation_name).add(i);
//
//                // add for the mapping from variable names to a list of indices
//                List<Term> terms = relationalAtom.getTerms();
//                for (Term term : terms) {
//                    // if the term is a variable and not in the restricted variable set
//                    if (term instanceof Variable && !restricted_var_set.contains(term.toString())) {
//                        if (!var_to_indices.containsKey(term.toString())) {
//                            // add var to set with empty index list
//                            var_to_indices.put(term.toString(), new HashSet<Integer>());
//                        }
//                        // add index to index list
//                        var_to_indices.get(term.toString()).add(i);
//                    }
//                }
//                i++;
//            }
//        }
//        System.out.println("var_to_indices: " + var_to_indices);
//        System.out.println("relation_to_indices: " + relation_to_indices);
//        // return a list of the two mappings
//        List<HashMap> mappings = new ArrayList<>();
//        mappings.add(relation_to_indices);
//        mappings.add(var_to_indices);
//        return mappings;
//    }


    /**
     * Example method for getting started with the parser.
     * Reads CQ from a file and prints it to screen, then extracts Head and Body
     * from the query and prints them to screen.
     */

    public static void parsingExample(String filename) {

        try {
            Query query = QueryParser.parse(Paths.get(filename));
            // Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w)");
            // Query query = QueryParser.parse("Q(x) :- R(x, 'z'), S(4, z, w)");

            System.out.println("Entire query: " + query);
            Head head = query.getHead();
            System.out.println("Head: " + head);
            List<Atom> body = query.getBody();
            System.out.println("Body: " + body);
        } catch (Exception e) {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

}
