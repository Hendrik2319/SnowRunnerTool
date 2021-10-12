package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;

public class TableSimplifier {
		
		final SimplifiedTableModel<?> tableModel;
		final JTable table;
		final JScrollPane tableScrollPane;
		final ContextMenu tableContextMenu;
	
		private TableSimplifier(SimplifiedTableModel<?> tableModel) {
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
			
			private Vector<ContextMenu.ContextMenuInvokeListener> listeners = new Vector<>();
			
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
				for (ContextMenu.ContextMenuInvokeListener listener:listeners)
					listener.contextMenuWillBeInvoked(table, x, y);
			}
			
			public void    addContextMenuInvokeListener( ContextMenu.ContextMenuInvokeListener listener ) { listeners.   add(listener); } 
			public void removeContextMenuInvokeListener( ContextMenu.ContextMenuInvokeListener listener ) { listeners.remove(listener); }
			
			public interface ContextMenuInvokeListener {
				public void contextMenuWillBeInvoked(JTable table, Integer x, Integer y);
			}
		}

		public static JComponent create(SimplifiedTableModel<?> tableModel) {
			return create(tableModel, (Consumer<ContextMenu>)null);
		}
		public static JComponent create(SimplifiedTableModel<?> tableModel, Consumer<ContextMenu> modifyContextMenu) {
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

		public static JComponent create(SimplifiedTableModel<?> tableModel, JTextArea outputObj, Function<Runnable,Runnable> modifyUpdateMethod) {
			return create(tableModel, outputObj, modifyUpdateMethod, null);
		}
		public static JComponent create(SimplifiedTableModel<?> tableModel, JTextArea outputObj, Function<Runnable,Runnable> modifyUpdateMethod, Consumer<ContextMenu> modifyContextMenu) {
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

		public static JComponent create(SimplifiedTableModel<?> tableModel, JTextPane outputObj, Function<Runnable,Runnable> modifyUpdateMethod) {
			return create(tableModel, outputObj, modifyUpdateMethod, null);
		}
		public static JComponent create(SimplifiedTableModel<?> tableModel, JTextPane outputObj, Function<Runnable,Runnable> modifyUpdateMethod, Consumer<ContextMenu> modifyContextMenu) {
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

		public static JComponent create(SimplifiedTableModel<?> tableModel, ArbitraryOutputSource arbitraryOutputSource) {
			return create(tableModel, arbitraryOutputSource, null);
		}
		public static JComponent create(SimplifiedTableModel<?> tableModel, ArbitraryOutputSource arbitraryOutputSource, Consumer<ContextMenu> modifyContextMenu) {
			return create(
					tableModel,
					arbitraryOutputSource,
					arbitraryOutputSource::modifyUpdateMethod,
					modifyContextMenu,
					new JLabel(), // dummy
					null // is not needed, because JLabel was given as existing but never used OutputObject
			); 
		}

		public static <OutputObject extends Component> JComponent create(
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

		interface TableContextMenuModifier {
			void modifyTableContextMenu(JTable table, ContextMenu tableContextMenu);
		}

		interface OutputSource<OutputObject extends Component> {
			void setOutputUpdateMethod(Runnable outputUpdateMethod);
			void setContentForRow(OutputObject outputObject, int rowIndex);
		}

		public interface ArbitraryOutputSource extends OutputSource<Component>{
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