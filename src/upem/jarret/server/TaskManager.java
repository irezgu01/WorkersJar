package upem.jarret.server;

import java.util.BitSet;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskManager{
	private final int jobId;
	private final int jobTaskNumber;
	private final Task task;
	private int currentTaskNumber = -1;
	private final ObjectMapper mapper = new ObjectMapper();
	private final BitSet bitSet ;

	public TaskManager(int jobId, int jobTaskNumber, String workerVersionNumber, String workerURL, String workerClassName) {
		this.task = new Task(jobId, workerVersionNumber, workerURL, workerClassName,0);
		this.jobId = jobId;
		this.jobTaskNumber = jobTaskNumber;
		bitSet = new BitSet(jobTaskNumber);

	}

	private boolean fullBitset (){
		for (int i = 0; i < jobTaskNumber; i++) {
			if(!bitSet.get(i)){
				return false;
			}
		}
		return true;
	}

	public boolean hasNext(){
		return currentTaskNumber < jobTaskNumber && !fullBitset();
	}

	public Optional<String> nextTask(){
		String jsonTask;

		if(!hasNext()){
			return Optional.empty();
		}
		if((currentTaskNumber == jobTaskNumber -1)){
			currentTaskNumber = 0;
		}else{
			currentTaskNumber++;
		}
		if(bitSet.get(currentTaskNumber)){
			return nextTask();
		}
		task.setTask(currentTaskNumber);
		try {
			jsonTask = mapper.writeValueAsString(task);
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
		return Optional.of(jsonTask);
	}

	public void setBitSet(int index){
		bitSet.set(index);
	}
	public int getJobId() {
		return jobId;
	}
/*
	public static void main(String[] args) {
		TaskManager taskManager = new TaskManager(1, 10, "1.2", "http://www.test.fr/test1.jar", "test.test");
		int index = 0;
		while(taskManager.hasNext()){
			System.out.println(taskManager.nextTask());
			taskManager.setBitSet(index++);
			index = index % 10;
		}
	}
	*/



}
