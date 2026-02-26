package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.PreProcessingStrategy;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGenerator;

/**
 * AST-based plagiarism detector.
 * <p>
 * This detector parses source files into abstract syntax trees and compares
 * their structural similarity. It is effective at detecting plagiarism where
 * variable names, formatting, or comments have been changed but the underlying
 * code structure remains the same.
 * </p>
 * <p>
 * Extends {@link PairwiseDetector} to automatically generate a worker for
 * each pair of files in the submission set.
 * </p>
 */
public class ASTDetector extends PairwiseDetector<ASTDetectorWorker> {

    /**
     * The minimum structural similarity score (0 to 1) required for two
     * AST subtrees to be considered a match.
     * <p>
     * Higher values require closer structural matches; lower values are
     * more permissive.
     * </p>
     */
    @AdjustableParameter(
            name = "Similarity Threshold",
            defaultValue = 0.7f,
            minimumBound = 0.0f,
            maximumBound = 1.0f,
            step = 0.01f,
            description = "The minimum structural similarity for two AST subtrees to be considered a match. Higher values require closer matches."
    )
    public float similarityThreshold;

    /**
     * The minimum number of AST nodes a subtree must contain to be
     * considered for comparison.
     * <p>
     * This filters out trivially small code fragments (e.g. single
     * statements) that would produce false positives.
     * </p>
     */
    @AdjustableParameter(
            name = "Minimum Subtree Size",
            defaultValue = 5,
            minimumBound = 1,
            maximumBound = 50,
            step = 1,
            description = "The minimum number of AST nodes in a subtree for it to be considered for comparison. Filters out trivially small fragments."
    )
    public int minimumSubtreeSize;

    /**
     * Constructs the AST detector with metadata, worker class, and preprocessing strategy.
     */
    public ASTDetector() {
        super("AST Detector", "Detects plagiarism by comparing the abstract syntax tree structures of source files",
                ASTDetectorWorker.class, PreProcessingStrategy.of("parseTree", ParseTreeGenerator.class));
    }
}
