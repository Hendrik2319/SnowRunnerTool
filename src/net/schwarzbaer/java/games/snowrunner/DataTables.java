package net.schwarzbaer.java.games.snowrunner;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.LabelRendererComponent;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.snowrunner.Data.AddonCategories;
import net.schwarzbaer.java.games.snowrunner.Data.Engine;
import net.schwarzbaer.java.games.snowrunner.Data.Gearbox;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Suspension;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.Data.SetInstance;
import net.schwarzbaer.java.games.snowrunner.Data.Winch;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;

class DataTables {
	
	static class VerySimpleTableModel<RowType> extends Tables.SimplifiedTableModel<VerySimpleTableModel.ColumnID> implements LanguageListener, TableCellRenderer, SwingConstants {
		
		protected final Controllers controllers;
		protected final Vector<RowType> rows;
		protected Language language;
		private final LabelRendererComponent rendererComp;
		private Comparator<RowType> initialRowOrder;
	
		VerySimpleTableModel(Controllers controllers, ColumnID[] columns) {
			super(columns);
			this.controllers = controllers;
			this.language = null;
			this.initialRowOrder = null;
			rows = new Vector<>();
			this.controllers.languageListeners.add(this);
			rendererComp = new Tables.LabelRendererComponent();
		}
		
		void connectToGlobalData(Function<Data,Collection<RowType>> getData) {
			if (getData!=null)
				controllers.dataReceivers.add(data -> setData(data==null ? null : getData.apply(data)));
		}
	
		void setInitialRowOrder(Comparator<RowType> initialRowOrder) {
			this.initialRowOrder = initialRowOrder;
		}
		
		protected void extraUpdate() {}
		
		@Override public void setLanguage(Language language) {
			this.language = language;
			extraUpdate();
			fireTableUpdate();
		}
	
		void setData(Collection<RowType> rows) {
			this.rows.clear();
			if (rows!=null) {
				this.rows.addAll(rows);
				if (initialRowOrder != null)
					this.rows.sort(initialRowOrder);
			}
			extraUpdate();
			fireTableUpdate();
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			String valueStr = value==null ? null : value.toString();
			
			int columnM = table.convertColumnIndexToModel(columnV);
			ColumnID columnID = getColumnID(columnM);
			
			if (columnID!=null) {
				
				if (columnID.format!=null && value!=null)
					valueStr = String.format(Locale.ENGLISH, columnID.format, value);
				
				if (columnID.horizontalAlignment!=null)
					rendererComp.setHorizontalAlignment(columnID.horizontalAlignment);
				
				else if (Number.class.isAssignableFrom(columnID.config.columnClass))
					rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
				
				else
					rendererComp.setHorizontalAlignment(SwingConstants.LEFT);
			}
			
			rendererComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus);
			return rendererComp;
		}
		
		@Override public void setTable(JTable table) {
			super.setTable(table);
			
			TableColumnModel columnModel = table.getColumnModel();
			if (columnModel!=null) {
				for (int i=0; i<columns.length; i++) {
					ColumnID columnID = columns[i];
					if (columnID.horizontalAlignment==null && columnID.format==null)
						continue;
					
					int colV = table.convertColumnIndexToView(i);
					TableColumn column = columnModel.getColumn(colV);
					if (column!=null)
						column.setCellRenderer(this);
					
				}
			}
		}
		
		@Override public int getRowCount() {
			return rows.size();
		}
	
		RowType getRow(int rowIndex) {
			if (rowIndex<0 || rowIndex>=rows.size()) return null;
			return rows.get(rowIndex);
		}
	
		@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			RowType row = getRow(rowIndex);
			if (row==null) return null;
			
			if (columnID.getValue!=null) {
				if (columnID.useValueAsStringID) {
					String stringID = (String) columnID.getValue.apply(row);
					return SnowRunner.solveStringID(stringID, language);
				}
				return columnID.getValue.apply(row);
			}
			if (columnID.getValueL!=null) {
				return columnID.getValueL.getValue(row,language);
			}
			
			return null;
			
		}
		
		static class ColumnID implements Tables.SimplifiedColumnIDInterface {
			
			interface LanguageBasedStringBuilder {
				String getValue(Object value, Language language);
			}
			
			private final SimplifiedColumnConfig config;
			private final Function<Object, ?> getValue;
			private final LanguageBasedStringBuilder getValueL;
			private final boolean useValueAsStringID;
			private final Integer horizontalAlignment;
			private final String format;
			
			ColumnID(String name, Class<String> columnClass, int prefWidth, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
				this(name, columnClass, prefWidth, horizontalAlignment, format, false, null, getValue);
			}
			<ColumnType> ColumnID(String name, Class<ColumnType> columnClass, int prefWidth, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue) {
				this(name, columnClass, prefWidth, horizontalAlignment, format, useValueAsStringID, getValue, null);
			}
			<ColumnType> ColumnID(String name, Class<ColumnType> columnClass, int prefWidth, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue, LanguageBasedStringBuilder getValueL) {
				this.horizontalAlignment = horizontalAlignment;
				this.format = format;
				this.useValueAsStringID = useValueAsStringID;
				this.getValue = getValue;
				this.getValueL = getValueL;
				this.config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
				if (useValueAsStringID && columnClass!=String.class)
					throw new IllegalStateException();
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}
	}

	interface RowTextTableModel {
		String getTextForRow(int rowIndex);
		void setTextAreaUpdateMethod(Runnable textAreaUpdateMethod);
	}

	static abstract class ExtendedVerySimpleTableModel<RowType> extends VerySimpleTableModel<RowType> implements RowTextTableModel {
		
		private Runnable textAreaUpdateMethod;
	
		ExtendedVerySimpleTableModel(Controllers controllers, ColumnID[] columns) {
			super(controllers, columns);
			textAreaUpdateMethod = null;
		}
	
		@Override protected void extraUpdate() {
			if (textAreaUpdateMethod!=null)
				textAreaUpdateMethod.run();
		}
	
		@Override public void setTextAreaUpdateMethod(Runnable textAreaUpdateMethod) {
			this.textAreaUpdateMethod = textAreaUpdateMethod;
		}
		
		@Override public String getTextForRow(int rowIndex) {
			RowType row = getRow(rowIndex);
			if (row==null) return "";
			return getTextForRow(row);
		}
	
		protected abstract String getTextForRow(RowType row);
	}

	static class SetInstancesTableModel<RowType extends SetInstance> extends ExtendedVerySimpleTableModel<RowType> {
	
		SetInstancesTableModel(Controllers controllers, ColumnID[] columns) {
			super(controllers, columns);
			setInitialRowOrder(Comparator.<RowType,String>comparing(e->e.setID).thenComparing(e->e.id));
		}
	
		@Override
		protected String getTextForRow(RowType row) {
			StringBuilder sb = new StringBuilder();
			boolean isFirst = true;
			
			String description = SnowRunner.solveStringID(row.description_StringID, language);
			if (description!=null && !"EMPTY_LINE".equals(description)) {
				sb.append(String.format("Description: <%s>%n%s", row.description_StringID, description));
				isFirst = false;
			}
			
			if (!row.usableBy.isEmpty()) {
				if (!isFirst) sb.append("\r\n\r\n");
				isFirst = false;
				sb.append("Usable By:\r\n");
				sb.append(SnowRunner.toString(row.usableBy,language));
			}
			
			return sb.toString();
		}
	}

	static class AddonCategoriesTableModel extends VerySimpleTableModel<AddonCategories.Category> {

		AddonCategoriesTableModel(Controllers controllers) {
			super(controllers, new ColumnID[] {
					new ColumnID("ID"                          ,  String.class, 100, null, null, false, row->((AddonCategories.Category)row).name),
					new ColumnID("Label"                       ,  String.class, 200, null, null,  true, row->((AddonCategories.Category)row).label_StringID),
					new ColumnID("Icon"                        ,  String.class, 130, null, null, false, row->((AddonCategories.Category)row).icon),
					new ColumnID("Requires One Addon Installed", Boolean.class, 160, null, null, false, row->((AddonCategories.Category)row).requiresOneAddonInstalled),
			});
			connectToGlobalData(data->data.addonCategories.categories.values());
			setInitialRowOrder(Comparator.<AddonCategories.Category,String>comparing(cat->cat.name));
		}
		
	}

	static class SuspensionsTableModel extends SetInstancesTableModel<Suspension> {

		SuspensionsTableModel(Controllers controllers, boolean connectToGlobalData) {
			super(controllers, new ColumnID[] {
					new ColumnID("Set ID"               ,  String.class, 130,   null,      null, false, row->((Suspension)row).setID),
					new ColumnID("Item ID"              ,  String.class, 220,   null,      null, false, row->((Suspension)row).id),
					new ColumnID("Type"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).type_StringID),
					new ColumnID("Name"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).name_StringID),
					new ColumnID("Description"          ,  String.class, 150,   null,      null,  true, row->((Suspension)row).description_StringID),
					new ColumnID("Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Suspension)row).price),
					new ColumnID("Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Suspension)row).unlockByExploration),
					new ColumnID("Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Suspension)row).unlockByRank),
					new ColumnID("Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Suspension)row).damageCapacity),
					new ColumnID("Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.toString(((Suspension)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.suspensions.values());
		}
	}

	static class WinchesTableModel extends SetInstancesTableModel<Winch> {

		WinchesTableModel(Controllers controllers, boolean connectToGlobalData) {
			super(controllers, new ColumnID[] {
					new ColumnID("Set ID"                  ,  String.class, 140,   null,      null, false, row->((Winch)row).setID),
					new ColumnID("Item ID"                 ,  String.class, 160,   null,      null, false, row->((Winch)row).id),
					new ColumnID("Name"                    ,  String.class, 150,   null,      null,  true, row->((Winch)row).name_StringID),
					new ColumnID("Description"             ,  String.class, 150,   null,      null,  true, row->((Winch)row).description_StringID),
					new ColumnID("Price"                   , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Winch)row).price),
					new ColumnID("Unlock By Exploration"   , Boolean.class, 120,   null,      null, false, row->((Winch)row).unlockByExploration),
					new ColumnID("Unlock By Rank"          , Integer.class,  85, CENTER, "Rank %d", false, row->((Winch)row).unlockByRank),
					new ColumnID("Requires Engine Ignition", Boolean.class, 130,   null,      null, false, row->((Winch)row).isEngineIgnitionRequired),
					new ColumnID("Length"                  , Integer.class,  50, CENTER,      null, false, row->((Winch)row).length),
					new ColumnID("Strength Multi"          ,   Float.class,  80, CENTER,   "%1.2f", false, row->((Winch)row).strengthMult),
					new ColumnID("Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.toString(((Winch)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.winches.values());
		}
	}

	static class GearboxesTableModel extends SetInstancesTableModel<Gearbox> {

		GearboxesTableModel(Controllers controllers, boolean connectToGlobalData) {
			super(controllers, new ColumnID[] {
					new ColumnID("Set ID"               ,  String.class, 180,   null,      null, false, row->((Gearbox)row).setID),
					new ColumnID("Item ID"              ,  String.class, 140,   null,      null, false, row->((Gearbox)row).id),
					new ColumnID("Name"                 ,  String.class, 110,   null,      null,  true, row->((Gearbox)row).name_StringID),
					new ColumnID("Description"          ,  String.class, 150,   null,      null,  true, row->((Gearbox)row).description_StringID),
					new ColumnID("Price"                , Integer.class,  60,   null,   "%d Cr", false, row->((Gearbox)row).price),
					new ColumnID("Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Gearbox)row).unlockByExploration),
					new ColumnID("Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Gearbox)row).unlockByRank),
					new ColumnID("(H)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsHighGear),
					new ColumnID("(L)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerGear),
					new ColumnID("(L+)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerPlusGear),
					new ColumnID("(L-)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerMinusGear),
					new ColumnID("is Manual Low Gear"   , Boolean.class, 110,   null,      null, false, row->((Gearbox)row).isManualLowGear),
					new ColumnID("Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Gearbox)row).damageCapacity),
					new ColumnID("AWD Consumption Mod." ,   Float.class, 130,   null,   "%1.2f", false, row->((Gearbox)row).awdConsumptionModifier),
					new ColumnID("Fuel Consumption"     ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).fuelConsumption),
					new ColumnID("Idle Fuel Modifier"   ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).idleFuelModifier),
					new ColumnID("Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.toString(((Gearbox)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.gearboxes.values());
		}
	}

	static class EnginesTableModel extends SetInstancesTableModel<Engine> {
	
		EnginesTableModel(Controllers controllers, boolean connectToGlobalData) {
			super(controllers, new ColumnID[] {
					new ColumnID("Set ID"               ,  String.class, 160,   null,      null, false, row->((Engine)row).setID),
					new ColumnID("Item ID"              ,  String.class, 190,   null,      null, false, row->((Engine)row).id),
					new ColumnID("Name"                 ,  String.class, 130,   null,      null,  true, row->((Engine)row).name_StringID),
					new ColumnID("Description"          ,  String.class, 150,   null,      null,  true, row->((Engine)row).description_StringID),
					new ColumnID("Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Engine)row).price),
					new ColumnID("Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Engine)row).unlockByExploration),
					new ColumnID("Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Engine)row).unlockByRank),
					new ColumnID("Torque"               , Integer.class,  70,   null,      null, false, row->((Engine)row).torque),
					new ColumnID("Fuel Consumption"     ,   Float.class, 100,  RIGHT,   "%1.2f", false, row->((Engine)row).fuelConsumption),
					new ColumnID("Damage Capacity"      , Integer.class, 100,  RIGHT,    "%d R", false, row->((Engine)row).damageCapacity),
					new ColumnID("Brakes Delay"         ,   Float.class,  70,  RIGHT,   "%1.2f", false, row->((Engine)row).brakesDelay),
					new ColumnID("Responsiveness"       ,   Float.class,  90,  RIGHT,   "%1.4f", false, row->((Engine)row).engineResponsiveness),
					new ColumnID("Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.toString(((Engine)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.engines.values());
		}
	}

	static class TrailersTableModel extends ExtendedVerySimpleTableModel<Trailer> {
		
		TrailersTableModel(Controllers controllers, boolean connectToGlobalData) {
			super(controllers, new ColumnID[] {
					new ColumnID("ID"                   ,  String.class, 230,   null,      null, false, row->((Trailer)row).id),
					new ColumnID("Name"                 ,  String.class, 200,   null,      null,  true, row->((Trailer)row).name_StringID), 
					new ColumnID("DLC"                  ,  String.class,  80,   null,      null, false, row->((Trailer)row).dlcName),
					new ColumnID("Install Socket"       ,  String.class, 130,   null,      null, false, row->((Trailer)row).installSocket),
					new ColumnID("Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((Trailer)row).cargoSlots),
					new ColumnID("Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((Trailer)row).repairsCapacity),
					new ColumnID("Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((Trailer)row).wheelRepairsCapacity),
					new ColumnID("Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((Trailer)row).fuelCapacity),
					new ColumnID("Is Quest Item"        , Boolean.class,  80,   null,      null, false, row->((Trailer)row).isQuestItem),
					new ColumnID("Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((Trailer)row).price), 
					new ColumnID("Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Trailer)row).unlockByExploration), 
					new ColumnID("Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((Trailer)row).unlockByRank), 
					new ColumnID("Attach Type"          ,  String.class,  70,   null,      null, false, row->((Trailer)row).attachType),
					new ColumnID("Description"          ,  String.class, 200,   null,      null,  true, row->((Trailer)row).description_StringID), 
					new ColumnID("Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->toString(((Trailer)row).excludedCargoTypes)),
					new ColumnID("Required Addons"      ,  String.class, 150,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((Trailer)row).requiredAddons)),
					new ColumnID("Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.toString(((Trailer)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.trailers.values());
			setInitialRowOrder(Comparator.<Trailer,String>comparing(row->row.id));
		}
	
		private static String toString(String[] strs) {
			if (strs==null) return "<null>";
			if (strs.length==0) return "[]";
			if (strs.length==1) return strs[0];
			return Arrays.toString(strs);
		}
	
		@Override public String getTextForRow(Trailer row) {
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
	}

	static class TruckAddonsTableModel extends ExtendedVerySimpleTableModel<TruckAddon> {
		
		TruckAddonsTableModel(Controllers controllers, boolean connectToGlobalData) {
			super(controllers, new ColumnID[] {
					new ColumnID("ID"                   ,  String.class, 230,   null,      null, false, row->((TruckAddon)row).id),
					new ColumnID("DLC"                  ,  String.class,  80,   null,      null, false, row->((TruckAddon)row).dlcName),
					new ColumnID("Category"             ,  String.class, 150,   null,      null, false, row->((TruckAddon)row).category),
					new ColumnID("Name"                 ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.name_StringID!=null ? row.name_StringID : row.cargoName_StringID; }), 
					new ColumnID("Install Socket"       ,  String.class, 130,   null,      null, false, row->((TruckAddon)row).installSocket),
					new ColumnID("Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((TruckAddon)row).cargoSlots),
					new ColumnID("Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((TruckAddon)row).repairsCapacity),
					new ColumnID("Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((TruckAddon)row).wheelRepairsCapacity),
					new ColumnID("Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((TruckAddon)row).fuelCapacity),
					new ColumnID("Enables AWD"          , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).enablesAllWheelDrive), 
					new ColumnID("Enables DiffLock"     , Boolean.class,  90,   null,      null, false, row->((TruckAddon)row).enablesDiffLock), 
					new ColumnID("Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((TruckAddon)row).price), 
					new ColumnID("Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((TruckAddon)row).unlockByExploration), 
					new ColumnID("Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((TruckAddon)row).unlockByRank), 
					new ColumnID("Description"          ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.description_StringID!=null ? row.description_StringID : row.cargoDescription_StringID; }), 
					new ColumnID("Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->toString(((TruckAddon)row).excludedCargoTypes)),
					new ColumnID("Required Addons"      ,  String.class, 200,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((TruckAddon)row).requiredAddons)),
					new ColumnID("Is Cargo"             , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).isCargo),
					new ColumnID("Cargo Length"         , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).cargoLength),
					new ColumnID("Cargo Type"           ,  String.class, 170,   null,      null, false, row->((TruckAddon)row).cargoType),
					new ColumnID("Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.toString(((TruckAddon)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.truckAddons.values());
			Comparator<String> string_nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
			setInitialRowOrder(Comparator.<TruckAddon,String>comparing(row->row.category,string_nullsLast).thenComparing(row->row.id));
		}
	
		private static String toString(String[] strs) {
			if (strs==null) return "<null>";
			if (strs.length==0) return "[]";
			if (strs.length==1) return strs[0];
			return Arrays.toString(strs);
		}
	
		@Override public String getTextForRow(TruckAddon row) {
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
	}

	static class WheelsTableModel extends VerySimpleTableModel<WheelsTableModel.RowItem> {
	
		WheelsTableModel(Controllers controllers) {
			super(controllers, new ColumnID[] {
					new ColumnID("ID"     , String .class, 300,   null,    null, false, row -> ((RowItem)row).label),
					new ColumnID("Names"  , String .class, 130,   null,    null, (row,lang) -> getNameList ( ((RowItem)row).names_StringID, lang)),
					new ColumnID("Sizes"  , String .class, 300,   null,    null, false, row -> getSizeList ( ((RowItem)row).sizes  )),
					new ColumnID("Highway", Float  .class,  55,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionHighway), 
					new ColumnID("Offroad", Float  .class,  50,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionOffroad), 
					new ColumnID("Mud"    , Float  .class,  50,  RIGHT, "%1.2f", false, row -> ((RowItem)row).tireValues.frictionMud), 
					new ColumnID("On Ice" , Boolean.class,  50,   null,    null, false, row -> ((RowItem)row).tireValues.onIce), 
					new ColumnID("Trucks" , String .class, 800,   null,    null, (row,lang) -> getTruckList( ((RowItem)row).trucks, lang)),
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
		
		private static class RowItem {
	
			final String key;
			final String label;
			final HashSet<Truck> trucks;
			final HashSet<Integer> sizes;
			final HashSet<String> names_StringID;
			final TireValues tireValues;
	
			public RowItem(String key, String label, TireValues tireValues) {
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
				TireValues newTireValues = new TireValues(tire);
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
					if (!(obj instanceof TireValues))
						return false;
					
					TireValues other = (TireValues) obj;
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

	static class DLCTableModel extends Tables.SimplifiedTableModel<DLCTableModel.ColumnID> implements LanguageListener, TruckToDLCAssignmentListener, DataReceiver {
	
		private Language language;
		private final Vector<RowItem> rows;
		private HashMap<String, String> truckToDLCAssignments;
		private Data data;
	
		DLCTableModel(Controllers controllers) {
			super(ColumnID.values());
			language = null;
			truckToDLCAssignments = null;
			data = null;
			rows = new Vector<>();
			controllers.languageListeners.add(this);
			controllers.truckToDLCAssignmentListeners.add(this);
			controllers.dataReceivers.add(this);
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
}
