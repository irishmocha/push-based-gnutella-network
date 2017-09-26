package servent;


public class ServerInfo {
	
	private String serverID;
	private String serverIP;
	private String serverPort;
	private String serverTTR;
	
	public String getServerTTR() {
		return serverTTR;
	}

	public void setServerTTR(String serverTTR) {
		this.serverTTR = serverTTR;
	}

	public String getServerID() {
		return serverID;
	}
	
	public void setServerID(String serverID) {
		this.serverID = serverID;
	}
	
	public String getServerIP() {
		return serverIP;
	}
	
	public void setServerIP(String string) {
		this.serverIP = string;
	}
	
	public String getServerPort() {
		return serverPort;
	}
	
	public void setServerPort(String string) {
		this.serverPort = string;
	}
}
