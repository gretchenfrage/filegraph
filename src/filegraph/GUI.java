package filegraph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
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
import javax.swing.tree.TreePath;

import com.phoenixkahlo.nodenet.proxy.Proxy;
import com.phoenixkahlo.util.UUID;

public class GUI {

	private LocalClient client;

	private Font font = new Font("arial", Font.PLAIN, 12);
	private JFrame frame;
	private JPanel availableFiles;
	private JTree remoteTree;
	private DefaultMutableTreeNode remoteRoot;
	private JPanel progressingDownloads;

	private volatile UUID localSelected = null;
	private volatile Consumer<UUID> localSelector = id -> {
	};

	private volatile Runnable downloadService = () -> {
	};

	public GUI(LocalClient client, String label) {
		this.client = client;

		frame = new JFrame("File Graph Client " + label);
		JPanel contentPane = new JPanel();
		contentPane.add(makeSection1());
		contentPane.add(makeSection2());
		contentPane.add(makeSection3());
		frame.setContentPane(contentPane);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent event) {
				client.shutdown();
			}

		});
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

		JButton rescind = new JButton("Rescind");
		rescind.addActionListener(event -> rescindButton());
		panel.add(rescind);

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

		JButton download = new JButton("Download");
		download.addActionListener(event -> downloadButton());
		panel.add(download);

		JButton connect = new JButton("Connect");
		connect.addActionListener(event -> connectButton());
		panel.add(connect);

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

	public void refreshRemote() {
		SwingUtilities.invokeLater(() -> {
			remoteTree.setRootVisible(true);

			Set<String> opened = new HashSet<>();
			for (int i = 0; i < remoteRoot.getChildCount(); i++) {
				if (remoteTree.isExpanded(new TreePath(((DefaultMutableTreeNode) remoteRoot.getChildAt(i)).getPath())))
					opened.add((String) ((DefaultMutableTreeNode) remoteRoot.getChildAt(i)).getUserObject());
			}

			while (remoteRoot.getChildCount() > 0)
				((DefaultTreeModel) remoteTree.getModel())
						.removeNodeFromParent((MutableTreeNode) remoteRoot.getFirstChild());

			client.getRemote().forEach(proxy -> {
				try {
					DefaultMutableTreeNode clientNode = new DefaultMutableTreeNode(proxy.blocking().getName());
					((DefaultTreeModel) remoteTree.getModel()).insertNodeInto(clientNode, remoteRoot, 0);
					for (Proxy<AvailableFile> file : proxy.blocking().getAvailableFiles()) {
						try {
							DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(file.blocking().getName());
							
							((DefaultTreeModel) remoteTree.getModel()).insertNodeInto(fileNode, clientNode, 0);
						} catch (Exception e) {
						}
					}
				} catch (Exception e) {
				}
			});

			for (int i = 0; i < remoteTree.getModel().getChildCount(remoteRoot); i++) {
				if (opened.contains(
						((DefaultMutableTreeNode) remoteTree.getModel().getChild(remoteRoot, i)).getUserObject()))
					remoteTree.expandPath(new TreePath(((DefaultMutableTreeNode) remoteRoot.getChildAt(i)).getPath()));
			}

			remoteTree.expandRow(0);
			remoteTree.setRootVisible(false);

			frame.revalidate();
			frame.repaint();
		});
	}

	public void refreshLocal() {
		SwingUtilities.invokeLater(() -> {
			availableFiles.removeAll();
			Map<UUID, JLabel> labels = Collections.synchronizedMap(new HashMap<>());
			client.getAvailableFiles().forEach(proxy -> {
				try {
					UUID id = proxy.blocking().getID();
					String name = proxy.blocking().getName();
					System.out.println("adding " + name + " " + id);
					JLabel label = new JLabel(name);
					labels.put(id, label);
					label.setOpaque(true);
					label.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent event) {
							localSelector.accept(id);
						}
					});
					availableFiles.add(label);
				} catch (Exception e) {
				}
			});
			localSelector = id -> {
				localSelected = id;
				SwingUtilities.invokeLater(() -> {
					for (JLabel label : labels.values()) {
						label.setBackground(Color.WHITE);
					}
					labels.get(id).setBackground(Color.CYAN);
					frame.revalidate();
					frame.repaint();
				});
			};
			frame.revalidate();
			frame.repaint();
		});
	}

	private void provideButton() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			new Thread(() -> {
				client.provideFile(fileChooser.getSelectedFile());
			}).start();
		}
	}

	private void connectButton() {
		JDialog dialog = new JDialog(frame, "Connect");
		BiConsumer<String, String> connectCallback = (address, port) -> {
			JPanel contentPane2 = new JPanel();
			contentPane2.add(new JLabel("Trying to connect to " + address + " : " + port + "..."));
			dialog.setContentPane(contentPane2);
			dialog.revalidate();
			dialog.repaint();

			new Thread(() -> {
				String result = client.connect(address, port);
				JPanel contentPane3 = new JPanel();
				contentPane3.add(new JLabel(result));
				JButton ok = new JButton("Ok");
				ok.addActionListener(event -> dialog.dispose());
				contentPane3.add(ok);
				SwingUtilities.invokeLater(() -> {
					dialog.setContentPane(contentPane3);
					dialog.revalidate();
					dialog.repaint();
				});
			}).start();
		};
		dialog.setContentPane(makeConnectPane(connectCallback));
		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	private JPanel makeConnectPane(BiConsumer<String, String> connectCallback) {
		JPanel panel = new JPanel();
		LayoutManager layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(layout);

		JPanel addressPanel = new JPanel();
		LayoutManager addressPanelLayout = new BoxLayout(addressPanel, BoxLayout.X_AXIS);
		addressPanel.setLayout(addressPanelLayout);

		JTextField address = new JTextField();
		address.setMaximumSize(new Dimension(Integer.MAX_VALUE, address.getPreferredSize().height));
		address.setPreferredSize(new Dimension(250, address.getPreferredSize().height));
		addressPanel.add(address);
		JTextField port = new JTextField();
		port.setMaximumSize(new Dimension(100, port.getPreferredSize().height));
		port.setPreferredSize(new Dimension(100, port.getPreferredSize().height));
		addressPanel.add(port);

		panel.add(addressPanel);

		JButton connect = new JButton("Connect");
		connect.addActionListener(event -> connectCallback.accept(address.getText(), port.getText()));
		panel.add(connect);

		return panel;
	}

	private void downloadButton() {

	}

	private void rescindButton() {
		client.rescindFile(localSelected);
	}

}
