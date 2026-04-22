package uk.ac.warwick.dcs.sherlock.module.model.base.detection;

import uk.ac.warwick.dcs.sherlock.api.util.IndexedString;
import uk.ac.warwick.dcs.sherlock.api.model.detection.IDetector;
import uk.ac.warwick.dcs.sherlock.api.model.detection.ModelDataItem;
import uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker;
import uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing.SimpleObjectEqualityRawResult;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.StringMatch;
import uk.ac.warwick.dcs.sherlock.api.model.preprocessing.LineListArtifact;

import java.util.List;

public class VariableNameDetectorWorker extends PairwiseDetectorWorker<SimpleObjectEqualityRawResult<StringMatch>> {

	public VariableNameDetectorWorker(IDetector parent, ModelDataItem file1Data, ModelDataItem file2Data) {
		super(parent, file1Data, file2Data);
	}

	@Override
	public void execute() {
		// This detector finds and matches up variables - it only works on declarations of the variable, not every time the variable is called.

		LineListArtifact artiF1 = (LineListArtifact) this.file1.getPreProcessedArtifact("variables");
		LineListArtifact artiF2 = (LineListArtifact) this.file2.getPreProcessedArtifact("variables");

		List<IndexedString> linesF1 = artiF1.lines();
		List<IndexedString> linesF2 = artiF2.lines();

		SimpleObjectEqualityRawResult<StringMatch> res = new SimpleObjectEqualityRawResult<>(this.file1.getFile(), this.file2.getFile(), linesF1.size(), linesF2.size());

		for (IndexedString checkLine : linesF1) {
			linesF2.stream().filter(x -> x.valueEquals(checkLine)).forEach(
				x -> res.put(
				new StringMatch(checkLine.getKey(), checkLine.getKey(), x.getKey(), x.getKey(), checkLine.getValue(),
							this.file1.getFile(), this.file2.getFile()),
						checkLine.getKey(), x.getKey()));
		}

		this.result = res;
	}
}
