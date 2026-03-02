package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.TrimWhitespaceOnly;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ASTArtifact;
import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;

/**
 * AST-based plagiarism detector.
 * <p>
 * Parses source files into abstract syntax trees (ASTs) and compares
 * their structural similarity.
 * </p>
 */
public class ASTDetector extends PairwiseDetector<ASTDetector.ASTDetectorWorker> {

    @AdjustableParameter(
            name = "Similarity Threshold",
            defaultValue = 0.7f,
            minimumBound = 0.0f,
            maximumBound = 1.0f,
            step = 0.01f,
            description = "Minimum structural similarity for two AST subtrees to be considered a match."
    )
    public float similarityThreshold;

    @AdjustableParameter(
            name = "Minimum Subtree Size",
            defaultValue = 5,
            minimumBound = 1,
            maximumBound = 50,
            step = 1,
            description = "Minimum number of AST nodes in a subtree for comparison."
    )
    public int minimumSubtreeSize;

    public ASTDetector() {
        super("AST Detector",
                "Detects plagiarism by comparing abstract syntax tree structures of source files",
                ASTDetectorWorker.class,
                PreProcessingStrategy.of("no_whitespace", TrimWhitespaceOnly.class));
    }

    /**
     * Worker class that performs AST-based comparison for a single file pair.
     */
    public class ASTDetectorWorker extends PairwiseDetectorWorker<ASTRawResult> {

        public ASTDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
            super(parent, file1Data, file2Data);
        }

        /**
         * Core execution method.
         * Parses both files into ASTs, compares their structures, and records matches (GumTree anchor/container mappings and/or BFS mappings)
         */
        @Override
        public void execute() {

            // Gets each line as a string in the list, as returned by the specified preprocessor
            ASTArtifact artiF1 = (ASTArtifact) this.file1.getPreProcessedArtifact("ast");
            ASTArtifact artiF2 = (ASTArtifact) this.file2.getPreProcessedArtifact("ast");

            // TODO: Replace with actual AST parsing

            this.result = new ASTRawResult(file1.getFile(), file2.getFile());
        }
    }
}