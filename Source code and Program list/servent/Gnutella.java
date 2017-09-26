package servent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Gnutella {

	public static String consistency;
	
	private static String configFilePath = "configuration.xml";
	
	public static final int MAX_NUMBER_OF_SERVER = 10;
	public static final int TTL = MAX_NUMBER_OF_SERVER;
	public static int globalSequenceNumber = 0;
	
	// Queries to be broadcasted are stored in this queue
	public static ConcurrentLinkedQueue<String> REQUEST_POOL = new ConcurrentLinkedQueue<String>();
	// Data structures for storing file informations
	public static ConcurrentHashMap<String, Integer> MASTER_FILES = new  ConcurrentHashMap<String, Integer>();
	public static ConcurrentHashMap<String, ArrayList<String>> DOWNLOADABLE_FILES = new ConcurrentHashMap<String, ArrayList<String>>();
	public static ConcurrentHashMap<String, String> lastModified = new ConcurrentHashMap<String, String>();
	public static ConcurrentHashMap<String, String> pullFileInfo = new ConcurrentHashMap<String, String>();
	public static ConcurrentHashMap<String, String>DOWNLOADED_FILES = new ConcurrentHashMap<String, String>();
	// Data Structures that stores the network and neighbor information
	public static ArrayList<ServerInfo> neighborServerList = new ArrayList<ServerInfo>();
	public static ArrayList<ServerInfo> STATIC_NETWORK = new ArrayList<ServerInfo>();
	// download Log file object
	public static File downloadLog;
	// information of this server, used to access network topology
	public static int[] myNeighbors;
	public static int myServerID;
	public static int myServerIndex;
	public static String myServerPort;
	public static String myServerIP;
	public static String myServerTTR;
	// variables for setting up the shared directory and it's files
	public static String sharedDirectoryPath = "/Users/Sevas/incoming";	
	public static File sharedDirectory;									
	public static File[] fileList;										
	public static String[] stringFileList;
		
	private static Scanner sc;
		
	/**
	 * This program gets an argument to decide a consistency policy.
	 * '0' = pull-based network, '1' = push-based network
	 * the argument will be used to run proper threads for correct purpose
	 * @param args
	 */
	public static void main(String[] args) {
		// get consistency policy
		consistency = args[0];
		sc = new Scanner(System.in);
		
		// setup the network and neighbor information
		new XMLParser(configFilePath);
		SetMyServerInfo();
		SetSharedDirectory();
		InitializeMasterFiles();
		SetMyNeighborServerInfo();
		
		// run the shared directory monitor for detecting any kind of modification of master files
		Thread sharedDirMonitor = new Thread(new SharedDirectoryMonitor(sharedDirectoryPath));
		sharedDirMonitor.start();
		
		// push based consistency policy
		if (consistency.equals("0")) {
			PushBasedClient serventClient = new PushBasedClient();
			serventClient.start();
			
			Thread serventServer = new Thread(new PushBasedServer());
			serventServer.start();
		} 
		
		// pull based consistency policy
		else if (consistency.equals("1")) {
			PullBasedClient pullBasedClient = new PullBasedClient();
			pullBasedClient.start();
			
			Thread pullBasedServer = new Thread(new PullBasedServer());
			pullBasedServer.start();
		}
		
		// when the mode was not set accordingly
		else { System.out.println("invalid consistency"); }
	}

	/**
	 * Get an original server information which holds a file wanted
	 * @param originalServerID
	 * @return
	 */
	public static String GetOriginServerInfo(String originalServerID) {
		int serverIndex = Integer.parseInt(originalServerID) - 1;
		return STATIC_NETWORK.get(serverIndex).getServerIP() + ":" + STATIC_NETWORK.get(serverIndex).getServerPort();
	}
	
	/**
	 * Set my neighbors' server information according to the input from user
	 */
	public static void SetMyNeighborServerInfo() {
		myNeighbors = new int[10];
		
		for(int i=0; i<MAX_NUMBER_OF_SERVER; i++) {
			System.out.println("Enter your neighbors. 99 to break");
			int temp = sc.nextInt();
			if(temp == 99) {
				break;
			}
			myNeighbors[i] = temp;
		}
		
		for(int j=0; j<MAX_NUMBER_OF_SERVER; j++) {
			if (myNeighbors[j] != 0) {
				System.out.println(j+1 + "st neighbor: " + myNeighbors[j]);
				
				int neighborIndex = myNeighbors[j] - 1;
				String neighborIP = STATIC_NETWORK.get(neighborIndex).getServerIP();
				String neighborPort = STATIC_NETWORK.get(neighborIndex).getServerPort();
				
				ServerInfo serverInfo = new ServerInfo();
				serverInfo.setServerIP(neighborIP);
				serverInfo.setServerPort(neighborPort);
				
				neighborServerList.add(serverInfo);
			}
		}
	}
	
	/**
	 * Set my server information for using the information conveniently in the program
	 */
	public static void SetMyServerInfo() {
		System.out.println("Your server ID is ? (1-" + MAX_NUMBER_OF_SERVER + ")");
		while(true) {
			myServerID = sc.nextInt();
			
			if(myServerID < 1 || myServerID > MAX_NUMBER_OF_SERVER) {
				System.out.println("Invalid Server ID");	
				continue;
			} else { 
				myServerIndex = myServerID-1;
				myServerPort = STATIC_NETWORK.get(myServerIndex).getServerPort();
				myServerIP = STATIC_NETWORK.get(myServerIndex).getServerIP();
				myServerTTR = STATIC_NETWORK.get(myServerIndex).getServerTTR();
				break;	
			}
		}
		sharedDirectoryPath = sharedDirectoryPath + myServerID;
		downloadLog = new File(Gnutella.sharedDirectoryPath + "/Downloads" + "/downloadLog");
	}
	
	/**
	 * Set shared directory and list up the content (files) of directory
	 * @return true when shared directory is ready
	 */
	public static boolean SetSharedDirectory() {
		sharedDirectory = new File(sharedDirectoryPath);
		// check whether the path is valid shared directory
		if(!sharedDirectory.isDirectory()) {
			System.out.println("Warning: " + sharedDirectoryPath + " is not a directory");
			return false;
		}
		// store list of files to the local
		fileList = sharedDirectory.listFiles();
		stringFileList = sharedDirectory.list();
		System.out.println("Shared Directory is Ready");
		return true;
	}
	
	/**
	 * Set version of master files to 0 and set last modified time of master files
	 * @return true when initialization was successful
	 */
	public static boolean InitializeMasterFiles() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH-mm-ss");
		for (File file : fileList) {
			String fileName = file.getName();
			lastModified.put(fileName, sdf.format(file.lastModified()));
		}
		for (String file : stringFileList) {
			MASTER_FILES.put(file, 0);
		}
		return true;
	}
}