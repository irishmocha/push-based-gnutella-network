package servent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Map.Entry;

public class PullBasedClient extends Thread {
	private static Scanner sc;
	
	public PullBasedClient() {
		sc = new Scanner(System.in);
	}
	
	public void run() {
		while (true) {
			System.out.println("Select Menu");
			System.out.println("1. Search a file");
			System.out.println("2. Obtain a file");
			System.out.println("3. Refresh download log");
			System.out.println("q. exit");
			String option = sc.nextLine();
			
			// Search a file
			if(option.equals("1")) {
				System.out.println("Please write down a file name to search");
				String fileName = sc.nextLine();
				
				// have the values ready for query
				MessageID messageID= new MessageID();
				messageID.setSequenceNumber(Gnutella.globalSequenceNumber);
				messageID.setIpAddress(Gnutella.myServerIP);
				messageID.setPortNumber(Gnutella.myServerPort);
				// invoke query to neighbors
				Query(messageID, Gnutella.TTL, fileName);
			} 
			// Obtain the identified file
			else if(option.equals("2")) {
				System.out.println("Please write down a file name to download");
				String fileName = sc.nextLine();
				obtain(fileName);
			}	
			// Refresh outdated files
			else if(option.equals("3")) {
				System.out.println("Refresh outdated files");
				for (Entry<String, String> entry : Gnutella.DOWNLOADED_FILES.entrySet()) {
					String fileName = entry.getKey();
					String status = entry.getValue();
					if(status.equals("invalid")) {
						System.out.println("file name " + fileName + " is outdated. Do you want to re-download this file?(y/n)");
						String r = sc.nextLine();
						if (r.equals("y")) {
							obtain(fileName);
						} else if (r.equals("n")) {
							continue;
						} else {
							System.out.println("invalid input");
							continue;
						}
					}
				}
			}
			// Exit
			else if(option.equals("q")) {
				System.out.println("Thank you");
				System.exit(0);
			}
		}
	}

	/**
	 * When TTR is expired, send pull message to the origin server based on the file information
	 * @param fileName
	 * @param originalServerID
	 */
	public void PullRequest(String fileName, String originalServerID) {
		System.out.println("PullRequest");
		String fileNameWithVersion = null;
		
		// check whether the file actually exists in the downloadable files
		for (Entry<String, ArrayList<String>> entry : Gnutella.DOWNLOADABLE_FILES.entrySet()) {
			String fn = entry.getKey();
			if(fn.contains(fileName)) {
				fileNameWithVersion = fn;
				break;
			} else {
				fileNameWithVersion = null;
			}
		}
		// make a new pull request message
		String filename = fileNameWithVersion.split("_")[0];
		String fileversion = fileNameWithVersion.split("_")[1];
		String originServer = Gnutella.GetOriginServerInfo(originalServerID);
		String originServerIP = originServer.split(":")[0];
		int originServerPort = Integer.parseInt(originServer.split(":")[1]);
		
		String pullQuery = "pull" + "," + filename + "," + fileversion + "," + Gnutella.myServerIP + "," + Gnutella.myServerPort;
		
		DataOutputStream dos;
		Socket socket;
		// send created pull request message
		try {
			socket = new Socket(originServerIP, originServerPort);
			dos = new DataOutputStream(socket.getOutputStream());
			dos.writeUTF(pullQuery);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * public void Query(MessageID messageID, int TTL, String fileName):
	 * creates a new query to search a file in this network
	 */
	public void Query(MessageID messageID, int TTL, String fileName) {
		System.out.println("Query");
		// if there is a file in this local hard drive
		if(Arrays.asList(Gnutella.fileList).contains(fileName)) {
			System.out.println("File exists in local shared directory");
		} 
		// else there is no such file in local hard drive,
		else {
			// make a new query
			String initialQuery = Integer.toString(messageID.getSequenceNumber()) + "/" 	
								+ messageID.getIpAddress() + ":" 
								+ messageID.getPortNumber() + "/" 
								+ Gnutella.myServerIP  + ":" 
								+ Gnutella.myServerPort + "/" 
								+ "neighbor" + "/" 
								+ fileName + "/"
								+ Integer.toString(TTL);
			Gnutella.REQUEST_POOL.add(initialQuery);
			new PullBasedServer().Broadcast();
			Gnutella.globalSequenceNumber++;	
		}
	}
	
	/*
	 * obtain(String fileName):
	 * 1. Gets file name as a parameter to download.
	 * 2. Select a peer from the peer list to connect and download.
	 * 3. Starts a downloader thread.
	 */
	public static void obtain(String filename) {
		DataOutputStream dos;
		Socket obtainSocket;
		sc = new Scanner(System.in);
		
		try {
			int select;
			ArrayList<String> fileList = new ArrayList<String>();
			String fileNameWithVersion = null;
			
			// get a downloadable file information from downloadable file list that was listed when search completed
			for (Entry<String, ArrayList<String>> entry : Gnutella.DOWNLOADABLE_FILES.entrySet()) {
				String fn = entry.getKey();
				if(fn.contains(filename)) {
					fileNameWithVersion = fn;
					break;
				} else {
					fileNameWithVersion = null;
				}
			}
			
			String[] temp = fileNameWithVersion.split("_");
			String file = temp[0];
			String version = temp[1];
			
			// get the list of peers that hold the file
			fileList = Gnutella.DOWNLOADABLE_FILES.get(fileNameWithVersion);
			System.out.println("Select the peer to connect for obtain " + file + "(Ver " + version + ")");
			System.out.println("(Please enter 0 if you want to select first, enter 1 if you want to select second)");
			System.out.println("Peer List: " + fileList);
			
			// get selection of peers from user			
			while (true) {
				select = sc.nextInt();
				if(select < 0 || (select > fileList.size())) {
					System.out.println("Invalid input");
				} else {
					break;
				}
			}
			// set the target servent information to download
			String destination[] = fileList.get(select).split(":");
			String destIP = destination[0];
			int destPort = Integer.parseInt(destination[1]); 
			// connect to the target servent and send requestor information of file transfer
			obtainSocket = new Socket(destIP, destPort); 
			dos = new DataOutputStream(obtainSocket.getOutputStream());
			dos.writeUTF("obtain," + filename + "," + Gnutella.myServerIP + "," +Gnutella.myServerPort);
			// create a new thread to download
			Thread fileDownloader = new Thread(new FileDownloader(file, version, destIP, destPort));
			fileDownloader.start();
		} catch (Exception e) {
			
		} finally {
		}
	}
}
