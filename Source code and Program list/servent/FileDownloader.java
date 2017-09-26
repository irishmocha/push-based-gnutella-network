package servent;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileDownloader extends Thread {

	private final int MAX_SIZE = 65536;
	
	private ServerSocket serverSocket;
	private Socket downloaderSocket;
	
	private DataInputStream dis;
	private FileOutputStream fos;
	
	private String fileToDownload;
	private String fileVersion;
	private String originServerIP;
	private String originServerPort;
	
	public FileDownloader(String filename, String version, String originServerIP, int originServerPort) {
		System.out.println("File downloader activated");
		// set the file name to download and original server information that holds master copy 
		this.fileToDownload = filename;
		this.fileVersion = version;
		this.originServerIP = originServerIP;
		this.originServerPort = Integer.toString(originServerPort);
		try {
			serverSocket = new ServerSocket(15000);
			while (true) {
				downloaderSocket = serverSocket.accept();
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			// Download a file
			File file = new File(Gnutella.sharedDirectoryPath + "/Downloads/" + fileToDownload);
			fos = new FileOutputStream(file);
			dis = new DataInputStream(downloaderSocket.getInputStream());
			int length;
			byte[] data = new byte[MAX_SIZE];
			
			System.out.println("display file " + fileToDownload);
			while ((length = dis.read(data)) != -1) {
				fos.write(data, 0, length);
			}
			fos.close();
			dis.close();
			Gnutella.DOWNLOADED_FILES.put(fileToDownload, "valid");
			// if network is pull-based, run the TTR checker regarding the file
			if (Gnutella.consistency.equals("1")) {
				System.out.println("download->ttr activate");
				Thread ttrChecker = new Thread(new TTRChecker(fileToDownload, Gnutella.pullFileInfo.get(fileToDownload).split("/")[1]));
				ttrChecker.start();
			}
			System.out.println("Download Complete");
			serverSocket.close();
			Thread.currentThread().interrupt();
		} catch (IOException e) {
		} catch (Exception e) {
		} finally {
			
		}
	}
}
