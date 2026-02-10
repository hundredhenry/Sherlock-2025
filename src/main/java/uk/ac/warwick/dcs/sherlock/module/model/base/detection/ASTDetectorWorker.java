package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTRawResult;

import java.util.List;

/**
 * Worker for the AST-based plagiarism detector.
 * <p>
 * Each instance compares a single pair of files by parsing them into
 * abstract syntax trees and comparing subtree structures for similarity.
 * </p>
 */
public class ASTDetectorWorker extends PairwiseDetectorWorker<ASTRawResult> {

    public ASTDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
        super(parent, file1Data, file2Data);
    }

    /**
     * Executes the AST-based comparison between the two files.
     * <p>
     * TODO: Implement the following steps:
     * <ol>
     *   <li>Retrieve pre-processed lines from both files</li>
     *   <li>Parse each file into an AST representation</li>
     *   <li>Extract comparable subtrees from both ASTs</li>
     *   <li>Compare subtrees for structural similarity</li>
     *   <li>Record matches that exceed the similarity threshold</li>
     * </ol>
     * </p>
     */
    @Override
    public void execute() {
        // Retrieve pre-processed lines using the strategy name defined in ASTDetector
        List<IndexedString> linesF1 = this.file1.getPreProcessedLines("no_whitespace");
        List<IndexedString> linesF2 = this.file2.getPreProcessedLines("no_whitespace");

        // Create raw result container
        ASTRawResult result = new ASTRawResult(this.file1.getFile(), this.file2.getFile());

        // TODO: Parse files into AST representations

        // TODO: Extract subtrees for comparison

        // TODO: Compare subtrees and record matches using result.put(...)

        this.result = result;
    }
}
