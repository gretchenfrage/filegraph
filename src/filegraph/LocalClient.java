package filegraph;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.Scanner;

import com.phoenixkahlo.nodenet.BasicLocalNode;
import com.phoenixkahlo.nodenet.LocalNode;
import com.phoenixkahlo.nodenet.proxy.Proxy;

public class LocalClient implements Client {

	public static void main(String[] args) throws SocketException {
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.print("provide port:\n> ");
			new LocalClient(OptionalInt.of(scanner.nextInt()));
		}
	}
	
	private GUI gui;
	private LocalNode network;
	private Proxy<Client> selfProxy;
	private List<Proxy<Client>> remoteClients = Collections.synchronizedList(new ArrayList<>());
	
	public LocalClient(OptionalInt port) throws SocketException {
		gui = new GUI(this, port.toString());
		if (port.isPresent())
			network = new BasicLocalNode(port.getAsInt());
		else
			network = new BasicLocalNode();
		selfProxy = network.makeProxy(this, Client.class);
		network.acceptAllIncoming();
		network.listenForJoin(node -> {
			try {
				node.send(selfProxy);
				remoteClients.add(((Proxy<?>) node.receive()).cast(Client.class));
			} catch (Exception e) {
			}
		});
		network.listenForLeave(node -> {
			remoteClients.removeIf(proxy -> proxy.getSource().equals(node.getAddress()));
		});
		gui.start();
	}
	
	public void provideFile(File file) {
		
	}
	
	public void connect(InetSocketAddress address) {
		network.connect(address);
	}
	
	public List<Proxy<Client>> getRemote() {
		return remoteClients;
	}
	
	@Override
	public String getName() {
		return network.getAddress() + " " + System.getProperty("user.name");
	}
	
}
