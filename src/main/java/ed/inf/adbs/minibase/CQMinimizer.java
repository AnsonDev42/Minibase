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
    private static HashMap<Variable, Term> homomorphism = new HashMap<>();// TODO: THIS one doesn't work for now, since static related issues

    public static HashMap<Variable, Term> getHomomorphism() {
        return homomorphism;
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
     *
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     *
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        // TODO: add your implementation
        try {
//            Query query = QueryParser.parse(Paths.get(filename));
             Query query = QueryParser.parse("Q(x, z) :- R(x, 5, u), R(w, 5, v), R(w, 5, z)");
            // Query query = QueryParser.parse("Q(x) :- R(x, 'z'), S(4, z, w)");

            System.out.println("Entire query: " + query);
            Head head = query.getHead();
            List<Atom> body = query.getBody();
//            System.out.println("Head: " + head);
//            System.out.println("Body: " + body);

            // create a set for the restricted variable names  ( constant not included)
            List<Variable> restricted_var_list = head.getVariables();
            Set<String> restricted_var_set = new HashSet<>();
            for (Variable var : restricted_var_list) {
                restricted_var_set.add(var.getName());
            }

            // create mappings
            List<HashMap> mappings = getMappings(body, restricted_var_set);
            HashMap<Variable, Term> homomorphism = getHomomorphism(); //TODO: NOT WORKING see above
            ArrayList<Integer> removed_indeces =  new ArrayList<Integer>();
            while (true){
                // TODO: fix this: for now it can only perform one round of removal, due to global homo and local
                // homo issue, e.g. if find the first removal uses a-> b, it does not update all the a's in the body,
                // and therefore second round of removal will not work
                List<Atom> newBody = checkIfRemovable( body, mappings.get(1), restricted_var_set, homomorphism);
                if (newBody.size() == body.size()) {
                    break;
                }
                else {
                    body = newBody;
                }
            }
            System.out.println(body);
            }


        catch (Exception e)
        {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }

    }
    private static HashMap getTmpHomo( List<Atom> similarRelationAtoms, HashMap hashMap) {

        if (similarRelationAtoms.size() <= 1) {
            return null;
        }
        return null;
    }
    private static List<Atom> checkIfRemovable( List<Atom> body, HashMap hashMap,Set restricted_var_set, HashMap<Variable, Term> homomorphism) {
        if (body.size() <= 1) { return body;}

//       nested loop to check if the atom can be removed
        for (int i = 0; i < body.size(); i++) { //iterate all the atoms
            // get all the terms of the atom i
            List<Term> terms1  = ((RelationalAtom) body.get(i)).getTerms();
            // get  atom 1 head
            String head1 = ((RelationalAtom) body.get(i)).getName();
            System.out.println("terms1: " + terms1);
//      now iterate all the atoms except i
            for (int j = 0; j < body.size(); j++) { // iterate all the atoms except i
                if (i == j) {continue;}
                String head2 = ((RelationalAtom) body.get(j)).getName();
                if (!head1.equals(head2)) {continue;}
                // get all the terms of the atom j
                List<Term> terms2= ((RelationalAtom) body.get(j)).getTerms();
                if (terms1.size() != terms2.size()) {continue;}
                System.out.println("terms2: " + terms2);
                HashMap<Variable, Term> forward_tmp_homomorphism = new HashMap<>();
                for (int k = 0; k < terms1.size(); k++) {
                    // firstly check if the term1 is (string) constant and if matched by term2 in the same position
                    // since constants and restricted vars are non-replaceable
                    if ( terms1.get(k) instanceof Constant || restricted_var_set.contains(terms1.get(k).toString()) ) {
                        if (!terms1.get(k).toString().equals(terms2.get(k).toString())) {
                            System.out.println("terms1.get(k) not equal terms2.get(k): " + terms1.get(k) + " " + terms2.get(k));
                            break;
                        }
                    }
                    // secondly check if term1 is variable
                    if (terms1.get(k) instanceof Variable) {
                        System.out.println("terms1.get(k) variable: " + terms1.get(k));
                        // check if existed in the current homomorphism
                        if (forward_tmp_homomorphism.containsKey(terms1.get(k))) {
                            System.out.println("EXISTED KEY FROM forward_tmp_homomorphism: " + forward_tmp_homomorphism);
                            // use the mapping for term1 and compare
                            if (!forward_tmp_homomorphism.get(terms1.get(k).toString()).equals(terms2.get(k).toString())) {
                                    break;
                            }
                        }
                        else {      // add the mapping to the homomorphism if necessary
//                            if (!terms1.get(k).toString().equals(terms2.get(k).toString())) {
                                System.out.println("UPDATE homo: terms1.get(k) not equal terms2.get(k): " +
                                        terms1.get(k) + " " + terms2.get(k));
                                forward_tmp_homomorphism.put((Variable) terms1.get(k), terms2.get(k));
//                            }
                        }
                    }

                    // matched all the terms, return the index of the atom
                    if (k == (terms1.size() - 1)) {
                        System.out.println("matched all the terms, return the index of the atom");
                        List<Atom> newBody = new ArrayList<>();
                        for (int l = 0; l < body.size(); l++) {
                            if (l != i) {
                                newBody.add(body.get(l));
                            }
                        }
                        return newBody;
                    }
                }
            }
        }
        return body;
    }

    private static List<Atom> getSimilarRelationAtoms(List<Atom> body, HashMap<String,HashSet>  relation_to_indices , String relationName) {

        List<Atom> similar_relation_atoms = new ArrayList<>();
        List<Integer> list =  new ArrayList<Integer>(relation_to_indices.get(relationName));
        System.out.println("list: " + list);
        for (int i : list) {
                RelationalAtom relationalAtom = (RelationalAtom) body.get(i);
                Integer testLength = 3;
                if (relationalAtom.getTerms().size() == (testLength)) {
                    similar_relation_atoms.add(relationalAtom);
                }
        }
        return similar_relation_atoms;
    }


    public static List<HashMap> getMappings(List<Atom> body, Set<String> restricted_var_set) {
        // CREATE two mappings:
        // 1."relation_to_indices":  create an empty set for the replaceable variables, which maps variable names to a
        // list of indices that the variable appears in the body[index]
        // 2."var_to_indices" : create a set for mapping from relation names to a list of indices
        // that the relation appears in the body[index]
        HashMap<String,HashSet> relation_to_indices = new HashMap<>();
        HashMap<String,HashSet> var_to_indices = new HashMap<>();


        //  add replaceable variables to the allowed variables list
        int i = 0;
        for (Atom atom : body) {
            if (atom instanceof RelationalAtom) {
                RelationalAtom relationalAtom = (RelationalAtom) atom;

                // add for the mapping from relation names to a list of indices
                String relation_name = relationalAtom.getName();
                if (!relation_to_indices.containsKey(relation_name)) {
                    // add relation to set with empty index list
                    relation_to_indices.put(relation_name, new HashSet<Integer>());
                }
                // add index to index list
                relation_to_indices.get(relation_name).add(i);

        // add for the mapping from variable names to a list of indices
                List<Term> terms = relationalAtom.getTerms();
                for (Term term : terms) {
                    // if the term is a variable and not in the restricted variable set
                    if (term instanceof Variable && !restricted_var_set.contains(term.toString())){
                        if (!var_to_indices.containsKey(term.toString())) {
                            // add var to set with empty index list
                            var_to_indices.put(term.toString(), new HashSet<Integer>());
                        }
                        // add index to index list
                        var_to_indices.get(term.toString()).add(i);
                    }

//                    if (term instanceof StringConstant) {
//                        // print the constant
//                        System.out.println("String Constant found: " + term.toString());
//                        // print the whole atom
//                        System.out.println("Atom: " + atom);
//                    }
//                    if (term instanceof IntegerConstant) {
//                        // print the constant
//                        System.out.println("Integer Constant found: " + term.toString());
//                        // print the whole atom
//                        System.out.println("Atom: " + atom);
//                    }

                }
                i++;
            }
        }
        System.out.println("var_to_indices: " + var_to_indices);
        System.out.println("relation_to_indices: " + relation_to_indices);
        // return a list of the two mappings
        List<HashMap> mappings = new ArrayList<>();
        mappings.add(relation_to_indices);
        mappings.add(var_to_indices);
        return mappings;
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
