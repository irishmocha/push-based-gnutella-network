package servent;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;

public class FileUploader extends Thread {

	private Socket uploaderSocket;
	private DataOutputStream dos;	
	private FileInputStream fis;
	private BufferedInputStream bis;
	private String fileToSend;

	public FileUploader(String requestedFile, String requestorIP, int requestorPort) {
		this.fileToSend = requestedFile;
		// connect to the requestor peer to transfer the requested file
		try {
			uploaderSocket = new Socket(requestorIP, 15000);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			dos = new DataOutputStream(uploaderSocket.getOutputStream());
			String filePath = Gnutella.sharedDirectoryPath + "/" + fileToSend;
			File file = new File(filePath);
			
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			
			byte[] fileToByte = new byte[(int) file.length()];
			
			bis.read(fileToByte, 0, fileToByte.length);
			dos.write(fileToByte, 0, fileToByte.length);

			// clear streams
			dos.flush();
			dos.close();
			bis.close();
			fis.close();
			System.out.println("Upload Complete");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
