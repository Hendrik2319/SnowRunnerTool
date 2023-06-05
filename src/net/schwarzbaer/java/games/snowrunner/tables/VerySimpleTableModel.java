package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
import java.util.function.Consumer;
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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TextOutput;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TableContextMenuModifier;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TextAreaOutputSource;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TextPaneOutputSource;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.UnspecificOutputSource;
import net.schwarzbaer.java.lib.gui.HSColorChooser;
import net.schwarzbaer.java.lib.gui.StandardDialog;
import net.schwarzbaer.java.lib.gui.StyledDocumentInterface;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.TextAreaDialog;
import net.schwarzbaer.java.lib.gui.GeneralIcons.GrayCommandIcons;
import net.schwarzbaer.java.lib.gui.Tables.SimplifiedColumnConfig;

public abstract class VerySimpleTableModel<RowType> extends Tables.SimplifiedTableModel<VerySimpleTableModel.ColumnID> implements LanguageListener, SwingConstants, TableContextMenuModifier, Finalizable {
	
	static final Color COLOR_BG_FALSE = new Color(0xFF6600);
	static final Color COLOR_BG_TRUE = new Color(0x99FF33);
	static final Color COLOR_BG_EDITABLECELL = new Color(0xFFFAE7);
	static final Color COLOR_BG_ODD  = new Color(0xF5F5FF);
	static final Color COLOR_BG_EVEN = new Color(0xFFFFEF);
	
	@SuppressWarnings("unused")
	private   final Float columns; // to hide super.columns
	private   final String tableModelID;
	private   final String tableModelInstanceID;
	private   final ColumnID[] originalColumns;
	protected final Vector<RowType> rows;
	private   final Vector<RowType> originalRows;
	protected final Window mainWindow;
	protected final Finalizer finalizer;
	protected final Coloring<RowType> coloring;
	protected Language language;
	private   Comparator<RowType> initialRowOrder;
	private   int clickedColumnIndex;
	private   int clickedRowIndex;
	private   ColumnID clickedColumn;
	private   RowType clickedRow;
	protected final GlobalFinalDataStructures gfds;

	protected VerySimpleTableModel(Window mainWindow, GlobalFinalDataStructures gfds, ColumnID[] columns) {
		this(mainWindow, gfds, null, columns);
	}
	protected VerySimpleTableModel(Window mainWindow, GlobalFinalDataStructures gfds, String tableModelInstanceID, ColumnID[] columns) {
		super(columns);
		this.mainWindow = mainWindow;
		this.gfds = gfds;
		this.tableModelInstanceID = tableModelInstanceID;
		this.finalizer = this.gfds.controllers.createNewFinalizer();
		this.originalColumns = columns;
		this.columns = null; // to hide super.columns
		tableModelID = getClass().getName();
		language = null;
		initialRowOrder = null;
		rows = new Vector<>();
		originalRows = new Vector<>();
		clickedColumn = null;
		clickedColumnIndex = -1;
		clickedRow = null;
		coloring = new Coloring<>(this);
		
		finalizer.addLanguageListener(lang->{
			clearCacheOfColumns(ColumnID.Update.Language);
			setLanguage(lang);
		});
		
		for (ColumnID.Update event : ColumnID.Update.values())
			if (exitsColumnWithUpdateEvent(event))
				switch (event)
				{
					case Data    : break; // --> connectToGlobalData
					case Language: break; // --> some lines above :)
					case DLCAssignment:
						finalizer.addDLCListener(() ->
							clearCacheOfColumns(ColumnID.Update.DLCAssignment)
						);
						break;
					case SaveGame:
						finalizer.addSaveGameListener(savegame -> {
							clearCacheOfColumns(ColumnID.Update.SaveGame);
						});
						break;
					case SpecialTruckAddons:
						finalizer.addSpecialTruckAddonsListener((category, change) -> {
							clearCacheOfColumns(ColumnID.Update.SpecialTruckAddons);
						});
						break;
				}
			
		coloring.setBackgroundColumnColoring(true, Boolean.class, (row, b) -> b ? COLOR_BG_TRUE : COLOR_BG_FALSE);
		
		HashSet<String> columnIDIDs = new HashSet<>();
		for (int i=0; i<originalColumns.length; i++) {
			ColumnID columnID = originalColumns[i];
			if (columnIDIDs.contains(columnID.id))
				throw new IllegalStateException(String.format("Found a non unique column ID \"%s\" in column %d in TableModel \"%s\"", columnID.id, i, this.getClass()));
			columnIDIDs.add(columnID.id);
		}
	}

	private boolean exitsColumnWithUpdateEvent(ColumnID.Update event)
	{
		for (ColumnID columnID : originalColumns)
			if (columnID.cacheUpdateEvents.contains(event))
				return true;
		return false;
	}

	protected void clearCacheOfAllColumns()
	{
		for (ColumnID columnID : originalColumns)
			columnID.valueCache.clear();
	}

	protected void clearCacheOfColumns(ColumnID.Update event)
	{
		for (ColumnID columnID : originalColumns)
			if (columnID.cacheUpdateEvents.contains(event))
				columnID.valueCache.clear();
	}

	@Override public void prepareRemovingFromGUI() {
		finalizer.removeSubCompsAndListenersFromGUI();
	}

	static class Coloring<RowType> {

		private final Vector<Function<RowType,Color>> rowColorizersFG = new Vector<>();
		private final Vector<Function<RowType,Color>> rowColorizersBG = new Vector<>();
		
		private final HashMap<Class<?>,BiFunction<RowType,Object,Color>> classColorizersFG        = new HashMap<>();
		private final HashMap<Class<?>,BiFunction<RowType,Object,Color>> classColorizersBG        = new HashMap<>();
		private final HashMap<Class<?>,BiFunction<RowType,Object,Color>> classColorizersFGspecial = new HashMap<>();
		private final HashMap<Class<?>,BiFunction<RowType,Object,Color>> classColorizersBGspecial = new HashMap<>();
		
		private final HashMap<String,BiFunction<RowType,Object,Color>> columnColorizersFG        = new HashMap<>();
		private final HashMap<String,BiFunction<RowType,Object,Color>> columnColorizersBG        = new HashMap<>();
		private final HashMap<String,BiFunction<RowType,Object,Color>> columnColorizersFGspecial = new HashMap<>();
		private final HashMap<String,BiFunction<RowType,Object,Color>> columnColorizersBGspecial = new HashMap<>();
		
		private final HashSet<Integer> columnsWithActiveSpecialColoring = new HashSet<>();
		
		private final VerySimpleTableModel<RowType> tablemodel;
		private UserDefinedRowColorizer<RowType> getUserDefinedRowForeground = null;
		private UserDefinedRowColorizer<RowType> getUserDefinedRowBackground = null;
		
		interface UserDefinedRowColorizer<RowType>
		{
			Color getColor(RowType row, int rowIndex);
		}
		
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

		void setUserDefinedRowColorings(UserDefinedRowColorizer<RowType> getForeground, UserDefinedRowColorizer<RowType> getBackground)
		{
			this.getUserDefinedRowForeground = getForeground;
			this.getUserDefinedRowBackground = getBackground;
		}

		void addForegroundRowColorizer(Function<RowType,Color> colorizer) {
			if (colorizer==null) throw new IllegalArgumentException();
			rowColorizersFG.add(colorizer);
		}
		
		void addBackgroundRowColorizer(Function<RowType,Color> colorizer) {
			if (colorizer==null) throw new IllegalArgumentException();
			rowColorizersBG.add(colorizer);
		}
		
		void removeForegroundRowColorizer(Function<RowType,Color> colorizer) {
			if (colorizer==null) throw new IllegalArgumentException();
			rowColorizersFG.remove(colorizer);
		}
		
		void removeBackgroundRowColorizer(Function<RowType,Color> colorizer) {
			if (colorizer==null) throw new IllegalArgumentException();
			rowColorizersBG.remove(colorizer);
		}
		
		static <RowType> Function<RowType, Color> createOddEvenColorizer(Function<RowType, Integer> getIndex) {
			return row -> (getIndex.apply(row) & 1) == 1 ? COLOR_BG_ODD : COLOR_BG_EVEN;
		}
		
		private static <RowType, Type> BiFunction<RowType, Object, Color> convertClassColorizer(Class<Type> class_, BiFunction<RowType,Type,Color> getcolor) {
			return (row,obj)->!class_.isInstance(obj) ? null : getcolor.apply(row, class_.cast(obj));
		}

		<Type> void setForegroundColumnColoring(boolean isSpecial, Class<Type> class_, BiFunction<RowType,Type,Color> getcolor) {
			if (isSpecial) classColorizersFGspecial.put(class_, convertClassColorizer(class_, getcolor));
			else           classColorizersFG       .put(class_, convertClassColorizer(class_, getcolor));
		}
		<Type> void setBackgroundColumnColoring(boolean isSpecial, Class<Type> class_, BiFunction<RowType,Type,Color> getcolor) {
			if (isSpecial) classColorizersBGspecial.put(class_, convertClassColorizer(class_, getcolor));
			else           classColorizersBG       .put(class_, convertClassColorizer(class_, getcolor));
		}
		<Type> void setForegroundColumnColoring(boolean isSpecial, String columnID, Class<Type> class_, BiFunction<RowType,Type,Color> getcolor) {
			if (isSpecial) columnColorizersFGspecial.put(columnID, convertClassColorizer(class_, getcolor));
			else           columnColorizersFG       .put(columnID, convertClassColorizer(class_, getcolor));
		}
		<Type> void setBackgroundColumnColoring(boolean isSpecial, String columnID, Class<Type> class_, BiFunction<RowType,Type,Color> getcolor) {
			if (isSpecial) columnColorizersBGspecial.put(columnID, convertClassColorizer(class_, getcolor));
			else           columnColorizersBG       .put(columnID, convertClassColorizer(class_, getcolor));
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
			
			HashMap<String, BiFunction<RowType, Object, Color>> columnColorizersSpecial;  // prio 1
			HashMap<String, BiFunction<RowType, Object, Color>> columnColorizers;         // prio 3
			HashMap<Class<?>, BiFunction<RowType, Object, Color>> classColorizersSpecial; // prio 2
			HashMap<Class<?>, BiFunction<RowType, Object, Color>> classColorizers;        // prio 4
			Vector<Function<RowType,Color>> rowColorizers;  // prio 5
			Color defaultColor;  // prio 6
			UserDefinedRowColorizer<RowType> getUserDefinedRowColor;
			
			if (isForeground) {
				columnColorizersSpecial = columnColorizersFGspecial;
				columnColorizers        = columnColorizersFG;
				classColorizersSpecial = classColorizersFGspecial;
				classColorizers        = classColorizersFG;
				rowColorizers          = rowColorizersFG;
				getUserDefinedRowColor = getUserDefinedRowForeground;
				defaultColor = columnID.foreground;
			} else {
				columnColorizersSpecial = columnColorizersBGspecial;
				columnColorizers        = columnColorizersBG;
				classColorizersSpecial = classColorizersBGspecial;
				classColorizers        = classColorizersBG;
				rowColorizers          = rowColorizersBG;
				getUserDefinedRowColor = getUserDefinedRowBackground;
				defaultColor = columnID.background;
			}
			Color color = null;
			
			if (columnsWithActiveSpecialColoring.contains(columnM)) {
				BiFunction<RowType, Object, Color> specialColumnColorizer = columnColorizersSpecial.get(columnID.id);
				if (color==null && specialColumnColorizer!=null)
					color = specialColumnColorizer.apply(row, value);
				
				BiFunction<RowType, Object, Color> specialClassColorizer = classColorizersSpecial.get(columnID.config.columnClass);
				if (color==null && specialClassColorizer!=null)
					color = specialClassColorizer.apply(row, value);
			}
			
			BiFunction<RowType, Object, Color> columnColorizer = columnColorizers.get(columnID.id);
			if (color==null && columnColorizer!=null)
				color = columnColorizer.apply(row, value);
			
			BiFunction<RowType, Object, Color> classColorizer = classColorizers.get(columnID.config.columnClass);
			if (color==null && classColorizer!=null)
				color = classColorizer.apply(row, value);
			
			if (color==null && getUserDefinedRowColor!=null)
				color = getUserDefinedRowColor.getColor(row, rowM);
			
			if (color==null && !rowColorizers.isEmpty())
				color = getRowColor(rowColorizers,row);
			
			if (color==null) color = defaultColor;
			
			if (!isForeground && color==null && tablemodel.isCellEditable(rowM, columnM, columnID))
				color = COLOR_BG_EDITABLECELL;
			
			return color;
		}
	}
	
	protected void connectToGlobalData(Function<Data,Collection<RowType>> getData) {
		connectToGlobalData(false, getData);
	}
	protected void connectToGlobalData(boolean forwardNulls, Function<Data,Collection<RowType>> getData) {
		if (getData!=null)
			finalizer.addDataReceiver(data -> {
				clearCacheOfColumns(ColumnID.Update.Data);
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
		clearCacheOfAllColumns();
		if (rows!=null) {
			originalRows.addAll(rows);
			if (initialRowOrder != null)
				originalRows.sort(initialRowOrder);
			this.rows.addAll(RowFiltering.filterRows(originalRows, Arrays.asList(originalColumns), this::getValue));
		}
		extraUpdate();
		fireTableUpdate();
	}

	private class GeneralPurposeTCR implements TableCellRenderer {
		private final Tables.LabelRendererComponent    label;
		private final Tables.CheckBoxRendererComponent checkBox;
		private final Tables.ColorRendererComponent    colorComp;
		
		GeneralPurposeTCR() {
			label = new Tables.LabelRendererComponent();
			checkBox = new Tables.CheckBoxRendererComponent();
			checkBox.setHorizontalAlignment(SwingConstants.CENTER);
			colorComp = new Tables.ColorRendererComponent();
		}
		
		private enum RendComp { Label, CheckBox, Color }
		
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
			RendComp usedRendComp = RendComp.Label;
			
			if (columnID!=null) {
				
				if (columnID.format!=null && value!=null)
					valueStr = String.format(Locale.ENGLISH, columnID.format, value);
				
				if (columnID.horizontalAlignment!=null)
					label.setHorizontalAlignment(columnID.horizontalAlignment);
				
				else if (Number.class.isAssignableFrom(columnID.config.columnClass))
					label.setHorizontalAlignment(SwingConstants.RIGHT);
				
				else
					label.setHorizontalAlignment(SwingConstants.LEFT);
				
				if (columnID.config.columnClass == Color[].class && value instanceof Color[]) {
					usedRendComp = RendComp.Color;
				}
				if (columnID.config.columnClass == Color.class && value instanceof Color) {
					usedRendComp = RendComp.Color;
				}
				
				if (columnID.config.columnClass == Boolean.class) {
					checkBox.setHorizontalAlignment(SwingConstants.CENTER);
					valueStr = null;
					if (value instanceof Boolean) {
						Boolean b = (Boolean) value;
						isChecked = b.booleanValue();
						usedRendComp = RendComp.CheckBox;
					}
				}
				
				if (Data.BooleanWithText.class.isAssignableFrom(columnID.config.columnClass)) {
					checkBox.setHorizontalAlignment(SwingConstants.LEFT);
					if (value instanceof Data.BooleanWithText) {
						Data.BooleanWithText bwt = (Data.BooleanWithText) value;
						isChecked = bwt.getBool();
						valueStr  = bwt.getText();
						usedRendComp = RendComp.CheckBox;
						
					} else {
						valueStr = null;
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
			
			switch (usedRendComp)
			{
				default:
				case Label:
					label    .configureAsTableCellRendererComponent(table, null     , valueStr, isSelected, hasFocus,       getCustomBackground, getCustomForeground);
					return label;
					
				case CheckBox:
					checkBox .configureAsTableCellRendererComponent(table, isChecked, valueStr, isSelected, hasFocus,       getCustomForeground, getCustomBackground);
					return checkBox;
					
				case Color:
					colorComp.configureAsTableCellRendererComponent(table,               value, isSelected, hasFocus, null, getCustomBackground, getCustomForeground);
					return colorComp;
			}
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
			ColumnHiding.ColumnHideDialog dlg = new ColumnHiding.ColumnHideDialog(mainWindow, originalColumns, tableModelID);
			boolean changed = dlg.showDialog();
			if (changed) {
				super.columns = ColumnHiding.removeHiddenColumns(originalColumns);
				fireTableStructureUpdate();
				reconfigureAfterTableStructureUpdate();
			}
		}));
		
		contextMenu.add(SnowRunner.createMenuItem("Filter Rows ...", true, e->{
			RowFiltering.FilterRowsDialog dlg = new RowFiltering.FilterRowsDialog(mainWindow, originalColumns, tableModelID);
			boolean changed = dlg.showDialog();
			if (changed) {
				rows.clear();
				clearCacheOfAllColumns();
				this.rows.addAll(RowFiltering.filterRows(originalRows, Arrays.asList(originalColumns), this::getValue));
				fireTableStructureUpdate();
				reconfigureAfterTableStructureUpdate();
			}
		}));
		
		contextMenu.addSeparator();
		
		HashSet<String> activeColorings = new HashSet<>();
		if (tableModelInstanceID!=null)
			RowColoring.activeColoringsStorage.get(tableModelID, tableModelInstanceID, activeColorings);
		JMenu rowColoringsMenu = new JMenu("Row Colorings");
		contextMenu.add(rowColoringsMenu);
		RowColoring.rebuildMenu(this, rowColoringsMenu, activeColorings, table);
		RowColoring.updateColoring(this, activeColorings);
		
		contextMenu.add(SnowRunner.createMenuItem("Configure Row Colorings ...", true, e->{
			RowColoring.ConfigureDialog dlg = new RowColoring.ConfigureDialog(mainWindow, originalColumns, tableModelID);
			dlg.showDialog();
			RowColoring.rebuildMenu(this, rowColoringsMenu, activeColorings, table);
			RowColoring.updateColoring(this, activeColorings);
			table.repaint();
		}));
		
		contextMenu.addSeparator();
		
		JMenuItem miShowCalculationPath = contextMenu.add(SnowRunner.createMenuItem("Show calculation path", true, e->{
			if (clickedColumn==null) return;
			if (clickedRow   ==null) return;
			boolean writeToConsole = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.Tables_WriteCalculationDetailsToConsole, true);
			if (writeToConsole)
				clickedColumn.showValueComputation(clickedRow, language, this, new TextOutput.SystemOut());
			else
			{
				TextOutput.Collector textOutput = new TextOutput.Collector();
				clickedColumn.showValueComputation(clickedRow, language, this, textOutput);
				TextAreaDialog.showText(mainWindow, "Calculation Details", 300, 400, false, false, textOutput.toString(), null, dialog -> {
					SnowRunner.settings.registerExtraWindow(dialog,
						SnowRunner.AppSettings.ValueKey.Tables_CalcDetailsDialog_WindowX,
						SnowRunner.AppSettings.ValueKey.Tables_CalcDetailsDialog_WindowY,
						SnowRunner.AppSettings.ValueKey.Tables_CalcDetailsDialog_WindowWidth,
						SnowRunner.AppSettings.ValueKey.Tables_CalcDetailsDialog_WindowHeight,
						300, 400
					);
				});
			}
		}));
		
		contextMenu.addContextMenuInvokeListener((table_, x, y) -> {
			Point p = x==null || y==null ? null : new Point(x,y);
			int colV = p==null ? -1 : table_.columnAtPoint(p);
			clickedColumnIndex = colV<0 ? -1 : table_.convertColumnIndexToModel(colV);
			clickedColumn = clickedColumnIndex<0 ? null : super.columns[clickedColumnIndex];
			
			int rowV = p==null ? -1 : table_.rowAtPoint(p);
			clickedRowIndex = rowV<0 ? -1 : table_.convertRowIndexToModel(rowV);
			clickedRow = clickedRowIndex<0 ? null : getRow(clickedRowIndex);
			
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
			
			miShowCalculationPath.setEnabled(clickedColumn!=null && clickedColumn.hasShowValueComputationFcn());
			
			String columnLabel = null;
			if (columnLabel==null && clickedColumn!=null && !clickedColumn.config.name.isBlank())
				columnLabel = "field \""+clickedColumn.config.name+"\"";
			if (columnLabel==null && clickedColumnIndex>=0)
				columnLabel = "field "+clickedColumnIndex;
			if (columnLabel==null)
				columnLabel = "field";
			
			String rowLabel = null;
			if (rowLabel==null && clickedRow!=null)
			{
				String rowName = getRowName(clickedRow);
				if (rowName!=null && !rowName.isBlank())
					rowLabel = "row \""+rowName+"\"";
			}
			if (rowLabel==null && clickedRowIndex>=0)
				rowLabel = "row "+clickedRowIndex;
			if (rowLabel==null)
				rowLabel = "row";
				
			miShowCalculationPath.setText( String.format( "Show calculation details of %s of %s", columnLabel, rowLabel ) );
		});
	}
	
	protected RowType getRowAt(JTable table, Integer x, Integer y)
	{
		Point p = x==null || y==null ? null : new Point(x,y);
		int rowV = p==null ? -1 : table.rowAtPoint(p);
		int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
		return rowM<0 ? null : getRow(rowM);
	}

	@Override public int getRowCount() {
		return rows.size();
	}

	public RowType getRow(int rowIndex) {
		if (rowIndex<0 || rowIndex>=rows.size()) return null;
		return rows.get(rowIndex);
	}

	protected abstract String getRowName(RowType row);

	@Override public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
		RowType row = getRow(rowIndex);
		if (row==null) return null;
		return getValue(columnID, rowIndex, row);
	}
	
	Object getValue(ColumnID columnID, int rowIndex, RowType row) {
		return columnID.getValue(rowIndex, row, language, this);
	}
	
	protected void fireTableColumnUpdate(String id)
	{
		int colM = findColumnByID("DLC");
		if (colM>=0) fireTableColumnUpdate(colM);
	}
	
	protected int findColumnByID(String id) {
		if (id==null) throw new IllegalArgumentException();
		for (int i=0; i<super.columns.length; i++)
			if (id.equals(((ColumnID)super.columns[i]).id))
				return i;
		return -1;
	}
	
	protected ColumnID getColumnID(String id)
	{
		for (ColumnID columnID : originalColumns)
			if (columnID.id.equals(id))
				return columnID;
		return null;
	}

	@SuppressWarnings("unused")
	public static void initializePresetMaps()
	{
		Object x;
		x = ColumnHiding.presets;
		x = RowFiltering.presets;
		x = RowColoring.coloringsMap;
		x = RowColoring.activeColoringsStorage;
	}

	private static class RowColoring
	{
		static final RowColoringsMap coloringsMap = new RowColoringsMap();
		static final ActiveColoringsStorage activeColoringsStorage = new ActiveColoringsStorage();
		
		static <RowType> void rebuildMenu(
				VerySimpleTableModel<RowType> tableModel,
				JMenu rowColoringsMenu,
				HashSet<String> activeColorings,
				JTable table
		)
		{
			rowColoringsMenu.removeAll();
			
			HashMap<String, RowColoringPreset> presets = coloringsMap.getModelPresets(tableModel.tableModelID);
			for (String name : SnowRunner.getSorted(presets.keySet()))
				rowColoringsMenu.add(SnowRunner.createCheckBoxMenuItem(name, activeColorings.contains(name), null, true, b -> {
					if (b) activeColorings.add(name);
					else   activeColorings.remove(name);
					
					if (tableModel.tableModelInstanceID!=null)
						RowColoring.activeColoringsStorage.set(tableModel.tableModelID, tableModel.tableModelInstanceID, activeColorings);
					
					updateColoring(tableModel, activeColorings);
					table.repaint();
				}));
		}

		static <RowType> void updateColoring(VerySimpleTableModel<RowType> tableModel, HashSet<String> activeColorings)
		{
			HashMap<String, RowColoringPreset> presets = coloringsMap.getModelPresets(tableModel.tableModelID);
			Coloring.UserDefinedRowColorizer<RowType> getForeground = generateRowColoringFunction(presets, activeColorings, preset -> preset.foreground, tableModel::getValue, tableModel::getColumnID);
			Coloring.UserDefinedRowColorizer<RowType> getBackground = generateRowColoringFunction(presets, activeColorings, preset -> preset.background, tableModel::getValue, tableModel::getColumnID);
			tableModel.coloring.setUserDefinedRowColorings(getForeground, getBackground);
		}
		
		private static <RowType> Coloring.UserDefinedRowColorizer<RowType>
			generateRowColoringFunction(
				HashMap<String, RowColoringPreset> presets,
				HashSet<String> activeColorings,
				Function<RowColoringPreset,Color> getColor,
				RowFiltering.GetValue<RowType> getValue,
				Function<String, ColumnID> getColumnID
		)
		{
			Vector<RowColorizer> activeColorizers = new Vector<>();
			for (String name : activeColorings)
			{
				RowColoringPreset preset = presets.get(name);
				Color color = getColor.apply(preset);
				if (color!=null)
					activeColorizers.add(new RowColorizer(preset.filterMap, preset.orderIndex, color, getColumnID));
			}
			activeColorizers.sort(Comparator.<RowColorizer,Integer>comparing(rc -> rc.orderIndex));
			
			return (row, rowIndex) -> {
				for (RowColorizer rc : activeColorizers)
				{
					boolean meetsFilter = RowFiltering.meetsFilter(rc.filters, columnID -> getValue.getValue(columnID, rowIndex, row));
					if (meetsFilter) return rc.color;
				}
				return null;
			};
		}

		private static class RowColorizer
		{
			private final Color color;
			private final int orderIndex;
			private final Vector<RowFiltering.FixedValueFilterContainer.Pair> filters;

			RowColorizer(HashMap<String,RowFiltering.ValueFilter> filterMap, int orderIndex, Color color, Function<String,ColumnID> getColumnID)
			{
				this.color = color;
				this.orderIndex = orderIndex;
				this.filters = new Vector<RowFiltering.FixedValueFilterContainer.Pair>();
				filterMap.forEach((id,filter) -> {
					ColumnID columnID = getColumnID.apply(id);
					filters.add(new RowFiltering.FixedValueFilterContainer.Pair(columnID, filter));
				});
			}
		}

		static class ConfigureDialog extends JDialog
		{
			private static final long serialVersionUID = 3994955199504822407L;

			private static HSColorChooser.UserdefinedColors userdefinedColors = new HSColorChooser.UserdefinedColors();
			
			private final Window owner;
			private final ColumnID[] originalColumns;
			private final HashMap<String, RowColoringPreset> tableColorings;
			private final RowFiltering.RowFilterPanel rowFilterPanel;
			private final JTable coloringsTable;
			private final ColoringsTableModel coloringsTableModel;
			private final ColoringsTableToolBar coloringsTableToolBar;
			private final FilterListToolBar filterListToolBar;
			private boolean saveChangesAutomatically;
			private boolean rowFilterPanelHasChanges;
			private int selectedRowM;
			private ColoringsTableModel.Row selectedPreset;

			ConfigureDialog(Window owner, ColumnID[] originalColumns, String tableModelID)
			{
				super(owner, "Configure RowColorings", ModalityType.APPLICATION_MODAL);
				this.owner = owner;
				this.originalColumns = originalColumns;
				tableColorings = coloringsMap.getModelPresets(tableModelID);
				saveChangesAutomatically = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.Tables_ConfigureRowColoringDialog_SaveChangesAutomatically, false);
				selectedRowM = -1;
				selectedPreset = null;
				
				rowFilterPanel = new RowFiltering.RowFilterPanel(this.originalColumns, true, ()->{
					rowFilterPanelHasChanges = true;
					updateButtons();
				});
				rowFilterPanel.setEnabled(false);
				
				JScrollPane filterListScrollPane = new JScrollPane(rowFilterPanel.panel);
				filterListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				filterListScrollPane.setPreferredSize(new Dimension(600,700));
				filterListScrollPane.getVerticalScrollBar().setUnitIncrement(10);
				
				coloringsTableModel = new ColoringsTableModel(tableColorings, coloringsMap::write);
				coloringsTable = new JTable(coloringsTableModel);
				JScrollPane coloringsTableScrollPane = new JScrollPane(coloringsTable);
				
				coloringsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				coloringsTableModel.setTable(coloringsTable);
				coloringsTableModel.setColumnWidths(coloringsTable);
				coloringsTableModel.setCellRenderer(ColoringsTableModel.ColumnID.Colors, new ExampleCellRenderer(coloringsTableModel));
				coloringsTableScrollPane.setPreferredSize(new Dimension(300,100));
				coloringsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				coloringsTable.getSelectionModel().addListSelectionListener(e -> {
					if (saveChangesAutomatically && rowFilterPanelHasChanges) saveChanges();
					int rowV = coloringsTable.getSelectedRow();
					selectedRowM = rowV<0 ? -1 : coloringsTable.convertRowIndexToModel(rowV);
					selectedPreset = selectedRowM<0 ? null : coloringsTableModel.getRow(selectedRowM);
					rowFilterPanel.setPresetInGui(selectedPreset==null ? null : selectedPreset.preset.filterMap);
					rowFilterPanel.setEnabled(selectedPreset!=null);
					rowFilterPanelHasChanges = false;
					updateButtons();
				});
				
				JPanel topPanel = new JPanel(new BorderLayout());
				topPanel.add(coloringsTableToolBar = new ColoringsTableToolBar(), BorderLayout.PAGE_START);
				topPanel.add(coloringsTableScrollPane, BorderLayout.CENTER);
				
				JPanel bottomPanel = new JPanel(new BorderLayout());
				bottomPanel.add(filterListToolBar = new FilterListToolBar(), BorderLayout.PAGE_START);
				bottomPanel.add(filterListScrollPane, BorderLayout.CENTER);
				
				JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
				splitPane.setTopComponent(topPanel);
				splitPane.setBottomComponent(bottomPanel);
				
				JPanel dlgButtonPanel = new JPanel(new GridBagLayout());
				dlgButtonPanel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1;
				dlgButtonPanel.add(new JLabel(),c);
				c.weightx = 0;
				dlgButtonPanel.add(SnowRunner.createButton("Close", true, e->{
					if (saveChangesAutomatically && rowFilterPanelHasChanges) saveChanges();
					setVisible(false);
				}),c);
				
				JPanel contentPane = new JPanel(new BorderLayout());
				contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
				contentPane.add(splitPane, BorderLayout.CENTER);
				contentPane.add(dlgButtonPanel, BorderLayout.SOUTH);
				
				setContentPane(contentPane);
				updateButtons();
				
				addWindowListener(new WindowAdapter() {
					@Override public void windowClosing(WindowEvent e)
					{
						if (saveChangesAutomatically && rowFilterPanelHasChanges) saveChanges();
					}
				});
			}
			
			private class ColoringsTableToolBar extends JToolBar
			{
				private static final long serialVersionUID = -638019696789518107L;
				
				private final JButton btnAdd;
				private final JButton btnCopy;
				private final JButton btnRemove;
				private final JButton btnMoveUp;
				private final JButton btnMoveDown;
				private final JButton btnSetForeground;
				private final JButton btnSetBackground;
				private final JButton btnRemoveForeground;
				private final JButton btnRemoveBackground;
				
				ColoringsTableToolBar()
				{
					setFloatable(false);
					
					add(btnAdd = SnowRunner.createButton("Add", true, GrayCommandIcons.Add.getIcon(), GrayCommandIcons.Add_Dis.getIcon(), e->{
						addPreset(new RowColoringPreset());
						updateButtons();
					}));
					add(btnCopy = SnowRunner.createButton("Copy", false, GrayCommandIcons.Copy.getIcon(), GrayCommandIcons.Copy_Dis.getIcon(), e->{
						if (selectedPreset!=null)
						{
							addPreset(new RowColoringPreset(selectedPreset.preset));
							updateButtons();
						}
					}));
					add(btnRemove = SnowRunner.createButton("Remove", false, GrayCommandIcons.Delete.getIcon(), GrayCommandIcons.Delete_Dis.getIcon(), e->{
						if (selectedPreset!=null)
						{
							tableColorings.remove(selectedPreset.name);
							coloringsTableModel.removeRow(selectedPreset, true);
							coloringsTable.clearSelection();
							coloringsMap.write();
							updateButtons();
						}
					}));
					
					addSeparator();
					
					add(btnMoveUp = SnowRunner.createButton("Move Up", true, GrayCommandIcons.Up.getIcon(), GrayCommandIcons.Up_Dis.getIcon(), e->{
						if (selectedRowM > 0)
						{
							coloringsTableModel.moveRowUp(selectedRowM, true);
							selectedRowM--;
							coloringsTable.setRowSelectionInterval(selectedRowM, selectedRowM);
							coloringsMap.write();
							updateButtons();
						}
					}));
					add(btnMoveDown = SnowRunner.createButton("Move Down", true, GrayCommandIcons.Down.getIcon(), GrayCommandIcons.Down_Dis.getIcon(), e->{
						if (selectedRowM < coloringsTableModel.getRowCount()-1)
						{
							coloringsTableModel.moveRowDown(selectedRowM, true);
							selectedRowM++;
							coloringsTable.setRowSelectionInterval(selectedRowM, selectedRowM);
							coloringsMap.write();
							updateButtons();
						}
					}));
					
					addSeparator();
					
					add(btnSetForeground = SnowRunner.createButton("Set Foreground", true, e->{
						if (selectedPreset!=null)
							changeColor(
								"Foreground",
								selectedPreset.name,
								selectedPreset.preset.foreground,
								coloringsTable.getForeground(),
								c -> selectedPreset.preset.foreground = c
							);
					}));
					
					add(btnSetBackground = SnowRunner.createButton("Set Background", true, e->{
						if (selectedPreset!=null)
							changeColor(
								"Background",
								selectedPreset.name,
								selectedPreset.preset.background,
								coloringsTable.getBackground(),
								c -> selectedPreset.preset.background = c
							);
					}));
					
					add(btnRemoveForeground = SnowRunner.createButton("Foreground", true, GrayCommandIcons.Delete.getIcon(), GrayCommandIcons.Delete_Dis.getIcon(), e->{
						if (selectedPreset!=null)
						{
							selectedPreset.preset.foreground = null;
							updateAfterColorChange();
						}
					}));
					
					add(btnRemoveBackground = SnowRunner.createButton("Background", true, GrayCommandIcons.Delete.getIcon(), GrayCommandIcons.Delete_Dis.getIcon(), e->{
						if (selectedPreset!=null)
						{
							selectedPreset.preset.background = null;
							updateAfterColorChange();
						}
					}));
				}

				private void changeColor(String colorLabel, String presetName, Color initialValue, Color defaultinitialValue, Consumer<Color> setValue)
				{
					Color newColor = HSColorChooser.showDialog(
							ConfigureDialog.this,
							String.format("%s Color of \"%s\"", colorLabel, presetName),
							initialValue==null ? defaultinitialValue : initialValue,
							userdefinedColors,
							StandardDialog.Position.PARENT_CENTER
					);
					if (newColor!=null)
					{
						setValue.accept(newColor);
						updateAfterColorChange();
					}
				}

				private void updateAfterColorChange()
				{
					coloringsTableModel.fireTableColumnUpdate(ColoringsTableModel.ColumnID.Colors);
					coloringsMap.write();
					updateButtons();
				}

				void updateToolbarButtons()
				{
					btnAdd             .setEnabled(true);
					btnCopy            .setEnabled(selectedPreset!=null);
					btnRemove          .setEnabled(selectedPreset!=null);
					btnMoveUp          .setEnabled(selectedRowM >  0 && selectedRowM < coloringsTableModel.getRowCount()  );
					btnMoveDown        .setEnabled(selectedRowM >= 0 && selectedRowM < coloringsTableModel.getRowCount()-1);
					btnSetForeground   .setEnabled(selectedPreset!=null);
					btnSetBackground   .setEnabled(selectedPreset!=null);
					btnRemoveForeground.setEnabled(selectedPreset!=null && selectedPreset.preset.foreground!=null);
					btnRemoveBackground.setEnabled(selectedPreset!=null && selectedPreset.preset.background!=null);
				}
			}
			
			private class FilterListToolBar extends JToolBar
			{
				private static final long serialVersionUID = 1058473217213473561L;
				
				private final JButton btnSave;
				private final JCheckBox chkbxSaveAutomatically;
				
				FilterListToolBar()
				{
					setFloatable(false);
					
					add(btnSave = SnowRunner.createButton("Save Changes", false, GrayCommandIcons.Save.getIcon(), GrayCommandIcons.Save_Dis.getIcon(), e->{
						saveChanges();
						updateToolbarButtons();
					}));
					
					add(chkbxSaveAutomatically = SnowRunner.createCheckBox("Save Changes Automatically", saveChangesAutomatically, null, true, b->{
						saveChangesAutomatically = b;
						SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.Tables_ConfigureRowColoringDialog_SaveChangesAutomatically, saveChangesAutomatically);
						if (saveChangesAutomatically && rowFilterPanelHasChanges)
							saveChanges();
						updateToolbarButtons();
					}));
				}

				void updateToolbarButtons()
				{
					btnSave.setEnabled(selectedPreset!=null && !saveChangesAutomatically && rowFilterPanelHasChanges);
					chkbxSaveAutomatically.setEnabled(true);
				}
			}

			private void saveChanges()
			{
				if (selectedPreset!=null && rowFilterPanelHasChanges)
				{
					HashMap<String, RowFiltering.ValueFilter> filterMap = selectedPreset.preset.filterMap;
					filterMap.clear();
					filterMap.putAll(rowFilterPanel.getCopyOfCurrentPreset());
					coloringsMap.write();
					rowFilterPanelHasChanges = false;
				}
			}

			private void updateButtons()
			{
				coloringsTableToolBar.updateToolbarButtons();
				filterListToolBar.updateToolbarButtons();
			}

			private void addPreset(RowColoringPreset preset)
			{
				String name = askUserForNewName();
				if (name==null) return;
				tableColorings.put(name, preset);
				coloringsTableModel.addRow(name, preset, true);
				coloringsMap.write();
			}
			
			private String askUserForNewName()
			{
				String title1 = "Enter Name";
				String msg1 = "Enter a new name: ";
				
				while (true)
				{
					String newName = JOptionPane.showInputDialog(this, msg1, title1, JOptionPane.QUESTION_MESSAGE);
					if (newName==null) return null;
					if (!tableColorings.keySet().contains(newName)) return newName;
					
					String title2 = "Name already used";
					Object msg2 = String.format("Name \"%s\" is already in use.", newName);
					JOptionPane.showMessageDialog(this, msg2, title2, JOptionPane.ERROR_MESSAGE);
				}
			}

			void showDialog() {
				pack();
				setLocationRelativeTo(owner);
				setVisible(true);
			}
			
			private static class ExampleCellRenderer implements TableCellRenderer
			{
				private final Tables.LabelRendererComponent rendComp;
				private final ColoringsTableModel tableModel;

				ExampleCellRenderer(ColoringsTableModel tableModel)
				{
					this.tableModel = tableModel;
					rendComp = new Tables.LabelRendererComponent();
				}

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV)
				{
					String valuestr = value==null ? null : value.toString();
					
					int rowM = table.convertRowIndexToModel(rowV);
					ColoringsTableModel.Row row = tableModel.getRow(rowM);
					
					Supplier<Color> getCustomBackground = () -> row==null ? null : row.preset.background;
					Supplier<Color> getCustomForeground = () -> row==null ? null : row.preset.foreground;
					rendComp.configureAsTableCellRendererComponent(table, null, valuestr, isSelected, hasFocus, getCustomBackground, getCustomForeground);
					
					return rendComp;
				}
				
			}
			
			static class ColoringsTableModel extends Tables.SimplifiedTableModel<ColoringsTableModel.ColumnID>
			{
				enum ColumnID implements Tables.SimplifiedColumnIDInterface
				{
					Name  ( "Name"  , String.class, 150),
					Colors( "Colors", String.class, 150),
					;
					private final Tables.SimplifiedColumnConfig config;
					ColumnID(String name, Class<?> columnClass, int width)
					{
						config = new Tables.SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
					}
					@Override public Tables.SimplifiedColumnConfig getColumnConfig() { return config; }
				}
				
				private final Vector<Row> data;

				ColoringsTableModel(HashMap<String, RowColoringPreset> tableColorings, Runnable writeColoringsToFile)
				{
					super(ColumnID.values());
					data = new Vector<>();
					tableColorings.forEach((name, preset) -> data.add(new Row(name, preset)));
					data.sort(Comparator.<Row,Integer>comparing(row -> row.preset.orderIndex).thenComparing(row -> row.name));
					
					boolean orderIndexesChanged = fixOrderIndexes(data);
					if (orderIndexesChanged)
						writeColoringsToFile.run();
				}

				void moveRowUp  (int rowIndex, boolean fixOrderIndex) { swapRows(rowIndex, rowIndex-1, fixOrderIndex); }
				void moveRowDown(int rowIndex, boolean fixOrderIndex) { swapRows(rowIndex, rowIndex+1, fixOrderIndex); }

				private void swapRows(int rowIndex1, int rowIndex2, boolean fixOrderIndex)
				{
					Row row1 = getRow(rowIndex1);
					Row row2 = getRow(rowIndex2);
					if (row1==null || row2==null) return;
					
					data.set(rowIndex1, row2);
					data.set(rowIndex2, row1);
					
					if (fixOrderIndex)
					{
						row1.preset.orderIndex = rowIndex2+1;
						row2.preset.orderIndex = rowIndex1+1;
					}
				}

				private static boolean fixOrderIndexes(Vector<Row> data)
				{
					boolean changed = false;
					for (int i=0; i<data.size(); i++)
					{
						RowColoringPreset preset = data.get(i).preset;
						if (preset.orderIndex != i+1)
						{
							changed = true;
							preset.orderIndex = i+1;
						}
					}
					return changed;
				}

				void addRow(String name, RowColoringPreset preset, boolean fixOrderIndex)
				{
					data.add(new Row(name, preset));
					if (fixOrderIndex)
						preset.orderIndex = data.size();
					fireTableRowAdded(data.size()-1);
				}

				boolean removeRow(Row preset, boolean fixOrderIndexes)
				{
					int rowIndex = data.indexOf(preset);
					if (rowIndex<0) return false;
					
					data.remove(rowIndex);
					if (fixOrderIndexes)
						fixOrderIndexes(data);
					
					fireTableRowRemoved(rowIndex);
					return true;
				}

				record Row(String name, RowColoringPreset preset) {}

				@Override public int getRowCount() { return data.size(); }
				
				Row getRow(int rowIndex)
				{
					if (rowIndex<0) return null;
					if (rowIndex>=data.size()) return null;
					return data.get(rowIndex);
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID)
				{
					Row row = getRow(rowIndex);
					if (row==null) return null;
					
					switch (columnID)
					{
						case Name : return row.name;
						case Colors: return String.format("%s / %s", toDisplayString(row.preset.foreground), toDisplayString(row.preset.background));
					}
					return null;
				}

				private String toDisplayString(Color color)
				{
					if (color==null) return "-------";
					return String.format("#%06X", color.getRGB() & 0xFFFFFF);
				}
			}
		}

		static class RowColoringPreset
		{
			private final HashMap<String,RowFiltering.ValueFilter> filterMap;
			private int orderIndex;
			private Color foreground;
			private Color background;
			
			RowColoringPreset()
			{
				filterMap = new HashMap<>();
				orderIndex = 0;
				foreground = null;
				background = null;
			}
			
			RowColoringPreset(RowColoringPreset other)
			{
				filterMap = deepCopyOf(other.filterMap);
				orderIndex = other.orderIndex;
				foreground = other.foreground;
				background = other.background;
			}
			
			private static HashMap<String, RowFiltering.ValueFilter> deepCopyOf(HashMap<String, RowFiltering.ValueFilter> filterMap)
			{
				HashMap<String, RowFiltering.ValueFilter> newMap = new HashMap<>();
				filterMap.forEach((id, filter) -> newMap.put(id, filter.clone()));
				return newMap;
			}

			void parseLine(String line)
			{
				String valueStr;
				if ( (valueStr=Data.getLineValue(line, "OrderIndex="))!=null ) orderIndex = parseInt(valueStr, Integer.MAX_VALUE);
				if ( (valueStr=Data.getLineValue(line, "Foreground="))!=null ) foreground = parseColor(valueStr);
				if ( (valueStr=Data.getLineValue(line, "Background="))!=null ) background = parseColor(valueStr);
				
				RowFiltering.parseFilterLine(line, filterMap);
			}

			private static int parseInt(String valueStr, int defaultValue)
			{
				try { return Integer.parseInt(valueStr); }
				catch (NumberFormatException e) { return defaultValue; }
			}

			private static Color parseColor(String valueStr)
			{
				try { return new Color(Integer.parseInt(valueStr, 16) & 0xFFFFFF); }
				catch (NumberFormatException e) { return null; }
			}

			void writeLines(PrintWriter out)
			{
				out.printf("OrderIndex=%d%n", orderIndex);
				if (foreground!=null) out.printf("Foreground=%06X%n", foreground.getRGB() & 0xFFFFFF);
				if (background!=null) out.printf("Background=%06X%n", background.getRGB() & 0xFFFFFF);
				RowFiltering.writeFilterLines(filterMap, out);
			}
		}

		static class RowColoringsMap extends PresetMaps<RowColoringPreset>
		{
			RowColoringsMap()
			{
				super("RowColoringsMap", SnowRunner.RowColoringsFile, RowColoringPreset::new);
			}
		
			@Override protected void parseLine(String line, RowColoringPreset preset)
			{
				preset.parseLine(line);
			}
		
			@Override protected void writePresetInLines(RowColoringPreset preset, PrintWriter out)
			{
				preset.writeLines(out);
			}
		}

		static class ActiveColoringsStorage
		{
			private static final SnowRunner.AppSettings.ValueKey settingsKey = SnowRunner.AppSettings.ValueKey.Tables_RowColorings_Active;
			private final HashMap<String, HashMap<String, HashSet<String>>> storage;
			
			ActiveColoringsStorage()
			{
				storage = new HashMap<>();
				readFromAppSettings();
			}

			void get(String tableModelID, String tableModelInstanceID, HashSet<String> activeColorings)
			{
				HashMap<String, HashSet<String>> instanceMap = storage.get(tableModelID);
				if (instanceMap==null) return;
				
				HashSet<String> storedActiveColorings = instanceMap.get(tableModelInstanceID);
				if (storedActiveColorings==null) return;
				
				activeColorings.clear();
				activeColorings.addAll(storedActiveColorings);
			}

			void set(String tableModelID, String tableModelInstanceID, HashSet<String> activeColorings)
			{
				HashMap<String, HashSet<String>> instanceMap = storage.get(tableModelID);
				if (instanceMap==null) storage.put(tableModelID, instanceMap = new HashMap<>());
				
				HashSet<String> storedActiveColorings = instanceMap.get(tableModelInstanceID);
				if (storedActiveColorings==null) instanceMap.put(tableModelInstanceID, storedActiveColorings = new HashSet<>());
				
				storedActiveColorings.clear();
				storedActiveColorings.addAll(activeColorings);
				
				writeToAppSettings();
			}

			private void readFromAppSettings()
			{
				String storageContent = SnowRunner.settings.getString(settingsKey, "");
				storage.clear();
				
				try (BufferedReader in = new BufferedReader(new StringReader(storageContent)))
				{
					String line, valueStr;
					HashMap<String, HashSet<String>> instanceMap = null;
					HashSet<String> storedActiveColorings = null;
					while ( (line = in.readLine())!=null )
					{
						if (line.equals("[ActiveColorings]") || line.isEmpty())
						{
							instanceMap = null;
							storedActiveColorings = null;
						}
						
						if ( (valueStr=Data.getLineValue(line, "TableModel="))!=null )
						{
							instanceMap = SnowRunner.getOrCreate(storage, valueStr, HashMap<String, HashSet<String>>::new);
							storedActiveColorings = null;
						}
						
						if ( (valueStr=Data.getLineValue(line, "Instance="))!=null && instanceMap!=null)
							storedActiveColorings = SnowRunner.getOrCreate(instanceMap, valueStr, HashSet<String>::new);
						
						if ( (valueStr=Data.getLineValue(line, "Active="))!=null && storedActiveColorings!=null)
							storedActiveColorings.add(valueStr);
					}
				}
				catch (IOException e)
				{
					System.err.printf("%s while reading ActiveColoringsStorage from SnowRunner.AppSettings: %s%n", e.getClass().getName(), e.getMessage());
					//e.printStackTrace();
				}
			}

			private void writeToAppSettings()
			{
				StringWriter stringWriter = new StringWriter();
				try (PrintWriter out = new PrintWriter(stringWriter))
				{
					SnowRunner.forEachSortedByKey(storage, (tableModelID, instanceMap) -> {
						SnowRunner.forEachSortedByKey(instanceMap, (tableModelInstanceID, storedActiveColorings) -> {
							
							out.printf("[ActiveColorings]%n");
							out.printf("TableModel=%s%n", tableModelID);
							out.printf("Instance=%s%n", tableModelInstanceID);
							for (String activePreset : SnowRunner.getSorted(storedActiveColorings))
								out.printf("Active=%s%n", activePreset);
							out.printf("%n");
							
						});
					});
					out.flush();
				}
				
				String storageContent = stringWriter.toString();
				SnowRunner.settings.putString(settingsKey, storageContent);
			}
		}
	}
	
	
	private static class RowFiltering
	{
		static final Presets presets = new Presets();

		static class Presets extends PresetMaps<HashMap<String,ValueFilter>> {
		
			Presets() {
				super("FilterRowsPresets", SnowRunner.FilterRowsPresetsFile, HashMap<String,ValueFilter>::new);
			}
		
			@Override protected void parseLine(String line, HashMap<String, ValueFilter> preset) {
				RowFiltering.parseFilterLine(line, preset);
			}
		
			@Override protected void writePresetInLines(HashMap<String, ValueFilter> preset, PrintWriter out) {
				RowFiltering.writeFilterLines(preset, out);
			}
			
		}

		static void parseFilterLine(String line, HashMap<String, ValueFilter> preset)
		{
			String valueStr;
			if ( (valueStr=Data.getLineValue(line, "Filter="))!=null ) {
				int endOfKey = valueStr.indexOf(':');
				if (endOfKey<0) {
					System.err.printf("Can't parse filter in line for FilterRowsPresets: \"%s\"%n", valueStr);
					return;
				}
				String key = valueStr.substring(0, endOfKey);
				String filterStr = valueStr.substring(endOfKey+1);
				ValueFilter filter = ValueFilter.parse( filterStr );
				if (filter!=null) preset.put(key, filter);
				else System.err.printf("Can't parse filter in line for FilterRowsPresets: \"%s\"%n", valueStr);
			}
		}

		static void writeFilterLines(HashMap<String, ValueFilter> filterMap, PrintWriter out)
		{
			Vector<String> keys = new Vector<>(filterMap.keySet());
			keys.sort(null);
			for (String key : keys) {
				ValueFilter filter = filterMap.get(key);
				out.printf("Filter=%s:%s%n", key, filter.toParsableString());
			}
		}

		interface GetValue<RowType>
		{
			Object getValue(ColumnID columnID, int rowIndex, RowType row);
		}

		static <RowType> Vector<RowType> filterRows(Vector<RowType> originalRows, Collection<? extends FixedValueFilterContainer> filterContainers, GetValue<RowType> getValue) {
			Vector<RowType> filteredRows = new Vector<>();
			for (int rowIndex=0; rowIndex<originalRows.size(); rowIndex++)
			{
				RowType row = originalRows.get(rowIndex);
				int rowIndex_ = rowIndex;
				boolean meetsFilter = meetsFilter(filterContainers, columnID -> getValue.getValue(columnID, rowIndex_, row));
				if (meetsFilter)
					filteredRows.add(row);
			}
			
			return filteredRows;
		}

		static boolean meetsFilter(Collection<? extends FixedValueFilterContainer> filterContainers, Function<ColumnID, Object> getValue)
		{
			for (FixedValueFilterContainer filterContainer : filterContainers)
			{
				ValueFilter filter = filterContainer.getFilter();
				if (filter!=null && filter.active)
				{
					Object value = getValue.apply(filterContainer.getColumnID());
					if (!filter.valueMeetsFilter(value))
						return false;
				}
			}
			return true;
		}

		interface FixedValueFilterContainer
		{
			ColumnID getColumnID();
			ValueFilter getFilter();
			
			record Pair(ColumnID columnID, ValueFilter filter) implements FixedValueFilterContainer
			{
				@Override public ColumnID getColumnID() { return columnID; }
				@Override public ValueFilter getFilter() { return filter; }
			}
		}

		interface ValueFilterContainer extends FixedValueFilterContainer
		{
			void setFilter(ValueFilter filter);
		}

		static abstract class ValueFilter {
			
			boolean active = false;
			boolean allowUnset = false;
		
			boolean valueMeetsFilter(Object value) {
				if (allowUnset && value==null)
					return true;
				return valueMeetsFilter_SubType(value);
			}
		
			public static ValueFilter parse(String str) {
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
				
				ValueFilter filter = null;
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
				if (!(obj instanceof ValueFilter)) return false;
				ValueFilter other = (ValueFilter) obj;
				if (this.active    !=other.active    ) return false;
				if (this.allowUnset!=other.allowUnset) return false;
				return equals_SubType(other);
			}
			
			@Override final protected ValueFilter clone() {
				ValueFilter filter = clone_SubType();
				filter.active     = active    ;
				filter.allowUnset = allowUnset;
				return filter;
			}
		
			protected abstract boolean valueMeetsFilter_SubType(Object value);
			protected abstract String toParsableString_SubType();
			protected abstract boolean equals_SubType(ValueFilter other);
			protected abstract ValueFilter clone_SubType();
			
			
			static class EnumFilter<E extends Enum<E>> extends ValueFilter {
				private final static HashMap<String,Function<String[],EnumFilter<?>>> KnownTypes = new HashMap<>();
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
		
				@Override protected boolean equals_SubType(ValueFilter other) {
					if (other instanceof EnumFilter) {
						EnumFilter<?> otherEnumFilter = (EnumFilter<?>) other;
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
		
				@Override protected ValueFilter clone_SubType() {
					EnumFilter<E> enumFilter = new EnumFilter<>(filterID,valueClass);
					enumFilter.allowedValues.addAll(allowedValues);
					return enumFilter;
				}
		
				public static ValueFilter parseEnumFilter(String str) {
					int pos = str.indexOf(':');
					if (pos<0) return null;
					String filterID = str.substring(0, pos);
					String namesStr = str.substring(pos+1);
					String[] names = namesStr.split(",");
					
					Function<String[], EnumFilter<?>> parseFilter = KnownTypes.get(filterID);
					if (parseFilter==null) {
						System.err.printf("Can't parse EnumFilter: Unknown filter ID \"%s\" in \"%s\"%n", filterID, str);
						return null;
					}
					
					return parseFilter.apply(names);
				}
		
				private static <E extends Enum<E>> EnumFilter<E> parseEnumFilter(Class<E> valueClass, String filterID, String[] names, Function<String, E> parseEnumValue) {
					EnumFilter<E> filter = new EnumFilter<>(filterID,valueClass);
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
		
		
			static class NumberFilter<V extends Number> extends ValueFilter {
				
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
		
				@Override protected boolean equals_SubType(ValueFilter other) {
					if (other instanceof NumberFilter) {
						NumberFilter<?> numberFilter = (NumberFilter<?>) other;
						if (valueClass!=numberFilter.valueClass) return false;
						if (!min.equals(numberFilter.min)) return false;
						if (!max.equals(numberFilter.max)) return false;
						return true;
					}
					return false;
				}
		
				@Override
				protected ValueFilter clone_SubType() {
					return new NumberFilter<>(valueClass, min, max, compare, toParsableString);
				}
				
				public static ValueFilter parseNumberFilter(String str) {
					String[] strs = str.split(":");
					if (strs.length!=3) return null;
					String valueClassName = strs[0];
					String minStr = strs[1];
					String maxStr = strs[2];
					
					switch (valueClassName) {
					case "Integer":
						try {
							NumberFilter<Integer> filter = createIntFilter();
							filter.min = Integer.parseInt(minStr);
							filter.max = Integer.parseInt(maxStr);
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					case "Long":
						try {
							NumberFilter<Long> filter = createLongFilter();
							filter.min = Long.parseLong(minStr);
							filter.max = Long.parseLong(maxStr);
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					case "Float":
						try {
							NumberFilter<Float> filter = createFloatFilter();
							filter.min = Float.intBitsToFloat( Integer.parseUnsignedInt(minStr,16) );
							filter.max = Float.intBitsToFloat( Integer.parseUnsignedInt(maxStr,16) );
							return filter;
						} catch (NumberFormatException e) {
							return null;
						}
					case "Double":
						try {
							NumberFilter<Double> filter = createDoubleFilter();
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
		
				static NumberFilter<Integer> createIntFilter   () { return new NumberFilter<Integer>(Integer.class, 0 , 0 , Integer::compare, v->Integer.toString(v)); }
				static NumberFilter<Long   > createLongFilter  () { return new NumberFilter<Long   >(Long   .class, 0L, 0L, Long   ::compare, v->Long.toString(v)); }
				static NumberFilter<Float  > createFloatFilter () { return new NumberFilter<Float  >(Float  .class, 0f, 0f, Float  ::compare, v->String.format("%08X", Float.floatToIntBits(v))); }
				static NumberFilter<Double > createDoubleFilter() { return new NumberFilter<Double >(Double .class, 0d, 0d, Double ::compare, v->String.format("%016X", Double.doubleToLongBits(v))); }
				
			}
			
			
			static class BoolFilter extends ValueFilter {
			
				boolean allowTrues = true;
		
				@Override protected boolean valueMeetsFilter_SubType(Object value) {
					if (value instanceof Boolean) {
						Boolean v = (Boolean) value;
						return allowTrues == v.booleanValue();
					}
					if (value instanceof Data.BooleanWithText) {
						Data.BooleanWithText v = (Data.BooleanWithText) value;
						return allowTrues == v.getBool();
					}
					return false;
				}
		
				@Override protected boolean equals_SubType(ValueFilter other) {
					if (other instanceof BoolFilter) {
						BoolFilter boolFilter = (BoolFilter) other;
						return this.allowTrues == boolFilter.allowTrues;
					}
					return false;
				}
			
				@Override protected BoolFilter clone_SubType() {
					BoolFilter boolFilter = new BoolFilter();
					boolFilter.allowTrues = allowTrues;
					return boolFilter;
				}
		
				public static ValueFilter parseBoolFilter(String str) {
					BoolFilter boolFilter = new BoolFilter();
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
		
		static class RowFilterPanel
		{
			private final ValueFilterGuiElement[] columnElements;
			final JPanel panel;

			RowFilterPanel(ColumnID[] originalColumns) { this(originalColumns, false, null); }
			RowFilterPanel(ColumnID[] originalColumns, boolean ignoreFilterInColumnID, Runnable updateAfterChange)
			{
				panel = new JPanel(new GridBagLayout());
				
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				
				columnElements = new ValueFilterGuiElement[originalColumns.length];
				ValueFilterGuiElement ce;
				for (int i=0; i<originalColumns.length; i++)
				{
					columnElements[i] = ce = new ValueFilterGuiElement(originalColumns[i], ignoreFilterInColumnID, updateAfterChange);
					
					c.weightx = 0;
					c.gridwidth = 1;
					panel.add(ce.baseCheckBox, c);
					
					c.weightx = 1;
					c.gridwidth = GridBagConstraints.REMAINDER;
					panel.add(ce.optionsPanel, c);
				}
				
				c.weighty = 1;
				panel.add(new JLabel(), c);
			}

			void setEnabled(boolean enabled)
			{
				for (ValueFilterGuiElement ce : columnElements)
					ce.setEnabled(enabled);
			}
			
			boolean setFilterFinal(boolean hasChanged, int i, ValueFilterContainer vfc)
			{
				ValueFilter filterInGui = columnElements[i].getFilter();
				
				ValueFilter storedFilter = vfc.getFilter();
				if (filterInGui==null && storedFilter!=null) {
					storedFilter.active = false;
					hasChanged = true;
					
				} else if (filterInGui!=null && !filterInGui.equals(storedFilter)) {
					vfc.setFilter(filterInGui);
					hasChanged = true;
				}
				return hasChanged;
			}

			HashMap<String, ValueFilter> getCopyOfCurrentPreset()
			{
				HashMap<String, ValueFilter> preset = new HashMap<>();
				for (ValueFilterGuiElement elem : columnElements) {
					ValueFilter filter = elem.getFilter();
					if (filter!=null && filter.active)
						preset.put(elem.id, filter.clone());
				}
				return preset;
			}

			void setPresetInGui(HashMap<String, ValueFilter> preset)
			{
				for (ValueFilterGuiElement elem : columnElements)
					elem.setValues(preset==null ? null : preset.get(elem.id));
			}

			static class ValueFilterGuiElement
			{
				interface OptionsPanelConstructor
				{
					OptionsPanel<?> create(Runnable updateAfterChange);
				}
				
				private static final HashMap<Class<?>,OptionsPanelConstructor> RegisteredOptionsPanels = new HashMap<>();
				static {
					RegisteredOptionsPanels.put(Boolean            .class, uac->new OptionsPanel.BoolOptions(new ValueFilter.BoolFilter(), uac));
					RegisteredOptionsPanels.put(Data.Capability    .class, uac->new OptionsPanel.BoolOptions(new ValueFilter.BoolFilter(), uac));
					RegisteredOptionsPanels.put(Integer            .class, uac->new OptionsPanel.NumberOptions<>( ValueFilter.NumberFilter.createIntFilter()   , v->Integer.toString(v), Integer::parseInt   , null            , uac ));
					RegisteredOptionsPanels.put(Long               .class, uac->new OptionsPanel.NumberOptions<>( ValueFilter.NumberFilter.createLongFilter()  , v->Long   .toString(v), Long   ::parseLong  , null            , uac ));
					RegisteredOptionsPanels.put(Float              .class, uac->new OptionsPanel.NumberOptions<>( ValueFilter.NumberFilter.createFloatFilter() , v->Float  .toString(v), Float  ::parseFloat , Float ::isFinite, uac ));
					RegisteredOptionsPanels.put(Double             .class, uac->new OptionsPanel.NumberOptions<>( ValueFilter.NumberFilter.createDoubleFilter(), v->Double .toString(v), Double ::parseDouble, Double::isFinite, uac ));
					RegisteredOptionsPanels.put(Truck.Country      .class, uac->new OptionsPanel.EnumOptions<>(Truck.Country      .class, Truck.Country      .values(), uac));
					RegisteredOptionsPanels.put(Truck.DiffLockType .class, uac->new OptionsPanel.EnumOptions<>(Truck.DiffLockType .class, Truck.DiffLockType .values(), uac));
					RegisteredOptionsPanels.put(Truck.TruckType    .class, uac->new OptionsPanel.EnumOptions<>(Truck.TruckType    .class, Truck.TruckType    .values(), uac));
					RegisteredOptionsPanels.put(Truck.UDV.ItemState.class, uac->new OptionsPanel.EnumOptions<>(Truck.UDV.ItemState.class, Truck.UDV.ItemState.values(), uac));
				}
				
				private static final Color COLOR_BG_ACTIVE_FILTER = new Color(0xFFDB00);
				private final String id;
				private final JCheckBox baseCheckBox;
				private final OptionsPanel<?> optionsPanel;
				private final Color defaultBG;
				private final Runnable updateAfterChange;
			
				ValueFilterGuiElement(ColumnID columnID, boolean ignoreFilterInColumnID, Runnable updateAfterChange)
				{
					this(
						columnID.id,
						columnID.config.name,
						columnID.config.columnClass,
						ignoreFilterInColumnID ? null : columnID.filter,
						updateAfterChange
					);
				}
				ValueFilterGuiElement(String id, String label, Class<?> valueClass, ValueFilter filter, Runnable updateAfterChange)
				{
					this.id = id;
					this.updateAfterChange = updateAfterChange;
					
					OptionsPanelConstructor creator = RegisteredOptionsPanels.get(valueClass);
					if (creator==null) optionsPanel = new OptionsPanel.DummyOptions(valueClass);
					else               optionsPanel = creator.create(updateAfterChange);
					
					optionsPanel.setValues(filter);
					if (optionsPanel.filter!=null)
						optionsPanel.filter.active = filter!=null && filter.active;
					
					baseCheckBox = SnowRunner.createCheckBox(
							label,
							optionsPanel.filter!=null && optionsPanel.filter.active,
							null, optionsPanel.filter!=null,
							this::setActive);
					defaultBG = baseCheckBox.getBackground();
					
					showActive(optionsPanel.filter!=null && optionsPanel.filter.active);
				}
			
				void setEnabled(boolean isEnabled)
				{
					baseCheckBox.setEnabled(isEnabled && optionsPanel.filter!=null);
					showActive             (isEnabled && optionsPanel.filter!=null && optionsPanel.filter.active);
				}
				
				private void setActive(boolean isActive) {
					if (optionsPanel.filter!=null)
						optionsPanel.filter.active = isActive;
					showActive(isActive);
					if (updateAfterChange!=null)
						updateAfterChange.run();
				}
			
				private void showActive(boolean isActive) {
					optionsPanel.setEnableOptions(isActive);
					baseCheckBox.setBackground(isActive ? COLOR_BG_ACTIVE_FILTER : defaultBG);
					//optionsPanel.setBackground(isActive ? COLOR_BG_ACTIVE_FILTER : defaultBG);
				}
			
				ValueFilter getFilter() {
					return optionsPanel.filter;
				}
			
				void setValues(ValueFilter filter) {
					if (optionsPanel.filter!=null) {
						optionsPanel.filter.active = filter!=null && filter.active;
						baseCheckBox.setSelected(optionsPanel.filter.active);
						showActive(optionsPanel.filter.active);
					}
					optionsPanel.setValues(filter);
				}
			
				static abstract class OptionsPanel<FilterType extends ValueFilter> extends JPanel {
					private static final long serialVersionUID = -8491252831775091069L;
					
					protected final GridBagConstraints c;
					final FilterType filter;
					private final JCheckBox chkbxUnset;
					protected final Runnable updateAfterChange;
			
					OptionsPanel(FilterType filter, Runnable updateAfterChange) {
						super(new GridBagLayout());
						this.filter = filter;
						this.updateAfterChange = updateAfterChange;
						c = new GridBagConstraints();
						c.fill = GridBagConstraints.BOTH;
						c.weightx = 0;
						if (this.filter!=null)
							add(chkbxUnset = SnowRunner.createCheckBox("unset", false, null, true, b->{ this.filter.allowUnset = b; updateAfterChange(); }), c);
						else
							chkbxUnset = null;
					}
					
					protected void updateAfterChange() 
					{
						if (updateAfterChange!=null)
							updateAfterChange.run();
					}

					void setEnableOptions(boolean isEnabled) {
						if (chkbxUnset!=null)
							chkbxUnset.setEnabled(isEnabled);
					}
			
					void setValues(ValueFilter filter) {
						if (this.filter!=null) {
							this.filter.allowUnset = filter!=null && filter.allowUnset;
							chkbxUnset.setSelected(this.filter.allowUnset);
						}
					}
			
			
					static class DummyOptions extends OptionsPanel<ValueFilter.BoolFilter>
					{
						private static final long serialVersionUID = 4500779916477896148L;
			
						public DummyOptions(Class<?> columnClass) {
							super(null, null);
							
							String message;
							if (columnClass==String.class) message = "---   No Filter for text values   ---";
							else message = String.format("---   No Filter for Column of %s   ---", columnClass==null ? "<null>" : columnClass.getCanonicalName());
							
							JLabel msgComp = new JLabel(message);
							msgComp.setEnabled(false);
							
							c.weightx = 1;
							add(msgComp, c);
						}
					}
			
			
					static class EnumOptions<E extends Enum<E>> extends OptionsPanel<ValueFilter.EnumFilter<E>> {
						private static final long serialVersionUID = -458324264563153126L;
						
						private final JCheckBox[] checkBoxes;
						private final E[] values;
			
						EnumOptions( Class<E> valueClass, E[] values, Runnable updateAfterChange) {
							this(new ValueFilter.EnumFilter<>(ValueFilter.EnumFilter.getFilterID(valueClass), valueClass), values, updateAfterChange);
						}
			
						EnumOptions(ValueFilter.EnumFilter<E> filter, E[] values, Runnable updateAfterChange) {
							super(filter, updateAfterChange);
							if (filter==null) throw new IllegalArgumentException();
							if (values==null) throw new IllegalArgumentException();
							
							this.values = values;
							
							checkBoxes = new JCheckBox[this.values.length];
							c.weightx = 0;
							for (int i=0; i<this.values.length; i++) {
								E e = this.values[i];
								boolean isSelected = this.filter.allowedValues.contains(e);
								checkBoxes[i] = SnowRunner.createCheckBox(e.toString(), isSelected, null, true, b->{
									if (b) this.filter.allowedValues.add(e);
									else   this.filter.allowedValues.remove(e);
									updateAfterChange();
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
			
						@Override void setValues(ValueFilter filter) {
							super.setValues(filter);
							if (filter instanceof ValueFilter.EnumFilter) {
								ValueFilter.EnumFilter<?> enumFilter = (ValueFilter.EnumFilter<?>) filter;
								
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
					}
			
			
					static class NumberOptions<V extends Number> extends OptionsPanel<ValueFilter.NumberFilter<V>> {
						private static final long serialVersionUID = -436186682804402478L;
						
						private final JLabel labMin, labMax;
						private final JTextField fldMin, fldMax;
						private final Function<V, String> toString;
						private final Function<String,V> convert;
			
						NumberOptions(ValueFilter.NumberFilter<V> filter, Function<V,String> toString, Function<String,V> convert, Predicate<V> isOK, Runnable updateAfterChange) {
							super(filter, updateAfterChange);
							if (filter  ==null) throw new IllegalArgumentException();
							if (toString==null) throw new IllegalArgumentException();
							if (convert ==null) throw new IllegalArgumentException();
							
							this.toString = v -> v==null ? "<null>" : toString.apply(v);
							
							this.convert = str -> {
								try { return convert.apply(str);
								} catch (NumberFormatException e) { return null; }
							};
							
							c.weightx = 0;
							add(labMin = new JLabel("   min: "), c);
							add(fldMin = SnowRunner.createTextField(10, this.toString.apply(this.filter.min), this.convert, isOK, v->{ this.filter.min = v; updateAfterChange(); }), c);
							add(labMax = new JLabel("   max: "), c);
							add(fldMax = SnowRunner.createTextField(10, this.toString.apply(this.filter.max), this.convert, isOK, v->{ this.filter.max = v; updateAfterChange(); }), c);
							c.weightx = 1;
							add(new JLabel(), c);
						}
			
						@Override void setEnableOptions(boolean isEnabled) {
							super.setEnableOptions(isEnabled);
							labMin.setEnabled(isEnabled);
							fldMin.setEnabled(isEnabled);
							labMax.setEnabled(isEnabled);
							fldMax.setEnabled(isEnabled);
						}
			
						@Override void setValues(ValueFilter filter) {
							super.setValues(filter);
							if (filter instanceof ValueFilter.NumberFilter) {
								ValueFilter.NumberFilter<?> numberFilter = (ValueFilter.NumberFilter<?>) filter;
								if (this.filter.valueClass!=numberFilter.valueClass)
									throw new IllegalStateException(String.format("filter.valueClass(%s) != numberFilter.valueClass(%s)", this.filter.valueClass, numberFilter.valueClass));
								this.filter.min = this.filter.valueClass.cast(numberFilter.min);
								this.filter.max = this.filter.valueClass.cast(numberFilter.max);
								fldMin.setText(toString.apply(this.filter.min));
								fldMax.setText(toString.apply(this.filter.max));
							}
						}
						
					}
			
			
					static class BoolOptions extends OptionsPanel<ValueFilter.BoolFilter> {
						private static final long serialVersionUID = 1821563263682227455L;
						private final JRadioButton rbtnTrue;
						private final JRadioButton rbtnFalse;
						
						BoolOptions(ValueFilter.BoolFilter filter, Runnable updateAfterChange) {
							super(filter, updateAfterChange);
							ButtonGroup bg = new ButtonGroup();
							c.weightx = 0;
							add(rbtnTrue  = SnowRunner.createRadioButton("TRUE" , bg, true,  this.filter.allowTrues, e->{ this.filter.allowTrues = true ; updateAfterChange(); } ), c);
							add(rbtnFalse = SnowRunner.createRadioButton("FALSE", bg, true, !this.filter.allowTrues, e->{ this.filter.allowTrues = false; updateAfterChange(); }), c);
							c.weightx = 1;
							add(new JLabel(), c);
						}
			
						@Override void setEnableOptions(boolean isEnabled) {
							super.setEnableOptions(isEnabled);
							rbtnTrue .setEnabled(isEnabled);
							rbtnFalse.setEnabled(isEnabled);
						}
			
						@Override void setValues(ValueFilter filter) {
							super.setValues(filter);
							if (filter instanceof ValueFilter.BoolFilter) {
								ValueFilter.BoolFilter boolFilter = (ValueFilter.BoolFilter) filter;
								this.filter.allowTrues = boolFilter.allowTrues;
								if (this.filter.allowTrues) rbtnTrue .setSelected(true);
								else                        rbtnFalse.setSelected(true);
							}
						}
						
					}
				}
			}
		}

		static class FilterRowsDialog extends ModelConfigureDialog<HashMap<String,ValueFilter>> {
			private static final long serialVersionUID = 6171301101675843952L;
			private final ColumnID[] originalColumns;
			private final RowFilterPanel rowFilterPanel;
		
			FilterRowsDialog(Window owner, ColumnID[] originalColumns, String tableModelID) {
				super(owner, "Filter Rows", "Define filters", presets, tableModelID);
				this.originalColumns = originalColumns;
				
				rowFilterPanel = new RowFilterPanel(this.originalColumns);
				
				filterListScrollPane.setViewportView(rowFilterPanel.panel);
				filterListScrollPane.setPreferredSize(new Dimension(600,700));
			}
		
			@Override protected boolean resetValuesFinal() {
				boolean hasChanged = false;
				for (ColumnID columnID : originalColumns) {
					if (columnID.filter!=null) {
						columnID.filter = null;
						hasChanged = true;
					}
				}
				return hasChanged;
			}
		
			@Override protected boolean setValuesFinal() {
				boolean hasChanged = false;
				for (int i=0; i<originalColumns.length; i++) {
					ColumnID columnID = originalColumns[i];
					hasChanged = rowFilterPanel.setFilterFinal(hasChanged, i, columnID);
				}
				return hasChanged;
			}
		
			@Override protected void setPresetInGui(HashMap<String, ValueFilter> preset) {
				rowFilterPanel.setPresetInGui(preset);
			}
		
			@Override protected HashMap<String, ValueFilter> getCopyOfCurrentPreset() {
				return rowFilterPanel.getCopyOfCurrentPreset();
			}
		
		}
		
	}
	
	private static class ColumnHiding
	{
		static final Presets presets = new Presets();

		static class Presets extends PresetMaps<HashSet<String>> {
		
			Presets() {
				super("ColumnHidePresets", SnowRunner.ColumnHidePresetsFile, HashSet<String>::new);
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

		static ColumnID[] removeHiddenColumns(ColumnID[] originalColumns) {
			return Arrays.stream(originalColumns).filter(column->column.isVisible).toArray(ColumnID[]::new);
		}

		static class ColumnHideDialog extends ModelConfigureDialog<HashSet<String>> {
			private static final long serialVersionUID = 4240161527743718020L;
			private final JCheckBox[] columnElements;
			private final HashSet<String> currentPreset;
			private final ColumnID[] originalColumns;
			
			ColumnHideDialog(Window owner, ColumnID[] originalColumns, String tableModelIDstr) {
				super(owner, "Visible Columns", "Define visible columns", presets, tableModelIDstr);
				this.originalColumns = originalColumns;
				
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
				
				filterListScrollPane.setViewportView(columnPanel);
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
		}
		
	}

	private static abstract class ModelConfigureDialog<Preset> extends JDialog {
		private static final long serialVersionUID = 8159900024537014376L;
		
		private final Window owner;
		private boolean hasChanged;
		private boolean ignorePresetComboBoxEvent;
		private final HashMap<String, Preset> modelPresets;
		private final Vector<String> presetNames;
		private final JComboBox<String> presetComboBox;
		private final PresetMaps<Preset> presetMaps;
		protected final JScrollPane filterListScrollPane;
	
		ModelConfigureDialog(Window owner, String title, String columnPanelHeadline, PresetMaps<Preset> presetMaps, String tableModelID) {
			super(owner, title, ModalityType.APPLICATION_MODAL);
			this.owner = owner;
			this.presetMaps = presetMaps;
			this.modelPresets = this.presetMaps.getModelPresets(tableModelID);
			this.hasChanged = false;
			GridBagConstraints c;
			
			filterListScrollPane = new JScrollPane();
			filterListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			filterListScrollPane.setPreferredSize(new Dimension(300,400));
			filterListScrollPane.getVerticalScrollBar().setUnitIncrement(10);
			
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
			contentPane.add(filterListScrollPane, c);
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

	private static abstract class PresetMaps<Preset> {
		
		private final String label;
		private final String pathname;
		private final Supplier<Preset> createEmptyPreset;
		private final HashMap<String,HashMap<String,Preset>> presets;
		
		PresetMaps(String label, String pathname, Supplier<Preset> createEmptyPreset) {
			if (pathname         ==null) throw new IllegalArgumentException();
			if (createEmptyPreset==null) throw new IllegalArgumentException();
			this.label = label;
			this.createEmptyPreset = createEmptyPreset;
			this.pathname = pathname;
			this.presets = new HashMap<>();
			read();
		}
	
		HashMap<String, Preset> getModelPresets(String tableModelID) {
			HashMap<String, Preset> modelPresets = presets.get(tableModelID);
			if (modelPresets==null)
				presets.put(tableModelID, modelPresets = new HashMap<>());
			return modelPresets;
		}
	
		void read()
		{
			presets.clear();
			
			File file = new File(pathname);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				
				System.out.printf("Read %s from file \"%s\" ...%n", label, file.getAbsolutePath());
				
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
					if ( (valueStr=Data.getLineValue(line, "Preset="))!=null && modelPresets!=null ) {
						preset = modelPresets.get(valueStr);
						if (preset==null)
							modelPresets.put(valueStr, preset = createEmptyPreset.get());
					}
					if (preset!=null)
						parseLine(line, preset);
					
				}
				
				System.out.printf("... done%n");
				
			} catch (FileNotFoundException ex) {
				//ex.printStackTrace();
			} catch (IOException ex) {
				System.err.printf("IOException while reading %s from file \"%s\": %s", label, file.getAbsolutePath(), ex.getMessage());
				//ex.printStackTrace();
			}
		}
	
		void write()
		{
			File file = new File(pathname);
			try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
				
				System.out.printf("Write %s to file \"%s\" ...%n", label, file.getAbsolutePath());
				
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
				
				System.out.printf("... done%n");
				
			} catch (IOException ex) {
				System.err.printf("IOException while writing %s to file \"%s\": %s", label, file.getAbsolutePath(), ex.getMessage());
				//ex.printStackTrace();
			}
		}
	
		protected abstract void parseLine(String line, Preset preset);
		protected abstract void writePresetInLines(Preset preset, PrintWriter out);
	}

	public static class ColumnID implements Tables.SimplifiedColumnIDInterface, RowFiltering.ValueFilterContainer {
		
		public interface TableModelBasedBuilder<ValueType> {
			ValueType getValue(Object value, VerySimpleTableModel<?> tableModel);
		}
		
		public interface LanguageBasedStringBuilder {
			String getValue(Object value, Language language);
		}
		
		public interface VerboseTableModelBasedBuilder<ValueType> {
			ValueType getValue(Object value, VerySimpleTableModel<?> tableModel, TextOutput textOutput);
		}
		
		public interface VerboseLanguageBasedStringBuilder {
			String getValue(Object value, Language language, TextOutput textOutput);
		}
		
		private final SimplifiedColumnConfig config;
		public  final String id;
		private final Function<Object, ?> getValue;
		private final LanguageBasedStringBuilder getValueL;
		private final TableModelBasedBuilder<?> getValueT;
		private final BiFunction<Object,TextOutput,?> showValueComputation;
		private final VerboseLanguageBasedStringBuilder showValueComputationL;
		private final VerboseTableModelBasedBuilder<?> showValueComputationT;
		private final boolean useValueAsStringID;
		private final Integer horizontalAlignment;
		private final Color foreground;
		private final Color background;
		private final String format;
		private boolean isVisible;
		private RowFiltering.ValueFilter filter;
		private final EnumSet<Update> cacheUpdateEvents;
		private final HashMap<Integer, Object> valueCache;
		
		public              ColumnID(String ID, String name, Class<String    > columnClass, int prefWidth,                                     Integer horizontalAlignment, String format,                             LanguageBasedStringBuilder         getValue) {
			this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, false            , null, getValue, null, null, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth,                                     Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType>        getValue) {
			this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, getValue, null, null, null, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth,                                     Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
			this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, null, null, getValue, null, null, null);
		}
		public              ColumnID(String ID, String name, Class<String    > columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format,                             LanguageBasedStringBuilder         getValue) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, false             , null, getValue, null, null, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType>        getValue) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, getValue, null, null, null, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, null, null, getValue, null, null, null);
		}
		public              ColumnID(String ID, String name, Class<String    > columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format,                             LanguageBasedStringBuilder         getValue, VerboseLanguageBasedStringBuilder         showValueComputation) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, false             , null, getValue, null, null, showValueComputation, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType>        getValue, BiFunction<Object,TextOutput,ColumnType>  showValueComputation) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, getValue, null, null, showValueComputation, null, null);
		}
		public <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue, VerboseTableModelBasedBuilder<ColumnType> showValueComputation) {
			this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, null, null, getValue, null, null, showValueComputation);
		}
		private <ColumnType> ColumnID(
				String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID,
				Function  <Object,           ColumnType> getValue            ,        LanguageBasedStringBuilder getValueL            ,        TableModelBasedBuilder<ColumnType> getValueT            ,
				BiFunction<Object,TextOutput,ColumnType> showValueComputation, VerboseLanguageBasedStringBuilder showValueComputationL, VerboseTableModelBasedBuilder<ColumnType> showValueComputationT
		) {
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
			this.showValueComputation = showValueComputation;
			this.showValueComputationL = showValueComputationL;
			this.showValueComputationT = showValueComputationT;
			
			cacheUpdateEvents = EnumSet.noneOf(Update.class);
			valueCache = new HashMap<>();
		}
		
		@Override public SimplifiedColumnConfig   getColumnConfig() { return config; }
		@Override public ColumnID                 getColumnID    () { return this;   }
		@Override public RowFiltering.ValueFilter getFilter      () { return filter; }
		@Override public void setFilter(RowFiltering.ValueFilter filter) { this.filter = filter; }

		public enum Update { Data, Language, SaveGame, DLCAssignment, SpecialTruckAddons }
		
		public ColumnID configureCaching(Update... updateEvents)
		{
			this.cacheUpdateEvents.addAll(Arrays.asList(updateEvents));
			return this;
		}
		
		public Object getValue(int rowIndex, Object row, Language language, VerySimpleTableModel<?> tableModel)
		{
			boolean useCaching = !cacheUpdateEvents.isEmpty();
			
			if (useCaching && valueCache.containsKey(rowIndex))
				return valueCache.get(rowIndex);
			
			Object value = getValue_uncached(row, language, tableModel);
			
			if (useCaching)
				valueCache.put(rowIndex, value);
			
			return value;
		}
		
		private Object getValue_uncached(Object row, Language language, VerySimpleTableModel<?> tableModel) {
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
				return getValueT.getValue(row,(VerySimpleTableModel<?>) tableModel);
			}
			
			return null;
		}
		
		public Object showValueComputation(Object row, Language language, VerySimpleTableModel<?> tableModel, TextOutput textOutput) {
			if (showValueComputation!=null) {
				if (useValueAsStringID) {
					String stringID = (String) showValueComputation.apply(row, textOutput);
					return SnowRunner.solveStringID(stringID, language);
				}
				return showValueComputation.apply(row, textOutput);
			}
			if (showValueComputationL!=null) {
				return showValueComputationL.getValue(row,language,textOutput);
			}
			if (showValueComputationT!=null) {
				return showValueComputationT.getValue(row,(VerySimpleTableModel<?>) tableModel,textOutput);
			}
			
			return null;
		}
		
		public boolean hasShowValueComputationFcn() {
			return showValueComputation!=null || showValueComputationL!=null || showValueComputationT!=null;
		}
		
		public interface GetFunction_MDLR<ResultType, ModelType, RowType>
		{
			ResultType get(ModelType model, Data data, Language language, RowType row);
		}
		
		public interface GetFunction_MLR<ResultType, ModelType, RowType>
		{
			ResultType get(ModelType model, Language language, RowType row);
		}
		
		public static <ResultType, ModelType extends VerySimpleTableModel<RowType>, RowType>
			TableModelBasedBuilder<ResultType>
			get(
					Class<ModelType> modelClass,
					Class<RowType> rowClass,
					Function<ModelType,Data> getData,
					GetFunction_MDLR<ResultType, ModelType, RowType> getFunction
			)
		{
			return (row_,model_) -> {
				if (  row_==null || !  rowClass.isAssignableFrom(  row_.getClass())) return null;
				if (model_==null || !modelClass.isAssignableFrom(model_.getClass())) return null;
				RowType   row   =   rowClass.cast(  row_);
				ModelType model = modelClass.cast(model_);
				return getFunction.get(model, getData.apply(model), model.language, row);
			};
		}
		
		public static <ResultType, ModelType extends VerySimpleTableModel<RowType>, RowType>
			TableModelBasedBuilder<ResultType>
			get(
					Class<ModelType> modelClass,
					Class<RowType> rowClass,
					GetFunction_MLR<ResultType, ModelType, RowType> getFunction
			)
		{
			return (row_,model_) -> {
				if (  row_==null || !  rowClass.isAssignableFrom(  row_.getClass())) return null;
				if (model_==null || !modelClass.isAssignableFrom(model_.getClass())) return null;
				RowType   row   =   rowClass.cast(  row_);
				ModelType model = modelClass.cast(model_);
				return getFunction.get(model, model.language, row);
			};
		}
		
		public static <ResultType, RowType>
			Function<Object, ResultType>
			get(
					Class<RowType> rowClass,
					Function<RowType, ResultType> getFunction
			)
		{
			return row_ -> {
				if (row_==null || !rowClass.isAssignableFrom(row_.getClass())) return null;
				RowType row = rowClass.cast(row_);
				return getFunction.apply(row);
			};
		}
	}
	
	public static abstract class OutputSourceTableModel<RowType> extends VerySimpleTableModel<RowType> {
		
		protected Runnable outputUpdateMethod;

		protected OutputSourceTableModel(Window mainWindow, GlobalFinalDataStructures gfds, ColumnID[] columns) {
			super(mainWindow, gfds, columns);
			outputUpdateMethod = null;
		}
		
		public void setOutputUpdateMethod(Runnable outputUpdateMethod) {
			this.outputUpdateMethod = outputUpdateMethod;
		}

		@Override protected void extraUpdate() {
			updateOutput();
		}

		void updateOutput() {
			if (outputUpdateMethod!=null)
				outputUpdateMethod.run();
		}
	}
	
	public static abstract class ExtendedVerySimpleTableModelUOS<RowType> extends OutputSourceTableModel<RowType> implements UnspecificOutputSource {
		protected ExtendedVerySimpleTableModelUOS(Window mainWindow, GlobalFinalDataStructures gfds, ColumnID[] columns) {
			super(mainWindow, gfds, columns);
		}

		@Override public void setOutputContentForRow(int rowIndex)
		{
			RowType row = getRow(rowIndex);
			if (row!=null)
				setContentForRow(rowIndex, row);
		}

		protected abstract void setContentForRow(int rowIndex, RowType row);
	}
	
	public static abstract class ExtendedVerySimpleTableModelTPOS<RowType> extends OutputSourceTableModel<RowType> implements TextPaneOutputSource {

		protected ExtendedVerySimpleTableModelTPOS(Window mainWindow, GlobalFinalDataStructures gfds, ColumnID[] columns) {
			super(mainWindow, gfds, columns);
		}

		@Override
		public void setOutputContentForRow(StyledDocumentInterface doc, int rowIndex) {
			RowType row = getRow(rowIndex);
			if (row!=null)
				setOutputContentForRow(doc, rowIndex, row);
		}

		protected abstract void setOutputContentForRow(StyledDocumentInterface doc, int rowIndex, RowType row);
	}
	
	public static abstract class ExtendedVerySimpleTableModelTAOS<RowType> extends OutputSourceTableModel<RowType> implements TextAreaOutputSource {

		protected ExtendedVerySimpleTableModelTAOS(Window mainWindow, GlobalFinalDataStructures gfds, ColumnID[] columns) {
			super(mainWindow, gfds, columns);
		}
		
		@Override public String getOutputTextForRow(int rowIndex) {
			RowType row = getRow(rowIndex);
			if (row==null) return "";
			return getOutputTextForRow(rowIndex, row);
		}

		protected abstract String getOutputTextForRow(int rowIndex, RowType row);
	}
}