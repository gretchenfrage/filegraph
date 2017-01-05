package filegraph;

import java.util.List;
import java.util.function.Consumer;

import com.phoenixkahlo.nodenet.proxy.Proxy;
import com.phoenixkahlo.nodenet.proxy.RuntimeDisconnectionException;
import com.phoenixkahlo.nodenet.proxy.RuntimeProxyException;
import com.phoenixkahlo.util.Tuple;
import com.phoenixkahlo.util.UUID;

public class RemoteClientCache {

	private Proxy<Client> proxy;
	private Consumer<RemoteClientCache> remove;

	private String name;
	private List<Tuple<UUID, String>> availableFiles;

	public RemoteClientCache(Proxy<Client> proxy, Consumer<RemoteClientCache> remove) {
		this.proxy = proxy;
		this.remove = remove;
		refreshCache();
	}

	/**
	 * Refresh the cache of the remote client.
	 */
	public void refreshCache() {
		try {
			name = proxy.blocking().getName();
			availableFiles = proxy.blocking().getAvailableFiles();
		} catch (RuntimeDisconnectionException | RuntimeProxyException e) {
			remove.accept(this);
		}
	}

	/**
	 * Call refresh on the remote client.
	 */
	public void refreshRemote() {
		proxy.unblocking(false).refresh();
	}

	public String getName() {
		return name;
	}

	public List<Tuple<UUID, String>> getAvailableFiles() {
		return availableFiles;
	}

}
