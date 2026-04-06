package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;


import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;




/**
 * Match storage class, stores further information about a match between two code blocks. 
 * <p>
 * Must be serializable!!!
 */
public abstract class AbstractMatch implements Serializable {

	// @Serial
	// private static final long serialVersionUID = 24L;

	/**
     * The line positions of both blocks.
     * Index 0 = file 1 (start, end), Index 1 = file 2 (start, end).
     */
    public ArrayList<Tuple<Integer, Integer>> lines;    // array list used for type safety of generics
   
    /**
     * The two files being compared.
     */
    public ISourceFile[] files;

    /**
     * Structural similarity score between 0 and 1.
     */
    public float similarity;

    /**
     * Constructs a new match.
     *
     * @param file1Start start line in file 1
     * @param file1End   end line in file 1
     * @param file2Start start line in file 2
     * @param file2End   end line in file 2
     * @param similarity structural similarity score (0 to 1)
     * @param file1      the first source file
     * @param file2      the second source file
     */
    public AbstractMatch(int file1Start, int file1End, int file2Start, int file2End,
                float similarity, ISourceFile file1, ISourceFile file2) {
        // init the array list
        this.lines = new ArrayList<>();
        // fill with line positions
        this.lines.add(new Tuple<>(file1Start, file1End));
        this.lines.add(new Tuple<>(file2Start, file2End));

        this.similarity = similarity;

        // init the array to a pair
        this.files = new ISourceFile[2];
        // store the file objects (pointers, the same pointers should be used globally)
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
        if (!obj.getClass().equals(this.getClass())) return false;
        
        AbstractMatch other = (AbstractMatch) obj;

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
