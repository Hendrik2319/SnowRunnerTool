package net.schwarzbaer.java.games.snowrunner;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.ExpandedCompatibleWheel;

class TruckPanel extends JSplitPane {
	private static final long serialVersionUID = -5138746858742450458L;
	
	private final JTextArea topTextArea;
	private final JTextArea allWheelsInfoTextArea;
	private final JTextArea singleWheelInfoTextArea;
	private final JTable compatibleWheelsTable;
	private final CompatibleWheelsTableModel compatibleWheelsTableModel;
	private Language language;
	private Truck truck;
	private ExpandedCompatibleWheel selectedWheel;

	TruckPanel() {
		super(JSplitPane.VERTICAL_SPLIT);
		setResizeWeight(0);

		language = null;
		truck = null;
		selectedWheel = null;
		
		topTextArea = new JTextArea();
		topTextArea.setEditable(false);
		topTextArea.setWrapStyleWord(true);
		topTextArea.setLineWrap(true);
		JScrollPane topTextAreaScrollPane = new JScrollPane(topTextArea);
		topTextAreaScrollPane.setPreferredSize(new Dimension(300,300));
		
		allWheelsInfoTextArea = new JTextArea();
		allWheelsInfoTextArea.setEditable(false);
		allWheelsInfoTextArea.setWrapStyleWord(true);
		allWheelsInfoTextArea.setLineWrap(true);
		JScrollPane wheelsInfoTextAreaScrollPane = new JScrollPane(allWheelsInfoTextArea);
		
		
		compatibleWheelsTableModel = new CompatibleWheelsTableModel();
		compatibleWheelsTable = new JTable(compatibleWheelsTableModel);
		compatibleWheelsTableModel.setTable(compatibleWheelsTable);
		SimplifiedRowSorter rowSorter = new SimplifiedRowSorter(compatibleWheelsTableModel);
		compatibleWheelsTable.setRowSorter(rowSorter);
		compatibleWheelsTableModel.setRenderers();
		compatibleWheelsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		compatibleWheelsTable.getSelectionModel().addListSelectionListener(e->{
			int rowV = compatibleWheelsTable.getSelectedRow();
			int rowM = compatibleWheelsTable.convertRowIndexToModel(rowV);
			selectedWheel = compatibleWheelsTableModel.getRow(rowM);
			updateWheelInfo();
		});
		compatibleWheelsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		compatibleWheelsTableModel.setColumnWidths(compatibleWheelsTable);
		JScrollPane compatibleWheelsTableScrollPane = new JScrollPane(compatibleWheelsTable);
		
		ContextMenu compatibleWheelsTableContextMenu = new ContextMenu();
		compatibleWheelsTableContextMenu.addTo(compatibleWheelsTable);
		
		compatibleWheelsTableContextMenu.add(SnowRunner.createMenuItem("Reset Row Order",true,e->{
			rowSorter.resetSortOrder();
			compatibleWheelsTable.repaint();
		}));
		compatibleWheelsTableContextMenu.add(SnowRunner.createMenuItem("Show Column Widths", true, e->{
			System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(compatibleWheelsTable));
		}));
		
		singleWheelInfoTextArea = new JTextArea();
		singleWheelInfoTextArea.setEditable(false);
		singleWheelInfoTextArea.setWrapStyleWord(true);
		singleWheelInfoTextArea.setLineWrap(true);
		JScrollPane wheelsInfo2TextAreaScrollPane = new JScrollPane(singleWheelInfoTextArea);
		
		JSplitPane condensedCompatibleWheelsInfoPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		condensedCompatibleWheelsInfoPanel.setResizeWeight(0.5);
		condensedCompatibleWheelsInfoPanel.setLeftComponent(compatibleWheelsTableScrollPane);
		condensedCompatibleWheelsInfoPanel.setRightComponent(wheelsInfo2TextAreaScrollPane);
		
		JTabbedPane bottomPanel = new JTabbedPane();
		bottomPanel.addTab("Compatible Wheels (Full Info)", wheelsInfoTextAreaScrollPane);
		bottomPanel.addTab("Compatible Wheels (Condensed Info)", condensedCompatibleWheelsInfoPanel);
		bottomPanel.setSelectedIndex(1);
		
		setTopComponent(topTextAreaScrollPane);
		setBottomComponent(bottomPanel);
		
		updateWheelInfo();
		updateOutput();
	}
	
	void setLanguage(Language language) {
		this.language = language;
		compatibleWheelsTableModel.setLanguage(language);
		updateWheelInfo();
		updateOutput();
	}

	void setTruck(Truck truck) {
		this.truck = truck;
		if (!this.truck.expandedCompatibleWheels.isEmpty()) {
			compatibleWheelsTableModel.setData(this.truck.expandedCompatibleWheels);
		} else
			compatibleWheelsTableModel.setData(null);
		updateWheelInfo();
		updateOutput();
	}

	private void updateOutput() {
		if (truck==null) {
			topTextArea.setText("<NULL>");
			return;
		}
		
		ValueListOutput outTop = new ValueListOutput();
		if (truck.dlcName!=null)
			outTop.add(0, "DLC", truck.dlcName);
		outTop.add(0, "Country", truck.country);
		outTop.add(0, "Price"  , truck.price);
		outTop.add(0, "Type"   , truck.type);
		outTop.add(0, "Unlock By Exploration", truck.unlockByExploration);
		outTop.add(0, "Unlock By Rank"       , truck.unlockByRank);
		outTop.add(0, "XML file"             , truck.xmlName);
		
		String name = null;
		String description = null;
		if (language!=null) {
			name        = language.dictionary.get(truck.name_StringID);
			description = language.dictionary.get(truck.description_StringID);
		}
		outTop.add(0, "Name", "<%s>", truck.name_StringID);
		if (name!=null)
			outTop.add(0, null, name);
		
		outTop.add(0, "Description", "<%s>", truck.description_StringID);
		if (description != null)
			outTop.add(0, null, description);
		
		topTextArea.setText(outTop.generateOutput());
		
		if (!truck.compatibleWheels.isEmpty()) {
			ValueListOutput outFull = new ValueListOutput();
			outFull.add(0, "Compatible Wheels", truck.compatibleWheels.size());
			for (int i=0; i<truck.compatibleWheels.size(); i++) {
				Data.Truck.CompatibleWheel cw = truck.compatibleWheels.get(i);
				outFull.add(1, String.format("[%d]", i+1), "(%s) %s", cw.scale, cw.type);
				cw.printTireList(outFull,2);
			}
			allWheelsInfoTextArea.setText(outFull.generateOutput());
		} else
			allWheelsInfoTextArea.setText("<No Compatible Wheels>");
	}

	private void updateWheelInfo() {
		singleWheelInfoTextArea.setText("");
		if (selectedWheel != null) {
			singleWheelInfoTextArea.append(selectedWheel.type_StringID+"\r\n");
			singleWheelInfoTextArea.append("Description:\r\n");
			String description = null;
			if (language!=null) description = language.dictionary.get(selectedWheel.description_StringID);
			if (description == null) description = String.format("<%s>", selectedWheel.description_StringID);
			singleWheelInfoTextArea.append(description+"\r\n");
		}
	}

	private static class CompatibleWheelsTableCellRenderer implements TableCellRenderer {
	
		private final CompatibleWheelsTableModel tableModel;
		private final Tables.LabelRendererComponent rendererComp;

		public CompatibleWheelsTableCellRenderer(CompatibleWheelsTableModel tableModel) {
			this.tableModel = tableModel;
			rendererComp = new Tables.LabelRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			String valueStr = value==null ? null : value.toString();
			
			int columnM = table.convertColumnIndexToModel(columnV);
			CompatibleWheelsTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			
			if (columnID!=null) {
				if (columnID.config.columnClass==Float.class) {
					valueStr = value==null ? "<???>" : String.format(Locale.ENGLISH, "%1.2f", value);
					rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
				}
				if (columnID.config.columnClass==Integer.class) {
					switch (columnID) {
					case Size:
						valueStr = value==null ? "<???>" : String.format("%d\"", value);
						rendererComp.setHorizontalAlignment(SwingConstants.CENTER);
						break;
					case Price:
						valueStr = value==null ? "<???>" : String.format("%d Cr", value);
						rendererComp.setHorizontalAlignment(SwingConstants.RIGHT);
						break;
					case UnlockByRank:
						valueStr = value==null ? "<???>" : value.toString();
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

	private static class CompatibleWheelsTableModel extends SimplifiedTableModel<CompatibleWheelsTableModel.ColumnID>{
	
		private Vector<ExpandedCompatibleWheel> data;
		private Language language;
	
		enum ColumnID implements SimplifiedColumnIDInterface {
			Type                ("Type"                 , String .class,  80), 
			Name                ("Name"                 , String .class, 130), 
			DLC                 ("DLC"                  , String .class,  80), 
			Size                ("Size"                 , Integer.class,  50), 
			Friction_highway    ("Highway"              , Float  .class,  55), 
			Friction_offroad    ("Offroad"              , Float  .class,  50), 
			Friction_mud        ("Mud"                  , Float  .class,  50), 
			OnIce               ("On Ice"               , Boolean.class,  50), 
			Price               ("Price"                , Integer.class,  50), 
			UnlockByExploration ("Unlock By Exploration", Boolean.class, 120), 
			UnlockByRank        ("Unlock By Rank"       , Integer.class, 100), 
			Description         ("Description"          , String .class, 200), 
			;
	
			private final SimplifiedColumnConfig config;
			ColumnID(String name, Class<?> columnClass, int prefWidth) {
				config = new SimplifiedColumnConfig(name, columnClass, 20, -1, prefWidth, prefWidth);
			}
			@Override public SimplifiedColumnConfig getColumnConfig() { return config; }
		}
	
		CompatibleWheelsTableModel() {
			super(ColumnID.values());
		}
	
		public void setRenderers() {
			CompatibleWheelsTableCellRenderer renderer = new CompatibleWheelsTableCellRenderer(this);
			//table.setDefaultRenderer(String .class, renderer);
			table.setDefaultRenderer(Integer.class, renderer);
			table.setDefaultRenderer(Float  .class, renderer);
			//table.setDefaultRenderer(Boolean.class, null);
		}

		public void setLanguage(Language language) {
			this.language = language;
			fireTableUpdate();
		}

		void setData(Vector<ExpandedCompatibleWheel> data) {
			this.data = new Vector<>(data);
			Comparator<Float >  floatNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
			Comparator<String> stringNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
			Comparator<String> typeComparator = Comparator.nullsLast(Comparator.<String,Integer>comparing(this::getTypeOrder).thenComparing(Comparator.naturalOrder()));
			Comparator<ExpandedCompatibleWheel> comparator = Comparator
					.<ExpandedCompatibleWheel,String>comparing(cw->cw.type_StringID,typeComparator)
					.thenComparing(cw->cw.scale,floatNullsLast)
					.thenComparing(cw->cw.name_StringID,stringNullsLast);
			this.data.sort(comparator);
			fireTableUpdate();
		}
		
		private int getTypeOrder(String type_StringID) {
			if (type_StringID==null) return 0;
			switch (type_StringID) {
			case "UI_TIRE_TYPE_HIGHWAY_NAME"   : return 1;
			case "UI_TIRE_TYPE_ALLTERRAIN_NAME": return 2;
			case "UI_TIRE_TYPE_OFFROAD_NAME"   : return 3;
			case "UI_TIRE_TYPE_MUDTIRES_NAME"  : return 4;
			case "UI_TIRE_TYPE_CHAINS_NAME"    : return 5;
			}
			return 0;
		}

		public ExpandedCompatibleWheel getRow(int rowIndex) {
			if (data==null || rowIndex<0 || rowIndex>=data.size())
				return null;
			return data.get(rowIndex);
		}
	
		@Override public int getRowCount() {
			return data==null ? 0 : data.size();
		}
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex, ColumnID columnID) {
			ExpandedCompatibleWheel row = getRow(rowIndex);
			if (row!=null)
				switch (columnID) {
				case Type       : return getLangString( row.type_StringID );
				case Name       : return getLangString( row.name_StringID );
				case Description: return reducedString( getLangString( row.description_StringID ), 40 );
				case DLC        : return row.dlc;
				case Friction_highway: return row.friction_highway;
				case Friction_offroad: return row.friction_offroad;
				case Friction_mud    : return row.friction_mud;
				case OnIce: return row.onIce;
				case Price: return row.price;
				case Size : return row.scale==null ? null : Math.round(row.scale.floatValue()*78.5f);
				case UnlockByExploration: return row.unlockByExploration;
				case UnlockByRank: return row.unlockByRank;
				}
			return null;
		}

		private String reducedString(String str, int maxLength) {
			if (str.length() > maxLength-4)
				return str.substring(0,maxLength-4)+" ...";
			return str;
		}

		private String getLangString(String stringID) {
			String str = null;
			if (language!=null) str = language.dictionary.get(stringID);
			return str==null ? String.format("<%s>", stringID) : str;
		}
		
	}
}