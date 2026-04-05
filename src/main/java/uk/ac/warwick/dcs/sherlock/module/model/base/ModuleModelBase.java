package uk.ac.warwick.dcs.sherlock.module.model.base;

import uk.ac.warwick.dcs.sherlock.api.registry.SherlockRegistry;
import uk.ac.warwick.dcs.sherlock.api.annotation.EventHandler;
import uk.ac.warwick.dcs.sherlock.api.annotation.SherlockModule;
import uk.ac.warwick.dcs.sherlock.api.event.EventInitialisation;
import uk.ac.warwick.dcs.sherlock.api.event.EventPreInitialisation;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.ASTDetector;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.NGramDetector;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.VariableNameDetector;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.JavaLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.lang.HaskellLexer;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTPostProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.ASTRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramPostProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.NGramRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.SimpleObjectEqualityPostProcessor;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.SimpleObjectEqualityRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.CommentExtractor;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.CommentRemover;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.TrimWhitespaceOnly;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.VariableExtractor;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.VariableExtractorJava;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGenerator;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGeneratorJava;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGeneratorHaskell;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ASTGenerator;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ASTGeneratorJava;
import uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ASTGeneratorHaskell;

@SherlockModule
public class ModuleModelBase {

	@EventHandler
	public void initialisation(EventInitialisation event) {
		SherlockRegistry.registerGeneralPreProcessor(CommentExtractor.class);
		SherlockRegistry.registerGeneralPreProcessor(CommentRemover.class);
		SherlockRegistry.registerGeneralPreProcessor(TrimWhitespaceOnly.class);
		SherlockRegistry.registerAdvancedPreProcessorImplementation("uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.VariableExtractor", VariableExtractorJava.class);
		SherlockRegistry.registerAdvancedPreProcessorImplementation("uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGenerator", ParseTreeGeneratorJava.class);
		SherlockRegistry.registerAdvancedPreProcessorImplementation("uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ASTGenerator", ASTGeneratorJava.class);
		SherlockRegistry.registerAdvancedPreProcessorImplementation("uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ParseTreeGenerator", ParseTreeGeneratorHaskell.class);
		SherlockRegistry.registerAdvancedPreProcessorImplementation("uk.ac.warwick.dcs.sherlock.module.model.base.preprocessing.ASTGenerator", ASTGeneratorHaskell.class);

		SherlockRegistry.registerDetector(VariableNameDetector.class);
		SherlockRegistry.registerPostProcessor(SimpleObjectEqualityPostProcessor.class, SimpleObjectEqualityRawResult.class);

		SherlockRegistry.registerDetector(NGramDetector.class);
		SherlockRegistry.registerPostProcessor(NGramPostProcessor.class, NGramRawResult.class);

		SherlockRegistry.registerDetector(ASTDetector.class);
		SherlockRegistry.registerPostProcessor(ASTPostProcessor.class, ASTRawResult.class);

	}

	@EventHandler
	public void preInitialisation(EventPreInitialisation event) {
		SherlockRegistry.registerLanguage("Java", JavaLexer.class);
		SherlockRegistry.registerLanguage("Haskell", HaskellLexer.class);

		SherlockRegistry.registerAdvancedPreProcessorGroup(VariableExtractor.class);
		SherlockRegistry.registerAdvancedPreProcessorGroup(ParseTreeGenerator.class);
		SherlockRegistry.registerAdvancedPreProcessorGroup(ASTGenerator.class);
	}

}
