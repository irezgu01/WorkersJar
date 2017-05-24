package upem.jarret.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Task {
	private final int jobId;
	private String workerVersion;
	private String workerURL;
	private String workerClassName;
	private int task ;
	
	
	public void setTask(int task) {
		this.task = task;
	}
	public Task(int jobId, String workerVersionNumber, String workerURL, String workerClassName,int task) {
		this.jobId = jobId;
		this.workerVersion = workerVersionNumber;
		this.workerURL = workerURL;
		this.workerClassName = workerClassName;
		this.task = task;
	}
	@JsonProperty("JobId")
	public int getJobId() {
		return jobId;
	}
	@JsonProperty("WorkerVersionNumber")
	public String getWorkerVersionNumber() {
		return workerVersion;
	}
	@JsonProperty("WorkerURL")
	public String getWorkerURL() {
		return workerURL;
	}
	@JsonProperty("WorkerClassName")
	public String getWorkerClassName() {
		return workerClassName;
	}
	
	@JsonProperty("Task")
	public int getJobTaskNumber() {
		return task;
	}


	

}
