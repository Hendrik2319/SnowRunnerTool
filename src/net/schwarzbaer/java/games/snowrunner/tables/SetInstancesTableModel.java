package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Window;
import java.util.Comparator;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.java.games.snowrunner.Data.Engine;
import net.schwarzbaer.java.games.snowrunner.Data.Gearbox;
import net.schwarzbaer.java.games.snowrunner.Data.Suspension;
import net.schwarzbaer.java.games.snowrunner.Data.TruckComponent;
import net.schwarzbaer.java.games.snowrunner.Data.Winch;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.tables.TruckPanelProto.AddonCategoriesPanel.DisplayedTruckComponentList;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTPOS;

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
				doc,
				row.gameData.description_StringID,
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

	public static class EnginesTableModel extends SetInstancesTableModel<Engine> {
	
		public EnginesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public EnginesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 160,   null,      null, false, row->((Engine)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 190,   null,      null, false, row->((Engine)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 130,   null,      null,  true, row->((Engine)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Engine)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Engine)row).gameData.price),
					new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, get((model, lang, row)->getOwnedCount(model,row))),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Engine)row).gameData.unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Engine)row).gameData.unlockByRank),
					new ColumnID("Torque"   ,"Torque"               , Integer.class,  70,   null,      null, false, row->((Engine)row).torque),
					new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,  RIGHT,   "%1.2f", false, row->((Engine)row).fuelConsumption),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,  RIGHT,    "%d R", false, row->((Engine)row).damageCapacity),
					new ColumnID("BrakesDel","Brakes Delay"         ,   Float.class,  70,  RIGHT,   "%1.2f", false, row->((Engine)row).brakesDelay),
					new ColumnID("Respons"  ,"Responsiveness"       ,   Float.class,  90,  RIGHT,   "%1.4f", false, row->((Engine)row).engineResponsiveness),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((Engine)row).usableBy, lang)),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.engines.getAllInstances());
		}
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,EnginesTableModel,Engine> getFunction)
		{
			return ColumnID.get(EnginesTableModel.class, Engine.class, getFunction);
		}
		@Override boolean isComponentOfDisplayedTruck(Engine row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.engine);
		}
	}

	public static class GearboxesTableModel extends SetInstancesTableModel<Gearbox> {
	
		public GearboxesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public GearboxesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 180,   null,      null, false, row->((Gearbox)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 140,   null,      null, false, row->((Gearbox)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Gearbox)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Gearbox)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,   null,   "%d Cr", false, row->((Gearbox)row).gameData.price),
					new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, get((model, lang, row)->getOwnedCount(model,row))),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Gearbox)row).gameData.unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Gearbox)row).gameData.unlockByRank),
					new ColumnID("GearH"    ,"(H)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsHighGear),
					new ColumnID("GearL"    ,"(L)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerGear),
					new ColumnID("GearLP"   ,"(L+)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerPlusGear),
					new ColumnID("GearLM"   ,"(L-)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerMinusGear),
					new ColumnID("ManualLG" ,"is Manual Low Gear"   , Boolean.class, 110,   null,      null, false, row->((Gearbox)row).isManualLowGear),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Gearbox)row).damageCapacity),
					new ColumnID("AWDCons"  ,"AWD Consumption Mod." ,   Float.class, 130,   null,   "%1.2f", false, row->((Gearbox)row).awdConsumptionModifier),
					new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).fuelConsumption),
					new ColumnID("IdleFuel" ,"Idle Fuel Modifier"   ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).idleFuelModifier),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((Gearbox)row).usableBy, lang)),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.gearboxes.getAllInstances());
		}
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,GearboxesTableModel,Gearbox> getFunction)
		{
			return ColumnID.get(GearboxesTableModel.class, Gearbox.class, getFunction);
		}
		@Override boolean isComponentOfDisplayedTruck(Gearbox row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.gearbox);
		}
	}

	public static class SuspensionsTableModel extends SetInstancesTableModel<Suspension> {
	
		public SuspensionsTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public SuspensionsTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 130,   null,      null, false, row->((Suspension)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 220,   null,      null, false, row->((Suspension)row).id),
					new ColumnID("Type"     ,"Type"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).type_StringID),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Suspension)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Suspension)row).gameData.price),
					new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, get((model, lang, row)->getOwnedCount(model,row))),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Suspension)row).gameData.unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Suspension)row).gameData.unlockByRank),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Suspension)row).damageCapacity),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((Suspension)row).usableBy, lang)),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.suspensions.getAllInstances());
		}
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,SuspensionsTableModel,Suspension> getFunction)
		{
			return ColumnID.get(SuspensionsTableModel.class, Suspension.class, getFunction);
		}
		@Override boolean isComponentOfDisplayedTruck(Suspension row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.suspension);
		}
	}

	public static class WinchesTableModel extends SetInstancesTableModel<Winch> {

		public WinchesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, gfds, connectToGlobalData, saveGame, true);
		}
		public WinchesTableModel(Window mainWindow, GlobalFinalDataStructures gfds, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, gfds, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"                  ,  String.class, 140,   null,      null, false, row->((Winch)row).setID),
					new ColumnID("ItemID"   ,"Item ID"                 ,  String.class, 160,   null,      null, false, row->((Winch)row).id),
					new ColumnID("Name"     ,"Name"                    ,  String.class, 150,   null,      null,  true, row->((Winch)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"             ,  String.class, 150,   null,      null,  true, row->((Winch)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                   , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Winch)row).gameData.price),
					new ColumnID("Owned"    ,"Owned"                   ,    Long.class,  60, CENTER,      null, false, get((model, lang, row)->getOwnedCount(model,row))),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration"   , Boolean.class, 120,   null,      null, false, row->((Winch)row).gameData.unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"          , Integer.class,  85, CENTER, "Rank %d", false, row->((Winch)row).gameData.unlockByRank),
					new ColumnID("RequEngI" ,"Requires Engine Ignition", Boolean.class, 130,   null,      null, false, row->((Winch)row).isEngineIgnitionRequired),
					new ColumnID("Length"   ,"Length"                  , Integer.class,  50, CENTER,      null, false, row->((Winch)row).length),
					new ColumnID("StrengthM","Strength Multi"          ,   Float.class,  80, CENTER,   "%1.2f", false, row->((Winch)row).strengthMult),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((Winch)row).usableBy, lang)),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.winches.getAllInstances());
		}
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,WinchesTableModel,Winch> getFunction)
		{
			return ColumnID.get(WinchesTableModel.class, Winch.class, getFunction);
		}
		@Override boolean isComponentOfDisplayedTruck(Winch row)
		{
			return row!=null && displayedTruck!=null && row.id!=null && row.id.equals(displayedTruck.winch);
		}
	}
}