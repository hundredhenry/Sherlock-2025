package uk.ac.warwick.dcs.sherlock.api.model.detection;

import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.IPreprocessArtifact;

import java.util.HashMap;
import java.util.Map;

/**
 * Object to store the preprocessed data passed to the detector
 */
public class ModelDataItem {

	private final ISourceFile file;
	private final Map<String, IPreprocessArtifact> mapping;

	/**
	 * Build data item for file
	 *
	 * @param file file to build for
	 */
	public ModelDataItem(ISourceFile file) {
		this.file = file;
		this.mapping = new HashMap<>();
	}

	/**
	 * Build data item for file
	 *
	 * @param file file to build for
	 * @param map  map of preprocessed artifacts against the tag for their producing strategy
	 */
	public ModelDataItem(ISourceFile file, Map<String, IPreprocessArtifact> map) {
		this.file = file;
		this.mapping = new HashMap<>(map);
	}

	/**
	 * Adds a mapping for a preprocessing strategy
	 *
	 * @param strategyName tag for strategy
	 * @param artifact     preprocessed artifact
	 */
	public void addPreProcessedArtifact(String strategyName, IPreprocessArtifact artifact) {
		this.mapping.put(strategyName, artifact);
	}

	/**
	 * Get the file this data item for
	 *
	 * @return file
	 */
	public ISourceFile getFile() {
		return this.file;
	}

	/**
	 * get the preprocessed artifact for a strategy, returns null if strategy does not exist
	 *
	 * @param strategyName strategy tag
	 *
	 * @return artifact, null if strategy does not exist
	 */
	public IPreprocessArtifact getPreProcessedArtifact(String strategyName) {
		return this.mapping.get(strategyName);
	}
}
