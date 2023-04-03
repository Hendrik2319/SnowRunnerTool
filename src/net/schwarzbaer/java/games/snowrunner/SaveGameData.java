package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
					System.err.printf("Can't parse SaveGame \"%s\": %s%n", indexStr, e.getMessage());
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
		
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Root = KJV_FACTORY.create("[SaveGame]<root>")
		//		.add("CompleteSave#"  , JSON_Data.Value.Type.Object)
				.add("cfg_version"    , JSON_Data.Value.Type.Integer);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_CompleteSave = KJV_FACTORY.create("[SaveGame]<root>.CompleteSave")
				.add("SslValue"       , JSON_Data.Value.Type.Object)
				.add("SslType"        , JSON_Data.Value.Type.String);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Timestamp = KJV_FACTORY.create()
				.add("timestamp"      , JSON_Data.Value.Type.String);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_NameValue = KJV_FACTORY.create()
				.add("name"           , JSON_Data.Value.Type.String );
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES__EMPTY_OBJECT = KJV_FACTORY.create();
		
		@SuppressWarnings("unused")
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_SslValue = KJV_FACTORY.create("[SaveGame]<root>.CompleteSave.SslValue")
				.add("saveTime"                , JSON_Data.Value.Type.Object)
				.add("gameTime"                , JSON_Data.Value.Type.Float)
				.add("isHardMode"              , JSON_Data.Value.Type.Bool)
				.add("worldConfiguration"      , JSON_Data.Value.Type.String)
				.add("persistentProfileData"   , JSON_Data.Value.Type.Object)
				.add("garagesData"             , JSON_Data.Value.Type.Object)
				.add("levelGarageStatuses"     , JSON_Data.Value.Type.Object)
				.add("visitedLevels"           , JSON_Data.Value.Type.Array)
				
				.add("birthVersion"            , JSON_Data.Value.Type.Integer)
				.add("cargoLoadingCounts"      , JSON_Data.Value.Type.Object) // unparsed
				.add("discoveredObjectives"    , JSON_Data.Value.Type.Array) // unparsed
				.add("discoveredObjects"       , JSON_Data.Value.Type.Array) // unparsed
				.add("finishedObjs"            , JSON_Data.Value.Type.Array) // unparsed
				.add("forcedModelStates"       , JSON_Data.Value.Type.Object) // empty object
				.add("gameDifficultyMode"      , JSON_Data.Value.Type.Integer)
				.add("gameDifficultySettings"  , JSON_Data.Value.Type.Object) // unparsed
				.add("gameStat"                , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
//				.add("garagesData"             , JSON_Data.Value.Type.Object) // unparsed
				;
	
		public final String fileName;
		public final String indexStr;
		public final JSON_Data.Value<NV, V> data;
		
		// basic values
		public final long saveTime;
		public final double gameTime;
		public final boolean isHardMode;
		public final String worldConfiguration;
		
		// complex values
		public final PersistentProfileData ppd;
		public final HashMap<String, MapInfos> _maps;
		public final HashMap<String, Contest > _contests;
		public final HashMap<String, Addon   > _addons;
		
		// currently not shown data
		public final long _birthVersion;
		public final Long _gameDifficultyMode;

		private SaveGame(String fileName, String indexStr, JSON_Data.Value<NV, V> data) throws TraverseException {
			if (data==null)
				throw new IllegalArgumentException();
			
			this.fileName = fileName;
			this.indexStr = indexStr;
			this.data = data;
			_maps     = new HashMap<>();
			_contests = new HashMap<>();
			_addons   = new HashMap<>();
			
			JSON_Object<NV, V> rootObject      = JSON_Data.getObjectValue(this.data      ,                          "SaveGame.<root>");
			JSON_Object<NV, V> complSaveObject = JSON_Data.getObjectValue(rootObject     , "CompleteSave"+indexStr, "SaveGame.<root>");
			JSON_Object<NV, V> sslValueObj     = JSON_Data.getObjectValue(complSaveObject, "SslValue"             , "SaveGame.<root>.CompleteSave"+indexStr);
			KNOWN_JSON_VALUES_Root.add("CompleteSave"+indexStr, JSON_Data.Value.Type.Object); // adding known fields dynamically :)
			KNOWN_JSON_VALUES_Root        .scanUnexpectedValues(rootObject     );
			KNOWN_JSON_VALUES_CompleteSave.scanUnexpectedValues(complSaveObject);
			
			String debugOutputPrefixStr = "CompleteSave"+indexStr+".SslValue";
			JSON_Object<NV, V> garagesData, forcedModelStates, levelGarageStatuses;
			JSON_Array<NV, V> visitedLevels;
			JSON_Data.Null levelGarageStatuses_Null;
			saveTime                 = parseTimestamp             (sslValueObj, "saveTime"             , debugOutputPrefixStr);
			gameTime                 = JSON_Data.getFloatValue    (sslValueObj, "gameTime"             , debugOutputPrefixStr);
			isHardMode               = JSON_Data.getBoolValue     (sslValueObj, "isHardMode"           , debugOutputPrefixStr);
			worldConfiguration       = JSON_Data.getStringValue   (sslValueObj, "worldConfiguration"   , debugOutputPrefixStr);
			ppd                      = PersistentProfileData.parse(sslValueObj, "persistentProfileData", debugOutputPrefixStr, this);
			garagesData              = JSON_Data.getObjectValue   (sslValueObj, "garagesData"          , debugOutputPrefixStr);
			levelGarageStatuses      = JSON_Data.getObjectValue   (sslValueObj, "levelGarageStatuses"  , false, true, debugOutputPrefixStr);
			levelGarageStatuses_Null = JSON_Data.getNullValue     (sslValueObj, "levelGarageStatuses"  , false, true, debugOutputPrefixStr);
			visitedLevels            = JSON_Data.getArrayValue    (sslValueObj, "visitedLevels"        , debugOutputPrefixStr);
			
			_birthVersion            = JSON_Data.getIntegerValue  (sslValueObj, "birthVersion"         , debugOutputPrefixStr);
			forcedModelStates        = JSON_Data.getObjectValue   (sslValueObj, "forcedModelStates"    , debugOutputPrefixStr);
			_gameDifficultyMode      = JSON_Data.getIntegerValue  (sslValueObj, "gameDifficultyMode"   , true, false, debugOutputPrefixStr);
			// TODO: parse more values in SaveGame
			
			/*
				unparsed:
					cargoLoadingCounts, discoveredObjectives, discoveredObjects, finishedObjs, gameDifficultySettings, gameStat
					
				unparsed, but interesting:
					
				empty:
					forcedModelStates
			 */
			//KNOWN_JSON_VALUES_SslValue.scanUnexpectedValues(sslValueObj);
			KNOWN_JSON_VALUES__EMPTY_OBJECT.scanUnexpectedValues(forcedModelStates, debugOutputPrefixStr+".forcedModelStates");
			
			checkObjectOrNull(levelGarageStatuses, levelGarageStatuses_Null, "levelGarageStatuses", debugOutputPrefixStr);
			
			MapInfos.parseGaragesData        (_maps, garagesData        , debugOutputPrefixStr+".garagesData"        );
			MapInfos.parseLevelGarageStatuses(_maps, levelGarageStatuses, debugOutputPrefixStr+".levelGarageStatuses");
			MapInfos.parseVisitedLevels      (_maps, visitedLevels      , debugOutputPrefixStr+".visitedLevels"      );
		}
		
		private static void checkObjectOrNull(JSON_Object<NV, V> object, JSON_Data.Null null_, String fieldName, String debugOutputPrefixStr) throws TraverseException
		{
			if (object==null && null_==null)
				throw new TraverseException("%s.%s isn't a ObjectValue or Null", debugOutputPrefixStr, fieldName);
		}
		
		private static void checkEmptyArray(JSON_Array<NV, V> array, String fieldName, String debugOutputPrefixStr, String fileName)
		{
			if (array!=null && !array.isEmpty())
				System.err.printf("Array %s.%s is not empty as expected in file \"%s\".%n", debugOutputPrefixStr, fieldName, fileName);
		}

		private static void checkEmptyObject(JSON_Object<NV, V> object, String fieldName, String debugOutputPrefixStr)
		{
			KNOWN_JSON_VALUES__EMPTY_OBJECT.scanUnexpectedValues(object, debugOutputPrefixStr+"."+fieldName);
		}

		private static long parseTimestamp(JSON_Object<NV, V> object, String subValueName, String debugOutputPrefixStr) throws TraverseException
		{
			JSON_Object<NV, V> saveTime = JSON_Data.getObjectValue(object, subValueName, debugOutputPrefixStr);
			String timestampStr = JSON_Data.getStringValue(saveTime, "timestamp", debugOutputPrefixStr+".saveTime");
			KNOWN_JSON_VALUES_Timestamp.scanUnexpectedValues(saveTime, debugOutputPrefixStr+".saveTime");
			
			if (!timestampStr.startsWith("0x"))
				throw new JSON_Data.TraverseException("Unexpected string value in %s: %s", debugOutputPrefixStr+".saveTime.timestamp", timestampStr);
			
			return Long.parseUnsignedLong(timestampStr.substring(2), 16);
		}

		private static String parseNameValue(JSON_Object<NV, V> object, String fieldName, String debugOutputPrefixStr) throws TraverseException
		{
			JSON_Object<NV, V> obj2 = JSON_Data.getObjectValue(object, fieldName, debugOutputPrefixStr              );
			String             str  = JSON_Data.getStringValue(obj2  , "name"   , debugOutputPrefixStr+"."+fieldName);
			KNOWN_JSON_VALUES_NameValue.scanUnexpectedValues(obj2, debugOutputPrefixStr+"."+fieldName);
			return str;
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
			//if (garages.isEmpty()) { if (textOutput!=null) textOutput.printf("No garages found in SaveGame.%n"); return 0; }
			
			if (textOutput!=null)
				textOutput.printf("<%s> Trucks in garages?%n", truck.id);
			
			int count = 0;
			for (MapInfos map : _maps.values())
			{
				Garage garage = map.garage;
				if (garage!=null)
					for (int i=0; i<garage.garageSlots.length; i++)
					{
						TruckDesc truckInSlot = garage.garageSlots[i];
						if (truckInSlot!=null && truckInSlot.type.equals(truck.id))
						{
							count++;
							if (textOutput!=null)
								textOutput.printf("    Truck <%s> found in garage \"%s\", slot %d.%n", truck.id, garage.name, i+1);
						}
					}
			}
			
			if (textOutput!=null && count==0)
				textOutput.printf("    Truck <%s> not found in any garage.%n", truck.id);
			
			return count;
		}
		
		public static class PersistentProfileData
		{
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(PersistentProfileData.class)
					.add("experience"              , JSON_Data.Value.Type.Integer)
					.add("money"                   , JSON_Data.Value.Type.Integer)
					.add("rank"                    , JSON_Data.Value.Type.Integer)
					.add("ownedTrucks"             , JSON_Data.Value.Type.Object )
					.add("trucksInWarehouse"       , JSON_Data.Value.Type.Array  )
					.add("ownedTrucks"             , JSON_Data.Value.Type.Object )
					.add("contestAttempts"         , JSON_Data.Value.Type.Object )
					.add("contestLastTimes"        , JSON_Data.Value.Type.Object )
					.add("contestTimes"            , JSON_Data.Value.Type.Object )
					.add("addons"                  , JSON_Data.Value.Type.Object )
					.add("customizationRefundMoney", JSON_Data.Value.Type.Integer)
					.add("damagableAddons"         , JSON_Data.Value.Type.Object )
					.add("discoveredTrucks"        , JSON_Data.Value.Type.Object )
					.add("discoveredUpgrades"      , JSON_Data.Value.Type.Object )
					.add("distance"                , JSON_Data.Value.Type.Object ) // unparsed
					.add("dlcNotes"                , JSON_Data.Value.Type.Array  ) // unparsed
					.add("knownRegions"            , JSON_Data.Value.Type.Array  ) // unparsed
					.add("newTrucks"               , JSON_Data.Value.Type.Array  ) // unparsed
					.add("refundGarageTruckDescs"  , JSON_Data.Value.Type.Array  ) // empty array
					.add("refundMoney"             , JSON_Data.Value.Type.Integer)
					.add("refundTruckDescs"        , JSON_Data.Value.Type.Object ) // empty object
					.add("unlockedItemNames"       , JSON_Data.Value.Type.Object ) // unparsed
					.add("userId"                  , JSON_Data.Value.Type.Object ) // empty object
					;
			
			public final long money;
			public final long experience;
			public final long rank;
			public final HashMap<String, Integer> ownedTrucks;
			public final Vector<TruckDesc> trucksInWarehouse;
			
			public final Long _customizationRefundMoney;
			public final long _refundMoney;
			
			private static PersistentProfileData parse(JSON_Object<NV, V> object, String subValueName, String debugOutputPrefixStr, SaveGame saveGame) throws TraverseException
			{
				JSON_Object<NV, V> data      = JSON_Data.getObjectValue(object, subValueName, false, true, debugOutputPrefixStr);
				JSON_Data.Null     data_Null = JSON_Data.getNullValue  (object, subValueName, false, true, debugOutputPrefixStr);
				
				if (data==null && data_Null==null)
					throw new JSON_Data.TraverseException("Unexpected type of <%s>", subValueName);
				
				if (data!=null)
					return new PersistentProfileData(saveGame, data, debugOutputPrefixStr+"."+subValueName);
				
				return null;
			}
			
			private PersistentProfileData(SaveGame saveGame, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				JSON_Object<NV, V> ownedTrucks, contestAttempts, contestLastTimes, contestTimes, addons, damagableAddons, discoveredTrucks, discoveredUpgrades, refundTruckDescs, userId;
				JSON_Array<NV, V> trucksInWarehouse, refundGarageTruckDescs;
				experience                = JSON_Data.getIntegerValue(object, "experience"              , debugOutputPrefixStr);
				money                     = JSON_Data.getIntegerValue(object, "money"                   , debugOutputPrefixStr);
				rank                      = JSON_Data.getIntegerValue(object, "rank"                    , debugOutputPrefixStr);
				ownedTrucks               = JSON_Data.getObjectValue (object, "ownedTrucks"             , debugOutputPrefixStr);
				trucksInWarehouse         = JSON_Data.getArrayValue  (object, "trucksInWarehouse"       , debugOutputPrefixStr);
				contestAttempts           = JSON_Data.getObjectValue (object, "contestAttempts"         , debugOutputPrefixStr);
				contestLastTimes          = JSON_Data.getObjectValue (object, "contestLastTimes"        , true, false, debugOutputPrefixStr);
				contestTimes              = JSON_Data.getObjectValue (object, "contestTimes"            , debugOutputPrefixStr);
				addons                    = JSON_Data.getObjectValue (object, "addons"                  , debugOutputPrefixStr);
				_customizationRefundMoney = JSON_Data.getIntegerValue(object, "customizationRefundMoney", true, false, debugOutputPrefixStr);
				damagableAddons           = JSON_Data.getObjectValue (object, "damagableAddons"         , debugOutputPrefixStr);
				discoveredTrucks          = JSON_Data.getObjectValue (object, "discoveredTrucks"        , debugOutputPrefixStr);
				discoveredUpgrades        = JSON_Data.getObjectValue (object, "discoveredUpgrades"      , debugOutputPrefixStr);
				refundGarageTruckDescs    = JSON_Data.getArrayValue  (object, "refundGarageTruckDescs"  , debugOutputPrefixStr);
				_refundMoney              = JSON_Data.getIntegerValue(object, "refundMoney"             , debugOutputPrefixStr);
				refundTruckDescs          = JSON_Data.getObjectValue (object, "refundTruckDescs"        , debugOutputPrefixStr);
				userId                    = JSON_Data.getObjectValue (object, "userId"                  , debugOutputPrefixStr);
				/*
					unparsed:
						distance, dlcNotes, newTrucks
						
					unparsed, but interesting:
						knownRegions
						unlockedItemNames -> differentiate addons from trucks
						
					empty:
						refundGarageTruckDescs, refundTruckDescs, userId
				 */
				
				KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				
				checkEmptyObject(refundTruckDescs      , "refundTruckDescs"      , debugOutputPrefixStr);
				checkEmptyObject(userId                , "userId"                , debugOutputPrefixStr);
				checkEmptyArray (refundGarageTruckDescs, "refundGarageTruckDescs", debugOutputPrefixStr, saveGame.fileName);
				
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
				
				MapInfos.parseDiscoveredTrucks  (saveGame._maps , discoveredTrucks  , debugOutputPrefixStr+".discoveredTrucks"  );
				MapInfos.parseDiscoveredUpgrades(saveGame._maps , discoveredUpgrades, debugOutputPrefixStr+".discoveredUpgrades");
				Contest.parseContestAttempts (saveGame._contests, contestAttempts   , debugOutputPrefixStr+".contestAttempts"   );
				Contest.parseContestLastTimes(saveGame._contests, contestLastTimes  , debugOutputPrefixStr+".contestLastTimes"  );
				Contest.parseContestTimes    (saveGame._contests, contestTimes      , debugOutputPrefixStr+".contestTimes"      );
				Addon.parseOwned          (saveGame._addons     , addons            , debugOutputPrefixStr+".addons"            );
				Addon.parseDamagableAddons(saveGame._addons     , damagableAddons   , debugOutputPrefixStr+".damagableAddons"   );
				
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
			public final Vector<InstalledAddon> addons;
			public final long    damage;
			public final String  engine;
			public final long    engineDamage;
			public final double  fuel;
			public final long    fuelTankDamage;
			public final String  gearbox;
			public final long    gearboxDamage;
			public final String  itemForObjectiveId;
			public final boolean needToInstallDefaultAddons;
			public final long    phantomMode;
			public final long    repairs;
			public final String  rims;
			public final String  suspension;
			public final long    suspensionDamage;
			public final String  tires;
			public final String  trailerGlobalId;
			public final Long    truckCRC;
			public final long    wheelRepairs;
			public final double  wheelsScale;
			public final String  wheels;
			public final String  winch;

			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(TruckDesc.class)
					.add("type"                      , JSON_Data.Value.Type.String )
					.add("globalId"                  , JSON_Data.Value.Type.String )
					.add("id"                        , JSON_Data.Value.Type.String )
					.add("retainedMapId"             , JSON_Data.Value.Type.String )
					.add("isInvalid"                 , JSON_Data.Value.Type.Bool   )
					.add("isPacked"                  , JSON_Data.Value.Type.Bool   )
					.add("isUnlocked"                , JSON_Data.Value.Type.Bool   )
					                                 
					.add("addons"                    , JSON_Data.Value.Type.Array  )
					.add("constraints"               , JSON_Data.Value.Type.Array  )
					.add("controlConstrPosition"     , JSON_Data.Value.Type.Array  )
					.add("customizationPreset"       , JSON_Data.Value.Type.Object )
					.add("damage"                    , JSON_Data.Value.Type.Integer)
					.add("damageDecals"              , JSON_Data.Value.Type.Array  )
					.add("engine"                    , JSON_Data.Value.Type.Object )
					.add("engineDamage"              , JSON_Data.Value.Type.Integer)
					.add("fuel"                      , JSON_Data.Value.Type.Float  )
					.add("fuelTankDamage"            , JSON_Data.Value.Type.Integer)
					.add("gearbox"                   , JSON_Data.Value.Type.Object )
					.add("gearboxDamage"             , JSON_Data.Value.Type.Integer)
					.add("isPoweredEngaged"          , JSON_Data.Value.Type.Array  )
					.add("itemForObjectiveId"        , JSON_Data.Value.Type.String )
					.add("needToInstallDefaultAddons", JSON_Data.Value.Type.Bool   )
					.add("phantomMode"               , JSON_Data.Value.Type.Integer)
					.add("repairs"                   , JSON_Data.Value.Type.Integer)
					.add("rims"                      , JSON_Data.Value.Type.String )
					.add("suspension"                , JSON_Data.Value.Type.String )
					.add("suspensionDamage"          , JSON_Data.Value.Type.Integer)
					.add("tires"                     , JSON_Data.Value.Type.String )
					.add("tmBodies"                  , JSON_Data.Value.Type.Array  )
					.add("trailerGlobalId"           , JSON_Data.Value.Type.String )
					.add("truckCRC"                  , JSON_Data.Value.Type.Integer)
					.add("wheelRepairs"              , JSON_Data.Value.Type.Integer)
					.add("wheelsDamage"              , JSON_Data.Value.Type.Array  )
					.add("wheelsScale"               , JSON_Data.Value.Type.Float  )
					.add("wheelsSuspHeight"          , JSON_Data.Value.Type.Array  )
					.add("wheelsType"                , JSON_Data.Value.Type.String )
					.add("winchUpgrade"              , JSON_Data.Value.Type.Object )
					;
		
			private TruckDesc(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				//optionalValues.scan(object, "TruckDesc");
				
				//JSON_Object<NV, V> engine;
				JSON_Array<NV, V> addons, constraints, controlConstrPosition, isPoweredEngaged, tmBodies;
				type                       = JSON_Data.getStringValue (object, "type"                      , debugOutputPrefixStr);
				globalId                   = JSON_Data.getStringValue (object, "globalId"                  , debugOutputPrefixStr);
				id                         = JSON_Data.getStringValue (object, "id"                        , debugOutputPrefixStr);
				retainedMapId              = JSON_Data.getStringValue (object, "retainedMapId"             , debugOutputPrefixStr);
				isInvalid                  = JSON_Data.getBoolValue   (object, "isInvalid"                 , debugOutputPrefixStr);
				isPacked                   = JSON_Data.getBoolValue   (object, "isPacked"                  , debugOutputPrefixStr);
				isUnlocked                 = JSON_Data.getBoolValue   (object, "isUnlocked"                , debugOutputPrefixStr);
				                                                                                           
				addons                     = JSON_Data.getArrayValue  (object, "addons"                    , debugOutputPrefixStr);
				constraints                = JSON_Data.getArrayValue  (object, "constraints"               , debugOutputPrefixStr);
				controlConstrPosition      = JSON_Data.getArrayValue  (object, "controlConstrPosition"     , true, false, debugOutputPrefixStr);
				damage                     = JSON_Data.getIntegerValue(object, "damage"                    , debugOutputPrefixStr);
				engine                     = parseNameValue           (object, "engine"                    , debugOutputPrefixStr);
				engineDamage               = JSON_Data.getIntegerValue(object, "engineDamage"              , debugOutputPrefixStr);
				fuel                       = JSON_Data.getFloatValue  (object, "fuel"                      , debugOutputPrefixStr);
				fuelTankDamage             = JSON_Data.getIntegerValue(object, "fuelTankDamage"            , debugOutputPrefixStr);
				gearbox                    = parseNameValue           (object, "gearbox"                   , debugOutputPrefixStr);
				gearboxDamage              = JSON_Data.getIntegerValue(object, "gearboxDamage"             , debugOutputPrefixStr);
				isPoweredEngaged           = JSON_Data.getArrayValue  (object, "isPoweredEngaged"          , debugOutputPrefixStr);
				itemForObjectiveId         = JSON_Data.getStringValue (object, "itemForObjectiveId"        , debugOutputPrefixStr);
				needToInstallDefaultAddons = JSON_Data.getBoolValue   (object, "needToInstallDefaultAddons", debugOutputPrefixStr);
				phantomMode                = JSON_Data.getIntegerValue(object, "phantomMode"               , debugOutputPrefixStr);
				repairs                    = JSON_Data.getIntegerValue(object, "repairs"                   , debugOutputPrefixStr);
				rims                       = JSON_Data.getStringValue (object, "rims"                      , debugOutputPrefixStr);
				suspension                 = JSON_Data.getStringValue (object, "suspension"                , debugOutputPrefixStr);
				suspensionDamage           = JSON_Data.getIntegerValue(object, "suspensionDamage"          , debugOutputPrefixStr);
				tires                      = JSON_Data.getStringValue (object, "tires"                     , debugOutputPrefixStr);
				tmBodies                   = JSON_Data.getArrayValue  (object, "tmBodies"                  , debugOutputPrefixStr);
				trailerGlobalId            = JSON_Data.getStringValue (object, "trailerGlobalId"           , debugOutputPrefixStr);
				truckCRC                   = JSON_Data.getIntegerValue(object, "truckCRC"                  , true, false, debugOutputPrefixStr);
				wheelRepairs               = JSON_Data.getIntegerValue(object, "wheelRepairs"              , debugOutputPrefixStr);
			//	wheelsDamage               = JSON_Data.getArrayValue  (object, "wheelsDamage"              , debugOutputPrefixStr);
				wheelsScale                = JSON_Data.getFloatValue  (object, "wheelsScale"               , debugOutputPrefixStr);
				wheels                     = JSON_Data.getStringValue (object, "wheelsType"                , debugOutputPrefixStr);
				winch                      = parseNameValue           (object, "winchUpgrade"              , debugOutputPrefixStr);
				/*
					unparsed:
						customizationPreset, damageDecals, wheelsSuspHeight
						
					unparsed, but interesting:
						wheelsDamage
						
					empty:
						constraints, controlConstrPosition, isPoweredEngaged, tmBodies
				 */

				KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				checkEmptyArray (constraints          , "constraints"          , debugOutputPrefixStr, "<fileName>");
				checkEmptyArray (controlConstrPosition, "controlConstrPosition", debugOutputPrefixStr, "<fileName>");
				checkEmptyArray (isPoweredEngaged     , "isPoweredEngaged"     , debugOutputPrefixStr, "<fileName>");
				checkEmptyArray (tmBodies             , "tmBodies"             , debugOutputPrefixStr, "<fileName>");
				
				this.addons = new Vector<>();
				for (int i=0; i<addons.size(); i++)
				{
					String local_debugOutputPrefixStr = debugOutputPrefixStr+".addons["+i+"]";
					JSON_Object<NV, V> obj = JSON_Data.getObjectValue(addons.get(i), local_debugOutputPrefixStr);
					this.addons.add(new InstalledAddon(obj,local_debugOutputPrefixStr));
				}
			}
			
			public static class InstalledAddon
			{
				@SuppressWarnings("unused")
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(TruckDesc.class)
//						.add("fuel"      , JSON_Data.Value.Type.Float )
						;
				
				public final String id;

				public InstalledAddon(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					id          = JSON_Data.getStringValue(object, "name"         , debugOutputPrefixStr);
					// TODO: parse more values in InstalledAddon
					//KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				}
				
			}
		}
		
		public static class SpreadedValuesHelper<ValueType>
		{
			private final Function<String, ValueType> constructor;

			SpreadedValuesHelper(Function<String,ValueType> constructor)
			{
				this.constructor = constructor;
			}
			
			ValueType get(HashMap<String, ValueType> valueMap, String valueID)
			{
				ValueType value = valueMap.get(valueID);
				if (value==null) valueMap.put(valueID, value = constructor.apply(valueID));
				return value;
			}
			
			private interface Action<ValueType>
			{
				void parseValues(ValueType value, JSON_Data.NamedValue<NV,V> nv) throws TraverseException;
			}
			
			void forEachNV(HashMap<String, ValueType> maps, JSON_Object<NV, V> object, Action<ValueType> parseValues) throws TraverseException
			{
				if (object!=null)
					for (JSON_Data.NamedValue<NV,V> nv : object)
					{
						String mapId = nv.name;
						ValueType map = get(maps, mapId);
						parseValues.parseValues(map, nv);
					}
			}
		}
		
		public static class Addon
		{
			private static final SpreadedValuesHelper<Addon> helper = new SpreadedValuesHelper<>(Addon::new);
			
			public final String addonId;
			public Long owned = null;
			public DamagableData damagable = null;
			
			private Addon(String addonId)
			{
				this.addonId = addonId;
			}

			private static void parseOwned(HashMap<String, Addon> addons, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(addons, object, (addon,nv) -> {
					addon.owned = JSON_Data.getIntegerValue(nv.value, debugOutputPrefixStr+"."+nv.name);
				});
			}

			public static void parseDamagableAddons(HashMap<String, Addon> addons, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(addons, object, (addon,nv) -> {
					JSON_Object<NV, V> datObj = JSON_Data.getObjectValue(nv.value, debugOutputPrefixStr+"."+nv.name);
					addon.damagable = new DamagableData(datObj, debugOutputPrefixStr+"."+nv.name);
				});
			}
			
			public static class DamagableData
			{
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(DamagableData.class)
						.add("itemsDamage", JSON_Data.Value.Type.Array);
				
				public final long[][] itemsDamage;
				
				private DamagableData(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					//optionalValues.scan(object, "Addon.DamagableData");
					
					JSON_Array<NV, V> itemsDamage = JSON_Data.getArrayValue(object, "itemsDamage", debugOutputPrefixStr);
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
					
					Vector<Vector<Long>> itemsDamage_parsed = new Vector<>();
					for (int i=0; i<itemsDamage.size(); i++)
					{
						String prefixStr1 = debugOutputPrefixStr+"["+i+"]";
						JSON_Array<NV, V> array_unparsed = JSON_Data.getArrayValue(itemsDamage.get(i), prefixStr1);
						Vector<Long> vector_parsed = new Vector<>();
						itemsDamage_parsed.add(vector_parsed);
						for (int j=0; j<array_unparsed.size(); j++)
						{
							JSON_Data.Value<NV,V> v1 = array_unparsed.get(j);
							long value = JSON_Data.getIntegerValue(v1, prefixStr1+"["+j+"]");
							vector_parsed.add(value);
						}
					}
					
					this.itemsDamage = new long[itemsDamage_parsed.size()][];
					for (int i=0; i<itemsDamage_parsed.size(); i++)
					{
						Vector<Long> vec1 = itemsDamage_parsed.get(i);
						this.itemsDamage[i] = new long[vec1.size()];
						for (int j=0; j<vec1.size(); j++)
						{
							Long value = vec1.get(j);
							this.itemsDamage[i][j] = value;
						}
					}
				}

				@Override
				public String toString()
				{
					Function<long[],List<String>> convert1 = arr->Arrays.stream(arr).mapToObj(n->Long.toString(n)).toList();
					Function<List<String>,String> toString = list->String.format("[%s]", String.join(",", list));
					return toString.apply( Arrays.stream(itemsDamage).map( arr->toString.apply( convert1.apply(arr) ) ).toList() );
				}
			}
		}
		
		public static class Contest
		{
			private static final SpreadedValuesHelper<Contest> helper = new SpreadedValuesHelper<>(Contest::new);
			
			public final String contestId;
			public Long attempts = null;
			public Long lastTimes = null;
			public Long times = null;
			
			private Contest(String contestId)
			{
				this.contestId = contestId;
			}

			private static void parseContestAttempts(HashMap<String, Contest> contests, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(contests, object, (contest,nv) -> {
					contest.attempts  = JSON_Data.getIntegerValue(nv.value, debugOutputPrefixStr+"."+nv.name);
				});
			}

			private static void parseContestLastTimes(HashMap<String, Contest> contests, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(contests, object, (contest,nv) -> {
					contest.lastTimes = JSON_Data.getIntegerValue(nv.value, debugOutputPrefixStr+"."+nv.name);
				});
			}

			private static void parseContestTimes(HashMap<String, Contest> contests, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(contests, object, (contest,nv) -> {
					contest.times     = JSON_Data.getIntegerValue(nv.value, debugOutputPrefixStr+"."+nv.name);
				});
			}
		}
		
		public static class MapInfos
		{
			private static final SpreadedValuesHelper<MapInfos> helper = new SpreadedValuesHelper<>(MapInfos::new);
			
			public final String mapId;
			public boolean wasVisited = false;
			public Garage garage = null;
			public Long garageStatus = null;
			public DiscoveredObjects discoveredTrucks = null;
			public DiscoveredObjects discoveredUpgrades = null;
			
			private MapInfos(String mapId)
			{
				this.mapId = mapId;
			}
			
			private static void parseGaragesData(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(maps, object, (map,nv) -> {
					JSON_Object<NV, V> garageObject = JSON_Data.getObjectValue(nv.value, debugOutputPrefixStr+"."+nv.name);
					if (map.garage!=null) System.err.printf("Found more than 1 garage on 1 map: %s%n", nv.name);
					else map.garage = new Garage(garageObject, nv.name, debugOutputPrefixStr+"."+nv.name);
				});
			}

			private static void parseLevelGarageStatuses(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(maps, object, (map,nv) -> {
					map.garageStatus = JSON_Data.getIntegerValue(nv.value, debugOutputPrefixStr+"."+nv.name);
				});
			}

			private static void parseVisitedLevels(HashMap<String, MapInfos> maps, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				for (int i=0; i<array.size(); i++)
				{
					JSON_Data.Value<NV,V> value = array.get(i);
					String mapId = JSON_Data.getStringValue(value, debugOutputPrefixStr+"["+i+"]");
					MapInfos map = helper.get(maps, mapId);
					map.wasVisited = true;
				}
			}

			private static void parseDiscoveredTrucks(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				parseDiscoveredObjects(maps, (map,dobs)->map.discoveredTrucks = dobs, object, debugOutputPrefixStr);
			}

			private static void parseDiscoveredUpgrades(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				parseDiscoveredObjects(maps, (map,dobs)->map.discoveredUpgrades = dobs, object, debugOutputPrefixStr);
			}

			private static void parseDiscoveredObjects(HashMap<String, MapInfos> maps, BiConsumer<MapInfos,DiscoveredObjects> setValue, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.forEachNV(maps, object, (map,nv) -> {
					JSON_Object<NV, V> dataObj = JSON_Data.getObjectValue(nv.value, debugOutputPrefixStr+"."+nv.name);
					setValue.accept(map, new DiscoveredObjects(dataObj, debugOutputPrefixStr+"."+nv.name));
				});
			}
			
			public static class DiscoveredObjects
			{
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(DiscoveredObjects.class)
						.add("all"    , JSON_Data.Value.Type.Integer)
						.add("current", JSON_Data.Value.Type.Integer);
				
				public final long all;
				public final long current;
				
				private DiscoveredObjects(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					//optionalValues.scan(object, "MapInfos.DiscoveredObjects");
					
					all     = JSON_Data.getIntegerValue(object, "all"    , debugOutputPrefixStr);
					current = JSON_Data.getIntegerValue(object, "current", debugOutputPrefixStr);
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				}

				@Override
				public String toString()
				{
					return String.format("%d / %d", current, all);
				}
				
				
			}
		}
	}

}
