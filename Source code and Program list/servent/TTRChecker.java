package servent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TTRChecker extends Thread {
	
	private String fileName;
	private int TTR;
	
	public TTRChecker(String fileName, String TTR) {
		System.out.println("TTR Checker activated for the file " + fileName + " / TTR: " + TTR);
		
		// set the ttr checker with file name and it's ttr
		this.fileName = fileName;
		this.TTR = Integer.parseInt(TTR);
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(TTR*1000);
				
				// set the status of file to TTR Expired
				Gnutella.DOWNLOADED_FILES.put(fileName, "TTRExpired");
				System.out.println("TTR expired. Sending pull request message");
				
				// then send new pull request message to the original server to get the information of the file.
				new PullBasedClient().PullRequest(fileName, Gnutella.pullFileInfo.get(fileName).split("/")[0]);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void SetTTRExpired(String filename, int version) {
		FileReader fr;
		FileWriter fw;
		BufferedReader br;
		BufferedWriter bw;
		String line;
		String newLog = "";
		//Boolean flag = null;
		try {
			fw = new FileWriter(Gnutella.downloadLog, true);
			fr = new FileReader(Gnutella.downloadLog);
			br = new BufferedReader(fr);
			bw = new BufferedWriter(fw);
			
			while((line = br.readLine()) != null) {
				System.out.println("line::"+line);
				if(line.length() == 0) {
					break;
				}
				String[] temp = line.split("/");
				System.out.println("temp[0]: " + temp[0] + " temp[1]: " + temp[1] + " temp[2]: " + temp[2] + " temp[3]: " + temp[3]);
				if(temp[0].equals(filename) && temp[3].equals("valid")) {
					newLog = newLog + temp[0] + "/" + temp[1] + "/" + temp[2] + "/" + "TTRExpired" + "\n";
					System.out.println("Invalidation1: newLog: " + newLog);
					//flag = true;
				} else {
					newLog = newLog + line + "\n";
					System.out.println("Invalidation2: newLog: " + newLog);
					//flag = false;
				}
				System.out.println("while newLog: " + newLog);
			}
			fw.flush();
			bw.flush();
			fr.close();
			fw.close();
			br.close();
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
