package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParser;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParserBaseListener;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.ParseTreeArtifact;

import java.util.LinkedList;
import java.util.List;

public class ASTGenerator implements IAdvancedPreProcessor<JavaLexer> {

    @Override
    public ParseTreeArtifact process(JavaLexer lexer) {

        lexer.reset();
        JavaParser parser = new JavaParser(new CommonTokenStream(lexer));

        Tree tree = parser.compilationUnit();

        // TODO - Convert parse tree into AST

        return new ParseTreeArtifact(tree, parser);
    }
}