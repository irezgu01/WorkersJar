package upem.jarret.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

}
