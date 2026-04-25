package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Stores a single match of simple objects, ie Strings or Integers, between
 * two files.
 * </p>
 */
public class StringMatch extends AbstractMatch<StringMatch> {
    
    /**
     * The string shared between both files
     */
    public String string;

    /**
     * Constructs a new AST match.
     *
     * @param file1Start start line in file 1
     * @param file1End   end line in file 1
     * @param file2Start start line in file 2
     * @param file2End   end line in file 2
     * @param string     the string shared between both files
     * @param file1      the first source file
     * @param file2      the second source file
     */
    public StringMatch(int file1Start, int file1End, int file2Start, int file2End,
                    String str, ISourceFile file1, ISourceFile file2) {
        super(file1Start, file1End, file2Start, file2End, 1, file1, file2);

        this.string = str;
    }

    /**
     * Returns the string shared between both files
     * @return the string shared between both files
     */
    public String getString() {
        return this.string;
    }
    

    @Override
    public StringMatch copy() {
        return new StringMatch(this.lines.get(0).getKey(), this.lines.get(0).getValue(),
         this.lines.get(1).getKey(), this.lines.get(1).getValue(), 
         this.string, this.files[0], this.files[1]);
    }

    /**
     * String output of the match
     * @return the string shared between both files
     */
    @Override
    public String toString() {
        return this.string.toString();
    }


}
