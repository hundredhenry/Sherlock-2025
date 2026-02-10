package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Contains all data for a single matched pair.
 * <p>
 *     Contains:
 *     Start and end lines for block in reference file,
 *     Start and end lines for block in checked file,
 *     The float value denoting similarity between the 2 blocks,
 *     A pointer to the reference file (File1),
 *     A pointer to the checked file (File2).
 * </p>
 */
public class NgramMatch implements Serializable {
    /**
     * The line positions of both blocks.
     */
    public ArrayList<Tuple<Integer, Integer>> lines;    // array list used for type safety of generics
    /**
     * The similarity between the section of both files.
     */
    public float similarity;
    /**
     * The two files with matching sections.
     */
    public ISourceFile[] files;

    /**
     * Constructor, stores all inputted data in the container object.
     * @param refStart The start line of the block in File1.
     * @param refEnd The end line of the block in File1.
     * @param checkStart The start line of the block in File2.
     * @param checkEnd The end line of the block in File2.
     * @param similarity The similarity between the 2 blocks.
     * @param file1 The first file.
     * @param file2 The second file.
     */
    NgramMatch(int refStart, int refEnd, int checkStart, int checkEnd, float similarity, ISourceFile file1, ISourceFile file2) {
        // init the array list
        lines = new ArrayList<>();
        // fill with line positions
        lines.add(new Tuple<>(refStart, refEnd));
        lines.add(new Tuple<>(checkStart, checkEnd));

        this.similarity = similarity;
        // init the array to a pair
        files = new ISourceFile[2];
        // store the file objects (pointers, the same pointers should be used globally)
        files[0] = file1;
        files[1] = file2;
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
        if (!(obj instanceof NgramMatch)) return false;
        NgramMatch other = (NgramMatch) obj;
        
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