package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;

import net.schwarzbaer.java.games.snowrunner.Data.Engine;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;

public class EnginesTableModel extends SetInstancesTableModel<Engine> {

	public EnginesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("SetID"    ,"Set ID"               ,  String.class, 160,   null,      null, false, row->((Engine)row).setID),
				new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 190,   null,      null, false, row->((Engine)row).id),
				new ColumnID("Name"     ,"Name"                 ,  String.class, 130,   null,      null,  true, row->((Engine)row).name_StringID),
				new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Engine)row).description_StringID),
				new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Engine)row).price),
				new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Engine)row).unlockByExploration),
				new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Engine)row).unlockByRank),
				new ColumnID("Torque"   ,"Torque"               , Integer.class,  70,   null,      null, false, row->((Engine)row).torque),
				new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,  RIGHT,   "%1.2f", false, row->((Engine)row).fuelConsumption),
				new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,  RIGHT,    "%d R", false, row->((Engine)row).damageCapacity),
				new ColumnID("BrakesDel","Brakes Delay"         ,   Float.class,  70,  RIGHT,   "%1.2f", false, row->((Engine)row).brakesDelay),
				new ColumnID("Respons"  ,"Responsiveness"       ,   Float.class,  90,  RIGHT,   "%1.4f", false, row->((Engine)row).engineResponsiveness),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Engine)row).usableBy, lang)),
		});
		if (connectToGlobalData)
			connectToGlobalData(data->data.engines.values());
	}
}