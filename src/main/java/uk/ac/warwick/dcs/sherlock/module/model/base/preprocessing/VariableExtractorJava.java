package uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.LineListArtifact;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IAdvancedPreProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParser;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaParserBaseListener;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.ExecutorUtils;

import java.util.LinkedList;
import java.util.List;

public class VariableExtractorJava implements IAdvancedPreProcessor<JavaLexer> {

	@Override
	public LineListArtifact process(JavaLexer lexer) {
		List<IndexedString> fields = new LinkedList<>();

		lexer.reset();
		JavaParser parser = ExecutorUtils.configureAntlrParser(new JavaParser(new CommonTokenStream(lexer)));

		ParseTreeWalker.DEFAULT.walk(new JavaParserBaseListener() {
			//globals
			@Override
			public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
				fields.add(new IndexedString(ctx.start.getLine(), ctx.getText().split("=")[0]));
			}

			//locals
			@Override
			public void enterLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
				fields.add(new IndexedString(ctx.start.getLine(), ctx.getText().split("=")[0]));
			}
		}, parser.compilationUnit());

		//System.out.println("field -> " + fields.toString());
		return new LineListArtifact(fields);
	}
}
