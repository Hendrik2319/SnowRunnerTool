package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TableContextMenuModifier;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TextAreaOutputSource;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TextPaneOutputSource;

public class VerySimpleTableModel<RowType> extends Tables.SimplifiedTableModel<VerySimpleTableModel.ColumnID> implements LanguageListener, SwingConstants, TableContextMenuModifier, Finalizable {
	
	static final Color COLOR_BG_FALSE = new Color(0xFF6600);
	static final Color COLOR_BG_TRUE = new Color(0x99FF33);
	static final Color COLOR_BG_EDITABLECELL = new Color(0xFFFAE7);
	
	@SuppressWarnings("unused")
	private   final Float columns; // to hide super.columns
	private   final ColumnID[] originalColumns;
	protected final Vector<RowType> rows;
	private   final Vector<RowType> originalRows;
	protected final Window mainWindow;
	protected final Finalizer finalizer;
	protected final Coloring<RowType> coloring;
	protected Language language;
	private   Comparator<RowType> initialRowOrder;
	private   ColumnID clickedColumn;
	private   int clickedColumnIndex;
	private   RowType clickedRow;

	protected VerySimpleTableModel(Window mainWindow, Controllers controllers, ColumnID[] columns) {
		super(columns);
		this.mainWindow = mainWindow;
		this.finalizer = controllers.createNewFinalizer();
		this.originalColumns = columns;
		this.columns = null; // to hide super.columns
		language = null;
		initialRowOrder = null;
		rows = new Vector<>();
		originalRows = new Vector<>();
		clickedColumn = null;
		clickedColumnIndex = -1;
		clickedRow = null;
		coloring = new Coloring<>(this);
		
		finalizer.addLanguageListener(this);
		
		coloring.setBackgroundColumnColoring(true, Boolean.class, b -> b ? COLOR_BG_TRUE : COLOR_BG_FALSE);
		
		HashSet<String> columnIDIDs = new HashSet<>();
		for (int i=0; i<originalColumns.length; i++) {
			ColumnID columnID = originalColumns[i];
			if (columnIDIDs.contains(columnID.id))
				throw new IllegalStateException(String.format("Found a non unique column ID \"%s\" in column %d in TableModel \"%s\"", columnID.id, i, this.getClass()));
			columnIDIDs.add(columnID.id);
		}
	}

	@Override public void prepareRemovingFromGUI() {
		finalizer.removeSubCompsAndListenersFromGUI();
	}

	static class Coloring<RowType> {

		private final Vector<Function<RowType,Color>> rowColorizersFG = new Vector<>();
		private final Vector<Function<RowType,Color>> rowColorizersBG = new Vector<>();
		
		private final HashMap<Class<?>,Function<Object,Color>> classColorizersFG = new HashMap<>();
		private final HashMap<Class<?>,Function<Object,Color>> classColorizersBG = new HashMap<>();
		
		private final HashMap<Class<?>,Function<Object,Color>> classColorizersFGspecial = new HashMap<>();
		private final HashMap<Class<?>,Function<Object,Color>> classColorizersBGspecial = new HashMap<>();
		
		private final HashSet<Integer> columnsWithActiveSpecialColoring = new HashSet<>();
		
		private final VerySimpleTableModel<RowType> tablemodel;
		
		Coloring(VerySimpleTableModel<RowType> tablemodel) {
			this.tablemodel = tablemodel;
		}
		
		
		boolean containsSpecialColorizer(int clickedColumnIndex, Class<?> columnClass) {
			return 
					classColorizersFGspecial.containsKey(columnClass) ||
					classColorizersBGspecial.containsKey(columnClass) /*||
					specialForegroundColumnColorizers.containsKey(clickedColumnIndex) ||
					specialBackgroundColumnColorizers.containsKey(clickedColumnIndex)*/;
		}

		void addForegroundRowColorizer(Function<RowType,Color> colorizer) {
			if (colorizer==null) throw new IllegalArgumentException();
			rowColorizersFG.add(colorizer);
		}
		
		void addBackgroundRowColorizer(Function<RowType,Color> colorizer) {
			if (colorizer==null) throw new IllegalArgumentException();
			rowColorizersBG.add(colorizer);
		}
		
		private static <Type> Function<Object, Color> convertClassColorizer(Class<Type> class_, Function<Type, Color> getcolor) {
			return obj->!class_.isInstance(obj) ? null : getcolor.apply(class_.cast(obj));
		}

		<Type> void setForegroundColumnColoring(boolean isSpecial, Class<Type> class_, Function<Type,Color> getcolor) {
			if (isSpecial) classColorizersFGspecial.put(class_, convertClassColorizer(class_, getcolor));
			else           classColorizersFG       .put(class_, convertClassColorizer(class_, getcolor));
		}
		<Type> void setBackgroundColumnColoring(boolean isSpecial, Class<Type> class_, Function<Type,Color> getcolor) {
			if (isSpecial) classColorizersBGspecial.put(class_, convertClassColorizer(class_, getcolor));
			else           classColorizersBG       .put(class_, convertClassColorizer(class_, getcolor));
		}

		Color getRowColor(Vector<Function<RowType,Color>> colorizers, RowType row) {
			for (int i=colorizers.size()-1; i>=0; i--) {
				Color color = colorizers.get(i).apply(row);
				if (color!=null)
					return color;
			}
			return null;
		}
		
		Color getForeground(int columnM, ColumnID columnID, int rowM, RowType row, Object value) {
			return getColor(true, columnM, columnID, rowM, row, value);
		}

		Color getBackground(int columnM, ColumnID columnID, int rowM, RowType row, Object value) {
			return getColor(false, columnM, columnID, rowM, row, value);
		}

		private Color getColor(boolean isForeground, int columnM, ColumnID columnID, int rowM, RowType row, Object value) {
			
			HashMap<Class<?>, Function<Object, Color>> classColorizersSpecial;
			Vector<Function<RowType,Color>> rowColorizers;
			HashMap<Class<?>, Function<Object, Color>> classColorizers;
			Color defaultColor;
			
			if (isForeground) {
				classColorizersSpecial = classColorizersFGspecial;
				rowColorizers          = rowColorizersFG;
				classColorizers        = classColorizersFG;
				defaultColor = columnID.foreground;
			} else {
				classColorizersSpecial = classColorizersBGspecial;
				rowColorizers          = rowColorizersBG;
				classColorizers        = classColorizersBG;
				defaultColor = columnID.background;
			}
			Color color = null;
			
			if (columnsWithActiveSpecialColoring.contains(columnM)) {
				Function<Object, Color> colorizerFG = classColorizersSpecial.get(columnID.config.columnClass);
				if (color==null && colorizerFG!=null)
					color = colorizerFG.apply(value);
			}
			
			if (color==null) color = defaultColor;
			
			if (color==null && !rowColorizers.isEmpty())
				color = getRowColor(rowColorizers,row);
			
			Function<Object, Color> colorizerFG = classColorizers.get(columnID.config.columnClass);
			if (color==null && colorizerFG!=null)
				color = colorizerFG.apply(value);
			
			if (!isForeground && color==null && tablemodel.isCellEditable(rowM, columnM, columnID)) {
				color = COLOR_BG_EDITABLECELL;
			}
			
			return color;
		}
	}
	
	protected void connectToGlobalData(Function<Data,Collection<RowType>> getData) {
		connectToGlobalData(false, getData);
	}
	protected void connectToGlobalData(boolean forwardNulls, Function<Data,Collection<RowType>> getData) {
		if (getData!=null)
			finalizer.addDataReceiver(data -> {
				if (!forwardNulls)
					setRowData(data==null ? null : getData.apply(data));
				else
					setRowData(getData.apply(data));
			});
	}

	void setInitialRowOrder(Comparator<RowType> initialRowOrder) {
		this.initialRowOrder = initialRowOrder;
	}
	
	protected void extraUpdate() {}
	
	@Override public void setLanguage(Language language) {
		int before = table==null ? -1 : table.getSelectedRow();
		this.language = language;
		extraUpdate();
		fireTableUpdate();
		if (before>=0 && table!=null) {
			table.setRowSelectionInterval(before, before);
		}
	}

	public final void setRowData(Collection<RowType> rows) {
		this.rows.clear();
		originalRows.clear();
		if (rows!=null) {
			originalRows.addAll(rows);
			if (initialRowOrder != null)
				originalRows.sort(initialRowOrder);
			this.rows.addAll(FilterRowsDialog.filterRows(originalRows, originalColumns, this::getValue));
		}
		extraUpdate();
		fireTableUpdate();
	}

	private class GeneralPurposeTCR implements TableCellRenderer {
		private final Tables.LabelRendererComponent    label;
		private final Tables.CheckBoxRendererComponent checkBox;
		
		GeneralPurposeTCR() {
			label = new Tables.LabelRendererComponent();
			checkBox = new Tables.CheckBoxRendererComponent();
			checkBox.setHorizontalAlignment(SwingConstants.CENTER);
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			String valueStr = value==null ? null : value.toString();
			
			int columnM = table.convertColumnIndexToModel(columnV);
			ColumnID columnID = getColumnID(columnM);
			int rowM = table.convertRowIndexToModel(rowV);
			RowType row = getRow(rowM);
				
			Supplier<Color> getCustomForeground = ()->coloring.getForeground(columnM, columnID, rowM, row, value);
			Supplier<Color> getCustomBackground = ()->coloring.getBackground(columnM, columnID, rowM, row, value);
			
			boolean isChecked = false;
			boolean useCheckBox = false;
				
			if (columnID!=null) {
				
				if (columnID.format!=null && value!=null)
					valueStr = String.format(Locale.ENGLISH, columnID.format, value);
				
				if (columnID.horizontalAlignment!=null)
					label.setHorizontalAlignment(columnID.horizontalAlignment);
				
				else if (Number.class.isAssignableFrom(columnID.config.columnClass))
					label.setHorizontalAlignment(SwingConstants.RIGHT);
				
				else
					label.setHorizontalAlignment(SwingConstants.LEFT);
				
				if (columnID.config.columnClass == Boolean.class) {
					valueStr = null;
					if (value instanceof Boolean) {
						Boolean b = (Boolean) value;
						isChecked = b.booleanValue();
						useCheckBox = true;
						
					} else {
						useCheckBox = false;
						// -> empty JLabel -> No CheckBox
					}
				}
				
				/*if (columnID.config.columnClass == TruckAddon.InstState.class) {
					valueStr = null;
					if (value instanceof TruckAddon.InstState) {
						TruckAddon.InstState state = (TruckAddon.InstState) value;
						isChecked = state!=TruckAddon.InstState.NotInstallable;
						if (state==TruckAddon.InstState.Installed)
							valueStr = " (installed)";
						useCheckBox = true;
						
					} else {
						useCheckBox = false;
						// -> empty JLabel -> No CheckBox
					}
				}*/
			}
			
			if (useCheckBox) {
				checkBox.configureAsTableCellRendererComponent(table, isChecked, valueStr, isSelected, hasFocus, getCustomForeground, getCustomBackground);
				return checkBox;
			}
			label.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, getCustomForeground);
			return label;
		}
	}

	@Override final public void setTable(JTable table) {
		super.setTable(table);
	}
	
	public void reconfigureAfterTableStructureUpdate() {
		setColumnWidths(table);
		setCellRenderers();
	}

	private void setCellRenderers() {
		GeneralPurposeTCR tcr = new GeneralPurposeTCR();
		TableColumnModel columnModel = this.table.getColumnModel();
		if (columnModel!=null) {
			for (int i=0; i<super.columns.length; i++) {
				//ColumnID columnID = columns[i];
				//if (foregroundColorizers.isEmpty() && backgroundColorizers.isEmpty() && columnID.horizontalAlignment==null && columnID.format==null)
				//	continue;
				
				int colV = this.table.convertColumnIndexToView(i);
				TableColumn column = columnModel.getColumn(colV);
				if (column!=null)
					column.setCellRenderer(tcr);
			}
		}
	}
	
	@Override public void modifyTableContextMenu(JTable table, TableSimplifier.ContextMenu contextMenu) {
		
		contextMenu.addSeparator();
		
		JMenuItem miActivateSpecialColoring = contextMenu.add(SnowRunner.createMenuItem("Activate special coloring for this column", true, e->{
			if (coloring.columnsWithActiveSpecialColoring.contains(clickedColumnIndex))
				coloring.columnsWithActiveSpecialColoring.remove(clickedColumnIndex);
			else
				coloring.columnsWithActiveSpecialColoring.add(clickedColumnIndex);
			table.repaint();
		}));
		
		JMenuItem miDeactivateAllSpecialColorings = contextMenu.add(SnowRunner.createMenuItem("Deactivate all special column colorings", true, e->{
			coloring.columnsWithActiveSpecialColoring.clear();
			table.repaint();
		}));
		
		contextMenu.addSeparator();
		
		contextMenu.add(SnowRunner.createMenuItem("Hide/Unhide Columns ...", true, e->{
			ColumnHideDialog dlg = new ColumnHideDialog(mainWindow,originalColumns,getClass().getName());
			boolean changed = dlg.showDialog();
			if (changed) {
				super.columns = ColumnHideDialog.removeHiddenColumns(originalColumns);
				fireTableStructureUpdate();
				reconfigureAfterTableStructureUpdate();
			}
		}));
		
		contextMenu.add(SnowRunner.createMenuItem("Filter Rows ...", true, e->{
			FilterRowsDialog dlg = new FilterRowsDialog(mainWindow,originalColumns,getClass().getName());
			boolean changed = dlg.showDialog();
			if (changed) {
				rows.clear();
				this.rows.addAll(FilterRowsDialog.filterRows(originalRows, originalColumns, this::getValue));
				fireTableStructureUpdate();
				reconfigureAfterTableStructureUpdate();
			}
		}));
		
		contextMenu.addSeparator();
		
		JMenuItem miShowCalculationPath = contextMenu.add(SnowRunner.createMenuItem("Show calculation path", true, e->{
			if (clickedColumn==null) return;
			if (clickedRow   ==null) return;
			clickedColumn.getValue_verbose(clickedRow, language, this);
		}));
		
		contextMenu.addContextMenuInvokeListener((table_, x, y) -> {
			Point p = x==null || y==null ? null : new Point(x,y);
			int colV = p==null ? -1 : table_.columnAtPoint(p);
			clickedColumnIndex = colV<0 ? -1 : table_.convertColumnIndexToModel(colV);
			clickedColumn = clickedColumnIndex<0 ? null : super.columns[clickedColumnIndex];
			
			int rowV = p==null ? -1 : table_.rowAtPoint(p);
			int rowM = rowV<0 ? -1 : table_.convertRowIndexToModel(rowV);
			clickedRow = rowM<0 ? null : getRow(rowV);
			
			miDeactivateAllSpecialColorings.setEnabled(
					!coloring.columnsWithActiveSpecialColoring.isEmpty()
				);
			miActivateSpecialColoring.setEnabled(
					clickedColumn!=null &&
					clickedColumnIndex>=0 &&
					coloring.containsSpecialColorizer(clickedColumnIndex, clickedColumn.config.columnClass)
				);
			
			miActivateSpecialColoring.setText(
				clickedColumn==null
				? "Activate special coloring for this column"
				: coloring.columnsWithActiveSpecialColoring.contains(clickedColumnIndex)
				? String.format("Deactivate special coloring for column \"%s\"", clickedColumn.config.name)
				: String.format(  "Activate special coloring for column \"%s\"", clickedColumn.config.name)
			);
			
			miShowCalculationPath.setEnabled(clickedColumn!=null && clickedColumn.hasVerboseValueFcn());
		});
	}

	@Override public int getRowCount() {
		return rows.size();
	}

	public RowType getRow(int rowIndex) {
		if (rowIndex<0 || rowIndex>=rows.size()) return null;
		return rows.get(rowIndex);
	}

	@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
		RowType row = getRow(rowIndex);
		if (row==null) return null;
		return getValue(columnID, row);
	}
	
	Object getValue(ColumnID columnID, RowType row) {
		return columnID.getValue(row, language, this);
	}
	
	protected int findColumnByID(String id) {
		if (id==null) throw new IllegalArgumentException();
		for (int i=0; i<super.columns.length; i++)
			if (id.equals(((ColumnID)super.columns[i]).id))
				return i;
		return -1;
	}
	
	private static class FilterRowsDialog extends ModelConfigureDialog<HashMap<String,FilterRowsDialog.Filter>> {
		private static final long serialVersionUID = 6171301101675843952L;
		private static final FilterRowsDialog.Presets presets = new Presets();
		
		private final FilterRowsDialog.FilterGuiElement[] columnElements;
	
		FilterRowsDialog(Window owner, ColumnID[] originalColumns, String tableModelIDstr) {
			super(owner, "Filter Rows", "Define filters", originalColumns, presets, tableModelIDstr);
			
			JPanel columnPanel = FilterGuiElement.createEmptyColumnPanel();
			columnElements = new FilterRowsDialog.FilterGuiElement[originalColumns.length];
			for (int i=0; i<this.originalColumns.length; i++) {
				columnElements[i] = new FilterGuiElement(this.originalColumns[i]);
				columnElements[i].addToPanel(columnPanel);
			}
			
			GridBagConstraints c = new GridBagConstraints();
			c.weighty = 1;
			columnPanel.add(new JLabel(), c);
			
			columnScrollPane.setViewportView(columnPanel);
			columnScrollPane.setPreferredSize(new Dimension(600,700));
		}

		@Override protected boolean resetValuesFinal() {
			boolean hasChanged = false;
			for (ColumnID columnID : this.originalColumns) {
				if (columnID.filter!=null) {
					columnID.filter = null;
					hasChanged = true;
				}
			}
			return hasChanged;
		}

		@Override protected boolean setValuesFinal() {
			boolean hasChanged = false;
			for (int i=0; i<this.originalColumns.length; i++) {
				ColumnID columnID = this.originalColumns[i];
				FilterRowsDialog.Filter filter = columnElements[i].getFilter();
				
				if (filter==null && columnID.filter!=null) {
					columnID.filter.active = false;
					hasChanged = true;
					
				} else if (filter!=null && !filter.equals(columnID.filter)) {
					columnID.filter = filter;
					hasChanged = true;
				}
			}
			return hasChanged;
		}

		@Override protected void setPresetInGui(HashMap<String, FilterRowsDialog.Filter> preset) {
			for (FilterRowsDialog.FilterGuiElement elem : columnElements)
				elem.setValues(preset.get(elem.columnID.id));
		}

		@Override protected HashMap<String, FilterRowsDialog.Filter> getCopyOfCurrentPreset() {
			HashMap<String, FilterRowsDialog.Filter> preset = new HashMap<>();
			for (FilterRowsDialog.FilterGuiElement elem : columnElements) {
				FilterRowsDialog.Filter filter = elem.getFilter();
				if (filter!=null && filter.active)
					preset.put(elem.columnID.id, filter.clone());
			}
			return preset;
		}

		static <RowType> Vector<RowType> filterRows(Vector<RowType> originalRows, ColumnID[] originalColumns, BiFunction<ColumnID,RowType,Object> getValue) {
			Vector<RowType> filteredRows = new Vector<>();
			for (RowType row : originalRows) {
				boolean meetsFilter = true;
				for (ColumnID columnID : originalColumns) {
					if (columnID.filter==null || !columnID.filter.active)
						continue;
					
					Object value = getValue.apply(columnID, row);
					if (!columnID.filter.valueMeetsFilter(value)) {
						meetsFilter = false;
						break;
					}
				}
				if (meetsFilter)
					filteredRows.add(row);
			}
			
			return filteredRows;
		}

		static abstract class Filter {
			
			boolean active = false;
			boolean allowUnset = false;

			boolean valueMeetsFilter(Object value) {
				if (allowUnset && value==null)
					return true;
				return valueMeetsFilter_SubType(value);
			}

			public static FilterRowsDialog.Filter parse(String str) {
				int pos;
				String valueStr;
				
				pos = str.indexOf(':');
				if (pos<0) return null;
				valueStr = str.substring(0, pos);
				str = str.substring(pos+1);
				boolean active;
				switch (valueStr) {
				case "true" : active = true; break;
				case "false": active = false; break;
				default: return null;
				}
				
				pos = str.indexOf(':');
				if (pos<0) return null;
				valueStr = str.substring(0, pos);
				str = str.substring(pos+1);
				boolean allowUnset;
				switch (valueStr) {
				case "true" : allowUnset = true; break;
				case "false": allowUnset = false; break;
				default: return null;
				}
				
				pos = str.indexOf(':');
				if (pos<0) return null;
				String simpleClassName = str.substring(0, pos);
				str = str.substring(pos+1);
				
				FilterRowsDialog.Filter filter = null;
				switch (simpleClassName) {
				case "NumberFilter": filter = NumberFilter.parseNumberFilter(str); break;
				case "BoolFilter"  : filter = BoolFilter.parseBoolFilter(str); break;
				case "EnumFilter"  : filter = EnumFilter.parseEnumFilter(str); break;
				}
				
				if (filter != null) {
					filter.active = active;
					filter.allowUnset = allowUnset;
				}
				
				return filter;
			}

			final String toParsableString() {
				return String.format("%s:%s:%s:%s", active, allowUnset, getClass().getSimpleName(), toParsableString_SubType());
			}

			@Override final public boolean equals(Object obj) {
				if (!(obj instanceof FilterRowsDialog.Filter)) return false;
				FilterRowsDialog.Filter other = (FilterRowsDialog.Filter) obj;
				if (this.active    !=other.active    ) return false;
				if (this.allowUnset!=other.allowUnset) return false;
				return equals_SubType(other);
			}
			
			@Override final protected FilterRowsDialog.Filter clone() {
				FilterRowsDialog.Filter filter = clone_SubType();
				filter.active     = active    ;
				filter.allowUnset = allowUnset;
				return filter;
			}

			protected abstract boolean valueMeetsFilter_SubType(Object value);
			protected abstract String toParsableString_SubType();
			protected abstract boolean equals_SubType(FilterRowsDialog.Filter other);
			protected abstract FilterRowsDialog.Filter clone_SubType();
			
			
			static class EnumFilter<E extends Enum<E>> extends FilterRowsDialog.Filter {
				private final static HashMap<String,Function<String[],Filter.EnumFilter<?>>> KnownTypes = new HashMap<>();
				private final static HashMap<Class<?>,String> KnownFilterIDs = new HashMap<>();
				static {
					registerEnumFilter("DiffLockType", Truck.DiffLockType .class, Truck.DiffLockType ::valueOf);
					registerEnumFilter("Country"     , Truck.Country      .class, Truck.Country      ::valueOf);
					registerEnumFilter("TruckType"   , Truck.TruckType    .class, Truck.TruckType    ::valueOf);
					registerEnumFilter("ItemStat"    , Truck.UDV.ItemState.class, Truck.UDV.ItemState::valueOf);
				}

				private static <E extends Enum<E>> void registerEnumFilter(String filterID, Class<E> valueClass, Function<String, E> parseValue) {
					KnownFilterIDs.put(valueClass,filterID);
					KnownTypes.put(filterID, values -> parseEnumFilter(valueClass, filterID, values, parseValue));
				}

				static String getFilterID(Class<?> valueClass) {
					String filterID = KnownFilterIDs.get(valueClass);
					if (filterID==null) throw new IllegalStateException(String.format("EnumFilter: Can't find filter id for value class \"%s\"", valueClass.getCanonicalName()));
					return filterID;
				}

				private final String filterID;
				private final Class<E> valueClass;
				private final EnumSet<E> allowedValues;

				EnumFilter(String filterID, Class<E> valueClass) {
					this.filterID = filterID;
					if (valueClass==null) throw new IllegalArgumentException();
					this.valueClass = valueClass;
					this.allowedValues = EnumSet.noneOf(valueClass);
					EnumSet.allOf(valueClass);
				}

				@Override protected boolean valueMeetsFilter_SubType(Object value) {
					if (valueClass.isInstance(value)) {
						E e = valueClass.cast(value);
						return allowedValues.contains(e);
					} 
					return false;
				}

				@Override protected boolean equals_SubType(FilterRowsDialog.Filter other) {
					if (other instanceof Filter.EnumFilter) {
						Filter.EnumFilter<?> otherEnumFilter = (Filter.EnumFilter<?>) other;
						if (valueClass != otherEnumFilter.valueClass) return false;
						if (!areAllValuesContainedIn(otherEnumFilter.allowedValues)) return false;
						if (!otherEnumFilter.areAllValuesContainedIn(allowedValues)) return false;
						return true;
					}
					return false;
				}
				
				private boolean areAllValuesContainedIn(Set<?> values) {
					for (E e : allowedValues) {
						if (!values.contains(e))
							return false;
					}
					return true;
				}

				@Override protected FilterRowsDialog.Filter clone_SubType() {
					Filter.EnumFilter<E> enumFilter = new Filter.EnumFilter<>(filterID,valueClass);
					enumFilter.allowedValues.addAll(allowedValues);
					return enumFilter;
				}

				public static FilterRowsDialog.Filter parseEnumFilter(String str) {
					int pos = str.indexOf(':');
					if (pos<0) return null;
					String filterID = str.substring(0, pos);
					String namesStr = str.substring(pos+1);
					String[] names = namesStr.split(",");
					
					Function<String[], Filter.EnumFilter<?>> parseFilter = KnownTypes.get(filterID);
					if (parseFilter==null) {
						System.err.printf("Can't parse EnumFilter: Unknown filter ID \"%s\" in \"%s\"%n", filterID, str);
						return null;
					}
					
					return parseFilter.apply(names);
				}

				private static <E extends Enum<E>> Filter.EnumFilter<E> parseEnumFilter(Class<E> valueClass, String filterID, String[] names, Function<String, E> parseEnumValue) {
					Filter.EnumFilter<E> filter = new Filter.EnumFilter<>(filterID,valueClass);
					for (String name : names)
						try {
							filter.allowedValues.add(parseEnumValue.apply(name));
						} catch (Exception ex) {
							System.err.printf("EnumFilter[%s]: Can't parse value \"%s\" for enum \"%s\"%n", filterID, name, valueClass.getCanonicalName());
						}
					return filter;
				}

				@Override protected String toParsableString_SubType() {
					String[] names = allowedValues.stream().map(e->e.name()).toArray(String[]::new);
					return String.format("%s:%s", filterID, String.join(",", names));
				}
				
			}


			static class NumberFilter<V extends Number> extends FilterRowsDialog.Filter {
				
				V min,max;
				private final Class<V> valueClass;
				private final Function<V, String> toParsableString;
				private final BiFunction<V, V, Integer> compare;
				
				NumberFilter(Class<V> valueClass, V min, V max, BiFunction<V,V,Integer> compare, Function<V,String> toParsableString) {
					this.valueClass = valueClass;
					this.min = min;
					this.max = max;
					this.compare = compare;
					this.toParsableString = toParsableString;
					if (this.valueClass==null) throw new IllegalArgumentException();
					if (this.toParsableString==null) throw new IllegalArgumentException();
					if (this.min==null) throw new IllegalArgumentException();
					if (this.max==null) throw new IllegalArgumentException();
				}

				@Override protected boolean valueMeetsFilter_SubType(Object value) {
					if (valueClass.isInstance(value)) {
						V v = valueClass.cast(value);
						if (compare.apply(min, max)<=0)
							// ---- min #### max ---- 
							return compare.apply(min, v)<=0 && compare.apply(v, max)<=0;
						// #### max ---- min #### 
						return compare.apply(min, v)<=0 || compare.apply(v, max)<=0;
					} 
					return false;
				}

				@Override protected boolean equals_SubType(FilterRowsDialog.Filter other) {
					if (other instanceof Filter.NumberFilter) {
						Filter.NumberFilter<?> numberFilter = (Filter.NumberFilter<?>) other;
						if (valueClass!=numberFilter.valueClass) return false;
						if (!min.equals(numberFilter.min)) return false;
						if (!max.equals(numberFilter.max)) return false;
						return true;
					}
					return false;
				}

				@Override
				protected FilterRowsDialog.Filter clone_SubType() {
					return new Filter.NumberFilter<>(valueClass, min, max, compare, toParsableString);
				}
				
				public static FilterRowsDialog.Filter parseNumberFilter(String str) {
					String[] strs = str.split(":");
					if (strs.length!=3) return null;
					String valueClassName = strs[0];
					String minStr = strs[1];
					String maxStr = strs[2];
					
					switch (valueClassName) {
					case "Integer":
						try {
							Filter.NumberFilter<Integer> filter = createIntFilter();
							filter.min = Integer.parseInt(minStr);
							filter.max = Integer.parseInt(maxStr);
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					case "Long":
						try {
							Filter.NumberFilter<Long> filter = createLongFilter();
							filter.min = Long.parseLong(minStr);
							filter.max = Long.parseLong(maxStr);
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					case "Float":
						try {
							Filter.NumberFilter<Float> filter = createFloatFilter();
							filter.min = Float.intBitsToFloat( Integer.parseUnsignedInt(minStr,16) );
							filter.max = Float.intBitsToFloat( Integer.parseUnsignedInt(maxStr,16) );
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					case "Double":
						try {
							Filter.NumberFilter<Double> filter = createDoubleFilter();
							filter.min = Double.longBitsToDouble( Long.parseUnsignedLong(minStr,16) );
							filter.max = Double.longBitsToDouble( Long.parseUnsignedLong(maxStr,16) );
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					}
					return null;
				}

				@Override protected String toParsableString_SubType() {
					String minStr = toParsableString.apply(min);
					String maxStr = toParsableString.apply(max);
					String simpleName = valueClass.getSimpleName();
					return String.format("%s:%s:%s", simpleName, minStr, maxStr);
				}

				static Filter.NumberFilter<Integer> createIntFilter   () { return new Filter.NumberFilter<Integer>(Integer.class, 0 , 0 , Integer::compare, v->Integer.toString(v)); }
				static Filter.NumberFilter<Long   > createLongFilter  () { return new Filter.NumberFilter<Long   >(Long   .class, 0L, 0L, Long   ::compare, v->Long.toString(v)); }
				static Filter.NumberFilter<Float  > createFloatFilter () { return new Filter.NumberFilter<Float  >(Float  .class, 0f, 0f, Float  ::compare, v->String.format("%08X", Float.floatToIntBits(v))); }
				static Filter.NumberFilter<Double > createDoubleFilter() { return new Filter.NumberFilter<Double >(Double .class, 0d, 0d, Double ::compare, v->String.format("%016X", Double.doubleToLongBits(v))); }
				
			}
			
			
			static class BoolFilter extends FilterRowsDialog.Filter {
			
				boolean allowTrues = true;

				@Override protected boolean valueMeetsFilter_SubType(Object value) {
					if (value instanceof Boolean) {
						Boolean v = (Boolean) value;
						return allowTrues == v.booleanValue();
					}
					return false;
				}

				@Override protected boolean equals_SubType(FilterRowsDialog.Filter other) {
					if (other instanceof Filter.BoolFilter) {
						Filter.BoolFilter boolFilter = (Filter.BoolFilter) other;
						return this.allowTrues == boolFilter.allowTrues;
					}
					return false;
				}
			
				@Override protected Filter.BoolFilter clone_SubType() {
					Filter.BoolFilter boolFilter = new BoolFilter();
					boolFilter.allowTrues = allowTrues;
					return boolFilter;
				}

				public static FilterRowsDialog.Filter parseBoolFilter(String str) {
					Filter.BoolFilter boolFilter = new BoolFilter();
					switch (str) {
					case "true" : boolFilter.allowTrues = true; break;
					case "false": boolFilter.allowTrues = false; break;
					default: return null;
					}
					return boolFilter;
				}

				@Override protected String toParsableString_SubType() {
					return Boolean.toString(allowTrues);
				}
				
			}
			
			
			
		}
		
		static class FilterGuiElement {
			private static final HashMap<Class<?>,Supplier<FilterGuiElement.OptionsPanel>> RegisteredOptionsPanels = new HashMap<>();
			static {
				RegisteredOptionsPanels.put(Boolean.class, ()->new OptionsPanel.BoolOptions(new Filter.BoolFilter()));
				RegisteredOptionsPanels.put(Integer.class, ()->OptionsPanel.NumberOptions.create( Filter.NumberFilter.createIntFilter()   , v->Integer.toString(v), Integer::parseInt   , null             ));
				RegisteredOptionsPanels.put(Long   .class, ()->OptionsPanel.NumberOptions.create( Filter.NumberFilter.createLongFilter()  , v->Long   .toString(v), Long   ::parseLong  , null             ));
				RegisteredOptionsPanels.put(Float  .class, ()->OptionsPanel.NumberOptions.create( Filter.NumberFilter.createFloatFilter() , v->Float  .toString(v), Float  ::parseFloat , Float ::isFinite ));
				RegisteredOptionsPanels.put(Double .class, ()->OptionsPanel.NumberOptions.create( Filter.NumberFilter.createDoubleFilter(), v->Double .toString(v), Double ::parseDouble, Double::isFinite ));
				RegisteredOptionsPanels.put(Truck.Country      .class, ()->OptionsPanel.EnumOptions.create(Truck.Country      .class, Truck.Country      .values()));
				RegisteredOptionsPanels.put(Truck.DiffLockType .class, ()->OptionsPanel.EnumOptions.create(Truck.DiffLockType .class, Truck.DiffLockType .values()));
				RegisteredOptionsPanels.put(Truck.TruckType    .class, ()->OptionsPanel.EnumOptions.create(Truck.TruckType    .class, Truck.TruckType    .values()));
				RegisteredOptionsPanels.put(Truck.UDV.ItemState.class, ()->OptionsPanel.EnumOptions.create(Truck.UDV.ItemState.class, Truck.UDV.ItemState.values()));
			}
			
			private static final Color COLOR_BG_ACTIVE_FILTER = new Color(0xFFDB00);
			final ColumnID columnID;
			private final JCheckBox baseCheckBox;
			private final FilterGuiElement.OptionsPanel optionsPanel;
			private final Color defaultBG;

			FilterGuiElement(ColumnID columnID) {
				this.columnID = columnID;
				
				Supplier<FilterGuiElement.OptionsPanel> creator = RegisteredOptionsPanels.get(this.columnID.config.columnClass);
				if (creator==null) optionsPanel = new OptionsPanel.DummyOptions(this.columnID.config.columnClass);
				else               optionsPanel = creator.get();
				
				optionsPanel.setValues(this.columnID.filter);
				if (optionsPanel.filter!=null)
					optionsPanel.filter.active = this.columnID.filter!=null && this.columnID.filter.active;
				
				baseCheckBox = SnowRunner.createCheckBox(
						this.columnID.config.name,
						optionsPanel.filter!=null && optionsPanel.filter.active,
						null, optionsPanel.filter!=null,
						this::setActive);
				defaultBG = baseCheckBox.getBackground();
				
				showActive(optionsPanel.filter!=null && optionsPanel.filter.active);
			}

			private void setActive(boolean isActive) {
				if (optionsPanel.filter!=null)
					optionsPanel.filter.active = isActive;
				showActive(isActive);
			}

			private void showActive(boolean isActive) {
				optionsPanel.setEnableOptions(isActive);
				baseCheckBox.setBackground(isActive ? COLOR_BG_ACTIVE_FILTER : defaultBG);
				//optionsPanel.setBackground(isActive ? COLOR_BG_ACTIVE_FILTER : defaultBG);
			}

			FilterRowsDialog.Filter getFilter() {
				return optionsPanel.filter;
			}

			void setValues(FilterRowsDialog.Filter filter) {
				if (optionsPanel.filter!=null) {
					optionsPanel.filter.active = filter!=null && filter.active;
					baseCheckBox.setSelected(optionsPanel.filter.active);
					showActive(optionsPanel.filter.active);
				}
				optionsPanel.setValues(filter);
			}

			static JPanel createEmptyColumnPanel() {
				return new JPanel(new GridBagLayout());
			}

			void addToPanel(JPanel columnPanel) {
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				c.weightx = 0;
				c.gridwidth = 1;
				columnPanel.add(baseCheckBox, c);
				
				c.weightx = 1;
				c.gridwidth = GridBagConstraints.REMAINDER;
				columnPanel.add(optionsPanel, c);
			}
			
			static abstract class OptionsPanel extends JPanel {
				private static final long serialVersionUID = -8491252831775091069L;
				
				protected final GridBagConstraints c;
				final FilterRowsDialog.Filter filter;
				private final JCheckBox chkbxUnset;

				OptionsPanel(FilterRowsDialog.Filter filter) {
					super(new GridBagLayout());
					this.filter = filter;
					c = new GridBagConstraints();
					c.fill = GridBagConstraints.BOTH;
					c.weightx = 0;
					if (this.filter!=null)
						add(chkbxUnset = SnowRunner.createCheckBox("unset", false, null, true, b->this.filter.allowUnset = b), c);
					else
						chkbxUnset = null;
				}

				void setEnableOptions(boolean isEnabled) {
					if (chkbxUnset!=null)
						chkbxUnset.setEnabled(isEnabled);
				}

				void setValues(FilterRowsDialog.Filter filter) {
					if (this.filter!=null) {
						this.filter.allowUnset = filter!=null && filter.allowUnset;
						chkbxUnset.setSelected(this.filter.allowUnset);
					}
				}


				static class DummyOptions extends FilterGuiElement.OptionsPanel {
					private static final long serialVersionUID = 4500779916477896148L;

					public DummyOptions(Class<?> columnClass) {
						super(null);
						c.weightx = 1;
						String message;
						if (columnClass==String.class) message = "No Filter for text values";
						else message = String.format("No Filter for Column of %s", columnClass==null ? "<null>" : columnClass.getCanonicalName());
						add(new JLabel(message), c);
					}
				
				}


				static class EnumOptions<E extends Enum<E>> extends FilterGuiElement.OptionsPanel {
					private static final long serialVersionUID = -458324264563153126L;
					
					private final Filter.EnumFilter<E> filter;
					private final JCheckBox[] checkBoxes;
					private final E[] values;

					EnumOptions(Filter.EnumFilter<E> filter, E[] values) {
						super(filter);
						if (filter==null) throw new IllegalArgumentException();
						if (values==null) throw new IllegalArgumentException();
						
						this.filter = filter;
						this.values = values;
						
						checkBoxes = new JCheckBox[this.values.length];
						c.weightx = 0;
						for (int i=0; i<this.values.length; i++) {
							E e = this.values[i];
							boolean isSelected = this.filter.allowedValues.contains(e);
							checkBoxes[i] = SnowRunner.createCheckBox(e.toString(), isSelected, null, true, b->{
								if (b) this.filter.allowedValues.add(e);
								else   this.filter.allowedValues.remove(e);
							});
							add(checkBoxes[i], c);
						}
						c.weightx = 1;
						add(new JLabel(), c);
					}

					@Override void setEnableOptions(boolean isEnabled) {
						super.setEnableOptions(isEnabled);
						for (int i=0; i<checkBoxes.length; i++)
							checkBoxes[i].setEnabled(isEnabled);
					}

					@Override void setValues(FilterRowsDialog.Filter filter) {
						super.setValues(filter);
						if (filter instanceof Filter.EnumFilter) {
							Filter.EnumFilter<?> enumFilter = (Filter.EnumFilter<?>) filter;
							
							if (this.filter.valueClass!=enumFilter.valueClass)
								throw new IllegalStateException();
							
							this.filter.allowedValues.clear();
							for (int i=0; i<values.length; i++) {
								E e = values[i];
								if (enumFilter.allowedValues.contains(e)) {
									this.filter.allowedValues.add(e);
									checkBoxes[i].setSelected(true);										
								} else
									checkBoxes[i].setSelected(false);										
							}
						}
					}

					static <E extends Enum<E>> OptionsPanel.EnumOptions<E> create( Class<E> valueClass, E[] values) {
						return new OptionsPanel.EnumOptions<>(new Filter.EnumFilter<>(Filter.EnumFilter.getFilterID(valueClass), valueClass), values);
					}
				}


				static class NumberOptions<V extends Number> extends FilterGuiElement.OptionsPanel {
					private static final long serialVersionUID = -436186682804402478L;
					
					private final Filter.NumberFilter<V> filter;
					private final JLabel labMin, labMax;
					private final JTextField fldMin, fldMax;
					private final Function<V, String> toString;

					NumberOptions(Filter.NumberFilter<V> filter, Function<V,String> toString, Function<String,V> convert, Predicate<V> isOK) {
						super(filter);
						this.filter = filter;
						this.toString = toString;
						
						if (filter==null) throw new IllegalArgumentException();
						if (toString==null) throw new IllegalArgumentException();
						if (convert==null) throw new IllegalArgumentException();
						
						c.weightx = 0;
						add(labMin = new JLabel("   min: "), c);
						add(fldMin = SnowRunner.createTextField(10, this.toString.apply(this.filter.min), convert, isOK, v->this.filter.min = v), c);
						add(labMax = new JLabel("   max: "), c);
						add(fldMax = SnowRunner.createTextField(10, this.toString.apply(this.filter.max), convert, isOK, v->this.filter.max = v), c);
						c.weightx = 1;
						add(new JLabel(), c);
					}
					
					static <V extends Number> OptionsPanel.NumberOptions<V> create(Filter.NumberFilter<V> filter, Function<V,String> toString, Function<String,V> convert, Predicate<V> isOK) {
						return new OptionsPanel.NumberOptions<V>( filter,
								v -> v==null ? "<null>" : toString.apply(v),
								str -> {
									try { return convert.apply(str);
									} catch (NumberFormatException e) { return null; }
								},
								isOK
							);
					}

					@Override void setEnableOptions(boolean isEnabled) {
						super.setEnableOptions(isEnabled);
						labMin.setEnabled(isEnabled);
						fldMin.setEnabled(isEnabled);
						labMax.setEnabled(isEnabled);
						fldMax.setEnabled(isEnabled);
					}

					@Override void setValues(FilterRowsDialog.Filter filter) {
						super.setValues(filter);
						if (filter instanceof Filter.NumberFilter) {
							Filter.NumberFilter<?> numberFilter = (Filter.NumberFilter<?>) filter;
							if (this.filter.valueClass!=numberFilter.valueClass)
								throw new IllegalStateException();
							this.filter.min = this.filter.valueClass.cast(numberFilter.min);
							this.filter.max = this.filter.valueClass.cast(numberFilter.max);
							fldMin.setText(toString.apply(this.filter.min));
							fldMax.setText(toString.apply(this.filter.max));
						}
					}
					
				}


				static class BoolOptions extends FilterGuiElement.OptionsPanel {
					private static final long serialVersionUID = 1821563263682227455L;
					private final Filter.BoolFilter filter;
					private final JRadioButton rbtnTrue;
					private final JRadioButton rbtnFalse;
					
					BoolOptions(Filter.BoolFilter filter) {
						super(filter);
						this.filter = filter;
						ButtonGroup bg = new ButtonGroup();
						c.weightx = 0;
						add(rbtnTrue  = SnowRunner.createRadioButton("TRUE" , bg, true,  this.filter.allowTrues, e->this.filter.allowTrues = true ), c);
						add(rbtnFalse = SnowRunner.createRadioButton("FALSE", bg, true, !this.filter.allowTrues, e->this.filter.allowTrues = false), c);
						c.weightx = 1;
						add(new JLabel(), c);
					}

					@Override void setEnableOptions(boolean isEnabled) {
						super.setEnableOptions(isEnabled);
						rbtnTrue .setEnabled(isEnabled);
						rbtnFalse.setEnabled(isEnabled);
					}

					@Override void setValues(FilterRowsDialog.Filter filter) {
						super.setValues(filter);
						if (filter instanceof Filter.BoolFilter) {
							Filter.BoolFilter boolFilter = (Filter.BoolFilter) filter;
							this.filter.allowTrues = boolFilter.allowTrues;
							if (this.filter.allowTrues) rbtnTrue .setSelected(true);
							else                        rbtnFalse.setSelected(true);
						}
					}
					
				}
			}
		}

		private static class Presets extends ModelConfigureDialog.PresetMaps<HashMap<String,FilterRowsDialog.Filter>> {
		
			Presets() {
				super(SnowRunner.AppSettings.ValueKey.FilterRowsPresets, SnowRunner.FilterRowsPresetsFile, HashMap<String,FilterRowsDialog.Filter>::new);
			}
		
			@Override protected void parseLine(String line, HashMap<String, FilterRowsDialog.Filter> preset) {
				String valueStr;
				if ( (valueStr=Data.getLineValue(line, "Filter="))!=null ) {
					int endOfKey = valueStr.indexOf(':');
					if (endOfKey<0) {
						System.err.printf("Can't parse filter in line for FilterRowsPresets: \"%s\"%n", valueStr);
						return;
					}
					String key = valueStr.substring(0, endOfKey);
					String filterStr = valueStr.substring(endOfKey+1);
					FilterRowsDialog.Filter filter = FilterRowsDialog.Filter.parse( filterStr );
					if (filter!=null) preset.put(key, filter);
					else System.err.printf("Can't parse filter in line for FilterRowsPresets: \"%s\"%n", valueStr);
				}
			}
		
			@Override protected void writePresetInLines(HashMap<String, FilterRowsDialog.Filter> preset, PrintWriter out) {
				Vector<String> keys = new Vector<>(preset.keySet());
				keys.sort(null);
				for (String key : keys) {
					FilterRowsDialog.Filter filter = preset.get(key);
					out.printf("Filter=%s:%s%n", key, filter.toParsableString());
				}
			}
			
		}
	
	}

	private static class ColumnHideDialog extends ModelConfigureDialog<HashSet<String>> {
		private static final long serialVersionUID = 4240161527743718020L;
		private static final ColumnHideDialog.Presets presets = new Presets();
		
		private final JCheckBox[] columnElements;
		private final HashSet<String> currentPreset;
		
		ColumnHideDialog(Window owner, ColumnID[] originalColumns, String tableModelIDstr) {
			super(owner, "Visible Columns", "Define visible columns", originalColumns, presets, tableModelIDstr);
			
			currentPreset = new HashSet<>();
			JPanel columnPanel = new JPanel(new GridLayout(0,1));
			columnElements = new JCheckBox[originalColumns.length];
			for (int i=0; i<originalColumns.length; i++) {
				JCheckBox columnElement = createColumnElement(this.originalColumns[i]);
				columnElements[i] = columnElement;
				columnPanel.add(columnElement);
			}
			
			GridBagConstraints c = new GridBagConstraints();
			c.weighty = 1;
			columnPanel.add(new JLabel(), c);
			
			columnScrollPane.setViewportView(columnPanel);
		}

		@Override protected HashSet<String> getCopyOfCurrentPreset() {
			return new HashSet<>(currentPreset);
		}
		
		@Override protected void setPresetInGui(HashSet<String> preset) {
			currentPreset.clear();
			currentPreset.addAll(preset);
			for (int i=0; i<this.originalColumns.length; i++) {
				ColumnID column = this.originalColumns[i];
				boolean isVisible = preset.contains(column.id);
				columnElements[i].setSelected(isVisible);
			}
		}

		private JCheckBox createColumnElement(ColumnID columnID) {
			if (columnID.isVisible) currentPreset.add(columnID.id);
			JCheckBox checkBox = SnowRunner.createCheckBox(columnID.config.name, columnID.isVisible, null, true, b->{
				if (b) currentPreset.add(columnID.id);
				else   currentPreset.remove(columnID.id);
			});
			return checkBox;
		}

//			@Override protected void addColumnElementToPanel(JPanel columnPanel, JCheckBox columnElement) {
//				columnPanel.add(columnElement);
//			}

		@Override protected boolean resetValuesFinal() {
			boolean hasChanged = false;
			for (ColumnID columnID : this.originalColumns) {
				if (!columnID.isVisible) {
					columnID.isVisible = true;
					hasChanged = true;
				}
			}
			return hasChanged;
		}

		@Override protected boolean setValuesFinal() {
			boolean hasChanged = false;
			for (ColumnID columnID : originalColumns) {
				boolean isVisible = currentPreset.contains(columnID.id);
				if (columnID.isVisible != isVisible) {
					columnID.isVisible = isVisible;
					hasChanged = true;
				}
			}
			return hasChanged;
		}
		
		static ColumnID[] removeHiddenColumns(ColumnID[] originalColumns) {
			return Arrays.stream(originalColumns).filter(column->column.isVisible).toArray(ColumnID[]::new);
		}

		private static class Presets extends ModelConfigureDialog.PresetMaps<HashSet<String>> {
		
			Presets() {
				super(SnowRunner.AppSettings.ValueKey.ColumnHidePresets, SnowRunner.ColumnHidePresetsFile, HashSet<String>::new);
			}
			
			@Override protected void parseLine(String line, HashSet<String> preset) {
				String valueStr;
				if ( (valueStr=Data.getLineValue(line, "Columns="))!=null ) {
					String[] columnIDs = valueStr.split(",");
					preset.clear();
					preset.addAll(Arrays.asList(columnIDs));
				}
			}
			
			@Override protected void writePresetInLines(HashSet<String> preset, PrintWriter out) {
				Vector<String> sortedPreset = new Vector<>(preset);
				sortedPreset.sort(null);
				out.printf("Columns=%s%n", String.join(",", sortedPreset));
			}
			
		}
	}

	private static abstract class ModelConfigureDialog<Preset> extends JDialog {
		private static abstract class PresetMaps<Preset> {
			
			private final HashMap<String,HashMap<String,Preset>> presets;
			private final SnowRunner.AppSettings.ValueKey settingsKey;
			private final Supplier<Preset> createEmptyPreset;
			private final String pathname;
			
			@SuppressWarnings("unused")
			PresetMaps(SnowRunner.AppSettings.ValueKey settingsKey, Supplier<Preset> createEmptyPreset) {
				this(settingsKey, null, createEmptyPreset);
			}
			@SuppressWarnings("unused")
			PresetMaps(String pathname, Supplier<Preset> createEmptyPreset) {
				this(null, pathname, createEmptyPreset);
			}
			PresetMaps(SnowRunner.AppSettings.ValueKey settingsKey, String pathname, Supplier<Preset> createEmptyPreset) {
				this.settingsKey = settingsKey;
				this.createEmptyPreset = createEmptyPreset;
				this.pathname = pathname;
				this.presets = new HashMap<>();
				if (pathname==null && settingsKey==null)
					throw new IllegalArgumentException();
				read();
			}
		
			HashMap<String, Preset> getModelPresets(String tableModelID) {
				HashMap<String, Preset> modelPresets = presets.get(tableModelID);
				if (modelPresets==null)
					presets.put(tableModelID, modelPresets = new HashMap<>());
				return modelPresets;
			}
		
			void read() {
				presets.clear();
				
				String text = null;
				File file = pathname==null ? null : new File(pathname);
				
				if (settingsKey!=null && SnowRunner.settings.contains(settingsKey))
					text = SnowRunner.settings.getString(settingsKey, null);
				
				else if (file!=null && file.isFile())
					try {
						System.out.printf("Read PresetMaps from file \"%s\" ...%n", file.getAbsolutePath());
						byte[] bytes = Files.readAllBytes(file.toPath());
						text = new String(bytes, StandardCharsets.UTF_8);
						System.out.printf("... done%n");
					} catch (IOException e) {
						text = null;
					}
				
				else
					throw new IllegalStateException();
				
				if (text==null) return;
				
				try (BufferedReader in = new BufferedReader(new StringReader(text))) {
					
					HashMap<String, Preset> modelPresets = null;
					Preset preset = null;
					String line, valueStr;
					while ( (line=in.readLine())!=null ) {
						
						if (line.equals("[Preset]") || line.isEmpty()) {
							modelPresets = null;
							preset = null;
						}
						if ( (valueStr=Data.getLineValue(line, "TableModel="))!=null ) {
							modelPresets = getModelPresets(valueStr);
							preset = null;
						}
						if ( (valueStr=Data.getLineValue(line, "Preset="))!=null ) {
							preset = modelPresets.get(valueStr);
							if (preset==null)
								modelPresets.put(valueStr, preset = createEmptyPreset.get());
						}
						parseLine(line, preset);
						
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		
			void write() {
				StringWriter stringWriter = new StringWriter();
				try (PrintWriter out = new PrintWriter(stringWriter)) {
					
					Vector<String> tableModelIDs = new Vector<>(presets.keySet());
					tableModelIDs.sort(null);
					for (String tableModelID : tableModelIDs) {
						HashMap<String, Preset> modelPresets = presets.get(tableModelID);
						Vector<String> presetNames = new Vector<>(modelPresets.keySet());
						presetNames.sort(null);
						for (String presetName : presetNames) {
							out.printf("[Preset]%n");
							out.printf("TableModel=%s%n", tableModelID);
							out.printf("Preset=%s%n", presetName);
							writePresetInLines(modelPresets.get(presetName), out);
							out.printf("%n");
						}
					}
					
				}
				
				String text = stringWriter.toString();
				
				if (settingsKey!=null)
					SnowRunner.settings.putString(settingsKey, text);
				
				if (pathname!=null)
					try {
						File file = new File(pathname);
						System.out.printf("Write PresetMaps to file \"%s\" ...%n", file.getAbsolutePath());
						byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
						Files.write(file.toPath(), bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
						System.out.printf("... done%n");
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		
			protected abstract void parseLine(String line, Preset preset);
			protected abstract void writePresetInLines(Preset preset, PrintWriter out);
		}

		private static final long serialVersionUID = 8159900024537014376L;
		
		private final Window owner;
		private boolean hasChanged;
		private boolean ignorePresetComboBoxEvent;
		private final HashMap<String, Preset> modelPresets;
		protected final ColumnID[] originalColumns;
		private final Vector<String> presetNames;
		private final JComboBox<String> presetComboBox;
		private final ModelConfigureDialog.PresetMaps<Preset> presetMaps;
		protected final JScrollPane columnScrollPane;

		
		ModelConfigureDialog(Window owner, String title, String columnPanelHeadline, ColumnID[] originalColumns, ModelConfigureDialog.PresetMaps<Preset> presetMaps, String tableModelID) {
			super(owner, title, ModalityType.APPLICATION_MODAL);
			this.owner = owner;
			this.originalColumns = originalColumns;
			this.presetMaps = presetMaps;
			this.modelPresets = this.presetMaps.getModelPresets(tableModelID);
			this.hasChanged = false;
			GridBagConstraints c;
			
			columnScrollPane = new JScrollPane();
			columnScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			columnScrollPane.setPreferredSize(new Dimension(300,400));
			columnScrollPane.getVerticalScrollBar().setUnitIncrement(10);
			
			presetNames = new Vector<>(modelPresets.keySet());
			presetNames.sort(null);
			presetComboBox = new JComboBox<>(new Vector<>(presetNames));
			presetComboBox.setSelectedItem(null);
			//presetComboBox.setMinimumSize(new Dimension(100,20));
			
			JButton btnOverwrite, btnRemove;
			JPanel presetPanel = new JPanel(new GridBagLayout());
			//presetPanel.setPreferredSize(new Dimension(200,20));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx=0;
			presetPanel.add(new JLabel("Presets: "),c);
			c.weightx=1;
			presetPanel.add(presetComboBox,c);
			c.weightx=0;
			Insets smallButtonMargin = new Insets(3,3,3,3);
			presetPanel.add(btnOverwrite = SnowRunner.createButton("Overwrite", false, smallButtonMargin, e->overwriteSelectedPreset()),c);
			presetPanel.add(               SnowRunner.createButton("Add"      ,  true, smallButtonMargin, e->addPreset()),c);
			presetPanel.add(btnRemove    = SnowRunner.createButton("Remove"   , false, smallButtonMargin, e->removePreset()),c);
			
			ignorePresetComboBoxEvent = false;
			presetComboBox.addActionListener(e->{
				if (ignorePresetComboBoxEvent) return;
				int index = presetComboBox.getSelectedIndex();
				setSelectedPresetInGui(index);
				btnOverwrite.setEnabled(index>=0);
				btnRemove.setEnabled(index>=0);
			});
			
			
			JPanel dlgButtonPanel = new JPanel(new GridBagLayout());
			dlgButtonPanel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			dlgButtonPanel.add(new JLabel(),c);
			c.weightx = 0;
			dlgButtonPanel.add(SnowRunner.createButton("Reset", true, e->{
				hasChanged = resetValuesFinal();
				setVisible(false);
			}),c);
			dlgButtonPanel.add(SnowRunner.createButton("Set", true, e->{
				hasChanged = setValuesFinal();
				setVisible(false);
			}),c);
			dlgButtonPanel.add(SnowRunner.createButton("Cancel", true, e->{
				setVisible(false);
			}),c);
			
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.weighty = 0;
			contentPane.add(new JLabel(columnPanelHeadline+":"), c);
			c.weighty = 1;
			contentPane.add(columnScrollPane, c);
			c.weighty = 0;
			contentPane.add(presetPanel, c);
			contentPane.add(dlgButtonPanel, c);
			
			setContentPane(contentPane);
		}

		protected abstract boolean resetValuesFinal();
		protected abstract boolean setValuesFinal();
		protected abstract void setPresetInGui(Preset preset);
		protected abstract Preset getCopyOfCurrentPreset();

		private void addPreset() {
			String presetName = JOptionPane.showInputDialog(this, "Enter preset name:", "Preset Name", JOptionPane.QUESTION_MESSAGE);
			if (presetName==null)
				return;
			if (presetNames.contains(presetName)) {
				String message = String.format("Preset name \"%s\" is already in use. Do you want to overwrite this preset?", presetName);
				if (JOptionPane.showConfirmDialog(this, message, "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION)!=JOptionPane.YES_OPTION)
					return;
				ignorePresetComboBoxEvent = true;
				presetComboBox.setSelectedItem(presetName);
				ignorePresetComboBoxEvent = false;
				
			} else {
				presetNames.add(presetName);
				presetNames.sort(null);
				updatePresetComboBoxModel(presetName);
			}
			modelPresets.put(presetName, getCopyOfCurrentPreset());
			presetMaps.write();
		}

		private void removePreset() {
			int index = presetComboBox.getSelectedIndex();
			String presetName = getPresetName(index);
			if (presetName!=null) {
				String message = String.format("Do you really want to remove preset \"%s\"?", presetName);
				if (JOptionPane.showConfirmDialog(this, message, "Are you sure?", JOptionPane.YES_NO_CANCEL_OPTION)!=JOptionPane.YES_OPTION)
					return;
				modelPresets.remove(presetName);
				presetMaps.write();
				presetNames.remove(presetName);
				updatePresetComboBoxModel(null);
			}
		}

		private void updatePresetComboBoxModel(String presetName) {
			ignorePresetComboBoxEvent = true;
			presetComboBox.setModel(new DefaultComboBoxModel<>(presetNames));
			presetComboBox.setSelectedItem(presetName);
			ignorePresetComboBoxEvent = false;
		}

		private void overwriteSelectedPreset() {
			int index = presetComboBox.getSelectedIndex();
			String presetName = getPresetName(index);
			if (presetName!=null) {
				modelPresets.put(presetName, getCopyOfCurrentPreset());
				presetMaps.write();
			}
		}

		private void setSelectedPresetInGui(int index) {
			String presetName = getPresetName(index);
			if (presetName!=null) {
				Preset preset = modelPresets.get(presetName);
				setPresetInGui(preset);
			}
		}

		private String getPresetName(int index) {
			return index<0 ? null : presetNames.get(index);
		}
	
		boolean showDialog() {
			pack();
			setLocationRelativeTo(owner);
			setVisible(true);
			return hasChanged;
		}
	
	}

	public static class ColumnID implements Tables.SimplifiedColumnIDInterface {
		
		public interface TableModelBasedBuilder<ValueType> {
			ValueType getValue(Object value, VerySimpleTableModel<?> tableModel);
		}
		
		public interface LanguageBasedStringBuilder {
			String getValue(Object value, Language language);
		}
		
		private final SimplifiedColumnConfig config;
		private final String id;
		private final Function<Object, ?> getValue;
		private final LanguageBasedStringBuilder getValueL;
		private final TableModelBasedBuilder<?> getValueT;
		private Function<Object, ?> getValue_verbose;
		private LanguageBasedStringBuilder getValueL_verbose;
		private TableModelBasedBuilder<?> getValueT_verbose;
		private final boolean useValueAsStringID;
		private final Integer horizontalAlignment;
		private final Color foreground;
		private final Color background;
		private final String format;
		private boolean isVisible;
		private FilterRowsDialog.Filter filter;
		
		public ColumnID(String ID, String name, Class<String> columnClass, int prefWidth, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
			this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, false, null, getValue, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue) {
			this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, getValue, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
			this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, null, null, getValue);
		}
		public ColumnID(String ID, String name, Class<String> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, false, null, getValue, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, getValue, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, null, null, getValue);
		}
		private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue, LanguageBasedStringBuilder getValueL, TableModelBasedBuilder<ColumnType> getValueT) {
			this.isVisible = true;
			this.filter = null;
			this.id = ID;
			this.horizontalAlignment = horizontalAlignment;
			this.format = format;
			this.useValueAsStringID = useValueAsStringID;
			this.getValue = getValue;
			this.getValueL = getValueL;
			this.getValueT = getValueT;
			this.foreground = foreground;
			this.background = background;
			this.config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
			if (useValueAsStringID && columnClass!=String.class)
				throw new IllegalStateException();
			this.getValue_verbose = null;
			this.getValueL_verbose = null;
			this.getValueT_verbose = null;
		}
		@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		
		public Object getValue(Object row, Language language, VerySimpleTableModel<?> tableModel) {
			return getValue(row, language, tableModel, getValue, getValueL, getValueT);
		}
		
		public Object getValue_verbose(Object row, Language language, VerySimpleTableModel<?> tableModel) {
			return getValue(row, language, tableModel, getValue_verbose, getValueL_verbose, getValueT_verbose);
		}
		
		private Object getValue(
				Object row, Language language, VerySimpleTableModel<?> tableModel,
				Function<Object, ?> getValue, LanguageBasedStringBuilder getValueL, TableModelBasedBuilder<?> getValueT
		) {
			if (getValue!=null) {
				if (useValueAsStringID) {
					String stringID = (String) getValue.apply(row);
					return SnowRunner.solveStringID(stringID, language);
				}
				return getValue.apply(row);
			}
			if (getValueL!=null) {
				return getValueL.getValue(row,language);
			}
			if (getValueT!=null) {
				return getValueT.getValue(row,tableModel);
			}
			
			return null;
		}
		
		public boolean hasVerboseValueFcn() {
			return getValue_verbose!=null || getValueL_verbose!=null || getValueT_verbose!=null;
		}
		
		public ColumnID setVerboseValueFcn(Function<Object, ?> getValue) {
			getValue_verbose = getValue;
			return this;
		}
		
		public ColumnID setVerboseValueFcn(LanguageBasedStringBuilder getValueL) {
			getValueL_verbose = getValueL;
			return this;
		}
		
		public ColumnID setVerboseValueFcn(TableModelBasedBuilder<?> getValueT) {
			getValueT_verbose = getValueT;
			return this;
		}
	}
	
	public static abstract class TextOutputSourceTableModel<RowType> extends VerySimpleTableModel<RowType> {
		
		protected Runnable textOutputUpdateMethod;

		protected TextOutputSourceTableModel(Window mainWindow, Controllers controllers, ColumnID[] columns) {
			super(mainWindow, controllers, columns);
			textOutputUpdateMethod = null;
		}
		
		public void setOutputUpdateMethod(Runnable textOutputUpdateMethod) {
			this.textOutputUpdateMethod = textOutputUpdateMethod;
		}

		@Override protected void extraUpdate() {
			updateTextOutput();
		}

		void updateTextOutput() {
			if (textOutputUpdateMethod!=null)
				textOutputUpdateMethod.run();
		}
	}
	
	public static abstract class ExtendedVerySimpleTableModel2<RowType> extends TextOutputSourceTableModel<RowType> implements TextPaneOutputSource {

		protected ExtendedVerySimpleTableModel2(Window mainWindow, Controllers controllers, ColumnID[] columns) {
			super(mainWindow, controllers, columns);
		}

		@Override
		public void setContentForRow(StyledDocumentInterface doc, int rowIndex) {
			RowType row = getRow(rowIndex);
			if (row!=null)
				setContentForRow(doc, row);
		}

		protected abstract void setContentForRow(StyledDocumentInterface doc, RowType row);
	}
	
	public static abstract class ExtendedVerySimpleTableModel1<RowType> extends TextOutputSourceTableModel<RowType> implements TextAreaOutputSource {

		protected ExtendedVerySimpleTableModel1(Window mainWindow, Controllers controllers, ColumnID[] columns) {
			super(mainWindow, controllers, columns);
		}
		
		@Override public String getTextForRow(int rowIndex) {
			RowType row = getRow(rowIndex);
			if (row==null) return "";
			return getTextForRow(row);
		}

		protected abstract String getTextForRow(RowType row);
	}
}