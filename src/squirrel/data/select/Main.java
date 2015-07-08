package squirrel.data.select;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import squirrel.data.json.JSON_Document;
import squirrel.util.UTIL_FileOperations;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

/**
 * Helper tool for selecting relevant QAs to form a collection for further
 * experimentation.
 * 
 * @author Steffen Zschaler
 * 
 */
@SuppressWarnings("serial")
public class Main extends JFrame {

	public static void main(String[] args) {
		new Main().setVisible(true);
	}

	private DefaultListModel<File> lmSourceList;
	private DefaultListModel<File> lmTargetList;
	private List<File> lfRemovedFiles = new ArrayList<File>(); // List of
																// removed files
	private File fSourceDirectory = null;
	private JLabel jlSourceData;
	private JList<File> jlSourceList;
	private JList<File> jlTargetList;
	private JTextArea jtaSource;
	private JTextArea jtaTarget;

	public Main() {
		super("Collection Manager");

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				setSourceDirectory(null);
			}
		});

		initWidgets();
		initMenuBar();

		pack();
	}

	@SuppressWarnings("serial")
	private void initMenuBar() {
		JMenuBar jmb = new JMenuBar();
		JMenu jmFile = new JMenu("File");
		jmb.add(jmFile);

		jmFile.add(new JMenuItem(new AbstractAction("Select Source...") {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				doSelectSource();
			}
		}));
		jmFile.add(new JMenuItem(new AbstractAction("Export...") {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				doExportCollection();
			}
		}));

		setJMenuBar(jmb);
	}

	protected void doExportCollection() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			exportToDirectory(jfc.getSelectedFile());
		}
	}

	private void exportToDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
		Enumeration<File> efTargets = lmTargetList.elements();
		while (efTargets.hasMoreElements()) {
			File fCurrent = efTargets.nextElement();

			try {
				UTIL_FileOperations.copyFile(fCurrent,
						new File(dir, fCurrent.getName()));
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this, e.getLocalizedMessage(),
						"Error!", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void doSelectSource() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			setSourceDirectory(jfc.getSelectedFile());
		}
	}

	/**
	 * Set the source directory.
	 * 
	 * @param directory
	 *            the source directory. May be <code>null</code>.
	 */
	protected void setSourceDirectory(File directory) {
		if (fSourceDirectory != null) {
			File fCollectionData = new File(fSourceDirectory, ".collectionInfo");
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						fCollectionData));

				bw.write("Collection: " + fSourceDirectory.getName());
				bw.newLine();

				bw.write("Date: "
						+ DateFormat.getDateInstance().format(new Date()));
				bw.newLine();

				bw.write("[Include]");
				bw.newLine();

				Enumeration<File> _enum = lmTargetList.elements();
				while (_enum.hasMoreElements()) {
					File f = _enum.nextElement();

					bw.write(f.getAbsolutePath());
					bw.newLine();
				}

				bw.write("[Exclude]");
				bw.newLine();
				for (File f : lfRemovedFiles) {
					bw.write(f.getAbsolutePath());
					bw.newLine();
				}

				bw.close();
			} catch (IOException ioe) {
				System.err.println("Couldn't save collection info: "
						+ ioe.getMessage());
			}
		}
		if ((directory != null) && (directory.isDirectory())) {
			fSourceDirectory = directory;

			// First put all files found into source list
			lmSourceList.clear();
			for (File f : fSourceDirectory.listFiles(new FileFilter() {

				@Override
				public boolean accept(File pathname) {
					return pathname.isFile()
							&& pathname.getName().endsWith(".json");
				}
			})) {

				lmSourceList.addElement(f);
			}

			// If there is a list of previous selections, load it and update the
			// list accordingly...
			File fCollectionData = new File(fSourceDirectory, ".collectionInfo");
			if (fCollectionData.exists() && fCollectionData.isFile()) {
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(fCollectionData));

					// Skip name and date of collection
					br.readLine();
					br.readLine();

					if (br.readLine().equals("[Include]")) {
						// We're in the right position, read following lines and
						// update lists accordingly...
						boolean fInclude = true;
						String sLine = br.readLine();
						while (sLine != null) {
							if (sLine.equals("[Exclude]")) {
								fInclude = false;
							} else {
								File f = new File(sLine);
								lmSourceList.removeElement(f);
								if (fInclude) {
									lmTargetList.addElement(f);
								} else {
									lfRemovedFiles.add(f);
								}
							}
							sLine = br.readLine();
						}

					}

				} catch (IOException ioe) {
					ioe.printStackTrace();
				} finally {
					try {
						if (br != null) {
							br.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
			
			//updateSourceLabel();
		}
	}

	private void initWidgets() {
		Box bMain = Box.createHorizontalBox();
		add(bMain);
		JPanel jpLeft = new JPanel(new BorderLayout());
		bMain.add(jpLeft);
		Box bMiddle = Box.createVerticalBox();
		bMain.add(bMiddle);
		JPanel jpRight = new JPanel(new BorderLayout());
		bMain.add(jpRight);

		@SuppressWarnings("serial")
		class JSONRenderer extends DefaultListCellRenderer {
			@Override
			public Component getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel jlRenderer = (JLabel) super
						.getListCellRendererComponent(list, value, index,
								isSelected, cellHasFocus);
				String sText = ((File) value).getName();
				sText = sText.substring(0, sText.lastIndexOf(".json"));
				jlRenderer.setText(sText);
				return jlRenderer;
			}
		}

		lmSourceList = new DefaultListModel<File>();
		jlSourceData = new JLabel("Source Data");
		jpLeft.add(jlSourceData, BorderLayout.NORTH);
		jlSourceList = new JList<File>(lmSourceList);
		jlSourceList.setCellRenderer(new JSONRenderer());
		jlSourceList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				doUpdateText(jlSourceList, jtaSource);
			}
		});
		jlSourceList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// handle double-click events
				if (e.getClickCount() == 2) {
					if (jlSourceList.getSelectedIndex() >= 0) {
						File fSel = lmSourceList.get(jlSourceList
								.getSelectedIndex());
						try {
							UTIL_FileOperations.openFileWindow(fSel
									.getCanonicalPath());
							e.consume();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});
		jtaSource = new JTextArea(10, 20);
		jtaSource.setLineWrap(true);
		jpLeft.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(
				jlSourceList), new JScrollPane(jtaSource)), BorderLayout.CENTER);

		final JLabel jlTargetData = new JLabel("Collection");
		jpRight.add(jlTargetData, BorderLayout.NORTH);
		lmTargetList = new DefaultListModel<File>();
		jlTargetList = new JList<File>(lmTargetList);
		jlTargetList.setCellRenderer(new JSONRenderer());
		jlTargetList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// handle double-click events
				if (e.getClickCount() == 2) {
					if (jlTargetList.getSelectedIndex() >= 0) {
						File fSel = lmTargetList.get(jlTargetList
								.getSelectedIndex());
						try {
							UTIL_FileOperations.openFileWindow(fSel
									.getCanonicalPath());
							e.consume();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		});
		jlTargetList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				doUpdateText(jlTargetList, jtaTarget);
			}
		});
		jtaTarget = new JTextArea(10, 20);
		jtaTarget.setLineWrap(true);
		jpRight.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(
				jlTargetList), new JScrollPane(jtaTarget)), BorderLayout.CENTER);

		bMiddle.add(Box.createVerticalGlue());
		JButton jlRemove = new JButton(" X ");
		bMiddle.add(jlRemove);
		JButton jbMoveLeft = new JButton("-> ");
		bMiddle.add(jbMoveLeft);
		JButton jbMoveRight = new JButton(" <-");
		bMiddle.add(jbMoveRight);
		bMiddle.add(Box.createVerticalGlue());

		jbMoveLeft.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMove(jlSourceList, jlTargetList);
			}
		});
		jbMoveRight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMove(jlTargetList, jlSourceList);
			}
		});

		jlRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doRemove();
			}
		});
		
		lmSourceList.addListDataListener(new ListDataListener() {
			
			@Override
			public void intervalRemoved(ListDataEvent e) {
				updateSourceLabel();
			}
			
			@Override
			public void intervalAdded(ListDataEvent e) {
				updateSourceLabel();
			}
			
			@Override
			public void contentsChanged(ListDataEvent arg0) {
				updateSourceLabel();
			}
			
			private void updateSourceLabel() {
				jlSourceData.setText ("Source Data: " + fSourceDirectory.getName() + " (" + lmSourceList.size() + ")");
			}
		});
		
		lmTargetList.addListDataListener(new ListDataListener() {
			
			@Override
			public void intervalRemoved(ListDataEvent arg0) {
				updateTargetLabel();
			}
			
			@Override
			public void intervalAdded(ListDataEvent arg0) {
				updateTargetLabel();
			}
			
			@Override
			public void contentsChanged(ListDataEvent arg0) {
				updateTargetLabel();
			}
			
			private void updateTargetLabel() {
				jlTargetData.setText("Collection (" + lmTargetList.size() + ")");
			}
		});
	}

	/**
	 * Remove selected element from source list.
	 */
	protected void doRemove() {
		int nSelected = jlSourceList.getSelectedIndex();
		if ((nSelected >= 0) && (nSelected < lmSourceList.getSize())) {
			lfRemovedFiles.add(lmSourceList.remove(nSelected));
			jlSourceList.setSelectedIndex(nSelected);
		}
	}

	/**
	 * Move selected item between lists.
	 * 
	 * @param jlSource
	 * @param jlTarget
	 */
	protected void doMove(JList<File> jlSource, JList<File> jlTarget) {
		int nSelected = jlSource.getSelectedIndex();
		if ((nSelected >= 0) && (nSelected < jlSource.getModel().getSize())) {
			File f = ((DefaultListModel<File>) jlSource.getModel())
					.remove(nSelected);
			((DefaultListModel<File>) jlTarget.getModel()).addElement(f);
			jlSource.setSelectedIndex(nSelected);
		}
	}

	/**
	 * Update the text in jtaOutput according to the current selection in
	 * jlList.
	 * 
	 * @param jlList
	 * @param jtaOutput
	 */
	protected void doUpdateText(JList<File> jlList, JTextArea jtaOutput) {
		int selectedIndex = jlList.getSelectedIndex();
		if ((selectedIndex >= 0)
				&& (selectedIndex < jlList.getModel().getSize())) {
			File fToPresent = ((DefaultListModel<File>) jlList.getModel())
					.get(selectedIndex);
			jtaOutput.setText(getFileContent(fToPresent));
		} else {
			jtaOutput.setText("No file selected.");
		}

	}

	/**
	 * Get the contents of the file given in a format ready to be presented to
	 * the user.
	 * 
	 * @param fToPresent
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 */
	protected String getFileContent(File fToPresent) {
		Gson gson = new Gson();
		try {
			BufferedReader br = new BufferedReader(new FileReader(fToPresent));
			JSON_Document json = gson.fromJson(br, JSON_Document.class);
			br.close();

			StringBuffer sbText = new StringBuffer();
			sbText.append(json.getId()).append(": ").append(json.getTitle())
					.append("\n");
			sbText.append("----------------------------------------------------------\n");
			sbText.append(limitText(json.getQuestion(), 3));
			sbText.append("\n--------\n");
			sbText.append(limitText(json.getAnswersText(), 10));
			return sbText.toString();
		} catch (JsonParseException jpe) {
			return "File could not be retrieved. " + jpe.toString();
		} catch (IOException e) {
			return "File could not be retrieved. " + e.toString();
		}
	}

	/**
	 * Reduce the given text to its first <code>nCountOfLines</code> lines and return the result.
	 * 
	 * @param text
	 *            the source text
	 * @param nCountOfLines
	 *            number of lines to keep
	 */
	private String limitText(String text, int nCountOfLines) {
		if (text.indexOf('\n') >= 0) {
			int nIdx = -1;
			for (int i = 0; i < nCountOfLines; i++) {
				nIdx = text.indexOf('\n', nIdx + 1);
				if (nIdx < 0) {
					return text;
				}
			}
			return (text.substring(0, nIdx + 1) + "...");
		} else {
			return text;
		}
	}
}