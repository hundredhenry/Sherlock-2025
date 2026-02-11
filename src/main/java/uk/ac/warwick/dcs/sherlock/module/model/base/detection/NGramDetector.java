package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.LineListArtifact;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector.NGramDetectorWorker;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.TrimWhitespaceOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class NGramDetector extends PairwiseDetector<NGramDetectorWorker> {

	/**
	 * The character width of each N-Gram used in the detection.
	 * <p>
	 * In theory smaller is more sensitive, but realistically you don't want to use lower than 3 or higher than 8.
	 * </p>
	 */
	@AdjustableParameter (name = "N-Gram Size", defaultValue = 4, minimumBound = 1, maximumBound = 10, step = 1, description = "The width in characters of each N-Gram. Smaller is more sensitive.")
	public int ngram_size;
	/**
	 * The minimum size of a list of N-Grams before checks begin.
	 * <p>
	 * N-Grams are put into a linked list when being matched, to prevent a match being detected for a short number of N-Grams (e.g. picking up things like a for loop) a minimum window size is used.
	 * Before this size is reached if the match ends then nothing is flagged.
	 * </p>
	 */
	@AdjustableParameter (name = "Minimum Window", defaultValue = 5, minimumBound = 0, maximumBound = 20, step = 1, description = "The minimum number of N-Grams that can be detected as a matched block. Character width of minimum block is N-Gram size + minimum window - 1.")
	public int minimum_window;
	/**
	 * The threshold on the similarity value over which something is considered suspicious.
	 * <p>
	 * The 2 lists of N-Grams are compared to produce a similaity value between 0 and 1, with 1 being identical. This threshold decides at what point to consider a segment as similar, and when it's
	 * long enough to consider it possible plagerism.
	 * </p>
	 */
	@AdjustableParameter (name = "Threshold", defaultValue = 0.8f, minimumBound = 0.0f, maximumBound = 1.0f, step = 0.001f, description = "The threshold on the similarity at which a block of code will be no longer considered similar. This determines where the similarity ends, 1 will give only pure matches, 0 will match anything")
	public float threshold;

	/**
	 * Sets meta data for the detector, along with providing the API with pointers to the Worker and the Preprocessing Strategy
	 */
	public NGramDetector() {
		super("N-Gram Detector", "N-Gram implementation", NGramDetectorWorker.class, PreProcessingStrategy.of("no_whitespace", TrimWhitespaceOnly.class));
	}

	/**
	 * Compare 2 lists of N-Grams and return a similarity metric
	 * <p>
	 * Finds the Jaccard Similarity of the 2 lists of Ngrams
	 * </p>
	 *
	 * @param string1 The reference N-Gram list
	 * @param string2 The check N-Gram list
	 *
	 * @return The float val for Jaccard Similarity
	 */
	public float compare(ArrayList<NGram> string1, ArrayList<NGram> string2) {
		// Build frequency maps (multisets) for both n-gram lists
		HashMap<String, Integer> bag1 = new HashMap<>();
		HashMap<String, Integer> bag2 = new HashMap<>();
		
		for (NGram ngram : string1) {
			bag1.merge(ngram.getNgram(), 1, (a, b) -> a + b);
		}
		for (NGram ngram : string2) {
			bag2.merge(ngram.getNgram(), 1, (a, b) -> a + b);
		}

		// Calculate multiset Jaccard similarity: |intersection| / |union|
		int intersection = 0;
		int union = 0;

		// Get all unique n-gram strings from both bags
		HashSet<String> allKeys = new HashSet<>(bag1.keySet());
		allKeys.addAll(bag2.keySet());

		// For each n-gram string, count how many times it appears in intersection and union
		for (String key : allKeys) {
			int count1 = bag1.getOrDefault(key, 0);
			int count2 = bag2.getOrDefault(key, 0);
			intersection += Math.min(count1, count2);  // intersection uses minimum count
			union += Math.max(count1, count2);         // union uses maximum count
		}

		// Handle empty input case
		if (union == 0) return 0.0f;

		return (float) intersection / union;
	}

	// add line markers
	public void matchFound(NGramRawResult<NGramMatch> res, ArrayList<NGram> reference, ArrayList<NGram> check, NGram head, float last_peak, int since_last_peak, ISourceFile file1, ISourceFile file2) {
		// take out values back to the last peak
		for (int i = 0; i < since_last_peak; i++) {
			reference.remove(reference.size() - 1);
			check.remove(check.size() - 1);
		}
		// if the last peak is before the minimum window size skip the match construction (ignore case)
		if (reference.size() >= minimum_window) {
			// build an N-Gram match object to send to the post processor
			NGramMatch temp =
					new NGramMatch(reference.get(0).getLineNumber(), reference.get(reference.size() - 1).getLineNumber(), check.get(0).getLineNumber(), check.get(check.size() - 1).getLineNumber(),
							last_peak, file1, file2);
			// put an N-Gram match into res along wih the start points of the segment in reference file then checked file.
			res.put(temp, reference.get(0).getLineNumber(), reference.get(reference.size() - 1).getLineNumber(), check.get(0).getLineNumber(), check.get(check.size() - 1).getLineNumber());
		}

		// empties the lists for next detection
		reference.clear();
		check.clear();
	}

	/**
	 * Load the contents of a file into a linked list of N-Grams for easy reference
	 * <p>
	 * Each line of the file is taken and converted into N-Grams which are in turn put into a linked list as N-Gram objects containing the N-Gram and it's line number
	 * </p>
	 *
	 * @param storage_list The list the N-Grams are going to be stored in
	 * @param file         The list of lines in a file to be converted and stored
	 */
	private void loadNGramList(ArrayList<NGram> storage_list, ArrayList<IndexedString> file) {
		// the N-Gram string
		String substr;
		// the new N-Gram object
		NGram ngram = null;
		int line_number = 0;

		// variable to extract the string from the indexed container
		String line;
		// for each line get the indexed container
		for (IndexedString lineC : file) {
			// acquire line
			line = lineC.getValue();
			// to prevent N-Gram matches across lines
			ngram = null;
			// if line is shorter than the ngram_size pad it with whitespace
			// this should function without issue as an equivalent lines will also be too short and be padded the same
			if (line.length() < ngram_size) {
				// pad to the size of an ngram
				for (int i = ngram_size - line.length(); i > 0; i--) {
					line += " ";
				}
			}
			// acquire line number
			line_number = lineC.getKey();
			// for each N-Gram in a line
			for (int i = 0; i < line.length() - (ngram_size - 1); i++) {
				// build an N-Gram of ngram_size
				substr = line.substring(i, i + ngram_size);
				// create the next N-Gram object with its line number
				ngram = new NGram(substr, line_number);
				// add ngram to the list
				storage_list.add(ngram);
			}
		}
	}

	/**
	 * Load the contents of a file into an N-Gram map for easy retrieval
	 * <p>
	 * Each line of the file is taken in and converted into N-Grams, then stored in a hash map as an object containing the N-Gram, its line number, and the next N-Gram in the file (modeled as
	 * a linked list). Duplicate N-Grams are stored in a list under the same key.
	 * </p>
	 *
	 * @param storage_map The hashmap used to store the resulting N-Grams
	 * @param file        The file data to be deconstructed into and stored as ordered N-Grams
	 */
	private void loadNGramMap(HashMap<String, ArrayList<NGram>> storage_map, ArrayList<IndexedString> file) {
		// the N-Gram string
		String substr;
		// the new N-Gram object
		NGram ngram = null;
		int line_number = 0;

		// variable to extract the string from the indexed container
		String line;
		// for each line get the indexed container
		for (IndexedString lineC : file) {
			// acquire line
			line = lineC.getValue();
			// if line is shorter than the ngram_size pad it with whitespace
			// this should function without issue as an equivalent lines will also be too short and be padded the same
			if (line.length() < ngram_size) {
				// pad to the size of an ngram
				for (int i = ngram_size - line.length(); i > 0; i--) {
					line += " ";
				}
			}
			// acquire line number
			line_number = lineC.getKey();

			// for each N-Gram in a line
			for (int i = 0; i < line.length() - (ngram_size - 1); i++) {
				// build an N-Gram of ngram_size
				substr = line.substring(i, i + ngram_size);
				// if the N-Gram is the first
				if (ngram == null) {
					// build the N-Gram as an object with its line number
					ngram = new NGram(substr, line_number);
					// get or create list for this N-Gram string
					ArrayList<NGram> ngramList = storage_map.computeIfAbsent(substr, k -> new ArrayList<>());
					// add to the list
					ngramList.add(ngram);
				}
				// if at least 1 N-Gram already exists
				else {
					// create the next N-Gram object with its line number
					NGram temp = new NGram(substr, line_number);
					// set temp as the next N-Gram in the order
					ngram.setNextNgram(temp);
					// update the current N-Gram position
					ngram = temp;
					// get or create list for this N-Gram string
					ArrayList<NGram> ngramList = storage_map.computeIfAbsent(substr, k -> new ArrayList<>());
					// add to the list
					ngramList.add(ngram);
				}
			}
		}
	}

	/**
	 * The main processing method used in the detector
	 */
	public class NGramDetectorWorker extends PairwiseDetectorWorker<NGramRawResult> {

		public NGramDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
			super(parent, file1Data, file2Data);
		}

		/**
		 *
		 */
		@Override
		public void execute() {

			// Gets each line as a string in the list, as returned by the specified preprocessor
			LineListArtifact artiF1 = (LineListArtifact) this.file1.getPreProcessedArtifact("no_whitespace");
			LineListArtifact artiF2 = (LineListArtifact) this.file2.getPreProcessedArtifact("no_whitespace");

			ArrayList<IndexedString> linesF1 = new ArrayList<IndexedString>(artiF1.lines());
			ArrayList<IndexedString> linesF2 = new ArrayList<IndexedString>(artiF2.lines());

			// make raw result output container
			NGramRawResult<NGramMatch> res = new NGramRawResult<>(this.file1.getFile(), this.file2.getFile());

			// generate the N-Grams for file 1 and load them into a hash map
			HashMap<String, ArrayList<NGram>> storage_map = new HashMap<>();
			loadNGramMap(storage_map, linesF1);
			// generate the N-Grams for file 2 and load them into a list
			ArrayList<NGram> storage_list = new ArrayList<NGram>();
			loadNGramList(storage_list, linesF2);

			// start of file check
			NGram substrObj;
			ArrayList<NGram> reference = new ArrayList<NGram>();
			ArrayList<NGram> check = new ArrayList<NGram>();
			NGram head = null;

			// the value of similarity the last peak held
			float last_peak = 0.0f;
			// the last val for similarity held
			float last_val = 0.0f;
			// the val for similarity held
			float sim_val = 1.0f;
			// the number of steps made since the last time last peak was updated
			int since_last_peak = 0;

			// the counter for duplicate N-Grams. Used to keep track of which version is being refered to for comparison
			int ngram_id = 0;

			// the check N-Gram string
			String ngram_string;

			for (int i = 0; i < storage_list.size(); i++) {
				// acquire ngram
				substrObj = storage_list.get(i);
				// get N-Gram string
				ngram_string = substrObj.getNgram();

				// if the previous file has a matching ngram (id references the occurrence of said ngram if there are duplicates)
				ArrayList<NGram> ngramList = storage_map.get(ngram_string);
				boolean hasMatch = (ngramList != null && ngram_id < ngramList.size());
				if (hasMatch || reference.size() > 0) {
					// build up a window and threshold similarity
					// if over threshold keep increasing window by 1 until similarity drops bellow threshold

					// if head is null we are starting a new comparison check
					if (head == null) {
						// set head to the start of the sequence in the reference file
						head = ngramList.get(ngram_id);
						// add the reference start to the reference list
						reference.add(head);
					}
					// otherwise we update reference and head
					else {
						// get the next ngram in the reference sequence
						head = head.getNextNgram();
						// if sequence has ended
						if (head == null) {
							// EOF in reference reached, abandon loop and then check for match (post loop check)
							break;
						}
						// add next in sequence to list
						else {
							reference.add(head);
						}

					}
					// add the N-Gram to check
					check.add(substrObj);

					// update peak data
					// this allows retraction to last peak in the case of the similarity falling below the threshold
					// this prevents detection bleeding
					// compare the two lists
					sim_val = compare(reference, check);
					// if the similarity has risen we have a new peak
					if (sim_val >= last_val) {
						since_last_peak = 0;
						last_peak = sim_val;
					} else {
						since_last_peak++;
					}
					// update last val for use in next iteration
					last_val = sim_val;

					// nothing substantial has flagged, reset lists
					if (reference.size() == minimum_window && sim_val < threshold) {
						// if another case of the starting N-Gram exists in the other file move to that and reperform the check
						ArrayList<NGram> startNgramList = storage_map.get(reference.get(0).getNgram());
						if (startNgramList != null && (ngram_id + 1) < startNgramList.size()) {
							// move file position back to appropriate N-Gram
							i -= minimum_window;
							ngram_id++;
						}
						else {
							// no more duplicates to check, reset ngram_id
							ngram_id = 0;
						}
						// empty lists
						reference.clear();
						check.clear();
						head = null;
						// reset peak value trackers
						since_last_peak = 0;
						last_val = 0.0f;
						last_peak = 0.0f;
					}
					// when the window is over the minimum and drops below the threshold, save the match
					else if (reference.size() > minimum_window && sim_val < threshold) {
						// send the data to construct a match object for the found match
						matchFound(res, reference, check, head, last_peak, since_last_peak, this.file1.getFile(), this.file2.getFile());
						// reset duplicate ngram ID
						ngram_id = 0;
						// set head to null so a new reference can be made
						head = null;
						// reset peak value trackers
						since_last_peak = 0;
						last_val = 0.0f;
						last_peak = 0.0f;
					}
				}
			}
			// performs comparison if EOF for reference is reached
			if (compare(reference, check) > threshold && reference.size() >= minimum_window) {
				// if at EOF there is a match then output it
				matchFound(res, reference, check, head, last_peak, since_last_peak, this.file1.getFile(), this.file2.getFile());
			}

			// data of type Serializable, essentially raw data stored as a variable.
			this.result = res;
		}
	}

	/**
	 * Object to store N-Gram data in a refined structure.
	 */
	class NGram {

		/**
		 * The N-Gram itself in string form.
		 */
		private String segment;
		/**
		 * The line the N-Gram starts on.
		 */
		private int line_number;

		/**
		 * Linked List pointer to allow the next N-Gram in a reference file to be found when the start is acquired from a hashmap.
		 */
		private NGram next_ngram;

		/**
		 * Object constructor.
		 *
		 * @param segment     The N-Gram being stored.
		 * @param line_number The line number the N-Gram starts on.
		 */
		public NGram(String segment, int line_number) {
			this.segment = segment;
			this.line_number = line_number;
		}

		/**
		 * Checks if 2 N-Grams are the same string.
		 *
		 * @param ngram The N-Gram to compare to.
		 *
		 * @return True if strings are equal, false otherwise.
		 */
		public boolean equals(NGram ngram) {
			return this.segment.equals(ngram.getNgram());
		}

		/**
		 * @return The line number at the start of the N-Gram.
		 */
		public int getLineNumber() {
			return line_number;
		}

		/**
		 * @return The next N-Gram in the file.
		 */
		public NGram getNextNgram() {
			return next_ngram;
		}

		/**
		 * @param ngram The next N-Gram in the file.
		 */
		public void setNextNgram(NGram ngram) {
			next_ngram = ngram;
		}

		/**
		 * @return The N-Gram string.
		 */
		public String getNgram() {
			return segment;
		}

	}
}

// NOTE this will give the one way comparison, to get the other direction it must be run with the files reversed

// TODO finish commenting
// TODO clean
