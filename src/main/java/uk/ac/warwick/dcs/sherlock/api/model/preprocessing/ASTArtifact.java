package uk.ac.warwick.dcs.sherlock.api.model.preprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.ASTNode;
import uk.ac.warwick.dcs.sherlock.api.util.IPreprocessArtifact;

public record ASTArtifact(
        ASTNode ast
) implements IPreprocessArtifact {}