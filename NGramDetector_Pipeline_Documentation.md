# NGramDetector Pipeline Documentation

## Overview
The NGramDetector is a plagiarism detection system that identifies similar code blocks across multiple source files using N-gram analysis. It compares files pairwise and generates similarity scores based on overlapping character sequences.

---

## Pipeline Architecture

### High-Level Flow
```
Job Submission → Preprocessing → Detection → Postprocessing → Scoring → Results
```

---

## Detailed Pipeline Stages

### 1. **Job Initialization & Setup**

#### Entry Point
- **Class**: `WorkspaceWrapper.runTemplate()`
- **Location**: `src/main/java/uk/ac/warwick/dcs/sherlock/module/web/data/wrappers/WorkspaceWrapper.java`

**Process**:
1. Template validation (must contain detectors)
2. File validation (workspace must have files)
3. Job creation: `IJob job = workspace.createJob()`
4. Detector registration: `job.addDetector(NGramDetector.class)`
5. Job preparation: `job.prepare()`
6. Parameter configuration from template
7. Job submission to executor: `SherlockEngine.executor.submitJob(job)`

#### Detector Registration
- **Class**: `NGramDetector` extends `PairwiseDetector<NGramDetectorWorker>`
- **Registration**: Occurs in `ModuleModelBase` during system initialization
- **Associated Components**:
  - Preprocessing: `TrimWhitespaceOnly` (strategy name: "no_whitespace")
  - Postprocessing: `NGramPostProcessor`
  - Raw Result Type: `NGramRawResult<NgramMatch>`

#### Adjustable Parameters
Three configurable parameters with defaults:

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| **N-Gram Size** | 4 | 1-10 | Character width of each N-gram |
| **Minimum Window** | 5 | 0-20 | Minimum N-grams for valid match block |
| **Threshold** | 0.8 | 0.0-1.0 | Similarity threshold for detection |

---

### 2. **Execution Infrastructure**

#### Executor Framework
- **Class**: `BaseExecutor` (implements `IExecutor`)
- **Architecture**: Priority-based work scheduling with thread pools

**Components**:
1. **Priority Queue**: `PriorityBlockingQueue` for job management
2. **Thread Pools**:
   - Single-thread executor for job scheduling
   - Single-thread executor for job processing
3. **Job Status Tracking**: `JobStatus` objects with progress monitoring

**Job Execution Flow**:
```
PoolExecutorJob.run()
  ├─ Create PoolExecutorTask for each task (detector)
  ├─ Phase 1: Preprocessing (parallel across all files)
  ├─ Phase 2: Build Workers (for each task)
  ├─ Phase 3: Run Detection (parallel worker execution)
  └─ Phase 4: Postprocessing
```

---

### 3. **Preprocessing Stage**

#### Purpose
Transform source code to normalized form suitable for N-gram analysis by removing excess whitespace while preserving structure.

#### Implementation
- **Class**: `TrimWhitespaceOnly` implements `IGeneralPreProcessor`
- **Lexer**: `StandardLexerSpecification`

**Processing Rules**:
| Token Channel | Action |
|---------------|--------|
| COMMENT | Preserved as-is |
| DEFAULT | Preserved as-is |
| WHITESPACE | Preserved as-is |
| LONG_WHITESPACE | Replaced with single space " " |

**Output**: `List<IndexedString>` - Each line with line number preserved

**Execution Context**:
- **Class**: `WorkPreProcessFiles` (RecursiveAction)
- Processes all files for all preprocessing strategies in parallel
- Each file gets preprocessed once per strategy
- Results stored in `ModelDataItem` objects

---

### 4. **Worker Building Phase**

#### Worker Construction
- **Class**: `NGramDetectorWorker` extends `PairwiseDetectorWorker<NGramRawResult>`
- **Builder**: `PairwiseDetector.buildWorkers()`

**Worker Creation Logic**:
```java
// Generate all pairwise combinations of files
combinations(data, 2)
  .filter(x -> !x.get(0).getFile().getSubmission().equals(x.get(1).getFile().getSubmission()))
  .map(x -> new NGramDetectorWorker(this, x.get(0), x.get(1)))
```

**Rules**:
- One worker per unique file pair
- Files from same submission are excluded (no self-plagiarism)
- For N files with M submissions: generates O(N²) workers (filtered)

**Worker State**:
- `file1`: Reference file (ModelDataItem with preprocessed lines)
- `file2`: Check file (ModelDataItem with preprocessed lines)
- `parent`: Reference to NGramDetector instance (for parameters)

---

### 5. **Detection Phase (Core Algorithm)**

#### Execution Model
- **Framework**: Fork-Join parallel processing via `WorkDetect` (RecursiveTask)
- **Threshold**: Dynamically calculated based on worker count and CPU cores
- **Parallelism**: Divides workers into balanced subtasks

#### Core Detection Algorithm

**Step 5.1: Data Structure Initialization**

For each worker (file pair):

```
file1 → HashMap<String, Ngram> storage_map    (Reference file)
file2 → ArrayList<Ngram> storage_list           (Check file)
```

**Ngram Object Structure**:
```java
class Ngram {
    String segment;          // The N-gram string (ngram_size characters)
    int line_number;         // Line where N-gram starts
    int id;                  // Differentiation ID for duplicates
    Ngram next_ngram;        // Linked list pointer to next N-gram
}
```

**Step 5.2: Load Reference File (loadNgramMap)**

Process each line of file1:
1. Pad lines shorter than `ngram_size` with spaces
2. Extract overlapping N-grams (sliding window)
3. For first N-gram: Create Ngram object, store in HashMap with key "ngram_string+0"
4. For subsequent N-grams:
   - Link to previous via `next_ngram` pointer (forms linked list)
   - If N-gram string is unique: store with ID 0
   - If N-gram string exists: find next available ID (1, 2, 3...) and store with that ID

**Purpose of Duplicate IDs**: Handles cases where same N-gram appears multiple times in reference file

**Step 5.3: Load Check File (loadNgramList)**

Process each line of file2:
1. Pad lines shorter than `ngram_size` with spaces
2. Extract overlapping N-grams (sliding window)
3. Create Ngram object for each
4. Add to sequential ArrayList

**Step 5.4: Sliding Window Comparison**

Initialize tracking variables:
```java
ArrayList<Ngram> reference;      // N-grams from reference file in current match
ArrayList<Ngram> check;          // N-grams from check file in current match
Ngram head;                      // Current position in reference file
float last_peak = 1.0;           // Highest similarity in current window
float sim_val = 1.0;             // Current similarity
int since_last_peak = 0;         // Steps since peak (for backtracking)
int ngram_id = 0;                // Which duplicate version to use
```

**Main Detection Loop** (iterate through check file N-grams):

```
FOR each ngram in storage_list:
    IF ngram exists in storage_map OR reference is not empty:
        
        // Initialize or extend window
        IF head is null:
            head = storage_map.get(ngram_string + ngram_id)
            reference.add(head)
        ELSE:
            head = head.getNextNgram()
            IF head is null:
                break  // Reference file exhausted
            reference.add(head)
        
        check.add(ngram)
        
        // Calculate similarity
        sim_val = compare(reference, check)  // Jaccard similarity
        
        // Track peak
        IF sim_val >= last_val:
            since_last_peak = 0
            last_peak = sim_val
        since_last_peak++
        
        // Evaluate match conditions
        IF reference.size == minimum_window AND sim_val < threshold:
            // Window too small and below threshold - try next duplicate
            IF storage_map.contains(first_ngram + (ngram_id + 1)):
                i -= minimum_window  // Backtrack
                ngram_id++
            // Reset
            reference.clear(); check.clear(); head = null
            
        ELSE IF reference.size > minimum_window AND sim_val < threshold:
            // Valid match found - output it
            matchFound(...)  // Backtrack to peak
            // Reset
            reference.clear(); check.clear(); head = null; ngram_id = 0
```

**Step 5.5: Similarity Calculation (Jaccard Index)**

```java
compare(reference, check):
    same = 0, dis1 = 0, dis2 = 0
    
    FOR each ngram in reference:
        IF check contains ngram: same++
        ELSE: dis1++
    
    FOR each ngram in check:
        IF reference does NOT contain ngram: dis2++
    
    return same / (same + dis1 + dis2)
```

**Step 5.6: Match Recording**

When match found, `matchFound()` executes:
1. Backtrack by `since_last_peak` to return to similarity peak
2. Verify match meets `minimum_window` requirement
3. Create `NgramMatch` object:
   - Reference file: start line, end line
   - Check file: start line, end line
   - Similarity score: `last_peak`
4. Store in `NGramRawResult` container

**Step 5.7: Result Aggregation**

Worker returns:
```java
NGramRawResult<NgramMatch> {
    long file1id, file2id;
    List<NgramMatch> objects;              // Match metadata
    List<PairedTuple<...>> locations;      // Line ranges
    int size;
}
```

---

### 6. **Postprocessing Phase**

#### Purpose
Transform pairwise match results into code block groups representing the same duplicated content across multiple files.

#### Implementation
- **Class**: `NGramPostProcessor` implements `IPostProcessor<NGramRawResult>`
- **Input**: List of `NGramRawResult` (one per file pair)
- **Output**: `ModelTaskProcessedResults` containing `ICodeBlockGroup` objects

#### Adjustable Parameter
| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| **Common Threshold** | 0.3 | 0.0-1.0 | % of files above which matches are ignored as common code |

#### Process Flow

**Step 6.1: Match Grouping (processMatches)**

Group matches by code blocks that appear together:

```
Initialize: ArrayList<ArrayList<NgramMatch>> matches

FOR each NGramRawResult:
    FOR each NgramMatch in result:
        IF matches is empty:
            Create new group, add match
        ELSE:
            Find linked group via isLinked()
            IF link found:
                Add to that group
            ELSE:
                Create new group
```

**Linking Logic** (`isLinked`):
Two matches are linked if they share the same code block in any file:
```java
FOR each file combination (i, j):
    IF first.files[i] == second.files[j]:
        IF first.lines[i] == second.lines[j]:
            return true
return false
```

**Step 6.2: Scoring Groups (makeScoreGroups)**

For each match group:

1. **Initialize Scorer**: `NGramScorer.newGroup()`
   - Clears file_list and file_info

2. **Accumulate Match Data**: `scorer.add(match)` for each match
   - Builds file_list (unique files in this group)
   - Builds file_info (cumulative similarity per file)

3. **Common Code Check**: `scorer.checkSize(total_files, match_list)`
   ```java
   IF (file_list.size / total_files) <= threshold:
       return true   // Keep this group
   ELSE:
       Mark all matches as common
       return false  // Discard this group
   ```

4. **Score Calculation**: For each file in group
   ```java
   score = total_similarity / similar_files
   ```
   Where:
   - `total_similarity`: Sum of all similarity scores for this file in this group
   - `similar_files`: Count of files this file matched with in this group

5. **Add to Results**: `out_group.addCodeBlock(file, score, line_range)`

**Step 6.3: Result Finalization**

```
FOR each group:
    Set comment: "N-Gram Match Group"
    Set detection type: "BASE_BODY_REPLACE_CALL"
    IF group is empty:
        Remove group from results
```

---

### 7. **Results Structure**

#### ModelTaskProcessedResults
```
results
  └─ ICodeBlockGroup (one per unique code duplication)
      ├─ Comment: "N-Gram Match Group"
      ├─ Detection Type: "BASE_BODY_REPLACE_CALL"
      └─ ICodeBlock (one per file containing this duplication)
          ├─ ISourceFile reference
          ├─ Score (float)
          └─ Line range (start, end)
```

---

## Data Flow Summary

```
1. Source Files
   ↓
2. Preprocessing (TrimWhitespaceOnly)
   → IndexedString lists (normalized lines)
   ↓
3. Worker Building (PairwiseDetector)
   → NGramDetectorWorker instances (one per file pair)
   ↓
4. N-gram Extraction
   file1 → HashMap<String, Ngram> (reference)
   file2 → ArrayList<Ngram> (check)
   ↓
5. Sliding Window Detection
   → Compare overlapping N-gram windows
   → Calculate Jaccard similarity
   → Identify match blocks above threshold
   ↓
6. Raw Results
   → NGramRawResult<NgramMatch> per worker
   ↓
7. Match Grouping (Postprocessing)
   → Group pairwise matches by shared code blocks
   ↓
8. Scoring & Filtering
   → Calculate per-file scores
   → Filter common code blocks
   ↓
9. Final Results
   → ICodeBlockGroup per unique duplication
   → ICodeBlock per file containing duplication
```

---

## Key Algorithms

### N-gram Extraction Algorithm
```
FOR each line in file:
    IF line.length < ngram_size:
        Pad with spaces to ngram_size
    
    FOR i = 0 to line.length - ngram_size + 1:
        substring = line[i:i+ngram_size]
        Create Ngram(substring, line_number)
```

### Jaccard Similarity
```
similarity = |A ∩ B| / |A ∪ B|
           = same / (same + different_in_A + different_in_B)
```

Where A and B are N-gram sets from reference and check windows.

### Peak Tracking (Anti-bleed Mechanism)
```
Purpose: Prevent detection from extending beyond actual similar code

Track:
- last_peak: Highest similarity in current window
- since_last_peak: Steps since peak occurred

When similarity drops below threshold:
    Backtrack by since_last_peak steps
    Report match ending at peak
```

---

## Concurrency Model

### Parallel Stages

1. **Preprocessing**: RecursiveAction (Fork-Join)
   - All files processed in parallel
   - One task per (file × preprocessing_strategy)

2. **Worker Building**: Sequential
   - Lightweight operation
   - Creates worker objects only

3. **Detection**: RecursiveTask (Fork-Join)
   - Workers divided by threshold: `min(max(workers / cores, 1), 4)`
   - Each subtask executes workers sequentially
   - Results merged via join()

4. **Postprocessing**: Sequential
   - Single-threaded grouping and scoring
   - Operates on collected results

### Thread Safety
- Each worker operates on independent file pair
- No shared mutable state during detection
- Results collected via thread-safe lists
- Final aggregation serialized in postprocessing

---

## Performance Characteristics

### Time Complexity

**Per Worker**:
- N-gram extraction: O(L × C) where L = lines, C = chars per line
- Detection loop: O(N₂ × W) where N₂ = N-grams in check file, W = window size
- Similarity calculation: O(W) per comparison
- Overall: O(N₂ × W²) worst case

**Full Job**:
- Workers: O(F²) where F = files (pairwise combinations)
- Total: O(F² × N₂ × W²)

### Space Complexity

**Per Worker**:
- HashMap: O(N₁) where N₁ = N-grams in reference file
- ArrayList: O(N₂)
- Active window: O(W)
- Overall: O(N₁ + N₂ + W)

**Optimization Opportunities**:
- Duplicate N-gram handling uses string concatenation (O(1) but inefficient)
- Could use custom HashMap with List values
- Window comparison rebuilds string lists each iteration

---

## Configuration Best Practices

### N-Gram Size
- **Small (3-4)**: More sensitive, more false positives, slower
- **Large (7-8)**: Less sensitive, fewer false positives, faster
- **Recommended**: 4-5 for most code

### Minimum Window
- **Small (3-5)**: Detects short similarities (risk: common patterns)
- **Large (10-15)**: Only substantial duplications (risk: miss smaller plagiarism)
- **Recommended**: 5-7 for balanced detection

### Threshold
- **High (0.9-1.0)**: Only near-identical code
- **Low (0.5-0.7)**: Catches modified code (risk: false positives)
- **Recommended**: 0.8 for general use

### Common Threshold
- **Low (0.1-0.2)**: Aggressive filtering
- **High (0.5-0.7)**: Keeps more matches
- **Recommended**: 0.3 (ignores code in >30% of files)

---

## Error Handling & Edge Cases

### Handled Cases
1. **Short lines**: Padded with spaces to ngram_size
2. **Duplicate N-grams**: ID versioning in HashMap
3. **Reference exhaustion**: Null check on next_ngram
4. **Empty results**: Filtered by isEmpty() check
5. **Common code**: Filtered via common_threshold

### Potential Issues
1. **Overlapping matches**: May count same lines multiple times in scoring
2. **Memory**: Large files with many N-grams can consume significant memory
3. **ID collision**: String concatenation for duplicate IDs is not ideal
4. **Backtracking**: May miss matches if backtrack goes too far

---

## Dependencies & Integration Points

### API Dependencies
- `uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetector`
- `uk.ac.warwick.dcs.sherlock.api.model.detection.PairwiseDetectorWorker`
- `uk.ac.warwick.dcs.sherlock.api.model.postprocessing.IPostProcessor`
- `uk.ac.warwick.dcs.sherlock.api.model.preprocessing.IGeneralPreProcessor`

### Module Registration
**File**: `ModuleModelBase.java`
```java
SherlockRegistry.registerDetector(NGramDetector.class);
SherlockRegistry.registerPostProcessor(NGramPostProcessor.class, NGramRawResult.class);
SherlockRegistry.registerPreProcessor(TrimWhitespaceOnly.class);
```

### Web Integration
- Job submission via `WorkspaceWrapper.runTemplate()`
- Parameter configuration from `TemplateWrapper`
- Results displayed through `ResultsWrapper`

---

## File Locations

### Core Detection
- `src/main/java/uk/ac/warwick/dcs/sherlock/module/model/base/detection/NGramDetector.java`

### Supporting Classes
- `src/main/java/uk/ac/warwick/dcs/sherlock/module/model/base/detection/NgramMatch.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/module/model/base/postprocessing/NGramRawResult.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/module/model/base/postprocessing/NGramPostProcessor.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/module/model/base/scoring/NGramScorer.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/module/model/base/preprocessing/TrimWhitespaceOnly.java`

### Execution Framework
- `src/main/java/uk/ac/warwick/dcs/sherlock/engine/executor/BaseExecutor.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/engine/executor/pool/PoolExecutorJob.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/engine/executor/pool/PoolExecutorTask.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/engine/executor/work/WorkDetect.java`
- `src/main/java/uk/ac/warwick/dcs/sherlock/engine/executor/work/WorkPreProcessFiles.java`

---

## Testing

### Test Files
- `src/test/java/uk/ac/warwick/dcs/sherlock/module/model/base/detection/NGramDetectorTest.java`
- `src/test/java/uk/ac/warwick/dcs/sherlock/module/model/base/postprocessing/NGramPostProcessorTest.java`

### Test Coverage
- Detector instantiation
- Worker creation
- Parameter validation
- (Note: Integration tests disabled - isolating pipeline components proved difficult)

---

## Future Enhancements (from TODOs)

1. **Duplicate N-gram handling**: Custom map structure with List values
2. **Fuzzy matching**: Extend isLinked() for overlapping/subset matches
3. **Match connectivity checking**: Validate all matches in group are connected
4. **Overlapping block resolution**: Prevent duplicate line counting in scores
5. **Common code optimization**: Better handling of skeleton files
6. **Performance tuning**: Optimize string operations in hot paths

---

## Revision History

- **Document Version**: 1.0
- **Created**: 2025-11-26
- **Based on**: Sherlock-2025 codebase (branch: race-conditions)
- **Primary Analyzer**: NGramDetector and associated modules
