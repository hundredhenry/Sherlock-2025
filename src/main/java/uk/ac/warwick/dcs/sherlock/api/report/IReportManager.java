package uk.ac.warwick.dcs.sherlock.api.report;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.component.ISubmission;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;

import java.util.List;
import java.util.Map;

public interface IReportManager<T extends ISubmissionMatchGroup, S extends ISubmissionSummary> {

	/**
	 * To be called by the web report pages. Gets a list of submission summaries.
	 * @return a list of the matching SubmissionSummaries, each containing their ids, overall scores, and a list of the submissions that they were matched with.
	 */
	List<S> getMatchingSubmissions();

	/**
	 * Compares two submissions, finds all the matches in files they contain between them, and returns all relevant information about them.
	 *
	 * @param submissions The submissions to compare (should be a list of two submissions only; any submissions beyond the first two are ignored)
	 * @return A list of SubmissionMatchGroup objects which contain lists of SubmissionMatch objects; each have ids of the two matching files, a score for the match, a reason from the DetectionType, and the line numbers in each file where the match occurs.
	 */
	List<T> getSubmissionComparison(List<ISubmission> submissions);

	/**
	 * Generate a report for a single submission, containing all matches for all files within it, and a summary of the report as a string.
	 *
	 * @param submission The submission to generate the report for.
	 * @return A tuple. The key contains a list of SubmissionMatchGroup objects which contain lists of SubmissionMatch objects; each have objects which contain ids of the two matching files, a score for the match, a reason from the DetectionType, and the line numbers in each file where the match occurs. The value is the report summary.
	 */
	ITuple<List<T>, String> getSubmissionReport(ISubmission submission);

	/**
	 * Get the match scores between files from two submissions.
	 *
	 * @param submission1 The first submission.
	 * @param submission2 The second submission.
	 * @return A map where keys are file pairs (as ITuple of ISourceFile) and values are their match scores.
	 */
	Map<ITuple<ISourceFile, ISourceFile>, Float> getFileMatchScores(ISubmission submission1, ISubmission submission2);

}
