package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.AbstractModelTaskRawResult;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SimpleObjectEqualityRawResult<T extends Serializable> extends AbstractModelTaskRawResult<T> {


	private final int file1NumObjs;
	private final int file2NumObjs;


	public SimpleObjectEqualityRawResult(ISourceFile file1, ISourceFile file2, int numObjectsFile1, int numObjectsFile2) {
		super(file1,file2);

		this.file1NumObjs = numObjectsFile1;
		this.file2NumObjs = numObjectsFile2;
	}

	
	public int getFile1NumObjects() {
		return file1NumObjs;
	}

	
	public int getFile2NumObjects() {
		return file2NumObjs;
	}


	// public String toString() {
	// 	StringBuilder str = new StringBuilder();
	// 	str.append(this.getFile1().getFileDisplayName()).append(" vs ").append(this.getFile2().getFileDisplayName()).append("\n\r");
	// 	for (int i = 0; i < this.size; i++) {
	// 		str.append(this.objects.get(i).toString()).append(" - ").append(this.locations.get(i).toString()).append("\n\r");
	// 	}
	// 	return str.toString();
	// }
}
