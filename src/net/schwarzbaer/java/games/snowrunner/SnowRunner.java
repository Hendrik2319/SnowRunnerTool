package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.Tables;
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
	private final JList<Truck> truckList;
	private final TruckListCellRenderer truckListCellRenderer;
	private final TruckPanel truckPanel;
	private JMenu languageMenu;

	SnowRunner() {
		data = null;
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		
		folderChooser = new JFileChooser("./");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		folderChooser.setMultiSelectionEnabled(false);
		
		truckPanel = new TruckPanel();
		truckPanel.setBorder(BorderFactory.createTitledBorder("Selected Truck"));
		
		JScrollPane truckListScrollPane = new JScrollPane(truckList = new JList<>());
		truckListScrollPane.setBorder(BorderFactory.createTitledBorder("All Trucks in Game"));
		truckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		truckList.setCellRenderer(truckListCellRenderer = new TruckListCellRenderer());
		truckList.addListSelectionListener(e->truckPanel.setTruck(truckList.getSelectedValue()));
		
		JSplitPane trucksPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		trucksPanel.setResizeWeight(0);
		trucksPanel.setLeftComponent(truckListScrollPane);
		trucksPanel.setRightComponent(truckPanel);
		
		JPanel wheelsPanel = new JPanel();
		
		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.addTab("Trucks", trucksPanel);
		contentPane.addTab("Wheels", wheelsPanel);
		
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
		
		languageMenu = menuBar.add(new JMenu("Language"));
		
		mainWindow.setIconImagesFromResource("/AppIcons/AppIcon","016.png","024.png","032.png","040.png","048.png","056.png","064.png","128.png","256.png");
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
		
		Vector<Truck> items = new Vector<>(data.trucks.values());
		items.sort(Comparator.<Truck,String>comparing(Truck->Truck.xmlName));
		truckList.setListData(items);
		
		Vector<String> langs = new Vector<>(data.languages.keySet());
		langs.sort(null);
		String langID = settings.getString(AppSettings.ValueKey.Language, null);
		
		ButtonGroup bg = new ButtonGroup();
		languageMenu.removeAll();
		for (String langID_:langs)
			languageMenu.add(createCheckBoxMenuItem(langID_, langID_.equals(langID), bg, true, e->selectLanguage(langID_)));
		
		if (langID!=null)
			selectLanguage(langID);
	}

	private void selectLanguage(String langID_) {
		Language language = data.languages.get(langID_);
		if (language!=null) {
			settings.putString(AppSettings.ValueKey.Language, langID_);
			setLanguage(language);
		} else {
			settings.remove(AppSettings.ValueKey.Language);
			setLanguage(null);
		}
	}

	private void setLanguage(Language language) {
		truckPanel.setLanguage(language);
		truckListCellRenderer.setLanguage(language);
		truckList.repaint();
	}

	static JCheckBoxMenuItem createCheckBoxMenuItem(String title, boolean isSelected, ButtonGroup bg, boolean isEnabled, ActionListener al) {
		JCheckBoxMenuItem comp = new JCheckBoxMenuItem(title,isSelected);
		comp.setEnabled(isEnabled);
		if (al!=null) comp.addActionListener(al);
		if (bg!=null) bg.add(comp);
		return comp;
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

	private static class TruckListCellRenderer implements ListCellRenderer<Truck> {
		
		private static final Color COLOR_FG_DLCTRUCK = new Color(0x0070FF);
		private final Tables.LabelRendererComponent rendererComp;
		private Language language;

		TruckListCellRenderer() {
			rendererComp = new Tables.LabelRendererComponent();
			language = null;
		}
		
		void setLanguage(Language language) {
			this.language = language;
		}

		@Override
		public Component getListCellRendererComponent( JList<? extends Truck> list, Truck value, int index, boolean isSelected, boolean cellHasFocus) {
			rendererComp.configureAsListCellRendererComponent(list, null, getLabel(value), index, isSelected, cellHasFocus, null, ()->getForeground(value));
			return rendererComp;
		}

		private Color getForeground(Truck truck) {
			if (truck!=null && truck.dlcName!=null)
				return COLOR_FG_DLCTRUCK;
			return null;
		}

		private String getLabel(Truck truck) {
			if (truck==null)
				return "<null>";
			
			String truckName = truck.xmlName;
			
			if (language!=null && truck.name_StringID!=null) {
				truckName = language.dictionary.get(truck.name_StringID);
				if (truckName==null)
					truckName = truck.xmlName;
			}
			
			if (truck.dlcName!=null)
				truckName = String.format("%s [%s]", truckName, truck.dlcName);
			
			return truckName;
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
