package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.snowrunner.Data.AddonCategories;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.UserDefinedValues;
import net.schwarzbaer.java.games.snowrunner.MapTypes.StringVectorMap;
import net.schwarzbaer.java.games.snowrunner.MapTypes.VectorMap;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.KnownBugs;
import net.schwarzbaer.java.games.snowrunner.tables.AddonCategoriesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.CombinedTableTabTextOutputPanel.CombinedTableTabPaneTextPanePanel;
import net.schwarzbaer.java.games.snowrunner.tables.DLCTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.EnginesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.GearboxesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.SuspensionsTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.WinchesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier;
import net.schwarzbaer.java.games.snowrunner.tables.TrailersTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TruckAddonsTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TruckTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.WheelsTableModel;
import net.schwarzbaer.system.DateTimeFormatter;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	public static final String TruckToDLCAssignmentsFile = "SnowRunner - TruckToDLCAssignments.dat";
	public static final String UserDefinedValuesFile     = "SnowRunner - UserDefinedValues.dat";
	public static final String FilterRowsPresetsFile     = "SnowRunner - FilterRowsPresets.dat";
	public static final String ColumnHidePresetsFile     = "SnowRunner - ColumnHidePresets.dat";
	public static final String SpecialTruckAddonsFile    = "SnowRunner - SpecialTruckAddons.dat";
	
	public static final Color COLOR_FG_DLCTRUCK    = new Color(0x0070FF);
	public static final Color COLOR_FG_OWNEDTRUCK  = new Color(0x00AB00);
	
	public static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}


	private Data data;
	private final UserDefinedValues userDefinedValues;
	private HashMap<String,String> truckToDLCAssignments;
	private SaveGameData saveGameData;
	private SaveGame selectedSaveGame;
	private final StandardMainWindow mainWindow;
	private final JMenu languageMenu;
	private final Controllers controllers;
	private final RawDataPanel rawDataPanel;
	private final JMenuItem miSGValuesSorted;
	private final JMenuItem miSGValuesOriginal;
	private final JMenu selectedSaveGameMenu;
	private final SpecialTruckAddons specialTruckAddons;
	
	SnowRunner() {
		data = null;
		truckToDLCAssignments = null;
		saveGameData = null;
		selectedSaveGame = null;
		userDefinedValues = new UserDefinedValues();
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		controllers = new Controllers();
		Finalizer fin = controllers.createNewFinalizer();
		
		specialTruckAddons = new SpecialTruckAddons(controllers.specialTruckAddonsListeners);
		fin.addSpecialTruckAddonsListener((list, change) -> specialTruckAddons.writeToFile());
		
		rawDataPanel = new RawDataPanel(mainWindow, controllers);
		
		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.addTab("Trucks"          ,                        fin.addSubComp(new TrucksTablePanel         (mainWindow, controllers, specialTruckAddons, userDefinedValues)));
		contentPane.addTab("Trucks (old)"    ,                        fin.addSubComp(new TrucksListPanel          (mainWindow, controllers, specialTruckAddons)));
		contentPane.addTab("Wheels"          , TableSimplifier.create(fin.addSubComp(new WheelsTableModel         (mainWindow, controllers))));
		contentPane.addTab("DLCs"            , TableSimplifier.create(fin.addSubComp(new DLCTableModel            (            controllers))));
		contentPane.addTab("Trailers"        , TableSimplifier.create(fin.addSubComp(new TrailersTableModel       (mainWindow, controllers, true))));
		contentPane.addTab("Truck Addons"    ,                        fin.addSubComp(new TruckAddonsTablePanel    (mainWindow, controllers, specialTruckAddons)));
		contentPane.addTab("Engines"         , TableSimplifier.create(fin.addSubComp(new EnginesTableModel        (mainWindow, controllers, true, null))));
		contentPane.addTab("Gearboxes"       , TableSimplifier.create(fin.addSubComp(new GearboxesTableModel      (mainWindow, controllers, true, null))));
		contentPane.addTab("Suspensions"     , TableSimplifier.create(fin.addSubComp(new SuspensionsTableModel    (mainWindow, controllers, true, null))));
		contentPane.addTab("Winches"         , TableSimplifier.create(fin.addSubComp(new WinchesTableModel        (mainWindow, controllers, true, null))));
		contentPane.addTab("Addon Categories", TableSimplifier.create(fin.addSubComp(new AddonCategoriesTableModel(mainWindow, controllers))));
		contentPane.addTab("Raw Data"        ,                        fin.addSubComp(rawDataPanel));
		
		
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = menuBar.add(new JMenu("File"));
		fileMenu.add(createMenuItem("Switch to another \"initial.pak\"", true, e->{
			File initialPAK = selectInitialPAK();
			boolean changed = loadInitialPAK(initialPAK);
			if (changed) updateAfterDataChange();
		}));
		fileMenu.add(createMenuItem("Reload \"initial.pak\"", true, e->{
			boolean changed = loadInitialPAK();
			if (changed) updateAfterDataChange();
		}));
		fileMenu.add(createCheckBoxMenuItem("Hide Known Bugs", KnownBugs.getInstance().isHideKnownBugs(), null, true, KnownBugs.getInstance()::setHideKnownBugs));
		//fileMenu.add(createMenuItem("Reset application settings", true, e->{
		//	for (AppSettings.ValueKey key:AppSettings.ValueKey.values())
		//		settings.remove(key);
		//}));
		//fileMenu.add(createMenuItem("Test XMLTemplateStructure", true, e->{
		//	boolean changed = testXMLTemplateStructure();
		//	if (changed) updateAfterDataChange();
		//}));
		
		languageMenu = menuBar.add(new JMenu("Language"));
		
		ButtonGroup bg = new ButtonGroup();
		miSGValuesSorted   = createCheckBoxMenuItem("Show Values Sorted by Name"   ,  rawDataPanel.isShowingSaveGameDataSorted(), bg, false, ()->rawDataPanel.showSaveGameDataSorted(true ));
		miSGValuesOriginal = createCheckBoxMenuItem("Show Values in Original Order", !rawDataPanel.isShowingSaveGameDataSorted(), bg, false, ()->rawDataPanel.showSaveGameDataSorted(false));
		
		JMenu saveGameDataMenu = menuBar.add(new JMenu("SaveGame Data"));
		saveGameDataMenu.add(createMenuItem("Reload SaveGame Data", true, e->reloadSaveGameData()));
		saveGameDataMenu.addSeparator();
		saveGameDataMenu.add(selectedSaveGameMenu = new JMenu("Selected SaveGame"));
		saveGameDataMenu.addSeparator();
		saveGameDataMenu.add(miSGValuesSorted  );
		saveGameDataMenu.add(miSGValuesOriginal);
		selectedSaveGameMenu.setEnabled(false);
		
		JMenu testingMenu = menuBar.add(new JMenu("Testing"));
		testingMenu.add(createMenuItem("Show Event Listeners", true, e->{
			controllers.showListeners();
		}));
		
		mainWindow.setIconImagesFromResource("/images/AppIcons/AppIcon","016.png","024.png","032.png","040.png","048.png","056.png","064.png","128.png","256.png");
		mainWindow.startGUI(contentPane, menuBar);
		
		settings.registerAppWindow(mainWindow);
	}

	public interface TruckToDLCAssignmentListener {
		void setTruckToDLCAssignments(HashMap<String, String> assignments);
		void updateAfterAssignmentsChange();
	}
	
	private void initialize() {
		userDefinedValues.read();
		truckToDLCAssignments = TruckAssignToDLCDialog.loadStoredData();
		controllers.truckToDLCAssignmentListeners.setTruckToDLCAssignments(truckToDLCAssignments);
		
		if (loadInitialPAK    ()) updateAfterDataChange();
		if (reloadSaveGameData()) updateAfterSaveGameChange();
		
	}

	private boolean reloadSaveGameData() {
		File saveGameFolder = getSaveGameFolder();
		if (saveGameFolder==null) return false;
		
		System.out.printf("Read Data from SaveGame Folder \"%s\" ...%n", saveGameFolder.getAbsolutePath());
		saveGameData = new SaveGameData(saveGameFolder);
		saveGameData.readData();
		System.out.printf("... done%n");
		
		Vector<String> indexStrs = new Vector<>(saveGameData.saveGames.keySet());
		indexStrs.sort(null);
		
		selectedSaveGame = null;
		String selectedSaveGameIndexStr = settings.getString(AppSettings.ValueKey.SelectedSaveGame, null);
		if (selectedSaveGameIndexStr!=null) {
			selectedSaveGame = saveGameData.saveGames.get(selectedSaveGameIndexStr);
		}
		if (selectedSaveGame==null && !saveGameData.saveGames.isEmpty()) {
			if (indexStrs.size()==1) {
				selectedSaveGameIndexStr = indexStrs.get(0);
			} else {
				String[] values = indexStrs.stream().map(this::getSaveGameLabel).toArray(String[]::new);
				Object selection = JOptionPane.showInputDialog(mainWindow, "Select a SaveGame: ", "Select SaveGame", JOptionPane.QUESTION_MESSAGE, null, values, values[0]);
				int index = Arrays.asList(values).indexOf(selection);
				selectedSaveGameIndexStr = index<0 ? null : indexStrs.get(index);
			}
			if (selectedSaveGameIndexStr!=null)
				selectedSaveGame = saveGameData.saveGames.get(selectedSaveGameIndexStr);
			if (selectedSaveGame!=null)
				settings.putString(AppSettings.ValueKey.SelectedSaveGame, selectedSaveGameIndexStr);
		}
		
		ButtonGroup bg = new ButtonGroup();
		selectedSaveGameMenu.removeAll();
		selectedSaveGameMenu.setEnabled(true);
		for (String indexStr : indexStrs) {
			boolean isSelected = selectedSaveGame!=null && indexStr.equals(selectedSaveGame.indexStr);
			selectedSaveGameMenu.add(createCheckBoxMenuItem(getSaveGameLabel(indexStr), isSelected, bg, true, ()->{
				selectedSaveGame = saveGameData.saveGames.get(indexStr);
				if (selectedSaveGame!=null)
					settings.putString(AppSettings.ValueKey.SelectedSaveGame, indexStr);
				else
					settings.remove(AppSettings.ValueKey.SelectedSaveGame);
				updateAfterSaveGameChange();
			}));
		}
		
		rawDataPanel.setData(saveGameData);
		
		miSGValuesSorted  .setEnabled(true);
		miSGValuesOriginal.setEnabled(true);
		return true;
	}

	private String getSaveGameLabel(String indexStr) {
		SaveGame saveGame = saveGameData==null || saveGameData.saveGames==null ? null : saveGameData.saveGames.get(indexStr);
		String mode     = saveGame==null ? "??" : saveGame.isHardMode ? "HardMode" : "NormalMode";
		String saveTime = saveGame==null ? "??" : new DateTimeFormatter().getTimeStr(saveGame.saveTime, false, true, false, true, false);
		return String.format("SaveGame %s (%s, %s)", indexStr, mode, saveTime);
	}

	private boolean loadInitialPAK() {
		return loadInitialPAK(getInitialPAK());
	}

	private boolean loadInitialPAK(File initialPAK)
	{
		if (initialPAK!=null) {
			Data newData = ProgressDialog.runWithProgressDialogRV(mainWindow, String.format("Read \"%s\"", initialPAK.getAbsolutePath()), 600, pd->{
				Data localData = readXMLTemplateStructure(pd,initialPAK);
				if (Thread.currentThread().isInterrupted()) return null;
				return localData;
			});
			if (newData!=null) {
				data = newData;
				return true;
			}
		}
		return false;
	}
	
	private Data readXMLTemplateStructure(ProgressDialog pd, File initialPAK) {
		setTask(pd, "Read XMLTemplateStructure");
		XMLTemplateStructure structure = XMLTemplateStructure.readPAK(initialPAK,mainWindow);
		if (structure==null) return null;
		if (Thread.currentThread().isInterrupted()) return null;
		setTask(pd, "Parse Data from XMLTemplateStructure");
		System.out.printf("Parse Data from XMLTemplateStructure ...%n");
		Data data = new Data(structure);
		System.out.printf("... done%n");
		if (Thread.currentThread().isInterrupted()) return null;
		return data;
	}


	static void setTask(ProgressDialog pd, String taskTitle) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setIndeterminate(true);
		});
	}
	 
	private File getSaveGameFolder() {
		// c:\\Program Files (x86)\\Steam\\userdata\\<Account>\\1465360\\remote
		File saveGameFolder = settings.getFile(AppSettings.ValueKey.SaveGameFolder, null);
		if (saveGameFolder != null && saveGameFolder.isDirectory())
			return saveGameFolder;
		
		File steamFolder = new File("C:\\Program Files (x86)\\Steam");
		if (!steamFolder.isDirectory())
			steamFolder = new File("C:\\Program Files\\Steam");
		if (!steamFolder.isDirectory())
			return getSaveGameFolder_askUser();
		
		File userdataFolder = new File(steamFolder,"userdata");
		if (!userdataFolder.isDirectory())
			return getSaveGameFolder_askUser();
		
		File[] accountFolders = userdataFolder.listFiles(file->{
			String name = file.getName();
			return file.isDirectory() && !name.equals(".") && !name.equals("..");
		});
		
		saveGameFolder = null;
		for (File accountFolder : accountFolders) {
			saveGameFolder = new File(accountFolder,"1465360\\remote");
			if (saveGameFolder.isDirectory()) break;
			saveGameFolder = null;
		}
		if (saveGameFolder==null)
			return getSaveGameFolder_askUser();
		
		settings.putFile(AppSettings.ValueKey.SaveGameFolder, saveGameFolder);
		return saveGameFolder;
	}
	
	private File getSaveGameFolder_askUser() {
		JFileChooser fc = new JFileChooser("./");
		fc.setDialogTitle("Select SaveGame Folder");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
		fc.setMultiSelectionEnabled(false);
		if (fc.showOpenDialog(mainWindow)!=JFileChooser.APPROVE_OPTION)
			return null;
		
		File saveGameFolder = fc.getSelectedFile();
		settings.putFile(AppSettings.ValueKey.SaveGameFolder, saveGameFolder);
		return saveGameFolder;
	}

	private File getInitialPAK() {
		File initialPAK = settings.getFile(AppSettings.ValueKey.InitialPAK, null);
		if (initialPAK==null || !initialPAK.isFile())
			initialPAK = selectInitialPAK();
		return initialPAK;
	}

	private File selectInitialPAK() {
		DataSelectDialog dlg = new DataSelectDialog(mainWindow);
		File initialPAK = dlg.showDialog();
		if (initialPAK==null || !initialPAK.isFile()) return null;
		settings.putFile(AppSettings.ValueKey.InitialPAK, initialPAK);
		return initialPAK;
	}

	public interface SaveGameListener {
		void setSaveGame(SaveGame saveGame);
	}
	
	private void updateAfterSaveGameChange() {
		controllers.saveGameListeners.setSaveGame(selectedSaveGame);
	}

	public interface DataReceiver {
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
			languageMenu.add(createCheckBoxMenuItem(langID, langID.equals(currentLangID), bg, true, ()->setLanguage(langID)));
		
		setLanguage(currentLangID);
		
		EnumSet<SpecialTruckAddons.AddonCategory> emptyLists = EnumSet.noneOf(SpecialTruckAddons.AddonCategory.class);
		specialTruckAddons.foreachList((listID,list)->{
			int count = list.findIn(data.truckAddons);
			if (count==0)
				emptyLists.add(listID);
		});
		
		if (!emptyLists.isEmpty()) {
			Iterable<String> it = ()->emptyLists.stream().<String>map(listID->listID.label.toLowerCase()+"s").sorted().iterator();
			String missingObjects = String.join(" or ", it);
			String message = wrapWords(65, "Their are no special TruckAddons like " + missingObjects + " defined or found in loaded data. Please select these via context menu in any TruckAddon table.");
			JOptionPane.showMessageDialog(mainWindow, message, "Please define special TruckAddons", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	private static String wrapWords(int maxLength, String text) {
		String[] words = text.split(" ");
		StringBuilder sb = new StringBuilder();
		int lineLength = 0;
		for (String word : words) {
			if (lineLength+1+word.length() > maxLength) {
				sb.append(String.format("%n"));
				lineLength = 0;
			} else {
				sb.append(" ");
				lineLength++;
			}
			sb.append(word);
			lineLength += word.length();
			
		}
		return sb.toString();
	}

	public interface LanguageListener {
		void setLanguage(Language language);
	}
	
	private void setLanguage(String langID) {
		Language language = langID==null ? null : data.languages.get(langID);
		
		if (language!=null)
			settings.putString(AppSettings.ValueKey.Language, langID);
		else
			settings.remove(AppSettings.ValueKey.Language);
		
		/*
		// DEBUG
		if (language!=null) {
			for (String country : new String[] {"US","RU"}) {
				System.out.printf("Country: %s%n", country);
				for (int region=1; region<7; region++) {
					String regionName_ID = String.format("%s_%02d", country, region);
					String regionName = language.dictionary.get(regionName_ID);
					System.out.printf("   Region[%d]:  %s  <%s>%n", region, regionName, regionName_ID);
					for (int map=1; map<6; map++) {
						String mapName_ID = String.format("%s_%02d_%02d_NAME", country, region, map);
						String mapName = language.dictionary.get(mapName_ID);
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("%s_%02d_%02d_NEW_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase();
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase()+"_NAME";
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName!=null)
							System.out.printf("      Map[%d,%d]:  %s  <%s>%n", region, map, mapName, mapName_ID);
					}
				}
			}
		}
		// DEBUG
		*/
		
		controllers.languageListeners.setLanguage(language);
	}

	public static String solveStringID(Data.HasNameAndID namedToken, Language language) {
		if (namedToken==null) return null;
		String id = namedToken.getID();
		return solveStringID(namedToken, id, language);
	}

	public static String solveStringID(Data.HasNameAndID namedToken, String id, Language language) {
		String name_StringID = namedToken==null ? null : namedToken.getName_StringID();
		if (name_StringID==null && id==null) return null;
		return solveStringID(name_StringID, language, "<"+id+">");
	}
	
	public static String solveStringID(String strID, Language language) {
		if (strID==null) return null;
		return solveStringID(strID, language, "<"+strID+">");
	}
	
	public static String solveStringID(String strID, Language language, String defaultStr) {
		if (strID==null) strID = defaultStr;
		String str = null;
		if (language!=null) str = language.dictionary.get(strID);
		if (str==null) str = defaultStr;
		return str;
	}
	
	public static String getReducedString(String str, int maxLength) {
		if (str==null) return null;
		if (str.length() > maxLength-4)
			return str.substring(0,maxLength-4)+" ...";
		return str;
	}
	
	public static void removeRedundantStrs(Vector<String> strs, boolean sort) {
		HashSet<String> set = new HashSet<>(strs);
		strs.clear();
		strs.addAll(set);
		if (sort) strs.sort(null);
	}

	public static String[] removeRedundantStrs(String[] strs, boolean sort) {
		HashSet<String> set = new HashSet<>(Arrays.asList(strs));
		String[] array = set.toArray(new String[set.size()]);
		if (sort) Arrays.sort(array);
		return array;
	}

	public static String selectNonNull(String... strings) {
		for (String str : strings)
			if (str!=null)
				return str;
		return null;
	}

	public static <V> Vector<V> addNull(V[] array) {
		Vector<V> values = new Vector<>(Arrays.asList(array));
		values.insertElementAt(null, 0);
		return values;
	}

	private static <Comp extends AbstractButton> Comp configureAbstractButton(Comp comp, String title, Boolean isSelected, ButtonGroup bg, boolean isEnabled, Insets margin, Consumer<Boolean> setValue)
	{
		return configureAbstractButton(comp, title, isSelected, bg, isEnabled, margin, setValue == null ? null : (ActionListener) (e->{
			setValue.accept(comp.isSelected());			
		}));
	}

	private static <Comp extends AbstractButton> Comp configureAbstractButton(Comp comp, String title, Boolean isSelected, ButtonGroup bg, boolean isEnabled, Insets margin, ActionListener al)
	{
		comp.setEnabled(isEnabled);
		if (title     !=null) comp.setText(title);
		if (bg        !=null) bg.add(comp);
		if (isSelected!=null) comp.setSelected(isSelected);
		if (al        !=null) comp.addActionListener(al);
		if (margin    !=null) comp.setMargin(margin);
		return comp;
	}

	public static JCheckBoxMenuItem createCheckBoxMenuItem(String title, boolean isSelected, ButtonGroup bg, boolean isEnabled, Consumer<Boolean> setValue) {
		return configureAbstractButton(new JCheckBoxMenuItem(), title, isSelected, bg, isEnabled, null, setValue);
	}

	public static JCheckBoxMenuItem createCheckBoxMenuItem(String title, boolean isSelected, ButtonGroup bg, boolean isEnabled, Runnable al) {
		return configureAbstractButton(new JCheckBoxMenuItem(), title, isSelected, bg, isEnabled, null, (ActionListener) e->al.run());
	}

	public static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		return configureAbstractButton(new JMenuItem(), title, null, null, isEnabled, null, al);
	}
	
	public static JCheckBox createCheckBox(String title, boolean isSelected, ButtonGroup bg, boolean isEnabled, Consumer<Boolean> setValue) {
		return configureAbstractButton(new JCheckBox(), title, isSelected, bg, isEnabled, null, setValue);
	}

	public static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		return configureAbstractButton(new JButton(), title, null, null, isEnabled, null, al);
	}
	
	public static JButton createButton(String title, boolean isEnabled, Insets margin, ActionListener al) {
		return configureAbstractButton(new JButton(), title, null, null, isEnabled, margin, al);
	}
	public static <AC> JButton createButton(String title, boolean isEnabled, Disabler<AC> disabler, AC ac, ActionListener al) {
		JButton comp = createButton(title, isEnabled, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}

	public static JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, ActionListener al) {
		return configureAbstractButton(new JRadioButton(), title, isSelected, bg, isEnabled, null, al);
	}
	public static <AC> JRadioButton createRadioButton(String title, ButtonGroup bg, boolean isEnabled, boolean isSelected, Disabler<AC> disabler, AC ac, ActionListener al) {
		JRadioButton comp = createRadioButton(title, bg, isEnabled, isSelected, al);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}
	
	public static <AC> JLabel createLabel(String text, Disabler<AC> disabler, AC ac) {
		JLabel comp = new JLabel(text);
		if (disabler!=null) disabler.add(ac, comp);
		return comp;
	}
	
	public static JTextField createIntTextField(int columns, String text, Predicate<Integer> isOK, Consumer<Integer> setValue) {
		return createTextField(columns, text, str->{
			try { return Integer.parseInt(str); }
			catch (NumberFormatException e) { return null; }
		}, isOK, setValue);
	}
	
	public static JTextField createLongTextField(int columns, String text, Predicate<Long> isOK, Consumer<Long> setValue) {
		return createTextField(columns, text, str->{
			try { return Long.parseLong(str); }
			catch (NumberFormatException e) { return null; }
		}, isOK, setValue);
	}
	
	public static JTextField createFloatTextField(int columns, String text, Predicate<Float> isOK, Consumer<Float> setValue) {
		return createTextField(columns, text, str->{
			try { return Float.parseFloat(str); }
			catch (NumberFormatException e) { return null; }
		}, isOK, setValue);
	}
	
	public static JTextField createDoubleTextField(int columns, String text, Predicate<Double> isOK, Consumer<Double> setValue) {
		return createTextField(columns, text, str->{
			try { return Double.parseDouble(str); }
			catch (NumberFormatException e) { return null; }
		}, isOK, setValue);
	}
	
	public static JTextField createStringTextField(int columns, String text, Consumer<String> setValue) {
		return createTextField(columns, text, str->str, null, setValue);
	}
	
	public static <V> JTextField createTextField(int columns, String text, Function<String,V> convert, Predicate<V> isOK, Consumer<V> setValue) {
		JTextField comp = new JTextField(text,columns);
		Color defaultBG = comp.getBackground();
		if (setValue!=null && convert!=null) {
			Runnable action = ()->{
				String str = comp.getText();
				V value = convert.apply(str);
				if (value==null) {
					comp.setBackground(Color.RED);
					comp.setToolTipText("Can't convert entered text to value.");
					return;
				}
				if (isOK!=null && !isOK.test(value)) {
					comp.setBackground(Color.RED);
					comp.setToolTipText("Entered value doesn't meets required criteria.");
					return;
				}
				comp.setBackground(defaultBG);
				setValue.accept(value);
			};
			comp.addActionListener(e->action.run());
			comp.addFocusListener(new FocusListener() {
				@Override public void focusGained(FocusEvent e) {}
				@Override public void focusLost(FocusEvent e) { action.run(); }
			});
		} else
			throw new IllegalArgumentException();
		return comp;
	}

	public static String getTruckLabel(Truck truck, Language language) {
		return getTruckLabel(truck, language, true);
	}
	public static String getTruckLabel(Truck truck, Language language, boolean addInternalDLC ) {
		if (truck==null)
			return "<null>";
		
		String truckName = SnowRunner.solveStringID(truck, language);
		
		if (addInternalDLC && truck.dlcName!=null)
			truckName = String.format("%s [%s]", truckName, truck.dlcName);
		
		return truckName;
	}

	public static String[][] getNamesFromIDs(String[][] idLists, Function<String, String> getName_StringID, Language language) {
		String[][] names = new String[idLists.length][];
		for (int i=0; i<names.length; i++)
			names[i] = getNamesFromIDs(idLists[i],getName_StringID,language);
		return names;
	}

	public static String[] getNamesFromIDs(String[] idList, Function<String,String> getName_StringID, Language language) {
		if (getName_StringID==null)
			return idList;
		
		String[] names = Arrays.stream(idList).map(id->getNameFromID(id, getName_StringID, language)).toArray(String[]::new);
		return removeRedundantStrs(names, true);
	}

	public static Function<String,String> createGetNameFunction(HashMap<String, TruckAddon> truckAddons, HashMap<String, Trailer> trailers) {
		if (truckAddons==null && trailers==null)
			return null;
		
		return id -> {
			if (id == null)
				return "<null>";
			
			String name_StringID = null;
			
			if (truckAddons!=null && name_StringID==null) {
				TruckAddon truckAddon = truckAddons.get(id);
				if (truckAddon != null) name_StringID = truckAddon.gameData.name_StringID;
			}
			if (trailers!=null && name_StringID==null) {
				Trailer trailer = trailers.get(id);
				if (trailer != null) name_StringID = trailer.gameData.name_StringID;
			}
			
			return name_StringID;
		};
	}

	public static String getNameFromID(String id, Function<String,String> getName_StringID, Language language) {
		if (getName_StringID==null)
			throw new IllegalArgumentException();
		if (id == null) return "<null>";
		String name_StringID = getName_StringID.apply(id);
		return solveStringID(name_StringID, language, String.format("<%s>", id));
	}

	public static String joinTruckAddonNames(Vector<TruckAddon> list, AddonCategories addonCategories, Language language) {
		Function<TruckAddon, String> mapper = addon->{
			String name = SnowRunner.solveStringID(addon, language);
			String categoryLabel = AddonCategories.getCategoryLabel(addon.gameData.category,addonCategories,language);
			return String.format("[%s] \"%s\"", categoryLabel, name);
		};
		return String.join(", ", (Iterable<String>)()->list.stream().map(mapper).sorted().iterator());
	}
	
	public static String joinNames(Vector<? extends Data.HasNameAndID> list, Language language) {
		return String.join(", ", (Iterable<String>)()->list.stream().map(item->SnowRunner.solveStringID(item, language)).sorted().iterator());
	}

	public static String getIdAndName(String id, String stringID, Language lang) {
		if (id!=null && stringID!=null)
			return String.format("[%s] %s", id, SnowRunner.solveStringID(stringID, lang));
		if (id!=null)
			return String.format("[%s]", id);
		if (stringID!=null)
			return SnowRunner.solveStringID(stringID, lang);
		return null;
	}

	public static String joinRequiredAddonsToString_OneLine(String[][] requiredAddons) {
		if (requiredAddons==null || requiredAddons.length==0) return null;
		
		Iterable<String> it = ()->Arrays.stream(requiredAddons).map(list->{
			String str = String.join(" OR ", Arrays.asList(list));
			if (list.length==1) return str;
			return String.format("(%s)", str);
		}).iterator();
		
		String str = String.join(" AND ", it);
		if (requiredAddons.length==1) return str;
		return String.format("(%s)", str);
	}

	public static String joinRequiredAddonsToString(String[][] requiredAddons, String indent) {
		Iterable<String> it = ()->Arrays.stream(requiredAddons).map(list->String.join("  OR  ", list)).iterator();
		String orGroupIndent = "  ";
		return indent+orGroupIndent+String.join(String.format("%n%1$sAND%n%1$s"+orGroupIndent, indent), it);
	}

	public static String joinRequiredAddonsToString(String[][] requiredAddons, Function<String,String> getName_StringID, Language language, String indent) {
		if (requiredAddons==null || requiredAddons.length==0) return null;
		String[][] requiredAddonNames = getNamesFromIDs(requiredAddons, getName_StringID, language);
		return joinRequiredAddonsToString(requiredAddonNames, indent);
	}

	public static void writeRequiredAddonsToDoc(StyledDocumentInterface doc, Color operatorColor, String[][] requiredAddons, String indent) {
		for (int i=0; i<requiredAddons.length; i++) {
			if (i>0) {
				doc.append("%n%s", indent);
				doc.append(new StyledDocumentInterface.Style(operatorColor,"Monospaced"), "AND%n");
			}
			doc.append("%s", indent+"  ");
			for (int j=0; j<requiredAddons[i].length; j++) {
				if (j>0) doc.append(new StyledDocumentInterface.Style(operatorColor,"Monospaced"), " OR ");
				doc.append("%s", requiredAddons[i][j]);
			}
		}
	}
	
	private static class Pair<V1,V2> {
		final V1 v1;
		final V2 v2;
		Pair(V1 v1, V2 v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
		static Pair<Truck,String> create(Truck truck, Language language) {
			return new Pair<>(truck, SnowRunner.solveStringID(truck, language));
		}
	}

	public static void writeTruckNamesToDoc(StyledDocumentInterface doc, Color ownedTruckColor, Vector<Truck> list, Language language, SaveGame saveGame) {
		Stream<Pair<Truck,String>> stream = list.stream().map(truck->Pair.create(truck,language)).sorted(Comparator.<Pair<Truck,String>,String>comparing(pair->pair.v2));
		boolean isFirst = true;
		for (Iterator<Pair<Truck,String>> iterator = stream.iterator(); iterator.hasNext();) {
			if (!isFirst) doc.append(", ");
			isFirst = false;
			Pair<Truck,String> pair = iterator.next();
			if (saveGame!=null && saveGame.playerOwnsTruck(pair.v1))
				doc.append(ownedTruckColor,pair.v2);
			else
				doc.append(pair.v2);
		}
	}

	public static void writeRequiredAddonsToDoc(StyledDocumentInterface doc, Color operatorColor, String[][] requiredAddons, Function<String,String> getName_StringID, Language language, String indent) {
		if (requiredAddons==null || requiredAddons.length==0) return;
		String[][] requiredAddonNames = getNamesFromIDs(requiredAddons, getName_StringID, language);
		writeRequiredAddonsToDoc(doc, operatorColor, requiredAddonNames, indent);
	}

	public static String joinAddonIDs(String[] strs, boolean emptyAndNullReturnsNull) {
		if (strs==null    ) return emptyAndNullReturnsNull ? null : "<null>";
		if (strs.length==0) return emptyAndNullReturnsNull ? null : "[]";
		if (strs.length==1) return strs[0];
		return Arrays.toString(strs);
	}


	static final Comparator<String> CATEGORY_ORDER = Comparator.<String>comparingInt(SnowRunner::getCategoryOrderIndex).thenComparing(Comparator.naturalOrder());
	static final List<String> CATEGORY_ORDER_LIST = Arrays.asList("Trailers", "engine", "gearbox", "suspension", "winch", "awd", "diff_lock", "frame_addons");
	static int getCategoryOrderIndex(String category) {
		//return "frame_addons".equals(category) ? 0 : 1;
		int pos = CATEGORY_ORDER_LIST.indexOf(category);
		if (pos<0) return CATEGORY_ORDER_LIST.size();
		return pos;
	}
	
	public static class SpecialTruckAddons {

		private static final String ValuePrefix = "$ ";
		private static final String ListIdClosingBracket = "]]";
		private static final String ListIDOpeningBracket = "[[";

		public interface Listener {
			void specialTruckAddOnsHaveChanged(AddonCategory category, Change change);
		}

		public enum Change { Added, Removed }
		public enum AddonType {
			Addon(addon->!addon.gameData.isCargo),
			Cargo(addon-> addon.gameData.isCargo),
			;
			public final Predicate<TruckAddon> isAllowed;
			AddonType(Predicate<TruckAddon> isAllowed) { this.isAllowed = isAllowed; }
		}
		
		public enum AddonCategory {
			MetalDetectors  ("Metal Detector"  , AddonType.Addon),
			SeismicVibrators("Seismic Vibrator", AddonType.Addon),
			LogLifts        ("Log Lift"        , AddonType.Addon),
			MiniCranes      ("Mini Crane"      , AddonType.Addon),
			BigCranes       ("Big Crane"       , AddonType.Addon),
			ShortLogs       ("Short Log"       , AddonType.Cargo),
			MediumLogs      ("Medium Log"      , AddonType.Cargo),
			LongLogs        ("Long Log"        , AddonType.Cargo),
			;
			
			public final AddonType type;
			public final String label;
			public final Predicate<TruckAddon> isAllowed;
			AddonCategory(String label, AddonType type) {
				this(label, type, type.isAllowed);
			}
			AddonCategory(String label, AddonType type, Predicate<TruckAddon> isAllowed) {
				this.label = label;
				this.type = type;
				this.isAllowed = isAllowed;
			}
		}

		private final Listener listenersController;
		private final EnumMap<AddonCategory,SpecialTruckAddonList> lists;
		
		public SpecialTruckAddons(Listener listenersController) {
			this.listenersController = listenersController;
			lists = new EnumMap<>(AddonCategory.class);
			for (AddonCategory cat : AddonCategory.values())
				lists.put(cat, new SpecialTruckAddonList(cat));
			
			// classes using this expect lists with values 
			readFromFile();
		}
		
		public void readFromFile() {
			File file = new File(SpecialTruckAddonsFile);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				
				System.out.printf("Read SpecialTruckAddons from file \"%s\" ...%n", file.getAbsolutePath());
				
				SpecialTruckAddonList list = null;
				String line, valueStr;
				while ( (line=in.readLine())!=null ) {
					
					if (line.isEmpty()) continue;
					if (line.startsWith(ListIDOpeningBracket) && line.endsWith(ListIdClosingBracket)) {
						valueStr = line.substring(ListIDOpeningBracket.length(), line.length()-ListIdClosingBracket.length());
						try {
							AddonCategory listID = AddonCategory.valueOf(valueStr);
							list = lists.get(listID);
							if (list==null)
								throw new IllegalStateException(String.format("Found List ID (\"%s\") with no assigned SpecialTruckAddonList in file \"%s\".", listID, file.getAbsolutePath()));
						} catch (Exception e) {
							throw new IllegalStateException(String.format("Found unknown List ID (\"%s\") in file \"%s\".", valueStr, file.getAbsolutePath()));
						}
					}
					if (line.startsWith(ValuePrefix) && list!=null) {
						valueStr = line.substring(ValuePrefix.length());
						list.idList.add(valueStr);
					}
					
				}
				
				System.out.printf("... done%n");
				
			} catch (FileNotFoundException ex) {
				//ex.printStackTrace();
			} catch (IOException ex) {
				System.err.printf("IOException while reading SpecialTruckAddons from file \"%s\": %s", file.getAbsolutePath(), ex.getMessage());
				//ex.printStackTrace();
			}
		}

		public void writeToFile() {
			File file = new File(SpecialTruckAddonsFile);
			try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
				
				System.out.printf("Write SpecialTruckAddons to file \"%s\" ...%n", file.getAbsolutePath());
				
				String listIdFormat = String.format("%s%%s%s%%n", ListIDOpeningBracket, ListIdClosingBracket);
				String valueFormat = String.format("%s%%s%%n", ValuePrefix);
				for (AddonCategory list : AddonCategory.values()) {
					out.printf(listIdFormat, list.name());
					SpecialTruckAddonList values = lists.get(list);
					Vector<String> vec = new Vector<>(values.idList);
					vec.sort(null);
					for (String id : vec) out.printf(valueFormat, id);
					out.printf("%n");
				}
				
				System.out.printf("... done%n");
				
			} catch (IOException ex) {
				System.err.printf("IOException while writing SpecialTruckAddons to file \"%s\": %s", file.getAbsolutePath(), ex.getMessage());
				//ex.printStackTrace();
			}
		}

		public void foreachList(BiConsumer<AddonCategory,SpecialTruckAddonList> action) {
			for (AddonCategory category : AddonCategory.values())
				action.accept(category, lists.get(category));
		}

		public SpecialTruckAddonList getList(AddonCategory category) {
			return lists.get(category);
		}

		public class SpecialTruckAddonList {
			
			private final HashSet<String> idList;
			private final AddonCategory category;

			SpecialTruckAddonList(AddonCategory list) {
				this.category = list;
				idList = new HashSet<>();
			}

			@Override
			public String toString() {
				Vector<String> sorted = new Vector<>(idList);
				sorted.sort(null);
				return String.format("<%s> %s", category, sorted.toString());
			}

			public void forEach(Consumer<String> action) {
				idList.forEach(action);
			}

			public <Result> Result forEach(Supplier<Result> getInitial, BiFunction<Result,Result,Result> combine, Predicate<Result> isFinalResult, Function<String,Result> action) {
				Result result = getInitial.get();
				for (String id : idList) {
					result = combine.apply(result, action.apply(id));
					if (isFinalResult.test(result)) break;
				}
				return result;
			}

			int findIn(HashMap<String, TruckAddon> truckAddons) {
				if (truckAddons==null) return 0;
				int count = 0;
				for (String id : idList)
					if (truckAddons.containsKey(id))
						count++;
				return count;
			}

			public boolean contains(TruckAddon addon) {
				return addon!=null && idList.contains(addon.id);
			}

			public void remove(TruckAddon addon) {
				if (addon==null) return;
				idList.remove(addon.id);
				listenersController.specialTruckAddOnsHaveChanged(category, Change.Removed);
			}

			public void add(TruckAddon addon) {
				if (addon==null) return;
				idList.add(addon.id);
				listenersController.specialTruckAddOnsHaveChanged(category, Change.Added);
			}
		}
		
	}

	private static class TruckAddonsTablePanel extends CombinedTableTabPaneTextPanePanel implements Finalizable {
		private static final long serialVersionUID = 7841445317301513175L;
		private static final String CONTROLLERS_CHILDLIST_TABTABLEMODELS = "TabTableModels";
		
		private final Window mainWindow;
		private final Finalizer finalizer;
		private final Vector<Tab> tabs;
		private final SpecialTruckAddons specialTruckAddOns;
		private Data data;
		private Language language;
		private SaveGame saveGame;

		TruckAddonsTablePanel(Window mainWindow, Controllers controllers, SpecialTruckAddons specialTruckAddOns) {
			this.mainWindow = mainWindow;
			this.finalizer = controllers.createNewFinalizer();
			this.specialTruckAddOns = specialTruckAddOns;
			this.language = null;
			this.data = null;
			this.tabs = new Vector<>();
			this.saveGame = null;
			
			finalizer.addLanguageListener(language->{
				this.language = language;
				updateTabTitles();
			});
			finalizer.addDataReceiver(data->{
				this.data = data;
				rebuildTabPanels();
			});
			finalizer.addSaveGameListener(saveGame->{
				this.saveGame = saveGame;
			});
		}

		@Override public void prepareRemovingFromGUI() {
			finalizer.removeSubCompsAndListenersFromGUI();
		}

		private void updateTabTitles() {
			AddonCategories addonCategories = data==null ? null : data.addonCategories;
			for (int i=0; i<tabs.size(); i++)
				setTabTitle(i, tabs.get(i).getTabTitle(addonCategories, language));
			
		}

		private void rebuildTabPanels() {
			removeAllTabs();
			tabs.clear();
			finalizer.removeVolatileSubCompsFromGUI(CONTROLLERS_CHILDLIST_TABTABLEMODELS);
			
			if (data==null) return;
			
			StringVectorMap<TruckAddon> truckAddons = new StringVectorMap<>();
			for (TruckAddon addon : data.truckAddons.values())
				truckAddons.add(addon.getCategory(), addon);
			
			Tab allTab = new Tab(null, data.truckAddons.size());
			tabs.add(allTab);
			
			String title = allTab.getTabTitle(data.addonCategories, language);
			TruckAddonsTableModel tableModel = new TruckAddonsTableModel(mainWindow, finalizer.getControllers(), false, specialTruckAddOns).set(data, saveGame);
			finalizer.addVolatileSubComp(CONTROLLERS_CHILDLIST_TABTABLEMODELS, tableModel);
			addTab(title, tableModel);
			tableModel.setRowData(data.truckAddons.values());
			
			Vector<String> categories = new Vector<>(truckAddons.keySet());
			categories.sort(CATEGORY_ORDER);
			
			for (String category : categories) {
				Vector<TruckAddon> list = truckAddons.get(category);
				
				Tab tab = new Tab(category, list.size());
				tabs.add(tab);
				
				title = tab.getTabTitle(data.addonCategories, language);
				tableModel = new TruckAddonsTableModel(mainWindow, finalizer.getControllers(), false, specialTruckAddOns).set(data, saveGame);
				finalizer.addVolatileSubComp(CONTROLLERS_CHILDLIST_TABTABLEMODELS, tableModel);
				addTab(title, tableModel);
				tableModel.setRowData(list);
			}
		}
		
		private static class Tab {

			final String category;
			final int size;

			Tab(String category, int size) {
				this.category = category;
				this.size = size;
			}
			
			String getTabTitle(AddonCategories addonCategories, Language language) {
				String categoryLabel = category==null ? "All" : AddonCategories.getCategoryLabel(category, addonCategories, language);
				return String.format("%s [%d]", categoryLabel, size);
			}
		}
	}

	private static class TrucksTablePanel extends JSplitPane implements Finalizable {
		private static final long serialVersionUID = 6564351588107715699L;
		
		private Data data;
		private final Finalizer finalizer;

		TrucksTablePanel(Window mainWindow, Controllers controllers, SpecialTruckAddons specialTruckAddOns, UserDefinedValues userDefinedValues) {
			super(JSplitPane.VERTICAL_SPLIT, true);
			setResizeWeight(1);
			finalizer = controllers.createNewFinalizer();
			
			data = null;
			finalizer.addDataReceiver(data->{
				this.data = data;
			});
			
			TruckPanelProto truckPanelProto = new TruckPanelProto(mainWindow, controllers, specialTruckAddOns);
			finalizer.addSubComp(truckPanelProto);
			JTabbedPane tabbedPaneFromTruckPanel = truckPanelProto.createTabbedPane();
			tabbedPaneFromTruckPanel.setBorder(BorderFactory.createTitledBorder("Selected Truck"));

			TruckTableModel truckTableModel = new TruckTableModel(mainWindow, controllers, specialTruckAddOns, userDefinedValues);
			finalizer.addSubComp(truckTableModel);
			JComponent truckTableScrollPane = TableSimplifier.create(
					truckTableModel,
					(TableSimplifier.ArbitraryOutputSource) rowIndex -> truckPanelProto.setTruck(truckTableModel.getRow(rowIndex),data));
			
			setTopComponent(truckTableScrollPane);
			setBottomComponent(tabbedPaneFromTruckPanel);
		}

		@Override public void prepareRemovingFromGUI() {
			finalizer.removeSubCompsAndListenersFromGUI();
		}
	}

	private static class TrucksListPanel extends JSplitPane implements Finalizable {
		private static final long serialVersionUID = 7004081774916835136L;
		
		private final JList<Truck> truckList;
		private final Finalizer finalizer;
		private final StandardMainWindow mainWindow;
		private HashMap<String, String> truckToDLCAssignments;
		private Data data;
		
		TrucksListPanel(StandardMainWindow mainWindow, Controllers controllers, SpecialTruckAddons specialTruckAddOns) {
			super(JSplitPane.HORIZONTAL_SPLIT);
			this.mainWindow = mainWindow;
			this.finalizer = controllers.createNewFinalizer();
			this.truckToDLCAssignments = null;
			this.data = null;
			setResizeWeight(0);
			
			TruckPanelProto truckPanelProto = new TruckPanelProto(this.mainWindow, controllers, specialTruckAddOns);
			finalizer.addSubComp(truckPanelProto);
			JSplitPane splitPaneFromTruckPanel = truckPanelProto.createSplitPane();
			splitPaneFromTruckPanel.setBorder(BorderFactory.createTitledBorder("Selected Truck"));
			
			JScrollPane truckListScrollPane = new JScrollPane(truckList = new JList<>());
			truckListScrollPane.setBorder(BorderFactory.createTitledBorder("All Trucks in Game"));
			truckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			truckList.addListSelectionListener(e->truckPanelProto.setTruck(truckList.getSelectedValue(),data));
			finalizer.addDataReceiver(data -> {
				this.data = data;
				Vector<Truck> items = new Vector<>(data.trucks.values());
				items.sort(Comparator.<Truck,String>comparing(Truck->Truck.id));
				truckList.setListData(items);
			});
			
			TruckListContextMenu truckListContextMenu = new TruckListContextMenu();
			finalizer.addLanguageListener(truckListContextMenu);
			
			TruckListCellRenderer truckListCellRenderer = new TruckListCellRenderer(truckList);
			truckList.setCellRenderer(truckListCellRenderer);
			finalizer.addLanguageListener(truckListCellRenderer);
			finalizer.addSaveGameListener(truckListCellRenderer);
			
			setLeftComponent(truckListScrollPane);
			setRightComponent(splitPaneFromTruckPanel);
			
			finalizer.addTruckToDLCAssignmentListener(new TruckToDLCAssignmentListener() {
				@Override public void updateAfterAssignmentsChange() {}
				@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
					TrucksListPanel.this.truckToDLCAssignments = truckToDLCAssignments;
				}
			});
		}
		
		@Override public void prepareRemovingFromGUI() {
			finalizer.removeSubCompsAndListenersFromGUI();
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
						finalizer.getControllers().truckToDLCAssignmentListeners.updateAfterAssignmentsChange();
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
	
		private static class TruckListCellRenderer implements ListCellRenderer<Truck>, LanguageListener, SaveGameListener {
			
			private static final Color COLOR_FG_DLCTRUCK   = new Color(0x0070FF);
			private static final Color COLOR_FG_OWNEDTRUCK = new Color(0x00AB00);
			private final Tables.LabelRendererComponent rendererComp;
			private final JList<Truck> truckList;
			private Language language;
			private SaveGame saveGame;
		
			TruckListCellRenderer(JList<Truck> truckList) {
				this.truckList = truckList;
				rendererComp = new Tables.LabelRendererComponent();
				language = null;
				saveGame = null;
			}
			
			@Override public void setLanguage(Language language) {
				this.language = language;
				truckList.repaint();
			}
			
			@Override public void setSaveGame(SaveGame saveGame) {
				this.saveGame = saveGame;
				truckList.repaint();
			}

			@Override
			public Component getListCellRendererComponent( JList<? extends Truck> list, Truck value, int index, boolean isSelected, boolean cellHasFocus) {
				rendererComp.configureAsListCellRendererComponent(list, null, getTruckLabel(value, language), index, isSelected, cellHasFocus, null, ()->getForeground(value));
				return rendererComp;
			}
		
			private Color getForeground(Truck truck) {
				if (truck!=null) {
					if (saveGame!=null && saveGame.playerOwnsTruck(truck))
						return COLOR_FG_OWNEDTRUCK;
					if (truck.dlcName!=null)
						return COLOR_FG_DLCTRUCK;
				}
				return null;
			}
		
		}
		
	}

	public static class Controllers {
		
		public final LanguageListenerController languageListeners;
		public final DataReceiverController dataReceivers;
		public final SaveGameListenerController saveGameListeners;
		public final TruckToDLCAssignmentListenerController truckToDLCAssignmentListeners;
		public final SpecialTruckAddOnsController specialTruckAddonsListeners;
		public final FilterTrucksByTrailersController filterTrucksByTrailersListeners;
		
		Controllers() {
			languageListeners               = new LanguageListenerController();
			dataReceivers                   = new DataReceiverController();
			saveGameListeners               = new SaveGameListenerController();
			truckToDLCAssignmentListeners   = new TruckToDLCAssignmentListenerController();
			specialTruckAddonsListeners     = new SpecialTruckAddOnsController();
			filterTrucksByTrailersListeners = new FilterTrucksByTrailersController();
		}
		
		void showListeners() {
			String indent = "    ";
			
			System.out.printf("Current State of Listeners:%n");
			languageListeners              .showListeners(indent, "LanguageListeners"              );
			dataReceivers                  .showListeners(indent, "DataReceivers"                  );
			saveGameListeners              .showListeners(indent, "SaveGameListeners"              );
			truckToDLCAssignmentListeners  .showListeners(indent, "TruckToDLCAssignmentListeners"  );
			specialTruckAddonsListeners    .showListeners(indent, "SpecialTruckAddOnsListeners"    );
			filterTrucksByTrailersListeners.showListeners(indent, "FilterTrucksByTrailersListeners");
		}
		
		public interface Finalizable {
			void prepareRemovingFromGUI();
		}
		
		public Finalizer createNewFinalizer() {
			return new Finalizer();
		}
		
		public class Finalizer {
			
			private final Vector<Finalizable> subComps = new Vector<>();
			private final StringVectorMap<Finalizable> volatileSubComps = new StringVectorMap<>();
			
			public Controllers getControllers() {
				return Controllers.this;
			}

			public <T extends Finalizable> T addSubComp(T subComp) {
				subComps.add(subComp);
				return subComp;
			}
			
			public <T extends Finalizable> T addVolatileSubComp(String listID, T subComp) {
				volatileSubComps.add(listID, subComp);
				return subComp;
			}
			
			public void removeSubCompsAndListenersFromGUI() {
				for (Finalizable subComp : subComps)
					subComp.prepareRemovingFromGUI();
				subComps.clear();
				
				for (String listID : volatileSubComps.keySet())
					removeVolatileSubCompsFromGUI(listID);
				volatileSubComps.clear();
				
				languageListeners              .removeListenersOfSource(this);
				dataReceivers                  .removeListenersOfSource(this);
				saveGameListeners              .removeListenersOfSource(this);
				truckToDLCAssignmentListeners  .removeListenersOfSource(this);
				specialTruckAddonsListeners    .removeListenersOfSource(this);
				filterTrucksByTrailersListeners.removeListenersOfSource(this);
			}
			
			public void removeVolatileSubCompsFromGUI(String listID) {
				Vector<Finalizable> list = volatileSubComps.get(listID);
				if (list==null) return;
				for (Finalizable subComp : list)
					subComp.prepareRemovingFromGUI();
				volatileSubComps.remove(listID);
			}

			public void addLanguageListener              (LanguageListener l                               ) { languageListeners              .add(this,l); }
			public void addDataReceiver                  (DataReceiver l                                   ) { dataReceivers                  .add(this,l); }
			public void addSaveGameListener              (SaveGameListener l                               ) { saveGameListeners              .add(this,l); }
			public void addTruckToDLCAssignmentListener  (TruckToDLCAssignmentListener l                   ) { truckToDLCAssignmentListeners  .add(this,l); }
			public void addSpecialTruckAddonsListener    (SpecialTruckAddons.Listener l                    ) { specialTruckAddonsListeners    .add(this,l); }
			public void addFilterTrucksByTrailersListener(TruckTableModel.FilterTrucksByTrailersListener l ) { filterTrucksByTrailersListeners.add(this,l); }
		}

		private static class AbstractController<Listener> {
			protected final Vector<Listener> listeners = new Vector<>();
			private final VectorMap<Finalizer, Listener> listenersOfSource = new VectorMap<>();
			
			public void add(Finalizer source, Listener l) {
				listenersOfSource.add(source, l);
				listeners.add(l);
			}
			
			void removeListenersOfSource(Finalizer source) {
				Vector<Listener> list = listenersOfSource.remove(source);
				if (list==null) return;
				listeners.removeAll(list);
			}

			void showListeners(String indent, String label) {
				
				System.out.printf("%s%s.Array: [%d]%n", indent, label, listeners.size());
				for (Listener l : listeners)
					System.out.printf("%s    %s%n", indent, l);
				
				System.out.printf("%s%s.Sources: [%d]%n", indent, label, listenersOfSource.size());
				listenersOfSource.forEach((source,list)->{
					System.out.printf("%s    %s: [%d]%n", indent, source, list.size());
					for (Listener l : list)
						System.out.printf("%s        %s%n", indent, l);
				});
			}
		}
		
		public static class LanguageListenerController extends AbstractController<LanguageListener> implements LanguageListener {
			@Override public void setLanguage(Language language) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setLanguage(language);
			}
		}

		public static class DataReceiverController extends AbstractController<DataReceiver> implements DataReceiver {
			@Override public void setData(Data data) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setData(data);
			}
		}

		public static class SaveGameListenerController extends AbstractController<SaveGameListener> implements SaveGameListener {
			@Override public void setSaveGame(SaveGame saveGame) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setSaveGame(saveGame);
			}
		}

		public static class SpecialTruckAddOnsController extends AbstractController<SpecialTruckAddons.Listener> implements SpecialTruckAddons.Listener {
			@Override public void specialTruckAddOnsHaveChanged(SpecialTruckAddons.AddonCategory list, SpecialTruckAddons.Change change) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).specialTruckAddOnsHaveChanged(list, change);
			}
		}

		public static class TruckToDLCAssignmentListenerController extends AbstractController<TruckToDLCAssignmentListener> implements TruckToDLCAssignmentListener {
			
			@Override public void setTruckToDLCAssignments(HashMap<String, String> assignments) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setTruckToDLCAssignments(assignments);
			}
			@Override public void updateAfterAssignmentsChange() {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).updateAfterAssignmentsChange();
			}
		}

		public static class FilterTrucksByTrailersController extends AbstractController<TruckTableModel.FilterTrucksByTrailersListener> implements TruckTableModel.FilterTrucksByTrailersListener
		{
			@Override public void setFilter(Trailer trailer)
			{
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).setFilter(trailer);
			}
		}
	}

	public static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		public enum ValueKey {
			SteamLibraryFolder, Language, InitialPAK, SaveGameFolder,
			SelectedSaveGame, ShowingSaveGameDataSorted, XML_HideKnownBugs,
			
			TruckTableModel_enableOwnedTrucksHighlighting,
			TruckTableModel_enableDLCTrucksHighlighting,
			TruckAddonsTableModel_enableSpecialTruckAddonsHighlighting,
		}

		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		public AppSettings() {
			super(SnowRunner.class, ValueKey.values());
		}
	}

	public static <Type> Type[] mergeArrays(Type[] arr1, boolean addArr1BeforeArr2, Type[] arr2) {
		if (arr1==null || arr1.length==0) return arr2;
		if (arr2==null || arr2.length==0) return arr1;
		
		if (!addArr1BeforeArr2) {
			Type[] temp = arr1;
			arr1 = arr2;
			arr2 = temp;
		}
		
		Type[] result = Arrays.copyOf(arr1, arr1.length+arr2.length);
		for (int i=0; i<arr2.length; i++)
			result[i+arr1.length] = arr2[i];
		return result;
	}
}
