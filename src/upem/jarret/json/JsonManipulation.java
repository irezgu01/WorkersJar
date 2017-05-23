package upem.jarret.json;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonManipulation {
	
	/**
	 * retourne une chaine au format json
	 * @param string
	 * @return
	 * @throws JsonProcessingException
	 */
	public static String jsonString(ObjectNode string) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(string);
	}
	
	/**
	 * convert the string received from the server to an ObjectNode
	 * @param string
	 *            the body of the task received from the server
	 * @return ObjectNode the response of the server
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	public static ObjectNode tojson(String stringResponse) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(stringResponse);
		ObjectNode objectNode = (ObjectNode) node;
		return objectNode;
	}
	
	/**
	 * parse a  file and return a List of ObjectNode from the file 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static List<Task> parseFile(String file) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		List<Task> tasks = new LinkedList<Task>();

		try {
			JsonParser jsonParser = new JsonFactory().createParser(new File(file));
			MappingIterator<Task> job = mapper.readValues(jsonParser, Task.class);
			while (job.hasNext()) {
				Task node = job.next();
				tasks.add(node);
			}
		} catch (

		JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tasks;
	}
	public static String taskToJson(Task job) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(job);
	}
	public static void main(String[] args) throws Exception {
		List<Task> tasks = JsonManipulation.parseFile("/home/cho/git/WorkersJar/src/tasks.json");
		tasks.stream().forEach(s -> {
			try {
				System.out.println(taskToJson(s));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
			
	}

}
