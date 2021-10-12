package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Window;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckComponent;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;

class SetInstancesTableModel<RowType extends TruckComponent> extends ExtendedVerySimpleTableModel<RowType> {

	SetInstancesTableModel(Window mainWindow, Controllers controllers, ColumnID[] columns) {
		super(mainWindow, controllers, columns);
		setInitialRowOrder(Comparator.<RowType,String>comparing(e->e.setID).thenComparing(e->e.id));
	}

	@Override
	protected String getTextForRow(RowType row) {
		String description_StringID = row.description_StringID;
		Vector<Truck> usableBy = row.usableBy;
		return generateText(description_StringID, null, null, usableBy, language, null, null);
	}

	static String generateText(
			String description_StringID,
			String[][] requiredAddons,
			String[] excludedCargoTypes,
			Vector<Truck> usableBy,
			Language language, HashMap<String, TruckAddon> truckAddons, HashMap<String, Trailer> trailers) {
		
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		
		
		String description = SnowRunner.solveStringID(description_StringID, language);
		if (description!=null && !"EMPTY_LINE".equals(description)) {
			sb.append(String.format("Description: <%s>%n%s", description_StringID, description));
			isFirst = false;
		}
		
		if (requiredAddons!=null && requiredAddons.length>0) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Required Addons:\r\n");
			if (truckAddons==null && trailers==null)
				sb.append(SnowRunner.joinRequiredAddonsToString(requiredAddons, "    "));
			else {
				sb.append("    [IDs]\r\n");
				sb.append(SnowRunner.joinRequiredAddonsToString(requiredAddons, "        ")+"\r\n");
				sb.append("    [Names]\r\n");
				sb.append(SnowRunner.joinRequiredAddonsToString(requiredAddons, SnowRunner.createGetNameFunction(truckAddons, trailers), language, "        "));
			}
		}
		
		if (excludedCargoTypes!=null && excludedCargoTypes.length>0) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Excluded Cargo Types:\r\n");
			sb.append(SnowRunner.joinAddonIDs(excludedCargoTypes));
		}
		
		if (usableBy!=null && !usableBy.isEmpty()) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Usable By:\r\n");
			sb.append(SnowRunner.joinTruckNames(usableBy,language));
		}
		
		return sb.toString();
	}
}