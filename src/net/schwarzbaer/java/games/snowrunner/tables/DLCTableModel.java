package net.schwarzbaer.java.games.snowrunner.tables;

import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;

public class DLCTableModel extends SimplifiedTableModel<DLCTableModel.ColumnID> implements Finalizable {

	private final Finalizer finalizer;
	private final Vector<RowItem> rows;
	private Language language;
	private Data data;
	private SaveGame savegame;
	private final GlobalFinalDataStructures gfds;

	public DLCTableModel(GlobalFinalDataStructures gfds) {
		super(ColumnID.values());
		this.gfds = gfds;
		language = null;
		data = null;
		rows = new Vector<>();
		
		finalizer = gfds.controllers.createNewFinalizer();
		finalizer.addLanguageListener(language->{
			this.language = language;
			fireTableUpdate();
		});
		finalizer.addDLCListener(() ->
			rebuildRows()
		);
		finalizer.addDataReceiver(data->{
			this.data = data;
			rebuildRows();
		});
		finalizer.addSaveGameListener(savegame->{
			this.savegame = savegame;
			rebuildRows();
		});
	}

	@Override public void prepareRemovingFromGUI() {
		finalizer.removeSubCompsAndListenersFromGUI();
	}
	
	private void rebuildRows() {
		HashSet<String> truckIDs = new HashSet<>();
		truckIDs.addAll(gfds.dlcs.getTruckIDs());
		if (data!=null) truckIDs.addAll(data.trucks.keySet());
		Vector<String> sortedTruckIDs = new Vector<>(truckIDs);
		sortedTruckIDs.sort(null);
		
		HashSet<String> mapIDs = new HashSet<>();
		mapIDs.addAll(gfds.dlcs.getMapIDs());
		if (savegame!=null) mapIDs.addAll(savegame.maps.keySet());
		Vector<String> sortedMapIDs = new Vector<>(mapIDs);
		sortedMapIDs.sort(null);
		
		rows.clear();
		for (String truckID : sortedTruckIDs) {
			Truck truck = data==null ? null : data.trucks.get(truckID);
			String updateLevel = truck==null ? null : truck.updateLevel==null ? "<Launch>" : truck.updateLevel;
			String dlc = gfds.dlcs.getDLCofTruck(truckID);
			if ((updateLevel!=null && !updateLevel.equals("<Launch>")) || dlc!=null)
				rows.add(new RowItem(updateLevel, dlc, truckID, null));
		}
		for (String mapID : sortedMapIDs) {
			//SaveGame.MapInfos map = savegame==null ? null : savegame.maps.get(mapID);
			String updateLevel = null;
			String dlc = gfds.dlcs.getDLCofMap(mapID);
			if (dlc!=null)
				rows.add(new RowItem(updateLevel, dlc, null, Data.MapIndex.parse(mapID)));
		}
		fireTableUpdate();
	}

	private record RowItem(String updateLevel, String dlc, String truckID, Data.MapIndex mapIndex) {}


	@Override public int getRowCount() {
		return rows.size();
	}

	@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
		if (rowIndex<0 || rowIndex>=rows.size()) return null;
		RowItem row = rows.get(rowIndex);
		switch (columnID) {
			case UpdateLevel: return row.updateLevel;
			case OfficialDLC: return row.dlc;
			case Truck:
				Truck truck = data==null || row.truckID==null ? null : data.trucks.get(row.truckID);
				if (truck == null) return String.format("<%s>", row.truckID);
				return SnowRunner.getTruckLabel(truck, language);
			case Map:
				if (row.mapIndex==null) return null;
				return language.regionNames.getNameForMap(row.mapIndex, ()->"<"+row.mapIndex.originalMapID()+">");
		}
		return null;
	}

	enum ColumnID implements Tables.SimplifiedColumnIDInterface {
		UpdateLevel ("Update Level", String .class, 100),
		OfficialDLC ("Official DLC", String .class, 200),
		Truck       ("Truck"       , String .class, 200),
		Map         ("Map"         , String .class, 200),
		;
	
		private final SimplifiedColumnConfig config;
		ColumnID(String name, Class<?> columnClass, int prefWidth) {
			config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
	}
}