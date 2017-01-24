package filegraph;

import java.util.List;

import com.phoenixkahlo.nodenet.proxy.Proxy;

public interface Client {

	String getName();
	
	List<Proxy<AvailableFile>> getAvailableFiles();
	
	void refresh();
	
	void startDownloadService(Proxy<AvailableFile> upload, Proxy<FileReceiver> download);
	
}
