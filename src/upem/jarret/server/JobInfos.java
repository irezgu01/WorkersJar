package upem.jarret.server;

import java.io.File;

import upem.jarret.json.Job;

public class JobInfos {
	private final int nbTaskAssigned;
	private final TaskManager tasks;
	private final File fileContainsResponses;
	private boolean doneAllTasks = false;
	
	public boolean verifyDoneAllTasks() {
		return doneAllTasks;
	}

	public void setStatusOfDoneAllTasks() {
		this.doneAllTasks = true;
	}

	public File getFileContainsResponses() {
		return fileContainsResponses;
	}

	public JobInfos(int nbTaskAssigned,Job job) {
		this.nbTaskAssigned = nbTaskAssigned;
		tasks = new TaskManager(job.getJobId(), job.getJobTaskNumber(), job.getWorkerVersionNumber(), job.getWorkerURL(), job.getWorkerClassName());
		this.fileContainsResponses = new File("./Tasks_Responses/"+job.getJobId());
	}

	public int getNbTaskAssigned() {
		return nbTaskAssigned;
	}

	public TaskManager getTasks() {
		return tasks;
	}

}
