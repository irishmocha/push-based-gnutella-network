package servent;

public class MessageID {
	
	private int serventID;
	private int sequenceNumber;
	private String portNumber;
	private String ipAddress;
	
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(String myServerPort) {
		this.portNumber = myServerPort;
	}

	public int getServentID() {
		return serventID;
	}
	
	public void setServentID(int serventID) {
		this.serventID = serventID;
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}