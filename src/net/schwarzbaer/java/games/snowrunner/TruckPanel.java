package net.schwarzbaer.java.games.snowrunner;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedColumnIDInterface;
import net.schwarzbaer.gui.Tables.SimplifiedRowSorter;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;
import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.gui.ZoomableCanvas;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.ExpandedCompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TruckToDLCAssignmentListener;

class TruckPanel extends JSplitPane implements LanguageListener, TruckToDLCAssignmentListener {
	private static final long serialVersionUID = -5138746858742450458L;
	
	private final StandardMainWindow mainWindow;
	private final JTextArea topTextArea;
	private final JTextArea allWheelsInfoTextArea;
	private final JTextArea singleWheelInfoTextArea;
	private final JTable compatibleWheelsTable;
	private final CompatibleWheelsTableModel compatibleWheelsTableModel;
	private Language language;
	private Truck truck;
	private ExpandedCompatibleWheel selectedWheel;
	private HashMap<String, String> truckToDLCAssignments;

	TruckPanel(StandardMainWindow mainWindow) {
		super(JSplitPane.VERTICAL_SPLIT);
		this.mainWindow = mainWindow;
		setResizeWeight(0);

		language = null;
		truck = null;
		selectedWheel = null;
		truckToDLCAssignments = null;
		
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
		JScrollPane allWheelsInfoTextAreaScrollPane = new JScrollPane(allWheelsInfoTextArea);
		
		
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
		JScrollPane singleWheelInfoTextAreaScrollPane = new JScrollPane(singleWheelInfoTextArea);
		
		JPanel tableButtonsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0; tableButtonsPanel.add(SnowRunner.createButton("Show Wheel Data in Diagram", true, e->{
			if (compatibleWheelsTableModel.data!=null)
				new WheelsDiagramDialog(this.mainWindow, compatibleWheelsTableModel.data, language).showDialog();
		}),c);
		c.weightx = 1; tableButtonsPanel.add(new JLabel(),c);
		
		JPanel compatibleWheelsPanel = new JPanel(new BorderLayout());
		compatibleWheelsPanel.add(compatibleWheelsTableScrollPane, BorderLayout.CENTER);
		compatibleWheelsPanel.add(tableButtonsPanel, BorderLayout.SOUTH);
		
		JSplitPane condensedCompatibleWheelsInfoPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		condensedCompatibleWheelsInfoPanel.setResizeWeight(0.5);
		condensedCompatibleWheelsInfoPanel.setLeftComponent(compatibleWheelsPanel);
		condensedCompatibleWheelsInfoPanel.setRightComponent(singleWheelInfoTextAreaScrollPane);
		
		JTabbedPane bottomPanel = new JTabbedPane();
		bottomPanel.addTab("Compatible Wheels (Full Info)", allWheelsInfoTextAreaScrollPane);
		bottomPanel.addTab("Compatible Wheels (Condensed Info)", condensedCompatibleWheelsInfoPanel);
		bottomPanel.setSelectedIndex(1);
		
		setTopComponent(topTextAreaScrollPane);
		setBottomComponent(bottomPanel);
		
		updateWheelInfo();
		updateOutput();
	}
	
	@Override public void setLanguage(Language language) {
		this.language = language;
		compatibleWheelsTableModel.setLanguage(language);
		updateWheelInfo();
		updateOutput();
	}

	void setTruck(Truck truck) {
		this.truck = truck;
		if (this.truck!=null && !this.truck.expandedCompatibleWheels.isEmpty()) {
			compatibleWheelsTableModel.setData(this.truck.expandedCompatibleWheels);
		} else
			compatibleWheelsTableModel.setData(null);
		updateWheelInfo();
		updateOutput();
	}

	@Override public void updateAfterAssignmentsChange() {
		updateWheelInfo();
		updateOutput();
	}

	@Override public void setTruckToDLCAssignments(HashMap<String, String> truckToDLCAssignments) {
		this.truckToDLCAssignments = truckToDLCAssignments;
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
			outTop.add(0, "Internal DLC", truck.dlcName);
		
		if (truckToDLCAssignments!=null && truck.xmlName!=null) {
			String dlc = truckToDLCAssignments.get(truck.xmlName);
			if (dlc!=null)
				outTop.add(0, "Official DLC", dlc);
		}
		
		outTop.add(0, "Country", truck.country);
		outTop.add(0, "Price"  , truck.price);
		outTop.add(0, "Type"   , truck.type);
		outTop.add(0, "Unlock By Exploration", truck.unlockByExploration);
		outTop.add(0, "Unlock By Rank"       , truck.unlockByRank);
		outTop.add(0, "XML file"             , truck.xmlName);
		
		outTop.add(0, "");
		
		String name = null;
		String description = null;
		if (language!=null) {
			name        = language.dictionary.get(truck.name_StringID);
			description = language.dictionary.get(truck.description_StringID);
		}
		outTop.add(0, "Name", "<%s>", truck.name_StringID);
		if (name!=null)
			outTop.add(0, null, name);
		
		outTop.add(0, "");
		
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
			String description = SnowRunner.solveStringID(selectedWheel.description_StringID, language);
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
			this.data = new Vector<>();
			
			if (data!=null) {
				this.data.addAll(data);
				Comparator<Float >  floatNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
				Comparator<String> stringNullsLast = Comparator.nullsLast(Comparator.naturalOrder());
				Comparator<String> typeComparator = Comparator.nullsLast(Comparator.<String,Integer>comparing(this::getTypeOrder).thenComparing(Comparator.naturalOrder()));
				Comparator<ExpandedCompatibleWheel> comparator = Comparator
						.<ExpandedCompatibleWheel,String>comparing(cw->cw.type_StringID,typeComparator)
						.thenComparing(cw->cw.scale,floatNullsLast)
						.thenComparing(cw->cw.name_StringID,stringNullsLast);
				this.data.sort(comparator);
			}
			
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
				case Friction_highway: return row.frictionHighway;
				case Friction_offroad: return row.frictionOffroad;
				case Friction_mud    : return row.frictionMud;
				case OnIce: return row.onIce;
				case Price: return row.price;
				case Size : return row.getSize();
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
			return SnowRunner.solveStringID(stringID, language);
		}
		
	}

	private static class WheelsDiagram extends ZoomableCanvas<WheelsDiagram.ViewState> {
	
		private static final long serialVersionUID = 4384634067065873277L;
		private static final Color COLOR_AXIS = new Color(0x70000000,true);
		private static final Color COLOR_CONTOUR = Color.BLACK;
		private static final Color COLOR_FILL = Color.YELLOW;
		private static final Color COLOR_FILL_PARETO = new Color(0x00C6FF);
		private static final Color COLOR_FILL_HOVERED = Color.GREEN;
		private static final Color COLOR_DIAGRAM_BACKGROUND = Color.WHITE;
		private static final Color COLOR_TEXTBOX_BACKGROUND = new Color(0xFFFFDD);
		private static final Color COLOR_TEXT = Color.BLACK;
		private static final Color COLOR_TEXT_NOTPARETO = new Color(0x7F7F7F);
		
		private Vector<DataPoint> dataPoints;
		private Float minX;
		private Float maxX;
		private Float minY;
		private Float maxY;
		private final HashMap<DataPoint, TextBox> textBoxes;
		private final HashMap<DataPoint, Point> posMarkers;
		private final HashSet<DataPoint> paretoSet;
		private DataPoint hoveredDataPoint;
		
		WheelsDiagram() {
			textBoxes = new HashMap<>();
			posMarkers = new HashMap<>();
			paretoSet = new HashSet<>();
			setData(null,true,true);
			activateMapScale(COLOR_AXIS, "units", false);
			activateAxes(COLOR_AXIS, true,true,true,true);
		}
	
		void setData(Vector<DataPoint> dataPoints, boolean isXPositiveBetter, boolean isYPositiveBetter) {
			textBoxes.clear();
			posMarkers.clear();
			paretoSet.clear();
			hoveredDataPoint = null;
			
			this.dataPoints = dataPoints;
			
			minX = null;
			maxX = null;
			minY = null;
			maxY = null;
			if (dataPoints!=null) {
				paretoSet.addAll(dataPoints);
				for (DataPoint dataPoint:dataPoints) {
					minX = minX==null ? dataPoint.x : Math.min(minX, dataPoint.x); 
					minY = minY==null ? dataPoint.y : Math.min(minY, dataPoint.y); 
					maxX = maxX==null ? dataPoint.x : Math.max(maxX, dataPoint.x); 
					maxY = maxY==null ? dataPoint.y : Math.max(maxY, dataPoint.y);
					
					for (DataPoint paretoDataPoint:paretoSet)
						if (isBelow(dataPoint, paretoDataPoint, isXPositiveBetter, isYPositiveBetter)) {
							paretoSet.remove(dataPoint);
							break;
						}
				}
			}
			reset();
		}

		private boolean isBelow(DataPoint dataPoint, DataPoint paretoDataPoint, boolean isXPositiveBetter, boolean isYPositiveBetter) {
			float x1 = dataPoint.x;
			float y1 = dataPoint.y;
			float x2 = paretoDataPoint.x;
			float y2 = paretoDataPoint.y;
			
			if (x1==x2 && y1==y2) return false;
			if ((isXPositiveBetter && x1>x2) || (!isXPositiveBetter && x1<x2)) return false;
			if ((isYPositiveBetter && y1>y2) || (!isYPositiveBetter && y1<y2)) return false;
			return true;
		}

		private boolean isOver(int x, int y, DataPoint dataPoint) {
			if (dataPoint==null) return false;
			
			Point p = posMarkers.get(dataPoint);
			int dist_squared = (x-p.x)*(x-p.x) + (y-p.y)*(y-p.y);
			if (dist_squared < 10*10) return true;
			
			TextBox textBox = textBoxes.get(dataPoint);
			return textBox.boxRect.contains( x-textBox.textBoxBaseX_px, y-textBox.textBoxBaseY_px );
		}

		private DataPoint findNextDataPoint(int x, int y) {
			for (DataPoint dataPoint: dataPoints)
				if (isOver(x, y, dataPoint))
					return dataPoint;
			return null;
		}

		@Override public void mouseMoved(MouseEvent e) {
			if (!isOver(e.getX(),e.getY(),hoveredDataPoint)) {
				hoveredDataPoint = findNextDataPoint(e.getX(),e.getY());
				repaint();
			}
		}

		@Override public void mouseEntered(MouseEvent e) {
			hoveredDataPoint = findNextDataPoint(e.getX(),e.getY());
			repaint();
		}

		@Override public void mouseExited(MouseEvent e) {
			hoveredDataPoint = null;
			repaint();
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			if (!(g instanceof Graphics2D || !viewState.isOk()))
				return;
			
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
			
			Shape origClip = g2.getClip();
			g2.setClip(x, y, width, height);
			
			if (dataPoints!=null && minX!=null && maxX!=null && minY!=null && maxY!=null) {
				drawDiagram(g2, x, y);
				
				for (DataPoint dataPoint:dataPoints)
					if (dataPoint!=hoveredDataPoint)
						drawDataPointTextBox(g2,x,y,dataPoint);
				
				for (DataPoint dataPoint:dataPoints)
					if (dataPoint!=hoveredDataPoint)
						drawDataPointPosMarker(g2,x,y,dataPoint);
				
				if (hoveredDataPoint!=null) {
					drawDataPointTextBox(g2,x,y,hoveredDataPoint);
					drawDataPointPosMarker(g2,x,y,hoveredDataPoint);
				}
			}
			
			drawMapDecoration(g2, x, y, width, height);
			
			g2.setClip(origClip);
		}

		private void drawDiagram(Graphics2D g2, int x, int y) {
			float diagramMinX_u = Math.min(minX, 0);
			float diagramMinY_u = Math.min(minY, 0);
			float diagramMaxX_u = Math.max(maxX, 0);
			float diagramMaxY_u = Math.max(maxY, 0);
			
			int zeroX_px = x+viewState.convertPos_AngleToScreen_LongX(0);
			int zeroY_px = y+viewState.convertPos_AngleToScreen_LatY (0);
			int diagramMinX_px = x+viewState.convertPos_AngleToScreen_LongX(diagramMinX_u);
			int diagramMinY_px = y+viewState.convertPos_AngleToScreen_LatY (diagramMinY_u);
			int diagramMaxX_px = x+viewState.convertPos_AngleToScreen_LongX(diagramMaxX_u);
			int diagramMaxY_px = y+viewState.convertPos_AngleToScreen_LatY (diagramMaxY_u);
			int diagramWidth_px  = diagramMaxX_px-diagramMinX_px;
			int diagramHeight_px = diagramMinY_px-diagramMaxY_px; // pos. Y upwards
			
			g2.setColor(COLOR_DIAGRAM_BACKGROUND);
			g2.fillRect(diagramMinX_px-25, diagramMaxY_px-25, diagramWidth_px+50, diagramHeight_px+50);
			
			Stroke origStroke = g2.getStroke();
			g2.setStroke(new BasicStroke(2f));
			g2.setColor(COLOR_AXIS);
			g2.drawLine(diagramMinX_px-20, zeroY_px  , diagramMaxX_px+20, zeroY_px);
			g2.drawLine(diagramMaxX_px+5 , zeroY_px+5, diagramMaxX_px+20, zeroY_px);
			g2.drawLine(diagramMaxX_px+5 , zeroY_px-5, diagramMaxX_px+20, zeroY_px);
			g2.drawLine(zeroX_px  , diagramMinY_px+20, zeroX_px, diagramMaxY_px-20);
			g2.drawLine(zeroX_px+5, diagramMaxY_px-5 , zeroX_px, diagramMaxY_px-20);
			g2.drawLine(zeroX_px-5, diagramMaxY_px-5 , zeroX_px, diagramMaxY_px-20);
			g2.setStroke(origStroke);
		}
		
		private void drawDataPointTextBox(Graphics2D g2, int x, int y, DataPoint dataPoint) {
			int dataPointX_px = x+viewState.convertPos_AngleToScreen_LongX(dataPoint.x);
			int dataPointY_px = y+viewState.convertPos_AngleToScreen_LatY (dataPoint.y);
			TextBox textBox = textBoxes.get(dataPoint);
			if (textBox==null) {
				textBox = new TextBox(str->getStringBounds(g2, str), dataPoint.getTextBox());
				textBoxes.put(dataPoint, textBox);
			}
			textBox.draw(g2, x, y, dataPointX_px, dataPointY_px, dataPoint==hoveredDataPoint, paretoSet.contains(dataPoint));
		}

		private Rectangle getStringBounds(Graphics2D g2, String str) {
			return g2.getFontMetrics().getStringBounds(str, g2).getBounds();
		}
		
		private void drawDataPointPosMarker(Graphics2D g2, int x, int y, DataPoint dataPoint) {
			Point p = posMarkers.get(dataPoint);
			if (p == null)
				posMarkers.put(dataPoint, p = new Point());
			p.x = x+viewState.convertPos_AngleToScreen_LongX(dataPoint.x);
			p.y = y+viewState.convertPos_AngleToScreen_LatY (dataPoint.y);
			Color fillColor = dataPoint==hoveredDataPoint ? COLOR_FILL_HOVERED : paretoSet.contains(dataPoint) ? COLOR_FILL_PARETO : COLOR_FILL;
			drawFilledCircle(g2, p.x, p.y, 3, fillColor, COLOR_CONTOUR);
		}

		private void drawFilledCircle(Graphics2D g2, int x, int y, int radius, Color fillColor, Color contourColor) {
			g2.setColor(fillColor);
			g2.fillOval(x-radius, y-radius, radius*2+1, radius*2+1);
			g2.setColor(contourColor);
			g2.drawOval(x-radius, y-radius, radius*2, radius*2);
		}

		@Override
		protected ViewState createViewState() {
			return new ViewState();
		}
		
		class ViewState extends ZoomableCanvas.ViewState {
		
			private ViewState() {
				super(WheelsDiagram.this, 0.1f);
				setPlainMapSurface();
				//setVertAxisDownPositive(true);
				//debug_showChanges_scalePixelPerLength = true;
			}
			
			@Override
			protected void determineMinMax(MapLatLong min, MapLatLong max) {
				min.longitude_x = minX==null ? 0   : Math.min(minX, 0);
				min.latitude_y  = minY==null ? 0   : Math.min(minY, 0);
				max.longitude_x = maxX==null ? 100 : Math.max(maxX, 0);
				max.latitude_y  = maxY==null ? 100 : Math.max(maxY, 0);
				float overSize = Math.max(max.longitude_x-min.longitude_x, max.latitude_y-min.latitude_y)*0.1f;
				min.longitude_x -= overSize;
				min.latitude_y  -= overSize;
				max.longitude_x += overSize;
				max.latitude_y  += overSize;
			}
		
		}
		
		private static class TextBox {
			
			private final static int BorderX = 3;
			private final static int BorderY = 1;
			
			private final String[] text;
			private final int[] xOffsets;
			private final int[] yOffsets;
			private final Rectangle boxRect;
			private int textBoxBaseX_px;
			private int textBoxBaseY_px;
		
			TextBox(Function<String,Rectangle> getStringBounds, String[] text) {
				this.text = text;
				Rectangle[] stringBounds = new Rectangle[text.length];
				yOffsets = new int[text.length];
				xOffsets = new int[text.length];
				int rowOffset = 0;
				Rectangle stringBoundsTotal = null; 
				
				for (int i=text.length-1; i>=0; i--) {
					stringBounds[i] = getStringBounds.apply(text[i]);
					//stringBounds[i] = g2.getFontMetrics().getStringBounds(textBox[i], g2).getBounds();
					rowOffset -= stringBounds[i].height;
					xOffsets[i] = -stringBounds[i].x;
					yOffsets[i] = -stringBounds[i].y+rowOffset;
					stringBounds[i].x = 0;
					stringBounds[i].y = rowOffset;
					if (i==text.length-1) stringBoundsTotal = new Rectangle(stringBounds[i]);
					else                     stringBoundsTotal.add(stringBounds[i]);
				}
				
				boxRect = new Rectangle( stringBoundsTotal );
				boxRect.y      -= 2*BorderY;
				boxRect.width  += 2*BorderX;
				boxRect.height += 2*BorderY;
				
				textBoxBaseX_px = 0;
				textBoxBaseY_px = 0;
			}
			
			void draw(Graphics2D g2, int x, int y, int dataPointX_px, int dataPointY_px, boolean isHovered, boolean isInParetoSet) {
				textBoxBaseX_px = dataPointX_px+20;
				textBoxBaseY_px = dataPointY_px-10;
				
				if (isHovered) {
					g2.setColor(COLOR_TEXTBOX_BACKGROUND);
					g2.fillRect(textBoxBaseX_px+boxRect.x, textBoxBaseY_px+boxRect.y, boxRect.width, boxRect.height);
				}
				g2.setColor(isHovered || isInParetoSet ? COLOR_CONTOUR : COLOR_TEXT_NOTPARETO);
				g2.drawLine(dataPointX_px, dataPointY_px, textBoxBaseX_px, textBoxBaseY_px);
				g2.drawRect(textBoxBaseX_px+boxRect.x, textBoxBaseY_px+boxRect.y, boxRect.width-1, boxRect.height-1);
				
				for (int i=0; i<text.length; i++) {
					g2.setColor(isHovered || isInParetoSet ? COLOR_TEXT : COLOR_TEXT_NOTPARETO);
					g2.drawString(text[i], textBoxBaseX_px+xOffsets[i]+BorderX, textBoxBaseY_px+yOffsets[i]-BorderY);
				}
			}
			
		}

		private static class DataPoint {
			
			final float x,y;
			final HashMap<String,HashSet<Integer>> wheels;
			
			DataPoint(float x, float y) {
				this.x = x;
				this.y = y;
				wheels = new HashMap<>();
			}
			
			void add(ExpandedCompatibleWheel wheel, Language language) {
				String name = SnowRunner.solveStringID(wheel.name_StringID, language);
				Integer size = wheel.getSize();
				
				HashSet<Integer> sizes = wheels.get(name);
				if (sizes==null) wheels.put(name, sizes = new HashSet<>());
				if (size!=null) sizes.add(size);
			}

			String[] getTextBox() {
				Vector<String> names = new Vector<>(wheels.keySet());
				names.sort(null);
				String[] texts = new String[names.size()];
				
				for (int i=0; i<names.size(); i++) {
					String name = names.get(i);
					texts[i] = name;
					
					HashSet<Integer> sizes = wheels.get(name);
					if (!sizes.isEmpty()) {
						Vector<Integer> sizesVec = new Vector<>(sizes);
						sizesVec.sort(null);
						Iterable<String> it = ()->sizesVec.stream().map(size->size+"\"").iterator();
						texts[i] += String.format(" (%s)", String.join(", ", it));
					}
				}
				return texts;
			}
		}
	}
	
	private static class WheelsDiagramDialog extends JDialog {
		private static final long serialVersionUID = 1414536465711827973L;

		private enum AxisValue { Highway, Offroad, Mud }
		private enum GuiObjs { HorizAxesHighway, HorizAxesOffroad, HorizAxesMud, VertAxesHighway, VertAxesOffroad, VertAxesMud }

		private AxisValue horizAxis;
		private AxisValue vertAxis;
		private final Disabler<GuiObjs> disabler;
		private final Vector<ExpandedCompatibleWheel> data;
		private final WheelsDiagram diagramView;
		private final Language language;
		
		WheelsDiagramDialog(Window owner, Vector<ExpandedCompatibleWheel> data, Language language) {
			super(owner, ModalityType.APPLICATION_MODAL);
			this.data = data;
			this.language = language;
			
			horizAxis = AxisValue.Offroad;
			vertAxis  = AxisValue.Mud;
			diagramView = new WheelsDiagram();
			diagramView.setPreferredSize(700, 600);
			
			disabler = new Disabler<GuiObjs>();
			disabler.setCareFor(GuiObjs.values());
			
			JPanel optionsPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0; c.gridwidth = 1;
			ButtonGroup bgh = new ButtonGroup();
			optionsPanel.add(new JLabel("Horizontal Axis: "), c);
			optionsPanel.add(createRadioH(GuiObjs.HorizAxesHighway, "Highway", bgh, AxisValue.Highway), c);
			optionsPanel.add(createRadioH(GuiObjs.HorizAxesOffroad, "Offroad", bgh, AxisValue.Offroad), c);
			optionsPanel.add(createRadioH(GuiObjs.HorizAxesMud    , "Mud"    , bgh, AxisValue.Mud    ), c);
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			optionsPanel.add(new JLabel(), c);
			
			c.weightx = 0; c.gridwidth = 1;
			ButtonGroup bgv = new ButtonGroup();
			optionsPanel.add(new JLabel("Vertical Axis: "), c);
			optionsPanel.add(createRadioV(GuiObjs.VertAxesHighway, "Highway", bgv, AxisValue.Highway), c);
			optionsPanel.add(createRadioV(GuiObjs.VertAxesOffroad, "Offroad", bgv, AxisValue.Offroad), c);
			optionsPanel.add(createRadioV(GuiObjs.VertAxesMud    , "Mud"    , bgv, AxisValue.Mud    ), c);
			c.weightx = 1; c.gridwidth = GridBagConstraints.REMAINDER;
			optionsPanel.add(new JLabel(), c);
			
			JPanel contentPane = new JPanel(new BorderLayout());
			contentPane.add(optionsPanel,BorderLayout.NORTH);
			contentPane.add(diagramView,BorderLayout.CENTER);
			
			setContentPane(contentPane);
			pack();
			setLocationRelativeTo(owner);
			
			updateGuiAccess();
			updateDiagram();
		}

		private JRadioButton createRadioV( GuiObjs go, String title, ButtonGroup bg, AxisValue axisValue) {
			return SnowRunner.createRadioButton(title, bg, true, vertAxis == axisValue, disabler, go, e->{ vertAxis = axisValue; updateGuiAccess(); updateDiagram(); });
		}

		private JRadioButton createRadioH(GuiObjs go, String title, ButtonGroup bg, AxisValue axisValue) {
			return SnowRunner.createRadioButton(title, bg, true, horizAxis == axisValue, disabler, go, e->{ horizAxis = axisValue; updateGuiAccess(); updateDiagram(); });
		}
		
		void showDialog() {
			setVisible(true);
		}

		private void updateGuiAccess() {
			disabler.setEnable(go->{
				switch (go) {
				case HorizAxesHighway: return vertAxis !=AxisValue.Highway;
				case HorizAxesOffroad: return vertAxis !=AxisValue.Offroad;
				case HorizAxesMud    : return vertAxis !=AxisValue.Mud    ;
				case VertAxesHighway : return horizAxis!=AxisValue.Highway;
				case VertAxesOffroad : return horizAxis!=AxisValue.Offroad;
				case VertAxesMud     : return horizAxis!=AxisValue.Mud    ;
				}
				return null;
			});
		}

		private void updateDiagram() {
			HashMap<Float,HashMap<Float,WheelsDiagram.DataPoint>> dataPointsMap = new HashMap<>();
			for (ExpandedCompatibleWheel wheel:data) {
				Float x = getValue(wheel,horizAxis);
				Float y = getValue(wheel,vertAxis);
				if (x!=null && y!=null) {
					HashMap<Float, WheelsDiagram.DataPoint> yMap = dataPointsMap.get(x);
					if (yMap==null) dataPointsMap.put(x, yMap = new HashMap<>());
					WheelsDiagram.DataPoint dataPoint = yMap.get(y);
					if (dataPoint==null) yMap.put(y, dataPoint = new WheelsDiagram.DataPoint(x, y));
					dataPoint.add(wheel, language);
				}
			}
			
			Vector<WheelsDiagram.DataPoint> dataPointsVec = new Vector<>();
			dataPointsMap.forEach((x,yMap)->dataPointsVec.addAll(yMap.values()));
			
			diagramView.setData(dataPointsVec,true,true);
		}

		private Float getValue(ExpandedCompatibleWheel wheel, AxisValue value) {
			switch (value) {
			case Highway: return wheel.frictionHighway;
			case Offroad: return wheel.frictionOffroad;
			case Mud    : return wheel.frictionMud;
			}
			return null;
		}
	}
}