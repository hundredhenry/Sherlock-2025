package uk.ac.warwick.dcs.sherlock.engine.executor.pool;

import uk.ac.warwick.dcs.sherlock.api.component.*;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.AbstractModelTaskRawResult;
import uk.ac.warwick.dcs.sherlock.api.model.postprocessing.ModelTaskProcessedResults;
import uk.ac.warwick.dcs.sherlock.api.util.ITuple;
import uk.ac.warwick.dcs.sherlock.api.util.PairedTuple;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.engine.executor.JobStatus;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.ExecutorUtils;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.IPriorityWorkSchedulerWrapper;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.Priority;
import uk.ac.warwick.dcs.sherlock.engine.executor.work.WorkPreProcessFiles;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;



record FileIdPair(long file1Id, long file2Id) {}

/**
 * Executor which handles job wide tasks, uses 1 task executor per task
 */
public class PoolExecutorJob implements Runnable {

	private final IPriorityWorkSchedulerWrapper scheduler;
	private final IJob job;
	private final JobStatus status;

	public PoolExecutorJob(IPriorityWorkSchedulerWrapper scheduler, IJob job, JobStatus status) {
		this.scheduler = scheduler;
		this.job = job;
		this.status = status;
	}

	public long getId() {
		return this.job.getPersistentId();
	}

	public Priority getPriority() {
		return this.status.getPriority();
	}

	public JobStatus getStatus() {
		return this.status;
	}

	public IJob getJob() {
		return this.job;
	}

	@Override
	public void run() {
		List<PoolExecutorTask> tasks = job.getTasks().stream().map(x -> new PoolExecutorTask(this.status, scheduler, x, job.getWorkspace().getLanguage())).collect(Collectors.toList());
		ExecutorService exServ = Executors.newFixedThreadPool(tasks.size());

		if (tasks.isEmpty()) {
			ExecutorUtils.logger.error("Could not generate tasks for job {}, exiting", this.job.getPersistentId());
			job.setStatus(WorkStatus.INTERRUPTED);
			return;
		}

		// Run preprocessing, detection, and postprocessing
		job.setStatus(WorkStatus.ACTIVE);
		this.status.nextStep();
		this.status.calculateProgressIncrement(tasks.stream().mapToInt(t -> t.getPreProcessingStrategies().size()).sum() * this.job.getWorkspace().getFiles().size());

		List<PoolExecutorTask> detTasks = tasks.stream().filter(x -> x.getStatus() != WorkStatus.COMPLETE).collect(Collectors.toList());

		if (detTasks.isEmpty()) {
			ExecutorUtils.logger.error("Could not generate tasks for job {}, exiting", this.job.getPersistentId());
			job.setStatus(WorkStatus.INTERRUPTED);
			return;
		}

		RecursiveAction preProcess = new WorkPreProcessFiles(new ArrayList<>(detTasks), this.job.getWorkspace().getFiles());
		this.scheduler.invokeWork(preProcess, Priority.DEFAULT);

		// Check that preprocessing went okay
		detTasks.stream().filter(x -> x.dataItems.size() == 0).peek(x -> {
			ExecutorUtils.logger.error("PreProcessing output for detector {} is empty, this detector will be ignored.", x.getDetector().getName());
		}).forEach(detTasks::remove);

		if (detTasks.isEmpty()) {
			ExecutorUtils.logger.error("No detectors with valid preprocessing outputs for job {}, exiting", this.job.getPersistentId());
			job.setStatus(WorkStatus.INTERRUPTED);
			return;
		}

		this.status.nextStep();
		this.status.calculateProgressIncrement(detTasks.size());

		try {
			exServ.invokeAll(detTasks); // build tasks
		}
		catch (InterruptedException e) {
			job.setStatus(WorkStatus.INTERRUPTED);
			return;
		}

		this.status.nextStep();
		this.status.calculateProgressIncrement(detTasks.stream().mapToInt(PoolExecutorTask::getWorkerSize).sum());

		try {
			exServ.invokeAll(detTasks); // run tasks
		}
		catch (InterruptedException e) {
			job.setStatus(WorkStatus.INTERRUPTED);
			return;
		}

		job.setStatus(WorkStatus.REGEN_RESULTS);

		//currently all EntityTasks have their AbstractModelTaskRawResult's set to the results of the detectors
		//we essentially want to go through and do the following:
		//Code in Skeleton Submission S = [sf1,sf2,sf3,..]
		//All legitimate submissions LS = [ls1,ls2,..]
		// such that ls1 = [lf1,lf2,...], ie a submission made up of files
		//we want to go through all results, which is an arbitrary combination of (lsn.lfm, lsk.lfj), and do the following:
		//for each combination of (lsn.lfm, lsk.lfj):
		//  remove from Result((lsn.lfm, lsk.lfj)):
		//	Any locations that exist in S.lfm and Result((lsn.lfm, lsk.lfj)), and Any locations in S.lfj and Result((lsn.lfm, lsk.lfj)).

		// TODO: add threading to this 
		//for each task in the job (ie for each detector being ran)
		for (PoolExecutorTask poolTask : detTasks){//O(D)
			//get the EntityTask which contains all the results for the Detector's task
			ITask entityTask = poolTask.getTask();
			//hashmap for skeleton code results, for easy lookup, keys are <skeleton code file id, legitimate file id>
			HashMap<FileIdPair, AbstractModelTaskRawResult> skeletonCodeResults = new HashMap();
			List<AbstractModelTaskRawResult> normalSubmissionResults = new ArrayList();
			Set<Long> skeletonCodeFileIDs = new HashSet();

			List<AbstractModelTaskRawResult> results = entityTask.getRawResults();
			if (results == null) {
				continue;
			}
			for (AbstractModelTaskRawResult result : results){//O(L+S)
				//we know that results for code from same submissions are filtered out before this, so can assume that if first file 
				// comes from skeleton code, then the second file is legitimate.
				if (result.getFile1().getSubmission().getName().equals(SherlockEngine.skeletonCodeName)){
					skeletonCodeResults.put(new FileIdPair(result.getFile1().getPersistentId(), result.getFile2().getPersistentId()), result);
					skeletonCodeFileIDs.add(result.getFile1().getPersistentId());
				} else if(result.getFile2().getSubmission().getName().equals(SherlockEngine.skeletonCodeName)){
					skeletonCodeResults.put(new FileIdPair(result.getFile2().getPersistentId(), result.getFile1().getPersistentId()), result);
					skeletonCodeFileIDs.add(result.getFile2().getPersistentId());
				} else {
					normalSubmissionResults.add(result);
				}
			}

			//now we have a bunch of results, with skeleton code submissions in the hashmap, and normal submissions in the list
			// as well as a list of skeleton code file ids

			//first check if there actually are any skeleton code submissions
			if (skeletonCodeFileIDs.size() == 0){
				break; //can just break since whether or not there is a skeleton code submission will be consistent across all tasks
			}
			//now go through each result
			for (AbstractModelTaskRawResult result : normalSubmissionResults){ //O(L)
				//and for each result, get the skeleton code result for each file
				for (Long fileID : skeletonCodeFileIDs){ //O(S), which is effectively constant (unless someones uploading 100s of skeleton files...)
					//get the skeleton code result for the first file
					AbstractModelTaskRawResult skeletonCodeResult1 = skeletonCodeResults.get(new FileIdPair(fileID, result.getFile1().getPersistentId()));
					//and the second file
					AbstractModelTaskRawResult skeletonCodeResult2 = skeletonCodeResults.get(new FileIdPair(fileID, result.getFile2().getPersistentId()));

					//if there is no skeleton code result, then we can just continue
					if (skeletonCodeResult1 == null || skeletonCodeResult2 == null){
						continue;
					}

					//otherwise, get all the locations of all the matches between the two files and the skeleton code file
					List<PairedTuple<Integer, Integer, Integer, Integer>> skeletonCodeFile1Matches = skeletonCodeResult1.getLocations();
					List<PairedTuple<Integer, Integer, Integer, Integer>> skeletonCodeFile2Matches = skeletonCodeResult2.getLocations();

					//now the intention with the following code is to get the code in a state which miniises the number of 
					// loops spent removing lines from the raw results. The idea is to go through both lists of matches
					// and combine them, stripping the skeleton code matches but combining the legitimate matches into
					// one paired tuple. ie going from:
					//	skeletonCodeResult1: <<skeleCodeMatchStartLine1, skeleCodeMatchEndLine1>,<legitMatchStartLine1, legitMatchEndLine1>>
					//  skeletonCodeResult2: <<skeleCodeMatchStartLine2, skeleCodeMatchEndLine2>,<legitMatchStartLine2, legitMatchEndLine2>>
					//to:
					//  results: <<legitMatchStartLine1, legitMatchEndLine1>,<legitMatchStartLine2, legitMatchEndLine2>>
					//so do one loop over both lists (rather than two)
					for (int i = 0; i < skeletonCodeFile1Matches.size() || i < skeletonCodeFile2Matches.size(); i++){ //O(M)
						ITuple<Integer, Integer> file1Match;
						//and check if there are matches left in the first file
						if (i >= skeletonCodeFile1Matches.size()){
							//if not, then just use a dummy match, which will never affect results
							file1Match = new Tuple<>(-1,-1);
						} else {
							//otherwise, get the match
							PairedTuple<Integer, Integer, Integer, Integer> skeletonCodeFile1Match = skeletonCodeFile1Matches.get(i);
							//and if the first file is from the skeleton code submission, then use the second pair of locations
							// which represent the start and end of the code block in the legitimate file
							if (skeletonCodeResult1.getFile1().getSubmission().getName().equals(SherlockEngine.skeletonCodeName)){
								file1Match = skeletonCodeFile1Match.getPoint2();
							}else{
								//otherwise, use the first pair of locations
								file1Match = skeletonCodeFile1Match.getPoint1();
							}
						}
						//then do the same for the second file
						ITuple<Integer, Integer> file2Match;
						if (i >= skeletonCodeFile2Matches.size()){
							file2Match = new Tuple<>(-1,-1);
						} else {
							PairedTuple<Integer, Integer, Integer, Integer> skeletonCodeFile2Match = skeletonCodeFile2Matches.get(i);
							if (skeletonCodeResult2.getFile1().getSubmission().getName().equals(SherlockEngine.skeletonCodeName)){
								file2Match = skeletonCodeFile2Match.getPoint2();
							}else{
								file2Match = skeletonCodeFile2Match.getPoint1();
							}
						}


						//now we have one paired tuple, we can call the removeLine method to remove it from the raw result
						result.removeLine(new PairedTuple<>(file1Match, file2Match));//O(M)

					}
				}
			}
			//finally, do a cleanup to remove any matches that now have no lines in them
			//first remake the full list of rawresults
			List<AbstractModelTaskRawResult> fullResults = new ArrayList<>();
			for (AbstractModelTaskRawResult result : normalSubmissionResults){//O(L)
				//and just check that there are actually lines left in the result
				if (result.getLocations().size()>0){
					//clean up the internal skeleton code
					result.cleanInternalSkeletonCode();
					//and add it to the full list
					fullResults.add(result);
				}
			}
			//now here we *can* add skeleton code matches back in, if we want to. I imagine that users probably
			// dont care about how much someone has used the skeleton code though so will just leave it out.
			//If you want to add skeleton code back, you can just uncomment this:
			// for (AbstractModelTaskRawResult subResult : skeletonCodeResults.values()){
			// 	fullResults.add(subResult);
			// }

			//then clear the results, and set the new results,
			entityTask.clearResults();
			entityTask.setRawResults(fullResults);
			//then reset the job as complete
			entityTask.setComplete();
		}

		//so overall, the time complexity of this is O(D) * [O(L+S) + O(L)*O(S)*O(M)*O(M) + O(L)]
		// Which is equal to O(D) * O(L+S + L*S*M^2 + L)
		// Where D is the number of detectors, L is the number of legitimate files to be analysed,
		// S is the number of skeleton code files, and M is the number of matches per file
		// Since D and S are small, this brings us to worst case O(L*M^2). Feel free to optimise further!

		// Run postprocessing
		this.status.setStep(5);
		List<PoolExecutorTask> postTasks = tasks.stream().filter(x -> x.getStatus() == WorkStatus.COMPLETE).collect(Collectors.toList());
		List<ITuple<ITask, ModelTaskProcessedResults>> results = new LinkedList<>();
		this.status.calculateProgressIncrement(postTasks.size());

		try {
			List<Future<ModelTaskProcessedResults>> tmp = exServ.invokeAll(postTasks);
			for (int i = 0; i < postTasks.size(); i++) {
				ModelTaskProcessedResults m = tmp.get(i).get();
				if (m != null && m.getGroups().size() > 0) {
					results.add(new Tuple<>(postTasks.get(i).getTask(), m));
				}
			}
		}
		catch (InterruptedException | ExecutionException e) {
			return;
		}


		// score
		if (results.size() > 0) {
			this.status.nextStep();
			this.status.calculateProgressIncrement(((this.job.getWorkspace().getFiles().size() * results.size()) * 2) + this.job.getWorkspace().getFiles().size());

			List<ICodeBlockGroup> allGroups = results.stream().flatMap(f -> f.getValue().getGroups().stream()).collect(Collectors.toList());
			SherlockEngine.storage.storeCodeBlockGroups(allGroups);

			// TODO: thread scoring loops
			IResultJob jobRes = this.job.createNewResult();
			
			// Track which groups have been added to each task to prevent duplicates
			// Map: Task -> Set of group identity hash codes already added
			Map<ITask, Set<Integer>> taskGroupsAdded = new HashMap<>();
			for (ITuple<ITask, ModelTaskProcessedResults> t : results) {
				taskGroupsAdded.put(t.getKey(), new HashSet<>());
			}
			
			for (ISourceFile file : this.job.getWorkspace().getFiles()) {
				IResultFile fileRes = jobRes.addFile(file);
				List<ITuple<ICodeBlockGroup, Float>> overallGroupScores = new LinkedList<>();

				for (ITuple<ITask, ModelTaskProcessedResults> t : results) {
					try {
						List<ICodeBlockGroup> groupsContainingFile = t.getValue().getGroups(file);
						int fileTotal = t.getValue().getFileTotal(file);

						// Detect if AST-based by checking first block's subtreeWeight
						boolean isAST = !groupsContainingFile.isEmpty() && groupsContainingFile.get(0).getCodeBlock(file).getSubtreeWeight() != null && groupsContainingFile.get(0).getCodeBlock(file).getSubtreeWeight() != null;

						// Construct block scores weighted against the whole file, by default uses file line count, but can be set to custom totals (eg. variable counts)
						AtomicReference<Float> fullSize = new AtomicReference<>((float) 0);

						List<ITuple<ICodeBlockGroup, Float>> groupScores = groupsContainingFile.stream().map(x -> {
							ICodeBlock b = x.getCodeBlock(file);

							if (isAST) {
								// AST-based: weight by node count instead of line count
								float subtreeWeight = b.getSubtreeWeight();
								return new Tuple<>(x, b.getBlockScore() * (subtreeWeight/fileTotal));
							}else {
								// Syntax/token-based: weight by line count
								float size = b.getLineNumbers().stream().mapToInt(y -> y.getValue()- y.getKey() + 1).sum();
							    fullSize.updateAndGet(v -> v + size);
							return new Tuple<>(x, b.getBlockScore() * (size/fileTotal));
							}

						}).collect(Collectors.toList());

						// For non-AST detectors, normalise against full size to counteract overlapping line ranges.
						// Skipped for AST: AST groups are pairwise (2 files each), so fullSize sums subtree weights
						// across ALL pairs, incorrectly deflating per-pair scores.
						if (!isAST && fullSize.get() > fileTotal) {
							float factor = fullSize.get() / fileTotal;
							groupScores.forEach(x -> x.setValue(x.getValue()/factor));
						}

						this.status.incrementProgress();

						IResultTask taskRes = fileRes.addTaskResult(t.getKey());
						
						// Only add groups that haven't been added to this task yet (prevents duplicates across files)
						Set<Integer> addedGroups = taskGroupsAdded.get(t.getKey());
						List<ICodeBlockGroup> newGroups = groupsContainingFile.stream()
							.filter(g -> addedGroups.add(System.identityHashCode(g)))
							.collect(Collectors.toList());
						if (!newGroups.isEmpty()) {
							taskRes.addContainingBlock(newGroups);
						}

						// calculate and store the scores from the group scores, uses weightings
						calculateScoreForBlockList(file, groupScores, taskRes, taskRes.getClass().getDeclaredMethod("setTaskScore", float.class), taskRes.getClass().getDeclaredMethod("addFileScore", ISourceFile.class, float.class));
						overallGroupScores.addAll(groupScores);

						this.status.incrementProgress();
					}
					catch (Exception e) {
						synchronized (ExecutorUtils.logger) {
							ExecutorUtils.logger.error("Scorer error: ", e);
						}
					}
				}

				try {
					calculateScoreForBlockList(file, overallGroupScores, fileRes, fileRes.getClass().getDeclaredMethod("setOverallScore", float.class), fileRes.getClass().getDeclaredMethod("addFileScore", ISourceFile.class, float.class));
				}
				catch (NoSuchMethodException e) {
					e.printStackTrace();
				}

				this.status.incrementProgress();
			}
		}
		else {
			ExecutorUtils.logger.info("Job {} produced no results", job.getPersistentId());
		}

		job.setStatus(WorkStatus.COMPLETE);
	}

	@SuppressWarnings ("Duplicates")
	private void calculateScoreForBlockList(ISourceFile file, List<ITuple<ICodeBlockGroup, Float>> groupScores, Object obj, Method methodTotal, Method methodPerFile) {
		try {
			// Score each file for the task first, then use the max as the overall score
			float maxScore = 0f;
			for (ISourceFile fileComp : this.job.getWorkspace().getFiles()) {
				if (!fileComp.equals(file)) {
					float s = (float) groupScores.stream().filter(g -> g.getKey().filePresent(fileComp)).mapToDouble(x -> x.getValue()).sum();
					float capped = s > 1 ? 1 : s;
					methodPerFile.invoke(obj, fileComp, capped);
					if (capped > maxScore) maxScore = capped;
				}
			}

			// Overall score = maximum score against any single other file
			methodTotal.invoke(obj, maxScore);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
