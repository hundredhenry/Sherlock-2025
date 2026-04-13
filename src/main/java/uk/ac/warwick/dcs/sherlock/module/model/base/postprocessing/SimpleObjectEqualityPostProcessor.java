package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlockGroup;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.IPostProcessor;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.ModelTaskProcessedResults;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.StringMatch;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.StringMatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleObjectEqualityPostProcessor implements IPostProcessor<SimpleObjectEqualityRawResult> {

	/*@AdjustableParameter (name = "Test Param", defaultValue = 0, minimumBound = 0, maximumBound = 10, step = 1)
	public int testParam;*/

	@Override
	public ModelTaskProcessedResults processResults(List<ISourceFile> files, List<SimpleObjectEqualityRawResult> rawResults) {
		ModelTaskProcessedResults results = new ModelTaskProcessedResults();
		Map<ISourceFile, Integer> totals = new HashMap<>();
		results.setFileTotals(totals);

		Map<String, ICodeBlockGroup> map = new HashMap<>();
		for (SimpleObjectEqualityRawResult<StringMatch> res : rawResults) {
			totals.putIfAbsent(res.getFile1(), res.getFile1NumObjects());
			totals.putIfAbsent(res.getFile2(), res.getFile2NumObjects());

			for (int i = 0; i < res.getSize(); i++) {
				StringMatch match = (StringMatch) res.getObject(i);
				String o = match.getString();
				ICodeBlockGroup group;

				if (!map.containsKey(o)) {
					group = results.addGroup();
					group.setComment("Variable: " + o.toString());
					map.put(o, group);
				}
				else {
					group = map.get(o);
				}

				group.addCodeBlock(res.getFile1(), 1, res.getLocation(i).getPoint1(),res.getInternalSkeletonCode(1).get(res.getLocation(i).getPoint1())); //If file already present it will append to the existing files lines object
				group.addCodeBlock(res.getFile2(), 1, res.getLocation(i).getPoint2(), res.getInternalSkeletonCode(2).get(res.getLocation(i).getPoint2())); // ""
			}
		}

		return results;
	}
}
