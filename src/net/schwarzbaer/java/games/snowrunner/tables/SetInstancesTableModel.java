package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;
import java.util.Comparator;
import java.util.Vector;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.java.games.snowrunner.Data.Engine;
import net.schwarzbaer.java.games.snowrunner.Data.Gearbox;
import net.schwarzbaer.java.games.snowrunner.Data.Suspension;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckComponent;
import net.schwarzbaer.java.games.snowrunner.Data.Winch;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModel2;

public class SetInstancesTableModel<RowType extends TruckComponent> extends ExtendedVerySimpleTableModel2<RowType> {

	protected SaveGame saveGame;

	SetInstancesTableModel(Window mainWindow, Controllers controllers, SaveGame saveGame, ColumnID[] columns) {
		super(mainWindow, controllers, columns);
		this.saveGame = saveGame;
		setInitialRowOrder(Comparator.<RowType,String>comparing(e->e.setID).thenComparing(e->e.id));
		finalizer.addSaveGameListener(saveGame_->{
			this.saveGame = saveGame_;
			updateTextOutput();
		});
	}

	@Override protected void setContentForRow(StyledDocumentInterface doc, RowType row) {
		String description_StringID = row.gameData.description_StringID;
		Vector<Truck> usableBy = row.usableBy;
		TruckAddonsTableModel.generateText(doc, description_StringID, null, null, usableBy, language, null, null, saveGame);
	}
	
	public static class EnginesTableModel extends SetInstancesTableModel<Engine> {
	
		public EnginesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, controllers, connectToGlobalData, saveGame, true);
		}
		public EnginesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, controllers, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 160,   null,      null, false, row->((Engine)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 190,   null,      null, false, row->((Engine)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 130,   null,      null,  true, row->((Engine)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Engine)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Engine)row).gameData.price),
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
	}

	public static class GearboxesTableModel extends SetInstancesTableModel<Gearbox> {
	
		public GearboxesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, controllers, connectToGlobalData, saveGame, true);
		}
		public GearboxesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, controllers, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 180,   null,      null, false, row->((Gearbox)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 140,   null,      null, false, row->((Gearbox)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Gearbox)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Gearbox)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,   null,   "%d Cr", false, row->((Gearbox)row).gameData.price),
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
	}

	public static class SuspensionsTableModel extends SetInstancesTableModel<Suspension> {
	
		public SuspensionsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, controllers, connectToGlobalData, saveGame, true);
		}
		public SuspensionsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, controllers, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 130,   null,      null, false, row->((Suspension)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 220,   null,      null, false, row->((Suspension)row).id),
					new ColumnID("Type"     ,"Type"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).type_StringID),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Suspension)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Suspension)row).gameData.price),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Suspension)row).gameData.unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Suspension)row).gameData.unlockByRank),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Suspension)row).damageCapacity),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((Suspension)row).usableBy, lang)),
			}));
			if (connectToGlobalData)
				connectToGlobalData(data->data.suspensions.getAllInstances());
		}
	}

	public static class WinchesTableModel extends SetInstancesTableModel<Winch> {

		public WinchesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame) {
			this(mainWindow, controllers, connectToGlobalData, saveGame, true);
		}
		public WinchesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SaveGame saveGame, boolean addExtraColumnsBeforeStandard, ColumnID... extraColumns) {
			super(mainWindow, controllers, saveGame, SnowRunner.mergeArrays(extraColumns, addExtraColumnsBeforeStandard, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"                  ,  String.class, 140,   null,      null, false, row->((Winch)row).setID),
					new ColumnID("ItemID"   ,"Item ID"                 ,  String.class, 160,   null,      null, false, row->((Winch)row).id),
					new ColumnID("Name"     ,"Name"                    ,  String.class, 150,   null,      null,  true, row->((Winch)row).gameData.name_StringID),
					new ColumnID("Desc"     ,"Description"             ,  String.class, 150,   null,      null,  true, row->((Winch)row).gameData.description_StringID),
					new ColumnID("Price"    ,"Price"                   , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Winch)row).gameData.price),
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
	}
}