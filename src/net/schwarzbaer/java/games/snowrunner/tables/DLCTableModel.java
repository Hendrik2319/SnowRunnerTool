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
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;

public class DLCTableModel extends SimplifiedTableModel<DLCTableModel.ColumnID> implements Finalizable {

	private Language language;
	private final Vector<RowItem> rows;
	private HashMap<String, String> truckToDLCAssignments;
	private Data data;
	private final Finalizer finalizer;

	public DLCTableModel(Controllers controllers) {
		super(ColumnID.values());
		language = null;
		truckToDLCAssignments = null;
		data = null;
		rows = new Vector<>();
		
		finalizer = controllers.createNewFinalizer();
		finalizer.addLanguageListener(language->{
			this.language = language;
			fireTableUpdate();
		});
		finalizer.addTruckToDLCAssignmentListener(new TruckToDLCAssignmentListener() {
			@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
				DLCTableModel.this.truckToDLCAssignments = truckToDLCAssignments;
				rebuildRows();
			}
			@Override public void updateAfterAssignmentsChange() {
				rebuildRows();
			}
		});
		finalizer.addDataReceiver(data->{
			this.data = data;
			rebuildRows();
		});
	}

	@Override public void prepareRemovingFromGUI() {
		finalizer.removeSubCompsAndListenersFromGUI();
	}
	
	private void rebuildRows() {
		HashSet<String> truckIDs = new HashSet<>();
		if (truckToDLCAssignments!=null) truckIDs.addAll(truckToDLCAssignments.keySet());
		if (data                 !=null) truckIDs.addAll(data.trucks          .keySet());
		
		rows.clear();
		for (String truckID:truckIDs) {
			Truck truck = data==null ? null : data.trucks.get(truckID);
			String updateLevel = truck==null ? null : truck.updateLevel==null ? "<Launch>" : truck.updateLevel;
			String dlc = truckToDLCAssignments==null ? null : truckToDLCAssignments.get(truckID);
			if ((updateLevel!=null && !updateLevel.equals("<Launch>")) || dlc!=null)
				rows.add(new RowItem(updateLevel, dlc, truckID));
		}
		fireTableUpdate();
	}

	private static class RowItem {
		final String updateLevel;
		final String officialDLC;
		final String truckID;
		private RowItem(String internalDLC, String officialDLC, String truckID) {
			this.updateLevel = internalDLC;
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
		case UpdateLevel: return row.updateLevel;
		case OfficialDLC: return row.officialDLC;
		case Truck:
			Truck truck = data==null || row.truckID==null ? null : data.trucks.get(row.truckID);
			if (truck == null) return String.format("<%s>", row.truckID);
			return SnowRunner.getTruckLabel(truck, language);
		}
		return null;
	}

	enum ColumnID implements Tables.SimplifiedColumnIDInterface {
		UpdateLevel ("Update Level", String .class, 100),
		OfficialDLC ("Official DLC", String .class, 200),
		Truck       ("Truck"       , String .class, 200),
		;
	
		private final SimplifiedColumnConfig config;
		ColumnID(String name, Class<?> columnClass, int prefWidth) {
			config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
	}
}