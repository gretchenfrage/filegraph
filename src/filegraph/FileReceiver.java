package filegraph;

import java.io.IOException;
import java.util.List;

public interface FileReceiver {

	List<IndexRange> getNeededSections();
	
	void provide(IndexRange section, byte[] data) throws IOException, IllegalArgumentException;
	
	void finish(long checksum);
	
}
