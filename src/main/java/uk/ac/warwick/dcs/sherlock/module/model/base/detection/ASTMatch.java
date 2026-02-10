package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;

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
public class ASTMatch implements Serializable {
    /**
     * Line ranges for the matched blocks.
     * Index 0 = file 1 (start, end), Index 1 = file 2 (start, end).
     */
    public ArrayList<Tuple<Integer, Integer>> lines;

    /**
     * The two files being compared.
     */
    public ISourceFile[] files;

    /**
     * Structural similarity score between 0 and 1.
     */
    public float similarity;

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
     */
    public ASTMatch(int file1Start, int file1End, int file2Start, int file2End,
                    float similarity, ISourceFile file1, ISourceFile file2) {
        this.lines = new ArrayList<>();
        this.lines.add(new Tuple<>(file1Start, file1End));
        this.lines.add(new Tuple<>(file2Start, file2End));

        this.similarity = similarity;

        this.files = new ISourceFile[2];
        this.files[0] = file1;
        this.files[1] = file2;
    }

    /**
     * Checks if two matches reference the same line ranges.
     * Two matches are equal if they span the same lines in both files,
     * regardless of similarity score or file references.
     *
     * @param obj the object to compare with
     * @return true if both matches have identical line ranges, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ASTMatch)) return false;
        ASTMatch other = (ASTMatch) obj;
        return this.lines.get(0).equals(other.lines.get(0))
                && this.lines.get(1).equals(other.lines.get(1));
    }

    /**
     * Returns hash code based on line ranges.
     * Consistent with equals() — only considers line positions, not similarity or files.
     *
     * @return hash code value for this match
     */
    @Override
    public int hashCode() {
        return Objects.hash(lines.get(0), lines.get(1));
    }
}
