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

import javax.jdo.annotations.Element;

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

	EntityCodeBlock() {
		super();
	}

	EntityCodeBlock(EntityFile file, float score, ITuple<Integer, Integer> lines, HashSet<ITuple<Integer, Integer>> internalSkeletonCode) {
		super();
		this.file = file;
		this.score = score;

		this.size = 0;
		this.lines = new ArrayList<>();
		this.addLineToList(lines);

		this.internalSkeletonCode = new HashMap<>();
		this.internalSkeletonCode.put(new Tuple<>(lines.getKey(), lines.getValue()), internalSkeletonCode);
	}

	EntityCodeBlock(EntityFile file, float score, List<ITuple<Integer, Integer>> lines, List<HashSet<ITuple<Integer, Integer>>> internalSkeletonCode) {
		super();
		this.file = file;
		this.score = score;

		this.size = 0;
		this.lines = new ArrayList<>();
		lines.forEach(this::addLineToList);
		this.internalSkeletonCode = new HashMap<>();
		for (int i = 0; i < lines.size(); i++){
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

	void append(float score, ITuple<Integer, Integer> lines, HashSet<ITuple<Integer, Integer>> internalSkeletonCode) {
		this.score = ((this.score * this.size) + score) / (this.size + 1); //new avg score
		this.addLineToList(lines);
		if (!this.internalSkeletonCode.containsKey(new Tuple<>(lines.getKey(), lines.getValue()))){
			this.internalSkeletonCode.put(new Tuple<>(lines.getKey(), lines.getValue()), internalSkeletonCode);
		}else{
			this.internalSkeletonCode.get(new Tuple<>(lines.getKey(), lines.getValue())).addAll(internalSkeletonCode);
		}
	}

	void append(float score, List<ITuple<Integer, Integer>> lines, List<HashSet<ITuple<Integer, Integer>>> internalSkeletonCode) {
		this.score = ((this.score * this.size) + score) / (this.size + lines.size()); //new avg score
		lines.forEach(this::addLineToList);
		for (int i = 0; i < lines.size(); i++){
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
			BaseStorage.logger.warn("Null line ITuple added to code block");
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
}
