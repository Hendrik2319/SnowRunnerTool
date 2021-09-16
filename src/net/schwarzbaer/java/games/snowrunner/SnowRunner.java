package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.ExpandedCompatibleWheel;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	private static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}


	private Data data;
	private final StandardMainWindow mainWindow;
	private final JFileChooser folderChooser;
	private final JList<Truck> truckList;
	private final TruckListCellRenderer truckListCellRenderer;
	private final TruckPanel truckPanel;
	private final JMenu languageMenu;
	private final WheelsTableModel wheelsTableModel;

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
		
		JTable wheelsTable = new JTable(wheelsTableModel = new WheelsTableModel());
		SimplifiedRowSorter wheelsTableRowSorter = new SimplifiedRowSorter(wheelsTableModel);
		wheelsTable.setRowSorter(wheelsTableRowSorter);
		wheelsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		wheelsTableModel.setColumnWidths(wheelsTable);
		JScrollPane wheelsTableScrollPane = new JScrollPane(wheelsTable);
		
		ContextMenu wheelsTableContextMenu = new ContextMenu();
		wheelsTableContextMenu.addTo(wheelsTable);
		wheelsTableContextMenu.add(SnowRunner.createMenuItem("Reset Row Order",true,e->{
			wheelsTableRowSorter.resetSortOrder();
			wheelsTable.repaint();
		}));
		wheelsTableContextMenu.add(SnowRunner.createMenuItem("Show Column Widths", true, e->{
			System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(wheelsTable));
		}));
		
		JPanel wheelsPanel = new JPanel(new BorderLayout());
		wheelsPanel.add(wheelsTableScrollPane);
		
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
		
		wheelsTableModel.setData(data);
		
		Vector<String> langs = new Vector<>(data.languages.keySet());
		langs.sort(null);
		String currentLangID = settings.getString(AppSettings.ValueKey.Language, null);
		
		ButtonGroup bg = new ButtonGroup();
		languageMenu.removeAll();
		for (String langID:langs)
			languageMenu.add(createCheckBoxMenuItem(langID, langID.equals(currentLangID), bg, true, e->setLanguage(langID)));
		
		setLanguage(currentLangID);
	}

	private void setLanguage(String langID) {
		Language language = langID==null ? null : data.languages.get(langID);
		
		if (language!=null)
			settings.putString(AppSettings.ValueKey.Language, langID);
		else
			settings.remove(AppSettings.ValueKey.Language);
		
		wheelsTableModel.setLanguage(language);
		truckPanel.setLanguage(language);
		truckListCellRenderer.setLanguage(language);
		truckList.repaint();
	}

	public static String solveStringID(String strID, Language language) {
		String str = null;
		if (language!=null) str = language.dictionary.get(strID);
		if (str==null) str = strID;
		return str;
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

	private static class WheelsTableModel extends Tables.SimplifiedTableModel<WheelsTableModel.ColumnID>{

		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			ID               ("ID"     , String .class, 300),
			Names            ("Names"  , String .class, 130),
			Sizes            ("Sizes"  , String .class, 300),
			Friction_highway ("Highway", Float  .class,  55), 
			Friction_offroad ("Offroad", Float  .class,  50), 
			Friction_mud     ("Mud"    , Float  .class,  50), 
			OnIce            ("On Ice" , Boolean.class,  50), 
			Trucks           ("Trucks" , String .class, 800),
			;

			private final SimplifiedColumnConfig config;
			ColumnID(String name, Class<?> columnClass, int prefWidth) {
				config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}
		
		private Language language;
		private Vector<RowItem> rows;

		WheelsTableModel() {
			super(ColumnID.values());
			this.language = null;
			rows = new Vector<>();
		}

		void setLanguage(Language language) {
			this.language = language;
			fireTableUpdate();
		}
		
		private static class RowItem {

			final String key;
			final String label;
			final HashSet<Truck> trucks;
			final HashSet<Integer> sizes;
			final HashSet<String> names_StringID;
			final TireValues tireValues;

			public RowItem(String key, String label, ExpandedCompatibleWheel wheel) {
				this.key = key;
				this.label = label;
				trucks = new HashSet<>();
				sizes = new HashSet<>();
				names_StringID = new HashSet<>();
				tireValues = new TireValues(wheel);
			}

			public void add(ExpandedCompatibleWheel wheel, Truck truck) {
				trucks.add(truck);
				sizes.add(wheel.getSize());
				names_StringID.add(wheel.name_StringID);
				TireValues newTireValues = new TireValues(wheel);
				if (!tireValues.equals(newTireValues)) {
					System.err.printf("[WheelsTable] Found a wheel with same source (%s) but different values: %s <-> %s", key, tireValues, newTireValues);
				}
			}
			
			private static class TireValues {

				final Float friction_highway;
				final Float friction_offroad;
				final Float friction_mud;
				final Boolean onIce;

				public TireValues(ExpandedCompatibleWheel wheel) {
					friction_highway = wheel.friction_highway;
					friction_offroad = wheel.friction_offroad;
					friction_mud     = wheel.friction_mud;
					onIce            = wheel.onIce;
				}

				@Override
				public int hashCode() {
					int hashCode = 0;
					if (friction_highway!=null) hashCode ^= friction_highway.hashCode();
					if (friction_offroad!=null) hashCode ^= friction_offroad.hashCode();
					if (friction_mud    !=null) hashCode ^= friction_mud    .hashCode();
					if (onIce           !=null) hashCode ^= onIce           .hashCode();
					return hashCode;
				}

				@Override
				public boolean equals(Object obj) {
					if (!(obj instanceof TireValues))
						return false;
					
					TireValues other = (TireValues) obj;
					if (!equals( this.friction_highway, other.friction_highway )) return false;
					if (!equals( this.friction_offroad, other.friction_offroad )) return false;
					if (!equals( this.friction_mud    , other.friction_mud     )) return false;
					if (!equals( this.onIce           , other.onIce            )) return false;
					
					return true;
				}

				private boolean equals(Boolean v1, Boolean v2) {
					if (v1==null && v2==null) return true;
					if (v1==null || v2==null) return false;
					return v1.booleanValue() == v2.booleanValue();
				}

				private boolean equals(Float v1, Float v2) {
					if (v1==null && v2==null) return true;
					if (v1==null || v2==null) return false;
					return v1.floatValue() == v2.floatValue();
				}

				@Override public String toString() {
					return String.format("(H:%1.2f,O:%1.2f,M:%1.2f%s)\"", friction_highway, friction_offroad, friction_mud, onIce!=null && onIce ? ",Ice" : "");
				}
				
			}
		}
		
		void setData(Data data) {
			HashMap<String,RowItem> rows = new HashMap<>();
			for (Truck truck:data.trucks.values()) {
				for (ExpandedCompatibleWheel wheel:truck.expandedCompatibleWheels) {
					String key   = String.format("%s|%s[%d]", wheel.dlc==null ? "----" : wheel.dlc, wheel.definingXML, wheel.indexInDef);
					String label = wheel.dlc==null
							? String.format(     "%s [%d]",            wheel.definingXML, wheel.indexInDef)
							: String.format("%s | %s [%d]", wheel.dlc, wheel.definingXML, wheel.indexInDef);
					RowItem rowItem = rows.get(key);
					if (rowItem==null)
						rows.put(key, rowItem = new RowItem(key,label,wheel));
					rowItem.add(wheel,truck);
				}
			}
			this.rows.clear();
			this.rows.addAll(rows.values());
			this.rows.sort(Comparator.<RowItem,String>comparing(rowItem->rowItem.key));
			fireTableUpdate();
		}

		@Override public int getRowCount() {
			return rows.size();
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			if (rowIndex<0 || rowIndex>=rows.size()) return null;
			RowItem row = rows.get(rowIndex);
			switch (columnID) {
			case ID    : return row.label;
			case Sizes : return getSizeList ( row.sizes );
			case Trucks: return getTruckList( row.trucks );
			case Names : return getNameList ( row.names_StringID );
			case Friction_highway: return row.tireValues.friction_highway;
			case Friction_offroad: return row.tireValues.friction_offroad;
			case Friction_mud    : return row.tireValues.friction_mud;
			case OnIce           : return row.tireValues.onIce;
			}
			return null;
		}

		private String getTruckList(HashSet<Truck> trucks) {
			Iterable<String> it = ()->trucks
					.stream()
					.map(t->TruckListCellRenderer.getTruckLabel(t,language))
					.sorted()
					.iterator();
			return trucks.isEmpty() ? "" :  String.join(", ", it);
		}

		private String getSizeList(HashSet<Integer> sizes) {
			Iterable<String> it = ()->sizes
					.stream()
					.sorted()
					.map(size->String.format("%d\"", size))
					.iterator();
			return sizes.isEmpty() ? "" :  String.join(", ", it);
		}

		private String getNameList(HashSet<String> names_StringID) {
			Iterable<String> it = ()->names_StringID
					.stream()
					.sorted()
					.map(strID->solveStringID(strID, language))
					.iterator();
			return names_StringID.isEmpty() ? "" :  String.join(", ", it);
		}

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
			rendererComp.configureAsListCellRendererComponent(list, null, getTruckLabel(value, language), index, isSelected, cellHasFocus, null, ()->getForeground(value));
			return rendererComp;
		}

		private Color getForeground(Truck truck) {
			if (truck!=null && truck.dlcName!=null)
				return COLOR_FG_DLCTRUCK;
			return null;
		}

		static String getTruckLabel(Truck truck, Language language) {
			if (truck==null)
				return "<null>";
			
			String truckName = truck.xmlName;
			
			if (language!=null && truck.name_StringID!=null) {
				truckName = language.dictionary.get(truck.name_StringID);
				if (truckName==null)
					truckName = "<"+truck.xmlName+">";
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
