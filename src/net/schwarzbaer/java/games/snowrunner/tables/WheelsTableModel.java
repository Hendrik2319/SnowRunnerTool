package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.WheelsQualityRanges.QualityValue;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.WheelsQualityRanges.WheelValue;
import net.schwarzbaer.java.lib.gui.Tables;

public class WheelsTableModel extends VerySimpleTableModel<WheelsTableModel.RowItem> {

	private static final String QV_HIGH = "QV_High";
	private static final String QV_OFFR = "QV_Offr";
	private static final String QV_MUD  = "QV_Mud";
	public WheelsTableModel(Window mainWindow, GlobalFinalDataStructures gfds) {
		super(mainWindow, gfds, new ColumnID[] {
				new ColumnID("ID"     , "ID"     , String      .class, 300,   null,    null, false, row -> ((RowItem)row).label),
				new ColumnID("TireDef", "TireDef", String      .class, 110,   null,    null, false, row -> ((RowItem)row).tireValues.tireDefID),
				new ColumnID("Names"  , "Names"  , String      .class, 130,   null,    null, (row,lang) -> SnowRunner.joinStringIDs(((RowItem)row).names_StringID, lang)),
				new ColumnID("Sizes"  , "Sizes"  , String      .class, 300,   null,    null, false, row -> getSizeList ( ((RowItem)row).sizes  )),
				new ColumnID("Highway", "Highway", Float       .class,  55,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionHighway), 
				new ColumnID("Offroad", "Offroad", Float       .class,  50,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionOffroad), 
				new ColumnID("Mud"    , "Mud"    , Float       .class,  50,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionMud), 
				new ColumnID("OnIce"  , "On Ice" , Boolean     .class,  50,   null,    null, false, row -> ((RowItem)row).tireValues.onIce), 
				new ColumnID( QV_HIGH , "Highway", QualityValue.class,  55, CENTER,    null, false, row -> ((RowItem)row).getQualityValue(WheelValue.Highway)),
				new ColumnID( QV_OFFR , "Offroad", QualityValue.class,  55, CENTER,    null, false, row -> ((RowItem)row).getQualityValue(WheelValue.Offroad)),
				new ColumnID( QV_MUD  , "Mud"    , QualityValue.class,  55, CENTER,    null, false, row -> ((RowItem)row).getQualityValue(WheelValue.Mud    )),
				new ColumnID("Trucks" , "Trucks" , String      .class, 800,   null,    null, (row,lang) -> SnowRunner.joinNames(((RowItem)row).trucks, lang)),
		});
		connectToGlobalData(this::getData);
		finalizer.addWheelsQualityRangesListener((wheelsDefID, indexInDef, wheelValue) -> {
			if (wheelValue!=null)
				switch (wheelValue)
				{
					case Highway: fireTableColumnUpdate(QV_HIGH); break;
					case Offroad: fireTableColumnUpdate(QV_OFFR); break;
					case Mud    : fireTableColumnUpdate(QV_MUD ); break;
				}
		});
	}

	@Override protected String getRowName(RowItem row)
	{
		return row==null ? null : SnowRunner.joinStringIDs(row.names_StringID, language);
	}

	private Vector<RowItem> getData(Data data) {
		HashMap<String,RowItem> rows = new HashMap<>();
		for (Truck truck:data.trucks.values()) {
			for (CompatibleWheel wheel : truck.compatibleWheels) {
				if (wheel.wheelsDef==null) continue;
				String wheelsDefID = wheel.wheelsDef.id;
				String dlc = wheel.wheelsDef.updateLevel;
				for (int i=0; i<wheel.wheelsDef.truckTires.size(); i++) {
					TruckTire tire = wheel.wheelsDef.truckTires.get(i);
					String key   = String.format("%s|%s[%d]", dlc==null ? "----" : dlc, wheelsDefID, i);
					String label = dlc==null
							? String.format(     "%s [%d]",      wheelsDefID, i)
							: String.format("%s | %s [%d]", dlc, wheelsDefID, i);
					RowItem rowItem = rows.get(key);
					if (rowItem==null)
						rows.put(key, rowItem = new RowItem(wheelsDefID, i, key, label, new RowItem.TireValues(tire)));
					rowItem.add(wheel.scale,tire,truck);
				}
			}
		}
		Vector<RowItem> values = new Vector<>(rows.values());
		values.sort(Comparator.<RowItem,String>comparing(rowItem->rowItem.key));
		return values;
	}
	
	private static String getSizeList(HashSet<Integer> sizes) {
		Iterable<String> it = ()->sizes
				.stream()
				.sorted()
				.map(size->String.format("%d\"", size))
				.iterator();
		return sizes.isEmpty() ? "" :  String.join(", ", it);
	}

	class RowItem {

		final String wheelsDefID;
		final int indexInDef;
		final String key;
		final String label;
		final HashSet<Truck> trucks;
		final HashSet<Integer> sizes;
		final HashSet<String> names_StringID;
		final TireValues tireValues;

		public RowItem(String wheelsDefID, int indexInDef, String key, String label, TireValues tireValues) {
			this.wheelsDefID = wheelsDefID;
			this.indexInDef = indexInDef;
			this.key = key;
			this.label = label;
			this.tireValues = tireValues;
			trucks = new HashSet<>();
			sizes = new HashSet<>();
			names_StringID = new HashSet<>();
		}

		QualityValue getQualityValue(WheelValue wheelValue)
		{
			return gfds.wheelsQualityRanges.getQualityValue(wheelsDefID, indexInDef, null, wheelValue);
		}
		void setQualityValue(WheelValue wheelValue, QualityValue qualityValue)
		{
			gfds.wheelsQualityRanges.setQualityValue(wheelsDefID, indexInDef, null, wheelValue, qualityValue);
		}

		public void add(Float scale, TruckTire tire, Truck truck) {
			trucks.add(truck);
			sizes.add(CompatibleWheel.computeSize_inch(scale));
			names_StringID.add(tire.gameData.getNameStringID());
			TireValues newTireValues = new TireValues(tire);
			if (!tireValues.equals(newTireValues)) {
				System.err.printf("[WheelsTable] Found a wheel with same source (%s) but different values: %s <-> %s", key, tireValues, newTireValues);
			}
		}
		
		private static class TireValues {

			final String tireDefID;
			final Float frictionHighway;
			final Float frictionOffroad;
			final Float frictionMud;
			final Boolean onIce;

			public TireValues(TruckTire wheel) {
				tireDefID       = wheel.tireDefID;
				frictionHighway = wheel.frictionHighway;
				frictionOffroad = wheel.frictionOffroad;
				frictionMud     = wheel.frictionMud;
				onIce           = wheel.onIce;
			}

			@Override
			public int hashCode() {
				int hashCode = 0;
				if (tireDefID      !=null) hashCode ^= tireDefID      .hashCode();
				if (frictionHighway!=null) hashCode ^= frictionHighway.hashCode();
				if (frictionOffroad!=null) hashCode ^= frictionOffroad.hashCode();
				if (frictionMud    !=null) hashCode ^= frictionMud    .hashCode();
				if (onIce          !=null) hashCode ^= onIce          .hashCode();
				return hashCode;
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof TireValues))
					return false;
				
				TireValues other = (TireValues) obj;
				if (!equals( this.tireDefID      , other.tireDefID       )) return false;
				if (!equals( this.frictionHighway, other.frictionHighway )) return false;
				if (!equals( this.frictionOffroad, other.frictionOffroad )) return false;
				if (!equals( this.frictionMud    , other.frictionMud     )) return false;
				if (!equals( this.onIce          , other.onIce           )) return false;
				
				return true;
			}

			private boolean equals(String v1, String v2) {
				if (v1==null && v2==null) return true;
				if (v1==null || v2==null) return false;
				return v1.equals(v2);
			}

			private boolean equals(Boolean v1, Boolean v2) {
				if (v1==null && v2==null) return true;
				if (v1==null || v2==null) return false;
				return v1.booleanValue() == v2.booleanValue();
			}

			private boolean equals(Float v1, Float v2) {
				if (v1==null && v2==null) return true;
				if (v1==null || v2==null) return false;
				return v1.floatValue() == v2.floatValue();
			}

			@Override public String toString() {
				return String.format("(T:%s,H:%1.2f,O:%1.2f,M:%1.2f%s)\"", tireDefID, frictionHighway, frictionOffroad, frictionMud, onIce!=null && onIce ? ",Ice" : "");
			}
			
		}
	}

	@Override
	public void reconfigureAfterTableStructureUpdate()
	{
		super.reconfigureAfterTableStructureUpdate();
		table.setDefaultEditor(
				QualityValue.class,
				new Tables.ComboboxCellEditor<>(
						SnowRunner.addNull(QualityValue.values())
				)
		);
	}

	@Override
	protected boolean isCellEditable(int rowIndex, int columnIndex, ColumnID columnID)
	{
		if (columnID!=null)
			switch (columnID.id)
			{
				case QV_HIGH:
				case QV_OFFR:
				case QV_MUD:
					return true;
			}
		return false;
	}

	@Override
	protected void setValueAt(Object aValue, int rowIndex, int columnIndex, ColumnID columnID)
	{
		RowItem row = getRow(rowIndex);
		if (row==null) return;
		
		if (columnID==null) return;
		switch (columnID.id)
		{
			case QV_HIGH: row.setQualityValue(WheelValue.Highway, (QualityValue) aValue); break;
			case QV_OFFR: row.setQualityValue(WheelValue.Offroad, (QualityValue) aValue); break;
			case QV_MUD : row.setQualityValue(WheelValue.Mud    , (QualityValue) aValue); break;
		}
	}

}