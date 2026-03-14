package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTRawResult;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ParseTreeArtifact;

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
        ParseTreeArtifact treeF1 = (ParseTreeArtifact) this.file1.getPreProcessedArtifact("parseTree");
        ParseTreeArtifact treeF2 = (ParseTreeArtifact) this.file2.getPreProcessedArtifact("parseTree");

        // Create raw result container
        ASTRawResult result = new ASTRawResult(this.file1.getFile(), this.file2.getFile());

        // TODO: Parse files into AST representations

        // TODO: Extract subtrees for comparison

        // TODO: Compare subtrees and record matches using result.put(...)

        this.result = result;
    }
}
