package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;

import net.schwarzbaer.java.games.snowrunner.Data.Gearbox;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;

public class GearboxesTableModel extends SetInstancesTableModel<Gearbox> {

	public GearboxesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("SetID"    ,"Set ID"               ,  String.class, 180,   null,      null, false, row->((Gearbox)row).setID),
				new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 140,   null,      null, false, row->((Gearbox)row).id),
				new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Gearbox)row).name_StringID),
				new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Gearbox)row).description_StringID),
				new ColumnID("Price"    ,"Price"                , Integer.class,  60,   null,   "%d Cr", false, row->((Gearbox)row).price),
				new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Gearbox)row).unlockByExploration),
				new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Gearbox)row).unlockByRank),
				new ColumnID("GearH"    ,"(H)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsHighGear),
				new ColumnID("GearL"    ,"(L)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerGear),
				new ColumnID("GearLP"   ,"(L+)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerPlusGear),
				new ColumnID("GearLM"   ,"(L-)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerMinusGear),
				new ColumnID("ManualLG" ,"is Manual Low Gear"   , Boolean.class, 110,   null,      null, false, row->((Gearbox)row).isManualLowGear),
				new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Gearbox)row).damageCapacity),
				new ColumnID("AWDCons"  ,"AWD Consumption Mod." ,   Float.class, 130,   null,   "%1.2f", false, row->((Gearbox)row).awdConsumptionModifier),
				new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).fuelConsumption),
				new ColumnID("IdleFuel" ,"Idle Fuel Modifier"   ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).idleFuelModifier),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Gearbox)row).usableBy, lang)),
		});
		if (connectToGlobalData)
			connectToGlobalData(data->data.gearboxes.values());
	}
}