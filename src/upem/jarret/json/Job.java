package upem.jarret.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Job {

	private int jobId;
	private int JobTaskNumber; 
	private String JobDescription;
	private int JobPriority;
	private String workerVersionNumber;
	private String workerURL;
	private String workerClassName;
	
	public Job() {
		
	}

	public Job(int jobId,int task,String description,int priority, String workerVersion, String workerURL, String workerClassName) {
		super();
		this.jobId = jobId;
		this.JobTaskNumber = task;
		this.JobDescription = description;
		this.JobPriority = priority;
		this.workerVersionNumber = workerVersion;
		this.workerURL = workerURL;
		this.workerClassName = workerClassName;
	}
	@JsonProperty("JobId")
	public int getJobId() {
		return jobId;
	}
	@JsonProperty("JobTaskNumber")
	public int getJobTaskNumber() {
		return JobTaskNumber;
	}
	@JsonProperty("JobDescription")
	public String getJobDescription() {
		return JobDescription;
	}
	@JsonProperty("JobPriority")
	public int getJobPriority() {
		return JobPriority;
	}
	@JsonProperty("WorkerVersionNumber")
	public String getWorkerVersionNumber() {
		return workerVersionNumber;
	}
	@JsonProperty("WorkerURL")
	public String getWorkerURL() {
		return workerURL;
	}
	@JsonProperty("WorkerClassName")
	public String getWorkerClassName() {
		return workerClassName;
	}

	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	public void setJobTaskNumber(int jobTaskNumber) {
		JobTaskNumber = jobTaskNumber;
	}

	public void setJobDescription(String jobDescription) {
		JobDescription = jobDescription;
	}

	public void setJobPriority(int jobPriority) {
		JobPriority = jobPriority;
	}

	public void setWorkerVersionNumber(String workerVersionNumber) {
		this.workerVersionNumber = workerVersionNumber;
	}

	public void setWorkerURL(String workerURL) {
		this.workerURL = workerURL;
	}

	public void setWorkerClassName(String workerClassName) {
		this.workerClassName = workerClassName;
	}

	

}
