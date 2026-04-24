package net.schwarzbaer.java.games.snowrunner.tables;

import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedTableModel;

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
		
		finalizer = this.gfds.controllers.createNewFinalizer();
		finalizer.addLanguageListener(language->{
			this.language = language;
			fireTableUpdate();
		});
		finalizer.addDLCListener(new SnowRunner.DLCs.Listener() {
			@Override public void updateAfterAssignmentChange() { rebuildRows(); }
			@Override public void dlcOwnChanged() { fireTableColumnUpdate(ColumnID.OwnedByUser); }
		} );
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
		truckIDs.addAll(gfds.dlcs.getIDs(SnowRunner.DLCs.ItemType.Truck));
		if (data!=null) truckIDs.addAll(data.trucks.keySet());
		Vector<String> sortedTruckIDs = new Vector<>(truckIDs);
		sortedTruckIDs.sort(null);
		
		HashSet<String> regionIDs = new HashSet<>();
		regionIDs.addAll(gfds.dlcs.getIDs(SnowRunner.DLCs.ItemType.Region));
		if (savegame!=null) regionIDs.addAll(savegame.maps.keySet());
		Vector<String> sortedRegionIDs = new Vector<>(regionIDs);
		sortedRegionIDs.sort(null);
		
		HashSet<String> mapIDs = new HashSet<>();
		mapIDs.addAll(gfds.dlcs.getIDs(SnowRunner.DLCs.ItemType.Map));
		if (savegame!=null) mapIDs.addAll(savegame.maps.keySet());
		Vector<String> sortedMapIDs = new Vector<>(mapIDs);
		sortedMapIDs.sort(null);
		
		rows.clear();
		for (SnowRunner.DLCs.ItemType itemType : SnowRunner.DLCs.ItemType.values())
			switch (itemType)
			{
				case Truck:
					for (String truckID : sortedTruckIDs) {
						Truck truck = data==null ? null : data.trucks.get(truckID);
						String updateLevel = truck==null ? null : truck.updateLevel==null ? "<Launch>" : truck.updateLevel;
						String dlc = gfds.dlcs.getDLC(truckID, itemType);
						if ((updateLevel!=null && !updateLevel.equals("<Launch>")) || dlc!=null)
							rows.add(new RowItem(updateLevel, dlc, truckID, null, null));
					}
					break;
					
				case Region:
					for (String regionID : sortedRegionIDs) {
						String dlc = gfds.dlcs.getDLC(regionID, itemType);
						if (dlc!=null)
							rows.add(new RowItem(null, dlc, null, null, Data.MapIndex.parse(regionID)));
					}
					break;
					
				case Map:
					for (String mapID : sortedMapIDs) {
						String dlc = gfds.dlcs.getDLC(mapID, itemType);
						if (dlc!=null)
							rows.add(new RowItem(null, dlc, null, Data.MapIndex.parse(mapID), null));
					}
					break;
			}
		fireTableUpdate();
	}

	private record RowItem(String updateLevel, String dlc, String truckID, Data.MapIndex mapIndex, Data.MapIndex regionIndex) {}


	@Override public int getRowCount() {
		return rows.size();
	}
	
	private RowItem getRow(int rowIndex)
	{
		if (rowIndex<0 || rowIndex>=rows.size()) return null;
		return rows.get(rowIndex);
	}

	@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID)
	{
		RowItem row = getRow(rowIndex);
		if (row==null) return null;
		switch (columnID) {
			case UpdateLevel: return row.updateLevel;
			case OfficialDLC: return row.dlc;
			case OwnedByUser: return SnowRunner.DLCs.isOwnedByUser(row.dlc);
			case Truck:
				Truck truck = data==null || row.truckID==null ? null : data.trucks.get(row.truckID);
				if (truck       != null) return SnowRunner.getTruckLabel(truck, language);
				if (row.truckID != null) return String.format("<%s>", row.truckID);
				return null;
			case Region:
				if (row.regionIndex==null) return null;
				return language.regionNames.getName(row.regionIndex, ()->"<"+row.regionIndex.originalMapID()+">");
			case Map:
				if (row.mapIndex==null) return null;
				return language.regionNames.getName(row.mapIndex, ()->"<"+row.mapIndex.originalMapID()+">");
		}
		return null;
	}

	@Override
	protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
	{
		return columnID == ColumnID.OwnedByUser;
	}

	@Override
	protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
	{
		RowItem row = getRow(rowIndex);
		if (row==null || columnID==null) return;
		switch (columnID)
		{
		case OwnedByUser:
			if (aValue instanceof Boolean boolValue)
			{
				SnowRunner.DLCs.setOwnedByUser(row.dlc, boolValue.booleanValue());
				gfds.controllers.dlcListeners.dlcOwnChanged();
			}
			break;
		default:
			break;
		}
	}

	enum ColumnID implements Tables.SimplifiedColumnIDInterface
	{
		// Column Widths: [100, 200, 110, 200, 200, 350] in ModelOrder
		UpdateLevel ("Update Level"     , String .class, 100),
		OfficialDLC ("Official DLC"     , String .class, 200),
		OwnedByUser ("DLC owned by User", Boolean.class, 110),
		Truck       ("Truck"            , String .class, 200),
		Region      ("Region"           , String .class, 200),
		Map         ("Map"              , String .class, 350),
		;
	
		private final SimplifiedColumnConfig config;
		ColumnID(String name, Class<?> columnClass, int prefWidth) {
			config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
	}
}