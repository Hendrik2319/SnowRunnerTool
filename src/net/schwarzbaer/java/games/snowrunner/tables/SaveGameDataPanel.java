package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import javax.swing.SwingUtilities;

import net.schwarzbaer.java.games.snowrunner.AssignToDLCDialog;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.MapIndex;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.CompatibleWheel;
import net.schwarzbaer.java.games.snowrunner.Data.TruckAddon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Addon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Coord3F;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Garage;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.MapInfos;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Objective;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.RegionInfos;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TintColor;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TruckDesc;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TruckDesc.InstalledAddon;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TruckInfos;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.GlobalFinalDataStructures;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.ContextMenu;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.SplitOrientation;
import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.SplitPaneConfigurator;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelTAOS;
import net.schwarzbaer.java.games.snowrunner.tables.VerySimpleTableModel.ExtendedVerySimpleTableModelUOS;
import net.schwarzbaer.java.lib.gui.ScrollPosition;
import net.schwarzbaer.java.lib.gui.Tables;
import net.schwarzbaer.java.lib.gui.ValueListOutput;

public class SaveGameDataPanel extends JSplitPane implements Finalizable
{
	private static final long serialVersionUID = 1310479209736600258L;
	
	private final Finalizer finalizer;
	private final JTextArea textArea;
	private Data data;
	private SaveGame saveGame;
	private Language language;

	
	public SaveGameDataPanel(Window mainWindow, GlobalFinalDataStructures gfds)
	{
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		
		setResizeWeight(0);
		finalizer = gfds.controllers.createNewFinalizer();
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setWrapStyleWord(false);
		textArea.setLineWrap(false);
		JScrollPane textAreaScrollPane = new JScrollPane(textArea);
		textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Save Game"));
		textAreaScrollPane.setPreferredSize(new Dimension(400,100));
		
		JTabbedPane tableTabPanel = new JTabbedPane();
		StoredTrucksTableModel storedTrucksTableModel = addTab(finalizer, tableTabPanel, "Stored Trucks", new StoredTrucksTableModel(mainWindow, gfds));
		     RegionsTableModel      regionsTableModel = addTab(finalizer, tableTabPanel, "Regions"      , new      RegionsTableModel(mainWindow, gfds));
		        MapsTableModel         mapsTableModel = addTab(finalizer, tableTabPanel, "Maps"         , new         MapsTableModel(mainWindow, gfds));
		      AddonsTableModel       addonsTableModel = addTab(finalizer, tableTabPanel, "Addons"       , new       AddonsTableModel(mainWindow, gfds));
		  ObjectivesTableModel   objectivesTableModel = addTab(finalizer, tableTabPanel, "Objectives"   , new   ObjectivesTableModel(mainWindow, gfds));
		
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
			storedTrucksTableModel.setData(data);
		//	regionsTableModel     .setData(data);
			mapsTableModel        .setData(data);
			addonsTableModel      .setData(data, saveGame);
			objectivesTableModel  .setData(data);
		});
		
		saveGame = null;
		finalizer.addSaveGameListener(saveGame_->{
			saveGame = saveGame_;
			updateTextOutput();
			storedTrucksTableModel.setData(saveGame);
			regionsTableModel     .setData(saveGame);
			mapsTableModel        .setData(saveGame);
			addonsTableModel      .setData(data, saveGame);
			objectivesTableModel  .setData(saveGame);
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
		StringBuilder sb = new StringBuilder();
		
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
			out.add(0, "<Game Difficulty Mode>", saveGame.gameDifficultyMode);
			out.add(0, "Metric System"         , saveGame.metricSystem);
			out.add(0, "Tracked Objective"     , saveGame.trackedObjective);
			showObjective(out, 1, saveGame.trackedObjective, language, 30);
			out.add(0, "Last Loaded Map"       , saveGame.lastLoadedLevel);
			if (saveGame.lastLoadedLevel!=null && language!=null)
			{
				Data.MapIndex mapIndex = Data.MapIndex.parse(saveGame.lastLoadedLevel);
				String mapName = language.regionNames.getName(mapIndex, ()->null);
				if (mapName!=null)
					out.add(0, null, "%s", mapName);
			}
			out.add(0, "<Last Map State>", saveGame.lastLevelState);
			if (saveGame.gameStat.sessionNumber!=null) out.add(0, "Session №", saveGame.gameStat.sessionNumber);
			
			Long moneyEarned = saveGame.gameStat.moneyEarned;
			Long moneySpent  = saveGame.gameStat.moneySpent;
			if (saveGame.ppd!=null)
			{
				out.addEmptyLine();
				
				out.add(0, "Experience", saveGame.ppd.experience);	
				out.add(0, "Rank"      , saveGame.ppd.rank);
				
				sb.append(out.generateOutput());
				out.clear();
				out.addEmptyLine();
				
				if (moneyEarned == null && moneySpent == null)
					out.add(0, "Money", saveGame.ppd.money);
				else
				{
					out.add(0, "Money");
					out.add(1, "current", saveGame.ppd.money);
					if (moneyEarned!=null) out.add(1, "Earned", moneyEarned);	
					if (moneySpent !=null) out.add(1, "Spent" , moneySpent );
				}
				out.add(0, "<RefundMoney>"             , saveGame.ppd.refundMoney);	
				out.add(0, "<CustomizationRefundMoney>", saveGame.ppd.customizationRefundMoney);	
			}
			else
			{
				if (moneyEarned!=null) out.add(0, "Money Earned", moneyEarned);	
				if (moneySpent !=null) out.add(0, "Money Spent" , moneySpent );	
			}
			
			Long multiSePla = saveGame.gameStat.multiplayerSessionsPlayed;
			Long multiMiFin = saveGame.gameStat.multiplayerMissionsFinished;
			Long multiMoEar = saveGame.gameStat.multiplayerMoneyEarned;
			if (multiSePla!=null || multiMiFin!=null || multiMoEar!=null)
			{
				out.addEmptyLine();
				out.add(0, "Multiplayer");
				if (multiSePla!=null) out.add(1, "Sessions Played"  , multiSePla);
				if (multiMiFin!=null) out.add(1, "Missions Finished", multiMiFin);
				if (multiMoEar!=null) out.add(1, "Money Earned"     , multiMoEar);
			}
			
			sb.append(out.generateOutput());
			out.clear();
			out.addEmptyLine();
			
			addBoughtAndSold(out, "Trucks"  , saveGame.gameStat.  trucksBought, saveGame.gameStat.  trucksSold);
			addBoughtAndSold(out, "Trailers", saveGame.gameStat.trailersBought, saveGame.gameStat.trailersSold);
			addBoughtAndSold(out, "Addons"  , saveGame.gameStat.  addonsBought, saveGame.gameStat.  addonsSold);
			
			if (saveGame.ppd!=null)
			{
				TruckName[] truckNames = TruckName.getNames(saveGame.trucks.keySet(), data, language);
				if (truckNames.length>0)
				{
					sb.append(out.generateOutput());
					out.clear();
					out.addEmptyLine();
					
					out.add(0, "Trucks", truckNames.length);	
					for (TruckName truckName : truckNames)
					{
						TruckInfos truckInfos = saveGame.trucks.get(truckName.id);
						long owned = truckInfos==null || truckInfos.owned==null ? 0 : truckInfos.owned.longValue();
						out.add(1, truckName.name, "%d%s", owned, truckInfos!=null && truckInfos.isNew ? " (new)" : "");
					}
				}
			}
			
			sb.append(out.generateOutput());
			out.clear();
			out.addEmptyLine();

			if (saveGame.ppd!=null && saveGame.ppd.isNewProfile!=null)
				out.add(0, "<isNewProfile>"       , saveGame.ppd.isNewProfile, "Yes", "No");
			out.add(0, "<ObjVersion>"             , saveGame.objVersion);
			out.add(0, "<Birth Version>"          , saveGame.birthVersion);
			out.add(0, "<Save ID>"                , saveGame.saveId);
			out.add(0, "<First Garage Discovered>", saveGame.isFirstGarageDiscovered);
			out.add(0, "<Last Phantom Mode>"      , saveGame.lastPhantomMode);
			out.add(0, "<Objectives Validated>"   , saveGame.objectivesValidated);
			add(out,0, "<Forced Model States>"    , saveGame.forcedModelStates, out::add);
			add(out,0, "<Tutorial States>"        , saveGame.tutorialStates   , out::add);
			if (saveGame.ppd!=null)
				add(out,0, "<DLC Notes>"          , saveGame.ppd.dlcNotes     , out::add);
			
			if (saveGame.ppd!=null && !saveGame.ppd.customColors.isEmpty())
			{
				sb.append(out.generateOutput());
				out.clear();
				out.addEmptyLine();
				
				HashMap<String, TruckDesc.CustomizationPreset> customColors = saveGame.ppd.customColors;
				out.add(0, "CustomColors", customColors.size());	
				List<String> keys = customColors.keySet().stream().sorted().toList();
				for (String key : keys)
					WriterFunctions.write(out, 1, key, customColors.get(key));
			}
		}
		
		sb.append(out.generateOutput());
		out.clear();
		
		textArea.setText(sb.toString());
	}

	private void addBoughtAndSold(ValueListOutput out, String label, Long bought, Long sold)
	{
		if (bought!=null || sold!=null)
		{
			if      (bought==null) out.add(0, label, "%d sold"  , sold );
			else if (sold  ==null) out.add(0, label, "%d bought", bought);
			else out.add(0, label, "%d bought / %d sold", bought, sold);
		}
	}

	private static void showObjective(ValueListOutput out, int indentLevel, String objective, Language language, int maxLineLength)
	{
		if (objective!=null && language!=null)
		{
			String objName = language.get(objective);
			if (objName!=null)
			{
				out.add(indentLevel  , "Name");
				out.add(indentLevel+1, objName);
			}
			String objDesc = language.get(objective+"_DESC");
			if (objDesc!=null)
			{
				out.add(indentLevel, "Description");
				SnowRunner.lineWrap(objDesc, maxLineLength, str->out.add(indentLevel+1, str));
			}
		}
	}
	
	private interface OutAddFcn<ValueType>
	{
		void add(int indentLevel, String label, ValueType value);
	}
	
	private static <ValueType> void add(ValueListOutput out, int indentLevel, String label, HashMap<String,ValueType> map, OutAddFcn<ValueType> outAdd)
	{
		if (outAdd==null) throw new IllegalArgumentException();
		if (label==null) throw new IllegalArgumentException();
		
		out.add(indentLevel, label, map==null ? 0 : map.size());
		if (map!=null)
			for (String key : SnowRunner.getSorted(map.keySet()))
				outAdd.add(indentLevel+1, String.format("\"%s\"", key), map.get(key));
	}
	
	private static <ValueType> void add(ValueListOutput out, int indentLevel, String label, Collection<ValueType> list, OutAddFcn<ValueType> outAdd)
	{
		if (outAdd==null) throw new IllegalArgumentException();
		if (label==null) throw new IllegalArgumentException();
		
		out.add(indentLevel, label, list==null ? 0 : list.size());
		if (list!=null)
			for (ValueType value : list)
				outAdd.add(indentLevel+1, null, value);
	}
	
	private static String getMapName(MapIndex mapIndex, Language lang, boolean nullIfNoName)
	{
		if (lang==null || lang.regionNames==null) return null;
		if (nullIfNoName)
			return lang.regionNames.getName(mapIndex, ()->null);
		else
			return lang.regionNames.getName(mapIndex);
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

	private static <RowType> String getDLC(RowType row, VerySimpleTableModel<RowType> model, Function<RowType,String> getID, SnowRunner.DLCs.ItemType itemType)
	{
		if (row==null) return null;
		if (model==null) return null;
		return model.gfds.dlcs.getDLC(getID.apply(row), itemType);
	}

	private static String toString(double[] values, String formatStr)
	{
		Iterable<String> it = ()->Arrays.stream(values).mapToObj(n -> String.format(Locale.ENGLISH, formatStr, n)).iterator();
		return "[ "+String.join(", ", it)+" ]";
	}
	
	private static String toString(long[] values)
	{
		Iterable<String> it = ()->Arrays.stream(values).mapToObj(Long::toString).iterator();
		return "[ "+String.join(", ", it)+" ]";
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
	
	private static <ObjectType, ResultType> ResultType ifNotNull(ObjectType obj, Function<ObjectType,ResultType> getValue)
	{
		return obj==null ? null : getValue.apply(obj);
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

	private static class RegionsTableModel extends VerySimpleTableModel<RegionInfos>
	{

		private RegionInfos clickedItem;

		RegionsTableModel(Window mainWindow, GlobalFinalDataStructures gfds)
		{
			super(mainWindow, gfds, new ColumnID[] {
					new ColumnID("ID"      , "ID"      , String .class,  60,   null, null, false, row->((RegionInfos)row).region.originalMapID()),
					new ColumnID("Country" , "Country" , String .class,  60, CENTER, null, false, row->((RegionInfos)row).region.country()),
					new ColumnID("Region"  , "Region"  , Integer.class,  60, CENTER, null, false, row->((RegionInfos)row).region.region()),
					new ColumnID("Name"    , "Name"    , String .class, 250,   null, null, false, get((model,lang,row)->getMapName(row.region, lang, true))),
					new ColumnID("DLC"     , "DLC"     , String .class, 170,   null, null, false, get((model,lang,row)->getDLC(row, model, r->r.region.originalMapID(), SnowRunner.DLCs.ItemType.Region))),
					new ColumnID("Known"   , "Known"   , Boolean.class,  60,   null, null, false, row->((RegionInfos)row).isKnown),
					new ColumnID("Distance", "Distance", Long   .class,  60,   null, null, false, row->((RegionInfos)row).distance),
					new ColumnID("Payments", "Payments", Long   .class,  60,   null, null, false, row->((RegionInfos)row).paymentsReceived),
			});
			clickedItem = null;
			finalizer.addDLCListener(() -> fireTableColumnUpdate("DLC"));
		}
		
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,RegionsTableModel,RegionInfos> getFunction)
		{
			return ColumnID.get(RegionsTableModel.class, RegionInfos.class, getFunction);
		}

		void setData(SaveGame saveGame)
		{
			if (saveGame!=null)
			{
				Vector<String> mapIDs = new Vector<>(saveGame.regions.keySet());
				mapIDs.sort(null);
				setRowData(mapIDs.stream().map(saveGame.regions::get).toList());
			}
			else
				setRowData(null);
		}

		@Override protected String getRowName(RegionInfos row)
		{
			return row.region.originalMapID();
		}

		@Override public void modifyTableContextMenu(JTable table_, TableSimplifier.ContextMenu contextMenu) {
			super.modifyTableContextMenu(table_, contextMenu);
			
			
			contextMenu.addSeparator();
			
			JMenuItem miAssignToDLC = contextMenu.add(SnowRunner.createMenuItem("Assign map to an official DLC", true, e->{
				if (clickedItem==null) return;
				
				String label = language.regionNames.getName(clickedItem.region, ()->"<"+clickedItem.region.originalMapID()+">");
				AssignToDLCDialog dlg = new AssignToDLCDialog(
						mainWindow,
						SnowRunner.DLCs.ItemType.Region,
						clickedItem.region.originalMapID(),
						label,
						gfds.dlcs
				);
				boolean assignmentsChanged = dlg.showDialog();
				if (assignmentsChanged)
					gfds.controllers.dlcListeners.updateAfterChange();
			}));
			
			contextMenu.addContextMenuInvokeListener((table, x, y) -> {
				Point p = x==null || y==null ? null : new Point(x,y);
				int rowV = p==null ? -1 : table.rowAtPoint(p);
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedItem = rowM<0 ? null : getRow(rowM);
				
				miAssignToDLC.setEnabled(clickedItem!=null);
				
				miAssignToDLC.setText(
					clickedItem==null
					? "Assign region to an official DLC"
					: String.format("Assign \"%s\" to an official DLC", language.regionNames.getName(clickedItem.region, ()->"<"+clickedItem.region.originalMapID()+">"))
				);
			});
		}
	}
	
	public interface StoredTruckDisplayer
	{
		void setDisplayedTruck(TruckDesc displayedTruck);
	}

	private static class StoredTrucksTableModel extends ExtendedVerySimpleTableModelUOS<StoredTrucksTableModel.Row>
	{
		private static final Color BG_COLOR__DISPLAYED_TRUCK = new Color(0xFFDF00);
		private Data data;
		private final InfoPanel infoPanel;
		private Row clickedRow;
		private TruckDesc displayedTruck;
	
		StoredTrucksTableModel(Window mainWindow, GlobalFinalDataStructures gfds)
		{
			super(mainWindow, gfds, new ColumnID[] {
					new ColumnID("Location"   ,"Location"                    ,  String .class, 175,   null,      null, false, row->((Row)row).location),
					new ColumnID("MapID"      ,"Map ID"                      ,  String .class, 100,   null,      null, false, row->((Row)row).map.originalMapID()),
					new ColumnID("MapName"    ,"Map"                         ,  String .class, 250,   null,      null, false, get((model, data, lang, row)->getMapName(row.map,lang, true))),
					new ColumnID("TrType"     ,"type"                        ,  String .class, 170,   null,      null, false, get((model, data, lang, row)->row.truckDesc.type         )),
					new ColumnID("Truck"      ,"Truck"                       ,  String .class, 160,   null,      null, false, get((model, data, lang, row)->TruckName.getTruckLabel(row.truckDesc.type, data, lang))),
					new ColumnID("Colors"     ,"Colors"                      ,  Color[].class,  97,   null,      null, false, row->((Row)row).truckDesc.customizationPreset.toColorArray()),
					new ColumnID("TrDamage"   ,"Damage"                      ,  Long   .class,  50,   null,      null, false, row->((Row)row).truckDesc.damage                    ),
					new ColumnID("TrDmgDecals","Damage Decals"               ,  Integer.class,  85,   null,      null, false, row->((Row)row).truckDesc.damageDecals.length/4     ),
					new ColumnID("TrRepairs"  ,"Repairs"                     ,  Long   .class,  50,   null,      null, false, row->((Row)row).truckDesc.repairs                   ),
					new ColumnID("TrWater"    ,"Water"                       ,  Double .class,  60,   null, "%1.1f L", false, row->((Row)row).truckDesc.water                     ),
					new ColumnID("TrFuel"     ,"Fuel"                        ,  Double .class,  60,   null, "%1.1f L", false, row->((Row)row).truckDesc.fuel                      ),
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
					new ColumnID("TrWheelsSze","Wheels Size"                 ,  Integer.class,  75,   null,    "%d\"", false, row->CompatibleWheel.computeSize_inch(((Row)row).truckDesc.wheelsScale)),
					new ColumnID("TrWheelsDmg","Wheels Damage"               ,  String .class, 170,   null,      null, false, row->SaveGameDataPanel.toString(((Row)row).truckDesc.wheelsDamage)),
					new ColumnID("TrWheelsSHt","Wheels Susp. Height"         ,  String .class, 150,   null,      null, false, row->SaveGameDataPanel.toString(((Row)row).truckDesc.wheelsSuspHeight, "%1.3f")),
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
					new ColumnID("TrFreezed"  ,"<isFreezed>"                 ,  Boolean.class,  80,   null,      null, false, row->((Row)row).truckDesc.isFreezedByObjective      ),
					new ColumnID("TrInstDefAd","<needToInstallDefaultAddons>",  Boolean.class, 150,   null,      null, false, row->((Row)row).truckDesc.needToInstallDefaultAddons),
			});
			data = null;
			clickedRow = null;
			displayedTruck = gfds.controllers.storedTruckDisplayers.getDisplayedTruck();
			infoPanel = new InfoPanel(mainWindow, gfds);
			finalizer.addStoredTruckDisplayer(displayedTruck -> {
				this.displayedTruck = displayedTruck;
				table.repaint();
			});
			coloring.addBackgroundRowColorizer(row -> row.truckDesc == displayedTruck ? BG_COLOR__DISPLAYED_TRUCK : null);
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
	
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MDLR<ResultType,StoredTrucksTableModel,Row> getFunction)
		{
			return ColumnID.get(StoredTrucksTableModel.class, Row.class, model->model.data, getFunction);
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
			infoPanel.setData(null);
		}
	
		@Override protected String getRowName(Row row) { return row==null ? null : row.location; }
	
		@Override
		protected void setContentForRow(int rowIndex, Row row)
		{
			if (row!=null)
				infoPanel.setData(row.truckDesc);
			else
				infoPanel.setData(null);
		}

		@Override public Component createOutputComp() {
			return infoPanel;
		}
		@Override public boolean          createSplitPane      () { return true; }
		@Override public Boolean          putOutputInScrollPane() { return false; }
		@Override public SplitOrientation getSplitOrientation  () { return SplitOrientation.VERTICAL_SPLIT; }

		private record Row(String location, MapIndex map, TruckDesc truckDesc)
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

		private static void generateInfo(JTextArea textArea, TruckDesc data)
		{
			ValueListOutput out = new ValueListOutput();
			
			if (data!=null)
				WriterFunctions.write(out, 0, "Customization Preset", data.customizationPreset);
			
			textArea.setText(out.generateOutput());
		}

		@Override
		public void modifyTableContextMenu(JTable table, ContextMenu contextMenu)
		{
			super.modifyTableContextMenu(table, contextMenu);
			
			contextMenu.addSeparator();
			
			JMenuItem miShowTruckInTruckTable = contextMenu.add(SnowRunner.createMenuItem("Show truck in truck table", true, e->{
				if (clickedRow!=null)
					gfds.controllers.storedTruckDisplayers.setDisplayedTruck(clickedRow.truckDesc);
			}));
			JMenuItem miClearTruckDisplay = contextMenu.add(SnowRunner.createMenuItem("Clear truck display", true, e->{
				if (displayedTruck!=null)
					gfds.controllers.storedTruckDisplayers.setDisplayedTruck(null);
			}));
			
			contextMenu.addContextMenuInvokeListener((table_, x, y) ->
			{
				clickedRow = getRowAt(table_, x, y);
				
				miClearTruckDisplay.setEnabled(displayedTruck!=null);
				miShowTruckInTruckTable.setEnabled(clickedRow!=null);
				miShowTruckInTruckTable.setText(
					clickedRow == null
						? "Show truck in truck table"
						: String.format("Show \"%s\" in truck table", TruckName.getTruckLabel(clickedRow.truckDesc.type, data, language))
				);
			});
		}

		private static class InfoPanel extends JTabbedPane
		{
			private static final long serialVersionUID = 5027725919393825391L;
			
			private final InstalledAddonTableModel installedAddonTableModel;
			private final JTextArea textArea;
			private final JScrollPane textAreaScrollPane;
		
			InfoPanel(Window mainWindow, GlobalFinalDataStructures gfds)
			{
				textArea = new JTextArea();
				textArea.setEditable(false);
				textAreaScrollPane = new JScrollPane(textArea);
				
				installedAddonTableModel = new InstalledAddonTableModel(mainWindow, gfds);
				
				addTab("Info", textAreaScrollPane);
				addTab("Installed Addons", TableSimplifier.create(installedAddonTableModel));
			}
		
			void setData(TruckDesc data)
			{
				if (data!=null)
					installedAddonTableModel.setRowData(data.addons, data.emptyTruckAddonDesc);
				else
					installedAddonTableModel.setRowData(null,null);
				
				ScrollPosition scrollPos = ScrollPosition.getVertical(textAreaScrollPane);
				generateInfo(textArea, data);
				if (scrollPos!=null) SwingUtilities.invokeLater(()->scrollPos.setVertical(textAreaScrollPane));
			}
		
			private static class InstalledAddonTableModel extends ExtendedVerySimpleTableModelTAOS<InstalledAddon> implements SplitPaneConfigurator
			{
				private InstalledAddon emptyTruckAddonDesc = null;

				InstalledAddonTableModel(Window mainWindow, GlobalFinalDataStructures gfds)
				{   // Column Widths: [75, 280, 90, 90, 55, 85, 75, 130, 130, 70, 55, 100, 110, 100, 80] in ModelOrder
					super(mainWindow, gfds, new ColumnID[] {
							new ColumnID("EmptyAddon" , "Empty Addon"      ,  Boolean.class,  75,   null, null, false, get((model,lang,row)->row==model.emptyTruckAddonDesc)),
							
							new ColumnID("Name"       , "Name"             ,  String .class, 280,   null, null, false, row->((InstalledAddon)row).name              ),
							
							new ColumnID("Fuel"       , "Fuel"             ,  Double .class,  90,   null, null, false, row->((InstalledAddon)row).fuel              ),
							new ColumnID("Water"      , "Water"            ,  Double .class,  90,   null, null, false, row->((InstalledAddon)row).water             ),
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
				
				private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,InstalledAddonTableModel,InstalledAddon> getFunction)
				{
					return ColumnID.get(InstalledAddonTableModel.class, InstalledAddon.class, getFunction);
				}

				public void setRowData(Vector<InstalledAddon> addons, InstalledAddon emptyTruckAddonDesc)
				{
					this.emptyTruckAddonDesc = emptyTruckAddonDesc;
					Vector<InstalledAddon> vector = null;
					if (addons!=null || emptyTruckAddonDesc!=null) {
						vector = new Vector<>();
						if (emptyTruckAddonDesc!=null) vector.add   (emptyTruckAddonDesc);
						if (addons             !=null) vector.addAll(addons             );
					}
					super.setRowData(vector);
				}

				@Override protected String getRowName(InstalledAddon row) { return row.name; }
				@Override public boolean createSplitPane() { return true; }
				@Override public Boolean putOutputInScrollPane() { return true; }
				@Override public SplitOrientation getSplitOrientation() { return SplitOrientation.HORIZONTAL_SPLIT; }
				@Override public boolean useLineWrap() { return false; }
				
				@Override protected String getOutputTextForRow(int rowIndex, InstalledAddon row)
				{
					ValueListOutput out = new ValueListOutput();
					
					if (row==emptyTruckAddonDesc)
					{
						out.add(0, "Is \"Empty Truck Addon\"");
						out.addEmptyLine();
					}
					
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
		}
	}

	private static class MapsTableModel extends ExtendedVerySimpleTableModelTAOS<MapInfos> implements SplitPaneConfigurator
	{
		private MapInfos clickedItem;
		private Data data;

		MapsTableModel(Window mainWindow, GlobalFinalDataStructures gfds)
		{
			super(mainWindow, gfds, new ColumnID[] {
					new ColumnID("MapID"     ,"Map ID"                ,  String                    .class, 140,   null, null, false, row->((MapInfos)row).map.originalMapID()),
					new ColumnID("Name"      ,"Name"                  ,  String                    .class, 300,   null, null, false, get((model, lang, row)->getMapName(row.map,lang,true))),
					new ColumnID("DLC"       ,"DLC"                   ,  String                    .class, 170,   null, null, false, get((model, lang, row)->getDLC(row, model, r->r.map.originalMapID(), SnowRunner.DLCs.ItemType.Map))),
					new ColumnID("Visited"   ,"Visited"               ,  Boolean                   .class,  50,   null, null, false, row->((MapInfos)row).wasVisited),
					new ColumnID("Garage"    ,"Garage"                ,  Boolean                   .class,  50,   null, null, false, row->((MapInfos)row).garage!=null),
					new ColumnID("GarageStat","Garage Status"         ,  Long                      .class,  85, CENTER, null, false, row->((MapInfos)row).garageStatus),
					new ColumnID("UpgrGarags","Upgradable Garages"    ,  Integer                   .class, 115, CENTER, null, false, row->((MapInfos)row).upgradableGarages.size()),
					new ColumnID("DiscTrucks","Discovered Trucks"     ,  MapInfos.DiscoveredObjects.class, 100, CENTER, null, false, row->((MapInfos)row).discoveredTrucks),
					new ColumnID("DiscUpgrds","Discovered Upgrades"   ,  MapInfos.DiscoveredObjects.class, 115, CENTER, null, false, row->((MapInfos)row).discoveredUpgrades),
					new ColumnID("DiscWatchP","Discovered WatchPoints",  String                    .class, 130, CENTER, null, false, row->SaveGameDataPanel.toString(((MapInfos)row).watchPoints.values())),
			});
			clickedItem = null;
			data = null;
			
			finalizer.addDLCListener(() -> fireTableColumnUpdate("DLC"));
		}
		
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MLR<ResultType,MapsTableModel,MapInfos> getFunction)
		{
			return ColumnID.get(MapsTableModel.class, MapInfos.class, getFunction);
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
				if (clickedItem==null) return;
				
				String label = language.regionNames.getName(clickedItem.map, ()->"<"+clickedItem.map.originalMapID()+">");
				AssignToDLCDialog dlg = new AssignToDLCDialog(
						mainWindow,
						SnowRunner.DLCs.ItemType.Map,
						clickedItem.map.originalMapID(),
						label,
						gfds.dlcs
				);
				boolean assignmentsChanged = dlg.showDialog();
				if (assignmentsChanged)
					gfds.controllers.dlcListeners.updateAfterChange();
			}));
			
			contextMenu.addContextMenuInvokeListener((table, x, y) -> {
				Point p = x==null || y==null ? null : new Point(x,y);
				int rowV = p==null ? -1 : table.rowAtPoint(p);
				int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedItem = rowM<0 ? null : getRow(rowM);
				
				miAssignToDLC.setEnabled(clickedItem!=null);
				
				miAssignToDLC.setText(
					clickedItem==null
					? "Assign map to an official DLC"
					: String.format("Assign \"%s\" to an official DLC", language.regionNames.getName(clickedItem.map, ()->"<"+clickedItem.map.originalMapID()+">"))
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
				for (MapInfos.Waypoint wp : row.waypoints)
					out.add(1, String.format("<Type %d>", wp.type), "%s", SaveGameDataPanel.toString(wp.point));
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.watchPoints.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "WatchPoints:");
				for (String key : SnowRunner.getSorted(row.watchPoints.keySet()))
					out.add(1, key, row.watchPoints.get(key));
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.upgradableGarages.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Upgradable Garages:");
				Vector<String> keys = SnowRunner.getSorted(row.upgradableGarages.keySet());
				for (int i=0; i<keys.size(); i++)
				{
					String key = keys.get(i);
					MapInfos.UpgradableGarage upgradableGarage = row.upgradableGarages.get(key);
					out.add(1, String.format("%d. Garage", i+1));
					out.add(2, "Zone Name"     , upgradableGarage.zoneName);
					out.add(2, "Global Zone ID", upgradableGarage.zoneGlobalId);
					out.add(2, "Upgradable"    , upgradableGarage.isUpgradable, "Yes", "No");
					out.add(2, "Feature States", "%s", Arrays.toString(upgradableGarage.featureStates));
				}
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.upgradesGiverData.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Upgrades:");
				for (String key : SnowRunner.getSorted(row.upgradesGiverData.keySet()))
					out.add(1, key, row.upgradesGiverData.get(key));
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.discoveredObjects.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Discovered Objects:");
				for (String obj : SnowRunner.getSorted(row.discoveredObjects))
					out.add(1, obj);
				sb.append(out.generateOutput());
				out.clear();
			}
			
			if (!row.cargoLoadingCounts.isEmpty())
			{
				if (!sb.isEmpty()) out.addEmptyLine();
				out.add(0, "Cargo Loading Counts:");
				for (String key : SnowRunner.getSorted(row.cargoLoadingCounts.keySet()))
				{
					MapInfos.CargoLoadingCounts clc = row.cargoLoadingCounts.get(key);
					out.add(1, clc.stationName);
					for (String cargoType : SnowRunner.getSorted(clc.counts.keySet()))
					{
						// ∞
						
						Long count = clc.counts.get(cargoType);
						if    (count==null) out.add(2, String.format( "??? <%s>",        cargoType));
						else if (count>= 0) out.add(2, String.format( "%dx <%s>", count, cargoType));
						else if (count==-1) out.add(2, String.format(   "∞ <%s>",        cargoType));
						else                out.add(2, String.format("(%d) <%s>", count, cargoType));
						String name = getNameOfCargoType(cargoType, data, language);
						if (name!=null) out.add(3, name);
					}
				}
				sb.append(out.generateOutput());
				out.clear();
			}
			
			return sb.toString();
		}
	}

	private static class AddonsTableModel extends VerySimpleTableModel<AddonsTableModel.Row>
	{
		private Data data;
		
		AddonsTableModel(Window mainWindow, GlobalFinalDataStructures gfds)
		{
			super(mainWindow, gfds, new ColumnID[] {
					new ColumnID("ID"       ,"ID"       , String             .class, 300,   null, null, false, row->((Row)row).addon.addonId),
					new ColumnID("Type"     ,"Type"     , AddonType          .class,  80,   null, null, false, row->((Row)row).type),
					new ColumnID("Name"     ,"Name"     , String             .class, 180,   null, null, false, get((model, data, lang, row)->SnowRunner.solveStringID(row.item, lang))),
					new ColumnID("Owned"    ,"Owned"    , Long               .class,  50, CENTER, null, false, row->((Row)row).addon.owned),
					new ColumnID("Damagable","Damagable", Addon.DamagableData.class, 350,   null, null, false, row->((Row)row).addon.damagable),
			});
			data = null;
		}
		
		private static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(ColumnID.GetFunction_MDLR<ResultType,AddonsTableModel,Row> getFunction)
		{
			return ColumnID.get(AddonsTableModel.class, Row.class, m->m.data, getFunction);
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

	private static class ObjectivesTableModel extends ExtendedVerySimpleTableModelTAOS<Objective> implements SplitPaneConfigurator
	{
		private Data data;
		
		ObjectivesTableModel(Window mainWindow, GlobalFinalDataStructures gfds)
		{
			super(mainWindow, gfds, new ColumnID[] {
					new ColumnID("ID"        ,"ID"                                       ,  String .class, 320,   null,    null, false, row->((Objective)row).objectiveId      ),
					new ColumnID("Attempts"  ,"Attempts"                                 ,  Long   .class,  60,   null,    null, false, row->((Objective)row).attempts         ),
					new ColumnID("Times"     ,"Times"                                    ,  Long   .class,  40,   null,    null, false, row->((Objective)row).times            ),
					new ColumnID("LastTimes" ,"Last Times"                               ,  Long   .class,  60,   null,    null, false, row->((Objective)row).lastTimes        ),
					new ColumnID("Discovered","Discovered"                               ,  Boolean.class,  65,   null,    null, false, row->((Objective)row).discovered       ),
					new ColumnID("Finished"  ,"Finished"                                 ,  Boolean.class,  55,   null,    null, false, row->((Objective)row).finished         ),
					new ColumnID("ViewdUnact","Viewed, but Unactivated"                  ,  Boolean.class, 130,   null,    null, false, row->((Objective)row).viewedUnactivated),
					new ColumnID("SCNtbRoR"  ,"Saved Cargo Need To Be Removed On Restart",  Integer.class,  50, CENTER,    null, false, row->((Objective)row).savedCargoNeedToBeRemovedOnRestart.size()),
					new ColumnID("ID2"       ,"ID[2]"                                    ,  String .class, 200,   null,    null, false, row->ifNotNull(((Objective)row).objectiveStates, objst->objst.id                     )),
					new ColumnID("Stages"    ,"Stages"                                   ,  Integer.class,  45, CENTER,    null, false, row->ifNotNull(((Objective)row).objectiveStates, objst->objst.stagesState.size()     )),
					new ColumnID("SpentTime" ,"Spent Time"                               ,  Double .class,  75,   null, "%1.4f", false, row->ifNotNull(((Objective)row).objectiveStates, objst->objst.spentTime              )),
					new ColumnID("IsFinished","Is Finished"                              ,  Boolean.class,  65,   null,    null, false, row->ifNotNull(((Objective)row).objectiveStates, objst->objst.isFinished             )),
					new ColumnID("TimrStartd","Is Timer Started"                         ,  Boolean.class,  90,   null,    null, false, row->ifNotNull(((Objective)row).objectiveStates, objst->objst.isTimerStarted         )),
					new ColumnID("ComplOnce" ,"Was Completed At Least Once"              ,  Boolean.class, 160,   null,    null, false, row->ifNotNull(((Objective)row).objectiveStates, objst->objst.wasCompletedAtLeastOnce)),
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
			
			showObjective(out, 0, row.objectiveId, language, 30);
			
			if (!row.savedCargoNeedToBeRemovedOnRestart.isEmpty())
			{
				out.addEmptyLine();
				out.add(0, "Saved Cargo Need To Be Removed On Restart:");
				for (String cargoType : SnowRunner.getSorted(row.savedCargoNeedToBeRemovedOnRestart))
				{
					out.add(1, String.format("<%s>", cargoType));
					String name = getNameOfCargoType(cargoType, data, language);
					if (name!=null)
						out.add(2, name);
				}
			}
			
			if (row.objectiveStates!=null)
			{
				out.addEmptyLine();
				WriterFunctions.writeArray(out, 0, "Stages", row.objectiveStates.stagesState, "Stage", WriterFunctions::write);
			}
			
			return out.generateOutput();
		}
	}
	
	private static class WriterFunctions
	{
		private interface WriteArrayItemFcn<ValueType>
		{
			void write(ValueListOutput out, int indentLevel, int index, ValueType value);
		}

		private interface WriteArrayItemFcn2<ValueType>
		{
			void write(ValueListOutput out, int indentLevel, String label, ValueType value);
		}

		private static <ValueType> void writeArray(ValueListOutput out, int indentLevel, String label, List<ValueType> array, WriteArrayItemFcn<ValueType> writeFcn)
		{
			writeArray(out, indentLevel, label, false, array, writeFcn);
		}

		private static <ValueType> void writeArray(ValueListOutput out, int indentLevel, String label, boolean showEmptyArrays, List<ValueType> array, WriteArrayItemFcn<ValueType> writeFcn)
		{
			if (array==null) return;
			if (!showEmptyArrays && array.isEmpty()) return;
			out.add(indentLevel, label, array.size());
			for (int i=0; i<array.size(); i++)
				writeFcn.write(out, indentLevel+1, i, array.get(i));
		}

		private static <ValueType> void writeArray(ValueListOutput out, int indentLevel, String label, List<ValueType> array, String itemLabel, WriteArrayItemFcn2<ValueType> writeFcn)
		{
			writeArray(out, indentLevel, label, false, array, itemLabel, writeFcn);
		}

		private static <ValueType> void writeArray(ValueListOutput out, int indentLevel, String label, boolean showEmptyArrays, List<ValueType> array, String itemLabel, WriteArrayItemFcn2<ValueType> writeFcn)
		{
			if (array==null) return;
			if (!showEmptyArrays && array.isEmpty()) return;
			out.add(indentLevel, label, array.size());
			for (int i=0; i<array.size(); i++)
				writeFcn.write(out, indentLevel+1, String.format("%d. %s", i+1, itemLabel), array.get(i));
		}

		private static void write(ValueListOutput out, int indentLevel, int index, String str)
		{
			out.add(indentLevel, String.format("[%d]", index+1), str);
		}

		private static void write(ValueListOutput out, int indentLevel, String label, StagesState stage)
		{
			if (stage==null) return;
			
			out.add   (     indentLevel  , label);
			write     (out, indentLevel+1, "Living Area State"     , stage.livingAreaState);
			write     (out, indentLevel+1, "Visit All Zones State" , stage.visitAllZonesState);
			writeArray(out, indentLevel+1, "Cargo Delivery Actions", stage.cargoDeliveryActions, "Cargo Delivery Action", WriterFunctions::write);
			writeArray(out, indentLevel+1, "Cargo Spawn States"    , stage.cargoSpawnState     , "Cargo Spawn State"    , WriterFunctions::write);
			writeArray(out, indentLevel+1, "Truck Delivery States" , stage.truckDeliveryStates , "Truck Delivery State" , WriterFunctions::write);
			writeArray(out, indentLevel+1, "Truck Repair States"   , stage.truckRepairStates   , "Truck Repair State"   , WriterFunctions::write);
		}
		
		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.LivingAreaState data)
		{
			if (data==null) return;
			
			out.add(indentLevel  , label);
			out.add(indentLevel+1, "Global Zone ID"   , data.zoneGlobalId);
			out.add(indentLevel+1, "Living Area Value", "%d / %d", data.currentLivingAreaValue, data.neededLivingAreaValue);
			out.add(indentLevel+1, "Is Synced"        , data.isSynced, "Yes", "No");
		}
		
		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.VisitAllZonesState data)
		{
			if (data==null) return;
			
			out.add   (     indentLevel  , label);
			out.add   (     indentLevel+1, "Map"        , data.map);
			writeArray(out, indentLevel+1, "Zone States", data.zoneStates, "Zone State", WriterFunctions::write);
		}
		
		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.VisitAllZonesState.ZoneState data)
		{
			if (data==null) return;
			
			out.add(indentLevel  , label);
			out.add(indentLevel+1, "Zone"                       , data.zone                   );
			out.add(indentLevel+1, "Truck UID"                  , data.truckUid               );
			out.add(indentLevel+1, "Is Visited"                 , data.isVisited              , "Yes", "No");
			out.add(indentLevel+1, "Is Visit With Certain Truck", data.isVisitWithCertainTruck, "Yes", "No");
		}
		
		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.CargoDeliveryAction data)
		{
			if (data==null) return;
			
			out.add   (     indentLevel  , label);
			out.add   (     indentLevel+1, "Map"                     , data.map               );
			out.add   (     indentLevel+1, "Platform ID"             , data.platformId        );
			out.add   (     indentLevel+1, "Truck UID"               , data.truckUid          );
			out.add   (     indentLevel+1, "Visit On Truck"          , data.isNeedVisitOnTruck, "Needed", "Not Needed");
			out.add   (     indentLevel+1, "Model Building Tag"      , data.modelBuildingTag  );
			out.add   (     indentLevel+1, "Unloading Mode"          , data.unloadingMode     );
			out.add   (     indentLevel+1, "Is Visible With Platform", data.isVisibleWithPlatform);
			writeArray(out, indentLevel+1, "Zones"                   , data.zones, WriterFunctions::write);
			write     (out, indentLevel+1, "CargoState"              , data.cargoState);
			write     (out, indentLevel+1, "Platform Color Override" , data.platformColorOverride);
		}
		
		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.CargoDeliveryAction.CargoState data)
		{
			if (data==null) return;
			out.add(indentLevel  , label);
			out.add(indentLevel+1, String.format("%d/%d <%s>", data.curValue, data.aimValue, data.type));
		}
		

		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.CargoSpawnState data)
		{
			if (data==null) return;
			out.add   (     indentLevel  , label);
			out.add   (     indentLevel+1, "Spawned"       , data.spawned, "Yes", "No");
			out.add   (     indentLevel+1, "Metal Detector", data.needToBeDiscoveredByMetallodetector, "Needed", "Not Needed");
			out.add   (     indentLevel+1, "Hiding Cargo when not tracked", data.ignoreHidingCargoWhenNotTracked, "Ignored", "Not Ignored");
			writeArray(out, indentLevel+1, "Cargos"        , data.cargos , WriterFunctions::write);
			write     (out, indentLevel+1, "Zone"          , data.zone   );
		}

		private static void write(ValueListOutput out, int indentLevel, int index, StagesState.CargoSpawnState.Cargo data)
		{
			if (data==null) return;
			out.add(indentLevel  , String.format("%dx <%s>", data.count, data.name));
		}

		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.CargoSpawnState.Zone data)
		{
			if (data==null) return;
			out.add(indentLevel  , label);
			out.add(indentLevel+1, "Map"           , data.map         );
			out.add(indentLevel+1, "Zone Local"    , data.zoneLocal   );
			out.add(indentLevel+1, "Cached"        , data.cached      , "Yes", "No");
			out.add(indentLevel+1, "Global Zone ID", data.globalZoneId);
		}

		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.TruckDeliveryState data)
		{
			if (data==null) return;
			out.add   (     indentLevel  , label);
			out.add   (     indentLevel+1, "Map"           , data.mapDelivery  );
			out.add   (     indentLevel+1, "Truck ID"      , data.truckId      );
			out.add   (     indentLevel+1, "Is Delivered"  , data.isDelivered  , "Yes", "No");
			writeArray(out, indentLevel+1, "Delivery Zones", data.deliveryZones, WriterFunctions::write);
		}

		private static void write(ValueListOutput out, int indentLevel, String label, StagesState.TruckRepairState data)
		{
			if (data==null) return;
			out.add(indentLevel  , label);
			out.add(indentLevel+1, "Truck ID"   , data.truckId   );
			out.add(indentLevel+1, "Is Repaired", data.isRepaired, "Yes", "No");
			out.add(indentLevel+1, "Is Refueled", data.isRefueled, "Yes", "No");
		}

		private static void write(ValueListOutput out, int indentLevel, String label, TruckDesc.CustomizationPreset data)
		{
			if (data==null) return;
			out.add(indentLevel  , label);
			out.add(indentLevel+1, "ID"                    , data.id);
			out.add(indentLevel+1, "Special Skin"          , data.isSpecialSkin, "Yes", "No");
			out.add(indentLevel+1, "Override Material Name", data.overrideMaterialName);
			out.add(indentLevel+1, "Material Color Type"   , data.materialColorType);
			out.add(indentLevel+1, "UI Name"               , data.uiName);
			out.add(indentLevel+1, "Colors", data.colors.size());
			for (int i=0; i<data.colors.size(); i++)
				WriterFunctions.write(out, indentLevel+2, String.format("[%d]",i), data.colors.get(i));
		}
		
		private static void write(ValueListOutput out, int indentLevel, String label, TintColor data)
		{
			if (data==null) return;
			out.add(indentLevel, label, "a:%1.1f, r:%1.1f, g:%1.1f, b:%1.1f", data.a, data.r, data.g, data.b);
		}
	}
}
