package upem.jarret.server;

import java.io.File;

import upem.jarret.json.Job;

public class JobInfos {
	private final int nbTaskAssigned;
	private final TaskManager tasks;
	private final File fileContainsResponses;
	private final File fileContainsLogs;
	private boolean doneAllTasks = false;
	
	public JobInfos(int nbTaskAssigned,Job job,String answerDirectoryName,String logDirectoryName) {
		this.nbTaskAssigned = nbTaskAssigned;
		tasks = new TaskManager(job.getJobId(), job.getJobTaskNumber(), job.getWorkerVersionNumber(), job.getWorkerURL(), job.getWorkerClassName());
		this.fileContainsResponses = new File("./src/"+answerDirectoryName+""+job.getJobId());
		this.fileContainsLogs = new File("./src/"+logDirectoryName+""+job.getJobId());
	}
	
	public boolean verifyDoneAllTasks() {
		return doneAllTasks;
	}

	public void setStatusOfDoneAllTasks() {
		this.doneAllTasks = true;
	}

	public File getFileContainsResponses() {
		return fileContainsResponses;
	}
	
	public File getFileContainsLog() {
		return fileContainsLogs;
	}

	public int getNbTaskAssigned() {
		return nbTaskAssigned;
	}

	public TaskManager getTasks() {
		return tasks;
	}

}
