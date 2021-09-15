package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListDataListener;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
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
		
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(langCmbBx,BorderLayout.NORTH);
		leftPanel.add(truckListScrollPane,BorderLayout.CENTER);
		
		
		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.setResizeWeight(0);
		contentPane.setLeftComponent(leftPanel);
		contentPane.setRightComponent(truckPanel);
		
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

	static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		JButton comp = new JButton(title);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	static <AC> JButton createButton(String title, boolean isEnabled, Disabler<AC> disabler, AC ac, ActionListener al) {
		JButton comp = createButton(title, isEnabled, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}

	static JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, ActionListener al) {
		JRadioButton comp = new JRadioButton(title);
		if (bg!=null) bg.add(comp);
		comp.setEnabled(isEnabled);
		comp.setSelected(isSelected);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	static <AC> JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, Disabler<AC> disabler, AC ac, ActionListener al) {
		JRadioButton comp = createRadioButton(title, bg, isEnabled, isSelected, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}
	
	static <AC> JLabel createLabel(String text, Disabler<AC> disabler, AC ac) {
		JLabel comp = new JLabel(text);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
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
