package upem.jarret.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import upem.jarret.json.Job;
import upem.jarret.json.JsonManipulation;

public class JobManager {
	
	/*
	public JobManager() {
		map = new HashMap<>();
	}
	*/
	public static Map<Job, JobInfos> init() throws Exception{
		HashMap<Job, JobInfos> map = new HashMap<>();
		List<Job> jobs = JsonManipulation.parseFile("./src/tasks.json");
		int totalJob = jobs.size();
		
		//Calculer le nombre de tasks attribué par le serveur à ce job
		//Pour l'instant considérons que ça correspond à la priorité
		jobs.forEach(job->{
			map.put(job, new JobInfos(job.getJobPriority(), job));
		});
		return map;
	}
	


}
