package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
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
import javax.swing.JScrollBar;
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
import net.schwarzbaer.gui.ImageViewDialog;
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
import net.schwarzbaer.java.games.snowrunner.tables.SaveGameDataPanel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.EnginesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.GearboxesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.SuspensionsTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.WinchesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier;
import net.schwarzbaer.java.games.snowrunner.tables.TrailersTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TruckAddonsTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TruckPanelProto;
import net.schwarzbaer.java.games.snowrunner.tables.TruckTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.WheelsTableModel;
import net.schwarzbaer.system.DateTimeFormatter;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	public static final String DLCAssignmentsFile      = "SnowRunner - DLCAssignments.dat";
	public static final String UserDefinedValuesFile   = "SnowRunner - UserDefinedValues.dat";
	public static final String FilterRowsPresetsFile   = "SnowRunner - FilterRowsPresets.dat";
	public static final String ColumnHidePresetsFile   = "SnowRunner - ColumnHidePresets.dat";
	public static final String RowColoringsFile        = "SnowRunner - RowColorings.dat";
	public static final String SpecialTruckAddonsFile  = "SnowRunner - SpecialTruckAddons.dat";
	public static final String TruckImagesFile         = "SnowRunner - TruckImages.zip";
	public static final String WheelsQualityRangesFile = "SnowRunner - WheelsQualityRanges.dat";
	
	public static final Color COLOR_FG_DLCTRUCK    = new Color(0x0070FF);
	public static final Color COLOR_FG_OWNEDTRUCK  = new Color(0x00AB00);
	public static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatter();
	
	public static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}
	
	public static class GlobalFinalDataStructures
	{
		public final Controllers controllers;
		public final UserDefinedValues userDefinedValues;
		public final DLCs dlcs;
		public final TruckImages truckImages;
		public final SpecialTruckAddons specialTruckAddons;
		public final WheelsQualityRanges wheelsQualityRanges;
		
		private GlobalFinalDataStructures()
		{
			controllers = new Controllers();
			userDefinedValues = new UserDefinedValues();
			dlcs = new DLCs();
			truckImages = new TruckImages();
			specialTruckAddons = new SpecialTruckAddons(controllers.specialTruckAddonsListeners);
			wheelsQualityRanges = new WheelsQualityRanges(controllers.wheelsQualityRangesListeners);
		}

		private void initialize()
		{
			truckImages.read();
			userDefinedValues.read();
			specialTruckAddons.readFromFile();
			dlcs.loadStoredData();
			wheelsQualityRanges.readFile();
		}
	}

	
	private Data data;
	private SaveGameData saveGameData;
	private SaveGame selectedSaveGame;
	private File loadedInitialPAK;
	private final StandardMainWindow mainWindow;
	private final JMenu languageMenu;
	private final RawDataPanel rawDataPanel;
	private final JMenuItem miSGValuesSorted;
	private final JMenuItem miSGValuesOriginal;
	private final JMenu selectedSaveGameMenu;
	private final GlobalFinalDataStructures gfds;
	private final ImageDialogController truckImageDialogController;
	
	SnowRunner() {
		data = null;
		saveGameData = null;
		selectedSaveGame = null;
		loadedInitialPAK = null;
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		gfds = new GlobalFinalDataStructures();
		Finalizer fin = gfds.controllers.createNewFinalizer();
		
		fin.addSpecialTruckAddonsListener((list, change) -> gfds.specialTruckAddons.writeToFile());
		
		truckImageDialogController = new ImageDialogController(mainWindow, "Truck Image", 400,400);
		truckImageDialogController.registerDialogAsExtraWindow(
				AppSettings.ValueKey.TruckImageDialog_WindowX,
				AppSettings.ValueKey.TruckImageDialog_WindowY,
				AppSettings.ValueKey.TruckImageDialog_WindowWidth,
				AppSettings.ValueKey.TruckImageDialog_WindowHeight
		);
		
		rawDataPanel = new RawDataPanel(mainWindow, gfds);
		
		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.addTab("Trucks"          ,                        fin.addSubComp(new TrucksTablePanel         (mainWindow, gfds, truckImageDialogController)));
		contentPane.addTab("Trucks (old)"    ,                        fin.addSubComp(new TrucksListPanel          (mainWindow, gfds, truckImageDialogController)));
		contentPane.addTab("Wheels"          , TableSimplifier.create(fin.addSubComp(new WheelsTableModel         (mainWindow, gfds))));
		contentPane.addTab("DLCs"            , TableSimplifier.create(fin.addSubComp(new DLCTableModel            (            gfds))));
		contentPane.addTab("Trailers"        , TableSimplifier.create(fin.addSubComp(new TrailersTableModel       (mainWindow, gfds, true))));
		contentPane.addTab("Truck Addons"    ,                        fin.addSubComp(new TruckAddonsTablePanel    (mainWindow, gfds)));
		contentPane.addTab("Engines"         , TableSimplifier.create(fin.addSubComp(new EnginesTableModel        (mainWindow, gfds, true, null))));
		contentPane.addTab("Gearboxes"       , TableSimplifier.create(fin.addSubComp(new GearboxesTableModel      (mainWindow, gfds, true, null))));
		contentPane.addTab("Suspensions"     , TableSimplifier.create(fin.addSubComp(new SuspensionsTableModel    (mainWindow, gfds, true, null))));
		contentPane.addTab("Winches"         , TableSimplifier.create(fin.addSubComp(new WinchesTableModel        (mainWindow, gfds, true, null))));
		contentPane.addTab("Addon Categories", TableSimplifier.create(fin.addSubComp(new AddonCategoriesTableModel(mainWindow, gfds))));
		contentPane.addTab("SaveGame"        ,                        fin.addSubComp(new SaveGameDataPanel        (mainWindow, gfds)));
		contentPane.addTab("Raw Data"        ,                        fin.addSubComp(rawDataPanel));
		
		
		JMenuBar menuBar = new JMenuBar();
		
		JMenu baseDataMenu = menuBar.add(new JMenu("Base Data"));
		baseDataMenu.add(createMenuItem("Switch to another \"initial.pak\"", true, e->{
			File initialPAK = selectInitialPAK();
			boolean changed = loadInitialPAK(initialPAK);
			if (changed) updateAfterDataChange();
		}));
		baseDataMenu.add(createMenuItem("Reload \"initial.pak\"", true, e->{
			boolean changed = loadInitialPAK();
			if (changed) updateAfterDataChange();
		}));
		baseDataMenu.add(createCheckBoxMenuItem("Hide Known Bugs", KnownBugs.getInstance().isHideKnownBugs(), null, true, KnownBugs.getInstance()::setHideKnownBugs));
		//fileMenu.add(createMenuItem("Reset application settings", true, e->{
		//	for (AppSettings.ValueKey key:AppSettings.ValueKey.values())
		//		settings.remove(key);
		//}));
		//fileMenu.add(createMenuItem("Test XMLTemplateStructure", true, e->{
		//	boolean changed = testXMLTemplateStructure();
		//	if (changed) updateAfterDataChange();
		//}));
		
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
		
		boolean writeCalculationDetailsinToConsole = settings.getBool(AppSettings.ValueKey.Tables_WriteCalculationDetailsToConsole, true);
		JMenu optionsMenu = menuBar.add(new JMenu("Options"));
		optionsMenu.add(createMenuItem("Show Truck Images in separate window", true, e->{
			truckImageDialogController.showDialog();
		}));
		optionsMenu.add(createCheckBoxMenuItem("Write Calculation Details to Console", writeCalculationDetailsinToConsole, null, true, b->{
			settings.putBool(AppSettings.ValueKey.Tables_WriteCalculationDetailsToConsole, b);
		}));
		optionsMenu.addSeparator();
		optionsMenu.add(languageMenu = new JMenu("Language"));
		
		JMenu testingMenu = menuBar.add(new JMenu("Debug"));
		testingMenu.add(createMenuItem("Show Event Listeners", true, e->{
			gfds.controllers.showListeners();
		}));
		
		mainWindow.setIconImagesFromResource("/images/AppIcons/AppIcon","016.png","024.png","032.png","040.png","048.png","056.png","064.png","128.png","256.png");
		mainWindow.startGUI(contentPane, menuBar);
		
		settings.registerAppWindow(mainWindow);
	}
	
	private void initialize() {
		gfds.initialize();
		
		if (loadInitialPAK()) updateAfterDataChange();
		reloadSaveGameData();
		
	}

	private void reloadSaveGameData() {
		File saveGameFolder = getSaveGameFolder();
		if (saveGameFolder==null) return;
		
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
		
		updateAfterSaveGameChange();
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
				loadedInitialPAK = initialPAK;
				return true;
			}
		}
		loadedInitialPAK = null;
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
	
	private void updateWindowTitle() {
		String title = "SnowRunner Tool";
		if (selectedSaveGame!=null)
			title += "   -   "+selectedSaveGame.fileName;
		if (loadedInitialPAK!=null)
			title += "   -   "+loadedInitialPAK.getAbsolutePath();
		mainWindow.setTitle(title);
	}

	public interface SaveGameListener {
		void setSaveGame(SaveGame saveGame);
	}
	
	private void updateAfterSaveGameChange() {
		gfds.controllers.saveGameListeners.setSaveGame(selectedSaveGame);
		updateWindowTitle();
	}

	public interface DataReceiver {
		void setData(Data data);
	}
	
	private void updateAfterDataChange() {
		gfds.controllers.dataReceivers.setData(data);
		
		Vector<String> langs = new Vector<>(data.languages.keySet());
		langs.sort(null);
		String currentLangID = settings.getString(AppSettings.ValueKey.Language, null);
		
		ButtonGroup bg = new ButtonGroup();
		languageMenu.removeAll();
		for (String langID:langs)
			languageMenu.add(createCheckBoxMenuItem(langID, langID.equals(currentLangID), bg, true, ()->setLanguage(langID)));
		
		setLanguage(currentLangID);
		
		EnumSet<SpecialTruckAddons.AddonCategory> idsOfEmptyLists = EnumSet.noneOf(SpecialTruckAddons.AddonCategory.class);
		gfds.specialTruckAddons.foreachList((listID,list)->{
			int count = list.findIn(data.truckAddons);
			if (count==0)
				idsOfEmptyLists.add(listID);
		});
		
		if (!idsOfEmptyLists.isEmpty()) {
			Iterable<String> it = ()->idsOfEmptyLists.stream().<String>map(listID->listID.label.toLowerCase()).sorted().iterator();
			String missingObjects = String.join(" or ", it);
			String message = wrapWords(65, "Their are no special TruckAddons like " + missingObjects + " defined or found in loaded data. Please select these via context menu in any TruckAddon table.");
			JOptionPane.showMessageDialog(mainWindow, message, "Please define special TruckAddons", JOptionPane.INFORMATION_MESSAGE);
		}
		
		updateWindowTitle();
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
		
		if (language!=null)
			language.scanRegionNames(false);
		
		gfds.controllers.languageListeners.setLanguage(language);
	}

	public static String getTruckLabel(Truck truck, Language language) {
		return solveStringID_nonNull(truck, language);
	}

	public static String solveStringID_nonNull(Data.HasNameAndID namedToken, Language language) {
		return namedToken==null ? "<null>" : solveStringID(namedToken, language, "<null>");
	}
	
	public static String solveStringID(Data.HasNameAndID namedToken, Language language) {
		return solveStringID(namedToken, language, null);
	}
	
	private static String solveStringID(Data.HasNameAndID namedToken, Language language, String defaultStrIfIdAndNameStringIdIsNull) {
		if (namedToken==null) return null;
		String id = namedToken.getID();
		return solveStringID(namedToken, id, language, defaultStrIfIdAndNameStringIdIsNull);
	}

	public static String solveStringID(Data.HasNameAndID namedToken, String id, Language language) {
		return solveStringID(namedToken, id, language, null);
	}

	private static String solveStringID(Data.HasNameAndID namedToken, String id, Language language, String defaultStrIfIdAndNameStringIdIsNull) {
		String name_StringID = namedToken==null ? null : namedToken.getName_StringID();
		String defaultStr = id!=null ? "<"+id+">" : name_StringID!=null ? "<"+name_StringID+">" : defaultStrIfIdAndNameStringIdIsNull;
		return solveStringID(name_StringID, language, defaultStr);
	}
	
	public static String solveStringID(String strID, Language language) {
		if (strID==null) return null;
		return solveStringID(strID, language, "<"+strID+">");
	}
	
	public static String solveStringID(String strID, Language language, String defaultStr) {
		String str = language==null ? null : language.get(strID);
		if (str != null) return str;
		return defaultStr;
	}
	
	public static String solveStringID_Null(Data.HasNameAndID namedToken, Language language) {
		if (language==null) return null;
		if (namedToken==null) return null;
		String name_StringID = namedToken.getName_StringID();
		if (name_StringID==null) return null;
		return language.get(name_StringID);
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

	public static String[][] getNamesFromIDs(String[][] idLists, Function<String, String> getName_StringID, Language language) {
		String[][] names = new String[idLists.length][];
		for (int i=0; i<names.length; i++)
			names[i] = getNamesFromIDs(idLists[i],getName_StringID,language);
		return names;
	}

	public static String[] getNamesFromIDs(Collection<String> idList, Function<String,String> getName_StringID, Language language, boolean sorted) {
		if (getName_StringID==null)
			return idList.toArray(String[]::new);
		
		Stream<String> stream = idList.stream().map(id->getNameFromID(id, getName_StringID, language));
		if (sorted) stream = stream.sorted();
		return stream.toArray(String[]::new);
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

	public static String joinTruckAddonNames(Collection<TruckAddon> list, AddonCategories addonCategories, Language language) {
		Function<TruckAddon, String> mapper = addon->{
			String name = solveStringID(addon, language);
			String categoryLabel = AddonCategories.getCategoryLabel(addon.gameData.category,addonCategories,language);
			return String.format("[%s] \"%s\"", categoryLabel, name);
		};
		return String.join(", ", (Iterable<String>)()->list.stream().map(mapper).sorted().iterator());
	}
	
	public static String joinNames(Collection<? extends Data.HasNameAndID> list, Language language) {
		return String.join(", ", (Iterable<String>)()->list.stream().map(item->solveStringID(item, language)).sorted().iterator());
	}
	
	public static String joinStringIDs(Collection<String> strIDs, Language language) {
		return String.join(", ", (Iterable<String>)()->strIDs.stream().map(strID->solveStringID(strID, language)).sorted().iterator());
	}

	public static String getIdAndName(String id, String stringID, Language lang) {
		if (id!=null && stringID!=null)
			return String.format("[%s] %s", id, solveStringID(stringID, lang));
		if (id!=null)
			return String.format("[%s]", id);
		if (stringID!=null)
			return solveStringID(stringID, lang);
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

	private static class Pair<V1,V2> {
		final V1 v1;
		final V2 v2;
		Pair(V1 v1, V2 v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
		static Pair<Truck,String> create(Truck truck, Language language) {
			return new Pair<>(truck, solveStringID(truck, language));
		}
	}

	public static void writeTruckNamesToDoc(StyledDocumentInterface doc, Color ownedTruckColor, Collection<Truck> list, Language language, SaveGame saveGame) {
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
	
	public static <Type> void forEach(Iterable<Type> array, BiConsumer<Boolean, Type> action)
	{
		boolean isFirst = true;
		for (Type value : array)
		{
			action.accept(isFirst, value);
			isFirst = false;
		}
	}
	
	public static <Type> void forEach(Type[] array, BiConsumer<Boolean, Type> action)
	{
		boolean isFirst = true;
		for (Type value : array)
		{
			action.accept(isFirst, value);
			isFirst = false;
		}
	}

	public static void writeInstallSocketIssuesToDoc(StyledDocumentInterface doc, Color operatorColor, String installSocket, Truck.AddonSockets[] addonSockets, String indent, boolean addNewLineFirst)
	{
		boolean isFirst = true;
		if (installSocket!=null && addonSockets!=null)
			for (Truck.AddonSockets group : addonSockets)
				for (Truck.AddonSockets.Socket socket : group.sockets)
				{
					if (Data.contains(socket.socketIDs, installSocket))
					{
						Vector<String[]> blockCombis = socket.isBlockedBy.get(installSocket);
						if (blockCombis!=null && !blockCombis.isEmpty())
						{
							if (!isFirst || addNewLineFirst) doc.append("%n");
							isFirst = false;
							
							doc.append(StyledDocumentInterface.Style.BOLD, "Is Blocked");
							
							boolean isFirstCombi = true;
							Vector<String> singleCombis = new Vector<>();
							for (String[] blockCombi : blockCombis)
								if (blockCombi.length==1)
									singleCombis.add(blockCombi[0]);
								else
								{
									writeIdCombi(doc, operatorColor, indent, blockCombi, "and", isFirstCombi);
									isFirstCombi = false;
								}
							
							if (!singleCombis.isEmpty())
							{
								writeIdCombi(doc, operatorColor, indent, singleCombis.toArray(String[]::new), "or", isFirstCombi);
								isFirstCombi = false;
							}
						}
						
						if (!socket.isShiftedBy.isEmpty())
						{
							for (Truck.AddonSockets.Socket.AddonsShift as : socket.isShiftedBy)
							{
								if (!isFirst || addNewLineFirst) doc.append("%n");
								isFirst = false;
								doc.append(StyledDocumentInterface.Style.BOLD,"Is Shifted");
								doc.append(operatorColor, "%n%sby offset ", indent);
								doc.append(as.offset());
								writeIdCombi(doc, operatorColor, indent, as.types(), "and", null);
							}
						}
					}
				}
	}

	private static void writeIdCombi(StyledDocumentInterface doc, Color operatorColor, String indent, String[] ids, String operator, Boolean isFirst)
	{
		doc.append(operatorColor, "%n%s", indent);
		if (isFirst!=null)
		{
			String orStr = "OR ";
			if (isFirst) orStr = "   ";
			doc.append(new StyledDocumentInterface.Style(operatorColor,"Monospaced"), orStr);
		}
		doc.append(operatorColor, "if ");
		forEach( ids, (isFirstStr, str) -> {
			if (!isFirstStr) doc.append(operatorColor, " %s ", operator);
			doc.append(str);
		});
		doc.append(operatorColor, " is installed");
	}

	public static String joinAddonIDs(String[] strs, boolean emptyAndNullReturnsNull) {
		if (strs==null    ) return emptyAndNullReturnsNull ? null : "<null>";
		if (strs.length==0) return emptyAndNullReturnsNull ? null : "[]";
		if (strs.length==1) return strs[0];
		return Arrays.toString(strs);
	}


	public static final Comparator<String> CATEGORY_ORDER = Comparator.<String>comparingInt(SnowRunner::getCategoryOrderIndex).thenComparing(Comparator.naturalOrder());
	public static final List<String> CATEGORY_ORDER_LIST = Arrays.asList("Trailers", "engine", "gearbox", "suspension", "winch", "awd", "diff_lock", "frame_addons");
	static int getCategoryOrderIndex(String category) {
		//return "frame_addons".equals(category) ? 0 : 1;
		int pos = CATEGORY_ORDER_LIST.indexOf(category);
		if (pos<0) return CATEGORY_ORDER_LIST.size();
		return pos;
	}

	public static <ValueType extends Comparable<ValueType>> Vector<ValueType> getSorted(Collection<ValueType> list)
	{
		Vector<ValueType> vector = new Vector<>(list);
		vector.sort(null);
		return vector;
	}
	
	public static <K,V> V getOrCreate(Map<K,V> map, K key, Supplier<V> createNewValue)
	{
		V value = map.get(key);
		if (value==null)
			map.put(key, value = createNewValue.get());
		return value;
	}

	public static void lineWrap(String text, int maxLineLength, Consumer<String> writeLine)
	{
		if (text==null) return;
		
		StringBuilder line = new StringBuilder();
		Iterable<String> lines = ()->text.lines().iterator();
		for (String block : lines)
		{
			if (block.isBlank())
				writeLine.accept("");
			else
			{
				String[] words = block.split("\\s", -1);
				line.setLength(0);
				for (String word : words)
				{
					if (word.isBlank())
						continue;
					
					// pre: line.length() < maxLineLength
					int newLineLength = line.length() +1+ word.length();
					
					if (newLineLength < maxLineLength) // normal case: add a word
					{
						if (!line.isEmpty()) line.append(" ");
						line.append(word);
						continue;
					}
					
					if (line.length() < maxLineLength*0.8)
					{
						if (
								newLineLength < maxLineLength*1.2 ||
								(line.length() < maxLineLength*0.3 && word.length() >= maxLineLength)
						)
						{
							if (!line.isEmpty()) line.append(" ");
							line.append(word);
							writeLine.accept(line.toString());
							line.setLength(0);
							continue;
						}
					}
					
					writeLine.accept(line.toString());
					line.setLength(0);
					
					if (word.length() >= maxLineLength)
						writeLine.accept(word);
					else
						line.append(word);
					continue;
				}
				if (!line.isEmpty())
					writeLine.accept(line.toString());
			}
		}
	}

	public static <EnumType> EnumType parseEnum(String str, Function<String,EnumType> parseEnum)
	{
		if (str==null) return null;
		try { return parseEnum.apply(str); }
		catch (Exception e) { return null; }
	}

	public static class Initializable
	{
		private boolean wasInitialized;
		
		protected Initializable()
		{
			wasInitialized = false;
		}
		protected void checkInitialized()
		{
			if (!wasInitialized)
				throw new IllegalStateException();
		}
		protected void setInitialized()
		{
			wasInitialized = true;
		}
	}
	
	public static class TruckImages extends Initializable
	{
		public interface Listener
		{
			void imageHasChanged(String truckID);
		}
		
		private final HashMap<String,BufferedImage> images = new HashMap<>();
		
		public BufferedImage get(String truckID)
		{
			checkInitialized();
			return images.get(truckID);
		}
		
		public void set(String truckID, BufferedImage image)
		{
			checkInitialized();
			images.put(truckID, image);
			write();
		}
		
		public boolean contains(String truckID)
		{
			checkInitialized();
			return images.containsKey(truckID);
		}
		
		private void read()
		{
			File file = new File(TruckImagesFile);
			if (!file.isFile()) return;
			
			System.out.printf("Read Truck Images from file \"%s\" ...%n", file.getAbsolutePath());
			
			images.clear();
			
			try (ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ); )
			{
				Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements())
				{
					ZipEntry zipEntry = entries.nextElement();
					String truckID = zipEntry.getName();
					try
					{
						BufferedImage image = ImageIO.read(zipFile.getInputStream(zipEntry));
						images.put(truckID, image);
					}
					catch (IOException ex)
					{
						System.err.printf("IOException while reading Truck Image \"%s\": %s%n", truckID, ex.getMessage());
						// ex.printStackTrace();
					}
				}
			}
			catch (IOException ex)
			{
				System.err.printf("IOException while reading Truck Images: %s%n", ex.getMessage());
				// ex.printStackTrace();
			}
			
			System.out.printf("... done%n");
			setInitialized();
		}
		
		private void write()
		{
			checkInitialized();
			File file = new File(TruckImagesFile);
			System.out.printf("Write Truck Images to file \"%s\" ...%n", file.getAbsolutePath());
			
			try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file)))
			{
				images.forEach((truckID, image) -> {
					try
					{
						zipOut.putNextEntry(new ZipEntry(truckID));
						ImageIO.write(image, "png", zipOut);
					}
					catch (IOException ex)
					{
						System.err.printf("IOException while writing Truck Image \"%s\": %s%n", truckID, ex.getMessage());
						// ex.printStackTrace();
					}
				}); 
			}
			catch (FileNotFoundException ex) { ex.printStackTrace(); }
			catch (IOException ex)
			{
				System.err.printf("IOException while writing Truck Images: %s%n", ex.getMessage());
				// ex.printStackTrace();
			}
			
			System.out.printf("... done%n");
		}
	}
	
	public static class DLCs extends Initializable
	{
		public interface Listener {
			void updateAfterChange();
		}
		
		public enum ItemType {
			Truck (dlcs->dlcs.trucks ),
			Region(dlcs->dlcs.regions),
			Map   (dlcs->dlcs.maps   );
			private Function<DLCs, HashMap<String,String>> getAssignments;
			private ItemType(Function<DLCs, HashMap<String,String>> getAssignments)
			{
				this.getAssignments = Objects.requireNonNull(getAssignments);
			}
		}
		
		private final HashMap<String,String> trucks  = new HashMap<>();
		private final HashMap<String,String> regions = new HashMap<>();
		private final HashMap<String,String> maps    = new HashMap<>();
		
		public Vector<String> getAllDLCs()
		{
			checkInitialized();
			HashSet<String> dlcSet = new HashSet<>();
			dlcSet.addAll(trucks .values());
			dlcSet.addAll(regions.values());
			dlcSet.addAll(maps   .values());
			
			Vector<String> sortedDLCs = new Vector<>(dlcSet);
			sortedDLCs.sort(null);
			
			return sortedDLCs;
		}
		public void               setDLC(String id, ItemType type, String dlc) { checkInitialized();        type.getAssignments.apply(this).put(id, dlc); }
		public String             getDLC(String id, ItemType type            ) { checkInitialized(); return type.getAssignments.apply(this).get(id     ); }
		public Collection<String> getIDs(           ItemType type            ) { checkInitialized(); return type.getAssignments.apply(this).keySet(); }
		
		private static HashMap<String,Vector<String>> getReversedMap(HashMap<String,String> assignments)
		{
			HashMap<String,Vector<String>> reversedMap = new HashMap<>();
			assignments.forEach((item,dlc)->{
				Vector<String> items = reversedMap.get(dlc);
				if (items==null) reversedMap.put(dlc, items = new Vector<>());
				items.add(item);
			});
			reversedMap.forEach((dlc,items)->items.sort(null));
			return reversedMap;
		}
		
		void saveData()
		{
			checkInitialized();
			
			File file = new File(DLCAssignmentsFile);
			System.out.printf("Write DLCs to file \"%s\" ...%n", file.getAbsolutePath());
			
			HashMap<String,Vector<String>> reversedTruckMap  = getReversedMap(trucks );
			HashMap<String,Vector<String>> reversedRegionMap = getReversedMap(regions);
			HashMap<String,Vector<String>> reversedMapMap    = getReversedMap(maps   );
			
			HashSet<String> dlcs = new HashSet<>();
			dlcs.addAll(reversedTruckMap .keySet());
			dlcs.addAll(reversedMapMap   .keySet());
			dlcs.addAll(reversedRegionMap.keySet());
			Vector<String> sortedDLCs = new Vector<>(dlcs);
			sortedDLCs.sort(null);
			
			try (PrintWriter out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file ), StandardCharsets.UTF_8) ))
			{
				for (String dlc : sortedDLCs)
				{
					out.printf("[DLC]%n");
					out.printf("name = %s%n", dlc);
					writeIDs(reversedTruckMap , dlc, out, "truck" );
					writeIDs(reversedRegionMap, dlc, out, "region");
					writeIDs(reversedMapMap   , dlc, out, "map"   );
					out.println();
				}
			}
			catch (FileNotFoundException e) {}
			
			System.out.printf("... done%n");
		}
		
		private void writeIDs(HashMap<String, Vector<String>> reversedMap, String dlc, PrintWriter out, String fieldName)
		{
			Vector<String> ids = reversedMap.get(dlc);
			if (ids!=null)
				for (String id : ids)
					out.printf("%s = %s%n", fieldName, id);
		}

		void loadStoredData() {
			File file = new File(DLCAssignmentsFile);
			System.out.printf("Read DLCs from file \"%s\" ...%n", file.getAbsolutePath());
			
			trucks .clear();
			regions.clear();
			maps   .clear();
			
			try (BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( file ), StandardCharsets.UTF_8) )) {
				
				String line, value, lastDLC=null;
				while ( (line=in.readLine())!=null ) {
					
					if (line.equals("[DLC]"))
						lastDLC = null;
					
					if ( (value = Data.getLineValue(line, "name = "))!=null )
						lastDLC = value;
					
					if ( (value = Data.getLineValue(line, "truck = " ))!=null && lastDLC!=null) trucks .put(value, lastDLC);
					if ( (value = Data.getLineValue(line, "region = "))!=null && lastDLC!=null) regions.put(value, lastDLC);
					if ( (value = Data.getLineValue(line, "map = "   ))!=null && lastDLC!=null) maps   .put(value, lastDLC);
				}
				
				
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
			} catch (IOException e) {
				System.err.printf("IOException while reading DLCs: %s%n", e.getMessage());
				//e.printStackTrace();
			}
			
			System.out.printf("... done%n");
			setInitialized();
		}
	}
	
	public static class SpecialTruckAddons extends Initializable
	{

		private static final String ValuePrefix = "$ ";
		private static final String ListIdClosingBracket = "]]";
		private static final String ListIDOpeningBracket = "[[";

		public interface Listener {
			void specialTruckAddOnsHaveChanged(AddonCategory category, Change change);
		}

		public enum Change { Added, Removed }
		public enum AddonType {
			Addon        (addon->!addon.gameData.isCargo),
			LoadAreaCargo(addon-> addon.gameData.isCargo),
			;
			public final Predicate<TruckAddon> isAllowed;
			AddonType(Predicate<TruckAddon> isAllowed) { this.isAllowed = isAllowed; }
		}
		
		public enum AddonCategory {
			MetalDetector   ("Metal Detector"     , AddonType.Addon),
			SeismicVibrator ("Seismic Vibrator"   , AddonType.Addon),
			LogLift         ("Log Lift"           , AddonType.Addon),
			MiniCrane       ("Mini Crane"         , AddonType.Addon),
			BigCrane        ("Big Crane"          , AddonType.Addon),
			ShortLogs       ("Short Logs"         , AddonType.LoadAreaCargo),
			MediumLogs      ("Medium Logs"        , AddonType.LoadAreaCargo),
			LongLogs        ("Long Logs"          , AddonType.LoadAreaCargo),
			MediumLogs_burnt("Medium Logs (burnt)", AddonType.LoadAreaCargo),
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
			setInitialized();
		}

		public void writeToFile() {
			checkInitialized();
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
			checkInitialized();
			for (AddonCategory category : AddonCategory.values())
				action.accept(category, lists.get(category));
		}

		public SpecialTruckAddonList getList(AddonCategory category) {
			checkInitialized();
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

			public <Result> Result forEachAddon(Supplier<Result> getInitial, BiFunction<Result,Result,Result> combine, Predicate<Result> isFinalResult, Function<String,Result> action) {
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
		private final GlobalFinalDataStructures gfds;
		private final Vector<Tab> tabs;
		private Data data;
		private Language language;
		private SaveGame saveGame;

		TruckAddonsTablePanel(Window mainWindow, GlobalFinalDataStructures gfds) {
			this.mainWindow = mainWindow;
			this.gfds = gfds;
			this.finalizer = gfds.controllers.createNewFinalizer();
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
			TruckAddonsTableModel tableModel = new TruckAddonsTableModel(mainWindow, gfds, false).set(data, saveGame);
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
				tableModel = new TruckAddonsTableModel(mainWindow, gfds, false).set(data, saveGame);
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

		TrucksTablePanel(Window mainWindow, GlobalFinalDataStructures gfds, ImageDialogController imageDialogController) {
			super(JSplitPane.VERTICAL_SPLIT, true);
			setResizeWeight(1);
			finalizer = gfds.controllers.createNewFinalizer();
			
			data = null;
			finalizer.addDataReceiver(data->{
				this.data = data;
			});
			
			TruckPanelProto truckPanelProto = new TruckPanelProto(mainWindow, gfds, imageDialogController);
			finalizer.addSubComp(truckPanelProto);
			JTabbedPane tabbedPaneFromTruckPanel = truckPanelProto.createTabbedPane();
			tabbedPaneFromTruckPanel.setBorder(BorderFactory.createTitledBorder("Selected Truck"));

			TruckTableModel truckTableModel = new TruckTableModel(mainWindow, gfds);
			finalizer.addSubComp(truckTableModel);
			JComponent truckTableScrollPane = TableSimplifier.create(
					truckTableModel,
					(TableSimplifier.UnspecificOutputSource) rowIndex -> truckPanelProto.setTruck(truckTableModel.getRow(rowIndex),data));
			
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
		private Data data;
		private final GlobalFinalDataStructures gfds;
		
		TrucksListPanel(StandardMainWindow mainWindow, GlobalFinalDataStructures gfds, ImageDialogController imageDialogController) {
			super(JSplitPane.HORIZONTAL_SPLIT);
			this.mainWindow = mainWindow;
			this.gfds = gfds;
			this.finalizer = this.gfds.controllers.createNewFinalizer();
			this.data = null;
			setResizeWeight(0);
			
			TruckPanelProto truckPanelProto = new TruckPanelProto(this.mainWindow, this.gfds, imageDialogController);
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
					AssignToDLCDialog dlg = new AssignToDLCDialog(
							mainWindow,
							DLCs.ItemType.Truck,
							clickedItem.id,
							getTruckLabel(clickedItem,language),
							gfds.dlcs
					);
					boolean assignmentsChanged = dlg.showDialog();
					if (assignmentsChanged)
						gfds.controllers.dlcListeners.updateAfterChange();
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
					if (truck.updateLevel!=null)
						return COLOR_FG_DLCTRUCK;
				}
				return null;
			}
		
		}
		
	}

	public static class Controllers {
		
		public final LanguageController languageListeners;
		public final DataReceiverController dataReceivers;
		public final SaveGameController saveGameListeners;
		public final DLCAssignmentController dlcListeners;
		public final SpecialTruckAddOnsController specialTruckAddonsListeners;
		public final FilterTrucksByTrailersController filterTrucksByTrailersListeners;
		public final TruckImagesController truckImagesListeners;
		public final WheelsQualityRangesController wheelsQualityRangesListeners;
		
		Controllers() {
			languageListeners               = new LanguageController();
			dataReceivers                   = new DataReceiverController();
			saveGameListeners               = new SaveGameController();
			dlcListeners                    = new DLCAssignmentController();
			specialTruckAddonsListeners     = new SpecialTruckAddOnsController();
			filterTrucksByTrailersListeners = new FilterTrucksByTrailersController();
			truckImagesListeners            = new TruckImagesController();
			wheelsQualityRangesListeners    = new WheelsQualityRangesController();
		}
		
		void showListeners() {
			String indent = "    ";
			
			System.out.printf("Current State of Listeners:%n");
			languageListeners              .showListeners(indent, "LanguageListeners"              );
			dataReceivers                  .showListeners(indent, "DataReceivers"                  );
			saveGameListeners              .showListeners(indent, "SaveGameListeners"              );
			dlcListeners                   .showListeners(indent, "DLCAssignmentListeners"         );
			specialTruckAddonsListeners    .showListeners(indent, "SpecialTruckAddOnsListeners"    );
			filterTrucksByTrailersListeners.showListeners(indent, "FilterTrucksByTrailersListeners");
			truckImagesListeners           .showListeners(indent, "TruckImagesListeners"           );
			wheelsQualityRangesListeners   .showListeners(indent, "WheelsQualityRangesListeners"   );
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
				dlcListeners                   .removeListenersOfSource(this);
				specialTruckAddonsListeners    .removeListenersOfSource(this);
				filterTrucksByTrailersListeners.removeListenersOfSource(this);
				truckImagesListeners           .removeListenersOfSource(this);
				wheelsQualityRangesListeners   .removeListenersOfSource(this);
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
			public void addDLCListener                   (DLCs.Listener l                                  ) { dlcListeners                   .add(this,l); }
			public void addSpecialTruckAddonsListener    (SpecialTruckAddons.Listener l                    ) { specialTruckAddonsListeners    .add(this,l); }
			public void addFilterTrucksByTrailersListener(TruckTableModel.FilterTrucksByTrailersListener l ) { filterTrucksByTrailersListeners.add(this,l); }
			public void addTruckImagesListener           (TruckImages.Listener l                           ) { truckImagesListeners           .add(this,l); }
			public void addWheelsQualityRangesListener   (WheelsQualityRanges.Listener l                   ) { wheelsQualityRangesListeners   .add(this,l); }
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
		
		public static class LanguageController extends AbstractController<LanguageListener> implements LanguageListener {
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

		public static class SaveGameController extends AbstractController<SaveGameListener> implements SaveGameListener {
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

		public static class TruckImagesController extends AbstractController<TruckImages.Listener> implements TruckImages.Listener {
			@Override public void imageHasChanged(String truckID) {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).imageHasChanged(truckID);
			}
		}

		public static class DLCAssignmentController extends AbstractController<DLCs.Listener> implements DLCs.Listener {
			@Override public void updateAfterChange() {
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).updateAfterChange();
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
		
		public static class WheelsQualityRangesController extends AbstractController<WheelsQualityRanges.Listener> implements WheelsQualityRanges.Listener {
			@Override public void valueChanged(String wheelsDefID, int indexInDef, WheelsQualityRanges.WheelValue wheelValue)
			{
				for (int i=0; i<listeners.size(); i++)
					listeners.get(i).valueChanged(wheelsDefID, indexInDef, wheelValue);
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
			Tables_WriteCalculationDetailsToConsole,
			
			Tables_CalcDetailsDialog_WindowX,
			Tables_CalcDetailsDialog_WindowY,
			Tables_CalcDetailsDialog_WindowWidth,
			Tables_CalcDetailsDialog_WindowHeight,
			TruckImageDialog_WindowX,
			TruckImageDialog_WindowY,
			TruckImageDialog_WindowWidth,
			TruckImageDialog_WindowHeight,
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
	
	public interface TextOutput
	{
		void printf(               String format, Object... values);
		void printf(Locale locale, String format, Object... values);
		
		public static class SystemOut implements TextOutput
		{
			@Override public void printf(               String format, Object... values) { System.out.printf(        format, values); }
			@Override public void printf(Locale locale, String format, Object... values) { System.out.printf(locale, format, values); }
		}
		
		public static class Collector implements TextOutput
		{
			private final StringBuilder sb = new StringBuilder();
			@Override public void printf(               String format, Object... values) { sb.append( String.format(        format, values) ); }
			@Override public void printf(Locale locale, String format, Object... values) { sb.append( String.format(locale, format, values) ); }
			@Override public String toString() { return sb.toString(); }
			          public void clear() { sb.setLength(0); }
		}
	}
	
	public record ScrollValues(int min, int max, int ext, int val) {
		
		static ScrollValues get(JScrollBar scrollBar)
		{
			int min = scrollBar.getMinimum();
			int max = scrollBar.getMaximum();
			int ext = scrollBar.getVisibleAmount();
			int val = scrollBar.getValue();
			return new ScrollValues(min, max, ext, val);
		}

		public static ScrollValues getVertical(JScrollPane scrollPane)
		{
			if (scrollPane==null) return null;
			return get(scrollPane.getVerticalScrollBar());
		}

		public void setVertical(JScrollPane scrollPane)
		{
			if (scrollPane==null) return;
			JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
			ScrollValues newValues = get(scrollBar);
			//System.err.printf("scroll pos: %s -> %s%n", this, newValues);
			
			if (val==0) // start of page -> start of page
				scrollBar.setValue(val);
			
			else if (val+ext >= max) // end of page -> end of page
				scrollBar.setValue(newValues.max-newValues.ext);
			
			else if (val+newValues.ext >= newValues.max) // old val > max -> end of page
				scrollBar.setValue(newValues.max-newValues.ext);
			
			else
				scrollBar.setValue(val);
		}

	}
	
	public static class ImageDialogController
	{
		private ImageViewDialog dialog;

		public ImageDialogController(Window mainWindow, String title, int width, int height)
		{
			dialog = new ImageViewDialog(mainWindow, null, title, width, height, null, false, true);
			dialog.setModalityType(ModalityType.MODELESS);
		}

		private void registerDialogAsExtraWindow(AppSettings.ValueKey windowX, AppSettings.ValueKey windowY, AppSettings.ValueKey windowWidth, AppSettings.ValueKey windowHeight)
		{
			settings.registerExtraWindow(dialog, windowX, windowY, windowWidth, windowHeight);
		}

		public void showDialog()
		{
			if (dialog.isVisible())
				return;
			
			dialog.setVisible(true);
		}

		public void setImage(BufferedImage image)
		{
			dialog.setImage(image);
			dialog.setZoom(1);
		}
		
	}

	public static class WheelsQualityRanges extends Initializable
	{
		public interface Listener
		{
			void valueChanged(String wheelsDefID, int indexInDef, WheelValue wheelValue);
		}
		
		public enum WheelValue {
			Highway, Offroad, Mud
		}
		
		public enum QualityValue {
			Bad, //("Schlecht"),
			Average, //("Durchschnittlich"),
			Good, //("Gut"),
			Excellent, //("Exzellent"),
			;
			private final String label;
			private QualityValue() { this(null); }
			private QualityValue(String label) { this.label = label; }
			@Override public String toString() { return label==null ? name() : label; }
			private static QualityValue parse(String str) { return parseEnum(str, QualityValue::valueOf); }
			private static String toString(QualityValue qv) { return qv==null ? "" : qv.name(); }
		}
		
		private final HashMap<DataMapIndex,QualityData> data;
		private final Controllers.WheelsQualityRangesController listeners;
		
		public WheelsQualityRanges(Controllers.WheelsQualityRangesController listeners)
		{
			this.listeners = listeners;
			data = new HashMap<>();
		}

		public QualityValue getQualityValue(String wheelsDefID, int indexInDef, String truckId, WheelValue wheelValue)
		{
			checkInitialized();
			if (wheelsDefID==null) throw new IllegalArgumentException();
			if (wheelValue ==null) throw new IllegalArgumentException();
			QualityData qualityData = data.get(new DataMapIndex(wheelsDefID, indexInDef));
			if (qualityData==null) return null;
			return qualityData.get(truckId, wheelValue);
		}

		public void setQualityValue(String wheelsDefID, int indexInDef, String truckId, WheelValue wheelValue, QualityValue qualityValue)
		{
			checkInitialized();
			if (wheelsDefID==null) throw new IllegalArgumentException();
			if (wheelValue ==null) throw new IllegalArgumentException();
			
			DataMapIndex index = new DataMapIndex(wheelsDefID, indexInDef);
			QualityData qualityData = getOrCreateQualityData(index);
			qualityData.set(truckId, wheelValue, qualityValue);
			
			listeners.valueChanged(wheelsDefID, indexInDef, wheelValue);
			writeFile();
		}
		
		private QualityData getOrCreateQualityData(DataMapIndex index)
		{
			return getOrCreate(data, index, ()->new QualityData());
		}

		private void readFile()
		{
			File file = new File(WheelsQualityRangesFile);
			System.out.printf("Read WheelsQualityRanges from file \"%s\" ...%n", file.getAbsolutePath());
			
			data.clear();
			
			try (BufferedReader in = new BufferedReader( new InputStreamReader( new FileInputStream( file ), StandardCharsets.UTF_8) ))
			{
				
				String line, value;
				DataMapIndex qdIndex = null;
				
				while ( (line=in.readLine())!=null )
				{
					if (line.equals("[Wheel]"))
						qdIndex = null;
					
					if ((value = Data.getLineValue(line, "Index = "))!=null)
					{
						if (qdIndex != null)
							qdIndex = null;
						else
						{
							String[] parts = value.split(":", -1);
							if (parts.length == 2)
							{
								String wheelsDefID = parts[0];
								Integer indexInDef = parseInt(parts[1]);
								qdIndex = new DataMapIndex(wheelsDefID, indexInDef);
							}
						}
					}
					
					if ((value = Data.getLineValue(line, "General = "))!=null && qdIndex!=null)
					{
						EnumMap<WheelValue, QualityValue> qvMap = stringToQvMap(value);
						if (qvMap!=null && !qvMap.isEmpty())
						{
							QualityData qualityData = getOrCreateQualityData(qdIndex);
							qualityData.generalValues.putAll(qvMap);
						}
					}
					
					if ((value = Data.getLineValue(line, "Truck = "))!=null && qdIndex!=null)
					{
						int pos = value.indexOf(":");
						if (pos>=0)
						{
							String truckID = value.substring(0, pos);
							String qvMapStr = value.substring(pos+1);
							QualityData qualityData = getOrCreateQualityData(qdIndex);
							EnumMap<WheelValue, QualityValue> qvMap = stringToQvMap(qvMapStr);
							EnumMap<WheelValue, QualityValue> expQvMap = qvMap; //expandQvMap(qvMap, qualityData.generalValues);
							if (expQvMap!=null && !expQvMap.isEmpty())
								qualityData.modifyTruckQvMap(truckID, map -> {
									map.putAll(expQvMap);
								});
						}
					}
				}
				
			}
			catch (FileNotFoundException e) {}
			catch (IOException e)
			{
				System.err.printf("IOException while reading WheelsQualityRanges: %s%n", e.getMessage());
				//e.printStackTrace();
			}
			
			System.out.printf("... done%n");
			setInitialized();
		}

		private void writeFile()
		{
			checkInitialized();
			
			File file = new File(WheelsQualityRangesFile);
			System.out.printf("Write WheelsQualityRanges to file \"%s\" ...%n", file.getAbsolutePath());
			
			try (PrintWriter out = new PrintWriter( new OutputStreamWriter( new FileOutputStream( file ), StandardCharsets.UTF_8) ))
			{
				
				for (DataMapIndex index : getSorted(data.keySet()))
				{
					QualityData qualityData = data.get(index);
					
					out.printf("[Wheel]%n");
					out.printf("Index = %s:%d%n", index.wheelsDefID, index.indexInDef);
					out.printf("General = %s%n", qvMapToString(qualityData.generalValues));
					
					for (String truckID : getSorted(qualityData.truckValues.keySet()))
					{
						EnumMap<WheelValue, QualityValue> qvMap = qualityData.truckValues.get(truckID);
						qvMap = reduceQvMap(qvMap, qualityData.generalValues);
						if (!qvMap.isEmpty())
							out.printf("Truck = %s:%s%n", truckID, qvMapToString(qvMap));
					}
					out.printf("%n");
				}
				
			}
			catch (FileNotFoundException e) {}
			
			System.out.printf("... done%n");
		}

		private static Integer parseInt(String str)
		{
			try { return Integer.parseInt(str); }
			catch (NumberFormatException e) { return null; }
		}

		private static String qvMapToString(EnumMap<WheelValue, QualityValue> values)
		{
			return String.format(
					"%s:%s:%s",
					QualityValue.toString(values.get(WheelValue.Highway)),
					QualityValue.toString(values.get(WheelValue.Offroad)),
					QualityValue.toString(values.get(WheelValue.Mud    ))
			);
		}

		private static EnumMap<WheelValue, QualityValue> stringToQvMap(String str)
		{
			if (str==null) return null;
			
			String[] parts = str.split(":", -1);
			if (parts.length != 3) return null;
			
			EnumMap<WheelValue, QualityValue> qvMap = new EnumMap<>(WheelValue.class);
			parseAndAdd(parts[0], WheelValue.Highway, qvMap);
			parseAndAdd(parts[1], WheelValue.Offroad, qvMap);
			parseAndAdd(parts[2], WheelValue.Mud    , qvMap);
			if (qvMap.isEmpty()) return null;
			return qvMap;
		}

		private static void parseAndAdd(String str, WheelValue wheelValue, EnumMap<WheelValue, QualityValue> qvMap)
		{
			QualityValue qualityValue = str.isEmpty() ? null : QualityValue.parse(str);
			if (qualityValue!=null) qvMap.put(wheelValue, qualityValue);
		}

		private static EnumMap<WheelValue, QualityValue> reduceQvMap(EnumMap<WheelValue, QualityValue> qvMap, EnumMap<WheelValue, QualityValue> generalValues)
		{
			if (qvMap==null) return null;
			return mergeMaps(qvMap, generalValues, (qv, generalQV) -> qv==generalQV ? null : qv);
		}

		@SuppressWarnings("unused")
		private static EnumMap<WheelValue, QualityValue> expandQvMap(EnumMap<WheelValue, QualityValue> qvMap, EnumMap<WheelValue, QualityValue> generalValues)
		{
			if (qvMap==null) return null;
			return mergeMaps(qvMap, generalValues, (qv, generalQV) -> qv==null ? generalQV : qv);
		}

		private static EnumMap<WheelValue, QualityValue> mergeMaps(EnumMap<WheelValue, QualityValue> map1, EnumMap<WheelValue, QualityValue> map2, BinaryOperator<QualityValue> mergeValues)
		{
			if (map1==null) throw new IllegalArgumentException();
			if (map2==null) throw new IllegalArgumentException();
			
			EnumMap<WheelValue, QualityValue> newMap = new EnumMap<>(WheelValue.class);
			for (WheelValue wv : WheelValue.values())
			{
				QualityValue value1 = map1.get(wv);
				QualityValue value2 = map2.get(wv);
				value1 = mergeValues.apply(value1, value2);
				if (value1!=null)
					newMap.put(wv, value1);
			}
			return newMap;
		}

		private record DataMapIndex(String wheelsDefID, int indexInDef) implements Comparable<DataMapIndex>
		{
			@Override public int compareTo(DataMapIndex other)
			{
				if (other==null) return -1;
				int n = this.wheelsDefID.compareTo(other.wheelsDefID);
				if (n!=0) return n;
				return this.indexInDef - other.indexInDef;
			}
		}

		private static class QualityData
		{
			private final EnumMap<WheelValue, QualityValue> generalValues;
			private final HashMap<String, EnumMap<WheelValue, QualityValue>> truckValues;
			
			private QualityData() {
				generalValues = new EnumMap<>(WheelValue.class);
				truckValues = new HashMap<>();
			}
			
			private QualityValue get(String truckId, WheelValue wheelValue)
			{
				if (truckId==null)
					return generalValues.get(wheelValue);
				
				EnumMap<WheelValue, QualityValue> map = truckValues.get(truckId);
				if (map==null)
					return generalValues.get(wheelValue);
				
				QualityValue qv = map.get(wheelValue);
				if (qv==null)
					return generalValues.get(wheelValue);
				
				return qv;
			}
		
			private void set(String truckId, WheelValue wheelValue, QualityValue qualityValue)
			{
				if (truckId==null)
				{
					if (qualityValue!=null)
						generalValues.put(wheelValue, qualityValue);
					else
						generalValues.remove(wheelValue);
				}
				else
				{
					if (!generalValues.containsKey(wheelValue))
					{
						if (qualityValue!=null)
						{
							generalValues.put(wheelValue, qualityValue);
							qualityValue = null;
						}
					}
					else
					{
						QualityValue generalQV = generalValues.get(wheelValue);
						if (generalQV == qualityValue)
							qualityValue = null;
					}
					
					QualityValue qualityValue_ = qualityValue;
					
					modifyTruckQvMap(truckId, map ->
					{
						if (qualityValue_!=null)
							map.put(wheelValue, qualityValue_);
						else
							map.remove(wheelValue);
					});
				}
			}

			private void modifyTruckQvMap(String truckId, Consumer<EnumMap<WheelValue, QualityValue>> action)
			{
				EnumMap<WheelValue, QualityValue> map = getOrCreate(truckValues, truckId, ()->new EnumMap<>(WheelValue.class));
				action.accept(map);
				if (map.isEmpty())
					truckValues.remove(truckId);
			}
		}
		
		@SuppressWarnings("unused")
		private static class MinMax
		{
			private float min;
			private float max;

			private MinMax(float min, float max)
			{
				this.min = min;
				this.max = max;
			}
			
			private void update(float value)
			{
				if (min > value)
					min = value;
				if (max < value)
					max = value;
			}
		}
	}
}
