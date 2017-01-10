package filegraph;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.phoenixkahlo.nodenet.BasicLocalNode;
import com.phoenixkahlo.nodenet.LocalNode;
import com.phoenixkahlo.nodenet.Node;
import com.phoenixkahlo.nodenet.proxy.Proxy;
import com.phoenixkahlo.util.Tuple;
import com.phoenixkahlo.util.UUID;

public class LocalClient implements Client {

	public static void main(String[] args) throws SocketException {
		new LocalClient(OptionalInt.of(ThreadLocalRandom.current().nextInt(1000) + 3000));
	}

	private GUI gui;
	private LocalNode network;
	private List<AvailableFile> availableFiles = Collections.synchronizedList(new ArrayList<>());
	private List<Proxy<Client>> remote = Collections.synchronizedList(new ArrayList<>());

	@SuppressWarnings("unchecked")
	public LocalClient(OptionalInt port) throws SocketException {
		gui = new GUI(this, port.isPresent() ? "- port " + Integer.toString(port.getAsInt()) : "");

		if (port.isPresent())
			network = new BasicLocalNode(port.getAsInt());
		else
			network = new BasicLocalNode();

		network.addSerializer(Tuple.serializer(network.getSerializer()), 1);

		network.listenForJoin(node -> {
			try {
				node.send(network.makeProxy(LocalClient.this, Client.class));
				remote.add(node.receive(Proxy.class).cast(Client.class));
				refresh();
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
		availableFiles.add(new LocalAvailableFile(file, new UUID()));
		refreshAll();
		gui.refreshLocal();
	}

	/**
	 * Called by GUI.
	 */
	public void rescindFile(UUID id) {
		availableFiles.removeIf(file -> file.getID().equals(id));
		refreshAll();
		gui.refreshLocal();
	}

	/**
	 * Called by GUI.
	 */
	public String connect(String address, String port) {
		try {
			Optional<Node> connection = network.connect(new InetSocketAddress(address, Integer.parseInt(port)));
			if (connection.isPresent())
				return "Successful";
			else
				return "Failed";
		} catch (Exception e) {
			return "Failed with exception: " + e;
		}
	}

	/**
	 * Called by GUI.
	 */
	public List<Proxy<Client>> getRemote() {
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
	 * Called by remote and GUI.
	 */
	@Override
	public List<Proxy<AvailableFile>> getAvailableFiles() {
		synchronized (availableFiles) {
			return availableFiles.stream().map(file -> network.makeProxy(file, AvailableFile.class))
					.collect(Collectors.toList());
		}
	}

	/**
	 * Refresh all clients, including self.
	 */
	public void refreshAll() {
		remote.forEach(proxy -> proxy.unblocking(false).refresh());
		refresh();
	}

	/**
	 * Called by remote.
	 */
	@Override
	public void refresh() {
		gui.refreshRemote();
	}

	/**
	 * Called by GUI.
	 */
	public void shutdown() {
		network.disconnect();
	}

}
