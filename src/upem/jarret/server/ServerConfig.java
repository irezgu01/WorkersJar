package upem.jarret.server;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerConfig {
	private  int port;
	private  String logDirectory;
	private  String answersDirectory;
	private  int maxFileSize;
	private  int comeBackInSeconds;

	public ServerConfig(){};
	
	public ServerConfig(int port, String logDirectory, String answersDirectory, int maxFileSize,
			int comeBackInSeconds) {
		super();
		this.port = port;
		this.logDirectory = logDirectory;
		this.answersDirectory = answersDirectory;
		this.maxFileSize = maxFileSize;
		this.comeBackInSeconds = comeBackInSeconds;
	}
	@JsonProperty("Port")
	public int getPort() {
		return port;
	}

	@JsonProperty("LogDirectory")
	public String getLogDirectory() {
		return logDirectory;
	}

	@JsonProperty("AnswersDirectory")
	public String getAnswersDirectory() {
		return answersDirectory;
	}
	
	@JsonProperty("MaxFileSize")
	public int getMaxFileSize() {
		return maxFileSize;
	}
	@JsonProperty("ComeBackInSeconds")
	public int getComeBackInSeconds() {
		return comeBackInSeconds;
	}
	@Override
	public String toString() {
		return "Port : "+port+" Log Directory : "+logDirectory+" AnswersDirectory : "+answersDirectory+" maxFileSize : "+maxFileSize
				+" ComeBackInSeconds : "+comeBackInSeconds;
	}




}
