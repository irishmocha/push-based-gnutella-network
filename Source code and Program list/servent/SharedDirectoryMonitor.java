package servent;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;

public class SharedDirectoryMonitor extends Thread{
	
	private Path sharedDirectoryPath;
	
	private WatchKey watchKey;
	private WatchService watchService;
	
	public SharedDirectoryMonitor(String strPath) {
		sharedDirectoryPath 	= Paths.get(strPath);	
		try {
			watchService = FileSystems.getDefault().newWatchService();
			watchKey = sharedDirectoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(true) {
			for(WatchEvent<?> watchEvent : watchKey.pollEvents()) {
				System.out.println(watchEvent.kind() + ": " + watchEvent.context());
				String detectedFile = watchEvent.context().toString();
				String detectedEvent = watchEvent.kind().toString();
				// add filename when a file downloaded or created file into master file
				if (detectedEvent.equals("ENTRY_CREATE")) {
					Gnutella.MASTER_FILES.put(detectedFile, 0);
				} 
				// delete file from master files when a file deleted from disk
				else if (detectedEvent.equals("ENTRY_DELETE")) {
					Gnutella.MASTER_FILES.remove(detectedFile);
				}
				// detect modification of file then send invalidation message or set last modified time according to the consistency policy
				else if (detectedEvent.equals("ENTRY_MODIFY")) {
					SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH-mm-ss");
					// avoid detecting DS_Store of OS X or download folder
					if(detectedFile.equals(".DS_Store") || detectedFile.equals("Downloads")) {
						continue;
					}
					// update file version
					Gnutella.MASTER_FILES.computeIfPresent(detectedFile, (k, v) -> v + 1);
					System.out.println("Updated Version No. of " + detectedFile + ": " + Gnutella.MASTER_FILES.get(detectedFile));			
					// send invalidation message with new version number if the network is push-based
					if (Gnutella.consistency.equals("0")) {
						MessageID mid = new MessageID();
						mid.setSequenceNumber(Gnutella.globalSequenceNumber);
						File file = new File(Gnutella.sharedDirectoryPath + "/" + detectedFile);
						Gnutella.lastModified.put(detectedFile, sdf.format(file.lastModified()));
						new PushBasedClient().Invalidation(mid, Gnutella.myServerID, detectedFile, Gnutella.MASTER_FILES.get(detectedFile));
					}
					// set new modified time if the network is pull-based
					if (Gnutella.consistency.equals("1")) {
						File file = new File(Gnutella.sharedDirectoryPath + "/" + detectedFile);
						Gnutella.lastModified.put(detectedFile, sdf.format(file.lastModified()));
					}
				} 
			}
			if (!watchKey.reset()) {
				System.out.println("Invalid Directory");
				break;
			}
		}
	}
}
