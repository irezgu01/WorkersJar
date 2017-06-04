package upem.jarret.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Task {
	private final String jobId;
	private String workerVersion;
	private String workerURL;
	private String workerClassName;
	private String task ;
	
	
	public void setTask(int task) {
		this.task = ""+task;
	}
	public Task(int jobId, String workerVersionNumber, String workerURL, String workerClassName,int task) {
		this.jobId = ""+task;
		this.workerVersion = workerVersionNumber;
		this.workerURL = workerURL;
		this.workerClassName = workerClassName;
		this.task = ""+task;
	}
	@JsonProperty("JobId")
	public String getJobId() {
		return jobId;
	}
	@JsonProperty("WorkerVersion")
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
	public String getJobTaskNumber() {
		return task;
	}


	

}
