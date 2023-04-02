package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TextOutput;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Array;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper.KnownJsonValues;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;

public class SaveGameData {
	
	private static final List<String> DEFAULT_FILES = Arrays.asList( "CommonSslSave.cfg", "CompleteSave.cfg", "user_profile.cfg", "user_settings.cfg", "user_social_data.cfg" );
	private static final String SAVEGAME_PREFIX = "CompleteSave";
	private static final String SAVEGAME_SUFFIX = ".cfg";
	private static final JSON_Helper.KnownJsonValuesFactory<NV, V> KJV_FACTORY = new JSON_Helper.KnownJsonValuesFactory<NV, V>("net.schwarzbaer.java.games.snowrunner.");
	private static final JSON_Helper.OptionalValues<NV, V> optionalValues = new JSON_Helper.OptionalValues<NV,V>();
	
	static class NV extends JSON_Data.NamedValueExtra.Dummy{}
	static class V extends JSON_Data.ValueExtra.Dummy{}
	
	final File saveGameFolder;
	final HashMap<String, JSON_Data.Value<NV,V>> rawJsonData;
	final HashMap<String, SaveGame> saveGames;

	SaveGameData(File saveGameFolder) {
		this.saveGameFolder = saveGameFolder;
		rawJsonData = new HashMap<>();
		saveGames = new HashMap<>();
	}

	void readData() {
		rawJsonData.clear();
		saveGames.clear();
		KJV_FACTORY.clearStatementList();
		optionalValues.clear();
		
		File[] defaultFiles = DEFAULT_FILES.stream().<File>map(name->new File(saveGameFolder,name)).toArray(File[]::new);
		for (File file : defaultFiles) {
			JSON_Data.Value<NV, V> value = readJsonFile(file);
			if (value!=null) rawJsonData.put(file.getName(), value);
		}
		
		
		File[] saveFiles = saveGameFolder.listFiles(file->{
			if (!file.isFile()) return false;
			String name = file.getName();
			// "CompleteSave1.cfg"
			if (!name.startsWith(SAVEGAME_PREFIX)) return false;
			if (!name.  endsWith(SAVEGAME_SUFFIX)) return false;
			return true; // !name.equals(SAVEGAME_PREFIX+SAVEGAME_SUFFIX); // "CompleteSave.cfg" is also a save file 
		});
		for (File file : saveFiles) {
			JSON_Data.Value<NV, V> value = readJsonFile(file);
			if (value!=null) {
				String fileName = file.getName();
				rawJsonData.put(fileName, value);
				String indexStr = fileName.substring(SAVEGAME_PREFIX.length(), fileName.length()-SAVEGAME_SUFFIX.length());
				try {
					saveGames.put(indexStr, new SaveGame(fileName, indexStr, value));
				} catch (TraverseException e) {
					System.err.printf("Can't parse SaveGame \"%s\": %s", indexStr, e.getMessage());
					//e.printStackTrace();
				}
			}
		}
		
		KJV_FACTORY.showStatementList(System.err, "Unknown Fields in parsed Data");
		optionalValues.show("OptionalValues", System.out);
	}

	private JSON_Data.Value<NV, V> readJsonFile(File file) {
		if (!file.isFile()) return null;
		try {
			return JSON_Parser.<NV, V>parse_withParseException(file, StandardCharsets.UTF_8, null, null);
		} catch (ParseException e) {
			System.err.printf("ParseException while parsing JSON file \"%s\": %s%n", file.getAbsolutePath(), e.getMessage());
			//e.printStackTrace();
			return null;
		}
	}

	public static class SaveGame {
	
		public final String fileName;
		public final String indexStr;
		public final JSON_Data.Value<NV, V> data;
		public final long saveTime;
		public final double gameTime;
		public final boolean isHardMode;
		public final String worldConfiguration;
		public final PersistentProfileData ppd;
		public final HashMap<String, Garage> garages;

		private SaveGame(String fileName, String indexStr, JSON_Data.Value<NV, V> data) throws TraverseException {
			if (data==null)
				throw new IllegalArgumentException();
			
			this.fileName = fileName;
			this.indexStr = indexStr;
			this.data = data;
			
			JSON_Data.Value<NV, V> sslValue = JSON_Data.getSubNode(this.data, "CompleteSave"+indexStr, "SslValue");
			
			String debugOutputPrefixStr = "CompleteSave"+indexStr+".SslValue";
			JSON_Object<NV, V> sslValueObj = JSON_Data.getObjectValue(sslValue, debugOutputPrefixStr);
			
			JSON_Object<NV, V> garagesData;
			saveTime           = parseTimestamp             (sslValueObj, "saveTime"             , debugOutputPrefixStr);
			gameTime           = JSON_Data.getFloatValue    (sslValueObj, "gameTime"             , debugOutputPrefixStr);
			isHardMode         = JSON_Data.getBoolValue     (sslValueObj, "isHardMode"           , debugOutputPrefixStr);
			worldConfiguration = JSON_Data.getStringValue   (sslValueObj, "worldConfiguration"   , debugOutputPrefixStr);
			ppd                = PersistentProfileData.parse(sslValueObj, "persistentProfileData", debugOutputPrefixStr);
			garagesData        = JSON_Data.getObjectValue   (sslValueObj, "garagesData"          , debugOutputPrefixStr);
			
			garages = new HashMap<String, Garage>();
			for (JSON_Data.NamedValue<NV, V> nv : garagesData)
			{
				String local_debugOutputPrefixStr = debugOutputPrefixStr+".garagesData."+nv.name;
				JSON_Object<NV, V> object = JSON_Data.getObjectValue(nv.value, local_debugOutputPrefixStr);
				garages.put(nv.name, new Garage(object, nv.name, local_debugOutputPrefixStr));
			}
		}
		
		private static long parseTimestamp(JSON_Object<NV, V> object, String subValueName, String debugOutputPrefixStr)
				throws TraverseException
		{
			JSON_Object<NV, V> saveTime = JSON_Data.getObjectValue(object, subValueName, debugOutputPrefixStr);
			String timestampStr = JSON_Data.getStringValue(saveTime, "timestamp", debugOutputPrefixStr+".saveTime");
			if (!timestampStr.startsWith("0x"))
				throw new JSON_Data.TraverseException("Unexpected string value in %s: %s", debugOutputPrefixStr+".saveTime.timestamp", timestampStr);
			
			return Long.parseUnsignedLong(timestampStr.substring(2), 16);
		}

		public int getOwnedTruckCount(Truck truck) {
			if (truck==null) return 0;
			if (ppd==null) return 0;
			if (ppd.ownedTrucks==null) return 0;
			Integer amount = ppd.ownedTrucks.get(truck.id);
			return amount==null ? 0 : amount.intValue();
		}

		public boolean playerOwnsTruck(Truck truck) {
			return getOwnedTruckCount(truck)>0;
		}

		public int getTrucksInWarehouse(Truck truck, TextOutput textOutput) {
			if (truck==null) { if (textOutput!=null) textOutput.printf("No truck defined.%n"); return 0; }
			if (ppd  ==null) { if (textOutput!=null) textOutput.printf("No corresponding data in SaveGame.%n"); return 0; }
			if (ppd.trucksInWarehouse.isEmpty()) { if (textOutput!=null) textOutput.printf("No trucks in Warehouse.%n"); return 0; }
			
			if (textOutput!=null)
				textOutput.printf("<%s> Trucks in warehouse?%n", truck.id);
			
			int count = 0;
			for (int i=0; i<ppd.trucksInWarehouse.size(); i++)
			{
				TruckDesc truckInSlot = ppd.trucksInWarehouse.get(i);
				if (truckInSlot!=null && truckInSlot.type.equals(truck.id))
				{
					count++;
					if (textOutput!=null)
					{
						textOutput.printf("    Truck <%s> found in warehouse at position %d.", truck.id, i+1);
						if (!truckInSlot.retainedMapId.isBlank())
							textOutput.printf(" (Map: %s)%n", truckInSlot.retainedMapId);
						else
							textOutput.printf("%n");
					}
				}
			}
			
			if (textOutput!=null && count==0)
				textOutput.printf("    Truck <%s> not found in warehouse.%n", truck.id);
			
			return count;
		}

		public int getTrucksInGarage(Truck truck, TextOutput textOutput) {
			if (truck==null      ) { if (textOutput!=null) textOutput.printf("No truck defined.%n"); return 0; }
			if (garages.isEmpty()) { if (textOutput!=null) textOutput.printf("No garages found in SaveGame.%n"); return 0; }
			
			if (textOutput!=null)
				textOutput.printf("<%s> Trucks in garage?%n", truck.id);
			
			int count = 0;
			for (String garageName : garages.keySet())
			{
				Garage garage = garages.get(garageName);
				for (int i=0; i<garage.garageSlots.length; i++)
				{
					TruckDesc truckInSlot = garage.garageSlots[i];
					if (truckInSlot!=null && truckInSlot.type.equals(truck.id))
					{
						count++;
						if (textOutput!=null)
							textOutput.printf("    Truck <%s> found in garage \"%s\", slot %d.%n", truck.id, garageName, i+1);
					}
				}
			}
			
			if (textOutput!=null && count==0)
				textOutput.printf("    Truck <%s> not found in any garage.%n", truck.id);
			
			return count;
		}
		
		public static class PersistentProfileData
		{
			public final long money;
			public final long experience;
			public final long rank;
			public final HashMap<String, Integer> ownedTrucks;
			public final Vector<TruckDesc> trucksInWarehouse;
			
			private static PersistentProfileData parse(JSON_Object<NV, V> object, String subValueName, String debugOutputPrefixStr) throws TraverseException
			{
				JSON_Object<NV, V> persistentProfileData      = JSON_Data.getObjectValue(object, "persistentProfileData", false, true, debugOutputPrefixStr);
				JSON_Data.Null     persistentProfileData_Null = JSON_Data.getNullValue  (object, "persistentProfileData", false, true, debugOutputPrefixStr);
				
				if (persistentProfileData==null && persistentProfileData_Null==null)
					throw new JSON_Data.TraverseException("Unexpected type of <persistentProfileData>");
				
				if (persistentProfileData!=null)
					return new PersistentProfileData(persistentProfileData, debugOutputPrefixStr+".persistentProfileData");
				
				return null;
			}
			
			private PersistentProfileData(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				JSON_Object<NV, V> ownedTrucks;
				JSON_Array<NV, V> trucksInWarehouse;
				experience        = JSON_Data.getIntegerValue(object, "experience"       , debugOutputPrefixStr);
				money             = JSON_Data.getIntegerValue(object, "money"            , debugOutputPrefixStr);
				rank              = JSON_Data.getIntegerValue(object, "rank"             , debugOutputPrefixStr);
				ownedTrucks       = JSON_Data.getObjectValue (object, "ownedTrucks"      , debugOutputPrefixStr);
				trucksInWarehouse = JSON_Data.getArrayValue  (object, "trucksInWarehouse", debugOutputPrefixStr);
				
				this.ownedTrucks = new HashMap<>();
				for (JSON_Data.NamedValue<NV, V> nv : ownedTrucks)
				{
					int amount = (int) JSON_Data.getIntegerValue(nv.value, debugOutputPrefixStr+".ownedTrucks."+nv.name);
					this.ownedTrucks.put(nv.name, amount);
				}
				
				this.trucksInWarehouse = new Vector<TruckDesc>();
				for (int i=0; i<trucksInWarehouse.size(); i++)
				{
					String local_debugOutputPrefixStr = debugOutputPrefixStr+".trucksInWarehouse["+i+"]";
					JSON_Object<NV, V> obj = JSON_Data.getObjectValue(trucksInWarehouse.get(i), local_debugOutputPrefixStr);
					this.trucksInWarehouse.add(new TruckDesc(obj,local_debugOutputPrefixStr));
				}
			}
		}
		
		public static class Garage
		{
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Garage = KJV_FACTORY.create(Garage.class)
					.add("selectedSlot"  , JSON_Data.Value.Type.String)
					.add("slotsDatas"    , JSON_Data.Value.Type.Object);
			
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_slotsDatas = KJV_FACTORY.create("Garage.slotsDatas")
					.add("garage_interior_slot_1", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_2", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_3", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_4", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_5", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_6", JSON_Data.Value.Type.Object);
			
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_garageSlot = KJV_FACTORY.create("Garage.slotsDatas.garageSlot")
					.add("garageSlotZoneId", JSON_Data.Value.Type.String)
					.add("truckDesc"       , JSON_Data.Value.Type.Null  )
					.add("truckDesc"       , JSON_Data.Value.Type.Object);
			
			public final String name;
			public final String selectedSlot;
			public final TruckDesc[] garageSlots;
		
			private Garage(JSON_Object<NV, V> object, String name, String debugOutputPrefixStr) throws TraverseException
			{
				this.name = name;
				
				JSON_Object<NV, V> slotsDatas;
				selectedSlot = JSON_Data.getStringValue(object, "selectedSlot", debugOutputPrefixStr);
				slotsDatas   = JSON_Data.getObjectValue(object, "slotsDatas"  , debugOutputPrefixStr);
				
				garageSlots = new TruckDesc[6];
				String local_PrefixStr = debugOutputPrefixStr+".slotsDatas";
				for (int i=0; i<6; i++)
				{
					String garageSlotName = String.format("garage_interior_slot_%d", i+1);
					JSON_Object<NV, V> garageSlot = JSON_Data.getObjectValue(slotsDatas, garageSlotName, local_PrefixStr);
					
					String slot_PrefixStr = local_PrefixStr+"."+garageSlotName;
					
					String             garageSlotZoneId = JSON_Data.getStringValue(garageSlot, "garageSlotZoneId",       slot_PrefixStr);
					JSON_Data.Null     truckDesc_NULL   = JSON_Data.getNullValue  (garageSlot, "truckDesc", false, true, slot_PrefixStr);
					JSON_Object<NV, V> truckDesc        = JSON_Data.getObjectValue(garageSlot, "truckDesc", false, true, slot_PrefixStr);
					
					if (!garageSlotZoneId.equals(garageSlotName))
						System.err.printf("%s.garageSlotZoneId (\"%s\")  !=  expected value (\"%s\")%n", slot_PrefixStr, garageSlotZoneId, garageSlotName);
						
					if (truckDesc==null && truckDesc_NULL==null)
						throw new JSON_Data.TraverseException("%s.truckDesc has unexpected type", slot_PrefixStr);
					
					garageSlots[i] = truckDesc==null ? null : new TruckDesc(truckDesc, slot_PrefixStr+".truckDesc");
					
					KNOWN_JSON_VALUES_garageSlot.scanUnexpectedValues(garageSlot);
				}
				
				KNOWN_JSON_VALUES_Garage    .scanUnexpectedValues(object);
				KNOWN_JSON_VALUES_slotsDatas.scanUnexpectedValues(slotsDatas);
			}
			
		}

		public static class TruckDesc
		{
			public final String type;
			public final String globalId;
			public final String id;
			public final String retainedMapId;
			public final boolean isInvalid;
			public final boolean isPacked;
			public final boolean isUnlocked;
			public final double fuel;

			//private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(TruckDesc.class)
			//		.add("type"      , JSON_Data.Value.Type.String)
			//		.add("globalId"  , JSON_Data.Value.Type.String)
			//		.add("id"        , JSON_Data.Value.Type.String)
			//		.add("isInvalid" , JSON_Data.Value.Type.Bool  )
			//		.add("isPacked"  , JSON_Data.Value.Type.Bool  )
			//		.add("isUnlocked", JSON_Data.Value.Type.Bool  )
			//		.add("fuel"      , JSON_Data.Value.Type.Float )
			//		;
		
			private TruckDesc(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				//optionalValues.scan(object, "TruckDesc");
				
				type          = JSON_Data.getStringValue(object, "type"         , debugOutputPrefixStr);
				globalId      = JSON_Data.getStringValue(object, "globalId"     , debugOutputPrefixStr);
				id            = JSON_Data.getStringValue(object, "id"           , debugOutputPrefixStr);
				retainedMapId = JSON_Data.getStringValue(object, "retainedMapId", debugOutputPrefixStr);
				isInvalid     = JSON_Data.getBoolValue  (object, "isInvalid"    , debugOutputPrefixStr);
				isPacked      = JSON_Data.getBoolValue  (object, "isPacked"     , debugOutputPrefixStr);
				isUnlocked    = JSON_Data.getBoolValue  (object, "isUnlocked"   , debugOutputPrefixStr);
				fuel          = JSON_Data.getFloatValue (object, "fuel"         , debugOutputPrefixStr);
				
				//KNOWN_JSON_VALUES.scanUnexpectedValues(object);
			}
		}
	}

}
