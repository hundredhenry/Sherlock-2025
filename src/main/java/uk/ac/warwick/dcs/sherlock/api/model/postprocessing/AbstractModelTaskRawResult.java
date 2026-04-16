package uk.ac.warwick.dcs.sherlock.api.model.postprocessing;

import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.module.model.base.detection.AbstractMatch;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.method.P;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
	 * If there is skeleton code within a match, then this will track the locations of such, passing it
	 * eventually to the report generator to make sure it isnt highlighted.
	 */
	protected HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> file1InternalSkeletonCode;

	protected HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> file2InternalSkeletonCode;



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

		this.file1InternalSkeletonCode = new HashMap<>();
		this.file2InternalSkeletonCode = new HashMap<>();
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
	 * Gets the internal skeleton associated with the provided file number, either 1 or 2
	 * @param fileNum which file to get the internal skeleton code for
	 * @return a HashMap of ITuple keys, representing ranges of code blocks, and an associated HashSet of ITuple ranges
	 * 			which correspond to the ranges of skeleton code which lie fully within the code blocks
	 */
	public synchronized HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> getInternalSkeletonCode(int fileNum){
		if (fileNum == 1){
			return this.file1InternalSkeletonCode;
		}else{
			return this.file2InternalSkeletonCode;
		}
	}

	/**
	 * Getter for the number of matches stored in the object.
	 * @return The number of matches stored in the object.
	 */
	public synchronized int getSize() {
		return this.objects.size();
	}

	/**
	 * Removed two ranges of lines from the list of lines in the result
	 * @param line A paired ITuple of <<file1start, file1end>, <file2start, file2end>> to remove from
	 *             the list of lines in the result
	 */
	public synchronized void removeLine(PairedTuple<Integer, Integer, Integer, Integer> line) {
		//for each current pair of lines in the result, going backwards so we can remove as we go
		for (int i=this.locations.size()-1; i>=0; i--) {//O(m)
			//get the lines
			PairedTuple<Integer, Integer, Integer, Integer> location = this.locations.get(i);
			//and remove the first files lines
			List<ITuple<Integer, Integer>> newRanges1 = removePairFrom(location.getPoint1(), line.getPoint1());
			//this will either return one or two tuples. If its two, its guaranteed to be proper ranges.
			if (newRanges1.size()==1){
				//if its only one, then need to check if its a flag to remove the whole line
				if (newRanges1.get(0).getKey()==-1){
					//if it is, then remove the match
					this.locations.remove(i);
					this.objects.remove(i);
					continue;
				}
			}

			//then do the same for the second file
			List<ITuple<Integer, Integer>> newRanges2 = removePairFrom(location.getPoint2(), line.getPoint2());
			if (newRanges2.size()==1){
				if (newRanges2.get(0).getKey()==-1){
					this.locations.remove(i);
					this.objects.remove(i);
					continue;
				}
			}
		
			//check if we have two ranges
			if (newRanges1.size()==2){
				//then we have internal skeleton code
				if (this.file1InternalSkeletonCode.containsKey(newRanges1.get(0))){
					this.file1InternalSkeletonCode.get(newRanges1.get(0)).add(newRanges1.get(1));
				}else{
					HashSet<ITuple<Integer, Integer>> temp = new HashSet<>();
					temp.add(newRanges1.get(1));
					this.file1InternalSkeletonCode.put(newRanges1.get(0), temp);
				}
			}

			if (newRanges2.size()==2){
				//then we have internal skeleton code
				if (this.file2InternalSkeletonCode.containsKey(newRanges2.get(0))){
					this.file2InternalSkeletonCode.get(newRanges2.get(0)).add(newRanges2.get(1));
				}else{
					HashSet<ITuple<Integer, Integer>> temp = new HashSet<>();
					temp.add(newRanges2.get(1));
					this.file2InternalSkeletonCode.put(newRanges2.get(0), temp);
				}
			}

			ITuple<Integer, Integer> temp;
			HashSet<ITuple<Integer, Integer>> list = new HashSet<>();
			boolean change = true;
			HashSet<ITuple<Integer, Integer>> ISCs = new HashSet<>();
			//if there was a change
			if (!newRanges1.get(0).equals(this.locations.get(i).getPoint1())){
				//then for each ISC for that range
				if (this.file1InternalSkeletonCode.containsKey(this.locations.get(i).getPoint1())){
					temp = newRanges1.get(0);
					ISCs = this.file1InternalSkeletonCode.get(this.locations.get(i).getPoint1());
					while (change){
						change = false;
						list = new HashSet<>();
						for (ITuple<Integer, Integer> isc : ISCs) {
							//check if it is now on the side
							List<ITuple<Integer, Integer>> newList = removePairFrom(temp,isc);
							//if it isnt, then append to a new list, and continue
							if (newList.size()==2){
								//then still interal
								list.add(isc);
							}else{
								// if it is, then mark the fact there has been a change, 
								change = true;
								//if its only one, then need to check if its a flag to remove the whole line
								if (newList.get(0).getKey()==-1){
									//if it is, then remove the match
									this.locations.remove(i);
									this.objects.remove(i);
									continue;
								}
								temp = newList.get(0);
							}
						//continue until the end.
						}
					//if there was a change, then do the whole thing again
					ISCs = list;
					}
					//if there wasnt, append the new range to the hashmaps, with the new final range, and remove the old range
					if (this.file1InternalSkeletonCode.containsKey(temp)){
						for (ITuple<Integer, Integer> item : list) {
							this.file1InternalSkeletonCode.get(temp).add(item);
						}
					}else{
						if (list.size()>0){
							this.file1InternalSkeletonCode.put(temp, list);
						}
					}
					newRanges1.set(0, temp);
				}
			}
			//if there wasnt a change, then the ranges cant have changed, so internal is the same

			//do the same thing for the second file

			list = new HashSet<>();
			ISCs = new HashSet<>();
			change = true;
			//if there was a change
			if (!newRanges2.get(0).equals(this.locations.get(i).getPoint2())){
				//then for each ISC for that range
				if (this.file2InternalSkeletonCode.containsKey(this.locations.get(i).getPoint2())){
					temp = newRanges2.get(0);
					ISCs = this.file2InternalSkeletonCode.get(this.locations.get(i).getPoint2());
					while (change){
						change = false;
						list = new HashSet<>();
						for (ITuple<Integer, Integer> isc : ISCs) {
							//check if it is now on the side
							List<ITuple<Integer, Integer>> newList = removePairFrom(temp,isc);
							//if it isnt, then append to a new list, and continue
							if (newList.size()==2){
								//then still interal
								list.add(isc);
							}else{
								// if it is, then mark the fact there has been a change, 
								change = true;
								//if its only one, then need to check if its a flag to remove the whole line
								if (newList.get(0).getKey()==-1){
									//if it is, then remove the match
									this.locations.remove(i);
									this.objects.remove(i);
									continue;
								}
								temp = newList.get(0);
							}
						//continue until the end.
						}
					//if there was a change, then do the whole thing again
					ISCs = list;
					}
					//if there wasnt, append the new range to the hashmaps, with the new final range, and remove the old range
					if (this.file2InternalSkeletonCode.containsKey(temp)){
						for (ITuple<Integer, Integer> item : list) {
							this.file2InternalSkeletonCode.get(temp).add(item);
						}
					}else{
						if (list.size()>0){
							this.file2InternalSkeletonCode.put(temp, list);
						}
					}
					newRanges2.set(0, temp);
				}
			}

			//add the pair to the list of locations
			ITuple<Integer, Integer> start = newRanges1.get(0);
			ITuple<Integer, Integer> end = newRanges2.get(0);
			this.locations.add(new PairedTuple<Integer,Integer,Integer,Integer>(start,end));
			//and create a new match with updated locations
			T match = this.objects.get(i).copy();
			match.setLines(new PairedTuple<Integer,Integer,Integer,Integer>(start,end));
			//and add the match to the list of matches
			this.objects.add(match);

			//then finally remove the original pair of lines
			this.locations.remove(i);
			this.objects.remove(i);
		}
	}

	/**
	 * Takes two ranges as two tuples, the first being the current range, and the second being the range of values to remove
	 * @param original The current range, in the form of a ITuple <start, end>
	 * @param toRemove The range to remove, in the form of a ITuple <start, end>
	 * @return A list of ranges, which contain the ranges present in the original range, but not the range to remove. Returns
	 * 		<-1,-1> if there are no values in the original range left
	 */
	private List<ITuple<Integer, Integer>> removePairFrom(ITuple<Integer, Integer> original, ITuple<Integer, Integer> toRemove) {
		//define our start and ending points to be more readable
		Integer a1 = original.getKey(); //original start
		Integer b1 = original.getValue(); //original end
		Integer a2 = toRemove.getKey(); //to remove start
		Integer b2 = toRemove.getValue(); //to remove end

		//and initialise our return list
		List<ITuple<Integer, Integer>> list = new ArrayList<>();

		//if the start of the original range is greater than the end of the range to remove,
		// or if the start of the range to remove is greater than the end of the original range
		if (a1 > b2 || a2 > b1) {
			//then no values in toRemove exist in original, so just return the original
			list.add(original);
			return list;
		}
		//if the start of the range to remove is less than or equal to the start of the original range
		// and the end of the range to remove is greater than or equal to the end of the original range
		if (a2 <=a1 && b2>=b1){
			//then the values in toRemove fully encompass the original range, so just return a flag to remove the whole line
			list.add(new Tuple<>(-1,-1));
			return list;
		}

		//if the start of the range to remove is greater than or equal to the start of the original range
		// and the end of the range to remove is less than or equal to the end of the original range
		if (a2>=a1 && b2<=b1){
			//then we have a change to create two new ranges
			//if the start of the range to remove is greater than the start of the original range
			if (a2-1>=a1){
				//then add a new range from the start of the original range to the start of the range to remove
				list.add(new Tuple<>(a1,a2-1));
			}
			//if the end of the range to remove is less than the end of the original range
			if (b2+1<=b1){
				//then add a new range from the end of the range to remove to the end of the original range
				list.add(new Tuple<>(b2+1,b1));
			}

			//if lsit is two, ie we have made two locations, then ignore this and just return the original, along with
			// the range to remove - we will deal with this manually on return
			if (list.size()==2){
				list.clear();
				list.add(original);
				list.add(toRemove);
				return list;
			}

			//if the list is empty, then its because a1=a2, and b1=b2, (which cant happen as we check for this just above
			// but will keep regardless), so add a flag to remove the whole line
			if (list.size()==0){
				list.add(new Tuple<>(-1,-1));
			}
			return list;
		}
		//at this point, the toRemove range exists across a boundary either below or above the original range
		//if the start of the range to remove is greater than the start of the original range
		if (a2-1>=a1){
			//then we know anything above a2 will be removed, so add a range from the start of original to a2
			list.add(new Tuple<>(a1,a2-1));
			return list;
		}
		//and the opposite if the end of the range to remove is less than the end of the original range
		if (b2+1<=b1){
			list.add(new Tuple<>(b2+1,b1));
			return list;
		}
		//if we get here, then something has gone wrong, so just remove the line.
		list.add(new Tuple<>(-1,-1));
		return list;
	}

	/**
	 * Cleans the internal skeleton code hashmaps, removing any entry's that are no longer in locations, and
	 * removing any possible duplicates
	 */
	public void cleanInternalSkeletonCode(){
		//make some temp hashmaps for the final internal skeleton code
		HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> tempFile1ISC = new HashMap<>();
		HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> tempFile2ISC = new HashMap<>();
		//for each location
		for (int i=0; i<this.locations.size(); i++){
			//get the location
			PairedTuple<Integer, Integer, Integer, Integer> location = this.locations.get(i);

			//then for the first file, check if there is any internal skeleton code registered for this location
			if (this.file1InternalSkeletonCode.containsKey(location.getPoint1())){
				//if so, get the internal skeleton code for it
				HashSet<ITuple<Integer, Integer>> file1ISC = this.file1InternalSkeletonCode.get(location.getPoint1());
				//and then add the internal skeleton code to the range within the temporary hashmaps
				if (tempFile1ISC.containsKey(location.getPoint1())){
					tempFile1ISC.get(location.getPoint1()).addAll(file1ISC);
				}else{
					tempFile1ISC.put(location.getPoint1(), file1ISC);
				}
			}

			//and do the same thing for the second file
			if(this.file2InternalSkeletonCode.containsKey(location.getPoint2())){
				HashSet<ITuple<Integer, Integer>> file2ISC = this.file2InternalSkeletonCode.get(location.getPoint2());
				if (tempFile2ISC.containsKey(location.getPoint2())){
					tempFile2ISC.get(location.getPoint2()).addAll(file2ISC);
				}else{
					tempFile2ISC.put(location.getPoint2(), file2ISC);
				}
			}
		}
		//then update the actual attribute
		this.file1InternalSkeletonCode = tempFile1ISC;
		this.file2InternalSkeletonCode = tempFile2ISC;

		//then update all of the objects to have the same cleaned internal skeleton code
		for (int i=0; i<this.objects.size(); i++){
			this.objects.get(i).setInternalSkeletonCode(
				this.file1InternalSkeletonCode.get(this.locations.get(i).getPoint1()), 
				this.file2InternalSkeletonCode.get(this.locations.get(i).getPoint2()));
		}
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
