package upem.jarret.worker;

public class Job {
	private  long JobId;
	private  int JobTaskNumber;
	private  String JobDescription;
	private  int JobPriority;
	private  float WorkerVersionNumber;
	private  String WorkerURL;
	private  String WorkerClassName;
	
	
	public void setJobId(long jobId) {
		JobId = jobId;
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
	public void setWorkerVersionNumber(float workerVersionNumber) {
		WorkerVersionNumber = workerVersionNumber;
	}
	public void setWorkerURL(String workerURL) {
		WorkerURL = workerURL;
	}
	public void setWorkerClassName(String workerClassName) {
		WorkerClassName = workerClassName;
	}
	
	public long getJobId() {
		return JobId;
	}
	public int getJobTaskNumber() {
		return JobTaskNumber;
	}
	public String getJobDescription() {
		return JobDescription;
	}
	public int getJobPriority() {
		return JobPriority;
	}
	public float getWorkerVersionNumber() {
		return WorkerVersionNumber;
	}
	public String getWorkerURL() {
		return WorkerURL;
	}
	public String getWorkerClassName() {
		return WorkerClassName;
	}
}
