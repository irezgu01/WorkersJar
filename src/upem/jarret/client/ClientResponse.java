package upem.jarret.client;

public class ClientResponse {

	private String jobId;
	private String workerVersion;
	private String workerURL;
	private String workerClassName;
	private int task;
	private String clientId;

	public ClientResponse(String jobId, String workerVersion, String workerURL, String workerClassName, int task) {
		super();
		this.jobId = jobId;
		this.workerVersion = workerVersion;
		this.workerURL = workerURL;
		this.workerClassName = workerClassName;
		this.task = task;
	}

	public String getJobId() {
		return jobId;
	}

	public String getWorkerVersion() {
		return workerVersion;
	}

	public String getWorkerURL() {
		return workerURL;
	}

	public String getWorkerClassName() {
		return workerClassName;
	}

	public int getTask() {
		return task;
	}

	public String getClientId() {
		return clientId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public void setWorkerVersion(String workerVersion) {
		this.workerVersion = workerVersion;
	}

	public void setWorkerURL(String workerURL) {
		this.workerURL = workerURL;
	}

	public void setWorkerClassName(String workerClassName) {
		this.workerClassName = workerClassName;
	}

	public void setTask(int task) {
		this.task = task;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String toString() {
		return "jobId "+jobId + " task "+task+ " Worker url "+workerURL+ " worker class "+workerClassName+" client  id "+clientId;
	}
}
