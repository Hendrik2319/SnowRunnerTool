package net.schwarzbaer.java.games.snowrunner;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.RowTextTableModel;

class TruckAddonsTableModel extends Tables.SimplifiedTableModel<TruckAddonsTableModel.ColumnID> implements LanguageListener, DataReceiver, RowTextTableModel {
	
	private Language language;
	private final Vector<Data.TruckAddon> rows;
	private Runnable textAreaUpdateMethod;
	
	TruckAddonsTableModel(Controllers controllers) {
		super(ColumnID.values());
		language = null;
		textAreaUpdateMethod = null;
		rows = new Vector<>();
		controllers.languageListeners.add(this);
		controllers.dataReceivers.add(this);
	}
	
	@Override public void setLanguage(Language language) {
		this.language = language;
		if (textAreaUpdateMethod!=null)
			textAreaUpdateMethod.run();
		fireTableUpdate();
	}
	
	@Override public void setData(Data data) {
		setData(data==null ? null : data.truckAddons.values());
	}

	void setData(Collection<TruckAddon> data) {
		rows.clear();
		if (data!=null) {
			rows.addAll(data);
			Comparator<String> nullsLast_String = Comparator.nullsLast(Comparator.naturalOrder());
			rows.sort(Comparator.<Data.TruckAddon,String>comparing(rowItem->rowItem.category,nullsLast_String).thenComparing(rowItem->rowItem.id));
		}
		if (textAreaUpdateMethod!=null)
			textAreaUpdateMethod.run();
		fireTableUpdate();
	}

	private class CWTableCellRenderer implements TableCellRenderer {
	
		private final Tables.LabelRendererComponent rendererComp;
	
		CWTableCellRenderer() {
			rendererComp = new Tables.LabelRendererComponent();
		}
	
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			String valueStr = value==null ? null : value.toString();
			
			int columnM = table.convertColumnIndexToModel(columnV);
			TruckAddonsTableModel.ColumnID columnID = getColumnID(columnM);
			
			if (columnID!=null) {
				//if (columnID.config.columnClass==Float.class) {
				//	valueStr = value==null ? "<???>" : String.format(Locale.ENGLISH, "%1.2f", value);
				//	rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
				//}
				if (columnID.config.columnClass==Integer.class) {
					switch (columnID) {
					
					case Price:
						valueStr = value==null ? "" : String.format("%d Cr", value);
						rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
						break;
						
					case UnlockByRank:
					case CargoSlots:
					case CargoLength:
						rendererComp.setHorizontalAlignment(SwingConstants.CENTER);
						break;
						
					case FuelCapacity:
						valueStr = value==null ? "" : String.format("%d L", value);
						rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
						break;
						
					case RepairsCapacity:
						valueStr = value==null ? "" : String.format("%d R", value);
						rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
						break;
						
					case WheelRepairsCapacity:
						valueStr = value==null ? "" : String.format("%d WR", value);
						rendererComp.setHorizontalAlignment(SwingConstants.CENTER);
						break;
						
					default:
						rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
						break;
					}
				}
			}
			
			rendererComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
			return rendererComp;
		}
	
	}
	
	@Override public void setTable(JTable table) {
		super.setTable(table);
		
		CWTableCellRenderer renderer = new CWTableCellRenderer();
		//table.setDefaultRenderer(String .class, renderer);
		table.setDefaultRenderer(Integer.class, renderer);
		//table.setDefaultRenderer(Float  .class, renderer);
		//table.setDefaultRenderer(Boolean.class, null);
	}
	
	@Override public void setTextAreaUpdateMethod(Runnable textAreaUpdateMethod) {
		this.textAreaUpdateMethod = textAreaUpdateMethod;
	}

	@Override public String getTextForRow(int rowIndex) {
		Data.TruckAddon row = getRow(rowIndex);
		if (row==null) return null;
		
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		
		String description_StringID = SnowRunner.selectNonNull( row.description_StringID, row.cargoDescription_StringID );
		String description = SnowRunner.solveStringID(description_StringID, language);
		if (description!=null && !"EMPTY_LINE".equals(description)) {
			sb.append(String.format("Description: <%s>%n%s", description_StringID, description));
			isFirst = false;
		}
		
		if (row.requiredAddons.length>0) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Required Addons:");
			for (int i=0; i<row.requiredAddons.length; i++) {
				for (int j=0; j<row.requiredAddons[i].length; j++) {
					String ect = row.requiredAddons[i][j];
					sb.append(String.format("%n      %s%s", ect, j+1<row.requiredAddons[i].length ? " or" : ""));
				}
				if (i+1<row.requiredAddons.length)
					sb.append(String.format("%n   and"));
			}
		}
		
		if (row.excludedCargoTypes.length>0) {
			if (!isFirst) sb.append("\r\n\r\n");
			isFirst = false;
			sb.append("Excluded Cargo Types:");
			for (String ect : row.excludedCargoTypes)
				sb.append(String.format("%n   %s", ect));
		}
		
		return sb.toString();
	}

	@Override public int getRowCount() {
		return rows.size();
	}

	Data.TruckAddon getRow(int rowIndex) {
		if (rowIndex<0 || rowIndex>=rows.size()) return null;
		return rows.get(rowIndex);
	}

	private String toString(String[] strs) {
		if (strs==null) return "<null>";
		if (strs.length==0) return "[]";
		if (strs.length==1) return strs[0];
		return Arrays.toString(strs);
	}

	private String toString(String[][] strs) {
		if (strs==null) return "<null>";
		if (strs.length==0) return "-----";
		
		Iterable<String> it = ()->Arrays.stream(strs).map(list->{
			String str = String.join(" || ", Arrays.asList(list));
			if (list.length==1) return str;
			return String.format("(%s)", str);
		}).iterator();
		
		String str = String.join(" && ", it);
		if (strs.length==1) return str;
		return String.format("(%s)", str);
	}
	
	@Override public Object getValueAt(int rowIndex, int columnIndex, TruckAddonsTableModel.ColumnID columnID) {
		Data.TruckAddon row = getRow(rowIndex);
		if (row==null) return null;
		
		switch (columnID) {
		case ID                  : return row.id;
		case DLC                 : return row.dlcName;
		case Name                : return SnowRunner.solveStringID( SnowRunner.selectNonNull( row.name_StringID       , row.cargoName_StringID        ), language, null);
		case Description         : return SnowRunner.solveStringID( SnowRunner.selectNonNull( row.description_StringID, row.cargoDescription_StringID ), language, null);
		case InstallSocket       : return row.installSocket;
		case CargoSlots          : return row.cargoSlots;
		case RepairsCapacity     : return row.repairsCapacity;
		case WheelRepairsCapacity: return row.wheelRepairsCapacity;
		case FuelCapacity        : return row.fuelCapacity;
		case EnablesAWD          : return row.enablesAllWheelDrive;
		case EnablesDiffLock     : return row.enablesDiffLock;
		case Category            : return row.category;
		case CargoLength         : return row.cargoLength;
		case CargoType           : return row.cargoType;
		case IsCargo             : return row.isCargo;
		case Price               : return row.price;
		case UnlockByExploration : return row.unlockByExploration;
		case UnlockByRank        : return row.unlockByRank;
		case ExcludedCargoTypes  : return toString(row.excludedCargoTypes);
		case RequiredAddons      : return toString(row.requiredAddons);
		}
		return null;
	}

	enum ColumnID implements Tables.SimplifiedColumnIDInterface {
		ID                  ("ID"                   ,  String.class, 230),
		DLC                 ("DLC"                  ,  String.class,  80),
		Category            ("Category"             ,  String.class, 150),
		Name                ("Name"                 ,  String.class, 200), 
		InstallSocket       ("Install Socket"       ,  String.class, 130),
		CargoSlots          ("Cargo Slots"          , Integer.class,  70),
		RepairsCapacity     ("Repairs"              , Integer.class,  50),
		WheelRepairsCapacity("Wheel Repairs"        , Integer.class,  85),
		FuelCapacity        ("Fuel"                 , Integer.class,  50),
		EnablesAWD          ("Enables AWD"          , Boolean.class,  80), 
		EnablesDiffLock     ("Enables DiffLock"     , Boolean.class,  90), 
		Price               ("Price"                , Integer.class,  50), 
		UnlockByExploration ("Unlock By Exploration", Boolean.class, 120), 
		UnlockByRank        ("Unlock By Rank"       , Integer.class, 100), 
		Description         ("Description"          ,  String.class, 200), 
		ExcludedCargoTypes  ("Excluded Cargo Types" ,  String.class, 150),
		RequiredAddons      ("Required Addons"      ,  String.class, 200),
		IsCargo             ("Is Cargo"             , Boolean.class,  80),
		CargoLength         ("Cargo Length"         , Integer.class,  80),
		CargoType           ("Cargo Type"           ,  String.class, 170),
		;
	
		private final SimplifiedColumnConfig config;
		ColumnID(String name, Class<?> columnClass, int prefWidth) {
			config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
	}
}