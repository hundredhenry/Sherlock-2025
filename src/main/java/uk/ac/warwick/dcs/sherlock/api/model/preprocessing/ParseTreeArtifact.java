package uk.ac.warwick.dcs.sherlock.api.model.preprocessing;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.Tree;
import uk.ac.warwick.dcs.sherlock.api.util.IPreprocessArtifact;

public record ParseTreeArtifact(
        Tree tree,
        Parser parser
) implements IPreprocessArtifact {}