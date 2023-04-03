package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Dimension;
import java.awt.Window;
import java.util.Collection;
import java.util.Comparator;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.Data;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.Garage;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.MapInfos;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.SaveGame.TruckDesc;
import net.schwarzbaer.java.games.snowrunner.SnowRunner;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizable;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.Finalizer;

public class SaveGameDataPanel extends JSplitPane implements Finalizable
{
	private static final long serialVersionUID = 1310479209736600258L;
	private final JTextArea textArea;
	private final TruckTableModel truckTableModel;
	private final Finalizer finalizer;
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
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		JScrollPane textAreaScrollPane = new JScrollPane(textArea);
		textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Save Game"));
		textAreaScrollPane.setPreferredSize(new Dimension(400,100));
		
		truckTableModel = new TruckTableModel(mainWindow, controllers);
		finalizer.addSubComp(truckTableModel);
		JComponent truckTableScrollPane = TableSimplifier.create(truckTableModel);
		truckTableScrollPane.setBorder(BorderFactory.createTitledBorder("Trucks"));
		
		setLeftComponent(textAreaScrollPane);
		setRightComponent(truckTableScrollPane);
		
		language = null;
		finalizer.addLanguageListener(language_->{
			language = language_;
			updateTextOutput();
		});
		
		data = null;
		finalizer.addDataReceiver(data_->{
			data = data_;
			updateTextOutput();
			truckTableModel.setData(data, saveGame);
		});
		
		saveGame = null;
		finalizer.addSaveGameListener(saveGame_->{
			saveGame = saveGame_;
			updateTextOutput();
			truckTableModel.setData(data, saveGame);
		});
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
			out.add(0, "File Name"  , saveGame.fileName  );
			out.add(0, "Is HardMode", saveGame.isHardMode);
			out.add(0, "Save Time"  , saveGame.saveTime  );
			out.add(0, null         , "%s", SnowRunner.dateTimeFormatter.getTimeStr(saveGame.saveTime, false, true, false, true, false));
			out.add(0, "Game Time"  , saveGame.gameTime  );
			out.add(0, "World Configuration", saveGame.worldConfiguration);
			
			if (saveGame.ppd!=null)
			{
				out.add(0, "Experience", saveGame.ppd.experience);	
				out.add(0, "Money"     , saveGame.ppd.money);	
				out.add(0, "Rank"      , saveGame.ppd.rank);
				
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

	private static class TruckTableModel extends VerySimpleTableModel<TruckTableModel.Row>
	{
		
		private Data data;

		TruckTableModel(Window mainWindow, Controllers controllers)
		{
			super(mainWindow, controllers, new ColumnID[] {
					new ColumnID("Name"    ,"Name"           ,  String .class, 175,   null,      null, false, row->((Row)row).name ),
					new ColumnID("MapID"   ,"Map ID"         ,  String .class, 100,   null,      null, false, row->((Row)row).mapID),
					new ColumnID("MapName" ,"Map"            ,  String .class, 250,   null,      null, false, get((model, data, lang, row)->getMapName(row.mapID,lang))),
					new ColumnID("TrType"  ,"type"           ,  String .class, 170,   null,      null, false, get((model, data, lang, row)->row.truckDesc.type         )),
					new ColumnID("Truck"   ,"Truck"          ,  String .class, 160,   null,      null, false, get((model, data, lang, row)->TruckName.getTruckLabel(row.truckDesc.type, data, lang))),
					new ColumnID("TrFuel"  ,"Fuel"           ,  Double .class,  60,   null, "%1.1f L", false, get((model, data, lang, row)->row.truckDesc.fuel         )),
					new ColumnID("TrFuelMx","Max. Fuel"      ,  Integer.class,  60,   null,    "%d L", false, get((model, data, lang, row)->getTruckValue(row,data,truck->truck.fuelCapacity))),
					new ColumnID("TrFill"  ,"Fill Level"     ,  Double .class,  60,   null,"%1.1f %%", false, get((model, data, lang, row)->getNonNull2(row.truckDesc.fuel,getTruckValue(row,data,truck->truck.fuelCapacity),(v1,v2)->v1/v2*100))),
					new ColumnID("TrGlobID","<globalId>"     ,  String .class, 250,   null,      null, false, get((model, data, lang, row)->row.truckDesc.globalId     )),
					new ColumnID("TrID"    ,"<id>"           ,  String .class,  50,   null,      null, false, get((model, data, lang, row)->row.truckDesc.id           )),
					new ColumnID("TrRMapID","<retainedMapId>",  String .class, 100,   null,      null, false, get((model, data, lang, row)->row.truckDesc.retainedMapId)),
					new ColumnID("TrInval" ,"<isInvalid>"    ,  Boolean.class,  55,   null,      null, false, get((model, data, lang, row)->row.truckDesc.isInvalid    )),
					new ColumnID("TrPacked","<isPacked>"     ,  Boolean.class,  55,   null,      null, false, get((model, data, lang, row)->row.truckDesc.isPacked     )),
					new ColumnID("TrUnlock","<isUnlocked>"   ,  Boolean.class,  65,   null,      null, false, get((model, data, lang, row)->row.truckDesc.isUnlocked   )),
			});
			data = null;
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

		private static String getMapName(String mapID, Language lang)
		{
			if (lang==null || lang.regionNames==null) return null;
			return lang.regionNames.getNameForMap(mapID);
		}

		private interface GetFunction<ResultType>
		{
			ResultType get(TruckTableModel model, Data data, Language language, Row row);
		}
		
		static <ResultType> ColumnID.TableModelBasedBuilder<ResultType> get(GetFunction<ResultType> getFunction)
		{
			return (row_,model_) -> {
				if (!(row_   instanceof Row            )) return null;
				if (!(model_ instanceof TruckTableModel)) return null;
				Row row = (Row) row_;
				TruckTableModel model = (TruckTableModel) model_;
				return getFunction.get(model, model.data, model.language, row);
			};
		}

		void setData(Data data, SaveGame saveGame)
		{
			this.data = data;
			
			Vector<Row> rows = new Vector<>();
			if (saveGame!=null)
			{
				for (MapInfos map : saveGame._maps.values())
				{
					Garage garage = map.garage;
					if (garage!=null)
						for (int i=0; i<garage.garageSlots.length; i++)
						{
							TruckDesc truckDesc = garage.garageSlots[i];
							if (truckDesc!=null)
								rows.add(Row.createGarageTruck(truckDesc, map.mapId, garage.name, i));
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
		}

		@Override
		protected String getRowName(Row row)
		{
			return row==null ? null : row.name;
		}

		private record Row(String name, String mapID, TruckDesc truckDesc)
		{
			static Row createGarageTruck(TruckDesc truckDesc, String mapId, String garageName, int slotIndex)
			{
				String name = String.format("Garage \"%s\" Slot %d", garageName, slotIndex+1);
				return new Row(name, mapId, truckDesc);
			}

			static Row createWarehouseTruck(TruckDesc truckDesc, int index)
			{
				String name = String.format("Warehouse Slot %02d", index+1);
				return new Row(name, truckDesc.retainedMapId, truckDesc);
			}
		}
	}
}
