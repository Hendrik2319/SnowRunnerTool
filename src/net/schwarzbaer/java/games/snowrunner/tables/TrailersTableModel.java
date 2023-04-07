package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Point;
import java.awt.Window;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JTable;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.ContextMenu;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTPOS;

public class TrailersTableModel extends ExtendedVerySimpleTableModelTPOS<Trailer> {
	
	private HashMap<String, TruckAddon> truckAddons;
	private HashMap<String, Trailer> trailers;
	private SaveGame saveGame;
	private Trailer clickedRow;

	public TrailersTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, Data data, SaveGame saveGame) {
		this(mainWindow, controllers, connectToGlobalData);
		setExtraData(data);
		this.saveGame = saveGame;
	}
	public TrailersTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("ID"       ,"ID"                   ,  String.class, 230,   null,      null, false, row->((Trailer)row).id),
				new ColumnID("Name"     ,"Name"                 ,  String.class, 200,   null,      null,  true, row->((Trailer)row).gameData.name_StringID), 
				new ColumnID("UpdateLvl","Update Level"         ,  String.class,  80,   null,      null, false, row->((Trailer)row).updateLevel),
				new ColumnID("Owned"    ,"Owned"                ,    Long.class,  60, CENTER,      null, false, get((model, lang, row)->getOwnedCount(model,row))),
				new ColumnID("InstallSk","Install Socket"       ,  String.class, 130,   null,      null, false, row->((Trailer)row).gameData.installSocket),
				new ColumnID("CargoSlts","Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((Trailer)row).gameData.cargoSlots),
				new ColumnID("Repairs"  ,"Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((Trailer)row).repairsCapacity),
				new ColumnID("WheelRep" ,"Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((Trailer)row).wheelRepairsCapacity),
				new ColumnID("Fuel"     ,"Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((Trailer)row).fuelCapacity),
				new ColumnID("QuestItm" ,"Is Quest Item"        , Boolean.class,  80,   null,      null, false, row->((Trailer)row).gameData.isQuestItem),
				new ColumnID("Price"    ,"Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((Trailer)row).gameData.price), 
				new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Trailer)row).gameData.unlockByExploration), 
				new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((Trailer)row).gameData.unlockByRank), 
				new ColumnID("AttachTyp","Attach Type"          ,  String.class,  70,   null,      null, false, row->((Trailer)row).attachType),
				new ColumnID("AddonType","Addon Type"           ,  String.class,  70,   null,      null, false, row->((Trailer)row).gameData.addonType),
				new ColumnID("Desc"     ,"Description"          ,  String.class, 200,   null,      null,  true, row->((Trailer)row).gameData.description_StringID), 
				new ColumnID("ExclCargo","Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->SnowRunner.joinAddonIDs(((Trailer)row).gameData.excludedCargoTypes,true)),
				new ColumnID("RequAddon","Required Addons"      ,  String.class, 150,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((Trailer)row).gameData.requiredAddons)),
				new ColumnID("LoadAreas","Load Areas"           ,  String.class, 200,   null,      null, false, row->Data.GameData.GameDataT3NonTruck.LoadArea.toString(((Trailer)row).gameData.loadAreas)),
				new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinNames(((Trailer)row).usableBy, lang)),
		});
		
		truckAddons = null;
		trailers    = null;
		saveGame    = null;
		if (connectToGlobalData)
			connectToGlobalData(true, data->{
				truckAddons = data==null ? null : data.truckAddons;
				trailers    = data==null ? null : data.trailers;
				return trailers==null ? null : trailers.values();
			});
		else
			finalizer.addDataReceiver(data -> {
				setExtraData(data);
				updateTextOutput();
			});
		
		finalizer.addSaveGameListener(saveGame->{
			this.saveGame = saveGame;
			updateTextOutput();
		});
		
		setInitialRowOrder(Comparator.<Trailer,String>comparing(row->row.id));
	}
	private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,TrailersTableModel,Trailer> getFunction)
	{
		return ColumnID.get(TrailersTableModel.class, Trailer.class, getFunction);
	}
	
	private static Long getOwnedCount(TrailersTableModel model, Trailer row)
	{
		if (row==null) return null;
		if (model==null) return null;
		if (model.saveGame==null) return null;
		SaveGame.Addon addon = model.saveGame.addons.get(row.id);
		return addon==null ? null : addon.owned;
	}

	private void setExtraData(Data data) {
		truckAddons = data==null ? null : data.truckAddons;
		trailers    = data==null ? null : data.trailers;
	}

	@Override protected void setContentForRow(StyledDocumentInterface doc, Trailer row) {
		String description_StringID = row.gameData.description_StringID;
		String[][] requiredAddons   = row.gameData.requiredAddons;
		String[] excludedCargoTypes = row.gameData.excludedCargoTypes;
		Vector<Truck> usableBy = row.usableBy;
		TruckAddonsTableModel.generateText(doc, description_StringID, requiredAddons, excludedCargoTypes, usableBy, language, truckAddons, trailers, saveGame);
	}

	@Override protected String getRowName(Trailer row)
	{
		return SnowRunner.solveStringID(row, language);
	}
	
	@Override public void modifyTableContextMenu(JTable table_, ContextMenu contextMenu)
	{
		super.modifyTableContextMenu(table_, contextMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem miFilterTrucksByTrailer = contextMenu.add(SnowRunner.createMenuItem("Filter table \"Trucks\" by usability of this trailer", true, e->{
			if (clickedRow != null)
				finalizer.getControllers().filterTrucksByTrailersListeners.setFilter(clickedRow);
		}));
		
		contextMenu.addContextMenuInvokeListener((table, x, y) -> {
			Point p = x==null || y==null ? null : new Point(x,y);
			int rowV = p==null ? -1 : table.rowAtPoint(p);
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			clickedRow = rowM<0 ? null : getRow(rowM);
			
			String trailerName = clickedRow == null ? null : SnowRunner.solveStringID(clickedRow.getName_StringID(), language);
			miFilterTrucksByTrailer.setEnabled(clickedRow!=null);
			miFilterTrucksByTrailer.setText(
					trailerName == null
						?               "Filter table \"Trucks\" by usability of this trailer"
						: String.format("Filter table \"Trucks\" by usability of trailer \"%s\"", trailerName)
			);
			
		});
	}
}