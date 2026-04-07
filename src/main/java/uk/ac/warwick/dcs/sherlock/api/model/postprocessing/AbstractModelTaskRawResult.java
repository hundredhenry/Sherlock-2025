package uk.ac.warwick.dcs.sherlock.api.model.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


import java.util.ArrayList;

/**
 * Raw results storage class, acts a stored cache. Data structure from this can be directly accessed in post-processing
 * <p>
 * Must be serializable!!!
 */
public abstract class AbstractModelTaskRawResult<T extends AbstractMatch<T>> implements Serializable {

	@Serial
	private static final long serialVersionUID = 24L;

	/**
	 * the id of the first file in the comparison pair
	 */
	protected long file1id;
	
	/**
	 * the id of the second file in the comparison pair
	 */
	protected long file2id;
	
	/**
	 * the list of match objects (whatever object used by the implementation)
	 */
	protected List<T> objects;
	
	/**
	 * the list of file block locations. Stored in the form of 2 pairs, each pair denoting the start and end line of
	 * the code block in the respective file. Stored in form List &lt;PairedTuple&lt;Integer, Integer, Integer, Integer&gt;&gt;.
	 */
	protected List<PairedTuple<Integer, Integer, Integer, Integer>> locations;


	/**
	 * Object constructor, saves the compared file ids, initialises interior lists as ArrayLists, and sets size to zero.
	 * @param file1 File ID of the first file in the compared pair.
	 * @param file2 File ID of the second file in the compared pair.
	 */
	public AbstractModelTaskRawResult(ISourceFile file1, ISourceFile file2) {
		
		this.file1id = file1.getPersistentId();
		this.file2id = file2.getPersistentId();

		this.objects = new ArrayList<>();
		this.locations = new ArrayList<>();
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
	 * Get the list of match objects within the container.
	 * @return The list of match objects within the container.
	 * Safe-read via unmodifiable snapshot that isn't mutable
	 */
	public synchronized List<T> getObjects() {
		return List.copyOf(objects);
	}

	/**
	 * Get the object at the index
	 * @param index the index of the object
	 * @return the object at the index
	 * Safe-read via unmodifiable snapshot that isn't mutable
	 */
	public synchronized T getObject(int index) {
		return this.objects.get(index);
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
	 * Get the location at the index
	 * @param index the index of the location
	 * @return the location at the index
	 * Safe-read via unmodifiable snapshot that isn't mutable
	 */
	public synchronized PairedTuple<Integer, Integer, Integer, Integer> getLocation(int index) {
		return this.locations.get(index);
	}

	/**
	 * Getter for the number of matches stored in the object.
	 * @return The number of matches stored in the object.
	 */
	public synchronized int getSize() {
		return this.objects.size();
	}

	public synchronized void removeLine(PairedTuple<Integer, Integer, Integer, Integer> line) {
		for (int i=this.locations.size()-1; i>=0; i--) {//O(m)
			PairedTuple<Integer, Integer, Integer, Integer> location = this.locations.get(i);
			List<Tuple<Integer, Integer>> newRanges1 = removePairFrom(location.getPoint1(), line.getPoint1());
			if (newRanges1.size()==1){
				if (newRanges1.get(0).getKey()==-1){
					this.locations.remove(i);
					this.objects.remove(i);
					continue;
				}
			}

			List<Tuple<Integer, Integer>> newRanges2 = removePairFrom(location.getPoint2(), line.getPoint2());
			if (newRanges2.size()==1){
				if (newRanges2.get(0).getKey()==-1){
					this.locations.remove(i);
					this.objects.remove(i);
					continue;
				}
			}
		
			//otherwise, we have legitimate ranges. 
			for (Tuple<Integer, Integer> range1 : newRanges1) {//O(1)
				for (Tuple<Integer, Integer> range2 : newRanges2) {
					this.locations.add(new PairedTuple<>(range1,range2));
					T match = this.objects.get(i).copy();
					match.setLines(new PairedTuple<>(range1,range2));
					this.objects.add(match);
				}
			}

			this.locations.remove(i);
			this.objects.remove(i);
		}
	}

	private List<Tuple<Integer, Integer>> removePairFrom(Tuple<Integer, Integer> original, Tuple<Integer, Integer> toRemove) {
		Integer a1 = original.getKey();
		Integer b1 = original.getValue();
		Integer a2 = toRemove.getKey();
		Integer b2 = toRemove.getValue();


		List<Tuple<Integer, Integer>> list = new ArrayList<>();


		if (a1 > b2 || a2 > b1) {
			list.add(original);
			return list;
		}
		if (a2 <=a1 && b2>=b1){
			list.add(new Tuple<>(-1,-1));
			return list;
		}
		if (a2>=a1 && b2<=b1){
			if (a2-1>=a1){
				list.add(new Tuple<>(a1,a2-1));
			}
			if (b2+1<=b1){
				list.add(new Tuple<>(b2+1,b1));
			}
			if (list.size()==0){
				list.add(new Tuple<>(-1,-1));
			}
			return list;
		}
		if (a2-1>=a1){
			list.add(new Tuple<>(a1,a2-1));
			return list;
		}
		if (b2+1<=b1){
			list.add(new Tuple<>(b2+1,b1));
			return list;
		}
		list.add(new Tuple<>(-1,-1));
		return list;
	}

	/**
	 * Returns true if no matches in object, false otherwise.
	 * @return True if no matches in object, false otherwise.
	 * SYNCHRONISED thread-locked
	 */
	public boolean isEmpty() {
		synchronized (this) {
			return this.objects.size() == 0;
		}
	}

	/**
	 * Verifies that the inputted RawResult type is the same type as the current object.
	 * @param baseline the baseline object, in the set, current instance must be of the same exact type as this.
	 * @return True if input is same object type as current object, false otherwise.
	 */
	public synchronized boolean testType(AbstractModelTaskRawResult baseline) {
		if (baseline == null) return false;

		if (!baseline.getClass().equals(this.getClass())) return false;

		if (this.objects.isEmpty() || baseline.getObjects().isEmpty()) return false;

		return this.objects.get(0).getClass().equals(baseline.getObjects().get(0).getClass());
	}

	/**
	 * Returns the string form of the list of stored objects along with their locations in their respective files.
	 * @return The string form of the list of stored objects along with their locations in their respective files.
	 */
	public synchronized String toString() {
		if (this.objects.size() != this.locations.size()) {
			System.err.printf("[toString() MISMATCH] objects-size=%d, locations=%d",
				this.objects.size(), this.locations.size());
		}

		StringBuilder str = new StringBuilder();
		int n = this.objects.size();

		for (int i = 0; i < n; i++) {
			str.append(this.objects.get(i).toString()).append(" - ").append(this.locations.get(i).toString()).append("\n");
		}
		return str.toString();
	}
}
