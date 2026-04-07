package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.IPostProcessor;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.ModelTaskProcessedResults;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTMatch;

import java.util.ArrayList;
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
     * Matches whose line ranges overlap in any file are merged into a shared
     * group, mirroring the NGramPostProcessor grouping strategy. This prevents
     * the same line range appearing in N separate single-pair groups (one per
     * comparison partner), which would cause the PoolExecutorJob normalisation
     * to divide every score by ~N.
     * </p>
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


    /* Find a group that already covers an overlapping line range for the given file. */
    private ICodeBlockGroup findOverlappingGroup(Map<Long, List<RangeEntry>> rangesByFile, long fileId, int start, int end) {
        List<RangeEntry> entries = rangesByFile.get(fileId);
        if (entries == null) return null;
        for (RangeEntry entry : entries) {
            if (start <= entry.end && entry.start <= end) {
                return entry.group;
            }
        }
        return null;
    }

    /* Record a line range and its associated group for a file. */
    private void addRange(Map<Long, List<RangeEntry>> rangesByFile, long fileId, int start, int end, ICodeBlockGroup group) {
        rangesByFile.computeIfAbsent(fileId, k -> new ArrayList<>()).add(new RangeEntry(start, end, group));
    }

    private static class RangeEntry {
        final int start;
        final int end;
        final ICodeBlockGroup group;

        RangeEntry(int start, int end, ICodeBlockGroup group) {
            this.start = start;
            this.end = end;
            this.group = group;
        }
    }

}