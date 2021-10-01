package net.schwarzbaer.java.games.snowrunner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.ExtendedVerySimpleTableModel;

class TrailersTableModel extends ExtendedVerySimpleTableModel<Data.Trailer> {
	
	TrailersTableModel(Controllers controllers, boolean connectToGlobalData) {
		super(controllers, new ColumnID[] {
				new ColumnID("ID"                   ,  String.class, 230,   null,    null, false, row->((Data.Trailer)row).id),
				new ColumnID("Name"                 ,  String.class, 200,   null,    null,  true, row->((Data.Trailer)row).name_StringID), 
				new ColumnID("DLC"                  ,  String.class,  80,   null,    null, false, row->((Data.Trailer)row).dlcName),
				new ColumnID("Install Socket"       ,  String.class, 130,   null,    null, false, row->((Data.Trailer)row).installSocket),
				new ColumnID("Cargo Slots"          , Integer.class,  70, CENTER,    null, false, row->((Data.Trailer)row).cargoSlots),
				new ColumnID("Repairs"              , Integer.class,  50,  RIGHT,  "%d R", false, row->((Data.Trailer)row).repairsCapacity),
				new ColumnID("Wheel Repairs"        , Integer.class,  85, CENTER, "%d WR", false, row->((Data.Trailer)row).wheelRepairsCapacity),
				new ColumnID("Fuel"                 , Integer.class,  50,  RIGHT,  "%d L", false, row->((Data.Trailer)row).fuelCapacity),
				new ColumnID("Is Quest Item"        , Boolean.class,  80,   null,    null, false, row->((Data.Trailer)row).isQuestItem),
				new ColumnID("Price"                , Integer.class,  50,  RIGHT, "%d Cr", false, row->((Data.Trailer)row).price), 
				new ColumnID("Unlock By Exploration", Boolean.class, 120,   null,    null, false, row->((Data.Trailer)row).unlockByExploration), 
				new ColumnID("Unlock By Rank"       , Integer.class, 100, CENTER,    null, false, row->((Data.Trailer)row).unlockByRank), 
				new ColumnID("Attach Type"          ,  String.class,  70,   null,    null, false, row->((Data.Trailer)row).attachType),
				new ColumnID("Description"          ,  String.class, 200,   null,    null,  true, row->((Data.Trailer)row).description_StringID), 
				new ColumnID("Excluded Cargo Types" ,  String.class, 150,   null,    null, false, row->toString(((Data.Trailer)row).excludedCargoTypes)),
				new ColumnID("Required Addons"      ,  String.class, 150,   null,    null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((Data.Trailer)row).requiredAddons)),
				new ColumnID("Usable By"            ,  String.class, 150,   null,    null, (row,lang)->SnowRunner.toString(((Data.Trailer)row).usableBy, lang)),
		});
		if (connectToGlobalData)
			connectToGlobalData(data->{
				Vector<Trailer> values = new Vector<>(data.trailers.values());
				values.sort(Comparator.<Data.Trailer,String>comparing(rowItem->rowItem.id));
				return values;
			});
	}

	@Override public String getTextForRow(Data.Trailer row) {
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		
		
		String description = SnowRunner.solveStringID(row.description_StringID, language);
		if (description!=null && !"EMPTY_LINE".equals(description)) {
			sb.append(String.format("Description: <%s>%n%s", row.description_StringID, description));
			isFirst = false;
		}
		
		if (row.requiredAddons.length>0) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Required Addons:\r\n");
			sb.append(SnowRunner.joinRequiredAddonsToString(row.requiredAddons, "  "));
		}
		
		if (row.excludedCargoTypes.length>0) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Excluded Cargo Types:\r\n");
			sb.append(toString(row.excludedCargoTypes));
		}
		
		if (!row.usableBy.isEmpty()) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Usable By:\r\n");
			sb.append(SnowRunner.toString(row.usableBy,language));
		}
		
		return sb.toString();
	}

	private static String toString(String[] strs) {
		if (strs==null) return "<null>";
		if (strs.length==0) return "[]";
		if (strs.length==1) return strs[0];
		return Arrays.toString(strs);
	}
}