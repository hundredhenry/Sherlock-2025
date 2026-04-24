package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.CommonTokenStream;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParser;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ParseTreeArtifact;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.ExecutorUtils;

public class ParseTreeGeneratorJava implements IAdvancedPreProcessor<JavaLexer> {

    @Override
    public ParseTreeArtifact process(JavaLexer lexer) {

        lexer.reset();
        JavaParser parser = ExecutorUtils.configureAntlrParser(new JavaParser(new CommonTokenStream(lexer)));

        Tree tree = parser.compilationUnit();

        return new ParseTreeArtifact(tree, parser);
    }
}
