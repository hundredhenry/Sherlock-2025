package uk.ac.warwick.dcs.sherlock.engine.report;

import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlock;
import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.report.IReportGenerator;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.api.component.ISubmission;

import java.util.*;

public class ReportGenerator implements IReportGenerator<SubmissionMatchGroup> {

	ReportGenerator() {
	}

	@Override
	public List<SubmissionMatchGroup> generateSubmissionComparison(List<ISubmission> submissions, List<? extends ICodeBlockGroup> codeBlockGroups) {
		List<SubmissionMatch> matchList = new ArrayList<>();
		for (ICodeBlockGroup codeBlockGroup : codeBlockGroups) {
			List<SubmissionMatchItem> items = new ArrayList<>();

			for (ICodeBlock codeBlock : codeBlockGroup.getCodeBlocks()) {
				long subId = codeBlock.getFile().getSubmission().getId();
				if (submissions.get(0).getId() == subId || submissions.get(1).getId() == subId) {						
					items.add(new SubmissionMatchItem(codeBlock.getFile(), codeBlock.getBlockScore(), codeBlock.getLineNumbers(), codeBlock.getInternalSkeletonCode()));
				}
			}

			if (!items.isEmpty()) {
				String reason = codeBlockGroup.getComment() != null ? codeBlockGroup.getComment() : "Plagiarism detected";
				matchList.add(new SubmissionMatch(reason, items));
			}
		}

		if (matchList.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.singletonList(new SubmissionMatchGroup(matchList, "Plagiarism Matches"));
	}

	@Override
	public ITuple<List<SubmissionMatchGroup>, String> generateSubmissionReport(ISubmission submission, List<? extends ICodeBlockGroup> codeBlockGroups, float subScore) {
		// Build the summary string based on overall score
		String summary;
		if (subScore < 0.01f)
			summary = "No plagiarism was detected in this submission.";
		else if (subScore < 0.05f)
			summary = "Some potential plagiarism was detected, but it is small enough that it may be a false alarm or negligible.";
		else if (subScore < 0.2f)
			summary = "A small amount of plagiarism was detected in this submission.";
		else if (subScore < 0.5f)
			summary = "A significant amount of plagiarism was detected in this submission.";
		else
			summary = "A large portion of this submission contains plagiarism.";

		List<SubmissionMatch> matchList = new ArrayList<>();
		Set<Long> subIdsConnected = new HashSet<>();

		for (ICodeBlockGroup codeBlockGroup : codeBlockGroups) {
			List<SubmissionMatchItem> items = new ArrayList<>();

			for (ICodeBlock codeBlock : codeBlockGroup.getCodeBlocks()) {
				items.add(new SubmissionMatchItem(codeBlock.getFile(), codeBlock.getBlockScore(), codeBlock.getLineNumbers(), codeBlock.getInternalSkeletonCode()));

				if (!submission.getContainedFiles().contains(codeBlock.getFile()))
					subIdsConnected.add(codeBlock.getFile().getSubmission().getId());
			}

			String reason = codeBlockGroup.getComment() != null ? codeBlockGroup.getComment() : "Plagiarism detected";
			matchList.add(new SubmissionMatch(reason, items));
		}

		if (!subIdsConnected.isEmpty())
			summary += "\nIn total, this submission has content that may be plagiarised from up to " + subIdsConnected.size() + " other submissions.";

		List<SubmissionMatchGroup> matchGroups = matchList.isEmpty()
				? Collections.emptyList()
				: Collections.singletonList(new SubmissionMatchGroup(matchList, "Plagiarism Matches"));
		return new Tuple<>(matchGroups, summary);
	}
}
