package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.IPostProcessor;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.ModelTaskProcessedResults;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTMatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Post-processor for AST-based detection results.
 * <p>
 * Converts raw AST match data into scored code block groups that can
 * be displayed in reports. Matches that cover overlapping line ranges
 * in the same file are merged into a single group (even across different
 * file pairs) so that the downstream scoring normalisation does not
 * deflate scores.
 * </p>
 */
public class ASTPostProcessor implements IPostProcessor<ASTRawResult> {

    /**
     * Processes raw AST comparison results into scored code block groups.
     *
     * @param files      the list of all source files being compared
     * @param rawResults the raw results from all AST detector workers
     * @return the processed and scored results
     */
    @Override
    public ModelTaskProcessedResults processResults(List<ISourceFile> files, List<ASTRawResult> rawResults) {
        ModelTaskProcessedResults results = new ModelTaskProcessedResults();    

        Map<ISourceFile, Integer> totals = new HashMap<>(); // totals is the AST node count of eah file (i.e. the weight of the root node)
        results.setFileTotals(totals);

        for (ASTRawResult rawResult : rawResults) { // per pairwise comparison
            ISourceFile file1 = rawResult.getFile1();
            ISourceFile file2 = rawResult.getFile2();  
            // The fileTotal is the files' AST node count
            totals.putIfAbsent(file1, rawResult.getFile1NodeCount());
            totals.putIfAbsent(file2, rawResult.getFile2NodeCount());


            for (ASTMatch match : rawResult.getMatches()) {
                ICodeBlockGroup group = results.addGroup();

                // Add code blocks for both files in the match
                group.addCodeBlock(match.files[0], match.similarity, match.lines.get(0), match.subtreeWeight1);
                group.addCodeBlock(match.files[1], match.similarity, match.lines.get(1), match.subtreeWeight2);
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
    