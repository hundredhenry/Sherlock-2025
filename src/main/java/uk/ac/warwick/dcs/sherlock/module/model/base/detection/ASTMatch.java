package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Stores a single match between two code blocks detected by the AST-based detector.
 * <p>
 * Each match records the line ranges in both files and a similarity score
 * indicating how structurally similar the two AST subtrees are.
 * </p>
 */
public class ASTMatch extends AbstractMatch<ASTMatch> {
    

    /**
     * The  weight of AST nodes in matched subtree from file 1 and 2
    */
    public int subtreeWeight1;
    public int subtreeWeight2;

    /**
     * Constructs a new AST match.
     *
     * @param file1Start start line in file 1
     * @param file1End   end line in file 1
     * @param file2Start start line in file 2
     * @param file2End   end line in file 2
     * @param similarity structural similarity score (0 to 1)
     * @param file1      the first source file
     * @param file2      the second source file
     * @param subtreeWeight1 the weight of AST nodes in the subtree from file 1
     * @param subtreeWeight2 the weight of AST nodes in the subtree from file 2
     */
    public ASTMatch(int file1Start, int file1End, int file2Start, int file2End,
                    float similarity, ISourceFile file1, int subtreeWeight1, ISourceFile file2, int subtreeWeight2) {
        super(file1Start, file1End, file2Start, file2End, similarity, file1, file2);
        this.subtreeWeight1 = subtreeWeight1;
        this.subtreeWeight2 = subtreeWeight2;
    }


    @Override
    public ASTMatch copy() {
        return new ASTMatch(this.lines.get(0).getKey(), this.lines.get(0).getValue(),
         this.lines.get(1).getKey(), this.lines.get(1).getValue(),
         this.similarity, this.files[0], this.files[1]);
    }
}
