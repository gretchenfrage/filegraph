package filegraph;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

import com.phoenixkahlo.nodenet.BasicLocalNode;
import com.phoenixkahlo.nodenet.LocalNode;
import com.phoenixkahlo.nodenet.proxy.Proxy;
import com.phoenixkahlo.util.Tuple;
import com.phoenixkahlo.util.UUID;

public class LocalClient implements Client {

	public static void main(String[] args) throws SocketException {
		new LocalClient(OptionalInt.of(ThreadLocalRandom.current().nextInt(1000) + 3000));
	}

	private GUI gui;
	private List<Tuple<UUID, String>> availableFiles;
	private LocalNode network;
	private List<RemoteClientCache> remote = Collections.synchronizedList(new ArrayList<>());

	@SuppressWarnings("unchecked")
	public LocalClient(OptionalInt port) throws SocketException {
		gui = new GUI(this, port.isPresent() ? "- port " + Integer.toString(port.getAsInt()) : "");
		

		availableFiles = new ArrayList<>();
		availableFiles.add(new Tuple<UUID, String>(new UUID(), "it's fun to stay at the"));
		availableFiles.add(new Tuple<UUID, String>(new UUID(), "Y"));
		availableFiles.add(new Tuple<UUID, String>(new UUID(), "M"));
		availableFiles.add(new Tuple<UUID, String>(new UUID(), "C"));
		availableFiles.add(new Tuple<UUID, String>(new UUID(), "A"));

		if (port.isPresent())
			network = new BasicLocalNode(port.getAsInt());
		else
			network = new BasicLocalNode();
		
		network.addSerializer(Tuple.serializer(network.getSerializer()), 1);
		
		network.listenForJoin(node -> {
			try {
				node.send(network.makeProxy(LocalClient.this, Client.class));
				remote.add(new RemoteClientCache(node.receive(Proxy.class).cast(Client.class), remote::remove));
				refreshAll();
			} catch (Exception e) {
			}
		});
		network.listenForLeave(node -> refreshAll());
		gui.start();
		network.acceptAllIncoming();
	}

	/**
	 * Called by GUI.
	 */
	public void provideFile(File file) {

	}

	/**
	 * Called by GUI.
	 */
	public void connect(InetSocketAddress address) {
		new Thread(() -> {
			network.connect(address);
		}).start();
	}
	
	/**
	 * Called by GUI.
	 */
	public List<RemoteClientCache> getRemote() {
		return remote;
	}

	/**
	 * Called by remote.
	 */
	@Override
	public String getName() {
		return System.getProperty("user.name") + " " + network.getAddress().toString().substring(1, 8);
	}

	/**
	 * Called by remote and GUi.
	 */
	@Override
	public List<Tuple<UUID, String>> getAvailableFiles() {
		return availableFiles;
	}

	/**
	 * Refresh all clients, including self.
	 */
	public void refreshAll() {
		refresh();
		remote.forEach(RemoteClientCache::refreshRemote);
	}

	@Override
	public void refresh() {
		synchronized (remote) {
			for (int i = remote.size() - 1; i >= 0; i--) {
				remote.get(i).refreshCache();
			}
		}
		gui.refresh();
	}

}
