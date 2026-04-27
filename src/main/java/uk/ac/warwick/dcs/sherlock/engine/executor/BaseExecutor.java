package uk.ac.warwick.dcs.sherlock.engine.executor;

import uk.ac.warwick.dcs.sherlock.api.component.IJob;
import uk.ac.warwick.dcs.sherlock.api.component.WorkStatus;
import uk.ac.warwick.dcs.sherlock.api.executor.IExecutor;
import uk.ac.warwick.dcs.sherlock.api.executor.IJobStatus;
import uk.ac.warwick.dcs.sherlock.engine.SherlockEngine;
import uk.ac.warwick.dcs.sherlock.engine.executor.common.*;
import uk.ac.warwick.dcs.sherlock.engine.executor.pool.PoolExecutorJob;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Basic executor implementation
 */
public class BaseExecutor implements IExecutor, IPriorityWorkSchedulerWrapper {

	final Map<IJob, JobStatus> jobMap;
	private final Map<IJob, PoolExecutorJob> queuedJobs;
	private final Map<IJob, Future<?>> runningJobs;

	private final PriorityBlockingQueue<PoolExecutorJob> queue;
	private final PriorityWorkScheduler scheduler;
	private final ExecutorService exec;
	private final ExecutorService execScheduler;

	private final AtomicInteger curID;

	public BaseExecutor() {
		this.scheduler = new PriorityWorkScheduler();

		this.exec = Executors.newSingleThreadExecutor();
		this.execScheduler = Executors.newSingleThreadExecutor();
		this.queue = new PriorityBlockingQueue<PoolExecutorJob>(5, Comparator.comparing(PoolExecutorJob::getPriority));
		this.jobMap = new ConcurrentHashMap<>();
		this.queuedJobs = new ConcurrentHashMap<>();
		this.runningJobs = new ConcurrentHashMap<>();

		this.curID = new AtomicInteger(0); //counter for jobstatus ids

		this.execScheduler.execute(() -> {
			while (true) {
				try {
					PoolExecutorJob job;
					job = this.queue.take();
					this.queuedJobs.remove(job.getJob());

					ExecutorUtils.logger.info("Job {} starting", job.getId());

					job.getStatus().startJob();

					Future<?> f = this.exec.submit(job);
					this.runningJobs.put(job.getJob(), f);
					try {
						f.get();
					}
					catch (CancellationException e) {
						ExecutorUtils.logger.info("Job {} cancelled", job.getId());
					}
					catch (ExecutionException e) {
						ExecutorUtils.logger.error("Job {} failed", job.getId(), e);
						job.getJob().setStatus(WorkStatus.INTERRUPTED);
						job.getStatus().failJob();
					}
					finally {
						this.runningJobs.remove(job.getJob());
					}

					if (job.getStatus().isCancellationRequested()) {
						job.getJob().setStatus(WorkStatus.INTERRUPTED);
						job.getStatus().cancelJob();
					}
					else if (job.getJob().getStatus().equals(WorkStatus.COMPLETE)) {
						job.getStatus().finishJob();
					}
					else {
						job.getJob().setStatus(WorkStatus.INTERRUPTED);
						job.getStatus().failJob();
					}

					//Remove after some configured time
					if (job.getJob().getStatus().equals(WorkStatus.COMPLETE) && SherlockEngine.configuration.getJobCompleteDismissalTime() > 0) {
						Thread thread = new Thread(new JobDismisser(this, job));
						thread.start();
					}

					ExecutorUtils.logger.info("Job {} finished, took: {}", job.getId(), job.getStatus().getFormattedDuration());
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});
	}

	@Override
	public List<IJobStatus> getAllJobStatuses() {
		List<IJobStatus> res = new ArrayList<>(this.jobMap.values());
		res.sort(IJobStatus::compareTo);
		return res;
	}

	@Override
	public IJob getJob(IJobStatus jobStatus) {
		if (this.jobMap.containsValue(jobStatus)) {
			AtomicReference<IJob> ret = new AtomicReference<>(null);
			this.jobMap.forEach((job, status) -> {
				if (jobStatus.equals(status)) {
					ret.set(job);
				}
			});

			return ret.get();
		}
		else {
			return null;
		}
	}

	@Override
	public IJobStatus getJobStatus(IJob job) {
		return this.jobMap.getOrDefault(job, null);
	}

	@Override
	public List<IJob> getWaitingJobs() {
		return this.queue.stream().map(PoolExecutorJob::getJob).collect(Collectors.toList());
	}

	@Override
	public void invokeWork(ForkJoinTask topAction, Priority priority) {
		PriorityWorkTask task = new PriorityWorkTask(topAction, priority);

		synchronized (task) {
			this.submitWork(task);

			try {
				task.wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void shutdown() {
		this.scheduler.shutdown();
		this.exec.shutdownNow();
		this.execScheduler.shutdownNow();
	}

	@Override
	public boolean submitJob(IJob job) {
		if (job == null) {
			ExecutorUtils.logger.error("Job is null");
			return false;
		}

		if (!job.isPrepared() || job.getStatus().equals(WorkStatus.NOT_PREPARED)) {
			ExecutorUtils.logger.error("Job {} has not been prepared", job.getPersistentId());
			return false;
		}

		if (job.getTasks().isEmpty()) {
			ExecutorUtils.logger.error("Job {} does not have any tasks", job.getPersistentId());
			return false;
		}

		if (job.getFiles() == null || job.getFiles().length == 0) {
			ExecutorUtils.logger.error("Job {} workspace has no files", job.getPersistentId());
			return false;
		}

		JobStatus existingStatus = this.jobMap.get(job);
		if (existingStatus != null && !existingStatus.isFinished()) {
			ExecutorUtils.logger.error("Job {} is already queued or running", job.getPersistentId());
			return false;
		}

		JobStatus s = new JobStatus(curID.getAndIncrement(), Priority.DEFAULT);
		this.jobMap.put(job, s);

		PoolExecutorJob j = new PoolExecutorJob(this, job, s);
		this.queuedJobs.put(job, j);
		this.queue.add(j);

		ExecutorUtils.logger.info("Job {} added to queue", job.getPersistentId());

		return true;
	}

	@Override
	public boolean dismissJob(IJobStatus jobStatus) {
		return this.dismissJob(this.getJob(jobStatus), jobStatus);
	}

	@Override
	public boolean dismissJob(IJob job) {
		return this.dismissJob(job, this.getJobStatus(job));
	}

	private boolean dismissJob(IJob job, IJobStatus jobStatus) {
		if (job != null && jobStatus != null && this.jobMap.containsKey(job) && jobStatus.isFinished()) {
			this.jobMap.remove(job);
			return true;
		}

		return false;
	}

	@Override
	public boolean cancelJob(IJobStatus jobStatus) {
		return this.cancelJob(this.getJob(jobStatus));
	}

	@Override
	public boolean cancelJob(IJob job) {
		JobStatus status = this.jobMap.get(job);
		if (job == null || status == null || status.isFinished()) {
			return false;
		}

		status.requestCancellation();
		job.setStatus(WorkStatus.INTERRUPTED);

		PoolExecutorJob queued = this.queuedJobs.remove(job);
		if (queued != null) {
			boolean removed = this.queue.remove(queued);
			status.cancelJob();
			return removed;
		}

		Future<?> running = this.runningJobs.get(job);
		if (running != null) {
			return true;
		}

		status.cancelJob();
		return true;
	}

	@Override
	public void submitWork(PriorityWorkTask work) {
		this.scheduler.scheduleJob(work);
	}

	private class JobDismisser implements Runnable {

		private final BaseExecutor executor;
		private final PoolExecutorJob job;
		private final long time;

		JobDismisser(BaseExecutor executor, PoolExecutorJob job) {
			this.executor = executor;
			this.job = job;
			this.time = 60000 * SherlockEngine.configuration.getJobCompleteDismissalTime();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(this.time);
				this.executor.jobMap.remove(this.job.getJob());
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
