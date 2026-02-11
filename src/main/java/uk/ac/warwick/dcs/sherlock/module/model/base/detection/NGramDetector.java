package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector.NGramDetectorWorker;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.TrimWhitespaceOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
	 * Compare 2 lists of N-Grams and return a similarity metric.
	 * <p>
	 * Finds the multiset Jaccard Similarity of the 2 lists of N-Grams:
	 * |intersection| / |union|, where counts are used for duplicates.
	 * </p>
	 *
	 * @param string1 The reference N-Gram list
	 * @param string2 The check N-Gram list
	 *
	 * @return The float val for Jaccard Similarity (0.0 to 1.0)
	 */
	public float compare(List<NGram> string1, List<NGram> string2) {
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
	 * Build a reverse index mapping each N-Gram string to the list of positions
	 * where it occurs in the given N-Gram sequence.
	 *
	 * @param ngrams The flat list of N-Grams (as produced by loadNGramList)
	 * @return A map from N-Gram string to list of indices into ngrams
	 */
	private HashMap<String, ArrayList<Integer>> buildIndex(ArrayList<NGram> ngrams) {
		HashMap<String, ArrayList<Integer>> index = new HashMap<>();
		for (int pos = 0; pos < ngrams.size(); pos++) {
			index.computeIfAbsent(ngrams.get(pos).getNgram(), k -> new ArrayList<>()).add(pos);
		}
		return index;
	}

	/**
	 * The main processing method used in the detector
	 */
	public class NGramDetectorWorker extends PairwiseDetectorWorker<NGramRawResult> {

		public NGramDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
			super(parent, file1Data, file2Data);
		}

		/**
		 * Detects similar N-Gram regions between two files using a seed-and-extend approach.
		 * <p>
		 * Algorithm:
		 * 1. Convert both files into flat N-Gram sequences.
		 * 2. Build a reverse index on file 1 (N-Gram string -> positions).
		 * 3. Scan file 2 sequentially. When an N-Gram matches the index, use that
		 *    as a "seed" and extend the match diagonally (advancing in both files
		 *    simultaneously) as long as Jaccard similarity stays above the threshold.
		 * 4. Record any match that reaches at least minimum_window N-Grams.
		 * </p>
		 */
		@Override
		public void execute() {
			// Get preprocessed lines for both files
			ArrayList<IndexedString> linesF1 = new ArrayList<>(this.file1.getPreProcessedLines("no_whitespace"));
			ArrayList<IndexedString> linesF2 = new ArrayList<>(this.file2.getPreProcessedLines("no_whitespace"));

			NGramRawResult<NGramMatch> res = new NGramRawResult<>(this.file1.getFile(), this.file2.getFile());

			// Build flat N-Gram sequences for both files
			ArrayList<NGram> ngramsF1 = new ArrayList<>();
			loadNGramList(ngramsF1, linesF1);
			ArrayList<NGram> ngramsF2 = new ArrayList<>();
			loadNGramList(ngramsF2, linesF2);

			// Build reverse index: N-Gram string -> positions in file 1
			HashMap<String, ArrayList<Integer>> index = buildIndex(ngramsF1);

			// Seed-and-extend: scan file 2 for matching regions
			int i = 0;
			while (i < ngramsF2.size()) {
				String ngram = ngramsF2.get(i).getNgram();
				ArrayList<Integer> seeds = index.get(ngram);

				if (seeds == null) {
					i++;
					continue;
				}

				// Try each seed position in file 1, keep the longest valid match
				int bestLen = 0;
				float bestSim = 0.0f;
				int bestSeed = -1;

				for (int j : seeds) {
					// Extend the match from this seed
					int len = 1;
					float lastValidSim = 0.0f;

					while (j + len < ngramsF1.size() && i + len < ngramsF2.size()) {
						len++;
						// Only evaluate similarity once the window is large enough to matter
						if (len >= minimum_window) {
							float sim = compare(ngramsF1.subList(j, j + len), ngramsF2.subList(i, i + len));
							if (sim < threshold) {
								len--; // step back to last valid length
								break;
							}
							lastValidSim = sim;
						}
					}

					// Keep this seed if it produced a longer match than previous seeds
					if (len >= minimum_window && len > bestLen) {
						bestLen = len;
						bestSim = lastValidSim;
						bestSeed = j;
					}
				}

				if (bestLen >= minimum_window) {
					// Record the match
					int refStart = ngramsF1.get(bestSeed).getLineNumber();
					int refEnd = ngramsF1.get(bestSeed + bestLen - 1).getLineNumber();
					int checkStart = ngramsF2.get(i).getLineNumber();
					int checkEnd = ngramsF2.get(i + bestLen - 1).getLineNumber();

					NGramMatch match = new NGramMatch(
							refStart, refEnd, checkStart, checkEnd,
							bestSim, this.file1.getFile(), this.file2.getFile());
					res.put(match, refStart, refEnd, checkStart, checkEnd);

					// Advance past the matched region in file 2
					i += bestLen;
				} else {
					i++;
				}
			}

			this.result = res;
		}
	}

	/**
	 * Object to store N-Gram data in a refined structure.
	 * Contains the N-Gram string and its originating line number.
	 */
	class NGram {

		private final String segment;
		private final int line_number;

		/**
		 * @param segment     The N-Gram string.
		 * @param line_number The source line number the N-Gram starts on.
		 */
		public NGram(String segment, int line_number) {
			this.segment = segment;
			this.line_number = line_number;
		}

		/**
		 * Checks if 2 N-Grams have the same string.
		 */
		public boolean equals(NGram ngram) {
			return this.segment.equals(ngram.getNgram());
		}

		public int getLineNumber() {
			return line_number;
		}

		public String getNgram() {
			return segment;
		}
	}
}
