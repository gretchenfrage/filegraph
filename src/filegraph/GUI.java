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
import javax.swing.JDialog;
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
		provide.addActionListener(event -> provideButton());
		panel.add(provide);

		panel.setPreferredSize(new Dimension(300, 500));
		return panel;
	}

	private Component makeSection2() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);

		JButton connect = new JButton("Connect");
		connect.addActionListener(event -> connectButton());
		panel.add(connect);
		
		panel.add(makeLabel("Remote Clients:"));

		remoteRoot = new DefaultMutableTreeNode();

		remoteTree = new JTree(remoteRoot);
		remoteTree.setRootVisible(false);
		JScrollPane remoteTreeView = new JScrollPane(remoteTree);
		panel.add(remoteTreeView);

		JButton download = new JButton("Download");
		download.addActionListener(event -> downloadButton());
		panel.add(download);

		panel.setPreferredSize(new Dimension(300, 500));
		return panel;
	}
	
	private Component makeSection3() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);
 

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

	private void provideButton() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			client.provideFile(fileChooser.getSelectedFile());
		}
	}

	private void connectButton() {
		JDialog dialog = new JDialog(frame);
		dialog.setContentPane(makeConnectPane());
		dialog.setSize(500, 200);
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}
	
	private JPanel makeConnectPane() {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);
		
		JPanel addressPanel = new JPanel();
		LayoutManager addressPanelLayout = new BoxLayout(addressPanel, BoxLayout.X_AXIS);
		addressPanel.setLayout(addressPanelLayout);
		
		JTextField address = new JTextField();
		address.setMaximumSize(new Dimension(Integer.MAX_VALUE, address.getPreferredSize().height));
		addressPanel.add(address);
		JTextField port = new JTextField();
		port.setMaximumSize(new Dimension(100, port.getPreferredSize().height));
		addressPanel.add(port);
		
		panel.add(addressPanel);

		JButton connect = new JButton("Connect");
		panel.add(connect);
		
		return panel;
	}
	
	private void downloadButton() {
		
	}
	
}
