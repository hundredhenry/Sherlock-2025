package uk.ac.warwick.dcs.sherlock.engine.storage;

import uk.ac.warwick.dcs.sherlock.api.component.ICodeBlock;
import uk.ac.warwick.dcs.sherlock.api.component.ISourceFile;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ICodeBlock object for base storage implementation
 */
@Entity (name = "CodeBlock")
public class EntityCodeBlock implements ICodeBlock, Serializable {

	private static final long serialVersionUID = 1L;

	private EntityFile file;
	private float score;

	private int size;
	private List<Integer> lines;

	@ElementCollection
	private HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> internalSkeletonCode;

	private Integer subtreeWeight;

	EntityCodeBlock() {
		super();
	}

	/**
	 * Constructor for Entity Code Block class, taking in a singlar range and associated internal skeleton code
	 * @param file The EntityFile that this code block belongs to
	 * @param score The score for the code block
	 * @param lines An ITuple representing the start and end range of the code block, inclusive
	 * @param internalSkeletonCode A HashSet representing tuple ranges of any skeleton code fully enclosed within the
	 *                             code block
	 */
	EntityCodeBlock(EntityFile file, float score, ITuple<Integer, Integer> lines, HashSet<ITuple<Integer, Integer>> internalSkeletonCode) {
		//run super constructor
		super();
		//assign attributes
		this.file = file;
		this.score = score;
		//initialise size + lines
		this.size = 0;
		this.lines = new ArrayList<>();
		//then add new line
		this.addLineToList(lines);

		//finally initialise internal skeleton code hashmap
		this.internalSkeletonCode = new HashMap<>();
		// check if there actually is any internal skeleton code available for this range
		if (internalSkeletonCode != null){
			if (internalSkeletonCode.size() != 0){
				//if there is, then add it to the hashmap
				this.internalSkeletonCode.put(new Tuple<>(lines.getKey(), lines.getValue()), internalSkeletonCode);
			}
		}

	}

	// Overloaded constructor for AST-based code blocks
	EntityCodeBlock(EntityFile file, float score, ITuple<Integer, Integer> lines, Integer subtreeWeight,
					HashSet<ITuple<Integer, Integer>> internalSkeletonCode) {
		this(file, score, lines, internalSkeletonCode);
		this.subtreeWeight = subtreeWeight;
	}

	/**
	 * Constructor for Entity Code Block class, taking in a list of ranges and associated internal skeleton code
	 * @param file The EntityFile that this code block belongs to
	 * @param score The score for the code block
	 * @param lines A list of ITuples representing the start and end range of all code blocks, inclusive
	 * @param internalSkeletonCode A list of HashSet representing tuple ranges of any skeleton code fully enclosed
	 *                             within the associated code block
	 */
	EntityCodeBlock(EntityFile file, float score, List<ITuple<Integer, Integer>> lines,
					List<HashSet<ITuple<Integer, Integer>>> internalSkeletonCode) {
		super();
		this.file = file;
		this.score = score;

		this.size = 0;
		this.lines = new ArrayList<>();
		//add each line to the lines list
		lines.forEach(this::addLineToList);
		//initialise the internal skeleton code hashmap
		this.internalSkeletonCode = new HashMap<>();
		//for each range
		for (int i = 0; i < lines.size(); i++){
			//check if there is internal skeleton code for the range
			if (internalSkeletonCode.get(i) == null) continue;
			if (internalSkeletonCode.get(i).size() == 0) continue;
			//if so, add it into the hashmap
			this.internalSkeletonCode.put(new Tuple<>(lines.get(i).getKey(), lines.get(i).getValue()), internalSkeletonCode.get(i));
		}
	}

	@Override
	public float getBlockScore() {
		return this.score;
	}

	@Override
	public ISourceFile getFile() {
		return this.file;
	}

	@Override
	public List<ITuple<Integer, Integer>> getLineNumbers() {
		return IntStream.range(0, this.size).mapToObj(this::getLineFromList).collect(Collectors.toList());
	}

	@Override
	public HashMap<ITuple, HashSet<ITuple<Integer, Integer>>> getInternalSkeletonCode() {
		return this.internalSkeletonCode;
	}

	/**
	 * Appends a new range, and associated internal skeleton code to the code block
	 * @param score The score for this range
	 * @param lines An ITuple representing the start and end range of the lines, inclusive
	 * @param internalSkeletonCode A HashSet representing any skeleton code fully contained within the range
	 */
	void append(float score, ITuple<Integer, Integer> lines, HashSet<ITuple<Integer, Integer>> internalSkeletonCode) {
		this.score = ((this.score * this.size) + score) / (this.size + 1); //new avg score
		this.addLineToList(lines);
		//check if there is any associated internal skeleton code for the range
		if (internalSkeletonCode == null) return;
		if (internalSkeletonCode.size() == 0) return;
		//if so, check if this range already has internal skeleton code
		if (!this.internalSkeletonCode.containsKey(new Tuple<>(lines.getKey(), lines.getValue()))){
			//if not, then create a new entry within the hashmap
			this.internalSkeletonCode.put(new Tuple<>(lines.getKey(), lines.getValue()), internalSkeletonCode);
		}else{
			//if yes, then just append all the new internal skeleton code into the range
			this.internalSkeletonCode.get(new Tuple<>(lines.getKey(), lines.getValue())).addAll(internalSkeletonCode);
		}
	}

	/**
	 * Appends a new list of ranges, and associated internal skeleton code for each range to the code block
	 * @param score The score for this list of ranges
	 * @param lines An list of ITuples representing the start and end range of the lines, inclusive
	 * @param internalSkeletonCode A list of HashSets representing any skeleton code fully contained within the
	 *                             associated range
	 */
	void append(float score, List<ITuple<Integer, Integer>> lines, List<HashSet<ITuple<Integer, Integer>>> internalSkeletonCode) {
		this.score = ((this.score * this.size) + score) / (this.size + lines.size()); //new avg score
		lines.forEach(this::addLineToList);

		//if no internal skeleton code at all, just return early
		if (internalSkeletonCode == null) return;
		if (internalSkeletonCode.size() == 0) return;
		//for each range
		for (int i = 0; i < lines.size(); i++){
			//add the internal skeleton code to the range if any exists
			if (internalSkeletonCode.get(i) == null) continue;
			if (internalSkeletonCode.get(i).size() == 0) continue;
			if (!this.internalSkeletonCode.containsKey(new Tuple<>(lines.get(i).getKey(), lines.get(i).getValue()))){
				this.internalSkeletonCode.put(new Tuple<>(lines.get(i).getKey(), lines.get(i).getValue()), internalSkeletonCode.get(i));
			}else{
				this.internalSkeletonCode.get(new Tuple<>(lines.get(i).getKey(), lines.get(i).getValue())).addAll(internalSkeletonCode.get(i));
			}
		}
	}

	void markRemove() {
		this.size = -5;
	}

	private void addLineToList(ITuple<Integer, Integer> line) {
		if (line == null || line.getKey() == null || line.getValue() == null) {
			BaseStorage.logger.warn("Null line tuple added to code block");
			return;
		}

		if (this.size == 0 || this.getLineNumbers().stream().noneMatch(line::equals)) {
			this.lines.add(line.getKey());
			this.lines.add(line.getValue());
			this.size++;
		}
	}

	private ITuple<Integer, Integer> getLineFromList(int index) {
		return new Tuple<>(this.lines.get(index * 2), this.lines.get((index * 2) + 1));
	}

	@Override
	public Integer getSubtreeWeight() {
		return this.subtreeWeight; // if null, then weight is not set, so this block is not from an AST-based algorithm
	}
}
