package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.nio.file.Paths;
import java.util.*;

/**
 *
 * Minimization of conjunctive queries
 *
 */
public class CQMinimizer {

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
     *
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     *
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        // TODO: add your implementation
        try {
//            Query query = QueryParser.parse(Paths.get(filename));
             Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), S(y,z,k)");
            // Query query = QueryParser.parse("Q(x) :- R(x, 'z'), S(4, z, w)");

            System.out.println("Entire query: " + query);
            Head head = query.getHead();
            System.out.println("Head: " + head);
            List<Atom> body = query.getBody();
            System.out.println("Body: " + body);

            // create a set for the restricted variable names
            List<Variable> restricted_var_list = head.getVariables();
            Set<String> restricted_var_set = new HashSet<>();
            for (Variable var : restricted_var_list) {
                restricted_var_set.add(var.getName());
            }

            // create an empty set for the allowed variables
            HashMap<String,ArrayList> allowed_var_set = new HashMap<>();


            //  add potential variables to the allowed variables list
            int i = 0;
            for (Atom atom : body) {
                if (atom instanceof RelationalAtom) {
                    RelationalAtom relationalAtom = (RelationalAtom) atom;
                    List<Term> terms = relationalAtom.getTerms();
                    for (Term term : terms) {
                        if (term instanceof Variable && !restricted_var_set.contains(term.toString())){
                            if (!allowed_var_set.containsKey(term.toString())) {
                                // add var to set with empty index list
                                allowed_var_set.put(term.toString(), new ArrayList<Integer>());
                            }
                            // add index to index list
                            allowed_var_set.get(term.toString()).add(i);
                        }
                    }
                    i++;
                }
            }
            System.out.println("Allowed variables: " + allowed_var_set);

            // remove the allowed variables from the body if it can be removed
            String atom_to_remove = "w";
            ArrayList<Integer> removing_index =  new ArrayList<Integer>();
            for (Atom atom : body) {
                if (atom instanceof RelationalAtom) {
                    RelationalAtom relationalAtom = (RelationalAtom) atom;
                    List<Term> terms = relationalAtom.getTerms();
                    // iterate through the terms by the removing_index
                    for (int index : removing_index) {
                        if (terms.get(index) instanceof Variable && terms.get(index).toString().equals(atom_to_remove)) {
                            // remove the atom
                            body.remove(atom);
                            break;
                        }
                    }
                }
            }

        }
        catch (Exception e)
        {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
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
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

}
