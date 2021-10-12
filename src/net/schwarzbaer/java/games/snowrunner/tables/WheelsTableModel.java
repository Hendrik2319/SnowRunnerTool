package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;

public class WheelsTableModel extends VerySimpleTableModel<WheelsTableModel.RowItem> {

	public WheelsTableModel(Window mainWindow, Controllers controllers) {
		super(mainWindow, controllers, new ColumnID[] {
				new ColumnID("ID"     , "ID"     , String .class, 300,   null,    null, false, row -> ((RowItem)row).label),
				new ColumnID("Names"  , "Names"  , String .class, 130,   null,    null, (row,lang) -> getNameList ( ((RowItem)row).names_StringID, lang)),
				new ColumnID("Sizes"  , "Sizes"  , String .class, 300,   null,    null, false, row -> getSizeList ( ((RowItem)row).sizes  )),
				new ColumnID("Highway", "Highway", Float  .class,  55,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionHighway), 
				new ColumnID("Offroad", "Offroad", Float  .class,  50,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionOffroad), 
				new ColumnID("Mud"    , "Mud"    , Float  .class,  50,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionMud), 
				new ColumnID("OnIce"  , "On Ice" , Boolean.class,  50,   null,    null, false, row -> ((RowItem)row).tireValues.onIce), 
				new ColumnID("Trucks" , "Trucks" , String .class, 800,   null,    null, (row,lang) -> getTruckList( ((RowItem)row).trucks, lang)),
		});
		connectToGlobalData(WheelsTableModel::getData);
	}

	private static Vector<RowItem> getData(Data data) {
		HashMap<String,RowItem> rows = new HashMap<>();
		for (Truck truck:data.trucks.values()) {
			for (CompatibleWheel wheel : truck.compatibleWheels) {
				if (wheel.wheelsDef==null) continue;
				String wheelsDefID = wheel.wheelsDef.id;
				String dlc = wheel.wheelsDef.dlcName;
				for (int i=0; i<wheel.wheelsDef.truckTires.size(); i++) {
					TruckTire tire = wheel.wheelsDef.truckTires.get(i);
					String key   = String.format("%s|%s[%d]", dlc==null ? "----" : dlc, wheelsDefID, i);
					String label = dlc==null
							? String.format(     "%s [%d]",      wheelsDefID, i)
							: String.format("%s | %s [%d]", dlc, wheelsDefID, i);
					RowItem rowItem = rows.get(key);
					if (rowItem==null)
						rows.put(key, rowItem = new RowItem(key, label, new RowItem.TireValues(tire)));
					rowItem.add(wheel.scale,tire,truck);
				}
			}
		}
		Vector<RowItem> values = new Vector<>(rows.values());
		values.sort(Comparator.<RowItem,String>comparing(rowItem->rowItem.key));
		return values;
	}
	
	static class RowItem {

		final String key;
		final String label;
		final HashSet<Truck> trucks;
		final HashSet<Integer> sizes;
		final HashSet<String> names_StringID;
		final RowItem.TireValues tireValues;

		public RowItem(String key, String label, RowItem.TireValues tireValues) {
			this.key = key;
			this.label = label;
			this.tireValues = tireValues;
			trucks = new HashSet<>();
			sizes = new HashSet<>();
			names_StringID = new HashSet<>();
		}

		public void add(Float scale, TruckTire tire, Truck truck) {
			trucks.add(truck);
			sizes.add(CompatibleWheel.computeSize_inch(scale));
			names_StringID.add(tire.name_StringID);
			RowItem.TireValues newTireValues = new TireValues(tire);
			if (!tireValues.equals(newTireValues)) {
				System.err.printf("[WheelsTable] Found a wheel with same source (%s) but different values: %s <-> %s", key, tireValues, newTireValues);
			}
		}
		
		private static class TireValues {

			final Float frictionHighway;
			final Float frictionOffroad;
			final Float frictionMud;
			final Boolean onIce;

			public TireValues(TruckTire wheel) {
				frictionHighway = wheel.frictionHighway;
				frictionOffroad = wheel.frictionOffroad;
				frictionMud     = wheel.frictionMud;
				onIce           = wheel.onIce;
			}

			@Override
			public int hashCode() {
				int hashCode = 0;
				if (frictionHighway!=null) hashCode ^= frictionHighway.hashCode();
				if (frictionOffroad!=null) hashCode ^= frictionOffroad.hashCode();
				if (frictionMud    !=null) hashCode ^= frictionMud    .hashCode();
				if (onIce          !=null) hashCode ^= onIce          .hashCode();
				return hashCode;
			}

			@Override
			public boolean equals(Object obj) {
				if (!(obj instanceof RowItem.TireValues))
					return false;
				
				RowItem.TireValues other = (RowItem.TireValues) obj;
				if (!equals( this.frictionHighway, other.frictionHighway )) return false;
				if (!equals( this.frictionOffroad, other.frictionOffroad )) return false;
				if (!equals( this.frictionMud    , other.frictionMud     )) return false;
				if (!equals( this.onIce          , other.onIce           )) return false;
				
				return true;
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
				return String.format("(H:%1.2f,O:%1.2f,M:%1.2f%s)\"", frictionHighway, frictionOffroad, frictionMud, onIce!=null && onIce ? ",Ice" : "");
			}
			
		}
	}

	private static String getTruckList(HashSet<Truck> trucks, Language language) {
		Iterable<String> it = ()->trucks
				.stream()
				.map(t->SnowRunner.getTruckLabel(t,language))
				.sorted()
				.iterator();
		return trucks.isEmpty() ? "" :  String.join(", ", it);
	}

	private static String getSizeList(HashSet<Integer> sizes) {
		Iterable<String> it = ()->sizes
				.stream()
				.sorted()
				.map(size->String.format("%d\"", size))
				.iterator();
		return sizes.isEmpty() ? "" :  String.join(", ", it);
	}

	private static String getNameList(HashSet<String> names_StringID, Language language) {
		Iterable<String> it = ()->names_StringID
				.stream()
				.sorted()
				.map(strID->SnowRunner.solveStringID(strID, language))
				.iterator();
		return names_StringID.isEmpty() ? "" :  String.join(", ", it);
	}

}