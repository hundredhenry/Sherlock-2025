package uk.ac.warwick.dcs.sherlock.module.model.base.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.AbstractModelTaskRawResult;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the set of match objects for a pair of inputted files.
 * @param <T> N-Gram match object comparing similarity data between 2 code blocks
 */
public class NGramRawResult<T extends Serializable> extends AbstractModelTaskRawResult {

	/**
	 * The ID number of the first file in the compared pair.
	 */
	long file1id;
	/**
	 * The ID number of the second file in the compared pair.
	 */
	long file2id;

	/**
	 * The list of match objects (containers).
	 * private ensures that only NGramRawResult can do thread-safe modifications on objects directly
	 */
	private final List<T> objects; 
	/**
	 * The list of file block locations. Stored in the form of 2 pairs, each pair denoting the start and end line of
	 * the code block in the respective file. Stored in form List &lt;PairedTuple&lt;Integer, Integer, Integer, Integer&gt;&gt;.
	 */
	private final List<PairedTuple<Integer, Integer, Integer, Integer>> locations;

	/**
	 * Object constructor, saves the compared file ids, initialises interior lists as ArrayLists, and sets size to zero.
	 * @param file1 File ID of the first file in the compared pair.
	 * @param file2 File ID of the second file in the compared pair.
	 */
	public NGramRawResult(ISourceFile file1, ISourceFile file2) {
		
		this.file1id = file1.getPersistentId();
		this.file2id = file2.getPersistentId();

		this.objects = new ArrayList<>();
		this.locations = new ArrayList<>();
	}

	/**
	 * Getter for ID of first file in comparison pair.
	 * @return First ID.
	 */
	public ISourceFile getFile1() {
		return SherlockHelper.getSourceFile(file1id);
	}

	/**
	 * Getter for ID of second file in comparison pair.
	 * @return Second ID.
	 */
	public ISourceFile getFile2() {
		return SherlockHelper.getSourceFile(file2id);
	}
	/**
	 * Returns true if no matches in object, false otherwise.
	 * @return True if no matches in object, false otherwise.
	 * SYNCHRONISED thread-locked
	 */
	@Override
	public boolean isEmpty() {
		synchronized (this) {
			return this.objects.size() == 0;
		}
	}

	/**
	 * Method to store a matched block.
	 * @param object The match object for the matched pair.
	 * @param file1BlockStart The start line of the block in File1.
	 * @param file1BlockEnd The end line of the block in File1.
	 * @param file2BlockStart The start line of the block in File2.
	 * @param file2BlockEnd The end line of the block in File2.
	 */
	public synchronized void put(T object, int file1BlockStart, int file1BlockEnd, int file2BlockStart, int file2BlockEnd) {
		if (this.objects.size() != this.locations.size()) {
			System.out.println(String.format("Object Size [%d], Location Size [%d], This is not sized (NGramRawResult)",this.objects.size(), this.locations.size())); // NEW-CHANGE
			return;
		} 

		this.objects.add(object);
		this.locations.add(new PairedTuple<>(file1BlockStart, file1BlockEnd, file2BlockStart, file2BlockEnd));
	}

	/**
	 * Method to store a matched block where both file blocks are a single line.
	 * @param object The mach object for the matched pair.
	 * @param file1Loc The line number of the File1 block.
	 * @param file2Loc The line number of the File2 block.
	 * SYNCHRONISED -> atomic list updates
	 */
	public void put(T object, int file1Loc, int file2Loc) {
		this.put(object, file1Loc, file1Loc, file2Loc, file2Loc);
	}

	/**
	 * Get the list of match objects within the container.
	 * @return The list of match objects within the container.
	 * Safe-read via unmodifiable snapshot that isn't mutable
	 */
	public synchronized List<T> getObjects() {
		return List.copyOf(objects);
	}

	/**
	 * Getter for the block location indexes in the order: File1 start, File1 end, File2 start, File2 end.
	 * @return Block location indexes.
	 * Safe-read
	 */
	public synchronized List<PairedTuple<Integer, Integer, Integer, Integer>> getLocations() {
		return List.copyOf(locations);
	}

	/**
	 * Getter for the number of matches stored in the object.
	 * @return The number of matches stored in the object.
	 */
	public synchronized int getSize() {
		return this.objects.size();
	}

	/**
	 * Verifies that the inputted RawResult type is the same type as the current object.
	 * @param baseline the baseline object, in the set, current instance must be of the same exact type as this.
	 * @return True if input is same object type as current object, false otherwise.
	 */
	@Override
	public synchronized boolean testType(AbstractModelTaskRawResult baseline) {
		if (baseline instanceof NGramRawResult) {
			NGramRawResult bl = (NGramRawResult) baseline;
			return bl.getObjects().get(0).getClass().equals(this.getObjects().get(0).getClass()); // Check generic type is the same
		}

		return false;
	}

	/**
	 * Returns the string form of the list of stored objects along with their locations in their respective files.
	 * @return The string form of the list of stored objects along with their locations in their respective files.
	 */
	@Override
	public synchronized String toString() {
		if (this.objects.size() != this.locations.size()) {
			System.err.printf("[toString() MISMATCH] objects-size=%d, locations=%d",
				this.objects.size(), this.locations.size());
		} // NEW-CHANGE

		StringBuilder str = new StringBuilder();
		int n = this.objects.size();

		for (int i = 0; i < n; i++) {
			str.append(this.objects.get(i).toString()).append(" - ").append(this.locations.get(i).toString()).append("\n");
		}
		return str.toString();
	}
}
