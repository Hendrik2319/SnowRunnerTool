package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;

import net.schwarzbaer.java.games.snowrunner.Data.Winch;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;

public class WinchesTableModel extends SetInstancesTableModel<Winch> {

	public WinchesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("SetID"    ,"Set ID"                  ,  String.class, 140,   null,      null, false, row->((Winch)row).setID),
				new ColumnID("ItemID"   ,"Item ID"                 ,  String.class, 160,   null,      null, false, row->((Winch)row).id),
				new ColumnID("Name"     ,"Name"                    ,  String.class, 150,   null,      null,  true, row->((Winch)row).name_StringID),
				new ColumnID("Desc"     ,"Description"             ,  String.class, 150,   null,      null,  true, row->((Winch)row).description_StringID),
				new ColumnID("Price"    ,"Price"                   , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Winch)row).price),
				new ColumnID("UnlExpl"  ,"Unlock By Exploration"   , Boolean.class, 120,   null,      null, false, row->((Winch)row).unlockByExploration),
				new ColumnID("UnlRank"  ,"Unlock By Rank"          , Integer.class,  85, CENTER, "Rank %d", false, row->((Winch)row).unlockByRank),
				new ColumnID("RequEngI" ,"Requires Engine Ignition", Boolean.class, 130,   null,      null, false, row->((Winch)row).isEngineIgnitionRequired),
				new ColumnID("Length"   ,"Length"                  , Integer.class,  50, CENTER,      null, false, row->((Winch)row).length),
				new ColumnID("StrengthM","Strength Multi"          ,   Float.class,  80, CENTER,   "%1.2f", false, row->((Winch)row).strengthMult),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Winch)row).usableBy, lang)),
		});
		if (connectToGlobalData)
			connectToGlobalData(data->data.winches.values());
	}
}