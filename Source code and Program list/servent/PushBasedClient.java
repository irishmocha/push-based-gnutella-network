package servent;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Scanner;

public class PushBasedClient extends Thread {
	
	private static Scanner sc;
	
	public PushBasedClient() {
		sc = new Scanner(System.in);
	}
	
	public void run() {
		while (true) {
			
			System.out.println("Select Menu");
			System.out.println("1. Search a file");
			System.out.println("2. Obtain a file");
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
				System.out.println("Please write down a file name you want to download");
				String fileName = sc.nextLine();
				obtain(fileName);
			} 
			// Exit
			else if(option.equals("q")) {
				System.out.println("Thank you");
				System.exit(0);
			}
		}
	}

	/*
	 * public void Query(MessageID messageID, int TTL, String fileName):
	 * creates a new query to search a file in this network
	 */
	public void Query(MessageID messageID, int TTL, String fileName) {
		if(Arrays.asList(Gnutella.fileList).contains(fileName)) {
			System.out.println("File exists in local shared directory");
		} else {
			String initialQuery = Integer.toString(messageID.getSequenceNumber()) + "/" 	
						+ messageID.getIpAddress() + ":" 
						+ messageID.getPortNumber() + "/" 
						+ Gnutella.myServerIP  + ":" 
						+ Gnutella.myServerPort + "/" 
						+ "neighbor" + "/" 
						+ fileName + "/"
						+ Integer.toString(TTL);
			Gnutella.REQUEST_POOL.add(initialQuery);
			new PushBasedServer().Broadcast();
			Gnutella.globalSequenceNumber++;	
		}
	}
	
	/**
	 * When shared monitor detects the modification, this client make a new invalidation message to be broadcasted.
	 * @param messageID
	 * @param serverID
	 * @param fileName
	 * @param versionNumber
	 */
	public void Invalidation(MessageID messageID, int serverID, String fileName, int versionNumber) {
		String invalidationQuery = Integer.toString(messageID.getSequenceNumber()) + "/"
				+ Integer.toString(serverID) + "/"
				+ "neighbor" + "/" 
				+ fileName + "_"
				+ Integer.toString(versionNumber) + "/"
				+ Integer.toString(Gnutella.TTL);
		System.out.println("Invalidation: "+invalidationQuery);
		Gnutella.REQUEST_POOL.add(invalidationQuery);
		new PushBasedServer().Broadcast();
		Gnutella.globalSequenceNumber++;	
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
		try {
			int select;
			ArrayList<String> fileList = new ArrayList<String>();
			String fileNameWithVersion = null;
			
			for (Entry<String, ArrayList<String>> entry : Gnutella.DOWNLOADABLE_FILES.entrySet()) {
				String fn = entry.getKey();
				if(fn.contains(filename)) {
					fileNameWithVersion = fn; break;
				} else {
					fileNameWithVersion = null;
				}
			}
			String[] temp = fileNameWithVersion.split("_");
			String file = temp[0];
			String version = temp[1];
			// get the list of servents that hold the file
			fileList = Gnutella.DOWNLOADABLE_FILES.get(fileNameWithVersion);
			System.out.println("Select the peer to connect for obtain " + file + "(Ver " + version + ")");
			System.out.println("(Please enter 0 if you want to select first, enter 1 if you want to select second)");
			System.out.println("Peer List: " + fileList);
			// get selection of servents from user			
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
			
			// connect to the target servent and information of file transfer
			obtainSocket = new Socket(destIP, destPort); 
			dos = new DataOutputStream(obtainSocket.getOutputStream());
			dos.writeUTF("obtain," + filename + "," + Gnutella.myServerIP + "," +Gnutella.myServerPort);
			
			// create a new thread to download
			Thread fileDownloader = new Thread(new FileDownloader(file, version, destIP, destPort));
			fileDownloader.start();
			fileList.clear();
		} catch (Exception e) {
			
		} finally {
		}
	}
}