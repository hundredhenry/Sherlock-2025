package uk.ac.warwick.dcs.sherlock.module.model.base.scoring;

import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.NgramMatch;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class NGramScorer {

	private final float threshold;

	// the list of files in the current match group
	public ArrayList<ISourceFile> file_list;		// public to allow use in external loops

	// The scoring info for each file
	private ArrayList<FileInfo> file_info;

	/**
	 * Object constructor.
	 * @param threshold The threshold used by the postprocessor to determine if cases are common.
	 */
	public NGramScorer(float threshold) {
		this.threshold = threshold;
	}

	/**
	 * Resets/Initialises the group info storage for each match group.
	 */
	public void newGroup() {
		file_list = new ArrayList<>();
		file_info = new ArrayList<>();
	}

	/**
	 * Adds files and their score info to the group data structure.
	 * <p>
	 *     Builds a list of all files (file_list) and adds each pair that is in that file
	 * </p>
	 * @param pair The pair of files and their local match score.
	 */
	public void add(NgramMatch pair) {
		// for both files
		for (int i = 0 ; i < 2 ; i++) {
			// check for if the files exist in file_list, if they do add to them, if not make a new object to add to the list
			if (file_list.contains(pair.files[i])) {
				// acquire the respective file_info index and update it with the new similarity score
				file_info.get(file_list.indexOf(pair.files[i])).addToFileInfo(pair.similarity);
				// add the pair to the match lists
//				file_matches.get(match_list.indexOf(file)).matches.add(pair);
			} else {
				// add the new file and a respective FileInfo object (ass they are always added in pairs the indexes will always match)
				file_list.add(pair.files[i]);
				file_info.add(new FileInfo(pair.similarity, pair.lines.get(i)));
				// create a new match list and add the pair to it
//				match_list.add(file);
//				file_matches.add(new MatchList(pair));
			}
		}
	}

	/**
	 * Check if the set of files is enough to be considered "common".
	 * @param file_count The number of files the system is comparing.
	 * @param list The list of matches (unused parameter, kept for API compatibility).
	 * @return true if the match group is uncommon (should be kept), false if common (should be filtered).
	 */
	public boolean checkSize(int file_count, ArrayList<NgramMatch> list) {
		// if the match is uncommon (appears in few files), return true to keep it
		if ((file_list.size() / file_count) <= threshold) {
			return true;
		}
		// if the match is common (appears in many files), return false to filter it out
		else {
			return false;
		}
	}

	/**
	 * Adds a block for the current file to the current groups output data structure along with its score.
	 * @param file The file that's block is being referenced.
	 * @param out_group The current block groups output structure.
	 */
	public void addScoredBlock(ISourceFile file, ICodeBlockGroup out_group) {
		// calculate a suitable score for the inputted file based on the available data
		int index = file_list.indexOf(file);
		// average similarity across all matches for this file
		float score = file_info.get(index).total_similarity / file_info.get(index).similar_files;

		out_group.addCodeBlock(file, score, file_info.get(index).lines);
	}

	/**
	 * Object used to store cumulative similarity score for a file in a match block.
	 */
	class FileInfo {

		/**
		 * The total of all similarity scores involving this file.
		 */
		public float total_similarity;
		/**
		 * The number of files that have been matched to this one for this code block.
		 */
		public int similar_files;
		/**
		 * The position of the block being referenced.
		 */
		public Tuple<Integer, Integer> lines;

		/**
		 * Constructor, adds first match info set and the relevant line position.
		 * @param similarity The similarity score of the first matched pair.
		 * @param lines The line positions of the referenced section.
		 */
		public FileInfo(float similarity, Tuple<Integer, Integer> lines) {
			total_similarity = similarity;
			similar_files = 1;
			this.lines = lines;
		}

		/**
		 * Adds a new similarity score to the total and keeps count of how many times a score has been added.
		 * @param similarity the score to be added.
		 */
		public void addToFileInfo(float similarity) {
			total_similarity += similarity;
			similar_files++;
		}
	}
}
