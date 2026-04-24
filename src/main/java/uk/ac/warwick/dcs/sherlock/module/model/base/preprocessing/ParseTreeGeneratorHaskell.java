package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.CommonTokenStream;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellParser;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ParseTreeArtifact;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.ExecutorUtils;

public class ParseTreeGeneratorHaskell implements IAdvancedPreProcessor<HaskellLexer> {

    @Override
    public ParseTreeArtifact process(HaskellLexer lexer) {

        lexer.reset();
        HaskellParser parser = ExecutorUtils.configureAntlrParser(new HaskellParser(new CommonTokenStream(lexer)));

        Tree tree = parser.module();

        return new ParseTreeArtifact(tree, parser);
    }
}
