package filegraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.phoenixkahlo.nodenet.proxy.Proxy;

public class TransferService {

	private static final int THREAD_COUNT = 10;

	private List<Thread> threads = new ArrayList<>();
	private Queue<IndexRange> queue;
	private Proxy<AvailableFile> upload;
	private Proxy<FileReceiver> download;

	public TransferService(Proxy<AvailableFile> upload, Proxy<FileReceiver> download) {
		this.upload = upload;
		this.download = download;
	}

	public void start() {
		queue = new ConcurrentLinkedQueue<>();
		for (IndexRange range : download.blocking().getNeededSections()) {
			queue.add(range);
		}
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads.add(new Thread(() -> {
				try {
					IndexRange section;
					while ((section = queue.poll()) != null) {
						download.blocking().provide(section, upload.blocking().getSection(section));
					}
				} catch (Exception e) {
					System.err.println("Exception in transfer service");
					e.printStackTrace();
				}
			}));
		}
		threads.forEach(Thread::start);
	}

	public void stop() {
		threads.forEach(Thread::interrupt);
	}

}
