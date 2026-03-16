package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.AbstractModelTaskRawResult;
import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTMatch;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the raw match results from an AST-based comparison of two files.
 * <p>
 * Thread-safe: all mutating and reading operations are synchronised.
 * </p>
 */
public class ASTRawResult extends AbstractModelTaskRawResult {

    /**
     * The persistent ID of the first file in the comparison pair.
     */
    private final long file1id;

    /**
     * The persistent ID of the second file in the comparison pair.
     */
    private final long file2id;

    /**
     * The list of AST matches found between the two files.
     */
    private final List<ASTMatch> matches;

    /**
     * The AST trees for the two files, used for calculating node counts.
     */
    private transient ASTNode tree1; // Does not need to be serialised (surviving to disk)
    private transient ASTNode tree2;
    /**
     * Constructs a new empty result container for a file pair.
     *
     * @param file1 the first source file
     * @param file2 the second source file
     */
    public ASTRawResult(ISourceFile file1, ISourceFile file2, ASTNode tree1, ASTNode tree2) {
        this.file1id = file1.getPersistentId();
        this.file2id = file2.getPersistentId();
        this.matches = new ArrayList<>();
        this.tree1 = tree1;
        this.tree2 = tree2;
    }

    /**
     * Records a structural match between two AST subtrees.
     *
     * @param match the AST match to store
     */
    public synchronized void addMatch(ASTMatch match) {
        this.matches.add(match);
    }

    /**
     * Convenience method to record a match by its components.
     *
     * @param file1Start start line in file 1
     * @param file1End   end line in file 1
     * @param file2Start start line in file 2
     * @param file2End   end line in file 2
     * @param similarity structural similarity score (0 to 1)
     */
    public synchronized void put(int file1Start, int file1End,
                                 int file2Start, int file2End,
                                 float similarity) {
        this.matches.add(new ASTMatch(
                file1Start, file1End,
                file2Start, file2End,
                similarity,
                getFile1(), getFile2()
        ));
    }

    /**
     * @return the first file in the comparison pair
     */
    public ISourceFile getFile1() {
        return SherlockHelper.getSourceFile(file1id);
    }

    /**
     * @return the second file in the comparison pair
     */
    public ISourceFile getFile2() {
        return SherlockHelper.getSourceFile(file2id);
    }

    /**
     * @return a copy of the list of matches
     */
    public synchronized List<ASTMatch> getMatches() {
        return new ArrayList<>(matches);
    }

    /**
     * @return the node count of the first file's AST
     */
    public int getFile1NodeCount() {
        return tree1.getWeight();
    }

    /**
     * @return the node count of the second file's AST
     */ 
    public int getFile2NodeCount() {
        return tree2.getWeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isEmpty() {
        return matches.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean testType(AbstractModelTaskRawResult baseline) {
        return baseline instanceof ASTRawResult;
    }





    /**
     * Returns a string representation of all stored matches.
     *
     * @return string form of the matches
     */
    @Override
    public synchronized String toString() {
        StringBuilder str = new StringBuilder();
        for (ASTMatch match : matches) {
            str.append("AST Match: similarity=").append(match.similarity)
                    .append(" lines=").append(match.lines).append("\n");
        }
        return str.toString();
    }
}
