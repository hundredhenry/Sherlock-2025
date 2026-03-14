package uk.ac.warwick.dcs.sherlock.api.model.preprocessing;

import org.antlr.v4.runtime.Lexer;
import uk.ac.warwick.dcs.sherlock.api.util.IPreprocessArtifact;

/**
 * Advanced preprocessor implementation, used to directly access and preprocess from a specific lexer
 * @param <T> Antlr lexer implementation (compiled)
 */
public interface IAdvancedPreProcessor<T extends Lexer> {

	/**
	 * Pre-process with a lexer
	 *
	 * @param lexer lexer instance
	 *
	 * @return preprocessed artifact
	 */
	IPreprocessArtifact process(T lexer);

}
