package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.AbstractModelTaskRawResult;
import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTMatch;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import java.util.ArrayList;
import java.util.List;

import java.io.Serial;
import java.io.Serializable;

/**
 * Stores the raw match results from an AST-based comparison of two files.
 * <p>
 * Thread-safe: all mutating and reading operations are synchronised.
 * </p>
 */
public class ASTRawResult extends AbstractModelTaskRawResult<ASTMatch> {

    /**
     * The AST trees for the two files, used for calculating node counts.
     */
    private transient ASTNode<?> tree1; // Does not need to be serialised (surviving to disk)
    private transient ASTNode<?> tree2;

    /**
     * Constructs a new empty result container for a file pair.
     *
     * @param file1 the first source file
     * @param file2 the second source file
     */
    public ASTRawResult(ISourceFile file1, ISourceFile file2, ASTNode<?> tree1, ASTNode<?> tree2) {
        super(file1,file2);
        this.tree1 = tree1;
        this.tree2 = tree2;
    }

    /**
     * Records a structural match between two AST subtrees.
     *
     * @param match the AST match to store
     */
    public synchronized void addMatch(ASTMatch match) {
        super.put(
                match,
                match.lines.get(0).getKey(), match.lines.get(0).getValue(),
                match.lines.get(1).getKey(), match.lines.get(1).getValue()
        );
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
                                 float similarity, int subtreeWeight1, int subtreeWeight2) {
        super.put(
                new ASTMatch(file1Start, file1End, file2Start, file2End, similarity, getFile1(), subtreeWeight1, getFile2(), subtreeWeight2)
                file1Start, file1End,
                file2Start, file2End
        );
    }


    /**
     * @return a copy of the list of matches
     */
    public synchronized List<ASTMatch> getMatches() {
        return super.getObjects();
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
     * Returns a string representation of all stored matches.
     *
     * @return string form of the matches
     */
    @Override
    public synchronized String toString() {
        StringBuilder str = new StringBuilder();
        for (ASTMatch match : super.getObjects()) {
            str.append("AST Match: similarity=").append(match.similarity)
                    .append(" lines=").append(match.lines).append("\n");
        }
        return str.toString();
    }
}
