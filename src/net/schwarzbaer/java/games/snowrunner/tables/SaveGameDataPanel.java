package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.AssignToDLCDialog;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.MapIndex;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Addon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Coord3F;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Garage;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.MapInfos;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.MapInfos.CargoLoadingCounts;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.MapInfos.Waypoint;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Objective;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TruckDesc;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TruckDesc.InstalledAddon;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DLCAssignmentListener;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DLCs;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.SplitOrientation;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.SplitPaneConfigurator;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTAOS;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelUOS;

public class SaveGameDataPanel extends JSplitPane implements Finalizable
{
	private static final long serialVersionUID = 1310479209736600258L;
	
	private final Finalizer finalizer;
	private final JTextArea textArea;
	private Data data;
	private SaveGame saveGame;
	private Language language;

	
	public SaveGameDataPanel(Window mainWindow, Controllers controllers)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		
		setResizeWeight(0);
		finalizer = controllers.createNewFinalizer();
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setWrapStyleWord(false);
		textArea.setLineWrap(false);
		JScrollPane textAreaScrollPane = new JScrollPane(textArea);
		textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Save Game"));
		textAreaScrollPane.setPreferredSize(new Dimension(400,100));
		
		JTabbedPane tableTabPanel = new JTabbedPane();
		    TruckTableModel     truckTableModel = addTab(finalizer, tableTabPanel, "Trucks"    , new     TruckTableModel(mainWindow, controllers));
		      MapTableModel       mapTableModel = addTab(finalizer, tableTabPanel, "Maps"      , new       MapTableModel(mainWindow, controllers));
		    AddonTableModel     addonTableModel = addTab(finalizer, tableTabPanel, "Addons"    , new     AddonTableModel(mainWindow, controllers));
		ObjectiveTableModel objectiveTableModel = addTab(finalizer, tableTabPanel, "Objectives", new ObjectiveTableModel(mainWindow, controllers));
		
		setLeftComponent(textAreaScrollPane);
		setRightComponent(tableTabPanel);
		
		language = null;
		finalizer.addLanguageListener(language_->{
			language = language_;
			updateTextOutput();
		});
		
		data = null;
		finalizer.addDataReceiver(data_->{
			data = data_;
			updateTextOutput();
			truckTableModel    .setData(data);
			mapTableModel      .setData(data);
			addonTableModel    .setData(data, saveGame);
			objectiveTableModel.setData(data);
		});
		
		saveGame = null;
		finalizer.addSaveGameListener(saveGame_->{
			saveGame = saveGame_;
			updateTextOutput();
			truckTableModel    .setData(saveGame);
			mapTableModel      .setData(saveGame);
			addonTableModel    .setData(data, saveGame);
			objectiveTableModel.setData(saveGame);
		});
	}
	
	private static <ModelType extends Tables.SimplifiedTableModel<?> & Finalizable> ModelType addTab(Finalizer finalizer, JTabbedPane tableTabPanel, String title, ModelType tableModel)
	{
		finalizer.addSubComp(tableModel);
		JComponent container = TableSimplifier.create(tableModel);
		tableTabPanel.addTab(title, container);
		return tableModel;
	}
	
	@Override public void prepareRemovingFromGUI() {
		finalizer.removeSubCompsAndListenersFromGUI();
	}

	private void updateTextOutput()
	{
		ValueListOutput out = new ValueListOutput();
		
		if (saveGame==null)
			out.add(0, "<No SaveGame>");
		else
		{
			out.add(0, "File Name"             , saveGame.fileName  );
		//	out.add(0, "Save Time"             , saveGame.saveTime  );
			out.add(0, "Save Time"             , "%s", SnowRunner.dateTimeFormatter.getTimeStr(saveGame.saveTime, false, true, false, true, false));
			out.add(0, "Game Time"             , "%s", saveGame.getGameTimeStr() );
			out.add(0, "Is HardMode"           , saveGame.isHardMode, "Yes", "False");
			out.add(0, "World Configuration"   , saveGame.worldConfiguration);
			out.add(0, "<Birth Version>"       , saveGame.birthVersion);
			out.add(0, "<Game Difficulty Mode>", saveGame.gameDifficultyMode);
			
			if (saveGame.ppd!=null)
			{
				out.add(0, "Experience", saveGame.ppd.experience);	
				out.add(0, "Rank"      , saveGame.ppd.rank);
				out.addEmptyLine();
				out.add(0, "Money"                     , saveGame.ppd.money);	
				out.add(0, "<RefundMoney>"             , saveGame.ppd.refundMoney);	
				out.add(0, "<CustomizationRefundMoney>", saveGame.ppd.customizationRefundMoney);	
				out.addEmptyLine();
				
				TruckName[] truckNames = TruckName.getNames(saveGame.ppd.ownedTrucks.keySet(), data, language);
				if (truckNames.length>0)
				{
					out.add(0, "Owned Trucks", truckNames.length);	
					for (TruckName truckName : truckNames)
						out.add(1, truckName.name, saveGame.ppd.ownedTrucks.get(truckName.id));
				}
			}
		}
		
		textArea.setText(out.generateOutput());
	}
	
	private static String getMapName(MapIndex mapIndex, Language lang, boolean nullIfNoName)
	{
		if (lang==null || lang.regionNames==null) return null;
		if (nullIfNoName)
			return lang.regionNames.getNameForMap(mapIndex, ()->null, ()->null);
		else
			return lang.regionNames.getNameForMap(mapIndex);
	}

	private static Vector<String> getSorted(Collection<String> list)
	{
		Vector<String> vector = new Vector<>(list);
		vector.sort(null);
		return vector;
	}

	private static String getNameOfCargoType(String cargoType, Data data, Language language)
	{
		if (data!=null)
			for (TruckAddon addon : data.truckAddons.values())
				if (addon.gameData.isCargo && cargoType.equals(addon.gameData.cargoType))
				{
					String name = SnowRunner.solveStringID_Null(addon, language);
					if (name!=null && !name.isBlank())
						return name;
				}
		return null;
	}

	private static String toString(Collection<Boolean> boolCollection)
	{
		int nulls = 0;
		int trues = 0;
		for (Boolean b : boolCollection)
		{
			if (b==null)
				nulls++;
			else if (b)
				trues++;
		}
		if (nulls>0)
			return String.format("%d / %d (%d nulls)", trues, boolCollection.size(), nulls);
		else
			return String.format("%d / %d"           , trues, boolCollection.size());
	}

	private static String toString(Coord3F p)
	{
		if (p==null   ) return "<null>";
		if (p.isZero()) return "(0,0,0)";
		return String.format(Locale.ENGLISH, "( %1.3f, %1.3f, %1.3f )", p.x, p.y, p.z);
	}

	private record TruckName(String id, String name)
	{
		static TruckName[] getNames(Collection<String> truckIDs, Data data, Language language)
		{
			return truckIDs.stream()
					.map(id->new TruckName(id, getTruckLabel(id, data, language)))
					.sorted(Comparator.<TruckName,String>comparing(tm->tm.name))
					.toArray(TruckName[]::new);
		}
		
		static String getTruckLabel(String truckID, Data data, Language language)
		{
			Truck truck = data==null ? null : data.trucks.get(truckID);
			if (truck==null) return "<"+truckID+">";
			return SnowRunner.getTruckLabel(truck, language);
		}
	}

	private static class InstalledAddonTableModel extends ExtendedVerySimpleTableModelTAOS<InstalledAddon> implements SplitPaneConfigurator
	{
		InstalledAddonTableModel(Window mainWindow, Controllers controllers)
		{
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("Name"       , "Name"             ,  String .class, 280,   null, null, false, row->((InstalledAddon)row).name              ),
					
					new ColumnID("Fuel"       , "Fuel"             ,  Double .class,  50,   null, null, false, row->((InstalledAddon)row).fuel              ),
					new ColumnID("Water"      , "Water"            ,  Double .class,  45,   null, null, false, row->((InstalledAddon)row).water             ),
					new ColumnID("Repairs"    , "Repairs"          ,  Long   .class,  55, CENTER, null, false, row->((InstalledAddon)row).repairs           ),
					new ColumnID("WheelReps"  , "WheelRepairs"     ,  Long   .class,  85, CENTER, null, false, row->((InstalledAddon)row).wheelRepairs      ),
					
					new ColumnID("IsInCockpit", "Is in Cockpit"    ,  Boolean.class,  75,   null, null, false, row->((InstalledAddon)row).isInCockpit       ),
					new ColumnID("Position"   , "Position"         ,  String .class, 130,   null, null, false, row->SaveGameDataPanel.toString(((InstalledAddon)row).position   )),
					new ColumnID("EulerAngles", "Euler Angles"     ,  String .class, 130,   null, null, false, row->SaveGameDataPanel.toString(((InstalledAddon)row).eulerAngles)),
					
					new ColumnID("AddonCRC"   , "Addon CRC"        ,  Long   .class,  70, CENTER, null, false, row->((InstalledAddon)row).addonCRC          ),
					new ColumnID("FirstSlot"  , "First Slot"       ,  Long   .class,  55, CENTER, null, false, row->((InstalledAddon)row).firstSlot         ),
					new ColumnID("OverrideMat", "Override Material",  String .class, 100,   null, null, false, row->((InstalledAddon)row).overrideMaterial  ),
					new ColumnID("ParentAddTp", "Parent Addon Type",  String .class, 110,   null, null, false, row->((InstalledAddon)row).parentAddonType   ),
					new ColumnID("ParentFrame", "Parent Frame"     ,  String .class, 100,   null, null, false, row->((InstalledAddon)row).parentFrame       ),
					new ColumnID("ExtrParents", "Extra Parents"    ,  Integer.class,  80, CENTER, null, false, row->((InstalledAddon)row).extraParents.size()),
			});
		}

		@Override protected String getRowName(InstalledAddon row) { return row.name; }
		@Override public boolean createSplitPane() { return true; }
		@Override public Boolean putOutputInScrollPane() { return true; }
		@Override public SplitOrientation getSplitOrientation() { return SplitOrientation.HORIZONTAL_SPLIT; }
		@Override public boolean useLineWrap() { return false; }

		@Override protected String getOutputTextForRow(int rowIndex, InstalledAddon row)
		{
			ValueListOutput out = new ValueListOutput();
			
			out.add(0, "Extra Parents", row.extraParents.size());
			for (InstalledAddon.ExtraParent exp : row.extraParents)
			{
				out.add(1, exp.frame);
				out.add(2, "posistion"   , "%s", SaveGameDataPanel.toString(exp.position));
				out.add(2, "euler angles", "%s", SaveGameDataPanel.toString(exp.eulerAngles));
			}
				
			return out.generateOutput();
		}
	}

	private static class TruckTableModel extends ExtendedVerySimpleTableModelUOS<TruckTableModel.Row>
	{
		private Data data;
		private final InstalledAddonTableModel installedAddonTableModel;
	
		TruckTableModel(Window mainWindow, Controllers controllers)
		{
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("Name"       ,"Name"                        ,  String .class, 175,   null,      null, false, row->((Row)row).name ),
					new ColumnID("MapID"      ,"Map ID"                      ,  String .class, 100,   null,      null, false, row->((Row)row).map.originalMapID()),
					new ColumnID("MapName"    ,"Map"                         ,  String .class, 250,   null,      null, false, get((model, data, lang, row)->getMapName(row.map,lang, true))),
					new ColumnID("TrType"     ,"type"                        ,  String .class, 170,   null,      null, false, get((model, data, lang, row)->row.truckDesc.type         )),
					new ColumnID("Truck"      ,"Truck"                       ,  String .class, 160,   null,      null, false, get((model, data, lang, row)->TruckName.getTruckLabel(row.truckDesc.type, data, lang))),
					new ColumnID("TrDamage"   ,"Damage"                      ,  Long   .class,  50,   null,      null, false, row->((Row)row).truckDesc.damage                    ),
					new ColumnID("TrRepairs"  ,"Repairs"                     ,  Long   .class,  50,   null,      null, false, row->((Row)row).truckDesc.repairs                   ),
					new ColumnID("TrFuel"     ,"Fuel"                        ,  Double .class,  60,   null, "%1.1f L", false, get((model, data, lang, row)->row.truckDesc.fuel         )),
					new ColumnID("TrFuelMx"   ,"Max. Fuel"                   ,  Integer.class,  60,   null,    "%d L", false, get((model, data, lang, row)->getTruckValue(row,data,truck->truck.fuelCapacity))),
					new ColumnID("TrFill"     ,"Fill Level"                  ,  Double .class,  60,   null,"%1.1f %%", false, get((model, data, lang, row)->getNonNull2(row.truckDesc.fuel,getTruckValue(row,data,truck->truck.fuelCapacity),(v1,v2)->v1/v2*100))),
					new ColumnID("TrFuelTkDmg","Fuel Tank Damage"            ,  Long   .class, 100, CENTER,      null, false, row->((Row)row).truckDesc.fuelTankDamage            ),
					new ColumnID("TrEngine"   ,"Engine"                      ,  String .class, 180,   null,      null, false, row->((Row)row).truckDesc.engine                    ),
					new ColumnID("TrEngineDmg","Engine Damage"               ,  Long   .class,  90, CENTER,      null, false, row->((Row)row).truckDesc.engineDamage              ),
					new ColumnID("TrGearbx"   ,"Gearbox"                     ,  String .class,  95,   null,      null, false, row->((Row)row).truckDesc.gearbox                   ),
					new ColumnID("TrGearbxDmg","Gearbox Damage"              ,  Long   .class, 100, CENTER,      null, false, row->((Row)row).truckDesc.gearboxDamage             ),
					new ColumnID("TrSuspen"   ,"Suspension"                  ,  String .class, 230,   null,      null, false, row->((Row)row).truckDesc.suspension                ),
					new ColumnID("TrSuspenDmg","Suspension Damage"           ,  Long   .class, 115, CENTER,      null, false, row->((Row)row).truckDesc.suspensionDamage          ),
					new ColumnID("TrTires"    ,"Tires"                       ,  String .class, 115,   null,      null, false, row->((Row)row).truckDesc.tires                     ),
					new ColumnID("TrRims"     ,"Rims"                        ,  String .class,  70,   null,      null, false, row->((Row)row).truckDesc.rims                      ),
					new ColumnID("TrWheels"   ,"Wheels"                      ,  String .class, 180,   null,      null, false, row->((Row)row).truckDesc.wheels                    ),
					new ColumnID("TrWheelReps","Wheel Repairs"               ,  Long   .class,  80, CENTER,      null, false, row->((Row)row).truckDesc.wheelRepairs              ),
					new ColumnID("TrWheelsScl","Wheels Scale"                ,  Double .class,  75,   null,   "%1.3f", false, row->((Row)row).truckDesc.wheelsScale               ),
					new ColumnID("TrWinch"    ,"Winch"                       ,  String .class, 140,   null,      null, false, row->((Row)row).truckDesc.winch                     ),
					new ColumnID("TrTruckCRC" ,"<truckCRC>"                  ,  Long   .class,  75,   null,      null, false, row->((Row)row).truckDesc.truckCRC                  ),
					new ColumnID("TrPhantomMd","<phantomMode>"               ,  Long   .class, 100,   null,      null, false, row->((Row)row).truckDesc.phantomMode               ),
					new ColumnID("TrTrailGlob","<trailerGlobalId>"           ,  String .class, 100,   null,      null, false, row->((Row)row).truckDesc.trailerGlobalId           ),
					new ColumnID("TrItmfObjId","<itemForObjectiveId>"        ,  String .class, 100,   null,      null, false, row->((Row)row).truckDesc.itemForObjectiveId        ),
					new ColumnID("TrGlobalID" ,"<globalId>"                  ,  String .class, 250,   null,      null, false, row->((Row)row).truckDesc.globalId                  ),
					new ColumnID("TrID"       ,"<id>"                        ,  String .class,  50,   null,      null, false, row->((Row)row).truckDesc.id                        ),
					new ColumnID("TrRMapID"   ,"<retainedMapId>"             ,  String .class, 100,   null,      null, false, row->((Row)row).truckDesc.retainedMap.originalMapID()),
					new ColumnID("TrInvalid"  ,"<isInvalid>"                 ,  Boolean.class,  70,   null,      null, false, row->((Row)row).truckDesc.isInvalid                 ),
					new ColumnID("TrPacked"   ,"<isPacked>"                  ,  Boolean.class,  70,   null,      null, false, row->((Row)row).truckDesc.isPacked                  ),
					new ColumnID("TrUnlocked" ,"<isUnlocked>"                ,  Boolean.class,  80,   null,      null, false, row->((Row)row).truckDesc.isUnlocked                ),
					new ColumnID("TrInstDefAd","<needToInstallDefaultAddons>",  Boolean.class, 150,   null,      null, false, row->((Row)row).truckDesc.needToInstallDefaultAddons),
			});
			data = null;
			installedAddonTableModel = new InstalledAddonTableModel(mainWindow, controllers);
		}
	
		private static <ResultType,V1,V2> ResultType getNonNull2(V1 value1, V2 value2, BiFunction<V1,V2,ResultType> computeValue)
		{
			if (computeValue==null) throw new IllegalArgumentException();
			if (value1==null) return null;
			if (value2==null) return null;
			return computeValue.apply(value1, value2);
		}
	
		private static <ResultType> ResultType getTruckValue(Row row, Data data, Function<Truck,ResultType> getValue)
		{
			if (getValue==null) throw new IllegalArgumentException();
			if (data==null) return null;
			if (row==null) return null;
			if (row.truckDesc==null) return null;
			Truck truck = data.trucks.get(row.truckDesc.type);
			if (truck==null) return null;
			return getValue.apply(truck);
		}
	
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MDLR<ResultType,TruckTableModel,Row> getFunction)
		{
			return ColumnID.get(TruckTableModel.class, Row.class, model->model.data, getFunction);
		}
		
		void setData(Data data)
		{
			this.data = data;
			fireTableUpdate();
		}
	
		void setData(SaveGame saveGame)
		{
			Vector<Row> rows = new Vector<>();
			if (saveGame!=null)
			{
				for (MapInfos map : saveGame.maps.values())
				{
					Garage garage = map.garage;
					if (garage!=null)
						for (int i=0; i<garage.garageSlots.length; i++)
						{
							TruckDesc truckDesc = garage.garageSlots[i];
							if (truckDesc!=null)
								rows.add(Row.createGarageTruck(truckDesc, map.map, garage.name, i));
						}
					
				}
				
				if (saveGame.ppd!=null)
					for (int i=0; i<saveGame.ppd.trucksInWarehouse.size(); i++)
					{
						TruckDesc truckDesc = saveGame.ppd.trucksInWarehouse.get(i);
						rows.add(Row.createWarehouseTruck(truckDesc, i));
					}
			}
			setRowData(rows);
			installedAddonTableModel.setRowData(null);
		}
	
		@Override protected String getRowName(Row row) { return row==null ? null : row.name; }
	
		@Override
		protected void setContentForRow(int rowIndex, Row row)
		{
			if (row!=null && row.truckDesc!=null && row.truckDesc.addons!=null)
				installedAddonTableModel.setRowData(row.truckDesc.addons);
			else
				installedAddonTableModel.setRowData(null);
		}

		@Override public Component createOutputComp() {
			JComponent comp = TableSimplifier.create(installedAddonTableModel);
			comp.setBorder(BorderFactory.createTitledBorder("Installed Addons"));
			return comp;
		}
		@Override public boolean          createSplitPane      () { return true; }
		@Override public Boolean          putOutputInScrollPane() { return false; }
		@Override public SplitOrientation getSplitOrientation  () { return SplitOrientation.VERTICAL_SPLIT; }

		private record Row(String name, MapIndex map, TruckDesc truckDesc)
		{
			static Row createGarageTruck(TruckDesc truckDesc, MapIndex map, String garageName, int slotIndex)
			{
				String name = String.format("Garage \"%s\" Slot %d", garageName, slotIndex+1);
				return new Row(name, map, truckDesc);
			}
	
			static Row createWarehouseTruck(TruckDesc truckDesc, int index)
			{
				String name = String.format("Warehouse Slot %02d", index+1);
				return new Row(name, truckDesc.retainedMap, truckDesc);
			}
		}
	}

	private static class MapTableModel extends ExtendedVerySimpleTableModelTAOS<MapInfos> implements SplitPaneConfigurator
	{
		private MapInfos clickedItem;
		protected DLCs dlcs;
		private Data data;

		MapTableModel(Window mainWindow, Controllers controllers)
		{
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("MapID"     ,"Map ID"                ,  String                    .class, 140,   null, null, false, row->((MapInfos)row).map.originalMapID()),
					new ColumnID("Name"      ,"Name"                  ,  String                    .class, 300,   null, null, false, get((model, lang, row)->getMapName(row.map,lang,true))),
					new ColumnID("DLC"       ,"DLC"                   ,  String                    .class, 170,   null, null, false, get((model, lang, row)->getDLC(row,model))),
					new ColumnID("Visited"   ,"Visited"               ,  Boolean                   .class,  50,   null, null, false, row->((MapInfos)row).wasVisited),
					new ColumnID("Garage"    ,"Garage"                ,  Boolean                   .class,  50,   null, null, false, row->((MapInfos)row).garage!=null),
					new ColumnID("GarageStat","Garage Status"         ,  Long                      .class,  85, CENTER, null, false, row->((MapInfos)row).garageStatus),
					new ColumnID("DiscTrucks","Discovered Trucks"     ,  MapInfos.DiscoveredObjects.class, 100, CENTER, null, false, row->((MapInfos)row).discoveredTrucks),
					new ColumnID("DiscUpgrds","Discovered Upgrades"   ,  MapInfos.DiscoveredObjects.class, 115, CENTER, null, false, row->((MapInfos)row).discoveredUpgrades),
					new ColumnID("DiscWatchP","Discovered WatchPoints",  String                    .class, 130, CENTER, null, false, row->SaveGameDataPanel.toString(((MapInfos)row).watchPoints.values())),
			});
			clickedItem = null;
			data = null;
			
			finalizer.addDLCAssignmentListener(new DLCAssignmentListener() {
				@Override public void updateAfterChange() {
					fireTableColumnUpdate("DLC");
				}
				@Override public void setDLCs(SnowRunner.DLCs dlcs) {
					MapTableModel.this.dlcs = dlcs;
					fireTableColumnUpdate("DLC");
				}
			});
		}
		
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,MapTableModel,MapInfos> getFunction)
		{
			return ColumnID.get(MapTableModel.class, MapInfos.class, getFunction);
		}

		private static String getDLC(MapInfos row, MapTableModel model)
		{
			if (row==null) return null;
			if (model==null) return null;
			if (model.dlcs==null) return null;
			return model.dlcs.getDLCofMap(row.map.originalMapID());
		}

		void setData(Data data)
		{
			this.data = data;
			updateOutput();
		}

		void setData(SaveGame saveGame)
		{
			if (saveGame!=null)
			{
				Vector<String> mapIDs = new Vector<>(saveGame.maps.keySet());
				mapIDs.sort(null);
				setRowData(mapIDs.stream().map(saveGame.maps::get).toList());
			}
			else
				setRowData(null);
		}

		@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
			super.modifyTableContextMenu(table_, contextMenu);
			
			
			contextMenu.addSeparator();
			
			JMenuItem miAssignToDLC = contextMenu.add(SnowRunner.createMenuItem("Assign map to an official DLC", true, e->{
				if (clickedItem==null || dlcs==null) return;
				
				String label = language.regionNames.getNameForMap(clickedItem.map, ()->"<"+clickedItem.map.originalMapID()+">");
				AssignToDLCDialog dlg = new AssignToDLCDialog(
						mainWindow,
						SnowRunner.DLCs.ItemType.Map,
						clickedItem.map.originalMapID(),
						label,
						dlcs
				);
				boolean assignmentsChanged = dlg.showDialog();
				if (assignmentsChanged)
					finalizer.getControllers().dlcAssignmentListeners.updateAfterChange();
			}));
			
			contextMenu.addContextMenuInvokeListener((table, x, y) -> {
				Point p = x==null || y==null ? null : new Point(x,y);
				int rowV = p==null ? -1 : table.rowAtPoint(p);
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedItem = rowM<0 ? null : getRow(rowM);
				
				miAssignToDLC.setEnabled(clickedItem!=null && dlcs!=null);
				
				miAssignToDLC.setText(
					clickedItem==null
					? "Assign map to an official DLC"
					: String.format("Assign \"%s\" to an official DLC", language.regionNames.getNameForMap(clickedItem.map, ()->"<"+clickedItem.map.originalMapID()+">"))
				);
			});
		}

		@Override protected String getRowName(MapInfos row) { return row==null ? null : row.map.originalMapID(); }
		@Override public boolean createSplitPane() { return true; }
		@Override public Boolean putOutputInScrollPane() { return true; }
		@Override public SplitOrientation getSplitOrientation() { return SplitOrientation.HORIZONTAL_SPLIT; }
		@Override public boolean useLineWrap() { return false; }

		@Override protected String getOutputTextForRow(int rowIndex, MapInfos row)
		{
			StringBuilder sb = new StringBuilder();
			ValueListOutput out = new ValueListOutput();
			
			if (!row.waypoints.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Waypoints:");
				for (Waypoint wp : row.waypoints)
					out.add(1, String.format("<Type %d>", wp.type), "%s", SaveGameDataPanel.toString(wp.point));
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.watchPoints.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "WatchPoints:");
				for (String key : getSorted(row.watchPoints.keySet()))
					out.add(1, key, row.watchPoints.get(key));
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.upgradesGiverData.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Upgrades:");
				for (String key : getSorted(row.upgradesGiverData.keySet()))
					out.add(1, key, row.upgradesGiverData.get(key));
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.discoveredObjects.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Discovered Objects:");
				for (String obj : getSorted(row.discoveredObjects))
					out.add(1, obj);
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.cargoLoadingCounts.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Cargo Loading Counts:");
				for (String key : getSorted(row.cargoLoadingCounts.keySet()))
				{
					CargoLoadingCounts clc = row.cargoLoadingCounts.get(key);
					out.add(1, clc.stationName);
					for (String cargoType : getSorted(clc.counts.keySet()))
					{
						out.add(2, String.format("<%s>", cargoType), clc.counts.get(cargoType));
						String name = getNameOfCargoType(cargoType, data, language);
						if (name!=null)
							out.add(3, name);
					}
				}
				sb.append(out.generateOutput());
				out.clear();
			}
			
			return sb.toString();
		}
	}

	private static class AddonTableModel extends VerySimpleTableModel<AddonTableModel.Row>
	{
		private Data data;
		
		AddonTableModel(Window mainWindow, Controllers controllers)
		{
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("ID"       ,"ID"       , String             .class, 300,   null, null, false, row->((Row)row).addon.addonId),
					new ColumnID("Type"     ,"Type"     , AddonType          .class,  80,   null, null, false, row->((Row)row).type),
					new ColumnID("Name"     ,"Name"     , String             .class, 180,   null, null, false, get((model, data, lang, row)->SnowRunner.solveStringID(row.item, lang))),
					new ColumnID("Owned"    ,"Owned"    , Long               .class,  50, CENTER, null, false, row->((Row)row).addon.owned),
					new ColumnID("Damagable","Damagable", Addon.DamagableData.class, 350,   null, null, false, row->((Row)row).addon.damagable),
			});
			data = null;
		}
		
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MDLR<ResultType,AddonTableModel,Row> getFunction)
		{
			return ColumnID.get(AddonTableModel.class, Row.class, m->m.data, getFunction);
		}
		
		private enum AddonType { TruckAddon, Trailer, Engine, Gearbox, Suspensions, Winch }
		
		private record Row(Addon addon, Data.HasNameAndID item, AddonType type)
		{
			static Row create(Addon addon, Data data)
			{
				Data.HasNameAndID item = null;
				AddonType type = null;
				if (data!=null)
				{
					if (item == null) { item = data.truckAddons.get         (addon.addonId); type = AddonType.TruckAddon ; }
					if (item == null) { item = data.trailers   .get         (addon.addonId); type = AddonType.Trailer    ; }
					if (item == null) { item = data.engines    .findInstance(addon.addonId); type = AddonType.Engine     ; }
					if (item == null) { item = data.gearboxes  .findInstance(addon.addonId); type = AddonType.Gearbox    ; }
					if (item == null) { item = data.suspensions.findInstance(addon.addonId); type = AddonType.Suspensions; }
					if (item == null) { item = data.winches    .findInstance(addon.addonId); type = AddonType.Winch      ; }
					if (item == null) type = null;
				}
				
				return new Row(addon, item, type);
			}
		}
		
		void setData(Data data, SaveGame saveGame)
		{
			List<Row> rows = null;
			if (saveGame != null)
			{
				Vector<String> addonIDs = new Vector<>(saveGame.addons.keySet());
				addonIDs.sort(null);
				rows = addonIDs.stream().map(addonID -> Row.create(saveGame.addons.get(addonID), data)).toList();
			}
			setRowData(rows);
		}

		@Override protected String getRowName(Row row) { return row==null ? null : row.addon.addonId; }
	}

	private static class ObjectiveTableModel extends ExtendedVerySimpleTableModelTAOS<Objective> implements SplitPaneConfigurator
	{
		private Data data;
		
		ObjectiveTableModel(Window mainWindow, Controllers controllers)
		{
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("ID"        ,"ID"                                       ,  String .class, 320,   null, null, false, row->((Objective)row).objectiveId        ),
					new ColumnID("Attempts"  ,"Attempts"                                 ,  Long   .class,  60,   null, null, false, row->((Objective)row).attempts         ),
					new ColumnID("Times"     ,"Times"                                    ,  Long   .class,  40,   null, null, false, row->((Objective)row).times            ),
					new ColumnID("LastTimes" ,"Last Times"                               ,  Long   .class,  60,   null, null, false, row->((Objective)row).lastTimes        ),
					new ColumnID("Discovered","Discovered"                               ,  Boolean.class,  65,   null, null, false, row->((Objective)row).discovered       ),
					new ColumnID("Finished"  ,"Finished"                                 ,  Boolean.class,  55,   null, null, false, row->((Objective)row).finished         ),
					new ColumnID("ViewdUnact","Viewed, but Unactivated"                  ,  Boolean.class, 130,   null, null, false, row->((Objective)row).viewedUnactivated),
					new ColumnID("SCNtbRoR"  ,"Saved Cargo Need To Be Removed On Restart",  Integer.class,  50, CENTER, null, false, row->((Objective)row).savedCargoNeedToBeRemovedOnRestart.size()),
			});
			data = null;
		}
		
		//private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunctionMDLR<ResultType,ContestTableModel,Contest> getFunction)
		//{
		//	return ColumnID.get(ContestTableModel.class, Contest.class, m->m.data, getFunction);
		//}
		
		void setData(Data data)
		{
			this.data = data;
			fireTableUpdate();
			updateOutput();
		}
	
		void setData(SaveGame saveGame)
		{
			if (saveGame!=null)
			{
				Vector<String> mapIDs = new Vector<>(saveGame.objectives.keySet());
				mapIDs.sort(null);
				setRowData(mapIDs.stream().map(saveGame.objectives::get).toList());
			}
			else
				setRowData(null);
		}

		@Override protected String getRowName(Objective row) { return row==null ? null : row.objectiveId; }
		@Override public boolean createSplitPane() { return true; }
		@Override public Boolean putOutputInScrollPane() { return true; }
		@Override public SplitOrientation getSplitOrientation() { return SplitOrientation.HORIZONTAL_SPLIT; }
		@Override public boolean useLineWrap() { return false; }

		@Override protected String getOutputTextForRow(int rowIndex, Objective row)
		{
			ValueListOutput out = new ValueListOutput();
			if (!row.savedCargoNeedToBeRemovedOnRestart.isEmpty())
			{
				out.add(0, "Saved Cargo Need To Be Removed On Restart:");
				for (String cargoType : getSorted(row.savedCargoNeedToBeRemovedOnRestart))
				{
					out.add(1, String.format("<%s>", cargoType));
					String name = getNameOfCargoType(cargoType, data, language);
					if (name!=null)
						out.add(2, name);
				}
			}
			return out.generateOutput();
		}
	}
}
