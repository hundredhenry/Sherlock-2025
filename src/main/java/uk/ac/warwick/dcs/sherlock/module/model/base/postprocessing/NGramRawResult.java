package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.AbstractModelTaskRawResult;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the set of match objects for a pair of inputted files.
 * @param <T> N-Gram match object comparing similarity data between 2 code blocks
 */
public class NGramRawResult<T extends AbstractMatch<T>> extends AbstractModelTaskRawResult<T> {

	/**
	 * Object constructor, saves the compared file ids, initialises interior lists as ArrayLists, and sets size to zero.
	 * @param file1 File ID of the first file in the compared pair.
	 * @param file2 File ID of the second file in the compared pair.
	 */
	public NGramRawResult(ISourceFile file1, ISourceFile file2) {
		super(file1,file2);
	}
}
