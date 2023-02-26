package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Minimization of conjunctive queries
 */
public class CQMinimizer {
    private static final Set<String> restricted_var_set = new HashSet<>();

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
    }

    /**
     * CQ minimization procedure
     * <p>
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        // TODO: add your implementation
        Query query = null;
        try {
            query = QueryParser.parse(Paths.get(inputFile));
//            Query query = QueryParser.parse("Q(x, z) :- R(4, z), S(x, y), T(12, 'x')");

            System.out.println("Entire query: " + query);

        } catch (Exception e) {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
        Head head = query.getHead();
        List<Atom> body = query.getBody();


        // create a set for the restricted variable names  ( constant not included)
        List<Variable> restricted_var_list = head.getVariables();
        for (Variable var : restricted_var_list) {
            restricted_var_set.add(var.toString());
        }
        List<Atom> newBody = null;
        while (true) {
            newBody = updateBody(body);
            if (newBody.size() == body.size()) { // equivalent to check len of removed_indeces
                break;
            } else {
                body = newBody; // update body and continue removing
            }
        }

        query = new Query(head, newBody);
        // save the query to the outputFile in string format
        String output = query.toString();
        System.out.println("Minimized query:" + output);
        // create the folder if not exist

        try {
//             write and overwrite the file if it exists
            Files.write(Paths.get(outputFile), output.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Write to file successfully");
        } catch (IOException e) {
            System.out.println("Write permission denied.");
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
                            if (!isSameTerm(terms1.get(k), terms2.get(k))) {
                                forward_tmp_homomorphism.put(terms1.get(k).toString(), terms2.get(k));
                            }
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
        System.out.println("updating the body: " + body);
        System.out.println("updating based on the homomorphism: " + homomorphism + " and removing index: " + removing_index);
        // remove the possible removable atom directly in newbody
        List<Atom> newBody = new ArrayList<Atom>(body);
        newBody.remove(removing_index);

        // STEP1:  find all affected atoms (not the removed atom) by the homomorphism
        HashSet affectedAtoms = new HashSet();
        for (int i = 0; i < newBody.size(); i++) {
            List<Term> terms = ((RelationalAtom) newBody.get(i)).getTerms();
            for (int j = 0; j < terms.size(); j++) {
                if (terms.get(j) instanceof Variable && homomorphism.containsKey(terms.get(j).toString())) {
                    System.out.println("Due to homo" + terms.get(j) + "->" + homomorphism.get(terms.get(j).toString()) +
                            ", atom " + terms + " is affected, need to check later");
                    affectedAtoms.add(i);
                    break;
                }
//                else {System.out.println("NOT UPDATE the terms since the term not affected by homo" + terms);}
            }
        }
        System.out.println("affected atoms: " + affectedAtoms + " and new body: " + newBody);
        Iterator<Integer> it = affectedAtoms.iterator();

        // STEP2: check if ALL affected atom are removable(i.e. find the equivalent atom), if not, return the original body
        Boolean removable = true;
        ArrayList removing_indices = new ArrayList();
        while (it.hasNext() && removable) {
            removable = false;
            int i = it.next();
            String affectedAtomHead = ((RelationalAtom) newBody.get(i)).getName();
            List<Term> affectedAtomTerms = new ArrayList<Term>(((RelationalAtom) newBody.get(i)).getTerms());
            // update the affected atom terms based on the homomorphism
            for (int k = 0; k < affectedAtomTerms.size(); k++) {
                if (affectedAtomTerms.get(k) instanceof Variable && homomorphism.containsKey(affectedAtomTerms.get(k).toString())) {
                    affectedAtomTerms.set(k, homomorphism.get(affectedAtomTerms.get(k).toString()));
                }
            }

            // check if current affectedAtom is equals to atom_j
            for (int j = 0; j < newBody.size(); j++) {
                String head2 = ((RelationalAtom) newBody.get(j)).getName();
                List<Term> terms2 = ((RelationalAtom) newBody.get(j)).getTerms();

                // skip itself and different head/size atoms
                if (affectedAtoms.contains(j) || !affectedAtomHead.equals(head2) || affectedAtomTerms.size() != terms2.size()) {
                    continue;
                }
                // check each term in the affectedAtom
                for (int k = 0; k < affectedAtomTerms.size(); k++) {
                    if (affectedAtomTerms.get(k) instanceof Constant) {
                        if (!affectedAtomTerms.get(k).toString().equals(terms2.get(k).toString())) {
                            break; // not removable
                        }
                    } else if (affectedAtomTerms.get(k) instanceof Variable) {
                        if (!isSameTerm(affectedAtomTerms.get(k), terms2.get(k))) {
                            break;
                        }
                    }
                    // matched all the terms, return the index of the atom
                    if (k == (affectedAtomTerms.size() - 1)) {
                        System.out.println("matched all the terms, can remove this affected atom");
                        removing_indices.add(i);
                        removable = true;
                    }
                }
            }
        }

        if (removable) {
            List<Atom> removedBody = new ArrayList<Atom>();
            for (int i = 0; i < newBody.size(); i++) {
                if (!removing_indices.contains(i)) {
                    removedBody.add(newBody.get(i));
                }
            }
            System.out.println("updated newBody: " + removedBody);
            return removedBody;
        } else {
            System.out.println("FAILED to update newBody not equal: body" + body + " vs newBody" + newBody);
            return body;
        }
    }


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
