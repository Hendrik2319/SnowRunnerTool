package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	private static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}


	private final StandardMainWindow mainWindow;
	private final JFileChooser folderChooser;
	private Data data;
	private final JList<TruckListModel.Item> truckList;
	private final JComboBox<String> langCmbBx;
	private final TruckPanel truckPanel;
	private TruckListModel truckListModel;

	SnowRunner() {
		data = null;
		truckListModel = null;
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		
		folderChooser = new JFileChooser("./");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		folderChooser.setMultiSelectionEnabled(false);
		
		truckPanel = new TruckPanel();
		
		truckList = new JList<>();
		JScrollPane truckListScrollPane = new JScrollPane(truckList);
		truckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		truckList.addListSelectionListener(e->{
			TruckListModel.Item item = truckList.getSelectedValue();
			truckPanel.setTruck(item==null ? null : item.truck);
		});
		
		langCmbBx = new JComboBox<>();
		langCmbBx.addActionListener(e->{
			if (data==null) return;
			Object obj = langCmbBx.getSelectedItem();
			if (obj!=null) {
				String langID = obj.toString();
				Language language = data.languages.get(langID);
				if (language!=null) {
					settings.putString(AppSettings.ValueKey.Language, langID);
					setLanguage(language);
					return;
				}
			}
			settings.remove(AppSettings.ValueKey.Language);
			setLanguage(null);
		});
		
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0;
		contentPane.add(langCmbBx,c);
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 2;
		c.gridx = 1;
		c.gridy = 0;
		contentPane.add(truckPanel,c);
		
		c.weightx = 0;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 1;
		contentPane.add(truckListScrollPane,c);
		
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = menuBar.add(new JMenu("File"));
		fileMenu.add(createMenuItem("Reload \"initial.pak\"", true, e->{
			boolean changed = reloadInitialPAK();
			if (changed) updateAfterDataChange();
		}));
		fileMenu.add(createMenuItem("Reset application settings", true, e->{
			for (AppSettings.ValueKey key:AppSettings.ValueKey.values())
				settings.remove(key);
		}));
		
		mainWindow.startGUI(contentPane, menuBar);
		
		if (settings.isSet(AppSettings.ValueGroup.WindowPos )) mainWindow.setLocation(settings.getWindowPos ());
		if (settings.isSet(AppSettings.ValueGroup.WindowSize)) mainWindow.setSize    (settings.getWindowSize());
		
		mainWindow.addComponentListener(new ComponentListener() {
			@Override public void componentShown  (ComponentEvent e) {}
			@Override public void componentHidden (ComponentEvent e) {}
			@Override public void componentResized(ComponentEvent e) { settings.setWindowSize( mainWindow.getSize() ); }
			@Override public void componentMoved  (ComponentEvent e) { settings.setWindowPos ( mainWindow.getLocation() ); }
		});
	}
	
	private void initialize() {
		boolean changed = reloadInitialPAK();
		if (changed) updateAfterDataChange();
	}

	private boolean reloadInitialPAK() {
		File initialPAK = getInitialPAK();
		if (initialPAK!=null) {
			Data newData = Data.readInitialPAK(initialPAK);
			if (newData!=null) {
				data = newData;
				return true;
			}
		}
		return false;
	}


	private File getInitialPAK() {
		File initialPAK = settings.getFile(AppSettings.ValueKey.InitialPAK, null);
		if (initialPAK==null || !initialPAK.isFile()) {
			DataSelectDialog dlg = new DataSelectDialog(mainWindow);
			initialPAK = dlg.showDialog();
			if (initialPAK == null || !initialPAK.isFile())
				return null;
			settings.putFile(AppSettings.ValueKey.InitialPAK, initialPAK);
		}
		return initialPAK;
	}

	private void updateAfterDataChange() {
		truckListModel = new TruckListModel(data);
		truckList.setModel(truckListModel);
		HashMap<String, Language> languages = data.languages;
		Vector<String> langs = new Vector<>(languages.keySet());
		langs.sort(null);
		langCmbBx.setModel(new DefaultComboBoxModel<String>(langs));
		String langID = settings.getString(AppSettings.ValueKey.Language, null);
		if (langID!=null)
			langCmbBx.setSelectedItem(langID);
	}

	private void setLanguage(Language language) {
		if (truckListModel!=null) {
			truckListModel.setLanguage(language);
			truckList.repaint();
		}
		truckPanel.setLanguage(language);
	}

	private static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	private static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		JButton comp = new JButton(title);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	private static <AC> JButton createButton(String title, boolean isEnabled, Disabler<AC> disabler, AC ac, ActionListener al) {
		JButton comp = createButton(title, isEnabled, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}

	private static JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, ActionListener al) {
		JRadioButton comp = new JRadioButton(title);
		if (bg!=null) bg.add(comp);
		comp.setEnabled(isEnabled);
		comp.setSelected(isSelected);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	private static <AC> JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, Disabler<AC> disabler, AC ac, ActionListener al) {
		JRadioButton comp = createRadioButton(title, bg, isEnabled, isSelected, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}
	
	private static <AC> JLabel createLabel(String text, Disabler<AC> disabler, AC ac) {
		JLabel comp = new JLabel(text);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}

	private static class DataSelectDialog extends JDialog {
		private static final long serialVersionUID = 5535879419617093256L;
		
		private final Disabler<ActionCommands> disabler;

		private Boolean defineFullPath;
		private Boolean selectSteamLibrary;
		private File result;

		private final JFileChooser fileChooser;


		private enum ActionCommands { GameFolderLabel, GameFolderRB, OkBtn }
		
		DataSelectDialog(Window owner) {
			super(owner, "", ModalityType.APPLICATION_MODAL);
			defineFullPath = null;
			selectSteamLibrary = null;
			result = null;
			
			fileChooser = new JFileChooser("./");
			fileChooser.setMultiSelectionEnabled(false);
			
			disabler = new Disabler<>();
			disabler.setCareFor(ActionCommands.values());
			
			JPanel centerPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.gridwidth = GridBagConstraints.REMAINDER;
			centerPanel.add(new JLabel("How do you want to define location of \"initial.pak\"?"),c);
			
			ButtonGroup bg0 = new ButtonGroup();
			centerPanel.add(createRadioButton("full path to \"initial.pak\"",bg0,true,false,e->{ defineFullPath = true ; updateGuiAccess(); }),c);
			centerPanel.add(createRadioButton("via game installation folder",bg0,true,false,e->{ defineFullPath = false; updateGuiAccess(); }),c);

			c.gridwidth = 1;
			String spacer = "          ";
			centerPanel.add(new JLabel(spacer),c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			centerPanel.add(createLabel("How is your game installed?", disabler, ActionCommands.GameFolderLabel),c);

			ButtonGroup bg1 = new ButtonGroup();
			c.gridwidth = 1;
			centerPanel.add(new JLabel(spacer),c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			centerPanel.add(createRadioButton("direct installation (e.g. in \"Program Files\") --> select installation folder",bg1,true,false,
					disabler, ActionCommands.GameFolderRB,
					e->{ selectSteamLibrary = false; updateGuiAccess(); }),c);
			
			c.gridwidth = 1;
			centerPanel.add(new JLabel(spacer),c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			centerPanel.add(createRadioButton("via Steam platform --> select Steam library where game is installed into",bg1,true,false,
					disabler, ActionCommands.GameFolderRB,
					e->{ selectSteamLibrary = true; updateGuiAccess(); }),c);
			
			c.gridwidth = 1;
			centerPanel.add(new JLabel(spacer),c);
			c.gridwidth = GridBagConstraints.REMAINDER;
			centerPanel.add(createRadioButton("via another gaming platform --> select installation folder",bg1,true,false,
					disabler, ActionCommands.GameFolderRB,
					e->{ selectSteamLibrary = false; updateGuiAccess(); }),c);
			
			JPanel buttonPanel = new JPanel(new GridBagLayout());
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weightx = 1;
			buttonPanel.add(new JLabel(),c);
			c.weightx = 0;
			buttonPanel.add(createButton("Ok", false, disabler, ActionCommands.OkBtn, e->{
				fileChooser.resetChoosableFileFilters();
				
				if (defineFullPath) {
					fileChooser.setDialogTitle("Select initial.pak");
					fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fileChooser.addChoosableFileFilter(new FileFilter() {
						@Override public String getDescription() { return "initial.pak only"; }
						@Override public boolean accept(File file) { return !file.isFile() || file.getName().equalsIgnoreCase("initial.pak"); }
					});
					
					if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
						return;
					
					File file = fileChooser.getSelectedFile();
					if (!file.isFile()) {
						String msg = "Selected file isn't a file or doesn't exist.";
						JOptionPane.showMessageDialog(this, msg, "Wrong file", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					result = file;
					
				} else if (selectSteamLibrary) {
					fileChooser.setDialogTitle("Select Steam Library");
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					
					if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
						return;
					
					File folder = fileChooser.getSelectedFile();
					if (!folder.isDirectory()) {
						String msg = "Selected folder isn't a folder or doesn't exist.";
						JOptionPane.showMessageDialog(this, msg, "Wrong folder", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					File file = new File(folder,"steamapps/common/SnowRunner/preload/paks/client/initial.pak");
					if (!file.isFile()) {
						String msg = String.format("Can't find \"initial.pak\" at expected location:%n\"%s\"", file.getAbsolutePath());
						JOptionPane.showMessageDialog(this, msg, "Can't find file", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					result = file;
					
				} else {
					fileChooser.setDialogTitle("Select game folder");
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					
					if (fileChooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
						return;
					
					File folder = fileChooser.getSelectedFile();
					if (!folder.isDirectory()) {
						String msg = "Selected folder isn't a folder or doesn't exist.";
						JOptionPane.showMessageDialog(this, msg, "Wrong folder", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					File file = new File(folder,"preload/paks/client/initial.pak");
					if (!file.isFile()) {
						String msg = String.format("Can't find \"initial.pak\" at expected location:%n\"%s\"", file.getAbsolutePath());
						JOptionPane.showMessageDialog(this, msg, "Can't find file", JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					result = file;
				}
				
				setVisible(false);
			}),c);
			buttonPanel.add(createButton("Cancel", true, e->setVisible(false)),c);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(centerPanel,BorderLayout.CENTER);
			contentPane.add(buttonPanel,BorderLayout.SOUTH);
			
			setContentPane(contentPane);
			pack();
			
			Point oLoc = owner.getLocation();
			Dimension oSize = owner.getSize();
			Dimension mySize = getSize();
			setLocation(
					oLoc.x + (oSize.width -mySize.width )/2,
					oLoc.y + (oSize.height-mySize.height)/2
			);
			
			updateGuiAccess();
		}

		private void updateGuiAccess() {
			disabler.setEnable(ac->{
				switch (ac) {
				case GameFolderLabel:
				case GameFolderRB:
					return defineFullPath!=null && !defineFullPath; 
					
				case OkBtn:
					return defineFullPath!=null && (defineFullPath || selectSteamLibrary!=null); 
				}
				return null;
			});
		}

		public File showDialog() {
			setVisible(true);
			return result;
		}
		
		
		
	}
	
	private class TruckPanel extends JScrollPane {
		private static final long serialVersionUID = -5138746858742450458L;
		
		private final JTextArea outTextArea;
		private Language language;
		private Truck truck;

		TruckPanel() {
			outTextArea = new JTextArea();
			outTextArea.setEditable(false);
			outTextArea.setWrapStyleWord(true);
			outTextArea.setLineWrap(true);
			setViewportView(outTextArea);
			language = null;
			truck = null;
		}

		void setLanguage(Language language) {
			this.language = language;
			updateOutput();
		}

		void setTruck(Truck truck) {
			this.truck = truck;
			updateOutput();
		}

		private void updateOutput() {
			if (truck==null) {
				outTextArea.setText("<NULL>");
				return;
			}
			
			ValueListOutput out = new ValueListOutput();
			out.add(0, "Country", truck.country);
			out.add(0, "Price"  , truck.price);
			out.add(0, "Type"   , truck.type);
			out.add(0, "Unlock By Exploration", truck.unlockByExploration);
			out.add(0, "Unlock By Rank"       , truck.unlockByRank);
			out.add(0, "XML file"             , truck.xmlName);
			
			String name = null;
			String description = null;
			if (language!=null) {
				name        = language.dictionary.get(truck.name_StringID);
				description = language.dictionary.get(truck.description_StringID);
			}
			if (name==null)
				out.add(0, "Name", "<%s>", truck.name_StringID);
			else {
				out.add(0, "Name", "%s"  , name);
				out.add(0, null  , "<%s>", truck.name_StringID);
			}
			out.add(0, "Description", "<%s>", truck.description_StringID);
			if (description != null)
				out.add(0, null, "\"%s\"", description);
			
			if (!truck.compatibleWheels.isEmpty()) {
				out.add(0, "Compatible Wheels", truck.compatibleWheels.size());
				for (int i=0; i<truck.compatibleWheels.size(); i++) {
					Data.Truck.CompatibleWheel cw = truck.compatibleWheels.get(i);
					out.add(1, String.format("[%d]", i+1), "(%s) %s", cw.scale, cw.type);
					cw.printTireList(out,2);
				}
			}
			
			outTextArea.setText(out.generateOutput());
		}
	}
	
	private static class TruckListModel implements ListModel<TruckListModel.Item> {
		
		private final Vector<ListDataListener> listDataListeners;
		private final Vector<Item> items;
		private Language language;
		
		public TruckListModel(Data data) {
			listDataListeners = new Vector<>();
			items = new Vector<>();
			language = null;
			
			data.defaultTrucks.forEach((name,truck)->{
				items.add(new Item(null, name,truck));
			});
			
			data.dlcList.forEach(dlc->{
				dlc.trucks.forEach((name,truck)->{
					items.add(new Item(dlc.name, name,truck));
				});
			});
			
			items.sort(Comparator.<Item,String>comparing(item->item.name));
		}

		public void setLanguage(Language language) {
			this.language = language;
		}

		@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
		@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }

		@Override public int getSize() { return items.size(); }
		@Override public Item getElementAt(int index) { return items.elementAt(index); }

		private class Item {

			private final String dlcName;
			private final String name;
			private final Truck truck;

			public Item(String dlcName, String name, Truck truck) {
				this.dlcName = dlcName;
				this.name = name;
				this.truck = truck;
			}

			@Override public String toString() {
				String truckName = name;
				if (language!=null && truck!=null && truck.name_StringID!=null) {
					truckName = language.dictionary.get(truck.name_StringID);
					if (truckName==null)
						truckName = name;
				}
				if (dlcName==null) return truckName;
				return String.format("[%s] %s", dlcName, truckName);
			}
		}
		
	}
	
	private static class AppSettings extends Settings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		enum ValueKey {
			WindowX, WindowY, WindowWidth, WindowHeight, SteamLibraryFolder, Language, InitialPAK,
		}

		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			WindowPos (ValueKey.WindowX, ValueKey.WindowY),
			WindowSize(ValueKey.WindowWidth, ValueKey.WindowHeight),
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		public AppSettings() { super(SnowRunner.class); }
		public Point     getWindowPos (              ) { return getPoint(ValueKey.WindowX,ValueKey.WindowY); }
		public void      setWindowPos (Point location) {        putPoint(ValueKey.WindowX,ValueKey.WindowY,location); }
		public Dimension getWindowSize(              ) { return getDimension(ValueKey.WindowWidth,ValueKey.WindowHeight); }
		public void      setWindowSize(Dimension size) {        putDimension(ValueKey.WindowWidth,ValueKey.WindowHeight,size); }
	}
}
