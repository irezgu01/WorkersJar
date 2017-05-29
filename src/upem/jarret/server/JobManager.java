package upem.jarret.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import upem.jarret.json.Job;
import upem.jarret.json.JsonManipulation;

public class JobManager {


	public static Map<Job, JobInfos> init() throws Exception{
		HashMap<Job, JobInfos> map = new HashMap<>();
		List<Job> jobs = JsonManipulation.parseFile("./src/tasks.json").
				stream().
				filter(job->job.getJobPriority() > 0)
				.collect(Collectors.toList());

		jobs.forEach(job->{
			map.put(job, new JobInfos(job.getJobPriority(), job));
		});
		return map;
	}
	/*
	public static void main(String[] args) throws Exception {
		List<Job> jobs = JsonManipulation.parseFile("./src/tasks.json").stream().filter(job->job.getJobPriority() > 0).collect(Collectors.toList());
		System.out.println(jobs);
	}
	*/
	



}
