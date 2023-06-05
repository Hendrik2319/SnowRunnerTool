package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.AddonCategories;
import net.schwarzbaer.java.games.snowrunner.Data.HasNameAndID;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.AddonSockets;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.Data.WheelsDef;
import net.schwarzbaer.java.games.snowrunner.MapTypes.StringVectorMap;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.ImageDialogController;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.ScrollValues;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.WheelsQualityRanges;
import net.schwarzbaer.java.games.snowrunner.tables.CombinedTableTabTextOutputPanel.CombinedTableTabPaneTextPanePanel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.EnginesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.GearboxesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.SuspensionsTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.SetInstancesTableModel.WinchesTableModel;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.SplitOrientation;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.SplitPaneConfigurator;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTAOS;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTPOS;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.lib.gui.Disabler;
import net.schwarzbaer.java.lib.gui.ImageView;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.TextAreaDialog;
import net.schwarzbaer.java.lib.gui.ValueListOutput;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas;
import net.schwarzbaer.java.lib.system.ValueContainer;

public class TruckPanelProto implements Finalizable {
	
	private final Finalizer finalizer;
	private final ImageView truckImageView;
	private final JTextArea truckInfoTextArea;
	private final JScrollPane truckInfoTextAreaScrollPane;
	private final CompatibleWheelsPanel compatibleWheelsPanel;
	private final AddonSocketsPanel addonSocketsPanel;
	private final AddonSocketGroupsPanel addonSocketGroupsPanel;
	private final AddonCategoriesPanel addonCategoriesPanel;
	private Language language;
	private Truck truck;
	private SaveGame saveGame;
	private AddonCategories addonCategories;
	private final GlobalFinalDataStructures gfds;
	private final ImageDialogController imageDialogController;

	public TruckPanelProto(Window mainWindow, GlobalFinalDataStructures gfds, ImageDialogController imageDialogController) {
		this.gfds = gfds;
		language = null;
		truck = null;
		saveGame = null;
		finalizer = gfds.controllers.createNewFinalizer();
		this.imageDialogController = imageDialogController;
		
		truckImageView = new ImageView(300,300, null, true);
		extendTruckImageViewContextMenu(truckImageView.getContextMenu());
		
		truckInfoTextArea = new JTextArea();
		truckInfoTextArea.setEditable(false);
		truckInfoTextArea.setWrapStyleWord(true);
		truckInfoTextArea.setLineWrap(true);
		truckInfoTextAreaScrollPane = new JScrollPane(truckInfoTextArea);
		truckInfoTextAreaScrollPane.setPreferredSize(new Dimension(300,300));
		
		SaveGame.TruckDesc displayedTruck = gfds.controllers.storedTruckDisplayers.getDisplayedTruck();
		finalizer.addSubComp(compatibleWheelsPanel  = new CompatibleWheelsPanel (mainWindow, gfds, displayedTruck));
		finalizer.addSubComp(addonSocketsPanel      = new AddonSocketsPanel     (mainWindow, gfds));
		finalizer.addSubComp(addonSocketGroupsPanel = new AddonSocketGroupsPanel(mainWindow, gfds));
		finalizer.addSubComp(addonCategoriesPanel   = new AddonCategoriesPanel  (mainWindow, gfds, displayedTruck));
		
		finalizer.addLanguageListener(language->{
			this.language = language;
			updateOutput();
		});
		finalizer.addSaveGameListener(saveGame->{
			this.saveGame = saveGame;
			updateOutput();
		});
		finalizer.addDLCListener(() ->
			updateOutput()
		);
		finalizer.addDataReceiver(data -> {
			addonCategories = data==null ? null : data.addonCategories;
			updateOutput();
		});
		finalizer.addTruckImagesListener(truckID ->  {
			if (truck==null) return;
			if (!truck.id.equals(truckID)) return;
			updateTruckImage();
		});
		finalizer.addStoredTruckDisplayer(displayedTruck_ -> {
			compatibleWheelsPanel.setDisplayedTruck(displayedTruck_);
			addonCategoriesPanel .setDisplayedTruck(displayedTruck_);
		});
		
		updateOutput();
		updateTruckImage();
	}
	
	private void extendTruckImageViewContextMenu(ContextMenu contextMenu)
	{
		contextMenu.addSeparator();
		contextMenu.add(SnowRunner.createMenuItem("Show in separate window", true, e->{
			imageDialogController.showDialog();
			updateTruckImage();
		}));
		//contextMenu.addContextMenuInvokeListener(null);
	}

	@Override public void prepareRemovingFromGUI() {
		finalizer.removeSubCompsAndListenersFromGUI();
	}
	
	public JTabbedPane createTabbedPane() {
		
		JTabbedPane tabbedPanel = new JTabbedPane();
		tabbedPanel.addTab("General Infos", truckInfoTextAreaScrollPane);
		tabbedPanel.addTab("Image", truckImageView);
		addStandardTabsTo(tabbedPanel);
		
		return tabbedPanel;
	}
	
	public JSplitPane createSplitPane() {
		JTabbedPane tabbedPanel = new JTabbedPane();
		tabbedPanel.addTab("Image", truckImageView);
		addStandardTabsTo(tabbedPanel);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		splitPane.setResizeWeight(0);
		splitPane.setTopComponent(truckInfoTextAreaScrollPane);
		splitPane.setBottomComponent(tabbedPanel);
		return splitPane;
	}

	private void addStandardTabsTo(JTabbedPane tabbedPanel) {
		tabbedPanel.addTab("Compatible Wheels"     , compatibleWheelsPanel);
		tabbedPanel.addTab("Addon Sockets"         , addonSocketsPanel.rootComp);
		tabbedPanel.addTab("Addons by Socket Group", addonSocketGroupsPanel);
		tabbedPanel.addTab("Addons by Category"    , addonCategoriesPanel);
	}

	public void setTruck(Truck truck, Data data) {
		this.truck = truck;
		compatibleWheelsPanel .setData(truck==null ? null : truck.compatibleWheels, truck==null ? null : truck.id, truck==null ? null : truck.gameData.name_StringID);
		addonSocketsPanel     .setData(truck==null ? null : truck.addonSockets);
		addonSocketGroupsPanel.setData(truck==null ? null : truck.addonSockets);
		addonCategoriesPanel  .setData(truck, data);
		updateTruckImage();
		updateOutput();
	}

	private void updateTruckImage()
	{
		BufferedImage image = truck==null ? null : gfds.truckImages.get(truck.id);
		truckImageView.setImage(image);
		truckImageView.setZoom(1);
		imageDialogController.setImage(image);
	}

	private void updateOutput() {
		if (truck==null) {
			truckInfoTextArea.setText("<NULL>");
			return;
		}
		
		ValueListOutput outTop = new ValueListOutput();
		
		outTop.add(0, "ID"     , truck.id);
		outTop.add(0, "Country", truck.gameData.country==null ? null : truck.gameData.country.toString());
		outTop.add(0, "Price"  , truck.gameData.price);
		outTop.add(0, "Type"   , truck.type==null ? null : truck.type.toString());
		outTop.add(0, "Unlock By Exploration", truck.gameData.unlockByExploration, "yes", "no");
		outTop.add(0, "Unlock By Rank"       , truck.gameData.unlockByRank);
		outTop.add(0, "Unlock By Objective"  , truck.gameData.unlockByObjective);
		outTop.add(0, "XML file"             , truck.xmlName);
		
		if (truck.updateLevel!=null)
			outTop.add(0, "Update Level", truck.updateLevel);
		
		if (truck.id!=null) {
			String dlc = gfds.dlcs.getDLC(truck.id, SnowRunner.DLCs.ItemType.Truck);
			if (dlc!=null)
				outTop.add(0, "DLC", dlc);
		}
		
		if (saveGame!=null) {
			outTop.add(0, "");
			outTop.add(0, "Owned by Player", saveGame.getOwnedTruckCount(truck));
		}
		
		
		outTop.add(0, "");
		
		String name = null;
		String description = null;
		if (language!=null) {
			name        = language.get(truck.gameData.name_StringID);
			description = language.get(truck.gameData.description_StringID);
		}
		outTop.add(0, "Name", "<%s>", truck.gameData.name_StringID);
		if (name!=null)
			outTop.add(0, null, name);
		
		outTop.add(0, "");
		
		outTop.add(0, "Description", "<%s>", truck.gameData.description_StringID);
		if (description != null)
			outTop.add(0, null, description);
		
		if (!truck.defaultAddons.isEmpty()) {
			outTop.add(0, "");
			outTop.add(0, "DefaultAddons", "%s", SnowRunner.joinTruckAddonNames(truck.defaultAddons, addonCategories, language));
		}
		
		ScrollValues scrollPos = ScrollValues.getVertical(truckInfoTextAreaScrollPane);
		truckInfoTextArea.setText(outTop.generateOutput());
		if (scrollPos!=null) SwingUtilities.invokeLater(()->scrollPos.setVertical(truckInfoTextAreaScrollPane));
	}

	static class AddonCategoriesPanel extends CombinedTableTabPaneTextPanePanel implements Finalizable {
		private static final long serialVersionUID = 4098254083170104250L;

		private static final String CONTROLLERS_CHILDLIST_TABTABLEMODELS = "TabTableModels";
		
		private final Window mainWindow;
		private final Finalizer finalizer;
		private Language language;
		private Truck truck;
		private final Vector<Tab<?>> currentTabs;
		private AddonCategories addonCategories;
		private SaveGame saveGame;
		private final GlobalFinalDataStructures gfds;
		private SaveGame.TruckDesc displayedTruck;

		AddonCategoriesPanel(Window mainWindow, GlobalFinalDataStructures gfds, SaveGame.TruckDesc displayedTruck) {
			this.mainWindow = mainWindow;
			this.gfds = gfds;
			this.displayedTruck = displayedTruck;
			this.finalizer = gfds.controllers.createNewFinalizer();
			this.truck = null;
			saveGame = null;
			addonCategories = null;
			currentTabs = new Vector<>();
			language = null;
			
			finalizer.addLanguageListener(language -> {
				this.language = language;
				updateTabTitles();
			});
			finalizer.addDataReceiver(data->{
				this.addonCategories = data.addonCategories;
				updateTabTitles();
			});
			finalizer.addSaveGameListener(saveGame->{
				this.saveGame = saveGame;
			});
		}
		
		public void setDisplayedTruck(SaveGame.TruckDesc displayedTruck)
		{
			this.displayedTruck = displayedTruck;
			for (Tab<?> tab : currentTabs)
				tab.updateDisplayedTruck();
		}

		@Override public void prepareRemovingFromGUI() {
			finalizer.removeSubCompsAndListenersFromGUI();
		}
		
		private void updateTabTitles() {
			for (Tab<?> tab : currentTabs)
				tab.updateTabTitle();
			repaint();
		}

		public void setData(Truck truck, Data data) {
			this.truck = truck;
			
			String selectedCategory = getSelectedCategory();
			currentTabs.clear();
			removeAllTabs();
			finalizer.removeVolatileSubCompsFromGUI(CONTROLLERS_CHILDLIST_TABTABLEMODELS);
			
			if (this.truck!=null) {
				
				createTab("Trailers"  , this.truck.compatibleTrailers    , () -> new TrailersTableModel   (mainWindow, gfds, false, data, saveGame).set(this.truck));
				createTab("engine"    , this.truck.compatibleEngines     , () -> new EnginesTableModel    (mainWindow, gfds, false, saveGame, true, createDefaultColumn_Single(tr->tr.defaultEngine_ItemID    , Data.Engine    .class)));
				createTab("gearbox"   , this.truck.compatibleGearboxes   , () -> new GearboxesTableModel  (mainWindow, gfds, false, saveGame, true, createDefaultColumn_Single(tr->tr.defaultGearbox_ItemID   , Data.Gearbox   .class)));
				createTab("suspension", this.truck.compatibleSuspensions , () -> new SuspensionsTableModel(mainWindow, gfds, false, saveGame, true, createDefaultColumn_Single(tr->tr.defaultSuspension_ItemID, Data.Suspension.class)));
				createTab("winch"     , this.truck.compatibleWinches     , () -> new WinchesTableModel    (mainWindow, gfds, false, saveGame, true, createDefaultColumn_Single(tr->tr.defaultWinch_ItemID     , Data.Winch     .class)));
				
				StringVectorMap<TruckAddon> compatibleTruckAddons = this.truck.compatibleTruckAddons;
				Vector<String> truckAddonCategories = new Vector<>(compatibleTruckAddons.keySet());
				truckAddonCategories.sort(SnowRunner.CATEGORY_ORDER);
				for (String category : truckAddonCategories)
					createTab(category, compatibleTruckAddons.get(category), () ->
						new TruckAddonsTableModel(
							mainWindow, gfds, false, true, createDefaultColumn_List(tr->tr.defaultAddonIDs,TruckAddon.class)
						).set(data, saveGame).set(this.truck)
					);
			}
			setSelectedCategory(selectedCategory);
		}

		private void setSelectedCategory(String category)
		{
			int tabIndex = getCategoryIndex(category);
			if (tabIndex>=0) setSelectedTab(tabIndex);
		}

		private int getCategoryIndex(String category)
		{
			if (category!=null)
				for (int tabIndex=0; tabIndex<currentTabs.size(); tabIndex++)
				{
					Tab<?> tab = currentTabs.get(tabIndex);
					if (category.equals(tab.category))
						return tabIndex;
				}
			return -1;
		}

		private String getSelectedCategory()
		{
			int tabIndex = getSelectedTab();
			Tab<?> tab = tabIndex<0 || tabIndex>=currentTabs.size() ? null : currentTabs.get(tabIndex);
			return tab== null ? null : tab.category;
		}

		private <RowType extends HasNameAndID> VerySimpleTableModel.ColumnID createDefaultColumn_List(Function<Truck,Collection<String>> getDefaultIDs, Class<RowType> rowType) {
			return createDefaultColumn(
					(truck,rowID)->{
						Collection<String> defaultIDs = getDefaultIDs.apply(truck);
						return defaultIDs!=null && defaultIDs.contains(rowID);
					},
					createRowIDFcn(rowType)
			);
		}

		private <RowType extends HasNameAndID> VerySimpleTableModel.ColumnID createDefaultColumn_Single(Function<Truck,String> getDefaultID, Class<RowType> rowType) {
			return createDefaultColumn(
					(truck,rowID) -> {
						String defaultID = getDefaultID.apply(truck);
						return defaultID!=null && defaultID.equals(rowID);
					},
					createRowIDFcn(rowType)
			);
		}

		private <RowType extends HasNameAndID> Function<Object, String> createRowIDFcn(Class<RowType> rowType) {
			return row -> !rowType.isInstance(row) ? null : rowType.cast(row).getID();
		}

		private VerySimpleTableModel.ColumnID createDefaultColumn(BiFunction<Truck,String,Boolean> isDefaultID, Function<Object,String> getRowID) {
			return createDefaultColumn(row->{
				if (truck==null) return false;
				if (row==null) return false;
				String rowID = getRowID.apply(row);
				return isDefaultID.apply(truck, rowID);
			});
		}

		private VerySimpleTableModel.ColumnID createDefaultColumn(Function<Object,Boolean> isDefault) {
			return new VerySimpleTableModel.ColumnID("Default","Default", String.class, 50, null, null, false, row->{
				if (isDefault.apply(row)) return "default";
				return null;
			});
		}

//		private boolean isDefault(Object row) {
//			TruckAddon addon = (TruckAddon)row;
//			boolean isDefault = truck.defaultAddonIDs.contains(addon.id);
//			return isDefault;
//		}

		private <ItemType> void createTab(String category, Collection<ItemType> usableItems, Supplier<ExtendedVerySimpleTableModelTPOS<ItemType>> constructor) {
			if (!usableItems.isEmpty()) {
				Tab<ItemType> tab = new Tab<>(category, usableItems.size(), constructor.get()); // create TableModel only in case of usableItems
				currentTabs.add(tab);
				finalizer.addVolatileSubComp(CONTROLLERS_CHILDLIST_TABTABLEMODELS, tab.tableModel);
				addTab("##", tab.tableModel);
				setTabComponentAt(currentTabs.size()-1, tab.tabComp);
				tab.tableModel.setLanguage(language);
				tab.tableModel.setRowData(usableItems);
				tab.updateDisplayedTruck();
			}
		}
				
		public interface DisplayedTruckComponentList
		{
			boolean setDisplayedTruck(SaveGame.TruckDesc displayedTruck);
		}

		private class Tab<ItemType>
		{
			private static final Color COLOR_BG_DISPLAYED_TRUCK_COMP = new Color(0xFFDF00);
			
			final String category;
			final int size;
			final Tables.LabelRendererComponent tabComp;
			final ExtendedVerySimpleTableModelTPOS<ItemType> tableModel;
			
			Tab(String category, int size, ExtendedVerySimpleTableModelTPOS<ItemType> tableModel) {
				this.category = category;
				this.size = size;
				this.tableModel = tableModel;
				this.tabComp = new Tables.LabelRendererComponent();
				if (SnowRunner.CATEGORY_ORDER_LIST.contains(this.category))
					this.tabComp.setFont(this.tabComp.getFont().deriveFont(Font.BOLD));
				updateTabTitle();
			}
			
			void updateDisplayedTruck()
			{
				if (tableModel instanceof DisplayedTruckComponentList)
				{
					DisplayedTruckComponentList tm = (DisplayedTruckComponentList) tableModel;
					boolean isTruck = displayedTruck!=null && truck!=null && truck.id.equals(displayedTruck.type);
					boolean hasHitRow = tm.setDisplayedTruck(isTruck ? displayedTruck : null);
					tabComp.setBackground(hasHitRow ? COLOR_BG_DISPLAYED_TRUCK_COMP : null);
					tabComp.setOpaque(hasHitRow);
				}
			}

			private void updateTabTitle() {
				String categoryLabel = AddonCategories.getCategoryLabel(category, addonCategories, language);
				this.tabComp.setText(String.format("%s [%d]", categoryLabel, size));
			}
		}
	}

	private static class AddonSocketGroupsPanel extends JPanel implements Finalizable {
		private static final long serialVersionUID = 5515829836865733889L;
		
		private final JLabel socketIndexLabel;
		private final JLabel defaultAddonLabel;
		private final JTabbedPane tablePanels;
		private final TrailersTableModel trailersTableModel;
		private final TruckAddonsTableModel truckAddonsTableModel;
		private AddonSockets[] addonSockets;
		private int currentSocketIndex;
		private Language language;
		private final Finalizer finalizer;

		AddonSocketGroupsPanel(Window mainWindow, GlobalFinalDataStructures gfds) {
			super(new GridBagLayout());
			
			finalizer = gfds.controllers.createNewFinalizer();
			addonSockets = null;
			currentSocketIndex = 0;
			language = null;
			
			tablePanels = new JTabbedPane();
			tablePanels.addTab("Trailers", TableSimplifier.create(finalizer.addSubComp(   trailersTableModel = new TrailersTableModel   (mainWindow, gfds, false))));
			tablePanels.addTab("Addons"  , TableSimplifier.create(finalizer.addSubComp(truckAddonsTableModel = new TruckAddonsTableModel(mainWindow, gfds, false))));
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.weightx = 0;
			add(SnowRunner.createButton("<", true, e->switchSocket(-1)),c);
			add(socketIndexLabel = new JLabel(" # / # "),c);
			add(SnowRunner.createButton(">", true, e->switchSocket(+1)),c);
			
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			add(defaultAddonLabel = new JLabel(" Default: ########"),c);
			
			c.weighty = 1;
			add(tablePanels,c);
			
			updatePanel();
			
			finalizer.addLanguageListener(language -> {
				this.language = language;
				updateDefaultAddonLabel();
			});
		}
		
		@Override public void prepareRemovingFromGUI() {
			finalizer.removeSubCompsAndListenersFromGUI();
		}
		
		void setData(AddonSockets[] addonSockets) {
			this.addonSockets = addonSockets;
			currentSocketIndex = 0;
			updatePanel();
		}

		private void switchSocket(int inc) {
			if (addonSockets==null) return;
			if (currentSocketIndex+inc<0) return;
			if (currentSocketIndex+inc>=addonSockets.length) return;
			currentSocketIndex = currentSocketIndex+inc;
			updatePanel();
		}

		private void updatePanel() {
			updateDefaultAddonLabel();
			
			if (addonSockets==null || currentSocketIndex<0 || currentSocketIndex>=addonSockets.length) {
				socketIndexLabel.setText(" # / # ");
				trailersTableModel.setRowData(null);
				truckAddonsTableModel.setRowData(null);
				tablePanels.setTitleAt(0, "Trailers");
				tablePanels.setTitleAt(1, "Addons");
				
			} else {
				socketIndexLabel.setText(String.format(" %d / %d ", currentSocketIndex+1, addonSockets.length));
				AddonSockets socket = addonSockets[currentSocketIndex];
				
				HashSet<Trailer> trailers = new HashSet<>();
				socket.compatibleTrailers.values().forEach(trailers::addAll);
				trailersTableModel.setRowData(trailers);
				
				HashSet<TruckAddon> truckAddons = new HashSet<>();
				socket.compatibleTruckAddons.values().forEach(truckAddons::addAll);
				truckAddonsTableModel.setRowData(truckAddons);
				
				if (trailers.isEmpty() && !truckAddons.isEmpty())
					tablePanels.setSelectedIndex(1);
				else
					tablePanels.setSelectedIndex(0);
				
				tablePanels.setTitleAt(0, String.format("Trailers [%d]", trailers.size()));
				tablePanels.setTitleAt(1, String.format("Addons [%d]", truckAddons.size()));
			}
		}

		private void updateDefaultAddonLabel() {
			if (addonSockets==null || currentSocketIndex<0 || currentSocketIndex>=addonSockets.length) {
				defaultAddonLabel.setText(" Default: ########");
			} else {
				AddonSockets socket = addonSockets[currentSocketIndex];
				
				String defaultAddon = "-- None --";
				if (socket.defaultAddonID!=null)
					defaultAddon = SnowRunner.solveStringID(socket.defaultAddonItem, socket.defaultAddonID, language);
				defaultAddonLabel.setText(String.format(" Default: %s", defaultAddon));
				
			}
		}
	}

	private static class AddonSocketsPanel implements Finalizable
	{
		private final Finalizer finalizer;
		private final AddonSocketsTableModel tableModel;
		private final JComponent rootComp;
		
		AddonSocketsPanel(Window mainWindow, GlobalFinalDataStructures gfds)
		{
			finalizer = gfds.controllers.createNewFinalizer();
			finalizer.addSubComp(tableModel = new AddonSocketsTableModel(mainWindow, gfds));
			rootComp = TableSimplifier.create(tableModel);
		}
	
		void setData(AddonSockets[] addonSockets)
		{
			tableModel.setData(addonSockets);
		}
		
		@Override public void prepareRemovingFromGUI() {
			finalizer.removeSubCompsAndListenersFromGUI();
		}

		private static class AddonSocketsTableModel extends ExtendedVerySimpleTableModelTAOS<AddonSocketsTableModel.RowItem> implements SplitPaneConfigurator
		{
			AddonSocketsTableModel(Window mainWindow, GlobalFinalDataStructures gfds) {
				super(mainWindow, gfds, new ColumnID[] {
						new ColumnID( "IndexAS         ", "#"                , Integer.class,  30, CENTER,    null, false, get(row -> row.indexAS+1                          )), 
						new ColumnID( "DefaultAddon    ", "Default Addon"    ,  String.class, 210,   null,    null, false, get(row -> row.as.defaultAddonID                  )),
						new ColumnID( "IndexSocket     ", "#"                , Integer.class,  30, CENTER,    null, false, get(row -> row.indexSocket+1                      )),
						new ColumnID( "SocketID        ", "SocketID"         ,  String.class, 230,   null,    null, false, get(row -> toString( row.socket.socketIDs )       )),
						new ColumnID( "InCockpit       ", "In Cockpit"       , Boolean.class,  60,   null,    null, false, get(row -> row.socket.isInCockpit                 )),
				});
				coloring.addBackgroundRowColorizer(Coloring.createOddEvenColorizer(row -> row.indexAS));
			}
			
			private static <ResultType> Function<Object,ResultType> get(Function<RowItem,ResultType> getFunction)
			{
				return ColumnID.get(RowItem.class, getFunction);
			}
			
			@Override public boolean useLineWrap() { return false; }
			@Override public boolean createSplitPane() { return true; }
			@Override public Boolean putOutputInScrollPane() { return true; }
			@Override public SplitOrientation getSplitOrientation() { return SplitOrientation.HORIZONTAL_SPLIT; }

			private static class RowItem
			{
				final int indexAS;
				final AddonSockets as;
				final int indexSocket;
				final AddonSockets.Socket socket;

				public RowItem(int indexAS, AddonSockets as, int indexSocket, AddonSockets.Socket socket)
				{
					this.indexAS = indexAS;
					this.as = as;
					this.indexSocket = indexSocket;
					this.socket = socket;
				}
			}
			
			@Override protected String getRowName(RowItem row)
			{
				return row==null ? null : String.format("Socket %d_%d", row.indexAS+1, row.indexSocket+1);
			}

			@Override
			protected String getOutputTextForRow(int rowIndex, RowItem row)
			{
				ValueListOutput out = new ValueListOutput();
				
				if (row.socket.offset_     !=null) out.add(0, "Offset"      , "%s", row.socket.offset_     );
				if (row.socket.dir_        !=null) out.add(0, "Dir"         , "%s", row.socket.dir_        );
				if (row.socket.upDir_      !=null) out.add(0, "UpDir"       , "%s", row.socket.upDir_      );
				if (row.socket.parentFrame_!=null) out.add(0, "Parent Frame", "%s", row.socket.parentFrame_);
				
				if (!row.socket.isBlockedBy.isEmpty())
				{
					if (!out.isEmpty()) out.addEmptyLine();
					for (String socketID : SnowRunner.getSorted(row.socket.isBlockedBy.keySet()))
					{
						out.add(0, String.format("\"%s\" is blocked by", socketID));
						for (String[] arr : row.socket.isBlockedBy.get(socketID))
							out.add(1, null, "%s", toString(arr));
					}
				}
				
				if (!row.socket.isShiftedBy.isEmpty())
				{
					if (!out.isEmpty()) out.addEmptyLine();
					for (AddonSockets.Socket.AddonsShift as : row.socket.isShiftedBy)
					{
						out.add(0, "is shifted");
						out.add(1, "by"    , "%s", toString(as.types()));
						out.add(1, "offset", "%s", as.offset());
					}
				}
				
				if (row.socket.raw_AddonsShifts.length>0)
				{
					if (!out.isEmpty()) out.addEmptyLine();
					out.add(0, "Raw Addons Shifts");
					AddonSockets.Socket.RawAddonsShift[] addonsShifts = row.socket.raw_AddonsShifts;
					for (int i=0; i<addonsShifts.length; i++)
					{
						AddonSockets.Socket.RawAddonsShift as = addonsShifts[i];
						out.add(1, String.format("Addons Shift [%d]", i+1));
						if (as.types_            !=null) out.add(2, "Types"            , "%s", toString(as.types_)  );
						if (as.trailerNamesBlock_!=null) out.add(2, "TrailerNamesBlock", "%s", as.trailerNamesBlock_);
						if (as.offset_           !=null) out.add(2, "Offset"           , "%s", as.offset_           );
					}
				}
				
				if (row.socket.raw_BlockedSocketIDs.length>0)
				{
					if (!out.isEmpty()) out.addEmptyLine();
					out.add(0, "Raw Blocked Socket IDs");
					out.add(1, null, "%s", toString(row.socket.raw_BlockedSocketIDs));
				}
				
				return out.generateOutput();
			}

			void setData(Data.Truck.AddonSockets[] addonSockets)
			{
				Vector<RowItem> data = new Vector<>();
				if (addonSockets!=null)
					for (int i=0; i<addonSockets.length; i++) {
						AddonSockets as = addonSockets[i];
						for (int j=0; j<as.sockets.length; j++) {
							AddonSockets.Socket socket = as.sockets[j];
							data.add(new RowItem(i,as,j,socket));
						}
					}
				setRowData(data);
			}

			private static String toString(String[] strs) {
				if (strs==null) return "<null>";
				if (strs.length==0) return "[]";
				if (strs.length==1) return strs[0];
				return Arrays.toString(strs);
			}
		}
	}

	private static class CompatibleWheelsPanel extends JSplitPane implements Finalizable {
		private static final long serialVersionUID = -6605852766458546928L;
		
		private final Window mainWindow;
		private final Finalizer finalizer;
		private final JTextArea textArea;
		private final CWTableModel tableModel;
		private CWTableModel.RowItem selectedWheel;
		private Language language;
		private CompatibleWheel[] compatibleWheels;
		private String truckName_StringID;
		
		CompatibleWheelsPanel(Window mainWindow, GlobalFinalDataStructures gfds, SaveGame.TruckDesc displayedTruck) {
			super(JSplitPane.VERTICAL_SPLIT, true);
			setResizeWeight(1);
			this.mainWindow = mainWindow;
			
			selectedWheel = null;
			language = null;
			compatibleWheels = null;
			truckName_StringID = null;
			finalizer = gfds.controllers.createNewFinalizer();
			
			JComponent tableScrollPane = TableSimplifier.create(
					tableModel = new CWTableModel(mainWindow, gfds, displayedTruck),
					(TableSimplifier.UnspecificOutputSource) rowIndex -> {
						selectedWheel = tableModel.getRow(rowIndex);
						updateWheelInfo();
					}
			);
			finalizer.addSubComp(tableModel);
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Description"));
			textAreaScrollPane.setPreferredSize(new Dimension(400,100));
			
			
			JPanel tableButtonsPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weightx = 0;
			tableButtonsPanel.add(SnowRunner.createButton("Show Wheel Data in Diagram", true, e->{
				if (tableModel.rows!=null)
					new WheelsDiagramDialog(this.mainWindow, tableModel.rows, truckName_StringID, language).showDialog();
			}),c);
			
			tableButtonsPanel.add(SnowRunner.createButton("Show Wheel Data as Text", true, e->{
				String text;
				if (compatibleWheels == null || compatibleWheels.length == 0)
					text = "<No Compatible Wheels>";
				else {
					ValueListOutput outFull = new ValueListOutput();
					outFull.add(0, "Compatible Wheels", compatibleWheels.length);
					for (int i=0; i<compatibleWheels.length; i++) {
						Data.Truck.CompatibleWheel cw = compatibleWheels[i];
						outFull.add(1, String.format("[%d]", i+1), "(%s) %s", cw.scale, cw.type);
						cw.printTireList(outFull,2);
					}
					text = outFull.generateOutput();
				}
				TextAreaDialog.showText(this.mainWindow, "Compatible Wheels", 600, 400, true, text);
			}),c);
			
			c.weightx = 1;
			tableButtonsPanel.add(new JLabel(),c);
			
			JPanel tablePanel = new JPanel(new BorderLayout());
			tablePanel.add(tableScrollPane, BorderLayout.CENTER);
			tablePanel.add(tableButtonsPanel, BorderLayout.SOUTH);
			
			setTopComponent(tablePanel);
			setBottomComponent(textAreaScrollPane);
			
			finalizer.addLanguageListener(language->{
				this.language = language;
				updateWheelInfo();
			});
			
			updateWheelInfo();
		}

		public void setDisplayedTruck(SaveGame.TruckDesc displayedTruck)
		{
			tableModel.setDisplayedTruck(displayedTruck);
		}

		@Override public void prepareRemovingFromGUI()
		{
			finalizer.removeSubCompsAndListenersFromGUI();
		}

		private void updateWheelInfo() {
			textArea.setText("");
			if (selectedWheel != null) {
				//singleWheelInfoTextArea.append(selectedWheel.tire.tireType_StringID+"\r\n");
				//singleWheelInfoTextArea.append("Description:\r\n");
				String description = SnowRunner.solveStringID(selectedWheel.tire.gameData.description_StringID, language);
				textArea.append(description+"\r\n");
			}
		}
	
		void setData(CompatibleWheel[] compatibleWheels, String truckId, String truckName_StringID) {
			this.compatibleWheels = compatibleWheels;
			this.truckName_StringID = truckName_StringID;
			tableModel.setData(compatibleWheels, truckId);
			updateWheelInfo();
		}

		private static class CWTableModel extends VerySimpleTableModel<CWTableModel.RowItem>
		{
			private static final String ID_QUALITY_HIGHWAY = "QualityHighway";
			private static final String ID_QUALITY_OFFROAD = "QualityOffroad";
			private static final String ID_QUALITY_MUD     = "QualityMud";
			private static final Color COLOR_BG_TRUCK_QV   = new Color(0xF1D5D5);
			private static final Color COLOR_BG_GENERAL_QV = new Color(0xD5F1D5);
			private static final Color COLOR_BG_DISPLAYED_TRUCK_WHEEL = new Color(0xFFDF00);
			
			private String truckId;
			private SaveGame.TruckDesc displayedTruck;

			CWTableModel(Window mainWindow, GlobalFinalDataStructures gfds, SaveGame.TruckDesc displayedTruck)
			{
				super(mainWindow, gfds, new ColumnID[] {
						new ColumnID( "WheelsDefID"        , "WheelsDef"            , String                          .class, 140,   null,    null, false, get(row -> row.wheelsDefID                )),
						new ColumnID( "TireDef"            , "TireDef"              , String                          .class, 110,   null,    null, false, get(row -> row.tire.tireDefID)),
						new ColumnID( "Type"               , "Type"                 , String                          .class,  80,   null,    null,  true, get(row -> row.tire.tireType_StringID     )),
						new ColumnID( "Name"               , "Name"                 , String                          .class, 130,   null,    null,  true, get(row -> row.tire.gameData.name_StringID)),
						new ColumnID( "UpdateLevel"        , "Update Level"         , String                          .class,  80,   null,    null, false, get(row -> row.updateLevel                )),
						new ColumnID( "Scale"              , "Scale"                , Float                           .class,  50,   null, "%1.4f", false, get(row -> row.scale                      )),
						new ColumnID( "Size"               , "Size"                 , Integer                         .class,  50, CENTER,  "%d\"", false, get(row -> row.getSize()                  )),
						new ColumnID( "FrictionHighway"    , "Highway"              , Float                           .class,  55,   null, "%1.2f", false, get(row -> row.tire.frictionHighway       )),
						new ColumnID( "FrictionOffroad"    , "Offroad"              , Float                           .class,  50,   null, "%1.2f", false, get(row -> row.tire.frictionOffroad       )),
						new ColumnID( "FrictionMud"        , "Mud"                  , Float                           .class,  50,   null, "%1.2f", false, get(row -> row.tire.frictionMud           )),
						new ColumnID( "OnIce"              , "On Ice"               , Boolean                         .class,  50,   null,    null, false, get(row -> row.tire.onIce                 )),
						new ColumnID( ID_QUALITY_HIGHWAY   , "Highway"              , WheelsQualityRanges.QualityValue.class,  55, CENTER,    null, false, get(row -> row.getQualityValue(WheelsQualityRanges.WheelValue.Highway))),
						new ColumnID( ID_QUALITY_OFFROAD   , "Offroad"              , WheelsQualityRanges.QualityValue.class,  55, CENTER,    null, false, get(row -> row.getQualityValue(WheelsQualityRanges.WheelValue.Offroad))),
						new ColumnID( ID_QUALITY_MUD       , "Mud"                  , WheelsQualityRanges.QualityValue.class,  55, CENTER,    null, false, get(row -> row.getQualityValue(WheelsQualityRanges.WheelValue.Mud    ))),
						new ColumnID( "Price"              , "Price"                , Integer                         .class,  50,   null, "%d Cr", false, get(row -> row.tire.gameData.price               )),
						new ColumnID( "UnlockByExploration", "Unlock By Exploration", Boolean                         .class, 120,   null,    null, false, get(row -> row.tire.gameData.unlockByExploration )),
						new ColumnID( "UnlockByRank"       , "Unlock By Rank"       , Integer                         .class, 100, CENTER,    null, false, get(row -> row.tire.gameData.unlockByRank        )),
						new ColumnID( "Description"        , "Description"          , String                          .class, 200,   null,    null,  true, get(row -> row.tire.gameData.description_StringID)),
				});
				this.displayedTruck = displayedTruck;
				truckId = null;
				setColumnBgColoring(coloring, ID_QUALITY_HIGHWAY, WheelsQualityRanges.WheelValue.Highway);
				setColumnBgColoring(coloring, ID_QUALITY_OFFROAD, WheelsQualityRanges.WheelValue.Offroad);
				setColumnBgColoring(coloring, ID_QUALITY_MUD    , WheelsQualityRanges.WheelValue.Mud    );
				coloring.addBackgroundRowColorizer(row -> {
					if (this.displayedTruck!=null)
					{
						if (
							truckId!=null && truckId.equals(this.displayedTruck.type) &&
							(
								(this.displayedTruck.wheels!=null && this.displayedTruck.wheels.equals(row.wheelsDefID)) ||
								(this.displayedTruck.wheels==null && row.wheelsDefID==null)
							) && (
								(this.displayedTruck.tires!=null && this.displayedTruck.tires.equals(row.tire.tireDefID)) ||
								(this.displayedTruck.tires==null && row.tire.tireDefID==null)
							) && (
								isEqual( this.displayedTruck.wheelsScale, row.scale, 0.01 )
							)
						)
							return COLOR_BG_DISPLAYED_TRUCK_WHEEL;
					}
					return null;
				});
			}

			private boolean isEqual(double v1, Float v2, double tolerance)
			{
				if (v2==null) return false;
				if (!Double.isFinite(v1)) return false;
				if (!Float .isFinite(v2)) return false;
				double q = v1/v2;
				return 1-tolerance < q && q < 1+tolerance;
			}

			void setDisplayedTruck(SaveGame.TruckDesc displayedTruck)
			{
				this.displayedTruck = displayedTruck;
				table.repaint();
			}

			private static void setColumnBgColoring(Coloring<RowItem> coloring, String columnID, WheelsQualityRanges.WheelValue wheelValue)
			{
				coloring.setBackgroundColumnColoring(false, columnID, WheelsQualityRanges.QualityValue.class, (row, truckQV) -> {
					if (truckQV==null) return null;
					WheelsQualityRanges.QualityValue generalQV = row.getGeneralQualityValue(wheelValue);
					return truckQV==generalQV ? COLOR_BG_GENERAL_QV : COLOR_BG_TRUCK_QV;
				});
			}
			
			private static <ResultType> Function<Object,ResultType> get(Function<RowItem,ResultType> getFunction)
			{
				return ColumnID.get(RowItem.class, getFunction);
			}

			class RowItem {
				
				final String wheelsDefID;
				final String updateLevel;
				final Float scale;
				final TruckTire tire;
				
				RowItem(Float scale, TruckTire tire) {
					this.wheelsDefID = tire.wheelsDefID;
					this.updateLevel = tire.updateLevel;
					this.scale = scale;
					this.tire = tire;
				}
				
				Integer getSize() {
					return CompatibleWheel.computeSize_inch(scale);
				}
				
				void setQualityValue(WheelsQualityRanges.WheelValue wheelValue, WheelsQualityRanges.QualityValue qualityValue)
				{
					gfds.wheelsQualityRanges.setQualityValue(wheelsDefID, tire.indexInDef, truckId, wheelValue, qualityValue);
				}

				WheelsQualityRanges.QualityValue getGeneralQualityValue(WheelsQualityRanges.WheelValue wheelValue)
				{
					return gfds.wheelsQualityRanges.getQualityValue(wheelsDefID, tire.indexInDef, null, wheelValue);
				}

				WheelsQualityRanges.QualityValue getQualityValue(WheelsQualityRanges.WheelValue wheelValue)
				{
					return gfds.wheelsQualityRanges.getQualityValue(wheelsDefID, tire.indexInDef, truckId, wheelValue);
				}
			}

			@Override
			public void reconfigureAfterTableStructureUpdate()
			{
				super.reconfigureAfterTableStructureUpdate();
				table.setDefaultEditor(
						WheelsQualityRanges.QualityValue.class,
						new Tables.ComboboxCellEditor<>(
								SnowRunner.addNull(WheelsQualityRanges.QualityValue.values())
						)
				);
			}

			@Override
			protected String getRowName(RowItem row)
			{
				return row==null ? null : SnowRunner.solveStringID(row.tire.gameData.name_StringID, language);
			}
			
			void setData(CompatibleWheel[] compatibleWheels, String truckId) {
				this.truckId = truckId;
				
				Vector<RowItem> data = new Vector<>();
				if (compatibleWheels!=null) {
					for (CompatibleWheel wheel : compatibleWheels) {
						WheelsDef wheelsDef = wheel.wheelsDef;
						if (wheelsDef==null) continue;
						for (TruckTire tire : wheelsDef.truckTires)
							data.add( new RowItem(wheel.scale, tire) );
					}
					Comparator<Float >  floatNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
					Comparator<String> stringNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
					Comparator<String> typeComparator  = Comparator.nullsLast(Comparator.<String,Integer>comparing(TruckTire::getTypeOrder).thenComparing(Comparator.naturalOrder()));
					Comparator<RowItem> comparator = Comparator
							.<RowItem,String>comparing(cw->cw.tire.tireType_StringID,typeComparator)
							.thenComparing(cw->cw.scale,floatNullsLast)
							.thenComparing(cw->cw.tire.gameData.name_StringID,stringNullsLast);
					data.sort(comparator);
				}
				setRowData(data);
			}

			@Override
			protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
			{
				if (columnID!=null)
					switch (columnID.id)
					{
						case ID_QUALITY_HIGHWAY:
						case ID_QUALITY_OFFROAD:
						case ID_QUALITY_MUD    :
							return true;
					}
				return false;
			}

			@Override
			protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
			{
				RowItem row = getRow(rowIndex);
				if (row==null) return;
				
				if (columnID==null) return;
				switch (columnID.id)
				{
					case ID_QUALITY_HIGHWAY: row.setQualityValue(WheelsQualityRanges.WheelValue.Highway, (WheelsQualityRanges.QualityValue)aValue); break;
					case ID_QUALITY_OFFROAD: row.setQualityValue(WheelsQualityRanges.WheelValue.Offroad, (WheelsQualityRanges.QualityValue)aValue); break;
					case ID_QUALITY_MUD    : row.setQualityValue(WheelsQualityRanges.WheelValue.Mud    , (WheelsQualityRanges.QualityValue)aValue); break;
				}
			}
		}
	}

	private static class WheelsDiagramDialog extends JDialog {
		private static final long serialVersionUID = 1414536465711827973L;

		private enum AxisValue { Highway, Offroad, Mud }
		private enum GuiObjs { HorizAxesHighway, HorizAxesOffroad, HorizAxesMud, VertAxesHighway, VertAxesOffroad, VertAxesMud }

		private AxisValue horizAxis;
		private AxisValue vertAxis;
		private final Disabler<GuiObjs> disabler;
		private final Vector<CompatibleWheelsPanel.CWTableModel.RowItem> data;
		private final WheelsDiagram diagramView;
		private final Language language;
		
		WheelsDiagramDialog(Window owner, Vector<CompatibleWheelsPanel.CWTableModel.RowItem> data, String truckName_StringID, Language language) {
			super(owner, ModalityType.APPLICATION_MODAL);
			this.data = data;
			this.language = language;
			
			String truckName = SnowRunner.solveStringID(truckName_StringID, language, "Truck ???");
			setTitle("Wheels of "+truckName);
			
			horizAxis = AxisValue.Offroad;
			vertAxis  = AxisValue.Mud;
			diagramView = new WheelsDiagram();
			diagramView.setPreferredSize(700, 600);
			
			disabler = new Disabler<GuiObjs>();
			disabler.setCareFor(GuiObjs.values());
			
			JPanel optionsPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0; c.gridwidth = 1;
			ButtonGroup bgh = new ButtonGroup();
			optionsPanel.add(new JLabel("Horizontal Axis: "), c);
			optionsPanel.add(createRadioH(GuiObjs.HorizAxesHighway, "Highway", bgh, AxisValue.Highway), c);
			optionsPanel.add(createRadioH(GuiObjs.HorizAxesOffroad, "Offroad", bgh, AxisValue.Offroad), c);
			optionsPanel.add(createRadioH(GuiObjs.HorizAxesMud    , "Mud"    , bgh, AxisValue.Mud    ), c);
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			optionsPanel.add(new JLabel(), c);
			
			c.weightx = 0; c.gridwidth = 1;
			ButtonGroup bgv = new ButtonGroup();
			optionsPanel.add(new JLabel("Vertical Axis: "), c);
			optionsPanel.add(createRadioV(GuiObjs.VertAxesHighway, "Highway", bgv, AxisValue.Highway), c);
			optionsPanel.add(createRadioV(GuiObjs.VertAxesOffroad, "Offroad", bgv, AxisValue.Offroad), c);
			optionsPanel.add(createRadioV(GuiObjs.VertAxesMud    , "Mud"    , bgv, AxisValue.Mud    ), c);
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			optionsPanel.add(new JLabel(), c);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			contentPane.add(optionsPanel,BorderLayout.NORTH);
			contentPane.add(diagramView,BorderLayout.CENTER);
			
			setContentPane(contentPane);
			pack();
			setLocationRelativeTo(owner);
			
			updateGuiAccess();
			updateDiagram();
		}

		private JRadioButton createRadioV( GuiObjs go, String title, ButtonGroup bg, AxisValue axisValue) {
			return SnowRunner.createRadioButton(title, bg, true, vertAxis == axisValue, disabler, go, e->{ vertAxis = axisValue; updateGuiAccess(); updateDiagram(); });
		}

		private JRadioButton createRadioH(GuiObjs go, String title, ButtonGroup bg, AxisValue axisValue) {
			return SnowRunner.createRadioButton(title, bg, true, horizAxis == axisValue, disabler, go, e->{ horizAxis = axisValue; updateGuiAccess(); updateDiagram(); });
		}
		
		void showDialog() {
			setVisible(true);
		}

		private void updateGuiAccess() {
			disabler.setEnable(go->{
				switch (go) {
				case HorizAxesHighway: return vertAxis !=AxisValue.Highway;
				case HorizAxesOffroad: return vertAxis !=AxisValue.Offroad;
				case HorizAxesMud    : return vertAxis !=AxisValue.Mud    ;
				case VertAxesHighway : return horizAxis!=AxisValue.Highway;
				case VertAxesOffroad : return horizAxis!=AxisValue.Offroad;
				case VertAxesMud     : return horizAxis!=AxisValue.Mud    ;
				}
				return null;
			});
		}

		private void updateDiagram() {
			HashMap<Float,HashMap<Float,WheelsDiagram.DataPoint>> dataPointsMap = new HashMap<>();
			for (CompatibleWheelsPanel.CWTableModel.RowItem wheel : data) {
				Float x = getValue(wheel.tire,horizAxis);
				Float y = getValue(wheel.tire,vertAxis);
				if (x!=null && y!=null) {
					HashMap<Float, WheelsDiagram.DataPoint> yMap = dataPointsMap.get(x);
					if (yMap==null) dataPointsMap.put(x, yMap = new HashMap<>());
					WheelsDiagram.DataPoint dataPoint = yMap.get(y);
					if (dataPoint==null) yMap.put(y, dataPoint = new WheelsDiagram.DataPoint(x, y));
					dataPoint.add(wheel, language);
				}
			}
			
			Vector<WheelsDiagram.DataPoint> dataPointsVec = new Vector<>();
			dataPointsMap.forEach((x,yMap)->dataPointsVec.addAll(yMap.values()));
			
			diagramView.setData(dataPointsVec,true,true, horizAxis.toString(), vertAxis.toString());
		}

		private Float getValue(TruckTire tire, AxisValue value) {
			switch (value) {
			case Highway: return tire.frictionHighway;
			case Offroad: return tire.frictionOffroad;
			case Mud    : return tire.frictionMud;
			}
			return null;
		}

		private static class WheelsDiagram extends ZoomableCanvas<WheelsDiagram.ViewState> {
		
			private static final long serialVersionUID = 4384634067065873277L;
			private static final Color COLOR_AXIS = new Color(0x70000000,true);
			private static final Color COLOR_CONTOUR = Color.BLACK;
			private static final Color COLOR_FILL = Color.YELLOW;
			private static final Color COLOR_FILL_PARETO = new Color(0x00C6FF);
			private static final Color COLOR_FILL_HOVERED = Color.GREEN;
			private static final Color COLOR_DIAGRAM_BACKGROUND = Color.WHITE;
			private static final Color COLOR_TEXTBOX_BACKGROUND = new Color(0xFFFFDD);
			private static final Color COLOR_TEXT = Color.BLACK;
			private static final Color COLOR_TEXT_NOTPARETO = new Color(0x7F7F7F);
			
			private Vector<DataPoint> dataPoints;
			private Float minX;
			private Float maxX;
			private Float minY;
			private Float maxY;
			private final HashMap<DataPoint, DataPointTextBox> textBoxes;
			private final HashMap<DataPoint, Point> posMarkers;
			private final HashSet<DataPoint> paretoSet;
			private DataPoint hoveredDataPoint;
			private final TextBox axisHLabel;
			private final TextBox axisVLabel;
			
			WheelsDiagram() {
				textBoxes = new HashMap<>();
				posMarkers = new HashMap<>();
				paretoSet = new HashSet<>();
				axisHLabel = new TextBox(TextBox.Anchor.TopRight, "Horizontal");
				axisVLabel = new TextBox(TextBox.Anchor.TopLeft , "Vertical");
				axisHLabel.setOffset(0,5);
				axisVLabel.setOffset(5,0);
				axisHLabel.setColors(null, null, Color.GRAY);
				axisVLabel.setColors(null, null, Color.GRAY);
				setData(null,true,true, "<Horizontal>", "<Vertical>");
				activateMapScale(COLOR_AXIS, "units", false);
				activateAxes(COLOR_AXIS, true,true,true,true);
			}
		
			void setData(Vector<DataPoint> dataPoints, boolean isXPositiveBetter, boolean isYPositiveBetter, String axisHLabel, String axisVLabel) {
				textBoxes.clear();
				posMarkers.clear();
				paretoSet.clear();
				hoveredDataPoint = null;
				
				this.dataPoints = dataPoints;
				this.axisHLabel.setText(axisHLabel);
				this.axisVLabel.setText(axisVLabel);
				
				minX = null;
				maxX = null;
				minY = null;
				maxY = null;
				if (dataPoints!=null) {
					paretoSet.addAll(dataPoints);
					for (DataPoint dataPoint:dataPoints) {
						minX = minX==null ? dataPoint.x : Math.min(minX, dataPoint.x); 
						minY = minY==null ? dataPoint.y : Math.min(minY, dataPoint.y); 
						maxX = maxX==null ? dataPoint.x : Math.max(maxX, dataPoint.x); 
						maxY = maxY==null ? dataPoint.y : Math.max(maxY, dataPoint.y);
						
						for (DataPoint paretoDataPoint:paretoSet)
							if (isBelow(dataPoint, paretoDataPoint, isXPositiveBetter, isYPositiveBetter)) {
								paretoSet.remove(dataPoint);
								break;
							}
					}
				}
				reset();
			}
		
			private boolean isBelow(DataPoint dataPoint, DataPoint paretoDataPoint, boolean isXPositiveBetter, boolean isYPositiveBetter) {
				float x1 = dataPoint.x;
				float y1 = dataPoint.y;
				float x2 = paretoDataPoint.x;
				float y2 = paretoDataPoint.y;
				
				if (x1==x2 && y1==y2) return false;
				if ((isXPositiveBetter && x1>x2) || (!isXPositiveBetter && x1<x2)) return false;
				if ((isYPositiveBetter && y1>y2) || (!isYPositiveBetter && y1<y2)) return false;
				return true;
			}
		
			private boolean isOver(int x, int y, DataPoint dataPoint)
			{
				return isOverMarker(x, y, dataPoint) || isOverTextBox(x, y, dataPoint);
			}

			private boolean isOverMarker(int x, int y, DataPoint dataPoint)
			{
				if (dataPoint==null) return false;
				Point p = posMarkers.get(dataPoint);
				int dist_squared = (x-p.x)*(x-p.x) + (y-p.y)*(y-p.y);
				return dist_squared < 10*10;
			}

			private boolean isOverTextBox(int x, int y, DataPoint dataPoint)
			{
				if (dataPoint==null) return false;
				return textBoxes.get(dataPoint).isOver(x,y);
			}
			
			private interface DataPointPredicate
			{
				boolean test(int x, int y, DataPoint dataPoint);
			}
			
			private DataPoint findDataPoint(int x, int y, DataPointPredicate predicate)
			{
				for (DataPoint dataPoint: dataPoints)
					if (predicate.test(x, y, dataPoint))
						return dataPoint;
				return null;
			}
		
			@Override public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				DataPoint overMarker = findDataPoint(x,y,this::isOverMarker);
				if ((overMarker!=null && overMarker!=hoveredDataPoint) || (overMarker==null && !isOverTextBox(x,y,hoveredDataPoint))) {
					hoveredDataPoint = overMarker!=null ? overMarker : findDataPoint(x,y,this::isOver);
					repaint();
				}
			}
		
			@Override public void mouseEntered(MouseEvent e) {
				hoveredDataPoint = findDataPoint(e.getX(),e.getY(),this::isOver);
				repaint();
			}
		
			@Override public void mouseExited(MouseEvent e) {
				hoveredDataPoint = null;
				repaint();
			}
		
			@Override
			protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
				if (!(g instanceof Graphics2D || !viewState.isOk()))
					return;
				
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
				
				Shape origClip = g2.getClip();
				g2.setClip(x, y, width, height);
				
				if (dataPoints!=null && minX!=null && maxX!=null && minY!=null && maxY!=null) {
					drawDiagram(g2);
					
					for (int stage=DataPointTextBox.MIN_STAGE; stage<=DataPointTextBox.MAX_STAGE; stage++)
						for (DataPoint dataPoint:dataPoints)
							if (dataPoint!=hoveredDataPoint && !paretoSet.contains(dataPoint))
								drawDataPointTextBox(g2, dataPoint, false, false, stage);
					
					for (int stage=DataPointTextBox.MIN_STAGE; stage<=DataPointTextBox.MAX_STAGE; stage++)
						for (DataPoint dataPoint:dataPoints)
							if (dataPoint!=hoveredDataPoint && paretoSet.contains(dataPoint))
								drawDataPointTextBox(g2, dataPoint, false, true, stage);
					
					for (DataPoint dataPoint:dataPoints)
						if (dataPoint!=hoveredDataPoint)
							drawDataPointPosMarker(g2,dataPoint);
					
					if (hoveredDataPoint!=null) {
						drawDataPointTextBox(g2, hoveredDataPoint, true, paretoSet.contains(hoveredDataPoint), DataPointTextBox.NO_STAGE);
						drawDataPointPosMarker(g2, hoveredDataPoint);
					}
				}
				
				drawMapDecoration(g2, x, y, width, height);
				
				g2.setClip(origClip);
			}
		
			private void drawDiagram(Graphics2D g2) {
				float diagramMinX_u = Math.min(minX, 0);
				float diagramMinY_u = Math.min(minY, 0);
				float diagramMaxX_u = Math.max(maxX, 0);
				float diagramMaxY_u = Math.max(maxY, 0);
				
				int zeroX_px = viewState.convertPos_AngleToScreen_LongX(0);
				int zeroY_px = viewState.convertPos_AngleToScreen_LatY (0);
				int diagramMinX_px = viewState.convertPos_AngleToScreen_LongX(diagramMinX_u);
				int diagramMinY_px = viewState.convertPos_AngleToScreen_LatY (diagramMinY_u);
				int diagramMaxX_px = viewState.convertPos_AngleToScreen_LongX(diagramMaxX_u);
				int diagramMaxY_px = viewState.convertPos_AngleToScreen_LatY (diagramMaxY_u);
				int diagramWidth_px  = diagramMaxX_px-diagramMinX_px;
				int diagramHeight_px = diagramMinY_px-diagramMaxY_px; // pos. Y upwards
				
				g2.setColor(COLOR_DIAGRAM_BACKGROUND);
				g2.fillRect(diagramMinX_px-25, diagramMaxY_px-25, diagramWidth_px+50, diagramHeight_px+50);
				
				Stroke origStroke = g2.getStroke();
				g2.setStroke(new BasicStroke(2f));
				g2.setColor(COLOR_AXIS);
				g2.drawLine(diagramMinX_px-20, zeroY_px  , diagramMaxX_px+20, zeroY_px);
				g2.drawLine(diagramMaxX_px+5 , zeroY_px+5, diagramMaxX_px+20, zeroY_px);
				g2.drawLine(diagramMaxX_px+5 , zeroY_px-5, diagramMaxX_px+20, zeroY_px);
				g2.drawLine(zeroX_px  , diagramMinY_px+20, zeroX_px, diagramMaxY_px-20);
				g2.drawLine(zeroX_px+5, diagramMaxY_px-5 , zeroX_px, diagramMaxY_px-20);
				g2.drawLine(zeroX_px-5, diagramMaxY_px-5 , zeroX_px, diagramMaxY_px-20);
				g2.setStroke(origStroke);
				
				axisHLabel.setPos(diagramMaxX_px, zeroY_px);
				axisHLabel.draw(g2);
				axisVLabel.setPos(zeroX_px, diagramMaxY_px);
				axisVLabel.draw(g2);
			}
			
			private void drawDataPointTextBox(Graphics2D g2, DataPoint dataPoint, boolean isHovered, boolean isInParetoSet, int stage) {
				int dataPointX_px = viewState.convertPos_AngleToScreen_LongX(dataPoint.x);
				int dataPointY_px = viewState.convertPos_AngleToScreen_LatY (dataPoint.y);
				DataPointTextBox textBox = textBoxes.get(dataPoint);
				if (textBox==null) {
					textBox = new DataPointTextBox(dataPoint.getTextBox());
					textBoxes.put(dataPoint, textBox);
				}
				textBox.draw(g2, dataPointX_px, dataPointY_px, isHovered, isInParetoSet, stage);
			}
			
			private void drawDataPointPosMarker(Graphics2D g2, DataPoint dataPoint) {
				Point p = posMarkers.get(dataPoint);
				if (p == null)
					posMarkers.put(dataPoint, p = new Point());
				p.x = viewState.convertPos_AngleToScreen_LongX(dataPoint.x);
				p.y = viewState.convertPos_AngleToScreen_LatY (dataPoint.y);
				Color fillColor = dataPoint==hoveredDataPoint ? COLOR_FILL_HOVERED : paretoSet.contains(dataPoint) ? COLOR_FILL_PARETO : COLOR_FILL;
				drawFilledCircle(g2, p.x, p.y, 3, fillColor, COLOR_CONTOUR);
			}
		
			private void drawFilledCircle(Graphics2D g2, int x, int y, int radius, Color fillColor, Color contourColor) {
				g2.setColor(fillColor);
				g2.fillOval(x-radius, y-radius, radius*2+1, radius*2+1);
				g2.setColor(contourColor);
				g2.drawOval(x-radius, y-radius, radius*2, radius*2);
			}
		
			@Override
			protected ViewState createViewState() {
				return new ViewState();
			}
			
			class ViewState extends ZoomableCanvas.ViewState {
			
				private ViewState() {
					super(WheelsDiagram.this, 0.1f);
					setPlainMapSurface();
					//setVertAxisDownPositive(true);
					//debug_showChanges_scalePixelPerLength = true;
				}
				
				@Override
				protected void determineMinMax(MapLatLong min, MapLatLong max) {
					min.longitude_x = (double) (minX==null ? 0   : Math.min(minX, 0));
					min.latitude_y  = (double) (minY==null ? 0   : Math.min(minY, 0));
					max.longitude_x = (double) (maxX==null ? 100 : Math.max(maxX, 0));
					max.latitude_y  = (double) (maxY==null ? 100 : Math.max(maxY, 0));
					double overSize = Math.max(max.longitude_x-min.longitude_x, max.latitude_y-min.latitude_y)*0.1;
					min.longitude_x -= overSize;
					min.latitude_y  -= overSize;
					max.longitude_x += overSize;
					max.latitude_y  += overSize;
				}
			
			}
			
			private static class DataPointTextBox extends ZoomableCanvas.TextBox
			{
				static final int NO_STAGE = -1;
				static final int MIN_STAGE = 0;
				static final int MAX_STAGE = 3;
				
				DataPointTextBox(String... texts) {
					super(Anchor.BottomLeft, texts);
					setOffset(20, -10);
				}
				
				void draw(Graphics2D g2, int dataPointX_px, int dataPointY_px, boolean isHovered, boolean isInParetoSet, int stage)
				{	
					setPos(dataPointX_px,dataPointY_px);
					Color borderColor = isHovered || isInParetoSet ? COLOR_CONTOUR : COLOR_TEXT_NOTPARETO;
					Color   fillColor = isHovered                  ? COLOR_TEXTBOX_BACKGROUND : Color.WHITE;
					Color   textColor = isHovered || isInParetoSet ? COLOR_TEXT : COLOR_TEXT_NOTPARETO;
					setColors(borderColor, fillColor, textColor);
					
					g2.setColor(borderColor);
					g2.drawLine(this.x, this.y, this.x+20, this.y-10);
					
					if (stage<0)
					{
						g2.setColor(borderColor);
						g2.drawLine(this.x, this.y, this.x+20, this.y-10);
						super.draw(g2);
					}
					else if (stage==0)
					{
						g2.setColor(borderColor);
						g2.drawLine(this.x, this.y, this.x+20, this.y-10);
					}
					else
					{
						super.draw(g2, stage-1);
					}
				}
				
				boolean isOver(int x, int y)
				{
					ValueContainer<Boolean> isOver = new ValueContainer<>(false);
					forEachRow((i,boxX,boxY,boxW,boxH,strX,strY) -> {
						isOver.value = new Rectangle(this.x+boxX, this.y+boxY, boxW, boxH).contains(x,y);
						return !isOver.value;
					});
					return isOver.value;
				}
			}
			
			private static class DataPoint {
				
				final float x,y;
				final HashMap<String,HashSet<Integer>> wheels;
				
				DataPoint(float x, float y) {
					this.x = x;
					this.y = y;
					wheels = new HashMap<>();
				}
				
				void add(CompatibleWheelsPanel.CWTableModel.RowItem wheel, Language language) {
					String name = SnowRunner.solveStringID(wheel.tire.gameData.name_StringID, language);
					Integer size = wheel.getSize();
					
					HashSet<Integer> sizes = wheels.get(name);
					if (sizes==null) wheels.put(name, sizes = new HashSet<>());
					if (size!=null) sizes.add(size);
				}
		
				String[] getTextBox() {
					Vector<String> names = new Vector<>(wheels.keySet());
					names.sort(null);
					String[] texts = new String[names.size()];
					
					for (int i=0; i<names.size(); i++) {
						String name = names.get(i);
						texts[i] = name;
						
						HashSet<Integer> sizes = wheels.get(name);
						if (!sizes.isEmpty()) {
							Vector<Integer> sizesVec = new Vector<>(sizes);
							sizesVec.sort(null);
							Iterable<String> it = ()->sizesVec.stream().map(size->size+"\"").iterator();
							texts[i] += String.format(" (%s)", String.join(", ", it));
						}
					}
					return texts;
				}
			}
		}
	}
}