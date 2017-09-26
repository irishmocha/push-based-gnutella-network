package servent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class PullBasedServer extends Thread {
	// the down stream servent to trace back to the original requestor
	String downStreamReceiver = null;
	String downStreamSenders = null;
	
	public void run() {
		ServerSocket serverSocket;
		Socket socket;
		try {
			// Open my server socket to accept connections from other servents
			serverSocket = new ServerSocket(Integer.parseInt(Gnutella.myServerPort));
			while (true) {
				socket = serverSocket.accept();
				ServerProcessor serverProcessor = new ServerProcessor(socket);
				serverProcessor.start();
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Server Processor:
	 * connected to neighbor socket(s) and interact with messages.
	 * identifies messages then broadcasts or sends queryhit message or send requested file
	 */
	public class ServerProcessor extends Thread {
		
		Socket processorSocket = null;

		DataInputStream dis = null;
		DataOutputStream dos = null;

		String queryString = null;
		
		ServerProcessor(Socket neighborSocket) {
			this.processorSocket = neighborSocket;
			try {
				dis = new DataInputStream(processorSocket.getInputStream());
				dos = new DataOutputStream(processorSocket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			try{
				// get messages from neighbor continuously
				while (dis != null) {
					
					queryString = dis.readUTF();
					
					// splits message to identify the type of messages
					String[] parsedQuery = queryString.split("/");
					
					// Message type: Invalidation
					// Message format: SeqNo/Original Address/Destination Address/file_versionNo/TTL
					if (parsedQuery.length == 5) {
						System.out.println("Message Type: Invalidation");
						String newSeqNo = parsedQuery[0];
						String newOriginal = parsedQuery[1];
						String newDestination = "neighbor";
						String[] temp = parsedQuery[3].split("_");
												
						String fileToBeChecked = temp[0];
						//int versionNumber = Integer.parseInt(temp[1]);
						
						if(Gnutella.DOWNLOADED_FILES.containsKey(fileToBeChecked.split("-")[0])){
							//UpdateIfInvalid(fileToBeChecked, versionNumber);
							Gnutella.DOWNLOADED_FILES.put(fileToBeChecked, "invalid");
						}
						int newTTL = Integer.parseInt(parsedQuery[4]) - 1;
						String newMessage = newSeqNo + "/" + newOriginal + "/" + newDestination + "/" + parsedQuery[3] + "/" + Integer.toString(newTTL);
						Gnutella.REQUEST_POOL.add(newMessage);
						Broadcast();
					}
					
					// when this server got a broadcast from another servent
					// the structure of massage is as follows
					// SeqNo / Requestor / Sender List / File Name / TTL
					else if (parsedQuery.length == 6) {
						// search requested file with local storage
						String fileName = parsedQuery[4];
						boolean found;
						found = Lookup(fileName);
	
						// when the file was found
						if (found == true) {
							// increase global sequence number
							Gnutella.globalSequenceNumber++;
							// set new values to make new query hit message
							MessageID messageID = new MessageID();
							messageID.setSequenceNumber(Gnutella.globalSequenceNumber);
							messageID.setIpAddress(Gnutella.myServerIP);
							messageID.setPortNumber(Gnutella.myServerPort);
							// set downstream servent for backtracking
							downStreamReceiver = parsedQuery[2];
							downStreamSenders = Gnutella.myServerIP + ":" + Gnutella.myServerPort;
							
							/* start change */
							String lastModified = Gnutella.lastModified.get(fileName);
							String fileNameWithVersion = fileName + "_" + Integer.toString(Gnutella.MASTER_FILES.get(fileName));
							String masterCopyBit = "1";
							
							// start query hit message in trace-back way
							Queryhit(messageID, Gnutella.TTL, fileName, Gnutella.myServerIP, Gnutella.myServerPort, lastModified, fileNameWithVersion, masterCopyBit);
							/* end change */
						}
						// when the file was not found
						else {
							// set values to broadcasting
							String newSeqNumber = parsedQuery[0];
							String originalSender = parsedQuery[1];
							String newSender;
							// prevent duplicated listing of current server information from broadcasting
							if(parsedQuery[2].contains(Gnutella.myServerIP + ":" + Gnutella.myServerPort)) {
								newSender = parsedQuery[2];
							} else {
								newSender = parsedQuery[2] + "," + Gnutella.myServerIP + ":" + Gnutella.myServerPort;
							}
							// string neighbor is changed by neighbor's address in the 'broadcast()' 
							String newReceiver = "neighbor";
							String newFileName = parsedQuery[4];
							int temp = Integer.parseInt(parsedQuery[5]) - 1;
							String newTTL = Integer.toString(temp);
							// make a new query for broadcasting
							String newQuery = newSeqNumber + "/" 
											+ originalSender + "/" 
											+ newSender + "/" 
											+ newReceiver + "/" 
											+ newFileName + "/" 
											+ newTTL;
							// add a new query into concurrentlinkedqueue
							Gnutella.REQUEST_POOL.add(newQuery);
							Broadcast();
						}
						queryString = null;
					}
	
					// when this servent got a query hit from another servent
					// the query hit message is as follows
					// SeqNo / Original Requestor / Address of senders / next servent for backtracking / Sender ID / File Name / TTL / Hit
					else if (parsedQuery.length == 10) {
						System.out.println("Message Type: Queryhit");
						System.out.println(queryString);
						String fileHolder = parsedQuery[1];
						String originalRequestor = parsedQuery[2];
						String lastReceiver = parsedQuery[3];
						String fileName = parsedQuery[5];
						// when the first sender address and receiver's address is same
						if (originalRequestor.equals(lastReceiver)) {
							System.out.println("Got a hit message from " + fileHolder);
							EnlistDownloadableFiles(fileName, fileHolder);
						}
						// when this query hit message has not been reached to the original requestor	
						else {
							// split message to analysis
							String[] temp = parsedQuery[2].split(",");
							// when this message has still need to be propagated
							if (temp.length > 1) {	
								// set next receiver
								downStreamSenders = temp[temp.length-2];
								// then set new sender list to backtracking
								downStreamReceiver = "";
								for(int i = 0; i < temp.length-1; i++) {
									downStreamReceiver = downStreamReceiver + temp[i] + ",";
								}
								downStreamReceiver = downStreamReceiver.substring(0, downStreamReceiver.length()-1);
								// set values to make new query hit message
								String tempIP = parsedQuery[1].split(":")[0];
								String tempPort = parsedQuery[1].split(":")[1];
								MessageID messageID = new MessageID();
								messageID.setSequenceNumber(Gnutella.globalSequenceNumber);
								messageID.setIpAddress(tempIP);
								messageID.setPortNumber(tempPort);
								int newTTL = Integer.parseInt(parsedQuery[6]) - 1;
								/* start change */
								String lastModified = parsedQuery[8];
								String fileNameWithVersion = parsedQuery[5];
								String masterCopyBit = parsedQuery[9];
								Queryhit(messageID, newTTL, parsedQuery[5], Gnutella.myServerIP, Gnutella.myServerPort, lastModified, fileNameWithVersion, masterCopyBit);
								/* end change */
							} 
							// when the requested file exists right next neighbor of this servent
							else if(temp.length == 1) {
								System.out.println("Got a hit message from " + parsedQuery[1]);
								// list the file and servent information that will be used to download
								EnlistDownloadableFiles(parsedQuery[5], parsedQuery[1]);
							}
						}
					}
					/*--------------------- start change ---------------------*/
					// when this sever gets file information for pull approaching
					else if (parsedQuery.length == 4) {
						System.out.println("got file info message including TTR");
						String temp = parsedQuery[0];
						String fileName = temp.split("_")[0];
						//String fileVersion = temp.split("_")[1];
						String originServer = parsedQuery[1];
						String TTR = parsedQuery[2];
						String lastModified = parsedQuery[3];
						String fileInfo = originServer + "/" + TTR + "/" + lastModified + "\n"; 
						// save the information to check TTR
						Gnutella.pullFileInfo.put(fileName, fileInfo);
					} 
					// when the obtain request occurs
					else if(queryString != null && queryString.startsWith("obtain")) {	
						String[] obtainQueryParser = queryString.split(",");
						String fileName = obtainQueryParser[1];
						String requestorIP = obtainQueryParser[2];
						int requestorPort = Integer.parseInt(obtainQueryParser[3]);
						System.out.println("Flie Obtain Requested from " + requestorIP + ":" + requestorPort);
						// send file information to the requestor
						DirectMsgFileInfo(fileName, requestorIP, requestorPort);
						// activate file uploader
						Thread fileUploader = new Thread(new FileUploader(fileName, requestorIP, requestorPort));
						fileUploader.start();
					} 
					// check validity of file with the version of file
					else if (queryString != null && queryString.startsWith("pull")) {
						String[] pullQueryParser = queryString.split(",");
						String filename = pullQueryParser[1];
						String fileversion = pullQueryParser[2];
						String clientIP = pullQueryParser[3];
						int clientPort = Integer.parseInt(pullQueryParser[4]);
						boolean validity;
						
						if (Integer.toString(Gnutella.MASTER_FILES.get(filename)).equals(fileversion)) {
							System.out.println("version not modified");
							validity = true;
						} else {
							System.out.println("version modified: (new version: " + Gnutella.MASTER_FILES.get(filename) + ")");
							validity = false;
						}
						PullResponse(filename, validity, clientIP, clientPort);
					} 
					// when a pull response message received
					else if (queryString != null && queryString.startsWith("File")) {
						String[] pullReply = queryString.split(",");
						String fileWithVersion = pullReply[1];
						String fileName = fileWithVersion.split("_")[0];
						int fileVersion = Integer.parseInt(fileWithVersion.split("_")[1]);
						
						// server got a file outdated message, invalidate the file if file has been modified
						if (pullReply.length == 4) {
							System.out.println("new version of the file " + fileName + " / version: " + fileVersion);
							Gnutella.DOWNLOADED_FILES.put(fileName, "invalid");
						}
						// start new ttr checker with new ttr value if file has not been modified
						else if (pullReply.length == 5){
							String newTTR = pullReply[4];
							Thread ttrChecker = new Thread(new TTRChecker(fileName, newTTR));
							ttrChecker.start();
						}
					} 
					/*--------------------- end change ---------------------*/
					else { System.out.println("Invalid Message Format"); }
				}
			} catch (IOException e) {

			} finally {
				
			}
		}
	}
	/*------------------------------------------ start change ------------------------------------------*/
	public synchronized void PullResponse(String filename, boolean validity, String requestorIP, int requestorPort) {
		Socket responseSocket;
		DataOutputStream responseDos;
		String responseMessage = null;
		// if file has been modified
		if (validity) {
			responseMessage = "File," + filename + "_" + Gnutella.MASTER_FILES.get(filename) + "," + Gnutella.myServerIP + "," + Gnutella.myServerPort + "," + Gnutella.myServerTTR;
		} 
		// send file outdated message if file has not been modified
		else {
			responseMessage = "File," + filename + "_" + Gnutella.MASTER_FILES.get(filename) + "," + Gnutella.myServerIP + "," + Gnutella.myServerPort;
		}
		try {
			responseSocket = new Socket(requestorIP, requestorPort);
			responseDos = new DataOutputStream(responseSocket.getOutputStream());
			responseDos.writeUTF(responseMessage);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * when an obtain request come in, this server send directly a message including the file information to the requestor 
	 * @param filename
	 * @param requestorIP
	 * @param requestorPort
	 */
	public synchronized void DirectMsgFileInfo(String filename, String requestorIP, int requestorPort) {
		File file = new File(Gnutella.sharedDirectoryPath + "/" + filename);
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH-mm-ss");
		System.out.println("DirectMsgFileInfo> filename: " + filename + " requestorIP: " + requestorIP + " requestorPort: " + requestorPort);
		String pullFileInfo = filename /*+ "_" + Gnutella.masterFiles.get(filename) */+ "/" + Integer.toString(Gnutella.myServerID) + "/" + Gnutella.myServerTTR + "/" + sdf.format(file.lastModified());
		Socket socket;
		DataOutputStream dos;
		try {
			socket = new Socket(requestorIP, requestorPort);
			dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF(pullFileInfo);
		} catch (IOException e) {
			
		}
	}
	/*------------------------------------------ end change ------------------------------------------*/
	
	
	/**
	 * checks whether this servent has the file requested from other server
	 * @param filename
	 * @return
	 */
	public boolean Lookup(String filename) {
		if (Gnutella.MASTER_FILES.containsKey(filename)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * When this servent get a queryh hit message finally
	 * the file name and the server that hold the file is listed to the local hashmap
	 * so that we can choose the servent to download the file
	 * @param fileName
	 * @param servent
	 */
	public synchronized void EnlistDownloadableFiles(String fileName, String servent) {
		
		if(Gnutella.DOWNLOADABLE_FILES.containsKey(fileName)) {
			Gnutella.DOWNLOADABLE_FILES.get(fileName).add(servent);		
		} else {
			Gnutella.DOWNLOADABLE_FILES.putIfAbsent(fileName, new ArrayList<String>());
			Gnutella.DOWNLOADABLE_FILES.get(fileName).add(servent);
		}
	}
	
	/**
	 * broadcast any kind of message to neighbors when requested
	 */
	public synchronized void Broadcast() {
		DataOutputStream broadcastDos;
		while (!Gnutella.REQUEST_POOL.isEmpty()) {
			// get a message from queue
			String message = Gnutella.REQUEST_POOL.remove();
			// check TTL, if TTL is expired, escape from this loop
			if(message.contains("-")){
				break;
			}
			// broadcasts query to neighbors
			for(ServerInfo neighbor : Gnutella.neighborServerList) {
				String newMessage = message.replaceAll("neighbor", neighbor.getServerIP() + ":" + neighbor.getServerPort());
				Socket broadcastSocket;
				try {
					broadcastSocket = new Socket(neighbor.getServerIP(), Integer.parseInt(neighbor.getServerPort()));
					broadcastDos = new DataOutputStream(broadcastSocket.getOutputStream());
					broadcastDos.writeUTF(newMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * send query hit message back to the original requestor
	 * @param messageID
	 * @param TTL
	 * @param filename
	 * @param inetAddr
	 * @param port
	 */
	public synchronized void Queryhit(MessageID messageID, int TTL, String filename, String inetAddr, String port, String lastModified, String fileNameWithVersion, String masterCopyBit) {		
		// make a new query message or update a propagated message from upstream servent
		String queryhitMsg = Integer.toString(messageID.getSequenceNumber()) + "/" 
							+ messageID.getIpAddress() + ":" 
							+ messageID.getPortNumber() + "/"
							+ downStreamReceiver + "/"
							+ downStreamSenders + "/"
							+ Integer.toString(Gnutella.myServerID) + "/"
							+ fileNameWithVersion + "/"
							+ Integer.toString(TTL) + "/" 
							+ "hit" + "/"
							+ lastModified + "/"
							+ masterCopyBit;
		
		DataOutputStream dos;
		Socket socket;
		
		try {
			// prevent concurrent access to already proceeded task caused from broadcasting
			if (downStreamReceiver != null) {
				String temp[] = downStreamReceiver.split(",");
				String[] newReceiver;
				String newReceiverIP;
				int newReceiverPort;
				// when a file exists right next neighbor, set the original sender to last receiver
				if(temp.length == 1) {
					System.out.println("1: " + downStreamReceiver);
					newReceiver = downStreamReceiver.split(":");
					newReceiverIP = newReceiver[0];
					newReceiverPort = Integer.parseInt(newReceiver[1]);
				} 
				// when a file exists far away, set the latest sender to last receiver
				else {
					String lastReceiver = temp[temp.length-1];
					newReceiver = lastReceiver.split(":");
					newReceiverIP = newReceiver[0];
					newReceiverPort = Integer.parseInt(newReceiver[1]);
				}
				socket = new Socket(newReceiverIP, newReceiverPort);
				dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(queryhitMsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			downStreamReceiver = null;
		}
	}
}
