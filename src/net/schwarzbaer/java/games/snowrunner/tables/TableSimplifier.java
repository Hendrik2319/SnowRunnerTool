package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultStyledDocument;

import net.schwarzbaer.gui.StyledDocumentInterface;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.ScrollValues;
import net.schwarzbaer.system.ClipboardTools;

public class TableSimplifier {
		
		final SimplifiedTableModel<?> tableModel;
		final JTable table;
		final JScrollPane tableScrollPane;
		final ContextMenu tableContextMenu;
		private String clickedStringValue;
	
		private TableSimplifier(SimplifiedTableModel<?> tableModel) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			this.tableModel = tableModel;
			clickedStringValue = null;
			
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
			JMenuItem miCopyValue = tableContextMenu.add(SnowRunner.createMenuItem("Copy Value",true,e->{
				if (clickedStringValue!=null)
					ClipboardTools.copyToClipBoard(clickedStringValue);
			}));
			tableContextMenu.add(SnowRunner.createMenuItem("Show Column Widths", true, e->{
				System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
			}));
			tableContextMenu.addContextMenuInvokeListener((table, x, y) -> {
				clickedStringValue = null;
				if (x!=null && y!=null) {
					Point point = new Point(x,y);
					int colV = table.columnAtPoint(point);
					int rowV = table.   rowAtPoint(point);
					int colM = colV<0 ? -1 : table.convertColumnIndexToModel(colV);
					int rowM = rowV<0 ? -1 : table.   convertRowIndexToModel(rowV);
					Class<?> columnClass = colM<0 ? null : this.tableModel.getColumnClass(colM);
					Object value = colM<0 || rowM<0 ? null : this.tableModel.getValueAt(rowM, colM);
					if (columnClass==String.class && value!=null)
						clickedStringValue = value.toString();
				}
				miCopyValue.setEnabled(clickedStringValue!=null);
				miCopyValue.setText(
					clickedStringValue==null
					? "Copy Value to ClipBoard"
					: String.format("Copy \"%s\" to ClipBoard", SnowRunner.getReducedString(clickedStringValue, 20))
				);
			});
			
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
			
			boolean createSplitPane = true;
			Boolean putOutputInScrollPane = true;
			SplitOrientation splitOrientation = SplitOrientation.VERTICAL_SPLIT;
			if (tableModel instanceof SplitPaneConfigurator)
			{
				SplitPaneConfigurator configurator = (SplitPaneConfigurator) tableModel;
				createSplitPane       = configurator.createSplitPane();
				putOutputInScrollPane = configurator.putOutputInScrollPane();
				splitOrientation      = configurator.getSplitOrientation();
			}
			
			if (tableModel instanceof TextAreaOutputSource)
				return createTAOS(tableModel, null, createSplitPane, putOutputInScrollPane, splitOrientation, null, modifyContextMenu);
			
			if (tableModel instanceof TextPaneOutputSource)
				return createTPOS(tableModel, null, createSplitPane, putOutputInScrollPane, splitOrientation, null, modifyContextMenu);
			
			if (tableModel instanceof UnspecificOutputSource)
				return create(tableModel, (UnspecificOutputSource) tableModel, modifyContextMenu);
			
			return create(tableModel, null, null, modifyContextMenu, null, false, null, null);
		}

		public static JComponent createTAOS(SimplifiedTableModel<?> tableModel, JTextArea outputObj, boolean createSplitPane, Boolean putOutputInScrollPane, SplitOrientation splitOrientation, Function<Runnable,Runnable> modifyUpdateMethod) {
			return createTAOS(tableModel, outputObj, createSplitPane, putOutputInScrollPane, splitOrientation, modifyUpdateMethod, null);
		}
		public static JComponent createTAOS(SimplifiedTableModel<?> tableModel, JTextArea outputObj, boolean createSplitPane, Boolean putOutputInScrollPane, SplitOrientation splitOrientation, Function<Runnable,Runnable> modifyUpdateMethod, Consumer<ContextMenu> modifyContextMenu) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			
			if (!(tableModel instanceof TextAreaOutputSource))
				throw new IllegalArgumentException();
			
			TextAreaOutputSource outputSource = (TextAreaOutputSource) tableModel;
			
			if (outputObj==null && createSplitPane) {
				outputObj = new JTextArea();
				outputObj.setEditable(false);
				outputObj.setWrapStyleWord(true);
				outputObj.setLineWrap(outputSource.useLineWrap());
				putOutputInScrollPane = true;
			}
			
			return create(
					tableModel, outputSource,
					modifyUpdateMethod, modifyContextMenu, outputObj, createSplitPane, putOutputInScrollPane, splitOrientation);
		}

		public static JComponent createTPOS(SimplifiedTableModel<?> tableModel, JTextPane outputObj, boolean createSplitPane, Boolean putOutputInScrollPane, SplitOrientation splitOrientation, Function<Runnable,Runnable> modifyUpdateMethod) {
			return createTPOS(tableModel, outputObj, createSplitPane, putOutputInScrollPane, splitOrientation, modifyUpdateMethod, null);
		}
		public static JComponent createTPOS(SimplifiedTableModel<?> tableModel, JTextPane outputObj, boolean createSplitPane, Boolean putOutputInScrollPane, SplitOrientation splitOrientation, Function<Runnable,Runnable> modifyUpdateMethod, Consumer<ContextMenu> modifyContextMenu) {
			if (tableModel==null)
				throw new IllegalArgumentException();
			
			if (!(tableModel instanceof TextPaneOutputSource))
				throw new IllegalArgumentException();
			
			TextPaneOutputSource outputSource = (TextPaneOutputSource) tableModel;
			
			if (outputObj==null && createSplitPane) {
				outputObj = new JTextPane();
				outputObj.setEditable(false);
				putOutputInScrollPane = true;
			}
			
			return create(
					tableModel, outputSource,
					modifyUpdateMethod, modifyContextMenu, outputObj, createSplitPane, putOutputInScrollPane, splitOrientation);
		}

		public static JComponent create(SimplifiedTableModel<?> tableModel, UnspecificOutputSource unspecificOutputSource) {
			return create(tableModel, unspecificOutputSource, null);
		}
		public static JComponent create(SimplifiedTableModel<?> tableModel, UnspecificOutputSource unspecificOutputSource, Consumer<ContextMenu> modifyContextMenu) {
			return create(
					tableModel,
					unspecificOutputSource,
					unspecificOutputSource::modifyUpdateMethod,
					modifyContextMenu,
					unspecificOutputSource.createOutputComp(),
					unspecificOutputSource.createSplitPane(),
					unspecificOutputSource.putOutputInScrollPane(),
					unspecificOutputSource.getSplitOrientation()
			);
		}

		public static <OutputObject extends Component> JComponent create(
				SimplifiedTableModel<?> tableModel,
				OutputSource<OutputObject> outputSource,
				Function<Runnable,Runnable> modifyUpdateMethod,
				Consumer<ContextMenu> modifyContextMenu,
				OutputObject output,
				boolean createSplitPane, Boolean putOutputInScrollPane, SplitOrientation splitOrientation) {
			
			if (tableModel==null)
				throw new IllegalArgumentException();
			
			TableSimplifier tableSimplifier = new TableSimplifier(tableModel);
			if (modifyContextMenu!=null)
				modifyContextMenu.accept(tableSimplifier.tableContextMenu);
			
			
			JScrollPane outputScrollPane = null;
			JComponent result = tableSimplifier.tableScrollPane;
			if (createSplitPane)
			{
				if (output==null)
					throw new IllegalArgumentException();
				if (putOutputInScrollPane==null)
					throw new IllegalArgumentException();
				if (splitOrientation==null)
					throw new IllegalArgumentException();
				
				
				JSplitPane panel = new JSplitPane(splitOrientation.value, true);
				panel.setResizeWeight(1);
				panel.setTopComponent(tableSimplifier.tableScrollPane);
				
				if (putOutputInScrollPane)
				{
					outputScrollPane = new JScrollPane(output);
					outputScrollPane.setPreferredSize(new Dimension(100,100));
					panel.setBottomComponent(outputScrollPane);
				}
				else
					panel.setBottomComponent(output);
				
				result = panel;
			}
			
			
			if (outputSource!=null)
			{
				JScrollPane outputScrollPane_final = outputScrollPane;
				Runnable outputUpdateMethod = ()->{
					int selectedRow = -1;
					int rowV = tableSimplifier.table.getSelectedRow();
					if (rowV>=0) {
						int rowM = tableSimplifier.table.convertRowIndexToModel(rowV);
						selectedRow = rowM<0 ? -1 : rowM;
					}
					outputSource.setOutputContentForRow(output, outputScrollPane_final, selectedRow);
				};
				if (modifyUpdateMethod!=null)
					outputUpdateMethod = modifyUpdateMethod.apply(outputUpdateMethod);
				outputSource.setOutputUpdateMethod(outputUpdateMethod);
				
				Runnable outputUpdateMethod_final = outputUpdateMethod;
				tableSimplifier.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				tableSimplifier.table.getSelectionModel().addListSelectionListener(e->outputUpdateMethod_final.run());
			}
			
			return result;
		}

		public enum SplitOrientation {
			  VERTICAL_SPLIT(JSplitPane.  VERTICAL_SPLIT),
			HORIZONTAL_SPLIT(JSplitPane.HORIZONTAL_SPLIT);
			private final int value;
			SplitOrientation(int value) { this.value = value; }
		}

		interface SplitPaneConfigurator {
			boolean createSplitPane();
			Boolean putOutputInScrollPane();
			SplitOrientation getSplitOrientation();
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
			void setOutputContentForRow(OutputObject outputObject, JScrollPane outputScrollPane, int rowIndex);
		}

		public interface UnspecificOutputSource extends OutputSource<Component>, SplitPaneConfigurator {
			@Override default void setOutputUpdateMethod(Runnable outputUpdateMethod) {}
			@Override default void setOutputContentForRow(Component dummy, JScrollPane outputScrollPane, int rowIndex) { setOutputContentForRow(rowIndex); }
			void setOutputContentForRow(int rowIndex);
			default Runnable modifyUpdateMethod(Runnable updateMethod) { return updateMethod; }
			default Component createOutputComp() { return null; }
			@Override default boolean createSplitPane() { return false; }
			@Override default Boolean putOutputInScrollPane() { return null; }
			@Override default SplitOrientation getSplitOrientation() { return null; }
		}

		interface TextAreaOutputSource extends OutputSource<JTextArea> {
			@Override default void setOutputContentForRow(JTextArea textArea, JScrollPane outputScrollPane, int rowIndex) {
				ScrollValues scrollPos = ScrollValues.getVertical(outputScrollPane);
				if (rowIndex<0)
					textArea.setText("");
				else
					textArea.setText(getOutputTextForRow(rowIndex));
				if (scrollPos!=null) SwingUtilities.invokeLater(()->scrollPos.setVertical(outputScrollPane));
			}
			String getOutputTextForRow(int rowIndex);
			default boolean useLineWrap() { return true; }
		}

		interface TextPaneOutputSource extends OutputSource<JTextPane> {
			@Override default void setOutputContentForRow(JTextPane textPane, JScrollPane outputScrollPane, int rowIndex) {
				ScrollValues scrollPos = ScrollValues.getVertical(outputScrollPane);
				DefaultStyledDocument doc = new DefaultStyledDocument();
				if (rowIndex>=0) setOutputContentForRow(new StyledDocumentInterface(doc, "TextPaneOutput", null, 12), rowIndex);
				textPane.setStyledDocument(doc);
				if (scrollPos!=null) SwingUtilities.invokeLater(()->scrollPos.setVertical(outputScrollPane));
			}
			void setOutputContentForRow(StyledDocumentInterface doc, int rowIndex);
		}
		
	}