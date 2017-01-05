package filegraph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.net.InetSocketAddress;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import com.phoenixkahlo.util.Tuple;
import com.phoenixkahlo.util.UUID;

public class GUI {

	private LocalClient client;

	private Font font = new Font("arial", Font.PLAIN, 12);
	private JFrame frame;
	private JPanel availableFiles;
	private JTree remoteTree;
	private DefaultMutableTreeNode remoteRoot;
	private JPanel progressingDownloads;

	public GUI(LocalClient client, String label) {
		this.client = client;

		frame = new JFrame("File Graph Client " + label);
		JPanel contentPane = new JPanel();
		contentPane.add(makeSection1());
		contentPane.add(makeSection2());
		contentPane.add(makeSection3());
		frame.setContentPane(contentPane);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private Component makeSection1() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);

		panel.add(makeLabel("My Available Files:"));

		availableFiles = new JPanel();
		LayoutManager availableFilesLayout = new BoxLayout(availableFiles, BoxLayout.Y_AXIS);
		availableFiles.setLayout(availableFilesLayout);
		availableFiles.setBackground(Color.WHITE);

		JScrollPane availableFilesView = new JScrollPane(availableFiles);
		panel.add(availableFilesView);

		JButton provide = new JButton("Provide");
		provide.addActionListener(event -> provideFile());
		panel.add(provide);

		panel.setPreferredSize(new Dimension(300, 500));
		return panel;
	}

	private Component makeSection2() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);

		panel.add(makeLabel("Remote Clients:"));

		remoteRoot = new DefaultMutableTreeNode();

		remoteTree = new JTree(remoteRoot);
		remoteTree.setRootVisible(false);
		JScrollPane remoteTreeView = new JScrollPane(remoteTree);
		panel.add(remoteTreeView);

		JButton refresh = new JButton("Refresh");
		refresh.addActionListener(event -> refreshRemote());
		panel.add(refresh);

		panel.setPreferredSize(new Dimension(300, 500));
		return panel;
	}

	private Component makeSection3() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);

		panel.add(makeLabel("Connect To:"));
		panel.add(makeOutreachPanel());

		panel.add(makeLabel("Downloads in Progress:"));

		progressingDownloads = new JPanel();
		LayoutManager progressingDownloadsLayout = new BoxLayout(progressingDownloads, BoxLayout.Y_AXIS);
		progressingDownloads.setLayout(progressingDownloadsLayout);
		progressingDownloads.setBackground(Color.WHITE);

		JScrollPane progressingDownloadsView = new JScrollPane(progressingDownloads);
		panel.add(progressingDownloadsView);

		panel.setPreferredSize(new Dimension(300, 500));
		return panel;
	}

	private Component makeOutreachPanel() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(layout);

		JTextField ipField = new JTextField();
		JTextField portField = new JTextField();
		Icon submitIcon = new ImageIcon(getClass().getResource("/assets/connect.png"));
		JButton submitButton = new JButton(submitIcon);

		final int PORT_WIDTH = 50;
		final int FIELD_HEIGHT = submitButton.getPreferredSize().height;

		ipField.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
		portField.setMaximumSize(new Dimension(PORT_WIDTH, FIELD_HEIGHT));
		portField.setPreferredSize(new Dimension(PORT_WIDTH, FIELD_HEIGHT));

		panel.add(ipField);
		panel.add(makeLabel(":"));
		panel.add(portField);
		panel.add(submitButton);

		submitButton.addActionListener(event -> connect(ipField.getText(), portField.getText()));

		return panel;
	}
	
	private JLabel makeLabel(String text) {
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setFont(font);
		return label;
	}

	public void start() {
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public void refresh() {
		SwingUtilities.invokeLater(() -> {
			// Refresh remote tree
			remoteTree.setRootVisible(true);
			
			while (remoteRoot.getChildCount() > 0)
				((DefaultTreeModel) remoteTree.getModel()).removeNodeFromParent((MutableTreeNode) remoteRoot.getFirstChild());
			
			synchronized (client.getRemote()) {
				for (RemoteClientCache cache : client.getRemote()) {
					DefaultMutableTreeNode node = new DefaultMutableTreeNode(cache.getName());
					((DefaultTreeModel) remoteTree.getModel()).insertNodeInto(node, remoteRoot, 0);
					for (Tuple<UUID, String> file : cache.getAvailableFiles()) {
						((DefaultTreeModel) remoteTree.getModel()).insertNodeInto(new DefaultMutableTreeNode(file.getB()), node, 0);
					}
				}
			}
			
			remoteTree.expandRow(0);
			remoteTree.setRootVisible(false);
			// Display changes
			frame.revalidate();
			frame.repaint();
		});
	}
	
	/**
	 * Response to refresh button.
	 */
	private void refreshRemote() {
		System.out.println("no");
		//SwingUtilities.invokeLater(() -> {
		//	remoteTree.setRootVisible(true);
		//	
		//	while (remoteRoot.getChildCount() > 0)
		//		((DefaultTreeModel) remoteTree.getModel()).removeNodeFromParent((MutableTreeNode) remoteRoot.getFirstChild());
		//	
		//	List<Proxy<Client>> remoteClients = client.getRemote();
		//	synchronized (remoteClients) {
		//		for (Proxy<Client> client : remoteClients) {
		//			String name = client.blocking().getName();
		//			System.out.println("found client with name: " + name);
		//			DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
		//			//remoteRoot.add(node);
		//			((DefaultTreeModel) remoteTree.getModel()).insertNodeInto(node, remoteRoot, 0);
		//		}
		//	}
		//	System.out.println("revalidating & repainting");
		//	remoteTree.expandRow(0);
		//	remoteTree.setRootVisible(false);
		//	frame.revalidate();
		//	frame.repaint();
		//});
	}

	/**
	 * Response to the connect button.
	 */
	private void connect(String address, String port) {
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(address, Integer.parseInt(port));
			client.connect(socketAddress);
		} catch (Exception e) {
		}

	}

	/**
	 * Response to the provide button.
	 */
	private void provideFile() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			client.provideFile(fileChooser.getSelectedFile());
		}
	}

}
