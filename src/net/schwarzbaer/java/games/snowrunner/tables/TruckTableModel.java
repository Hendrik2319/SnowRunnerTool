package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JTable;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.Data.UserDefinedValues;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TextOutput;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;
import net.schwarzbaer.java.games.snowrunner.TruckAssignToDLCDialog;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ColumnID.TableModelBasedBuilder;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ColumnID.VerboseTableModelBasedBuilder;

public class TruckTableModel extends VerySimpleTableModel<Truck> {

	private static final Color BG_COLOR__TRUCK_CAN_USE_TRAILER = new Color(0xCEFFC5);

	private enum Edit { UD_DiffLock, UD_AWD }
	
	private static boolean enableOwnedTrucksHighlighting = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableOwnedTrucksHighlighting, true);
	private static boolean enableDLCTrucksHighlighting   = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableDLCTrucksHighlighting  , true);  
	private static final String ID_MetalD    = "MetalD"   ;
	private static final String ID_Seismic   = "Seismic"  ;
	private static final String ID_BigCrane  = "BigCrane" ;
	private static final String ID_MiniCrane = "MiniCrane";
	private static final String ID_LogLift   = "LogLift"  ;
	private static final String ID_LongLogs  = "LongLogs" ;
	private static final String ID_MedLogs   = "MedLogs"  ;
	private static final String ID_ShortLogs = "ShortLogs";
	private static final String ID_MedLogsB  = "MedLogsB" ;
	
	private SaveGame saveGame;
	private Truck clickedItem;
	private HashMap<String, String> truckToDLCAssignments;
	private final UserDefinedValues userDefinedValues;
	private Data data;
	private final SpecialTruckAddons specialTruckAddons;
	private Function<Truck, Color> colorizeTrucksByTrailers;

	public TruckTableModel(Window mainWindow, Controllers controllers, SpecialTruckAddons specialTruckAddons, UserDefinedValues udv) {
		super(mainWindow, controllers, new VerySimpleTableModel.ColumnID[] {
				new ColumnID( "ID"       , "ID"                   ,               String.class, 160,             null,   null,      null, false, row -> ((Truck)row).id),
				new ColumnID( "UpdateLvl", "Update Level"         ,               String.class,  80,             null,   null,      null, false, row -> ((Truck)row).updateLevel),
				new ColumnID( "DLC"      , "DLC"                  ,               String.class, 170,             null,   null,      null, false, (row,model) -> getDLC(row,model)),
				new ColumnID( "Country"  , "Country"              ,      Truck.  Country.class,  50,             null, CENTER,      null, false, row -> ((Truck)row).gameData.country),
				new ColumnID( "Type"     , "Type"                 ,      Truck.TruckType.class,  80,             null, CENTER,      null, false, row -> ((Truck)row).type),
				new ColumnID( "Name"     , "Name"                 ,               String.class, 160,             null,   null,      null,  true, row -> ((Truck)row).gameData.name_StringID),
				new ColumnID( "Owned"    , "Owned"                ,              Integer.class,  50,             null, CENTER,      null, false, (row,model) -> getOwnedCount(row,model)),
				new ColumnID( "InWareHs" , "In Warehouse"         ,              Integer.class,  80,             null, CENTER,      null, false, (row,model) -> getTrucksInWarehouse(row, model, null), (row, model, textOutput) -> getTrucksInWarehouse(row, model, textOutput)), 
				new ColumnID( "InGarage" , "In Garage"            ,              Integer.class,  60,             null, CENTER,      null, false, (row,model) -> getTrucksInGarage   (row, model, null), (row, model, textOutput) -> getTrucksInGarage   (row, model, textOutput)),
				new ColumnID( "DLData"   , "DiffLock (from Data)" ,   Truck.DiffLockType.class, 110,             null, CENTER,      null, false, row -> ((Truck)row).diffLockType),
				new ColumnID( "DLUser"   , "DiffLock (by User)"   ,  Truck.UDV.ItemState.class, 100, Edit.UD_DiffLock, CENTER,      null, false, row -> udv.getTruckValues(((Truck)row).id).realDiffLock),
				new ColumnID( "DLTool"   , "DiffLock (by Tool)"   ,  Truck.UDV.ItemState.class, 100,             null, CENTER,      null, false, row -> getInstState((Truck)row, t->t.hasCompatibleDiffLock, t->t.defaultDiffLock, addon->addon.enablesDiffLock)),
				new ColumnID( "AWDData"  , "AWD (from Data)"      ,               String.class,  95,             null, CENTER,      null, false, row -> "??? t.b.d."),
				new ColumnID( "AWDUser"  , "AWD (by User)"        ,  Truck.UDV.ItemState.class,  85,      Edit.UD_AWD, CENTER,      null, false, row -> udv.getTruckValues(((Truck)row).id).realAWD),
				new ColumnID( "AWDTool"  , "AWD (by Tool)"        ,  Truck.UDV.ItemState.class,  85,             null, CENTER,      null, false, row -> getInstState((Truck)row, t->t.hasCompatibleAWD, t->t.defaultAWD, addon->addon.enablesAllWheelDrive)),
				new ColumnID( "AutoWinch", "AutomaticWinch"       ,              Boolean.class,  90,             null,   null,      null, false, row -> ((Truck)row).hasCompatibleAutomaticWinch),
				new ColumnID(ID_MetalD   , "Metal Detector"       ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MetalDetector   ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MetalDetector   ) ),
				new ColumnID(ID_Seismic  , "Seismic Vibrator"     ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.SeismicVibrator ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.SeismicVibrator ) ),
				new ColumnID(ID_BigCrane , "Big Crane"            ,      Data.Capability.class,  60,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.BigCrane        ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.BigCrane        ) ),
				new ColumnID(ID_MiniCrane, "Mini Crane"           ,      Data.Capability.class,  60,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MiniCrane       ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MiniCrane       ) ),
				new ColumnID(ID_LogLift  , "Log Lift"             ,      Data.Capability.class,  50,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.LogLift         ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.LogLift         ) ),
				new ColumnID(ID_LongLogs , "Long Logs"            ,      Data.Capability.class,  60,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.LongLogs        ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.LongLogs        ) ),
				new ColumnID(ID_MedLogs  , "Medium Logs"          ,      Data.Capability.class,  75,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MediumLogs      ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MediumLogs      ) ),
				new ColumnID(ID_ShortLogs, "Short Logs"           ,      Data.Capability.class,  65,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.ShortLogs       ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.ShortLogs       ) ),
				new ColumnID(ID_MedLogsB , "Medium L. (Burnt)"    ,      Data.Capability.class,  95,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MediumLogs_burnt), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MediumLogs_burnt) ),
				new ColumnID( "FuelCap"  , "Fuel Capacity"        ,              Integer.class,  80,             null,   null,    "%d L", false, row -> ((Truck)row).fuelCapacity),
				new ColumnID( "WheelSizs", "Wheel Sizes"          ,               String.class,  80,             null,   null,      null, false, row -> joinWheelSizes(((Truck)row).compatibleWheels)),
				new ColumnID( "WheelTyps", "Wheel Types"          ,               String.class, 280,             null,   null,      null, (row,lang) -> getWheelCategories((Truck)row,lang)),
				new ColumnID( "WhHigh"   , "[W] Highway"          ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionHighway)),
				new ColumnID( "WhOffr"   , "[W] Offroad"          ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionOffroad)),
				new ColumnID( "WhMud"    , "[W] Mud"              ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionMud)),
				new ColumnID( "Price"    , "Price"                ,              Integer.class,  60,             null,   null,   "%d Cr", false, row -> ((Truck)row).gameData.price), 
				new ColumnID( "UnlExpl"  , "Unlock By Exploration",              Boolean.class, 120,             null,   null,      null, false, row -> ((Truck)row).gameData.unlockByExploration), 
				new ColumnID( "UnlRank"  , "Unlock By Rank"       ,              Integer.class, 100,             null, CENTER, "Rank %d", false, row -> ((Truck)row).gameData.unlockByRank), 
				new ColumnID( "Desc"     , "Description"          ,               String.class, 200,             null,   null,      null,  true, row -> ((Truck)row).gameData.description_StringID), 
				new ColumnID( "DefEngine", "Default Engine"       ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultEngine    , ((Truck)row).defaultEngine_ItemID, lang)),
				new ColumnID( "DefGearbx", "Default Gearbox"      ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultGearbox   , ((Truck)row).defaultGearbox_ItemID, lang)),
				new ColumnID( "DefSusp"  , "Default Suspension"   ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultSuspension, ((Truck)row).defaultSuspension_ItemID, lang)),
				new ColumnID( "DefWinch" , "Default Winch"        ,               String.class, 130,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultWinch     , ((Truck)row).defaultWinch_ItemID, lang)),
				new ColumnID( "DefDifLck", "Default DiffLock"     ,               String.class,  95,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultDiffLock  , lang)),
				new ColumnID( "DefAWD"   , "Default AWD"          ,               String.class,  90,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultAWD       , lang)),
				new ColumnID( "UpgrWinch", "Upgradable Winch"     ,              Boolean.class, 110,             null,   null,      null, false, row -> ((Truck)row).isWinchUpgradable),
		//		new ColumnID( "MaxWhWoSp", "Max. WheelRadius Without Suspension", String.class, 200,             null,   null,      null, false, row -> ((Truck)row).maxWheelRadiusWithoutSuspension),
				new ColumnID( "Image"    , "Image"                ,               String.class, 130,             null,   null,      null, false, row -> ((Truck)row).image),
				new ColumnID( "CargoSlts", "Cargo Slots"          ,              Integer.class,  70,             null, CENTER,      null, false, row -> ((Truck)row).gameData.cargoSlots),
				new ColumnID( "ExclCargo", "Excluded Cargo Types" ,               String.class, 150,             null,   null,      null, false, row -> SnowRunner.joinAddonIDs(((Truck)row).gameData.excludedCargoTypes,true)),
				new ColumnID( "ExclAddon", "Exclude Addons"       ,               String.class, 150,             null,   null,      null, false, row -> SnowRunner.joinAddonIDs(((Truck)row).gameData.excludeAddons,true)),
		//		new ColumnID( "Recall"   , "Recallable"           ,              Boolean.class,  60,             null,   null,      null, false, row -> ((Truck)row).gameData.recallable_obsolete),
		});
		this.specialTruckAddons = specialTruckAddons;
		this.userDefinedValues = udv;
		this.data = null;
		this.saveGame = null;
		colorizeTrucksByTrailers = null;
		
		connectToGlobalData(true, data->{
			this.data = data;
			if (this.data==null) return null;
			return this.data.trucks.values();
		});
		setInitialRowOrder(Comparator.<Truck,String>comparing(truck->truck.id));
		
		
		coloring.addForegroundRowColorizer(truck->{
			if (truck==null)
				return null;
			
			if (enableOwnedTrucksHighlighting && saveGame!=null && saveGame.playerOwnsTruck(truck))
				return SnowRunner.COLOR_FG_OWNEDTRUCK;
			
			if (enableDLCTrucksHighlighting && truck.updateLevel!=null)
				return SnowRunner.COLOR_FG_DLCTRUCK;
			
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
		
		finalizer.addSaveGameListener(saveGame_->{
			this.saveGame = saveGame_;
			table.repaint();
		});
		finalizer.addSpecialTruckAddonsListener((list,change)->{
			String id = null;
			switch (list) {
				case MetalDetector   : id = ID_MetalD   ; break;
				case SeismicVibrator : id = ID_Seismic  ; break;
				case BigCrane        : id = ID_BigCrane ; break;
				case MiniCrane       : id = ID_MiniCrane; break;
				case LogLift         : id = ID_LogLift  ; break;
				case LongLogs        : id = ID_LongLogs ; break;
				case MediumLogs      : id = ID_MedLogs  ; break;
				case ShortLogs       : id = ID_ShortLogs; break;
				case MediumLogs_burnt: id = ID_MedLogsB ; break;
			}
			if (id!=null)
				fireTableColumnUpdate(findColumnByID(id));
		});
		
		finalizer.addTruckToDLCAssignmentListener(new TruckToDLCAssignmentListener() {
			@Override public void updateAfterAssignmentsChange() {
				int colV = findColumnByID("DLC");
				if (colV>=0) fireTableColumnUpdate(colV);
			}
			@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
				TruckTableModel.this.truckToDLCAssignments = truckToDLCAssignments;
			}
		});
		
		finalizer.addFilterTrucksByTrailersListener(this::setTrailerForFilter);
	}

	public interface FilterTrucksByTrailersListener
	{
		void setFilter(Trailer trailer);
	}
	
	private void setTrailerForFilter(Trailer trailer)
	{
		if (colorizeTrucksByTrailers != null)
			coloring.removeBackgroundRowColorizer(colorizeTrucksByTrailers);
		
		if (trailer==null)
			colorizeTrucksByTrailers = null;
		else
			colorizeTrucksByTrailers = truck -> {
				if (trailer.usableBy.contains(truck))
					return BG_COLOR__TRUCK_CAN_USE_TRAILER;
				return null;
			};
		
		if (colorizeTrucksByTrailers != null)
			coloring.addBackgroundRowColorizer(colorizeTrucksByTrailers);
		
		table.repaint();
	}

	private static <Result> Result castNCall(Object row, VerySimpleTableModel<?> model, BiFunction<Truck,TruckTableModel,Result> action)
	{
		if (!(row   instanceof Truck          )) return null;
		if (!(model instanceof TruckTableModel)) return null;
		return action.apply((Truck) row, (TruckTableModel) model);
	}

	private static TableModelBasedBuilder<Data.Capability> createIsCapableFcn(SpecialTruckAddons.AddonCategory listID) {
		return (row,model) -> castNCall(row, model, (truck_, model_) -> {
			if (model_.data==null) return null;
			if (model_.specialTruckAddons==null) return null;
			if (listID==null) return null;
			
			return model_.data.isCapable(truck_, listID, model_.specialTruckAddons, null);
		});
	}

	private static VerboseTableModelBasedBuilder<Data.Capability> createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory listID) {
		return (row,model,textOutput) -> castNCall(row, model, (truck_, model_) -> {
			if (model_.data==null) return null;
			if (model_.specialTruckAddons==null) return null;
			if (listID==null) return null;
			
			return model_.data.isCapable(truck_, listID, model_.specialTruckAddons, textOutput);
		});
	}

	private static String getDLC(Object row, VerySimpleTableModel<?> model)
	{
		return castNCall(row, model, (truck_, model_) -> {
			if (model_.truckToDLCAssignments==null) return null;
			return model_.truckToDLCAssignments.get(truck_.id);
		});
	}
	
	private static Integer getOwnedCount(Object row, VerySimpleTableModel<?> model)
	{
		return castNCall(row, model, (truck_, model_) -> {
			if (model_.saveGame==null) return null;
			return model_.saveGame.getOwnedTruckCount(truck_);
		});
	}
	
	private static Integer getTrucksInWarehouse(Object row, VerySimpleTableModel<?> model, TextOutput textOutput)
	{
		return castNCall(row, model, (truck_, model_) -> {
			if (model_.saveGame==null) return null;
			return model_.saveGame.getTrucksInWarehouse(truck_, textOutput);
		});
	}
	
	private static Integer getTrucksInGarage(Object row, VerySimpleTableModel<?> model, TextOutput textOutput)
	{
		return castNCall(row, model, (truck_, model_) -> {
			if (model_.saveGame==null) return null;
			return model_.saveGame.getTrucksInGarage(truck_, textOutput);
		});
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

		private              ColumnID(String ID, String name, Class<String    > columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format,                             LanguageBasedStringBuilder getValue) {
			super(ID, name, columnClass, prefWidth, horizontalAlignment, format, getValue);
			this.editMarker = editMarker;
		}

		private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object, ColumnType> getValue, BiFunction<Object, TextOutput, ColumnType> getValue_verbose) {
			super(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, getValue, getValue_verbose);
			this.editMarker = editMarker;
		}

		private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue, VerboseTableModelBasedBuilder<ColumnType> getValue_verbose) {
			super(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, getValue, getValue_verbose);
			this.editMarker = editMarker;
		}

		private              ColumnID(String ID, String name, Class<String    > columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format,                             LanguageBasedStringBuilder getValue, VerboseLanguageBasedStringBuilder getValue_verbose) {
			super(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, getValue, getValue_verbose);
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

	@Override protected String getRowName(Truck row)
	{
		return SnowRunner.solveStringID(row, language);
	}

	@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
		super.modifyTableContextMenu(table_, contextMenu);
		
		contextMenu.addSeparator();
		
		contextMenu.add(SnowRunner.createMenuItem("Filter by Trailer -> context menu of any \"Trailers\" table", true, e->{}));
		contextMenu.add(SnowRunner.createMenuItem("Reset Trailer Filter", true, e->setTrailerForFilter(null)));
		
		contextMenu.addSeparator();
		
		JMenuItem miAssignToDLC = contextMenu.add(SnowRunner.createMenuItem("Assign truck to an official DLC", true, e->{
			if (clickedItem==null || truckToDLCAssignments==null) return;
			TruckAssignToDLCDialog dlg = new TruckAssignToDLCDialog(mainWindow, clickedItem, language, truckToDLCAssignments);
			boolean assignmentsChanged = dlg.showDialog();
			if (assignmentsChanged)
				finalizer.getControllers().truckToDLCAssignmentListeners.updateAfterAssignmentsChange();
		}));
		
		contextMenu.addSeparator();
		
		JCheckBoxMenuItem miEnableOwnedTrucksHighlighting, miEnableDLCTrucksHighlighting;
		contextMenu.add(miEnableOwnedTrucksHighlighting = SnowRunner.createCheckBoxMenuItem("Highlighting of owned trucks", enableOwnedTrucksHighlighting, null, true, ()->{
			enableOwnedTrucksHighlighting = !enableOwnedTrucksHighlighting;
			SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableOwnedTrucksHighlighting, enableOwnedTrucksHighlighting);
			if (table!=null) table.repaint();
		}));
		contextMenu.add(miEnableDLCTrucksHighlighting = SnowRunner.createCheckBoxMenuItem("Highlighting of DLC trucks", enableDLCTrucksHighlighting, null, true, ()->{
			enableDLCTrucksHighlighting = !enableDLCTrucksHighlighting;
			SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableDLCTrucksHighlighting, enableDLCTrucksHighlighting);
			if (table!=null) table.repaint();
		}));
		
		contextMenu.addContextMenuInvokeListener((table, x, y) -> {
			Point p = x==null || y==null ? null : new Point(x,y);
			int rowV = p==null ? -1 : table.rowAtPoint(p);
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			clickedItem = rowM<0 ? null : getRow(rowM);
			
			miAssignToDLC.setEnabled(clickedItem!=null && truckToDLCAssignments!=null);
			
			miAssignToDLC.setText(
				clickedItem==null
				? "Assign truck to an official DLC"
				: String.format("Assign \"%s\" to an official DLC", SnowRunner.getTruckLabel(clickedItem,language))
			);
			miEnableOwnedTrucksHighlighting.setSelected(enableOwnedTrucksHighlighting);
			miEnableDLCTrucksHighlighting  .setSelected(enableDLCTrucksHighlighting  );
		});
		
		//   why here?  ->  moved into constructor
		//finalizer.addTruckToDLCAssignmentListener(new TruckToDLCAssignmentListener() {
		//	@Override public void updateAfterAssignmentsChange() {}
		//	@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
		//		TruckTableModel.this.truckToDLCAssignments = truckToDLCAssignments;
		//	}
		//});
	}
}