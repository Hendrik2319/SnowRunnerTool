package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.JMenuItem;
import javax.swing.JTable;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.Data.UserDefinedValues;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SaveGameListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons.SpecialTruckAddonList;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;
import net.schwarzbaer.java.games.snowrunner.TruckAssignToDLCDialog;

public class TruckTableModel extends VerySimpleTableModel<Truck> implements SaveGameListener, TruckToDLCAssignmentListener {

	private static final Color COLOR_FG_DLCTRUCK    = new Color(0x0070FF);
	private static final Color COLOR_FG_OWNEDTRUCK  = new Color(0x00AB00);
	private enum Edit { UD_DiffLock, UD_AWD }
	
	private SaveGame saveGame;
	private Truck clickedItem;
	private HashMap<String, String> truckToDLCAssignments;
	private final UserDefinedValues userDefinedValues;

	public TruckTableModel(Window mainWindow, Controllers controllers, SpecialTruckAddons specialTruckAddons, UserDefinedValues udv) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID( "ID"       , "ID"                   ,               String.class, 160,             null,   null,      null, false, row -> ((Truck)row).id),
				new ColumnID( "DLC"      , "DLC"                  ,               String.class,  80,             null,   null,      null, false, row -> ((Truck)row).dlcName),
				new ColumnID( "Country"  , "Country"              ,      Truck.  Country.class,  50,             null, CENTER,      null, false, row -> ((Truck)row).country),
				new ColumnID( "Type"     , "Type"                 ,      Truck.TruckType.class,  80,             null, CENTER,      null, false, row -> ((Truck)row).type),
				new ColumnID( "Name"     , "Name"                 ,               String.class, 160,             null,   null,      null,  true, row -> ((Truck)row).name_StringID),
				new ColumnID( "Owned"    , "Owned"                ,              Integer.class,  50,             null, CENTER,      null, false, (row,model) -> getOwnedCount((Truck)row,(TruckTableModel)model)), 
				new ColumnID( "DLData"   , "DiffLock (from Data)" ,   Truck.DiffLockType.class, 110,             null, CENTER,      null, false, row -> ((Truck)row).diffLockType),
				new ColumnID( "DLUser"   , "DiffLock (by User)"   ,  Truck.UDV.ItemState.class, 100, Edit.UD_DiffLock, CENTER,      null, false, row -> udv.getTruckValues(((Truck)row).id).realDiffLock),
				new ColumnID( "DLTool"   , "DiffLock (by Tool)"   ,  Truck.UDV.ItemState.class, 100,             null, CENTER,      null, false, row -> getInstState((Truck)row, t->t.hasCompatibleDiffLock, t->t.defaultDiffLock, addon->addon.enablesDiffLock)),
				new ColumnID( "AWDData"  , "AWD (from Data)"      ,               String.class,  95,             null, CENTER,      null, false, row -> "??? t.b.d."),
				new ColumnID( "AWDUser"  , "AWD (by User)"        ,  Truck.UDV.ItemState.class,  85,      Edit.UD_AWD, CENTER,      null, false, row -> udv.getTruckValues(((Truck)row).id).realAWD),
				new ColumnID( "AWDTool"  , "AWD (by Tool)"        ,  Truck.UDV.ItemState.class,  85,             null, CENTER,      null, false, row -> getInstState((Truck)row, t->t.hasCompatibleAWD, t->t.defaultAWD, addon->addon.enablesAllWheelDrive)),
				new ColumnID( "AutoWinch", "AutomaticWinch"       ,              Boolean.class,  90,             null,   null,      null, false, row -> ((Truck)row).hasCompatibleAutomaticWinch),
				new ColumnID( "MetalD"   , "Metal Detector"       ,              Boolean.class,  90,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.metalDetectors   )),
				new ColumnID( "Seismic"  , "Seismic Vibrator"     ,              Boolean.class,  90,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.seismicVibrators )),
				new ColumnID( "BigCrane" , "Big Crane"            ,              Boolean.class,  60,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.bigCranes        )),
				new ColumnID( "MiniCrane", "Mini Crane"           ,              Boolean.class,  60,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.miniCranes       )),
				new ColumnID( "LogLift"  , "Log Lift"             ,              Boolean.class,  50,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.logLifts         )),
				new ColumnID( "FuelCap"  , "Fuel Capacity"        ,              Integer.class,  80,             null,   null,    "%d L", false, row -> ((Truck)row).fuelCapacity),
				new ColumnID( "WheelSizs", "Wheel Sizes"          ,               String.class,  80,             null,   null,      null, false, row -> joinWheelSizes(((Truck)row).compatibleWheels)),
				new ColumnID( "WheelTyps", "Wheel Types"          ,               String.class, 280,             null,   null,      null, (row,lang) -> getWheelCategories((Truck)row,lang)),
				new ColumnID( "WhHigh"   , "[W] Highway"          ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionHighway)),
				new ColumnID( "WhOffr"   , "[W] Offroad"          ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionOffroad)),
				new ColumnID( "WhMud"    , "[W] Mud"              ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionMud)),
				new ColumnID( "Price"    , "Price"                ,              Integer.class,  60,             null,   null,   "%d Cr", false, row -> ((Truck)row).price), 
				new ColumnID( "UnlExpl"  , "Unlock By Exploration",              Boolean.class, 120,             null,   null,      null, false, row -> ((Truck)row).unlockByExploration), 
				new ColumnID( "UnlRank"  , "Unlock By Rank"       ,              Integer.class, 100,             null, CENTER, "Rank %d", false, row -> ((Truck)row).unlockByRank), 
				new ColumnID( "Desc"     , "Description"          ,               String.class, 200,             null,   null,      null,  true, row -> ((Truck)row).description_StringID), 
				new ColumnID( "DefEngine", "Default Engine"       ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultEngine, ((Truck)row).defaultEngine_ItemID, lang)),
				new ColumnID( "DefGearbx", "Default Gearbox"      ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultGearbox, ((Truck)row).defaultGearbox_ItemID, lang)),
				new ColumnID( "DefSusp"  , "Default Suspension"   ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultSuspension, ((Truck)row).defaultSuspension_ItemID, lang)),
				new ColumnID( "DefWinch" , "Default Winch"        ,               String.class, 130,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultWinch, ((Truck)row).defaultWinch_ItemID, lang)),
				new ColumnID( "DefDifLck", "Default DiffLock"     ,               String.class,  95,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultDiffLock, lang)),
				new ColumnID( "DefAWD"   , "Default AWD"          ,               String.class,  90,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultAWD, lang)),
				new ColumnID( "UpgrWinch", "Upgradable Winch"     ,              Boolean.class, 110,             null,   null,      null, false, row -> ((Truck)row).isWinchUpgradable),
		//		new ColumnID( "MaxWhWoSp", "Max. WheelRadius Without Suspension", String.class, 200,             null,   null,      null, false, row -> ((Truck)row).maxWheelRadiusWithoutSuspension),
				new ColumnID( "Image"    , "Image"                ,               String.class, 130,             null,   null,      null, false, row -> ((Truck)row).image),
		});
		this.userDefinedValues = udv;
		saveGame = null;
		connectToGlobalData(data->data.trucks.values());
		setInitialRowOrder(Comparator.<Truck,String>comparing(truck->truck.id));
		
		
		coloring.addForegroundRowColorizer(truck->{
			if (truck==null)
				return null;
			
			if (saveGame!=null && saveGame.ownedTrucks!=null && saveGame.ownedTrucks.containsKey(truck.id))
				return COLOR_FG_OWNEDTRUCK;
			
			if (truck.dlcName!=null)
				return COLOR_FG_DLCTRUCK;
			
			return null;
		});
		
		coloring.setBackgroundColumnColoring(true, Truck.UDV.ItemState.class, state->{
			switch (state) {
			case None:
				return COLOR_BG_FALSE;
			case Able:
			case Installed:
			case Permanent:
				return COLOR_BG_TRUE;
			}
			return null;
		});
		
		controllers.saveGameListeners.add(this, this);
		controllers.specialTruckAddonsListeners.add(this, (list,change)->{
			String id = null;
			switch (list) {
			case MetalDetectors  : id = "MetalD"; break;
			case SeismicVibrators: id = "Seismic"; break;
			case BigCranes       : id = "BigCrane"; break;
			case LogLifts        : id = "MiniCrane"; break;
			case MiniCranes      : id = "LogLift"; break;
			}
			if (id!=null)
				fireTableColumnUpdate(findColumnByID(id));
		});
		
	}
	
	private static Integer getOwnedCount(Truck truck, TruckTableModel model) {
		if (truck==null) return null;
		if (model==null) return null;
		if (model.saveGame==null) return null;
		if (model.saveGame.ownedTrucks==null) return null;
		return model.saveGame.ownedTrucks.get(truck.id);
	}

	private static Float getMaxWheelValue(Truck truck, Function<TruckTire,Float> getTireValue) {
		Float max = null;
		for (CompatibleWheel cw : truck.compatibleWheels)
			if (cw.wheelsDef!=null)
				for (TruckTire tire : cw.wheelsDef.truckTires) {
					Float value = getTireValue.apply(tire);
					max = max==null ? value : value==null ? max : Math.max(max,value);
				}
		return max;
	}
	
	private static String getWheelCategories(Truck truck, Language language) {
		HashSet<String> tireTypes_StringID = new HashSet<>();
		for (CompatibleWheel cw : truck.compatibleWheels)
			if (cw.wheelsDef!=null)
				for (TruckTire tire : cw.wheelsDef.truckTires)
					if (tire.tireType_StringID!=null)
						tireTypes_StringID.add(tire.tireType_StringID);
		Vector<String> vec = new Vector<>(tireTypes_StringID);
		vec.sort(Comparator.comparing(TruckTire::getTypeOrder).thenComparing(Comparator.naturalOrder()));
		Iterable<String> it = () -> vec.stream().map(stringID->SnowRunner.solveStringID(stringID, language)).iterator();
		return String.join(", ", it);
	}

	private static Truck.UDV.ItemState getInstState(
			Truck truck,
			Function<Truck,Boolean> isAddonCategoryInstallable,
			Function<Truck,TruckAddon> getDefaultAddon,
			Function<TruckAddon,Boolean> addonEnablesFeature
	) {
		if (truck==null) return null;
		
		Boolean bIsAddonInstallable = isAddonCategoryInstallable.apply(truck);
		if (bIsAddonInstallable==null || !bIsAddonInstallable.booleanValue())
			return null; // None || Permanent 
		
		TruckAddon defaultAddon = getDefaultAddon.apply(truck);
		Boolean bAddonEnablesFeature = defaultAddon==null ? false : addonEnablesFeature.apply(defaultAddon);
		if (bAddonEnablesFeature==null || !bAddonEnablesFeature.booleanValue())
			return Truck.UDV.ItemState.Able;
			
		return Truck.UDV.ItemState.Installed;
	}

	private static String joinWheelSizes(CompatibleWheel[] compatibleWheels) {
		HashSet<String> set = Arrays
				.stream(compatibleWheels)
				.<String>map(cw->String.format("%d\"", cw.getSize()))
				.collect(HashSet<String>::new, HashSet<String>::add, HashSet<String>::addAll);
		Vector<String> vector = new Vector<>(set);
		vector.sort(null);
		return String.join(", ", vector);
	}

	private static boolean hasCompatibleSpecialAddon(Truck truck, SpecialTruckAddonList addonList) {
		for (Vector<TruckAddon> list : truck.compatibleTruckAddons.values())
			for (TruckAddon addon : list)
				if (addonList.contains(addon))
					return true;
		return false;
	}

	private static class ColumnID extends VerySimpleTableModel.ColumnID {

		final Edit editMarker;

		private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object, ColumnType> getValue) {
			super(ID, name, columnClass, prefWidth, horizontalAlignment, format, useValueAsStringID, getValue);
			this.editMarker = editMarker;
		}

		private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
			super(ID, name, columnClass, prefWidth, horizontalAlignment, format, useValueAsStringID, getValue);
			this.editMarker = editMarker;
		}

		private ColumnID(String ID, String name, Class<String> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
			super(ID, name, columnClass, prefWidth, horizontalAlignment, format, getValue);
			this.editMarker = editMarker;
		}
		
	}

	@Override
	public void reconfigureAfterTableStructureUpdate() {
		super.reconfigureAfterTableStructureUpdate();
		table.setDefaultEditor(
				Truck.UDV.ItemState.class,
				new Tables.ComboboxCellEditor<>(
						SnowRunner.addNull(Truck.UDV.ItemState.values())
				)
		);
	}

	@Override protected boolean isCellEditable(int rowIndex, int columnIndex, VerySimpleTableModel.ColumnID columnID_) {
		ColumnID columnID = (ColumnID)columnID_;
		return columnID.editMarker!=null;
	}

	@Override
	protected void setValueAt(Object aValue, int rowIndex, int columnIndex, VerySimpleTableModel.ColumnID columnID_) {
		ColumnID columnID = (ColumnID)columnID_;
		if (columnID.editMarker==null) return;
		
		Truck row = getRow(rowIndex);
		if (row==null) return;
		
		Truck.UDV values = userDefinedValues.getOrCreateTruckValues(row.id);
		
		switch (columnID.editMarker) {
		case UD_AWD     : values.realAWD      = (Truck.UDV.ItemState)aValue; break;
		case UD_DiffLock: values.realDiffLock = (Truck.UDV.ItemState)aValue; break;
		}
		
		userDefinedValues.write();
	}

	@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
		super.modifyTableContextMenu(table_, contextMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem miAssignToDLC = contextMenu.add(SnowRunner.createMenuItem("Assign truck to an official DLC", true, e->{
			if (clickedItem==null || truckToDLCAssignments==null) return;
			TruckAssignToDLCDialog dlg = new TruckAssignToDLCDialog(mainWindow, clickedItem, language, truckToDLCAssignments);
			boolean assignmentsChanged = dlg.showDialog();
			if (assignmentsChanged)
				controllers.truckToDLCAssignmentListeners.updateAfterAssignmentsChange();
		}));
		
		contextMenu.addContextMenuInvokeListener((table, x, y) -> {
			int rowV = x==null || y==null ? -1 : table.rowAtPoint(new Point(x,y));
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			clickedItem = rowM<0 ? null : getRow(rowM);
			
			miAssignToDLC.setEnabled(clickedItem!=null && truckToDLCAssignments!=null);
			
			miAssignToDLC.setText(
				clickedItem==null
				? "Assign truck to an official DLC"
				: String.format("Assign \"%s\" to an official DLC", SnowRunner.getTruckLabel(clickedItem,language))
			);
		});
		
		controllers.truckToDLCAssignmentListeners.add(this, this);
	}

	@Override public void updateAfterAssignmentsChange() {}
	@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
		this.truckToDLCAssignments = truckToDLCAssignments;
	}

	@Override public void setSaveGame(SaveGame saveGame) {
		this.saveGame = saveGame;
		table.repaint();
	}
	
}