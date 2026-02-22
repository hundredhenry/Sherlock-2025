package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.IPostProcessor;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.ModelTaskProcessedResults;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTMatch;

import java.util.List;

/**
 * Post-processor for AST-based detection results.
 * <p>
 * Converts raw AST match data into scored code block groups that can
 * be displayed in reports. Handles filtering of common code patterns
 * that appear across many submissions.
 * </p>
 */
public class ASTPostProcessor implements IPostProcessor<ASTRawResult> {

    /**
     * Threshold for filtering common code.
     * <p>
     * If a code structure appears in more than this fraction of all files,
     * it is considered common (e.g. boilerplate or skeleton code) and is
     * excluded from results.
     * Set to 1.0 to disable filtering.
     * </p>
     */
    @AdjustableParameter(
            name = "Common Threshold",
            defaultValue = 0.3f,
            minimumBound = 0.0f,
            maximumBound = 1.0f,
            step = 0.01f,
            description = "If a structural pattern appears in more than this fraction of files, it will be ignored. Increase for small file sets."
    )
    public float commonThreshold;

    /**
     * Processes raw AST comparison results into scored code block groups.
     * <p>
     * TODO: Implement the following steps:
     * <ol>
     *   <li>Aggregate matches from all file pairs</li>
     *   <li>Group matches that refer to the same code structures</li>
     *   <li>Filter out groups that appear in too many files (common code)</li>
     *   <li>Score remaining groups and add them to the results</li>
     * </ol>
     * </p>
     *
     * @param files      the list of all source files being compared
     * @param rawResults the raw results from all AST detector workers
     * @return the processed and scored results
     */
    @Override
    public ModelTaskProcessedResults processResults(List<ISourceFile> files, List<ASTRawResult> rawResults) {
        ModelTaskProcessedResults results = new ModelTaskProcessedResults();

        for (ASTRawResult rawResult : rawResults) {
            List<ASTMatch> matches = rawResult.getMatches();

            for (ASTMatch match : matches) {
                // TODO: Implement grouping logic — check if this match belongs
                //  to an existing group or needs a new one

                // TODO: Implement common code filtering using commonThreshold

                ICodeBlockGroup group = results.addGroup();

                // Add code blocks for both files in the match
                group.addCodeBlock(match.files[0], match.similarity, match.lines.get(0));
                group.addCodeBlock(match.files[1], match.similarity, match.lines.get(1));

                group.setComment("AST Structural Match");

				// Remove empty groups
                if (group.getCodeBlocks().isEmpty()) {
                    results.removeGroup(group);
                }
            }
        }

        return results;
    }
}
