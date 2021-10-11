package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import java.util.EnumMap;
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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SingleSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data.AddonCategories;
import net.schwarzbaer.java.games.snowrunner.Data.Engine;
import net.schwarzbaer.java.games.snowrunner.Data.Gearbox;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Suspension;
import net.schwarzbaer.java.games.snowrunner.Data.Trailer;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.Data.TruckComponent;
import net.schwarzbaer.java.games.snowrunner.Data.TruckTire;
import net.schwarzbaer.java.games.snowrunner.Data.UserDefinedValues;
import net.schwarzbaer.java.games.snowrunner.Data.Winch;
import net.schwarzbaer.java.games.snowrunner.DataTables.TableSimplifier.TableContextMenuModifier;
import net.schwarzbaer.java.games.snowrunner.DataTables.TableSimplifier.TextAreaOutputSource;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.ListenerSource;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SaveGameListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons.List;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons.SpecialTruckAddonList;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;

class DataTables {
	
	static class TableSimplifierRowSorter extends Tables.SimplifiedRowSorter {

		public TableSimplifierRowSorter(SimplifiedTableModel<?> model) {
			super(model);
		}

		@Override
		protected boolean isNewClass(Class<?> columnClass) {
			return
				(columnClass == Truck.Country       .class) ||
				(columnClass == Truck.TruckType     .class) ||
				(columnClass == Truck.DiffLockType  .class) ||
				//(columnClass == TruckAddon.InstState.class) ||
				(columnClass == Truck.UDV.ItemState .class);
		}

		@Override
		protected Comparator<Integer> addComparatorForNewClass(Comparator<Integer> comparator, SortOrder sortOrder, Class<?> columnClass, Function<Integer,Object> getValueAtRow) {
			if      (columnClass == Truck.Country       .class) comparator = addComparator(comparator, sortOrder, row->(Truck.Country       )getValueAtRow.apply(row));
			else if (columnClass == Truck.TruckType     .class) comparator = addComparator(comparator, sortOrder, row->(Truck.TruckType     )getValueAtRow.apply(row));
			else if (columnClass == Truck.DiffLockType  .class) comparator = addComparator(comparator, sortOrder, row->(Truck.DiffLockType  )getValueAtRow.apply(row));
			//else if (columnClass == TruckAddon.InstState.class) comparator = addComparator(comparator, sortOrder, row->(TruckAddon.InstState)getValueAtRow.apply(row));
			else if (columnClass == Truck.UDV.ItemState .class) comparator = addComparator(comparator, sortOrder, row->(Truck.UDV.ItemState )getValueAtRow.apply(row));
			return comparator;
		}
	}

	static class TableSimplifier {
		
		final SimplifiedTableModel<?> tableModel;
		final JTable table;
		final JScrollPane tableScrollPane;
		final ContextMenu tableContextMenu;
	
		TableSimplifier(SimplifiedTableModel<?> tableModel) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			this.tableModel = tableModel;
			
			table = new JTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			tableScrollPane = new JScrollPane(table);
			
			table.setModel(this.tableModel);
			this.tableModel.setTable(table);
			if (this.tableModel instanceof VerySimpleTableModel)
				((VerySimpleTableModel<?>) this.tableModel).reconfigureAfterTableStructureUpdate();
			else
				this.tableModel.setColumnWidths(table);
			
			SimplifiedRowSorter rowSorter = new TableSimplifierRowSorter(this.tableModel);
			table.setRowSorter(rowSorter);
			
//			table.addMouseListener(new MouseAdapter() {
//				@Override public void mousePressed(MouseEvent e) {
//					System.out.printf("table.mousePressed(btn:%d, x:%d, y:%d)%n", e.getButton(), e.getX(), e.getY());
//				}
//			});
//			
//			tableScrollPane.addMouseListener(new MouseAdapter() {
//				@Override public void mousePressed(MouseEvent e) {
//					System.out.printf("tableScrollPane.mousePressed(btn:%d, x:%d, y:%d)%n", e.getButton(), e.getX(), e.getY());
//				}
//			});
			
			tableContextMenu = new ContextMenu();
			tableContextMenu.addTo(table, tableScrollPane);
			tableContextMenu.add(SnowRunner.createMenuItem("Reset Row Order",true,e->{
				rowSorter.resetSortOrder();
				table.repaint();
			}));
			tableContextMenu.add(SnowRunner.createMenuItem("Show Column Widths", true, e->{
				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
			
			if (this.tableModel instanceof TableContextMenuModifier)
				((TableContextMenuModifier)this.tableModel).modifyTableContextMenu(table,tableContextMenu);
		}
		
		static class ContextMenu extends JPopupMenu {
			private static final long serialVersionUID = 2403688936186717306L;
			
			private Vector<ContextMenuInvokeListener> listeners = new Vector<>();
			
			public void addTo(JTable table, JScrollPane scrollPane) {
				table.addMouseListener(new MouseAdapter() {
					@Override public void mouseClicked(MouseEvent e) {
						if (e.getButton()==MouseEvent.BUTTON3) {
							notifyListeners(table, e.getX(), e.getY());
							show(table, e.getX(), e.getY());
						}
					}

				});
				scrollPane.addMouseListener(new MouseAdapter() {
					@Override public void mouseClicked(MouseEvent e) {
						if (e.getButton()==MouseEvent.BUTTON3) {
							notifyListeners(table, null, null);
							show(scrollPane, e.getX(), e.getY());
						}
					}
				});
			}
			
			private void notifyListeners(JTable table, Integer x, Integer y) {
				for (ContextMenuInvokeListener listener:listeners)
					listener.contextMenuWillBeInvoked(table, x, y);
			}
			
			public void    addContextMenuInvokeListener( ContextMenuInvokeListener listener ) { listeners.   add(listener); } 
			public void removeContextMenuInvokeListener( ContextMenuInvokeListener listener ) { listeners.remove(listener); }
			
			public interface ContextMenuInvokeListener {
				public void contextMenuWillBeInvoked(JTable table, Integer x, Integer y);
			}
		}

		static JComponent create(SimplifiedTableModel<?> tableModel) {
			return create(tableModel, (Consumer<ContextMenu>)null);
		}
		static JComponent create(SimplifiedTableModel<?> tableModel, Consumer<ContextMenu> modifyContextMenu) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			
			if (tableModel instanceof TextAreaOutputSource)
				return create(tableModel, (JTextArea)null, null, modifyContextMenu);
			
			if (tableModel instanceof TextPaneOutputSource)
				return create(tableModel, (JTextPane)null, null, modifyContextMenu);
			
			TableSimplifier tableSimplifier = new TableSimplifier(tableModel);
			if (modifyContextMenu!=null) modifyContextMenu.accept(tableSimplifier.tableContextMenu);
			return tableSimplifier.tableScrollPane;
		}

		static JComponent create(SimplifiedTableModel<?> tableModel, JTextArea outputObj, Function<Runnable,Runnable> modifyUpdateMethod) {
			return create(tableModel, outputObj, modifyUpdateMethod, null);
		}
		static JComponent create(SimplifiedTableModel<?> tableModel, JTextArea outputObj, Function<Runnable,Runnable> modifyUpdateMethod, Consumer<ContextMenu> modifyContextMenu) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			
			if (tableModel instanceof TextAreaOutputSource) {
				return create(
						tableModel, (TextAreaOutputSource) tableModel,
						modifyUpdateMethod, modifyContextMenu, outputObj,
						()->{
							JTextArea outObj = new JTextArea();
							outObj.setEditable(false);
							outObj.setWrapStyleWord(true);
							outObj.setLineWrap(true);
							return outObj;
						});
			}
			
			if (outputObj!=null)
				System.err.printf("SimplifiedTablePanel.create: JTextArea!=null but no TextAreaOutputSource implementing TableModel given: %s%n", tableModel.getClass());
			
			TableSimplifier tableSimplifier = new TableSimplifier(tableModel);
			if (modifyContextMenu!=null) modifyContextMenu.accept(tableSimplifier.tableContextMenu);
			return tableSimplifier.tableScrollPane;
		}

		static JComponent create(SimplifiedTableModel<?> tableModel, JTextPane outputObj, Function<Runnable,Runnable> modifyUpdateMethod) {
			return create(tableModel, outputObj, modifyUpdateMethod, null);
		}
		static JComponent create(SimplifiedTableModel<?> tableModel, JTextPane outputObj, Function<Runnable,Runnable> modifyUpdateMethod, Consumer<ContextMenu> modifyContextMenu) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			
			if (tableModel instanceof TextPaneOutputSource) {
				return create(
						tableModel, (TextPaneOutputSource) tableModel,
						modifyUpdateMethod, null, outputObj,
						()->{
							JTextPane outObj = new JTextPane();
							outObj.setEditable(false);
							return outObj;
						});
			}
			
			if (outputObj!=null)
				System.err.printf("SimplifiedTablePanel.create: JTextPane!=null but no TextPaneOutputSource implementing TableModel given: %s%n", tableModel.getClass());
			
			TableSimplifier tableSimplifier = new TableSimplifier(tableModel);
			if (modifyContextMenu!=null) modifyContextMenu.accept(tableSimplifier.tableContextMenu);
			return tableSimplifier.tableScrollPane;
		}

		static JComponent create(SimplifiedTableModel<?> tableModel, ArbitraryOutputSource arbitraryOutputSource) {
			return create(tableModel, arbitraryOutputSource, null);
		}
		static JComponent create(SimplifiedTableModel<?> tableModel, ArbitraryOutputSource arbitraryOutputSource, Consumer<ContextMenu> modifyContextMenu) {
			return create(
					tableModel,
					arbitraryOutputSource,
					arbitraryOutputSource::modifyUpdateMethod,
					modifyContextMenu,
					new JLabel(), // dummy
					null // is not needed, because JLabel was given as existing but never used OutputObject
			); 
		}

		static <OutputObject extends Component> JComponent create(
				SimplifiedTableModel<?> tableModel,
				OutputSource<OutputObject> outputSource,
				Function<Runnable,Runnable> modifyUpdateMethod,
				Consumer<ContextMenu> modifyContextMenu,
				OutputObject output,
				Supplier<OutputObject> createOutputObject) {
			
			if (tableModel==null)
				throw new IllegalArgumentException();
			if (outputSource==null)
				throw new IllegalArgumentException();
			
			TableSimplifier tableSimplifier = new TableSimplifier(tableModel);
			if (modifyContextMenu!=null)
				modifyContextMenu.accept(tableSimplifier.tableContextMenu);
			
			
			JComponent result;
			if (output != null)
				result = tableSimplifier.tableScrollPane;
			
			else {
				if (createOutputObject==null)
					throw new IllegalArgumentException();
				
				output = createOutputObject.get();
				
				JScrollPane outputScrollPane = new JScrollPane(output);
				//outputScrollPane.setBorder(BorderFactory.createTitledBorder("Description"));
				outputScrollPane.setPreferredSize(new Dimension(400,50));
				
				JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
				panel.setResizeWeight(1);
				panel.setTopComponent(tableSimplifier.tableScrollPane);
				panel.setBottomComponent(outputScrollPane);
				result = panel;
			}
			
			
			OutputObject output_final = output;
			Runnable outputUpdateMethod = ()->{
				int selectedRow = -1;
				int rowV = tableSimplifier.table.getSelectedRow();
				if (rowV>=0) {
					int rowM = tableSimplifier.table.convertRowIndexToModel(rowV);
					selectedRow = rowM<0 ? -1 : rowM;
				}
				outputSource.setContentForRow(output_final, selectedRow);
				//textArea_.setText(rowTextTableModel.getTextForRow(selectedRow));
			};
			if (modifyUpdateMethod!=null)
				outputUpdateMethod = modifyUpdateMethod.apply(outputUpdateMethod);
			outputSource.setOutputUpdateMethod(outputUpdateMethod);
			
			Runnable outputUpdateMethod_final = outputUpdateMethod;
			tableSimplifier.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tableSimplifier.table.getSelectionModel().addListSelectionListener(e->outputUpdateMethod_final.run());
			
			return result;
		}

		interface TableContextMenuModifier {
			void modifyTableContextMenu(JTable table, ContextMenu tableContextMenu);
		}

		interface OutputSource<OutputObject extends Component> {
			void setOutputUpdateMethod(Runnable outputUpdateMethod);
			void setContentForRow(OutputObject outputObject, int rowIndex);
		}

		interface ArbitraryOutputSource extends OutputSource<Component>{
			@Override default void setOutputUpdateMethod(Runnable outputUpdateMethod) {}
			@Override default void setContentForRow(Component dummy, int rowIndex) { setContentForRow(rowIndex); }
			void setContentForRow(int rowIndex);
			default Runnable modifyUpdateMethod(Runnable updateMethod) { return updateMethod; }
		}

		interface TextAreaOutputSource extends OutputSource<JTextArea> {
			@Override default void setContentForRow(JTextArea textArea, int rowIndex) {
				if (rowIndex<0)
					textArea.setText("");
				else
					textArea.setText(getTextForRow(rowIndex));
			}
			String getTextForRow(int rowIndex);
		}

		interface TextPaneOutputSource extends OutputSource<JTextPane> {
			@Override default void setContentForRow(JTextPane textPane, int rowIndex) {
				DefaultStyledDocument doc = new DefaultStyledDocument();
				if (rowIndex>=0) setContentForRow(doc, rowIndex);
				textPane.setStyledDocument(doc);
			}
			void setContentForRow(StyledDocument doc, int rowIndex);
		}
	}

	static class CombinedTableTabPaneTextAreaPanel extends JSplitPane {
		private static final long serialVersionUID = -2637203211606881920L;
		
		private final JTextArea textArea;
		private final JTabbedPane tabbedPane;
		private int selectedTab;
		private final Vector<Runnable> updateMethods;

		CombinedTableTabPaneTextAreaPanel() {
			super(JSplitPane.VERTICAL_SPLIT, true);
			selectedTab = 0;
			updateMethods = new Vector<Runnable>();
			
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			//textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Description"));
			textAreaScrollPane.setPreferredSize(new Dimension(400,50));
			textAreaScrollPane.setMinimumSize(new Dimension(5,5));
			
			tabbedPane = new JTabbedPane();
			tabbedPane.setPreferredSize(new Dimension(400,50));
			tabbedPane.setMinimumSize(new Dimension(5,40));
			SingleSelectionModel tabbedPaneSelectionModel = tabbedPane.getModel();
			tabbedPaneSelectionModel.addChangeListener(e->{
				int i = tabbedPaneSelectionModel.getSelectedIndex();
				selectedTab = i;
				if (i<0 || i>=updateMethods.size()) return;
				updateMethods.get(i).run();
			});
			
			setTopComponent(tabbedPane);
			setBottomComponent(textAreaScrollPane);
			setResizeWeight(1);
		}
		
		void removeAllTabs() {
			tabbedPane.removeAll();
			updateMethods.clear();
		}
		
		void setTabTitle(int index, String title) {
			tabbedPane.setTitleAt(index, title);
		}
		
		void setTabComponentAt(int index, Component component) {
			tabbedPane.setTabComponentAt(index, component);
		}
		
		
		<TableModel extends VerySimpleTableModel<?> & TextAreaOutputSource> void addTab(String title, TableModel tableModel) {
			int tabIndex = tabbedPane.getTabCount();
			JComponent panel = TableSimplifier.create(tableModel, textArea, updateMethod->{
				Runnable modifiedUpdateMethod = ()->{ if (selectedTab==tabIndex) updateMethod.run(); };
				if (tabbedPane.getTabCount()!=updateMethods.size())
					throw new IllegalStateException();
				updateMethods.add(modifiedUpdateMethod);
				return modifiedUpdateMethod;
			});
			tabbedPane.addTab(title, panel);
		}
	}
	
	static class VerySimpleTableModel<RowType> extends Tables.SimplifiedTableModel<VerySimpleTableModel.ColumnID> implements LanguageListener, SwingConstants, ListenerSource, TableContextMenuModifier {
		
		static final ColumnHidePresets columnHidePresets = new ColumnHidePresets();
		static final FilterRowsPresets filterRowsPresets = new FilterRowsPresets();
		
		static final Color COLOR_BG_FALSE = new Color(0xFF6600);
		static final Color COLOR_BG_TRUE = new Color(0x99FF33);
		static final Color COLOR_BG_EDITABLECELL = new Color(0xFFFAE7);
		
		@SuppressWarnings("unused")
		private   final Float columns; // to hide super.columns
		private   final ColumnID[] originalColumns;
		protected final Vector<RowType> rows;
		private   final Vector<RowType> originalRows;
		protected final Window mainWindow;
		protected final Controllers controllers;
		protected final Coloring<RowType> coloring;
		protected Language language;
		private   Comparator<RowType> initialRowOrder;
		private   ColumnID clickedColumn;
		private   int clickedColumnIndex;
	
		VerySimpleTableModel(Window mainWindow, Controllers controllers, ColumnID[] columns) {
			super(columns);
			this.mainWindow = mainWindow;
			this.controllers = controllers;
			this.originalColumns = columns;
			this.columns = null; // to hide super.columns
			language = null;
			initialRowOrder = null;
			rows = new Vector<>();
			originalRows = new Vector<>();
			clickedColumn = null;
			clickedColumnIndex = -1;
			coloring = new Coloring<>(this);
			
			this.controllers.languageListeners.add(this,this);
			
			coloring.setBackgroundColumnColoring(true, Boolean.class, b -> b ? COLOR_BG_TRUE : COLOR_BG_FALSE);
			
			HashSet<String> columnIDIDs = new HashSet<>();
			for (int i=0; i<originalColumns.length; i++) {
				ColumnID columnID = originalColumns[i];
				if (columnIDIDs.contains(columnID.id))
					throw new IllegalStateException(String.format("Found a non unique column ID \"%s\" in column %d in TableModel \"%s\"", columnID.id, i, this.getClass()));
				columnIDIDs.add(columnID.id);
			}
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
		
		void connectToGlobalData(Function<Data,Collection<RowType>> getData) {
			connectToGlobalData(getData, false);
		}
		void connectToGlobalData(Function<Data,Collection<RowType>> getData, boolean forwardNulls) {
			if (getData!=null)
				controllers.dataReceivers.add(this, data -> {
					if (!forwardNulls)
						setData(data==null ? null : getData.apply(data));
					else
						setData(getData.apply(data));
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
	
		final void setData(Collection<RowType> rows) {
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
			
			contextMenu.addContextMenuInvokeListener((table_, x, y) -> {
				int colV = x==null || y==null ? -1 : table_.columnAtPoint(new Point(x,y));
				clickedColumnIndex = colV<0 ? -1 : table_.convertColumnIndexToModel(colV);
				clickedColumn = clickedColumnIndex<0 ? null : super.columns[clickedColumnIndex];
				
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
			});
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
			
			return getValue(columnID, row);
			
		}
		
		Object getValue(ColumnID columnID, RowType row) {
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
			if (columnID.getValueT!=null) {
				return columnID.getValueT.getValue(row,this);
			}
			
			return null;
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
			private final FilterGuiElement[] columnElements;
		
			FilterRowsDialog(Window owner, ColumnID[] originalColumns, String tableModelIDstr) {
				super(owner, "Filter Rows", "Define filters", originalColumns, filterRowsPresets, tableModelIDstr);
				
				JPanel columnPanel = FilterGuiElement.createEmptyColumnPanel();
				columnElements = new FilterGuiElement[originalColumns.length];
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
					Filter filter = columnElements[i].getFilter();
					
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

			@Override protected void setPresetInGui(HashMap<String, Filter> preset) {
				for (FilterGuiElement elem : columnElements)
					elem.setValues(preset.get(elem.columnID.id));
			}

			@Override protected HashMap<String, Filter> getCopyOfCurrentPreset() {
				HashMap<String, Filter> preset = new HashMap<>();
				for (FilterGuiElement elem : columnElements) {
					Filter filter = elem.getFilter();
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
				
				// TODO Auto-generated method stub
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

				public static Filter parse(String str) {
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
					
					Filter filter = null;
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
					if (!(obj instanceof Filter)) return false;
					Filter other = (Filter) obj;
					if (this.active    !=other.active    ) return false;
					if (this.allowUnset!=other.allowUnset) return false;
					return equals_SubType(other);
				}
				
				@Override final protected Filter clone() {
					Filter filter = clone_SubType();
					filter.active     = active    ;
					filter.allowUnset = allowUnset;
					return filter;
				}

				protected abstract boolean valueMeetsFilter_SubType(Object value);
				protected abstract String toParsableString_SubType();
				protected abstract boolean equals_SubType(Filter other);
				protected abstract Filter clone_SubType();
				
				
				static class EnumFilter<E extends Enum<E>> extends Filter {
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

					@Override protected boolean equals_SubType(Filter other) {
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

					@Override protected Filter clone_SubType() {
						EnumFilter<E> enumFilter = new EnumFilter<>(filterID,valueClass);
						enumFilter.allowedValues.addAll(allowedValues);
						return enumFilter;
					}

					public static Filter parseEnumFilter(String str) {
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


				static class NumberFilter<V extends Number> extends Filter {
					
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

					@Override protected boolean equals_SubType(Filter other) {
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
					protected Filter clone_SubType() {
						return new NumberFilter<>(valueClass, min, max, compare, toParsableString);
					}
					
					public static Filter parseNumberFilter(String str) {
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
				
				
				static class BoolFilter extends Filter {
				
					boolean allowTrues = true;

					@Override protected boolean valueMeetsFilter_SubType(Object value) {
						if (value instanceof Boolean) {
							Boolean v = (Boolean) value;
							return allowTrues == v.booleanValue();
						}
						return false;
					}

					@Override protected boolean equals_SubType(Filter other) {
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

					public static Filter parseBoolFilter(String str) {
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
			
			static class FilterGuiElement {
				private static final HashMap<Class<?>,Supplier<OptionsPanel>> RegisteredOptionsPanels = new HashMap<>();
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
				private final OptionsPanel optionsPanel;
				private final Color defaultBG;

				FilterGuiElement(ColumnID columnID) {
					this.columnID = columnID;
					
					Supplier<OptionsPanel> creator = RegisteredOptionsPanels.get(this.columnID.config.columnClass);
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

				Filter getFilter() {
					return optionsPanel.filter;
				}

				void setValues(Filter filter) {
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
					final Filter filter;
					private final JCheckBox chkbxUnset;

					OptionsPanel(Filter filter) {
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

					void setValues(Filter filter) {
						if (this.filter!=null) {
							this.filter.allowUnset = filter!=null && filter.allowUnset;
							chkbxUnset.setSelected(this.filter.allowUnset);
						}
					}


					static class DummyOptions extends OptionsPanel {
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


					static class EnumOptions<E extends Enum<E>> extends OptionsPanel {
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

						@Override void setValues(Filter filter) {
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

						static <E extends Enum<E>> EnumOptions<E> create( Class<E> valueClass, E[] values) {
							return new OptionsPanel.EnumOptions<>(new Filter.EnumFilter<>(Filter.EnumFilter.getFilterID(valueClass), valueClass), values);
						}
					}


					static class NumberOptions<V extends Number> extends OptionsPanel {
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
						
						static <V extends Number> NumberOptions<V> create(Filter.NumberFilter<V> filter, Function<V,String> toString, Function<String,V> convert, Predicate<V> isOK) {
							return new NumberOptions<V>( filter,
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

						@Override void setValues(Filter filter) {
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


					static class BoolOptions extends OptionsPanel {
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

						@Override void setValues(Filter filter) {
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
		
		}

		private static class ColumnHideDialog extends ModelConfigureDialog<HashSet<String>> {
			private static final long serialVersionUID = 4240161527743718020L;

			private final JCheckBox[] columnElements;
			private final HashSet<String> currentPreset;
			
			ColumnHideDialog(Window owner, ColumnID[] originalColumns, String tableModelIDstr) {
				super(owner, "Visible Columns", "Define visible columns", originalColumns, columnHidePresets, tableModelIDstr);
				
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
		}

		private static abstract class ModelConfigureDialog<Preset> extends JDialog {
			private static final long serialVersionUID = 8159900024537014376L;
			
			private final Window owner;
			private boolean hasChanged;
			private boolean ignorePresetComboBoxEvent;
			private final HashMap<String, Preset> modelPresets;
			protected final ColumnID[] originalColumns;
			private final Vector<String> presetNames;
			private final JComboBox<String> presetComboBox;
			private final PresetMaps<Preset> presetMaps;
			protected final JScrollPane columnScrollPane;

			
			ModelConfigureDialog(Window owner, String title, String columnPanelHeadline, ColumnID[] originalColumns, PresetMaps<Preset> presetMaps, String tableModelID) {
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

		private static class FilterRowsPresets extends PresetMaps<HashMap<String,FilterRowsDialog.Filter>> {

			FilterRowsPresets() {
				super(SnowRunner.AppSettings.ValueKey.FilterRowsPresets, /*SnowRunner.PresetsTestOutputFile,*/ HashMap<String,FilterRowsDialog.Filter>::new);
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

		private static class ColumnHidePresets extends PresetMaps<HashSet<String>> {
		
			ColumnHidePresets() {
				super(SnowRunner.AppSettings.ValueKey.ColumnHidePresets, /*SnowRunner.PresetsTestOutputFile,*/ HashSet<String>::new);
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

		private static abstract class PresetMaps<Preset> {
			
			private final HashMap<String,HashMap<String,Preset>> presets;
			private final SnowRunner.AppSettings.ValueKey settingsKey;
			private final Supplier<Preset> createEmptyPreset;
			private final String pathname;
			
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
				
				String text;
				if (settingsKey!=null)
					text = SnowRunner.settings.getString(settingsKey, null);
				
				else if (pathname!=null)
					try {
						File file = new File(pathname);
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

		static class ColumnID implements Tables.SimplifiedColumnIDInterface {
			
			interface TableModelBasedBuilder<ValueType> {
				ValueType getValue(Object value, VerySimpleTableModel<?> tableModel);
			}
			
			interface LanguageBasedStringBuilder {
				String getValue(Object value, Language language);
			}
			
			final SimplifiedColumnConfig config;
			final String id;
			final Function<Object, ?> getValue;
			final LanguageBasedStringBuilder getValueL;
			final TableModelBasedBuilder<?> getValueT;
			final boolean useValueAsStringID;
			final Integer horizontalAlignment;
			final Color foreground;
			final Color background;
			final String format;
			private boolean isVisible;
			private FilterRowsDialog.Filter filter;
			
			ColumnID(String ID, String name, Class<String> columnClass, int prefWidth, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
				this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, false, null, getValue, null);
			}
			<ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue) {
				this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, getValue, null, null);
			}
			<ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
				this(ID, name, columnClass, prefWidth, null, null, horizontalAlignment, format, useValueAsStringID, null, null, getValue);
			}
			ColumnID(String ID, String name, Class<String> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
				this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, false, null, getValue, null);
			}
			<ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue) {
				this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, getValue, null, null);
			}
			<ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
				this(ID, name, columnClass, prefWidth, foreground, background, horizontalAlignment, format, useValueAsStringID, null, null, getValue);
			}
			<ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Color foreground, Color background, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object,ColumnType> getValue, LanguageBasedStringBuilder getValueL, TableModelBasedBuilder<ColumnType> getValueT) {
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
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}
	}

	static abstract class ExtendedVerySimpleTableModel<RowType> extends VerySimpleTableModel<RowType> implements TextAreaOutputSource {
		
		private Runnable textAreaUpdateMethod;
	
		ExtendedVerySimpleTableModel(Window mainWindow, Controllers controllers, ColumnID[] columns) {
			super(mainWindow, controllers, columns);
			textAreaUpdateMethod = null;
		}
	
		@Override protected void extraUpdate() {
			updateTextArea();
		}

		void updateTextArea() {
			if (textAreaUpdateMethod!=null)
				textAreaUpdateMethod.run();
		}
	
		@Override public void setOutputUpdateMethod(Runnable textAreaUpdateMethod) {
			this.textAreaUpdateMethod = textAreaUpdateMethod;
		}
		
		@Override public String getTextForRow(int rowIndex) {
			RowType row = getRow(rowIndex);
			if (row==null) return "";
			return getTextForRow(row);
		}
	
		protected abstract String getTextForRow(RowType row);
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

	static class SetInstancesTableModel<RowType extends TruckComponent> extends ExtendedVerySimpleTableModel<RowType> {
	
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
	}

	static class AddonCategoriesTableModel extends VerySimpleTableModel<AddonCategories.Category> {

		AddonCategoriesTableModel(Window mainWindow, Controllers controllers) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("ID"         ,"ID"                          ,  String.class, 100, null, null, false, row->((AddonCategories.Category)row).name),
					new ColumnID("Label"      ,"Label"                       ,  String.class, 200, null, null,  true, row->((AddonCategories.Category)row).label_StringID),
					new ColumnID("Icon"       ,"Icon"                        ,  String.class, 130, null, null, false, row->((AddonCategories.Category)row).icon),
					new ColumnID("RequiresOAI","Requires One Addon Installed", Boolean.class, 160, null, null, false, row->((AddonCategories.Category)row).requiresOneAddonInstalled),
			});
			connectToGlobalData(data->data.addonCategories.categories.values());
			setInitialRowOrder(Comparator.<AddonCategories.Category,String>comparing(cat->cat.name));
		}
		
	}

	static class SuspensionsTableModel extends SetInstancesTableModel<Suspension> {

		SuspensionsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 130,   null,      null, false, row->((Suspension)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 220,   null,      null, false, row->((Suspension)row).id),
					new ColumnID("Type"     ,"Type"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).type_StringID),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Suspension)row).name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Suspension)row).description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Suspension)row).price),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Suspension)row).unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Suspension)row).unlockByRank),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Suspension)row).damageCapacity),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Suspension)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.suspensions.values());
		}
	}

	static class WinchesTableModel extends SetInstancesTableModel<Winch> {

		WinchesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"                  ,  String.class, 140,   null,      null, false, row->((Winch)row).setID),
					new ColumnID("ItemID"   ,"Item ID"                 ,  String.class, 160,   null,      null, false, row->((Winch)row).id),
					new ColumnID("Name"     ,"Name"                    ,  String.class, 150,   null,      null,  true, row->((Winch)row).name_StringID),
					new ColumnID("Desc"     ,"Description"             ,  String.class, 150,   null,      null,  true, row->((Winch)row).description_StringID),
					new ColumnID("Price"    ,"Price"                   , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Winch)row).price),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration"   , Boolean.class, 120,   null,      null, false, row->((Winch)row).unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"          , Integer.class,  85, CENTER, "Rank %d", false, row->((Winch)row).unlockByRank),
					new ColumnID("RequEngI" ,"Requires Engine Ignition", Boolean.class, 130,   null,      null, false, row->((Winch)row).isEngineIgnitionRequired),
					new ColumnID("Length"   ,"Length"                  , Integer.class,  50, CENTER,      null, false, row->((Winch)row).length),
					new ColumnID("StrengthM","Strength Multi"          ,   Float.class,  80, CENTER,   "%1.2f", false, row->((Winch)row).strengthMult),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Winch)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.winches.values());
		}
	}

	static class GearboxesTableModel extends SetInstancesTableModel<Gearbox> {

		GearboxesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 180,   null,      null, false, row->((Gearbox)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 140,   null,      null, false, row->((Gearbox)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 110,   null,      null,  true, row->((Gearbox)row).name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Gearbox)row).description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,   null,   "%d Cr", false, row->((Gearbox)row).price),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Gearbox)row).unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Gearbox)row).unlockByRank),
					new ColumnID("GearH"    ,"(H)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsHighGear),
					new ColumnID("GearL"    ,"(L)"                  , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerGear),
					new ColumnID("GearLP"   ,"(L+)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerPlusGear),
					new ColumnID("GearLM"   ,"(L-)"                 , Boolean.class,  35,   null,      null, false, row->((Gearbox)row).existsLowerMinusGear),
					new ColumnID("ManualLG" ,"is Manual Low Gear"   , Boolean.class, 110,   null,      null, false, row->((Gearbox)row).isManualLowGear),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,   null,    "%d R", false, row->((Gearbox)row).damageCapacity),
					new ColumnID("AWDCons"  ,"AWD Consumption Mod." ,   Float.class, 130,   null,   "%1.2f", false, row->((Gearbox)row).awdConsumptionModifier),
					new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).fuelConsumption),
					new ColumnID("IdleFuel" ,"Idle Fuel Modifier"   ,   Float.class, 100,   null,   "%1.2f", false, row->((Gearbox)row).idleFuelModifier),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Gearbox)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.gearboxes.values());
		}
	}

	static class EnginesTableModel extends SetInstancesTableModel<Engine> {
	
		EnginesTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("SetID"    ,"Set ID"               ,  String.class, 160,   null,      null, false, row->((Engine)row).setID),
					new ColumnID("ItemID"   ,"Item ID"              ,  String.class, 190,   null,      null, false, row->((Engine)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 130,   null,      null,  true, row->((Engine)row).name_StringID),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 150,   null,      null,  true, row->((Engine)row).description_StringID),
					new ColumnID("Price"    ,"Price"                , Integer.class,  60,  RIGHT,   "%d Cr", false, row->((Engine)row).price),
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Engine)row).unlockByExploration),
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class,  85, CENTER, "Rank %d", false, row->((Engine)row).unlockByRank),
					new ColumnID("Torque"   ,"Torque"               , Integer.class,  70,   null,      null, false, row->((Engine)row).torque),
					new ColumnID("FuelCons" ,"Fuel Consumption"     ,   Float.class, 100,  RIGHT,   "%1.2f", false, row->((Engine)row).fuelConsumption),
					new ColumnID("DamageCap","Damage Capacity"      , Integer.class, 100,  RIGHT,    "%d R", false, row->((Engine)row).damageCapacity),
					new ColumnID("BrakesDel","Brakes Delay"         ,   Float.class,  70,  RIGHT,   "%1.2f", false, row->((Engine)row).brakesDelay),
					new ColumnID("Respons"  ,"Responsiveness"       ,   Float.class,  90,  RIGHT,   "%1.4f", false, row->((Engine)row).engineResponsiveness),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Engine)row).usableBy, lang)),
			});
			if (connectToGlobalData)
				connectToGlobalData(data->data.engines.values());
		}
	}

	static class TrailersTableModel extends ExtendedVerySimpleTableModel<Trailer> {
		
		private HashMap<String, TruckAddon> truckAddons;
		private HashMap<String, Trailer> trailers;

		TrailersTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("ID"       ,"ID"                   ,  String.class, 230,   null,      null, false, row->((Trailer)row).id),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 200,   null,      null,  true, row->((Trailer)row).name_StringID), 
					new ColumnID("DLC"      ,"DLC"                  ,  String.class,  80,   null,      null, false, row->((Trailer)row).dlcName),
					new ColumnID("InstallSk","Install Socket"       ,  String.class, 130,   null,      null, false, row->((Trailer)row).installSocket),
					new ColumnID("CargoSlts","Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((Trailer)row).cargoSlots),
					new ColumnID("Repairs"  ,"Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((Trailer)row).repairsCapacity),
					new ColumnID("WheelRep","Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((Trailer)row).wheelRepairsCapacity),
					new ColumnID("Fuel"     ,"Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((Trailer)row).fuelCapacity),
					new ColumnID("QuestItm","Is Quest Item"        , Boolean.class,  80,   null,      null, false, row->((Trailer)row).isQuestItem),
					new ColumnID("Price"    ,"Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((Trailer)row).price), 
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((Trailer)row).unlockByExploration), 
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((Trailer)row).unlockByRank), 
					new ColumnID("AttachTyp","Attach Type"          ,  String.class,  70,   null,      null, false, row->((Trailer)row).attachType),
					new ColumnID("Desc"     ,"Description"          ,  String.class, 200,   null,      null,  true, row->((Trailer)row).description_StringID), 
					new ColumnID("ExclCargo","Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->SnowRunner.joinAddonIDs(((Trailer)row).excludedCargoTypes)),
					new ColumnID("RequAddon","Required Addons"      ,  String.class, 150,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((Trailer)row).requiredAddons)),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((Trailer)row).usableBy, lang)),
			});
			
			truckAddons = null;
			trailers    = null;
			if (connectToGlobalData)
				connectToGlobalData(data->{
					truckAddons = data==null ? null : data.truckAddons;
					trailers    = data==null ? null : data.trailers;
					return trailers==null ? null : trailers.values();
				}, true);
			else
				controllers.dataReceivers.add(this,data->{
					truckAddons = data==null ? null : data.truckAddons;
					trailers    = data==null ? null : data.trailers;
					updateTextArea();
				});
			
			setInitialRowOrder(Comparator.<Trailer,String>comparing(row->row.id));
		}
	
		@Override public String getTextForRow(Trailer row) {
			String description_StringID = row.description_StringID;
			String[][] requiredAddons = row.requiredAddons;
			String[] excludedCargoTypes = row.excludedCargoTypes;
			Vector<Truck> usableBy = row.usableBy;
			return generateText(description_StringID, requiredAddons, excludedCargoTypes, usableBy, language, truckAddons, trailers);
		}
	}

	static class TruckAddonsTableModel extends ExtendedVerySimpleTableModel<TruckAddon> {
		
		private static final Color COLOR_SPECIALTRUCKADDON = new Color(0xFFF3AD);
		private HashMap<String, TruckAddon> truckAddons;
		private HashMap<String, Trailer> trailers;
		private TruckAddon clickedItem;
		private final SpecialTruckAddons specialTruckAddons;

		TruckAddonsTableModel(Window mainWindow, Controllers controllers, boolean connectToGlobalData, SpecialTruckAddons specialTruckAddons) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("ID"       ,"ID"                   ,  String.class, 230,   null,      null, false, row->((TruckAddon)row).id),
					new ColumnID("DLC"      ,"DLC"                  ,  String.class,  80,   null,      null, false, row->((TruckAddon)row).dlcName),
					new ColumnID("Category" ,"Category"             ,  String.class, 150,   null,      null, false, row->((TruckAddon)row).category),
					new ColumnID("Name"     ,"Name"                 ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.name_StringID!=null ? row.name_StringID : row.cargoName_StringID; }), 
					new ColumnID("InstallSk","Install Socket"       ,  String.class, 130,   null,      null, false, row->((TruckAddon)row).installSocket),
					new ColumnID("CargoSlts","Cargo Slots"          , Integer.class,  70, CENTER,      null, false, row->((TruckAddon)row).cargoSlots),
					new ColumnID("Repairs"  ,"Repairs"              , Integer.class,  50,  RIGHT,    "%d R", false, row->((TruckAddon)row).repairsCapacity),
					new ColumnID("WheelRep" ,"Wheel Repairs"        , Integer.class,  85, CENTER,   "%d WR", false, row->((TruckAddon)row).wheelRepairsCapacity),
					new ColumnID("Fuel"     ,"Fuel"                 , Integer.class,  50,  RIGHT,    "%d L", false, row->((TruckAddon)row).fuelCapacity),
					new ColumnID("EnAWD"    ,"Enables AWD"          , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).enablesAllWheelDrive), 
					new ColumnID("EnDiffLck","Enables DiffLock"     , Boolean.class,  90,   null,      null, false, row->((TruckAddon)row).enablesDiffLock), 
					new ColumnID("Price"    ,"Price"                , Integer.class,  50,  RIGHT,   "%d Cr", false, row->((TruckAddon)row).price), 
					new ColumnID("UnlExpl"  ,"Unlock By Exploration", Boolean.class, 120,   null,      null, false, row->((TruckAddon)row).unlockByExploration), 
					new ColumnID("UnlRank"  ,"Unlock By Rank"       , Integer.class, 100, CENTER, "Rank %d", false, row->((TruckAddon)row).unlockByRank), 
					new ColumnID("Desc"     ,"Description"          ,  String.class, 200,   null,      null,  true, obj->{ TruckAddon row = (TruckAddon)obj; return row.description_StringID!=null ? row.description_StringID : row.cargoDescription_StringID; }), 
					new ColumnID("ExclCargo","Excluded Cargo Types" ,  String.class, 150,   null,      null, false, row->SnowRunner.joinAddonIDs(((TruckAddon)row).excludedCargoTypes)),
					new ColumnID("RequAddon","Required Addons"      ,  String.class, 200,   null,      null, false, row->SnowRunner.joinRequiredAddonsToString_OneLine(((TruckAddon)row).requiredAddons)),
					new ColumnID("IsCargo"  ,"Is Cargo"             , Boolean.class,  80,   null,      null, false, row->((TruckAddon)row).isCargo),
					new ColumnID("CargLngth","Cargo Length"         , Integer.class,  80, CENTER,      null, false, row->((TruckAddon)row).cargoLength),
					new ColumnID("CargType" ,"Cargo Type"           ,  String.class, 170,   null,      null, false, row->((TruckAddon)row).cargoType),
					new ColumnID("UsableBy" ,"Usable By"            ,  String.class, 150,   null,      null, (row,lang)->SnowRunner.joinTruckNames(((TruckAddon)row).usableBy, lang)),
			});
			this.specialTruckAddons = specialTruckAddons;
			
			clickedItem = null;
			truckAddons = null;
			trailers    = null;
			if (connectToGlobalData)
				connectToGlobalData(data->{
					truckAddons = data==null ? null : data.truckAddons;
					trailers    = data==null ? null : data.trailers;
					return truckAddons==null ? null : truckAddons.values();
				}, true);
			
			else
				controllers.dataReceivers.add(this,data->{
					truckAddons = data==null ? null : data.truckAddons;
					trailers    = data==null ? null : data.trailers;
					updateTextArea();
				});
			
			coloring.addBackgroundRowColorizer(addon->{
				for (List listID : SpecialTruckAddons.List.values()) {
					SpecialTruckAddonList list = this.specialTruckAddons.getList(listID);
					if (list.contains(addon)) return COLOR_SPECIALTRUCKADDON;
				}
				return null;
			});
			controllers.specialTruckAddonsListeners.add(this, (list, change) -> {
				table.repaint();
			});
			
			Comparator<String> string_nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
			setInitialRowOrder(Comparator.<TruckAddon,String>comparing(row->row.category,string_nullsLast).thenComparing(row->row.id));
		}

		@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
			super.modifyTableContextMenu(table_, contextMenu);
			
			contextMenu.addSeparator();
			
			JMenu specialAddonsMenu = new JMenu("Add Addon to List of known special Addons");
			contextMenu.add(specialAddonsMenu);
			
			EnumMap<SpecialTruckAddons.List, JCheckBoxMenuItem> menuItems = new EnumMap<>(SpecialTruckAddons.List.class);
			for (SpecialTruckAddons.List list : SpecialTruckAddons.List.values()) {
				String listLabel = "";
				switch (list) {
				case BigCranes       : listLabel = "Big Crane for howling trucks"; break;
				case MiniCranes      : listLabel = "MiniCrane for loading cargo"; break;
				case LogLifts        : listLabel = "Log Lift for loading logs"; break;
				case MetalDetectors  : listLabel = "Metal Detector for special missions"; break;
				case SeismicVibrators: listLabel = "Seismic Vibrator for special missions"; break;
				}
				
				JCheckBoxMenuItem  mi = SnowRunner.createCheckBoxMenuItem(listLabel, false, null, true, e->{
					if (clickedItem==null) return;
					SpecialTruckAddonList addonList = specialTruckAddons.getList(list);
					if (addonList.contains(clickedItem))
						addonList.remove(clickedItem);
					else
						addonList.add(clickedItem);
				});
				specialAddonsMenu.add(mi);
				menuItems.put(list, mi);
			}
			
			contextMenu.addContextMenuInvokeListener((table, x, y) -> {
				int rowV = x==null || y==null ? -1 : table.rowAtPoint(new Point(x,y));
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedItem = rowM<0 ? null : getRow(rowM);
				
				if (clickedItem!=null)
					menuItems.forEach((list,mi)->{
						SpecialTruckAddonList addonList = specialTruckAddons.getList(list);
						mi.setSelected(addonList.contains(clickedItem));
					});
				
				specialAddonsMenu.setEnabled(clickedItem!=null);
				specialAddonsMenu.setText(
					clickedItem==null
					? "Add Addon to a list of known special Addons"
					: String.format("Add Addon \"%s\" to a list of known special Addons", SnowRunner.solveStringID(clickedItem, language))
				);
			});
		}

		@Override public String getTextForRow(TruckAddon row) {
			String description_StringID = SnowRunner.selectNonNull( row.description_StringID, row.cargoDescription_StringID );
			String[][] requiredAddons = row.requiredAddons;
			String[] excludedCargoTypes = row.excludedCargoTypes;
			Vector<Truck> usableBy = row.usableBy;
			return generateText(description_StringID, requiredAddons, excludedCargoTypes, usableBy, language, truckAddons, trailers);
		}
	}
	
	static class TruckTableModel extends VerySimpleTableModel<Truck> implements SaveGameListener, TruckToDLCAssignmentListener {

		private static final Color COLOR_FG_DLCTRUCK    = new Color(0x0070FF);
		private static final Color COLOR_FG_OWNEDTRUCK  = new Color(0x00AB00);
		private enum Edit { UD_DiffLock, UD_AWD }
		
		private SaveGame saveGame;
		private Truck clickedItem;
		private HashMap<String, String> truckToDLCAssignments;
		private final UserDefinedValues userDefinedValues;

		TruckTableModel(Window mainWindow, Controllers controllers, SpecialTruckAddons specialTruckAddons, UserDefinedValues udv) {
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID( "ID"       , "ID"                   ,               String.class, 160,             null,   null,      null, false, row -> ((Truck)row).id),
					new ColumnID( "DLC"      , "DLC"                  ,               String.class,  80,             null,   null,      null, false, row -> ((Truck)row).dlcName),
					new ColumnID( "Country"  , "Country"              ,      Truck.  Country.class,  50,             null, CENTER,      null, false, row -> ((Truck)row).country),
					new ColumnID( "Type"     , "Type"                 ,      Truck.TruckType.class,  80,             null, CENTER,      null, false, row -> ((Truck)row).type),
					new ColumnID( "Name"     , "Name"                 ,               String.class, 160,             null,   null,      null,  true, row -> ((Truck)row).name_StringID),
					new ColumnID( "Owned"    , "Owned"                ,              Integer.class,  50,             null, CENTER,      null, false, (row,model) -> getOwnedCount((Truck)row,(TruckTableModel)model)), 
					new ColumnID( "DLData"   , "DiffLock (from Data)" ,   Truck.DiffLockType.class, 110,             null, CENTER,      null, false, row -> ((Truck)row).diffLockType),
					new ColumnID( "DLUser"   , "DiffLock (by User)"   ,  Truck.UDV.ItemState.class, 100, Edit.UD_DiffLock, CENTER,      null, false, row -> udv.getTruckValues(((Truck)row).id).realDiffLock),
					new ColumnID( "DLTool"   , "DiffLock (by Tool)"   ,  Truck.UDV.ItemState.class, 100,             null, CENTER,      null, false, row -> getInstState((Truck)row, t->t.hasCompatibleDiffLock, t->t.defaultDiffLock, addon->addon.enablesDiffLock)),
					new ColumnID( "AWDData"  , "AWD (from Data)"      ,               String.class,  95,             null, CENTER,      null, false, row -> "??? t.b.d."),
					new ColumnID( "AWDUser"  , "AWD (by User)"        ,  Truck.UDV.ItemState.class,  85,      Edit.UD_AWD, CENTER,      null, false, row -> udv.getTruckValues(((Truck)row).id).realAWD),
					new ColumnID( "AWDTool"  , "AWD (by Tool)"        ,  Truck.UDV.ItemState.class,  85,             null, CENTER,      null, false, row -> getInstState((Truck)row, t->t.hasCompatibleAWD, t->t.defaultAWD, addon->addon.enablesAllWheelDrive)),
					new ColumnID( "AutoWinch", "AutomaticWinch"       ,              Boolean.class,  90,             null,   null,      null, false, row -> ((Truck)row).hasCompatibleAutomaticWinch),
					new ColumnID( "MetalD"   , "Metal Detector"       ,              Boolean.class,  90,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.metalDetectors   )),
					new ColumnID( "Seismic"  , "Seismic Vibrator"     ,              Boolean.class,  90,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.seismicVibrators )),
					new ColumnID( "BigCrane" , "Big Crane"            ,              Boolean.class,  60,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.bigCranes        )),
					new ColumnID( "MiniCrane", "Mini Crane"           ,              Boolean.class,  60,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.miniCranes       )),
					new ColumnID( "LogLift"  , "Log Lift"             ,              Boolean.class,  50,             null,   null,      null, false, row -> hasCompatibleSpecialAddon( (Truck)row, specialTruckAddons.logLifts         )),
					new ColumnID( "FuelCap"  , "Fuel Capacity"        ,              Integer.class,  80,             null,   null,    "%d L", false, row -> ((Truck)row).fuelCapacity),
					new ColumnID( "WheelSizs", "Wheel Sizes"          ,               String.class,  80,             null,   null,      null, false, row -> joinWheelSizes(((Truck)row).compatibleWheels)),
					new ColumnID( "WheelTyps", "Wheel Types"          ,               String.class, 280,             null,   null,      null, (row,lang) -> getWheelCategories((Truck)row,lang)),
					new ColumnID( "WhHigh"   , "[W] Highway"          ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionHighway)),
					new ColumnID( "WhOffr"   , "[W] Offroad"          ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionOffroad)),
					new ColumnID( "WhMud"    , "[W] Mud"              ,                Float.class,  75,             null,   null,   "%1.2f", false, row -> getMaxWheelValue((Truck)row, tire->tire.frictionMud)),
					new ColumnID( "Price"    , "Price"                ,              Integer.class,  60,             null,   null,   "%d Cr", false, row -> ((Truck)row).price), 
					new ColumnID( "UnlExpl"  , "Unlock By Exploration",              Boolean.class, 120,             null,   null,      null, false, row -> ((Truck)row).unlockByExploration), 
					new ColumnID( "UnlRank"  , "Unlock By Rank"       ,              Integer.class, 100,             null, CENTER, "Rank %d", false, row -> ((Truck)row).unlockByRank), 
					new ColumnID( "Desc"     , "Description"          ,               String.class, 200,             null,   null,      null,  true, row -> ((Truck)row).description_StringID), 
					new ColumnID( "DefEngine", "Default Engine"       ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultEngine, ((Truck)row).defaultEngine_ItemID, lang)),
					new ColumnID( "DefGearbx", "Default Gearbox"      ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultGearbox, ((Truck)row).defaultGearbox_ItemID, lang)),
					new ColumnID( "DefSusp"  , "Default Suspension"   ,               String.class, 110,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultSuspension, ((Truck)row).defaultSuspension_ItemID, lang)),
					new ColumnID( "DefWinch" , "Default Winch"        ,               String.class, 130,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultWinch, ((Truck)row).defaultWinch_ItemID, lang)),
					new ColumnID( "DefDifLck", "Default DiffLock"     ,               String.class,  95,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultDiffLock, lang)),
					new ColumnID( "DefAWD"   , "Default AWD"          ,               String.class,  90,             null,   null,      null, (row,lang) -> SnowRunner.solveStringID(((Truck)row).defaultAWD, lang)),
					new ColumnID( "UpgrWinch", "Upgradable Winch"     ,              Boolean.class, 110,             null,   null,      null, false, row -> ((Truck)row).isWinchUpgradable),
			//		new ColumnID( "MaxWhWoSp", "Max. WheelRadius Without Suspension", String.class, 200,             null,   null,      null, false, row -> ((Truck)row).maxWheelRadiusWithoutSuspension),
					new ColumnID( "Image"    , "Image"                ,               String.class, 130,             null,   null,      null, false, row -> ((Truck)row).image),
			});
			this.userDefinedValues = udv;
			saveGame = null;
			connectToGlobalData(data->data.trucks.values());
			setInitialRowOrder(Comparator.<Truck,String>comparing(truck->truck.id));
			
			
			coloring.addForegroundRowColorizer(truck->{
				if (truck==null)
					return null;
				
				if (saveGame!=null && saveGame.ownedTrucks!=null && saveGame.ownedTrucks.containsKey(truck.id))
					return COLOR_FG_OWNEDTRUCK;
				
				if (truck.dlcName!=null)
					return COLOR_FG_DLCTRUCK;
				
				return null;
			});
			
			coloring.setBackgroundColumnColoring(true, Truck.UDV.ItemState.class, state->{
				switch (state) {
				case None:
					return COLOR_BG_FALSE;
				case Able:
				case Installed:
				case Permanent:
					return COLOR_BG_TRUE;
				}
				return null;
			});
			
			controllers.saveGameListeners.add(this, this);
			controllers.specialTruckAddonsListeners.add(this, (list,change)->{
				String id = null;
				switch (list) {
				case MetalDetectors  : id = "MetalD"; break;
				case SeismicVibrators: id = "Seismic"; break;
				case BigCranes       : id = "BigCrane"; break;
				case LogLifts        : id = "MiniCrane"; break;
				case MiniCranes      : id = "LogLift"; break;
				}
				if (id!=null)
					fireTableColumnUpdate(findColumnByID(id));
			});
			
		}
		
		private static Integer getOwnedCount(Truck truck, TruckTableModel model) {
			if (truck==null) return null;
			if (model==null) return null;
			if (model.saveGame==null) return null;
			if (model.saveGame.ownedTrucks==null) return null;
			return model.saveGame.ownedTrucks.get(truck.id);
		}

		private static Float getMaxWheelValue(Truck truck, Function<TruckTire,Float> getTireValue) {
			Float max = null;
			for (CompatibleWheel cw : truck.compatibleWheels)
				if (cw.wheelsDef!=null)
					for (TruckTire tire : cw.wheelsDef.truckTires) {
						Float value = getTireValue.apply(tire);
						max = max==null ? value : value==null ? max : Math.max(max,value);
					}
			return max;
		}
		
		private static String getWheelCategories(Truck truck, Language language) {
			HashSet<String> tireTypes_StringID = new HashSet<>();
			for (CompatibleWheel cw : truck.compatibleWheels)
				if (cw.wheelsDef!=null)
					for (TruckTire tire : cw.wheelsDef.truckTires)
						if (tire.tireType_StringID!=null)
							tireTypes_StringID.add(tire.tireType_StringID);
			Vector<String> vec = new Vector<>(tireTypes_StringID);
			vec.sort(Comparator.comparing(TruckTire::getTypeOrder).thenComparing(Comparator.naturalOrder()));
			Iterable<String> it = () -> vec.stream().map(stringID->SnowRunner.solveStringID(stringID, language)).iterator();
			return String.join(", ", it);
		}

		private static Truck.UDV.ItemState getInstState(
				Truck truck,
				Function<Truck,Boolean> isAddonCategoryInstallable,
				Function<Truck,TruckAddon> getDefaultAddon,
				Function<TruckAddon,Boolean> addonEnablesFeature
		) {
			if (truck==null) return null;
			
			Boolean bIsAddonInstallable = isAddonCategoryInstallable.apply(truck);
			if (bIsAddonInstallable==null || !bIsAddonInstallable.booleanValue())
				return null; // None || Permanent 
			
			TruckAddon defaultAddon = getDefaultAddon.apply(truck);
			Boolean bAddonEnablesFeature = defaultAddon==null ? false : addonEnablesFeature.apply(defaultAddon);
			if (bAddonEnablesFeature==null || !bAddonEnablesFeature.booleanValue())
				return Truck.UDV.ItemState.Able;
				
			return Truck.UDV.ItemState.Installed;
		}

		private static String joinWheelSizes(CompatibleWheel[] compatibleWheels) {
			HashSet<String> set = Arrays
					.stream(compatibleWheels)
					.<String>map(cw->String.format("%d\"", cw.getSize()))
					.collect(HashSet<String>::new, HashSet<String>::add, HashSet<String>::addAll);
			Vector<String> vector = new Vector<>(set);
			vector.sort(null);
			return String.join(", ", vector);
		}

		private static boolean hasCompatibleSpecialAddon(Truck truck, SpecialTruckAddonList addonList) {
			for (Vector<TruckAddon> list : truck.compatibleTruckAddons.values())
				for (TruckAddon addon : list)
					if (addonList.contains(addon))
						return true;
			return false;
		}

		private static class ColumnID extends VerySimpleTableModel.ColumnID {

			final Edit editMarker;

			private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, boolean useValueAsStringID, Function<Object, ColumnType> getValue) {
				super(ID, name, columnClass, prefWidth, horizontalAlignment, format, useValueAsStringID, getValue);
				this.editMarker = editMarker;
			}

			private <ColumnType> ColumnID(String ID, String name, Class<ColumnType> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, boolean useValueAsStringID, TableModelBasedBuilder<ColumnType> getValue) {
				super(ID, name, columnClass, prefWidth, horizontalAlignment, format, useValueAsStringID, getValue);
				this.editMarker = editMarker;
			}

			private ColumnID(String ID, String name, Class<String> columnClass, int prefWidth, Edit editMarker, Integer horizontalAlignment, String format, LanguageBasedStringBuilder getValue) {
				super(ID, name, columnClass, prefWidth, horizontalAlignment, format, getValue);
				this.editMarker = editMarker;
			}
			
		}

		@Override
		public void reconfigureAfterTableStructureUpdate() {
			super.reconfigureAfterTableStructureUpdate();
			table.setDefaultEditor(
					Truck.UDV.ItemState.class,
					new Tables.ComboboxCellEditor<>(
							SnowRunner.addNull(Truck.UDV.ItemState.values())
					)
			);
		}

		@Override protected boolean isCellEditable(int rowIndex, int columnIndex, VerySimpleTableModel.ColumnID columnID_) {
			ColumnID columnID = (ColumnID)columnID_;
			return columnID.editMarker!=null;
		}

		@Override
		protected void setValueAt(Object aValue, int rowIndex, int columnIndex, VerySimpleTableModel.ColumnID columnID_) {
			ColumnID columnID = (ColumnID)columnID_;
			if (columnID.editMarker==null) return;
			
			Truck row = getRow(rowIndex);
			if (row==null) return;
			
			Truck.UDV values = userDefinedValues.getOrCreateTruckValues(row.id);
			
			switch (columnID.editMarker) {
			case UD_AWD     : values.realAWD      = (Truck.UDV.ItemState)aValue; break;
			case UD_DiffLock: values.realDiffLock = (Truck.UDV.ItemState)aValue; break;
			}
			
			userDefinedValues.write();
		}

		@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
			super.modifyTableContextMenu(table_, contextMenu);
			
			contextMenu.addSeparator();
			
			JMenuItem miAssignToDLC = contextMenu.add(SnowRunner.createMenuItem("Assign truck to an official DLC", true, e->{
				if (clickedItem==null || truckToDLCAssignments==null) return;
				TruckAssignToDLCDialog dlg = new TruckAssignToDLCDialog(mainWindow, clickedItem, language, truckToDLCAssignments);
				boolean assignmentsChanged = dlg.showDialog();
				if (assignmentsChanged)
					controllers.truckToDLCAssignmentListeners.updateAfterAssignmentsChange();
			}));
			
			contextMenu.addContextMenuInvokeListener((table, x, y) -> {
				int rowV = x==null || y==null ? -1 : table.rowAtPoint(new Point(x,y));
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedItem = rowM<0 ? null : getRow(rowM);
				
				miAssignToDLC.setEnabled(clickedItem!=null && truckToDLCAssignments!=null);
				
				miAssignToDLC.setText(
					clickedItem==null
					? "Assign truck to an official DLC"
					: String.format("Assign \"%s\" to an official DLC", SnowRunner.getTruckLabel(clickedItem,language))
				);
			});
			
			controllers.truckToDLCAssignmentListeners.add(this, this);
		}

		@Override public void updateAfterAssignmentsChange() {}
		@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
			this.truckToDLCAssignments = truckToDLCAssignments;
		}

		@Override public void setSaveGame(SaveGame saveGame) {
			this.saveGame = saveGame;
			table.repaint();
		}
		
	}

	static class WheelsTableModel extends VerySimpleTableModel<WheelsTableModel.RowItem> {
	
		WheelsTableModel(Window mainWindow, Controllers controllers) {
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

	static class DLCTableModel extends SimplifiedTableModel<DLCTableModel.ColumnID> implements LanguageListener, TruckToDLCAssignmentListener, DataReceiver, ListenerSource {
	
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
}
