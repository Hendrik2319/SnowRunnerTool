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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	private static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}


	private Data data;
	private HashMap<String,String> truckToDLCAssignments;
	private final StandardMainWindow mainWindow;
	private final JMenu languageMenu;
	private final Controllers controllers;
	private final RawDataPanel rawDataPanel;
	private final TrucksPanel trucksPanel;
	
	SnowRunner() {
		data = null;
		truckToDLCAssignments = null;
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		controllers = new Controllers();
		
		rawDataPanel = new RawDataPanel(mainWindow, controllers);
		trucksPanel = new TrucksPanel(mainWindow,controllers);
		
		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.addTab("Trucks"      , trucksPanel);
		contentPane.addTab("Wheels"      , createSimplifiedTablePanel(new WheelsTableModel     (controllers)));
		contentPane.addTab("DLCs"        , createSimplifiedTablePanel(new DLCTableModel        (controllers)));
		contentPane.addTab("Trailers"    , createSimplifiedTablePanel(new TrailersTableModel   (controllers)));
		contentPane.addTab("Truck Addons", createSimplifiedTablePanel(new TruckAddonsTableModel(controllers)));
		contentPane.addTab("Raw Data", rawDataPanel);
		
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
		fileMenu.add(createMenuItem("Test XMLTemplateStructure", true, e->{
			boolean changed = testXMLTemplateStructure();
			if (changed) updateAfterDataChange();
		}));
		
		languageMenu = menuBar.add(new JMenu("Language"));
		
		mainWindow.setIconImagesFromResource("/images/AppIcons/AppIcon","016.png","024.png","032.png","040.png","048.png","056.png","064.png","128.png","256.png");
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

	static class ValueContainer<ValueType> {
		ValueType value = null;
	}
	
	static class Controllers {
		
		final LanguageListenerController languageListeners;
		final TruckToDLCAssignmentListenerController truckToDLCAssignmentListeners;
		final DataReceiverController dataReceivers;
		
		Controllers() {
			languageListeners = new LanguageListenerController();
			truckToDLCAssignmentListeners = new TruckToDLCAssignmentListenerController();
			dataReceivers = new DataReceiverController();
		}

		static class AbstractController<Listener> {
			protected final Vector<Listener> listeners = new Vector<>();
			
			void remove(Listener l) { listeners.remove(l); }
			void    add(Listener l) { listeners.   add(l); }
		}

		static class TruckToDLCAssignmentListenerController extends AbstractController<TruckToDLCAssignmentListener> implements TruckToDLCAssignmentListener {
			
			@Override public void setTruckToDLCAssignments(HashMap<String, String> assignments) {
				for (TruckToDLCAssignmentListener l:listeners)
					l.setTruckToDLCAssignments(assignments);
			}
			@Override public void updateAfterAssignmentsChange() {
				for (TruckToDLCAssignmentListener l:listeners)
					l.updateAfterAssignmentsChange();
			}
		}

		static class DataReceiverController extends AbstractController<DataReceiver> implements DataReceiver {
			@Override public void setData(Data data) {
				for (DataReceiver r:listeners)
					r.setData(data);
			}
		}

		static class LanguageListenerController extends AbstractController<LanguageListener> implements LanguageListener {
			@Override public void setLanguage(Language language) {
				for (LanguageListener l:listeners)
					l.setLanguage(language);
			}
		}
	}
	
	interface TruckToDLCAssignmentListener {
		void setTruckToDLCAssignments(HashMap<String, String> assignments);
		void updateAfterAssignmentsChange();
	}
	
	private void initialize() {
		truckToDLCAssignments = TruckAssignToDLCDialog.loadStoredData();
		controllers.truckToDLCAssignmentListeners.setTruckToDLCAssignments(truckToDLCAssignments);
		
		boolean changed = reloadInitialPAK();
		if (changed) updateAfterDataChange();
	}

	private boolean reloadInitialPAK() {
		return testXMLTemplateStructure();
//		File initialPAK = getInitialPAK();
//		if (initialPAK!=null) {
//			Data newData = Data.readInitialPAK(initialPAK);
//			if (newData!=null) {
//				data = newData;
//				return true;
//			}
//		}
//		return false;
	}

	private boolean testXMLTemplateStructure() {
		File initialPAK = getInitialPAK();
		if (initialPAK!=null) {
			Data newData = ProgressDialog.runWithProgressDialogRV(mainWindow, String.format("Read \"%s\"", initialPAK.getAbsolutePath()), 600, pd->{
				Data localData = testXMLTemplateStructure(pd,initialPAK);
				if (Thread.currentThread().isInterrupted()) return null;
				return localData;
			});
			//Data newData = testXMLTemplateStructure(initialPAK);
			if (newData!=null) {
				data = newData;
				return true;
			}
		}
		return false;
	}
	
	private Data testXMLTemplateStructure(ProgressDialog pd, File initialPAK) {
		setTask(pd, "Read XMLTemplateStructure");
		System.out.printf("XMLTemplateStructure.");
		XMLTemplateStructure structure = XMLTemplateStructure.readPAK(initialPAK,mainWindow);
		if (structure==null) return null;
		if (Thread.currentThread().isInterrupted()) return null;
		setTask(pd, "Parse Data from XMLTemplateStructure");
		System.out.printf("Parse Data from XMLTemplateStructure ...%n");
		Data data = new Data(structure);
		System.out.printf("... done");
		if (Thread.currentThread().isInterrupted()) return null;
		return data;
	}


	static void setTask(ProgressDialog pd, String taskTitle) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setIndeterminate(true);
		});
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
	
	interface DataReceiver {
		void setData(Data data);
	}
	
	private void updateAfterDataChange() {
		controllers.dataReceivers.setData(data);
		
		Vector<String> langs = new Vector<>(data.languages.keySet());
		langs.sort(null);
		String currentLangID = settings.getString(AppSettings.ValueKey.Language, null);
		
		ButtonGroup bg = new ButtonGroup();
		languageMenu.removeAll();
		for (String langID:langs)
			languageMenu.add(createCheckBoxMenuItem(langID, langID.equals(currentLangID), bg, true, e->setLanguage(langID)));
		
		setLanguage(currentLangID);
	}
	
	interface LanguageListener {
		void setLanguage(Language language);
	}
	
	private void setLanguage(String langID) {
		Language language = langID==null ? null : data.languages.get(langID);
		
		if (language!=null)
			settings.putString(AppSettings.ValueKey.Language, langID);
		else
			settings.remove(AppSettings.ValueKey.Language);
		
		controllers.languageListeners.setLanguage(language);
	}

	static String solveStringID(String strID, Language language) {
		if (strID==null) return null;
		return solveStringID(strID, language, "<"+strID+">");
	}
	static String solveStringID(String strID, Language language, String defaultStr) {
		if (strID==null) strID = defaultStr;
		String str = null;
		if (language!=null) str = language.dictionary.get(strID);
		if (str==null) str = defaultStr;
		return str;
	}
	
	static String getReducedString(String str, int maxLength) {
		if (str==null) return null;
		if (str.length() > maxLength-4)
			return str.substring(0,maxLength-4)+" ...";
		return str;
	}
	
	static String selectNonNull(String... strings) {
		for (String str : strings)
			if (str!=null)
				return str;
		return null;
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

	static JComponent createSimplifiedTablePanel(SimplifiedTableModel<?> tableModel) {
		JTable table = new JTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scrollPane = new JScrollPane(table);
		
		table.setModel(tableModel);
		tableModel.setTable(table);
		tableModel.setColumnWidths(table);
		
		SimplifiedRowSorter rowSorter = new SimplifiedRowSorter(tableModel);
		table.setRowSorter(rowSorter);
		
		ContextMenu contextMenu = new ContextMenu();
		contextMenu.addTo(table);
		contextMenu.add(createMenuItem("Reset Row Order",true,e->{
			rowSorter.resetSortOrder();
			table.repaint();
		}));
		contextMenu.add(createMenuItem("Show Column Widths", true, e->{
			System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
		}));
		
		if (tableModel instanceof RowTextTableModel) {
			RowTextTableModel rowTextTableModel = (RowTextTableModel) tableModel;
			
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			//textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Description"));
			textAreaScrollPane.setPreferredSize(new Dimension(400,100));
			
			Runnable textAreaUpdateMethod = ()->{
				Integer selectedRow = null;
				int rowV = table.getSelectedRow();
				if (rowV>=0) {
					int rowM = table.convertRowIndexToModel(rowV);
					selectedRow = rowM>=0 ? rowM : null;
				}
				if (selectedRow != null)
					textArea.setText(rowTextTableModel.getTextForRow(selectedRow));
				else
					textArea.setText("");
			};
			rowTextTableModel.setTextAreaUpdateMethod(textAreaUpdateMethod);
			
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e->{
				textAreaUpdateMethod.run();
			});
			
			JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
			panel.setResizeWeight(1);
			panel.setTopComponent(scrollPane);
			panel.setBottomComponent(textAreaScrollPane);
			return panel;
			
		} else {
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(scrollPane);
			return panel;
		}
		
	}

	static String getTruckLabel(Truck truck, Language language) {
		return getTruckLabel(truck, language, true);
	}
	static String getTruckLabel(Truck truck, Language language, boolean addInternalDLC ) {
		if (truck==null)
			return "<null>";
		
		String truckName = SnowRunner.solveStringID(truck.name_StringID, language, "<"+truck.id+">");
		
		if (addInternalDLC && truck.dlcName!=null)
			truckName = String.format("%s [%s]", truckName, truck.dlcName);
		
		return truckName;
	}

	static String toString(Vector<Truck> list, Language language) {
		return String.join(", ", (Iterable<String>)()->list.stream().map(truck->SnowRunner.solveStringID(truck.name_StringID, language, "<"+truck.id+">")).sorted().iterator());
	}

	static String joinRequiredAddonsToString(String[][] requiredAddons, String indent) {
		Iterable<String> it = ()->Arrays.stream(requiredAddons).map(list->String.join("  OR  ", list)).iterator();
		return indent+"  "+String.join(String.format("%n%1$sAND%n%1$s  ", indent), it);
	}

	static String joinRequiredAddonsToString_OneLine(String[][] strs) {
		if (strs==null) return "<null>";
		if (strs.length==0) return "-----";
		
		Iterable<String> it = ()->Arrays.stream(strs).map(list->{
			String str = String.join(" OR ", Arrays.asList(list));
			if (list.length==1) return str;
			return String.format("(%s)", str);
		}).iterator();
		
		String str = String.join(" AND ", it);
		if (strs.length==1) return str;
		return String.format("(%s)", str);
	}

	interface RowTextTableModel {
		String getTextForRow(int rowIndex);
		void setTextAreaUpdateMethod(Runnable textAreaUpdateMethod);
	}

	private static class WheelsTableModel extends Tables.SimplifiedTableModel<WheelsTableModel.ColumnID> implements LanguageListener, DataReceiver{
	
		private Language language;
		private final Vector<RowItem> rows;
	
		WheelsTableModel(Controllers controllers) {
			super(ColumnID.values());
			this.language = null;
			rows = new Vector<>();
			controllers.languageListeners.add(this);
			controllers.dataReceivers.add(this);
		}
	
		@Override public void setLanguage(Language language) {
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
	
			public RowItem(String key, String label, TireValues tireValues) {
				this.key = key;
				this.label = label;
				this.tireValues = tireValues;
				trucks = new HashSet<>();
				sizes = new HashSet<>();
				names_StringID = new HashSet<>();
			}
	
			public void add(Float scale, TruckTire tire, Truck truck) {
				trucks.add(truck);
				sizes.add(CompatibleWheel.computeSize_inch(scale));
				names_StringID.add(tire.name_StringID);
				TireValues newTireValues = new TireValues(tire);
				if (!tireValues.equals(newTireValues)) {
					System.err.printf("[WheelsTable] Found a wheel with same source (%s) but different values: %s <-> %s", key, tireValues, newTireValues);
				}
			}
			
			private static class TireValues {
	
				final Float frictionHighway;
				final Float frictionOffroad;
				final Float frictionMud;
				final Boolean onIce;
	
				public TireValues(TruckTire wheel) {
					frictionHighway = wheel.frictionHighway;
					frictionOffroad = wheel.frictionOffroad;
					frictionMud     = wheel.frictionMud;
					onIce           = wheel.onIce;
				}
	
				@Override
				public int hashCode() {
					int hashCode = 0;
					if (frictionHighway!=null) hashCode ^= frictionHighway.hashCode();
					if (frictionOffroad!=null) hashCode ^= frictionOffroad.hashCode();
					if (frictionMud    !=null) hashCode ^= frictionMud    .hashCode();
					if (onIce          !=null) hashCode ^= onIce          .hashCode();
					return hashCode;
				}
	
				@Override
				public boolean equals(Object obj) {
					if (!(obj instanceof TireValues))
						return false;
					
					TireValues other = (TireValues) obj;
					if (!equals( this.frictionHighway, other.frictionHighway )) return false;
					if (!equals( this.frictionOffroad, other.frictionOffroad )) return false;
					if (!equals( this.frictionMud    , other.frictionMud     )) return false;
					if (!equals( this.onIce          , other.onIce           )) return false;
					
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
					return String.format("(H:%1.2f,O:%1.2f,M:%1.2f%s)\"", frictionHighway, frictionOffroad, frictionMud, onIce!=null && onIce ? ",Ice" : "");
				}
				
			}
		}
		
		@Override public void setData(Data data) {
			HashMap<String,RowItem> rows = new HashMap<>();
			for (Truck truck:data.trucks.values()) {
				for (CompatibleWheel wheel : truck.compatibleWheels) {
					if (wheel.wheelsDef==null) continue;
					String wheelsDefID = wheel.wheelsDef.id;
					String dlc = wheel.wheelsDef.dlcName;
					for (int i=0; i<wheel.wheelsDef.truckTires.size(); i++) {
						TruckTire tire = wheel.wheelsDef.truckTires.get(i);
						String key   = String.format("%s|%s[%d]", dlc==null ? "----" : dlc, wheelsDefID, i);
						String label = dlc==null
								? String.format(     "%s [%d]",      wheelsDefID, i)
								: String.format("%s | %s [%d]", dlc, wheelsDefID, i);
						RowItem rowItem = rows.get(key);
						if (rowItem==null)
							rows.put(key, rowItem = new RowItem(key, label, new RowItem.TireValues(tire)));
						rowItem.add(wheel.scale,tire,truck);
					}
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
	
		private String getTruckList(HashSet<Truck> trucks) {
			Iterable<String> it = ()->trucks
					.stream()
					.map(t->getTruckLabel(t,language))
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
	
		@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			if (rowIndex<0 || rowIndex>=rows.size()) return null;
			RowItem row = rows.get(rowIndex);
			switch (columnID) {
			case ID    : return row.label;
			case Sizes : return getSizeList ( row.sizes );
			case Trucks: return getTruckList( row.trucks );
			case Names : return getNameList ( row.names_StringID );
			case Friction_highway: return row.tireValues.frictionHighway;
			case Friction_offroad: return row.tireValues.frictionOffroad;
			case Friction_mud    : return row.tireValues.frictionMud;
			case OnIce           : return row.tireValues.onIce;
			}
			return null;
		}
	
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
	
	}

	private static class DLCTableModel extends Tables.SimplifiedTableModel<DLCTableModel.ColumnID> implements LanguageListener, TruckToDLCAssignmentListener, DataReceiver {

		private Language language;
		private final Vector<RowItem> rows;
		private HashMap<String, String> truckToDLCAssignments;
		private Data data;

		DLCTableModel(Controllers controllers) {
			super(ColumnID.values());
			language = null;
			truckToDLCAssignments = null;
			data = null;
			rows = new Vector<>();
			controllers.languageListeners.add(this);
			controllers.truckToDLCAssignmentListeners.add(this);
			controllers.dataReceivers.add(this);
		}

		@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
			this.truckToDLCAssignments = truckToDLCAssignments;
			rebuildRows();
		}

		@Override public void updateAfterAssignmentsChange() {
			rebuildRows();
		}

		@Override public void setLanguage(Language language) {
			this.language = language;
			fireTableUpdate();
		}
		
		@Override public void setData(Data data) {
			this.data = data;
			rebuildRows();
		}
		
		private void rebuildRows() {
			HashSet<String> truckIDs = new HashSet<>();
			if (truckToDLCAssignments!=null) truckIDs.addAll(truckToDLCAssignments.keySet());
			if (data                 !=null) truckIDs.addAll(data.trucks          .keySet());
			
			rows.clear();
			for (String truckID:truckIDs) {
				Truck truck = data==null ? null : data.trucks.get(truckID);
				String internalDLC = truck==null ? null : truck.dlcName==null ? "<LaunchDLC>" : truck.dlcName;
				String officialDLC = truckToDLCAssignments==null ? null : truckToDLCAssignments.get(truckID);
				if ((internalDLC!=null && !internalDLC.equals("<LaunchDLC>")) || officialDLC!=null)
					rows.add(new RowItem(internalDLC, officialDLC, truckID));
			}
			fireTableUpdate();
		}

		private static class RowItem {
			final String internalDLC;
			final String officialDLC;
			final String truckID;
			private RowItem(String internalDLC, String officialDLC, String truckID) {
				this.internalDLC = internalDLC;
				this.officialDLC = officialDLC;
				this.truckID = truckID;
			}
		}


		@Override public int getRowCount() {
			return rows.size();
		}

		@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			if (rowIndex<0 || rowIndex>=rows.size()) return null;
			RowItem row = rows.get(rowIndex);
			switch (columnID) {
			case Internal: return row.internalDLC;
			case Official: return row.officialDLC;
			case Truck:
				Truck truck = data==null || row.truckID==null ? null : data.trucks.get(row.truckID);
				if (truck == null) return String.format("<%s>", row.truckID);
				return SnowRunner.getTruckLabel(truck, language, false);
			}
			return null;
		}

		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			Internal ("Internal Label", String .class, 100),
			Official ("Official DLC"  , String .class, 200),
			Truck    ("Truck"         , String .class, 200),
			;
		
			private final SimplifiedColumnConfig config;
			ColumnID(String name, Class<?> columnClass, int prefWidth) {
				config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}
	}

	private static class TrucksPanel extends JSplitPane {
		private static final long serialVersionUID = 7004081774916835136L;
		
		private final JList<Truck> truckList;
		private final Controllers controllers;
		private final StandardMainWindow mainWindow;
		private HashMap<String, String> truckToDLCAssignments;
		
		TrucksPanel(StandardMainWindow mainWindow, Controllers controllers) {
			super(JSplitPane.HORIZONTAL_SPLIT);
			this.mainWindow = mainWindow;
			this.controllers = controllers;
			this.truckToDLCAssignments = null;
			setResizeWeight(0);
			
			TruckPanel truckPanel = new TruckPanel(this.mainWindow, this.controllers);
			truckPanel.setBorder(BorderFactory.createTitledBorder("Selected Truck"));
			
			JScrollPane truckListScrollPane = new JScrollPane(truckList = new JList<>());
			truckListScrollPane.setBorder(BorderFactory.createTitledBorder("All Trucks in Game"));
			truckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			truckList.addListSelectionListener(e->truckPanel.setTruck(truckList.getSelectedValue()));
			this.controllers.dataReceivers.add(data -> {
				Vector<Truck> items = new Vector<>(data.trucks.values());
				items.sort(Comparator.<Truck,String>comparing(Truck->Truck.id));
				truckList.setListData(items);
			});
			
			TruckListContextMenu truckListContextMenu = new TruckListContextMenu();
			this.controllers.languageListeners.add(truckListContextMenu);
			
			TruckListCellRenderer truckListCellRenderer = new TruckListCellRenderer(truckList);
			truckList.setCellRenderer(truckListCellRenderer);
			this.controllers.languageListeners.add(truckListCellRenderer);
			
			setLeftComponent(truckListScrollPane);
			setRightComponent(truckPanel);
			
			this.controllers.truckToDLCAssignmentListeners.add(new TruckToDLCAssignmentListener() {
				@Override public void updateAfterAssignmentsChange() {}
				@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
					TrucksPanel.this.truckToDLCAssignments = truckToDLCAssignments;
				}
			});
		}

		private class TruckListContextMenu extends ContextMenu implements LanguageListener {
			private static final long serialVersionUID = 2917583352980234668L;
			
			private int clickedIndex;
			private Truck clickedItem;
			private Language language;
		
			TruckListContextMenu() {
				clickedIndex = -1;
				clickedItem = null;
				language = null;
				
				addTo(truckList);
				
				JMenuItem miAssignToDLC = add(createMenuItem("Assign truck to an official DLC", true, e->{
					if (clickedItem==null) return;
					TruckAssignToDLCDialog dlg = new TruckAssignToDLCDialog(mainWindow, clickedItem, language, truckToDLCAssignments);
					boolean assignmentsChanged = dlg.showDialog();
					if (assignmentsChanged)
						controllers.truckToDLCAssignmentListeners.updateAfterAssignmentsChange();
				}));
				
				addContextMenuInvokeListener((comp, x, y) -> {
					clickedIndex = truckList.locationToIndex(new Point(x,y));
					clickedItem = clickedIndex<0 ? null : truckList.getModel().getElementAt(clickedIndex);
					
					miAssignToDLC.setEnabled(clickedItem!=null);
					
					miAssignToDLC.setText(
						clickedItem==null
						? "Assign truck to an official DLC"
						: String.format("Assign \"%s\" to an official DLC", getTruckLabel(clickedItem,language))
					);
				});
				
			}
			
			@Override public void setLanguage(Language language) {
				this.language = language;
			}
		}

		private static class TruckListCellRenderer implements ListCellRenderer<Truck>, LanguageListener {
			
			private static final Color COLOR_FG_DLCTRUCK = new Color(0x0070FF);
			private final Tables.LabelRendererComponent rendererComp;
			private Language language;
			private final JList<Truck> truckList;
		
			TruckListCellRenderer(JList<Truck> truckList) {
				this.truckList = truckList;
				rendererComp = new Tables.LabelRendererComponent();
				language = null;
			}
			
			@Override public void setLanguage(Language language) {
				this.language = language;
				truckList.repaint();
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
