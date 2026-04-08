package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.games.snowrunner.AssignToDLCDialog;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TextOutput;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ColumnID.TableModelBasedBuilder;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ColumnID.VerboseTableModelBasedBuilder;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.system.ClipboardTools;

public class TruckTableModel extends VerySimpleTableModel<Truck>
{
	private static final GetValueConverter<Truck,TruckTableModel> GET = new GetValueConverter<>(Truck.class, TruckTableModel.class, m->m.data);
	private static final Color BG_COLOR__TRUCK_CAN_USE_TRAILER = new Color(0xCEFFC5);
	private static final Color BG_COLOR__DISPLAYED_TRUCK = new Color(0xFFDF00);

	private enum Edit { UD_DiffLock, UD_AWD }
	
	//private static boolean enableOwnedTrucksHighlighting = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableOwnedTrucksHighlighting, true);
	//private static boolean enableDLCTrucksHighlighting   = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableDLCTrucksHighlighting  , true);
	private static final String ID_DLC       = "DLC";
	private static final String ID_HasImage  = "HasImage" ;
	private static final String ID_MetalD    = "MetalD"   ;
	private static final String ID_Seismic   = "Seismic"  ;
	private static final String ID_BigCrane  = "BigCrane" ;
	private static final String ID_MiniCrane = "MiniCrane";
	private static final String ID_LogLift   = "LogLift"  ;
	private static final String ID_LongLogs  = "LongLogs" ;
	private static final String ID_MedLogs   = "MedLogs"  ;
	private static final String ID_ShortLogs = "ShortLogs";
	private static final String ID_MedLogsB  = "MedLogsB" ;
	
	private Data data;
	private SaveGame saveGame;
	private Truck clickedItem;
	private Function<Truck, Color> colorizeTrucksByTrailers;
	private SaveGame.TruckDesc displayedTruck;

	// Column Widths: [160, 80, 170, 140, 80, 160, 40, 45, 45, 80, 60, 75, 110, 100, 100, 95, 85, 85, 90, 90, 90, 60, 60, 60, 90, 90, 90, 90, 105, 90, 95, 95, 110, 80, 235, 125, 95, 80, 280, 75, 75, 75, 60, 120, 120, 100, 60, 200, 110, 110, 110, 130, 95, 90, 110, 130, 70, 80, 150, 150] in ModelOrder
	public TruckTableModel(Window mainWindow, GlobalFinalDataStructures gfds, String tableModelInstanceID) {
		super(mainWindow, gfds, tableModelInstanceID, new VerySimpleTableModel.ColumnID[] {
				new ColumnID( "ID"       , "ID"                    ,               String.class, 160,             null,   null,      null, false, GET.get(t->t.id         )),
				new ColumnID( "UpdateLvl", "Update Level"          ,               String.class,  80,             null,   null,      null, false, GET.get(t->t.updateLevel)),
				new ColumnID(ID_DLC      , "DLC"                   ,               String.class, 170,             null,   null,      null, false, GET.get(t->gfds.dlcs.getDLC(t.id, SnowRunner.DLCs.ItemType.Truck))),
				new ColumnID( "Country"  , "Country"               ,     Truck.CountrySet.class, 140,             null, CENTER,      null, false, GET.get(t->t.gameData, gd->gd.country)),
				new ColumnID( "Type"     , "Type"                  ,      Truck.TruckType.class,  80,             null, CENTER,      null, false, GET.get(t->t.type)),
				new ColumnID( "Name"     , "Name"                  ,               String.class, 160,             null,   null,      null,  true, GET.get(t->t.gameData, gd->gd.getNameStringID())),
				new ColumnID(ID_HasImage , "Image"                 ,              Boolean.class,  40,             null,   null,      null, false, GET.get(t->gfds.truckImages.contains(t.id))),
				new ColumnID( "OwnedBool", "Owned"                 ,              Boolean.class,  45,             null,   null,      null, false, GET.get((m,t)-> m.saveGame==null ? null : 0 < m.saveGame.getOwnedTruckCount(t))),
				new ColumnID( "Owned"    , "Owned"                 ,                 Long.class,  45,             null, CENTER,      null, false, GET.get((m,t)-> m.saveGame==null ? null :     m.saveGame.getOwnedTruckCount(t))),
				new ColumnID( "InWareHs" , "In Warehouse"          ,              Integer.class,  80,             null, CENTER,      null, false, GET.get((model,row) -> getTrucksInWarehouse(model,row,null)), GET.getV(TruckTableModel::getTrucksInWarehouse)), 
				new ColumnID( "InGarage" , "In Garage"             ,              Integer.class,  60,             null, CENTER,      null, false, GET.get((model,row) -> getTrucksInGarage   (model,row,null)), GET.getV(TruckTableModel::getTrucksInGarage   )),
				new ColumnID( "OnTheRoad", "On the Road"           ,                 Long.class,  75,             null, CENTER,      null, false, GET.get((model,row) -> getTrucksOnTheRoad  (model,row,null)), GET.getV(TruckTableModel::getTrucksOnTheRoad  )),
				new ColumnID( "DLData"   , "DiffLock (from Data)"  ,   Truck.DiffLockType.class, 110,             null, CENTER,      null, false, GET.get(t->t.diffLockType)),
				new ColumnID( "DLUser"   , "DiffLock (by User)"    ,  Truck.UDV.ItemState.class, 100, Edit.UD_DiffLock, CENTER,      null, false, GET.get(t->gfds.userDefinedValues.getTruckValues(t.id).realDiffLock)),
				new ColumnID( "DLTool"   , "DiffLock (by Tool)"    ,  Truck.UDV.ItemState.class, 100,             null, CENTER,      null, false, GET.get(tr->getInstState(tr, t->t.hasCompatibleDiffLock, t->t.defaultDiffLock, addon->addon.enablesDiffLock))),
				new ColumnID( "AWDData"  , "AWD (from Data)"       ,               String.class,  95,             null, CENTER,      null, false, row -> "??? t.b.d."),
				new ColumnID( "AWDUser"  , "AWD (by User)"         ,  Truck.UDV.ItemState.class,  85,      Edit.UD_AWD, CENTER,      null, false, GET.get(t->gfds.userDefinedValues.getTruckValues(t.id).realAWD)),
				new ColumnID( "AWDTool"  , "AWD (by Tool)"         ,  Truck.UDV.ItemState.class,  85,             null, CENTER,      null, false, GET.get(TruckTableModel::getAwdState)),
				new ColumnID( "AutoWinch", "Automatic Winch"       ,              Boolean.class,  90,             null,   null,      null, false, GET.get(t->t.hasCompatibleAutomaticWinch)),
				new ColumnID(ID_MetalD   , "Metal Detector"        ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MetalDetector   ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MetalDetector   ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_Seismic  , "Seismic Vibrator"      ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.SeismicVibrator ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.SeismicVibrator ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_BigCrane , "Big Crane"             ,      Data.Capability.class,  60,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.BigCrane        ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.BigCrane        ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_MiniCrane, "Mini Crane"            ,      Data.Capability.class,  60,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MiniCrane       ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MiniCrane       ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_LogLift  , "Log Lift"              ,      Data.Capability.class,  60,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.LogLift         ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.LogLift         ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_LongLogs , "Long Logs"             ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.LongLogs        ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.LongLogs        ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_MedLogs  , "Medium Logs"           ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MediumLogs      ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MediumLogs      ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_ShortLogs, "Short Logs"            ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.ShortLogs       ), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.ShortLogs       ) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID(ID_MedLogsB , "M. Logs (Burnt)"       ,      Data.Capability.class,  90,             null,   null,      null, false, createIsCapableFcn(SpecialTruckAddons.AddonCategory.MediumLogs_burnt), createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory.MediumLogs_burnt) ).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID( "MiniCargo", "Mini Crane & Cargo"    ,              Boolean.class, 105,             null,   null,      null, false, GET.get(TruckTableModel::canMiniCraneAndCargo                                          )).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID( "LogLiftL" , "Log Lift & L-Logs"     ,              Boolean.class,  90,             null,   null,      null, false, GET.get((m,t)->canLogLiftAndLogs(m,t,SpecialTruckAddons.AddonCategory.LongLogs        ))).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID( "LogLiftM" , "Log Lift & M-Logs"     ,              Boolean.class,  95,             null,   null,      null, false, GET.get((m,t)->canLogLiftAndLogs(m,t,SpecialTruckAddons.AddonCategory.MediumLogs      ))).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID( "LogLiftS" , "Log Lift & S-Logs"     ,              Boolean.class,  95,             null,   null,      null, false, GET.get((m,t)->canLogLiftAndLogs(m,t,SpecialTruckAddons.AddonCategory.ShortLogs       ))).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID( "LogLiftMB", "Log Lift & M-Logs (B)" ,              Boolean.class, 110,             null,   null,      null, false, GET.get((m,t)->canLogLiftAndLogs(m,t,SpecialTruckAddons.AddonCategory.MediumLogs_burnt))).configureCaching(ColumnID.Update.Data, ColumnID.Update.SpecialTruckAddons),
				new ColumnID( "FuelCap"  , "Fuel Capacity"         ,              Integer.class,  80,             null,   null,    "%d L", false, GET.get(t->t.fuelCapacity)),
				new ColumnID( "DefWhType", "Default Wheel Type"    ,               String.class, 235,             null,   null,      null, false, GET.get(t->t.wheels, wh->wh.defaultWheelType)),
				new ColumnID( "DefTire"  , "Default Tire"          ,               String.class, 125,             null,   null,      null, false, GET.get(t->t.wheels, wh->wh.defaultTire)),
				new ColumnID( "DefRim"   , "Default Rim"           ,               String.class,  95,             null,   null,      null, false, GET.get(t->t.wheels, wh->wh.defaultRim)),
				new ColumnID( "WheelSizs", "Wheel Sizes"           ,               String.class,  80,             null,   null,      null, false, GET.get(t->joinWheelSizes(t.compatibleWheels))),
				new ColumnID( "WheelTyps", "Wheel Types"           ,               String.class, 280,             null,   null,      null,        GET.getL((lang,t) -> getWheelCategories(t,lang))),
				new ColumnID( "WhHigh"   , "[W] Highway"           ,                Float.class,  75,             null,   null,   "%1.2f", false, GET.get(t->getMaxWheelValue(t, tire->tire.frictionHighway))),
				new ColumnID( "WhOffr"   , "[W] Offroad"           ,                Float.class,  75,             null,   null,   "%1.2f", false, GET.get(t->getMaxWheelValue(t, tire->tire.frictionOffroad))),
				new ColumnID( "WhMud"    , "[W] Mud"               ,                Float.class,  75,             null,   null,   "%1.2f", false, GET.get(t->getMaxWheelValue(t, tire->tire.frictionMud    ))),
				new ColumnID( "Price"    , "Price"                 ,              Integer.class,  60,             null,   null,   "%d Cr", false, GET.get(t->t.gameData, gd->gd.price              )),
				new ColumnID( "UnlExpl"  , "Unlock By Exploration" ,              Boolean.class, 120,             null,   null,      null, false, GET.get(t->t.gameData, gd->gd.unlockByExploration)),
				new ColumnID( "UnlObject", "Unlock By Objective"   ,               String.class, 120,             null,   null,      null, false, GET.get(t->t.gameData, gd->gd.unlockByObjective  )),
				new ColumnID( "UnlRank"  , "Unlock By Rank"        ,              Integer.class, 100,             null, CENTER, "Rank %d", false, GET.get(t->t.gameData, gd->gd.unlockByRank       )),
				new ColumnID( "Unlocked" , "Unlocked"              ,              Boolean.class,  60,             null,   null,      null, false, GET.get((m,t) -> SaveGame.isUnlockedItem(m.saveGame, t.id))),
				new ColumnID( "Desc"     , "Description"           ,               String.class, 200,             null,   null,      null,  true, GET.get(t->t.gameData, gd->gd.getDescriptionStringID())), 
				new ColumnID( "DefEngine", "Default Engine"        ,               String.class, 110,             null,   null,      null,        GET.getL((lang,t) -> SnowRunner.solveStringID(t.defaultEngine    , t.defaultEngine_ItemID    , lang))),
				new ColumnID( "DefGearbx", "Default Gearbox"       ,               String.class, 110,             null,   null,      null,        GET.getL((lang,t) -> SnowRunner.solveStringID(t.defaultGearbox   , t.defaultGearbox_ItemID   , lang))),
				new ColumnID( "DefSusp"  , "Default Suspension"    ,               String.class, 110,             null,   null,      null,        GET.getL((lang,t) -> SnowRunner.solveStringID(t.defaultSuspension, t.defaultSuspension_ItemID, lang))),
				new ColumnID( "DefWinch" , "Default Winch"         ,               String.class, 130,             null,   null,      null,        GET.getL((lang,t) -> SnowRunner.solveStringID(t.defaultWinch     , t.defaultWinch_ItemID     , lang))),
				new ColumnID( "DefDifLck", "Default DiffLock"      ,               String.class,  95,             null,   null,      null,        GET.getL((lang,t) -> SnowRunner.solveStringID(t.defaultDiffLock  , lang))),
				new ColumnID( "DefAWD"   , "Default AWD"           ,               String.class,  90,             null,   null,      null,        GET.getL((lang,t) -> SnowRunner.solveStringID(t.defaultAWD       , lang))),
				new ColumnID( "UpgrWinch", "Upgradable Winch"      ,              Boolean.class, 110,             null,   null,      null, false, GET.get(t->t.isWinchUpgradable)),
		//		new ColumnID( "MaxWhWoSp", "Max. WheelRadius Without Suspension" , String.class, 200,             null,   null,      null, false, GET.get(t->t.maxWheelRadiusWithoutSuspension)),
				new ColumnID( "Image"    , "Image"                 ,               String.class, 130,             null,   null,      null, false, GET.get(t->t.image)),
				new ColumnID( "CargoSlts", "Cargo Slots"           ,              Integer.class,  70,             null, CENTER,      null, false, GET.get(t->t.gameData, gd->gd.cargoSlots    )),
				new ColumnID( "CargoCarr", "Cargo Carrier"         ,              Boolean.class,  80,             null,   null,      null, false, GET.get(t->t.gameData, gd->gd.isCargoCarrier)),
				new ColumnID( "ExclCargo", "Excluded Cargo Types"  ,               String.class, 150,             null,   null,      null, false, GET.get(t->t.gameData, gd->gd.excludedCargoTypes, d->SnowRunner.joinAddonIDs(d,true))),
				new ColumnID( "ExclAddon", "Exclude Addons"        ,               String.class, 150,             null,   null,      null, false, GET.get(t->t.gameData, gd->gd.excludeAddons     , d->SnowRunner.joinAddonIDs(d,true))),
		//		new ColumnID( "Recall"   , "Recallable"            ,              Boolean.class,  60,             null,   null,      null, false, GET.get(t->t.gameData, gd->gd.recallable_obsolete)),
		});
		this.data = null;
		this.saveGame = null;
		colorizeTrucksByTrailers = null;
		displayedTruck = gfds.controllers.storedTruckDisplayers.getDisplayedTruck();
		
		connectToGlobalData(true, data->{
			this.data = data;
			if (this.data==null) return null;
			return this.data.trucks.values();
		});
		setInitialRowOrder(Comparator.<Truck,String>comparing(truck->truck.id));
		
		
		//coloring.addForegroundRowColorizer(truck->{
		//	if (truck==null)
		//		return null;
		//	
		//	if (enableOwnedTrucksHighlighting && saveGame!=null && saveGame.playerOwnsTruck(truck))
		//		return SnowRunner.COLOR_FG_OWNEDTRUCK;
		//	
		//	if (enableDLCTrucksHighlighting && truck.updateLevel!=null)
		//		return SnowRunner.COLOR_FG_DLCTRUCK;
		//	
		//	return null;
		//});
		
		coloring.addBackgroundRowColorizer(truck -> {
			if (truck!=null)
			{
				if (displayedTruck!=null && truck.id.equals(displayedTruck.type))
					return BG_COLOR__DISPLAYED_TRUCK;
			}
			return null;
		});
		coloring.setBackgroundColumnColoring(true, Truck.UDV.ItemState.class, (truck, state) ->{
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
				fireTableColumnUpdate(getColumnIndexByIdFromVisible(id));
		});
		
		finalizer.addDLCListener(() -> fireTableColumnUpdate(ID_DLC));
		finalizer.addFilterTrucksByTrailersListener(this::setTrailerForFilter);
		finalizer.addAddBoolColumnToTrucksListener(this::addExtraBoolColumn);
		
		finalizer.addStoredTruckDisplayer(displayedTruck -> {
			this.displayedTruck = displayedTruck;
			table.repaint();
		});
		
		Trailer filter = gfds.controllers.filterTrucksByTrailersListeners.getFilter();
		if (filter!=null)
			SwingUtilities.invokeLater(()->setTrailerForFilter(filter));
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

	private static TableModelBasedBuilder<Data.Capability> createIsCapableFcn(SpecialTruckAddons.AddonCategory listID) {
		return GET.get((model,truck) -> isCapable(model, truck, null, listID));
	}

	private static VerboseTableModelBasedBuilder<Data.Capability> createIsCapableFcn_verbose(SpecialTruckAddons.AddonCategory listID) {
		return GET.getV((model,truck,textOutput) -> isCapable(model, truck, textOutput, listID));
	}

	private static Data.Capability isCapable(TruckTableModel model, Truck truck, TextOutput textOutput, SpecialTruckAddons.AddonCategory listID)
	{
		return model.data==null || listID==null ? null : model.data.isCapable(truck, listID, model.gfds.specialTruckAddons, textOutput);
	}

	private static Boolean canMiniCraneAndCargo(TruckTableModel model, Truck truck)
	{
		if (truck.gameData.isCargoCarrier)
		{
			Data.Capability canMiniCrane = model.data.isCapable(truck, SpecialTruckAddons.AddonCategory.MiniCrane, model.gfds.specialTruckAddons, null);
			return canMiniCrane==null ? null : canMiniCrane.isCapable;
		}
		
		SpecialTruckAddons.SpecialTruckAddonList list = model.gfds.specialTruckAddons.getList(SpecialTruckAddons.AddonCategory.MiniCrane);
		return Data.canTruckCombineCompatibleAddons(
				truck,
				addon -> list.contains(addon),
				addon -> addon.gameData.isCargoCarrier
		);
	}

	private static Boolean canLogLiftAndLogs(TruckTableModel model, Truck truck, SpecialTruckAddons.AddonCategory logType)
	{
		if (logType.type != SpecialTruckAddons.AddonType.LoadAreaCargo)
			throw new IllegalArgumentException();
		
		SpecialTruckAddons.SpecialTruckAddonList listOfLogs = model.gfds.specialTruckAddons.getList(logType);
		Vector<String> logIDs = new Vector<>();
		listOfLogs.forEach(logIDs::add);
		HashSet<Data.CargoTypePair> logCargoTypes = model.data.getTruckAddonCargoTypes(logIDs);
		
		SpecialTruckAddons.SpecialTruckAddonList listOfLogLifts = model.gfds.specialTruckAddons.getList(SpecialTruckAddons.AddonCategory.LogLift);
		return Data.canTruckCombineCompatibleAddons(
				truck,
				addon -> listOfLogLifts.contains(addon),
				addon -> Data.hasItemACompatibleLoadArea(addon, logCargoTypes, true)
		);
	}

	private static Integer getTrucksInWarehouse(TruckTableModel model, Truck truck, TextOutput textOutput)
	{
		return model.saveGame==null ? null : model.saveGame.getTrucksInWarehouse(truck, textOutput);
	}
	private static Integer getTrucksInGarage(TruckTableModel model, Truck truck, TextOutput textOutput)
	{
		return model.saveGame==null ? null : model.saveGame.getTrucksInGarage(truck, textOutput);
	}
	private static Long getTrucksOnTheRoad(TruckTableModel model, Truck truck, TextOutput textOutput)
	{
		return model.saveGame==null ? null : model.saveGame.getTrucksOnTheRoad(truck, textOutput);
	}
	
	private static class ContainedFloat
	{
		Float value = null;
		
		void setMax(Float other)
		{
			if (value==null)
				value = other;
			else if (other!=null)
				value = Math.max( value, other );
		}
	}

	private static Float getMaxWheelValue(Truck truck, Function<TruckTire,Float> getTireValue) {
		ContainedFloat max = new ContainedFloat();
		for (CompatibleWheel cw : truck.compatibleWheels)
			if (cw.wheelsDef!=null)
				cw.wheelsDef.forEachTire((i,tire) -> max.setMax( getTireValue.apply(tire) ));
		return max.value;
	}
	
	private static String getWheelCategories(Truck truck, Data.Language language) {
		HashSet<String> tireTypes_StringID = new HashSet<>();
		for (CompatibleWheel cw : truck.compatibleWheels)
			if (cw.wheelsDef!=null)
				cw.wheelsDef.forEachTire((i,tire) -> {
					if (tire.tireType_StringID!=null)
						tireTypes_StringID.add(tire.tireType_StringID);
				});
		Vector<String> vec = new Vector<>(tireTypes_StringID);
		vec.sort(Comparator.comparing(TruckTire::getTypeOrder).thenComparing(Comparator.naturalOrder()));
		Iterable<String> it = () -> vec.stream().map(stringID->SnowRunner.solveStringID(stringID, language)).iterator();
		return String.join(", ", it);
	}

	private static Truck.UDV.ItemState getAwdState(Truck truck)
	{
		if (truck.defaultAWD!=null && truck.defaultAWD.enablesAllWheelDrive!=null && truck.defaultAWD.enablesAllWheelDrive.booleanValue())
			return Truck.UDV.ItemState.Installed;
		
		if (truck.hasCompatibleAWD)
			return Truck.UDV.ItemState.Able;
		
		Truck.UDV.ItemState awdState = truck.wheels.awdState;
		if (awdState == Truck.UDV.ItemState.Able)
		{
			if (!truck.hasCompatibleAWD)
				return Truck.UDV.ItemState.None;
		}
		return awdState;
	}

	private static Truck.UDV.ItemState getInstState(
			Truck truck,
			Predicate<Truck> hasInstallableEnablingAddon,
			Function<Truck,TruckAddon> getDefaultAddon,
			Function<TruckAddon,Boolean> addonEnablesFeature
	) {
		if (truck==null) return null;
		
		TruckAddon defaultAddon = getDefaultAddon.apply(truck);
		Boolean defaultAddonEnablesFeature = defaultAddon==null ? false : addonEnablesFeature.apply(defaultAddon);
		if (defaultAddonEnablesFeature!=null && defaultAddonEnablesFeature.booleanValue())
			return Truck.UDV.ItemState.Installed;
		
		if (hasInstallableEnablingAddon.test(truck))
			return Truck.UDV.ItemState.Able;
		
		return null; // None || Permanent 
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
		if (!(columnID_ instanceof ColumnID columnID))
			return false;
		return columnID.editMarker!=null;
	}

	@Override
	protected void setValueAt(Object aValue, int rowIndex, int columnIndex, VerySimpleTableModel.ColumnID columnID_) {
		if (!(columnID_ instanceof ColumnID columnID))
			return;
		if (columnID.editMarker==null) return;
		
		Truck row = getRow(rowIndex);
		if (row==null) return;
		
		Truck.UDV values = gfds.userDefinedValues.getOrCreateTruckValues(row.id);
		
		switch (columnID.editMarker) {
		case UD_AWD     : values.realAWD      = (Truck.UDV.ItemState)aValue; break;
		case UD_DiffLock: values.realDiffLock = (Truck.UDV.ItemState)aValue; break;
		}
		
		gfds.userDefinedValues.write();
	}

	@Override protected String getRowName(Truck row)
	{
		return SnowRunner.solveStringID(row, language);
	}

	@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
		super.modifyTableContextMenu(table_, contextMenu);
		
		contextMenu.addSeparator();
		
		contextMenu.add(SnowRunner.createMenuItem("Filter by Trailer -> context menu of any \"Trailers\" table", true, e->{}));
		contextMenu.add(SnowRunner.createMenuItem("Reset Trailer Filter", true, e->{
			gfds.controllers.filterTrucksByTrailersListeners.setFilter(null);
		}));
		
		contextMenu.addSeparator();
		
		JMenuItem miAssignToDLC = contextMenu.add(SnowRunner.createMenuItem("Assign truck to an official DLC", true, e->{
			if (clickedItem==null) return;
			AssignToDLCDialog dlg = new AssignToDLCDialog(
					mainWindow,
					SnowRunner.DLCs.ItemType.Truck,
					clickedItem.id,
					SnowRunner.getTruckLabel(clickedItem,language),
					gfds.dlcs
			);
			boolean assignmentsChanged = dlg.showDialog();
			if (assignmentsChanged)
				gfds.controllers.dlcListeners.updateAfterChange();
		}));
		
		JMenuItem miSetTruckImage = contextMenu.add(SnowRunner.createMenuItem("Paste image of truck from clipboard", true, e->{
			if (clickedItem==null) return;
			BufferedImage image = ClipboardTools.getImageFromClipBoard();
			if (image==null)
				JOptionPane.showMessageDialog(mainWindow, "Sorry, found no image in clipboard.", "No Image", JOptionPane.ERROR_MESSAGE);
			else
			{
				String truckID = clickedItem.id;
				gfds.truckImages.set(truckID, image);
				gfds.controllers.truckImagesListeners.imageHasChanged(truckID);
				fireTableColumnUpdate(ID_HasImage);
			}
		}));
		
		//contextMenu.addSeparator();
		
		//JCheckBoxMenuItem miEnableOwnedTrucksHighlighting, miEnableDLCTrucksHighlighting;
		//contextMenu.add(miEnableOwnedTrucksHighlighting = SnowRunner.createCheckBoxMenuItem("Highlighting of owned trucks", enableOwnedTrucksHighlighting, null, true, ()->{
		//	enableOwnedTrucksHighlighting = !enableOwnedTrucksHighlighting;
		//	SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableOwnedTrucksHighlighting, enableOwnedTrucksHighlighting);
		//	if (table!=null) table.repaint();
		//}));
		//contextMenu.add(miEnableDLCTrucksHighlighting = SnowRunner.createCheckBoxMenuItem("Highlighting of DLC trucks", enableDLCTrucksHighlighting, null, true, ()->{
		//	enableDLCTrucksHighlighting = !enableDLCTrucksHighlighting;
		//	SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.TruckTableModel_enableDLCTrucksHighlighting, enableDLCTrucksHighlighting);
		//	if (table!=null) table.repaint();
		//}));
		
		contextMenu.addContextMenuInvokeListener((table, x, y) -> {
			Point p = x==null || y==null ? null : new Point(x,y);
			int rowV = p==null ? -1 : table.rowAtPoint(p);
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			clickedItem = rowM<0 ? null : getRow(rowM);
			
			miAssignToDLC.setEnabled(clickedItem!=null);
			miAssignToDLC.setText(
				clickedItem==null
				? "Assign truck to an official DLC"
				: String.format("Assign \"%s\" to an official DLC", SnowRunner.getTruckLabel(clickedItem,language))
			);
			
			miSetTruckImage.setEnabled(clickedItem!=null);
			miSetTruckImage.setText(
				clickedItem==null
				? "Paste image of truck from clipboard"
				: String.format("Paste image of \"%s\" from clipboard", SnowRunner.getTruckLabel(clickedItem,language))
			);
			
			//miEnableOwnedTrucksHighlighting.setSelected(enableOwnedTrucksHighlighting);
			//miEnableDLCTrucksHighlighting  .setSelected(enableDLCTrucksHighlighting  );
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