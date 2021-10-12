package net.schwarzbaer.java.games.snowrunner.tables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.ListenerSource;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;

public class DLCTableModel extends SimplifiedTableModel<DLCTableModel.ColumnID> implements LanguageListener, TruckToDLCAssignmentListener, DataReceiver, ListenerSource {

	private Language language;
	private final Vector<RowItem> rows;
	private HashMap<String, String> truckToDLCAssignments;
	private Data data;

	public DLCTableModel(Controllers controllers) {
		super(ColumnID.values());
		language = null;
		truckToDLCAssignments = null;
		data = null;
		rows = new Vector<>();
		controllers.languageListeners.add(this,this);
		controllers.truckToDLCAssignmentListeners.add(this,this);
		controllers.dataReceivers.add(this,this);
	}

	@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
		this.truckToDLCAssignments = truckToDLCAssignments;
		rebuildRows();
	}

	@Override public void updateAfterAssignmentsChange() {
		rebuildRows();
	}

	@Override public void setLanguage(Language language) {
		this.language = language;
		fireTableUpdate();
	}
	
	@Override public void setData(Data data) {
		this.data = data;
		rebuildRows();
	}
	
	private void rebuildRows() {
		HashSet<String> truckIDs = new HashSet<>();
		if (truckToDLCAssignments!=null) truckIDs.addAll(truckToDLCAssignments.keySet());
		if (data                 !=null) truckIDs.addAll(data.trucks          .keySet());
		
		rows.clear();
		for (String truckID:truckIDs) {
			Truck truck = data==null ? null : data.trucks.get(truckID);
			String internalDLC = truck==null ? null : truck.dlcName==null ? "<LaunchDLC>" : truck.dlcName;
			String officialDLC = truckToDLCAssignments==null ? null : truckToDLCAssignments.get(truckID);
			if ((internalDLC!=null && !internalDLC.equals("<LaunchDLC>")) || officialDLC!=null)
				rows.add(new RowItem(internalDLC, officialDLC, truckID));
		}
		fireTableUpdate();
	}

	private static class RowItem {
		final String internalDLC;
		final String officialDLC;
		final String truckID;
		private RowItem(String internalDLC, String officialDLC, String truckID) {
			this.internalDLC = internalDLC;
			this.officialDLC = officialDLC;
			this.truckID = truckID;
		}
	}


	@Override public int getRowCount() {
		return rows.size();
	}

	@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
		if (rowIndex<0 || rowIndex>=rows.size()) return null;
		RowItem row = rows.get(rowIndex);
		switch (columnID) {
		case Internal: return row.internalDLC;
		case Official: return row.officialDLC;
		case Truck:
			Truck truck = data==null || row.truckID==null ? null : data.trucks.get(row.truckID);
			if (truck == null) return String.format("<%s>", row.truckID);
			return SnowRunner.getTruckLabel(truck, language, false);
		}
		return null;
	}

	enum ColumnID implements Tables.SimplifiedColumnIDInterface {
		Internal ("Internal Label", String .class, 100),
		Official ("Official DLC"  , String .class, 200),
		Truck    ("Truck"         , String .class, 200),
		;
	
		private final SimplifiedColumnConfig config;
		ColumnID(String name, Class<?> columnClass, int prefWidth) {
			config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
	}
}