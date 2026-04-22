package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Window;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiPredicate;

import net.schwarzbaer.java.games.snowrunner.Data.Engine;
import net.schwarzbaer.java.games.snowrunner.Data.Gearbox;
import net.schwarzbaer.java.games.snowrunner.Data.Gearbox.GearValues;
import net.schwarzbaer.java.games.snowrunner.Data.Suspension;
import net.schwarzbaer.java.games.snowrunner.Data.TruckComponent;
import net.schwarzbaer.java.games.snowrunner.Data.Winch;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.tables.TruckPanelProto.AddonCategoriesPanel.DisplayedTruckComponentList;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTPOS;
import net.schwarzbaer.java.lib.gui.StyledDocumentInterface;

public abstract class SetInstancesTableModel<RowType extends TruckComponent> extends ExtendedVerySimpleTableModelTPOS<RowType> implements DisplayedTruckComponentList
{
	private static final Color COLOR_BG_DISPLAYED_TRUCK_COMP = new Color(0xFFDF00);

	protected SaveGame saveGame;
	protected SaveGame.TruckDesc displayedTruck;

	SetInstancesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, SaveGame saveGame, ColumnID[] columns) {
		super(mainWindow, gfds, columns);
		this.saveGame = saveGame;
		displayedTruck = null;
		setInitialRowOrder(Comparator.<RowType,String>comparing(e->e.setID).thenComparing(e->e.id));
		finalizer.addSaveGameListener(saveGame_->{
			this.saveGame = saveGame_;
			updateOutput();
		});
		coloring.addBackgroundRowColorizer(row -> isComponentOfDisplayedTruck(row) ? COLOR_BG_DISPLAYED_TRUCK_COMP : null);
	}
	
	abstract boolean isComponentOfDisplayedTruck(RowType row);

	@Override public boolean setDisplayedTruck(SaveGame.TruckDesc displayedTruck)
	{
		this.displayedTruck = displayedTruck;
		boolean hasHit = false;
		for (RowType row : rows)
			if (isComponentOfDisplayedTruck(row))
			{
				hasHit = true;
				break;
			}
		return hasHit;
	}

	@Override protected void setOutputContentForRow(StyledDocumentInterface doc, int rowIndex, RowType row) {
		TruckAddonsTableModel.generateText(
				doc, row,
				row.gameData.getDescriptionStringID(),
				null, row.usableBy,
				language,
				null, null, null,
				saveGame
		);
	}

	@Override protected String getRowName(RowType row)
	{
		return SnowRunner.solveStringID(row, language);
	}
	
	protected static <RowType extends TruckComponent> Long getOwnedCount(SetInstancesTableModel<RowType> model, RowType row)
	{
		if (row==null) return null;
		if (model==null) return null;
		if (model.saveGame==null) return null;
		SaveGame.Addon addon = model.saveGame.addons.get(row.id);
		return addon==null ? null : addon.owned;
	}

	public static class EnginesTableModel extends SetInstancesTableModel<Engine>
	{
		private static final GetValueConverter<Engine, EnginesTableModel> GET = new GetValueConverter<>(Engine.class, EnginesTableModel.class);
	
		public EnginesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public EnginesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 160,   null,      null, false, GET.get(row->row.setID)),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 190,   null,      null, false, GET.get(row->row.id)),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 130,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getNameStringID())),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getDescriptionStringID())),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, GET.get(row->row.gameData, gd->gd.price)),
					new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, GET.get(SetInstancesTableModel::getOwnedCount)),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, GET.get(row->row.gameData, gd->gd.unlockByExploration)),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, GET.get(row->row.gameData, gd->gd.unlockByRank)),
					new ColumnID("Torque"   ,"Torque"               , Integer.class,  70,   null,      null, false, GET.get(row->row.torque)),
					new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,  RIGHT,   "%1.2f", false, GET.get(row->row.fuelConsumption)),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,  RIGHT,    "%d R", false, GET.get(row->row.damageCapacity)),
					new ColumnID("BrakesDel","Brakes Delay"         ,   Float.class,  70,  RIGHT,   "%1.2f", false, GET.get(row->row.brakesDelay)),
					new ColumnID("Respons"  ,"Responsiveness"       ,   Float.class,  90,  RIGHT,   "%1.4f", false, GET.get(row->row.engineResponsiveness)),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null,        GET.getL((lang,row)->SnowRunner.joinNames(row.usableBy, lang))),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.engines.getAllInstances());
		}
		@Override boolean isComponentOfDisplayedTruck(Engine row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.engine);
		}
	}

	public static class GearboxesTableModel extends SetInstancesTableModel<Gearbox>
	{
		private static final GetValueConverter<Gearbox, GearboxesTableModel> GET = new GetValueConverter<>(Gearbox.class, GearboxesTableModel.class);
	
		public GearboxesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public GearboxesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			// Column Widths: [180, 140, 110, 150, 60, 60, 120, 85, 35, 35, 35, 35, 110, 75, 65, 95, 80, 110, 170, 110, 110, 250] in ModelOrder
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 180,   null,      null, false, GET.get(row->row.setID)),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 140,   null,      null, false, GET.get(row->row.id)),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getNameStringID())),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getDescriptionStringID())),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,   null,   "%d Cr", false, GET.get(row->row.gameData, gd->gd.price)),
					new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, GET.get(SetInstancesTableModel::getOwnedCount)),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, GET.get(row->row.gameData, gd->gd.unlockByExploration)),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, GET.get(row->row.gameData, gd->gd.unlockByRank)),
					new ColumnID("GearH"    ,"(H)"                  , Boolean.class,  35,   null,      null, false, GET.get(row->row.existsHighGear)),
					new ColumnID("GearL"    ,"(L)"                  , Boolean.class,  35,   null,      null, false, GET.get(row->row.existsLowerGear)),
					new ColumnID("GearLP"   ,"(L+)"                 , Boolean.class,  35,   null,      null, false, GET.get(row->row.existsLowerPlusGear)),
					new ColumnID("GearLM"   ,"(L-)"                 , Boolean.class,  35,   null,      null, false, GET.get(row->row.existsLowerMinusGear)),
					new ColumnID("ManualLG" ,"is Manual Low Gear"   , Boolean.class, 110,   null,      null, false, GET.get(row->row.isManualLowGear)),
					new ColumnID("DamageCap","Damage Cap."          , Integer.class,  75, CENTER,    "%d R", false, GET.get(row->row.damageCapacity)),
					new ColumnID("FuelCons" ,"Fuel Cons."           ,   Float.class,  65, CENTER,   "%1.2f", false, GET.get(row->row.fuelConsumption)),
					new ColumnID("AWDCons"  ,"AWD Cons. Mod."       ,   Float.class,  95, CENTER,   "%1.2f", false, GET.get(row->row.awdConsumptionModifier)),
					new ColumnID("IdleFuel" ,"Idle Fuel Mod."       ,   Float.class,  80, CENTER,   "%1.2f", false, GET.get(row->row.idleFuelModifier)),
					new ColumnID("DamConsMd","Dam. Cons. Mod."      ,   Float.class, 110, CENTER,   "%1.2f", false, GET.get(row->row.damageConsumptionModifier)),
					new ColumnID("Gears"    ,"Gears"                ,  String.class, 170,   null,      null, false, GET.get(row->toString(row.gears       ))),
					new ColumnID("HighGears","High Gears"           ,  String.class, 110,   null,      null, false, GET.get(row->toString(row.highGears   ))),
					new ColumnID("RevGears" ,"Reverse Gears"        ,  String.class, 110,   null,      null, false, GET.get(row->toString(row.reverseGears))),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 250,   null,      null,        GET.getL((lang,row)->SnowRunner.joinNames(row.usableBy, lang))),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.gearboxes.getAllInstances());
		}
		@Override boolean isComponentOfDisplayedTruck(Gearbox row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.gearbox);
		}
		private static String toString(GearValues[] gearValues)
		{
			if (gearValues==null || gearValues.length==0)
				return null;
			
			if (gearValues.length==1)
				return "1x S/F:%s".formatted(toString(gearValues[0]));
			
			GearValues min = getMinVel(gearValues);
			GearValues max = getMaxVel(gearValues);
			return "%dx S/F:%s..%s".formatted(gearValues.length, toString(min), toString(max));
		}
		private static String toString(GearValues gearValues)
		{
			if (gearValues==null) return "??";
			return String.format(Locale.ENGLISH, "%1.2f/%1.2f", gearValues.angVel, gearValues.fuelModifier);
		}
		
		private static GearValues getMinVel(GearValues[] gearValues) { return getExtVel(gearValues, (v1,v2)->v1>v2); }
		private static GearValues getMaxVel(GearValues[] gearValues) { return getExtVel(gearValues, (v1,v2)->v1<v2); }
		private static GearValues getExtVel(GearValues[] gearValues, BiPredicate<Float, Float> compare)
		{
			if (gearValues==null)
				return null;
			
			float value = Float.NaN;
			GearValues returnValue = null;
			for (GearValues gearValue : gearValues)
				if (gearValue.angVel!=null && (Float.isNaN(value) || compare.test(value, gearValue.angVel)))
				{
					value = gearValue.angVel.floatValue();
					returnValue = gearValue;
				}
			return returnValue;
		}
	}

	public static class SuspensionsTableModel extends SetInstancesTableModel<Suspension>
	{
		private static final GetValueConverter<Suspension, SuspensionsTableModel> GET = new GetValueConverter<>(Suspension.class, SuspensionsTableModel.class);
	
		public SuspensionsTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public SuspensionsTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 130,   null,      null, false, GET.get(row->row.setID)),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 220,   null,      null, false, GET.get(row->row.id)),
					new ColumnID("Type"     ,"Type"                 ,  String.class, 110,   null,      null,  true, GET.get(row->row.type_StringID)),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getNameStringID())),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getDescriptionStringID())),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, GET.get(row->row.gameData, gd->gd.price)),
					new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, GET.get(SetInstancesTableModel::getOwnedCount)),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, GET.get(row->row.gameData, gd->gd.unlockByExploration)),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, GET.get(row->row.gameData, gd->gd.unlockByRank)),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, GET.get(row->row.damageCapacity)),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null,        GET.getL((lang,row)->SnowRunner.joinNames(row.usableBy, lang))),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.suspensions.getAllInstances());
		}
		@Override boolean isComponentOfDisplayedTruck(Suspension row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.suspension);
		}
	}

	public static class WinchesTableModel extends SetInstancesTableModel<Winch>
	{
		private static final GetValueConverter<Winch, WinchesTableModel> GET = new GetValueConverter<>(Winch.class, WinchesTableModel.class);

		public WinchesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public WinchesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"                  ,  String.class, 140,   null,      null, false, GET.get(row->row.setID)),
					new ColumnID("ItemID"   ,"Item ID"                 ,  String.class, 160,   null,      null, false, GET.get(row->row.id)),
					new ColumnID("Name"     ,"Name"                    ,  String.class, 150,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getNameStringID())),
					new ColumnID("Desc"     ,"Description"             ,  String.class, 150,   null,      null,  true, GET.get(row->row.gameData, gd->gd.getDescriptionStringID())),
					new ColumnID("Price"    ,"Price"                   , Integer.class,  60,  RIGHT,   "%d Cr", false, GET.get(row->row.gameData, gd->gd.price)),
					new ColumnID("Owned"    ,"Owned"                   ,    Long.class,  60, CENTER,      null, false, GET.get(SetInstancesTableModel::getOwnedCount)),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration"   , Boolean.class, 120,   null,      null, false, GET.get(row->row.gameData, gd->gd.unlockByExploration)),
					new ColumnID("UnlRank"  ,"Unlock By Rank"          , Integer.class,  85, CENTER, "Rank %d", false, GET.get(row->row.gameData, gd->gd.unlockByRank)),
					new ColumnID("RequEngI" ,"Requires Engine Ignition", Boolean.class, 130,   null,      null, false, GET.get(row->row.isEngineIgnitionRequired)),
					new ColumnID("Length"   ,"Length"                  , Integer.class,  50, CENTER,      null, false, GET.get(row->row.length)),
					new ColumnID("StrengthM","Strength Multi"          ,   Float.class,  80, CENTER,   "%1.2f", false, GET.get(row->row.strengthMult)),
					new ColumnID("UsableBy" ,"Usable By"               ,  String.class, 150,   null,      null,        GET.getL((lang,row)->SnowRunner.joinNames(row.usableBy, lang))),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.winches.getAllInstances());
		}
		@Override boolean isComponentOfDisplayedTruck(Winch row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.winch);
		}
	}
}