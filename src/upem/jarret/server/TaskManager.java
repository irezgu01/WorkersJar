package upem.jarret.server;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskManager{
	private final int jobId;
	private final int jobTaskNumber;
	private final Task task;
	private int currentTaskNumber = 0;
	private final ObjectMapper mapper = new ObjectMapper();
	
	public TaskManager(int jobId, int jobTaskNumber, String workerVersionNumber, String workerURL, String workerClassName) {
		this.task = new Task(jobId, workerVersionNumber, workerURL, workerClassName,0);
		this.jobId = jobId;
		this.jobTaskNumber = jobTaskNumber;
		
	}
	
	
	public boolean hasNext(){
		return currentTaskNumber < jobTaskNumber;
	}
	
	public Optional<String> nextTask(){
		String jsonTask;
		if(!hasNext()){
			throw new NoSuchElementException();
		}
		try {
			 jsonTask = mapper.writeValueAsString(task);
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
		currentTaskNumber++;
		task.setTask(currentTaskNumber);
		return Optional.of(jsonTask);
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public static void main(String[] args) {
		TaskManager taskManager = new TaskManager(1, 10, "1.2", "http://www.test.fr/test1.jar", "test.test");
		while(taskManager.hasNext()){
			System.out.println(taskManager.nextTask());
		}
	}



}
