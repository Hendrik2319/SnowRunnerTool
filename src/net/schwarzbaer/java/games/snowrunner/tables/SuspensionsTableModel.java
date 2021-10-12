package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;

import net.schwarzbaer.java.games.snowrunner.Data.Suspension;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;

public class SuspensionsTableModel extends SetInstancesTableModel<Suspension> {

	public SuspensionsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("SetID"    ,"Set ID"               ,  String.class, 130,   null,      null, false, row->((Suspension)row).setID),
				new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 220,   null,      null, false, row->((Suspension)row).id),
				new ColumnID("Type"     ,"Type"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).type_StringID),
				new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).name_StringID),
				new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Suspension)row).description_StringID),
				new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Suspension)row).price),
				new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Suspension)row).unlockByExploration),
				new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Suspension)row).unlockByRank),
				new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Suspension)row).damageCapacity),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Suspension)row).usableBy, lang)),
		});
		if (connectToGlobalData)
			connectToGlobalData(data->data.suspensions.values());
	}
}