package filegraph;

import java.util.List;

import com.phoenixkahlo.util.Tuple;
import com.phoenixkahlo.util.UUID;

public interface Client {

	String getName();
	
	List<Tuple<UUID, String>> getAvailableFiles();
	
	/**
	 * Refresh all RemoteClientCaches, then refresh the GUI.
	 */
	void refresh();
	
}
