package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JTable;

import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons.SpecialTruckAddonList;

public class TruckAddonsTableModel extends ExtendedVerySimpleTableModel<TruckAddon> {
	
	private static final Color COLOR_SPECIALTRUCKADDON = new Color(0xFFF3AD);
	private HashMap<String, TruckAddon> truckAddons;
	private HashMap<String, Trailer> trailers;
	private TruckAddon clickedItem;
	private final SpecialTruckAddons specialTruckAddons;

	public TruckAddonsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SpecialTruckAddons specialTruckAddons) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("ID"       ,"ID"                   ,  String.class, 230,   null,      null, false, row->((TruckAddon)row).id),
				new ColumnID("DLC"      ,"DLC"                  ,  String.class,  80,   null,      null, false, row->((TruckAddon)row).dlcName),
				new ColumnID("Category" ,"Category"             ,  String.class, 150,   null,      null, false, row->((TruckAddon)row).category),
				new ColumnID("Name"     ,"Name"                 ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.name_StringID!=null ? row.name_StringID : row.cargoName_StringID; }), 
				new ColumnID("InstallSk","Install Socket"       ,  String.class, 130,   null,      null, false, row->((TruckAddon)row).installSocket),
				new ColumnID("CargoSlts","Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((TruckAddon)row).cargoSlots),
				new ColumnID("Repairs"  ,"Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((TruckAddon)row).repairsCapacity),
				new ColumnID("WheelRep" ,"Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((TruckAddon)row).wheelRepairsCapacity),
				new ColumnID("Fuel"     ,"Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((TruckAddon)row).fuelCapacity),
				new ColumnID("EnAWD"    ,"Enables AWD"          , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).enablesAllWheelDrive), 
				new ColumnID("EnDiffLck","Enables DiffLock"     , Boolean.class,  90,   null,      null, false, row->((TruckAddon)row).enablesDiffLock), 
				new ColumnID("Price"    ,"Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((TruckAddon)row).price), 
				new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((TruckAddon)row).unlockByExploration), 
				new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((TruckAddon)row).unlockByRank), 
				new ColumnID("Desc"     ,"Description"          ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.description_StringID!=null ? row.description_StringID : row.cargoDescription_StringID; }), 
				new ColumnID("ExclCargo","Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->SnowRunner.joinAddonIDs(((TruckAddon)row).excludedCargoTypes)),
				new ColumnID("RequAddon","Required Addons"      ,  String.class, 200,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((TruckAddon)row).requiredAddons)),
				new ColumnID("IsCargo"  ,"Is Cargo"             , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).isCargo),
				new ColumnID("CargLngth","Cargo Length"         , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).cargoLength),
				new ColumnID("CargType" ,"Cargo Type"           ,  String.class, 170,   null,      null, false, row->((TruckAddon)row).cargoType),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((TruckAddon)row).usableBy, lang)),
		});
		this.specialTruckAddons = specialTruckAddons;
		
		clickedItem = null;
		truckAddons = null;
		trailers    = null;
		if (connectToGlobalData)
			connectToGlobalData(data->{
				truckAddons = data==null ? null : data.truckAddons;
				trailers    = data==null ? null : data.trailers;
				return truckAddons==null ? null : truckAddons.values();
			}, true);
		
		else
			controllers.dataReceivers.add(this,data->{
				truckAddons = data==null ? null : data.truckAddons;
				trailers    = data==null ? null : data.trailers;
				updateTextArea();
			});
		
		coloring.addBackgroundRowColorizer(addon->{
			for (SpecialTruckAddons.List listID : SpecialTruckAddons.List.values()) {
				SpecialTruckAddonList list = this.specialTruckAddons.getList(listID);
				if (list.contains(addon)) return COLOR_SPECIALTRUCKADDON;
			}
			return null;
		});
		controllers.specialTruckAddonsListeners.add(this, (list, change) -> {
			table.repaint();
		});
		
		Comparator<String> string_nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
		setInitialRowOrder(Comparator.<TruckAddon,String>comparing(row->row.category,string_nullsLast).thenComparing(row->row.id));
	}

	@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
		super.modifyTableContextMenu(table_, contextMenu);
		
		contextMenu.addSeparator();
		
		JMenu specialAddonsMenu = new JMenu("Add Addon to List of known special Addons");
		contextMenu.add(specialAddonsMenu);
		
		EnumMap<SpecialTruckAddons.List, JCheckBoxMenuItem> menuItems = new EnumMap<>(SpecialTruckAddons.List.class);
		for (SpecialTruckAddons.List list : SpecialTruckAddons.List.values()) {
			String listLabel = "";
			switch (list) {
			case BigCranes       : listLabel = "Big Crane for howling trucks"; break;
			case MiniCranes      : listLabel = "MiniCrane for loading cargo"; break;
			case LogLifts        : listLabel = "Log Lift for loading logs"; break;
			case MetalDetectors  : listLabel = "Metal Detector for special missions"; break;
			case SeismicVibrators: listLabel = "Seismic Vibrator for special missions"; break;
			}
			
			JCheckBoxMenuItem  mi = SnowRunner.createCheckBoxMenuItem(listLabel, false, null, true, e->{
				if (clickedItem==null) return;
				SpecialTruckAddonList addonList = specialTruckAddons.getList(list);
				if (addonList.contains(clickedItem))
					addonList.remove(clickedItem);
				else
					addonList.add(clickedItem);
			});
			specialAddonsMenu.add(mi);
			menuItems.put(list, mi);
		}
		
		contextMenu.addContextMenuInvokeListener((table, x, y) -> {
			int rowV = x==null || y==null ? -1 : table.rowAtPoint(new Point(x,y));
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			clickedItem = rowM<0 ? null : getRow(rowM);
			
			if (clickedItem!=null)
				menuItems.forEach((list,mi)->{
					SpecialTruckAddonList addonList = specialTruckAddons.getList(list);
					mi.setSelected(addonList.contains(clickedItem));
				});
			
			specialAddonsMenu.setEnabled(clickedItem!=null);
			specialAddonsMenu.setText(
				clickedItem==null
				? "Add Addon to a list of known special Addons"
				: String.format("Add Addon \"%s\" to a list of known special Addons", SnowRunner.solveStringID(clickedItem, language))
			);
		});
	}

	@Override public String getTextForRow(TruckAddon row) {
		String description_StringID = SnowRunner.selectNonNull( row.description_StringID, row.cargoDescription_StringID );
		String[][] requiredAddons = row.requiredAddons;
		String[] excludedCargoTypes = row.excludedCargoTypes;
		Vector<Truck> usableBy = row.usableBy;
		return SetInstancesTableModel.generateText(description_StringID, requiredAddons, excludedCargoTypes, usableBy, language, truckAddons, trailers);
	}
}