package uk.ac.warwick.dcs.sherlock.engine.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlock;
import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.component.ISubmission;
import uk.ac.warwick.dcs.sherlock.api.report.IReportGenerator;
import uk.ac.warwick.dcs.sherlock.api.report.IReportManager;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.api.component.IResultJob;

import java.util.*;

/**
 * A class to handle report generation in general (does not generate fileReportMap itself).
 * <p>
 * It takes all possible inputs that may be relevant from postprocessing, and handles requests for fileReportMap; sending the relevant information to the actual report generator in use.
 */
public class ReportManager implements IReportManager<SubmissionMatchGroup, SubmissionSummary> {

	/**
	 * Logger instance
	 */
	private final Logger logger = LoggerFactory.getLogger(ReportManager.class);

	/**
	 * Contains all the information needed to generate reports from.
	 */
	private final IResultJob results;

	/**
	 * A map of all the individual files according to their persitent ids.
	 */
	private final Map<Long, ISourceFile> fileMap;

	/**
	 * Maps submission ids to a list of file ids contained by that submission.
	 */
	private final Map<Long, List<Long>> submissionFileMap;

	/**
	 * Maps submission ids to their overall scores
	 */
	private final Map<Long, Float> submissionScores;

	/**
	 * Maps pairs of file ids to the match scores between them
	 */
	private final Map<List<Long>, Float> fileMatchScores;

	/**
	 * The object used to generate the report information.
	 */
	private IReportGenerator reportGenerator;

	/**
	 * Initialises the report manager with a set of results.
	 * @param results Contains a full set of results from postprocessing; all useful info is extracted from here
	 */
	public ReportManager(IResultJob results) {
		this.reportGenerator = new ReportGenerator();

		this.submissionFileMap = new HashMap<>();
		this.fileMap = new HashMap<>();
		this.results = results;
		this.submissionScores = new HashMap<>();
		this.fileMatchScores = new HashMap<>();
		this.results.getFileResults().forEach(resultFile -> this.fileMap.put(resultFile.getFile().getPersistentId(), resultFile.getFile()));
		this.results.getFileResults().forEach(resultFile -> resultFile.getFileScores().keySet().forEach(file2 -> this.fileMatchScores.put(new ArrayList<Long>(Arrays.asList(resultFile.getFile().getPersistentId(), file2.getPersistentId())), resultFile.getFileScore(file2))));
		this.results.getFileResults().forEach(resultFile -> {
			if (resultFile.getFile() == null) return;
			ISubmission submission = resultFile.getFile().getSubmission();
			if (submission == null) return;
			this.submissionScores.put(submission.getId(), resultFile.getOverallScore());
		});

		fillSubmissionFileMap();
	}

	/**
	 * Retrieve every ICodeBlockGroup stored in results.
	 * @return a list of ICodeBlockGroups.
	 */
	private List<ICodeBlockGroup> getCodeBlockGroups() {
		List<ICodeBlockGroup> codeBlockGroups = new ArrayList<>();
		this.results.getFileResults().stream().flatMap(file -> file.getTaskResults().stream()).filter(task -> task.getContainingBlocks() != null)
				.forEach(task -> codeBlockGroups.addAll(task.getContainingBlocks()));
		return codeBlockGroups;
	}

	/**
	 * Retrieve every ICodeBlockGroup with a file from the specified submission.
	 * @param submission the submission to find relevant ICodeBlockGroups for.
	 * @return a list of relevant ICodeBlockGroups.
	 */
	private List<ICodeBlockGroup> getCodeBlockGroups(ISubmission submission) {
		List<ICodeBlockGroup> codeBlockGroups = new ArrayList<>();
		this.results.getFileResults().stream().flatMap(file -> file.getTaskResults().stream()).filter(task -> task.getContainingBlocks() != null)
				.flatMap(task -> task.getContainingBlocks().stream().filter(group -> group.submissionIdPresent(submission.getId()))).forEach(group -> codeBlockGroups.add(group));
		return codeBlockGroups;
	}

	/**
	 * Retrieve every ICodeBlockGroup with a file from both of the specified submissions.
	 * @param submissions the submissions to find relevant ICodeBlockGroups for.
	 * @return a list of relevant ICodeBlockGroups.
	 */
	private List<ICodeBlockGroup> getCodeBlockGroups(List<ISubmission> submissions) {
		List<ICodeBlockGroup> codeBlockGroups = new ArrayList<>();
		this.results.getFileResults().stream().flatMap(file -> file.getTaskResults().stream()).filter(task -> task.getContainingBlocks() != null)
				.flatMap(task -> task.getContainingBlocks().stream().filter(group -> group.submissionIdPresent(submissions.get(0).getId()) && group.submissionIdPresent(submissions.get(1).getId()))).forEach(group -> codeBlockGroups.add(group));
		return codeBlockGroups;
	}

	/**
	 * Calculates the maximum match score between two submissions by finding the highest score among all file pairs.
	 * @param sub_id1 the id of the first submission.
	 * @param sub_id2 the id of the second submission.
	 * @return the maximum file match score between any pair of files in the two submissions, or 0 if no matches exist.
	 */
	private float getMaxMatchScore(long sub_id1, long sub_id2) {
		float maxScore = 0f;

		for (Long id1 : submissionFileMap.get(sub_id1)) {
			for (Long id2 : submissionFileMap.get(sub_id2)) {
				List<Long> filePair = new ArrayList<>(Arrays.asList(id1, id2));
				float score = fileMatchScores.getOrDefault(filePair, 0f);

				if (score > maxScore) {
					maxScore = score;
				}
			}
		}

		return maxScore;
	}

	/**
	 * Gets the match scores between all pairs of files from two submissions.
	 * @param submission1 the first submission.
	 * @param submission2 the second submission.
	 * @return a map of file pairs (as ITuple<ISourceFile, ISourceFile>) to their match scores.
	 */
	@Override
	public Map<ITuple<ISourceFile, ISourceFile>, Float> getFileMatchScores(ISubmission submission1, ISubmission submission2) {
		Map<ITuple<ISourceFile, ISourceFile>, Float> result = new HashMap<>();

		List<Long> fileIds1 = submissionFileMap.get(submission1.getId());
		List<Long> fileIds2 = submissionFileMap.get(submission2.getId());

		if (fileIds1 == null || fileIds2 == null) {
			return result;
		}

		for (Long id1 : fileIds1) {
			for (Long id2 : fileIds2) {
				List<Long> filePair = new ArrayList<>(Arrays.asList(id1, id2));
				float score = fileMatchScores.getOrDefault(filePair, 0f);

				if (score > 0f) {
					ISourceFile file1 = fileMap.get(id1);
					ISourceFile file2 = fileMap.get(id2);
					result.put(new Tuple<>(file1, file2), score);
				}
			}
		}

		return result;
	}

	/**
	 * Fill in submissionFileMap based off of the contents of fileMap.
	 */
	private void fillSubmissionFileMap() {
		for(ISourceFile file : this.fileMap.values()) {
			//If this is the first file of this submission seen, create a new list and put it into submissionFileMap.
			//otherwise add to the existing list.
			if(submissionFileMap.get(file.getSubmission().getId()) == null) {
				ArrayList<Long> idList = new ArrayList<>();
				idList.add(file.getPersistentId());
				submissionFileMap.put(file.getSubmission().getId(), idList);
			} else {
				submissionFileMap.get(file.getSubmission().getId()).add(file.getPersistentId());
			}
		}
	}

	/**
	 * To be called by the web report pages. Gets a list of submission summaries.
	 * @return a list of the matching SubmissionSummaries, each containing their ids, overall scores, and a list of the submissions that they were matched with.
	 */
	@Override
	public List<SubmissionSummary> getMatchingSubmissions() {
		ArrayList<SubmissionSummary> output = new ArrayList<>();

		for(Long submissionId : submissionFileMap.keySet()) {
			float overallScore = submissionScores.get(submissionId);
			SubmissionSummary submissionSummary = new SubmissionSummary(submissionId, overallScore);
			ArrayList<Tuple<Long, Float>> matchingSubs = new ArrayList<>();
			ArrayList<Long> matchingSubIds = new ArrayList<>();

			//Look through all the code block groups to determine which submissions have been matched with which other submissions.
			for(ICodeBlockGroup codeBlockGroup : getCodeBlockGroups()) {
				//If this group contains a file for the current submission, add all other submissions in the group to the list, if they aren't already added
				if(codeBlockGroup.submissionIdPresent(submissionId)) {
					for(ICodeBlock codeBlock : codeBlockGroup.getCodeBlocks()) {
						long currentId = codeBlock.getFile().getSubmission().getId();
						if(currentId != submissionId && !matchingSubIds.contains(currentId)) {
							float matchScore = getMaxMatchScore(submissionId, currentId);

							matchingSubs.add(new Tuple<>(currentId, matchScore));
							matchingSubIds.add(currentId);
						}
					}
				}

			}
			submissionSummary.addMatchingSubmissions(matchingSubs);
			output.add(submissionSummary);
		}

		return output;
	}

	/**
	 * Compares two submissions, finds all the matches in files they contain between them, and returns all relevant information about them.
	 *
	 * @param submissions The submissions to compare (should be a list of two submissions only; any submissions beyond the first two are ignored)
	 * @return A list of SubmissionMatchGroup objects which contain lists of SubmissionMatch objects; each have ids of the two matching files, a score for the match, a reason from the DetectionType, and the line numbers in each file where the match occurs.
	 */
	@Override
	public List<SubmissionMatchGroup> getSubmissionComparison(List<ISubmission> submissions) {
		if (submissions.stream().anyMatch(ISubmission::hasParent)) {
			// one of the submissions is not top level, don't make reports on non-top level submission.

			//Either just return null with an error msg
			logger.error("Cannot generate comparison on a submission which has a parent");
			return null;

			// or get the parent recursively and generate on that!
			/*ISubmission sub = ...
			while (sub.hasParent()) {
				sub = sub.getParent();
			}*/
		}

		if(submissions.size() < 2)
			return null;


		List<ICodeBlockGroup> relevantGroups = getCodeBlockGroups(submissions);

		return reportGenerator.generateSubmissionComparison(submissions, relevantGroups);
	}

	/**
	 * Generate a report for a single submission, containing all matches for all files within it, and a summary of the report as a string.
	 *
	 * @param submission The submission to generate the report for.
	 * @return A tuple. The key contains a list of SubmissionMatchGroup objects which contain lists of SubmissionMatch objects; each have objects which contain ids of the two matching files, a score for the match, a reason from the DetectionType, and the line numbers in each file where the match occurs. The value is the report summary.
	 */
	@Override
	public ITuple<List<SubmissionMatchGroup>, String> getSubmissionReport(ISubmission submission) {

		if (submission.hasParent()) {
			// submission is not top level, don't make reports on non-top level submission.

			//Either just return null with an error msg
			logger.error("Cannot generate report on a submission which has a parent");
			return null;

			// or get the parent recursively
			/*ISubmission sub = ...
			while (sub.hasParent()) {
				sub = sub.getParent();
			}*/
		}

		List<ICodeBlockGroup> relevantGroups = getCodeBlockGroups(submission);

		//Generate and return the report.
		return reportGenerator.generateSubmissionReport(submission, relevantGroups, submissionScores.get(submission.getId()));
	}

}
