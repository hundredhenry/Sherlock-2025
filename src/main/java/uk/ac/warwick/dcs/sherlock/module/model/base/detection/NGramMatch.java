package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;

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
public class NGramMatch extends AbstractMatch {

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
    NGramMatch(int refStart, int refEnd, int checkStart, int checkEnd, float similarity, ISourceFile file1, ISourceFile file2) {
        super(refStart, refEnd, checkStart, checkEnd, similarity, file1, file2);
    }
}