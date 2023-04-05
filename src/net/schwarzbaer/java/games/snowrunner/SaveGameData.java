package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import net.schwarzbaer.java.games.snowrunner.Data.MapIndex;
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
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_watchPointsData = KJV_FACTORY.create()
				.add("data"           , JSON_Data.Value.Type.Object);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Timestamp = KJV_FACTORY.create()
				.add("timestamp"      , JSON_Data.Value.Type.String);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_NameValue = KJV_FACTORY.create()
				.add("name"           , JSON_Data.Value.Type.String );
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES__EMPTY_OBJECT = KJV_FACTORY.create();
		
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_SslValue = KJV_FACTORY.create("[SaveGame]<root>.CompleteSave.SslValue")
				.add("birthVersion"                      , JSON_Data.Value.Type.Integer)
				.add("cargoLoadingCounts"                , JSON_Data.Value.Type.Object )
				.add("discoveredObjectives"              , JSON_Data.Value.Type.Array  )
				.add("discoveredObjects"                 , JSON_Data.Value.Type.Array  )
				.add("finishedObjs"                      , JSON_Data.Value.Type.Array  )
				.add("forcedModelStates"                 , JSON_Data.Value.Type.Object ) // empty
				.add("gameDifficultyMode"                , JSON_Data.Value.Type.Integer)
				.add("gameDifficultySettings"            , JSON_Data.Value.Type.Object ) // unparsed
				.add("gameStat"                          , JSON_Data.Value.Type.Object ) // unparsed
				.add("gameStatByRegion"                  , JSON_Data.Value.Type.Object ) // unparsed
				.add("gameTime"                          , JSON_Data.Value.Type.Float  )
				.add("garagesData"                       , JSON_Data.Value.Type.Object )
				.add("garagesShopData"                   , JSON_Data.Value.Type.Null   )
				.add("givenTrialRewards"                 , JSON_Data.Value.Type.Array  ) // empty
				.add("hiddenCargoes"                     , JSON_Data.Value.Type.Object )
				.add("isFirstGarageDiscovered"           , JSON_Data.Value.Type.Bool   )
				.add("isHardMode"                        , JSON_Data.Value.Type.Bool   )
				.add("justDiscoveredObjects"             , JSON_Data.Value.Type.Object ) // empty
				.add("lastLevelState"                    , JSON_Data.Value.Type.Integer)
				.add("lastLoadedLevel"                   , JSON_Data.Value.Type.String )
				.add("lastPhantomMode"                   , JSON_Data.Value.Type.Integer)
				.add("levelGarageStatuses"               , JSON_Data.Value.Type.Object )
				.add("levelGarageStatuses"               , JSON_Data.Value.Type.Null   )
				.add("metricSystem"                      , JSON_Data.Value.Type.Integer)
				.add("modTruckOnLevels"                  , JSON_Data.Value.Type.Object )
				.add("modTruckRefundValues"              , JSON_Data.Value.Type.Object ) // empty
				.add("modTruckTypesRefundValues"         , JSON_Data.Value.Type.Object ) // empty
				.add("objVersion"                        , JSON_Data.Value.Type.Integer)
				.add("objectiveStates"                   , JSON_Data.Value.Type.Object )
				.add("objectivesValidated"               , JSON_Data.Value.Type.Bool   )
				.add("persistentProfileData"             , JSON_Data.Value.Type.Object )
				.add("persistentProfileData"             , JSON_Data.Value.Type.Null   )
				.add("saveId"                            , JSON_Data.Value.Type.Integer)
				.add("saveTime"                          , JSON_Data.Value.Type.Object )
				.add("savedCargoNeedToBeRemovedOnRestart", JSON_Data.Value.Type.Object )
				.add("trackedObjective"                  , JSON_Data.Value.Type.String )
				.add("tutorialStates"                    , JSON_Data.Value.Type.Object )
				.add("tutorialStates"                    , JSON_Data.Value.Type.Null   )
				.add("upgradableGarages"                 , JSON_Data.Value.Type.Object ) // unparsed
				.add("upgradesGiverData"                 , JSON_Data.Value.Type.Object )
				.add("viewedUnactivatedObjectives"       , JSON_Data.Value.Type.Array  )
				.add("visitedLevels"                     , JSON_Data.Value.Type.Array  )
				.add("watchPointsData"                   , JSON_Data.Value.Type.Object )
				.add("watchPointsData"                   , JSON_Data.Value.Type.Null   )
				.add("waypoints"                         , JSON_Data.Value.Type.Object )
				.add("worldConfiguration"                , JSON_Data.Value.Type.String )
				;
	
		public final String fileName;
		public final String indexStr;
		public final JSON_Data.Value<NV, V> data;
		
		// basic values
		public final long saveTime;
		public final double gameTime;
		public final boolean isHardMode;
		public final String worldConfiguration;
		public final long birthVersion;
		public final Long gameDifficultyMode;
		
		// complex values
		public final PersistentProfileData ppd;
		public final HashMap<String, MapInfos > maps;
		public final HashMap<String, Objective> objectives;
		public final HashMap<String, Addon    > addons;
		
		// currently not shown data
		public final boolean isFirstGarageDiscovered;
		public final long lastLevelState;
		public final String lastLoadedLevel;
		public final long lastPhantomMode;
		public final long metricSystem;
		public final long objVersion;
		public final boolean objectivesValidated;
		public final long saveId;
		public final String trackedObjective;
		public final HashMap<String, Boolean> tutorialStates;

		private SaveGame(String fileName, String indexStr, JSON_Data.Value<NV, V> data) throws TraverseException {
			if (data==null)
				throw new IllegalArgumentException();
			
			this.fileName = fileName;
			this.indexStr = indexStr;
			this.data = data;
			maps       = new HashMap<>();
			objectives = new HashMap<>();
			addons     = new HashMap<>();
			
			JSON_Object<NV, V> rootObject      = JSON_Data.getObjectValue(this.data      ,                          "SaveGame.<root>");
			JSON_Object<NV, V> complSaveObject = JSON_Data.getObjectValue(rootObject     , "CompleteSave"+indexStr, "SaveGame.<root>");
			JSON_Object<NV, V> sslValueObj     = JSON_Data.getObjectValue(complSaveObject, "SslValue"             , "SaveGame.<root>.CompleteSave"+indexStr);
			KNOWN_JSON_VALUES_Root.add("CompleteSave"+indexStr, JSON_Data.Value.Type.Object); // adding known fields dynamically :)
			KNOWN_JSON_VALUES_Root        .scanUnexpectedValues(rootObject     );
			KNOWN_JSON_VALUES_CompleteSave.scanUnexpectedValues(complSaveObject);
			
			String debugOutputPrefixStr = "CompleteSave"+indexStr+".SslValue";
			
			JSON_Object<NV, V> cargoLoadingCounts, forcedModelStates, garagesData, hiddenCargoes, justDiscoveredObjects, levelGarageStatuses,
				modTruckOnLevels, modTruckRefundValues, modTruckTypesRefundValues, objectiveStates, persistentProfileData,
				savedCargoNeedToBeRemovedOnRestart, tutorialStates, upgradesGiverData, watchPointsData, waypoints;
			
			JSON_Array<NV, V> visitedLevels, discoveredObjectives, discoveredObjects, finishedObjs, givenTrialRewards, viewedUnactivatedObjectives;
			
			@SuppressWarnings("unused")
			JSON_Data.Null levelGarageStatuses_Null, garagesShopData_Null, persistentProfileData_Null, tutorialStates_Null, watchPointsData_Null;
			
			birthVersion                       = JSON_Data.getIntegerValue  (sslValueObj, "birthVersion"                      , debugOutputPrefixStr);
			cargoLoadingCounts                 = JSON_Data.getObjectValue   (sslValueObj, "cargoLoadingCounts"                , debugOutputPrefixStr);
			discoveredObjectives               = JSON_Data.getArrayValue    (sslValueObj, "discoveredObjectives"              , debugOutputPrefixStr);
			discoveredObjects                  = JSON_Data.getArrayValue    (sslValueObj, "discoveredObjects"                 , debugOutputPrefixStr);
			finishedObjs                       = JSON_Data.getArrayValue    (sslValueObj, "finishedObjs"                      , true, false, debugOutputPrefixStr);
			forcedModelStates                  = JSON_Data.getObjectValue   (sslValueObj, "forcedModelStates"                 , debugOutputPrefixStr);
			gameDifficultyMode                 = JSON_Data.getIntegerValue  (sslValueObj, "gameDifficultyMode"                , true, false, debugOutputPrefixStr);
			gameTime                           = JSON_Data.getFloatValue    (sslValueObj, "gameTime"                          , debugOutputPrefixStr);
			garagesData                        = JSON_Data.getObjectValue   (sslValueObj, "garagesData"                       , debugOutputPrefixStr);
			garagesShopData_Null               = JSON_Data.getNullValue     (sslValueObj, "garagesShopData"                   , true, false, debugOutputPrefixStr);
			givenTrialRewards                  = JSON_Data.getArrayValue    (sslValueObj, "givenTrialRewards"                 , debugOutputPrefixStr);
			hiddenCargoes                      = JSON_Data.getObjectValue   (sslValueObj, "hiddenCargoes"                     , debugOutputPrefixStr);
			isFirstGarageDiscovered            = JSON_Data.getBoolValue     (sslValueObj, "isFirstGarageDiscovered"           , debugOutputPrefixStr);
			isHardMode                         = JSON_Data.getBoolValue     (sslValueObj, "isHardMode"                        , debugOutputPrefixStr);
			justDiscoveredObjects              = JSON_Data.getObjectValue   (sslValueObj, "justDiscoveredObjects"             , debugOutputPrefixStr);
			lastLevelState                     = JSON_Data.getIntegerValue  (sslValueObj, "lastLevelState"                    , debugOutputPrefixStr);
			lastLoadedLevel                    = JSON_Data.getStringValue   (sslValueObj, "lastLoadedLevel"                   , debugOutputPrefixStr);
			lastPhantomMode                    = JSON_Data.getIntegerValue  (sslValueObj, "lastPhantomMode"                   , debugOutputPrefixStr);
			levelGarageStatuses                = JSON_Data.getObjectValue   (sslValueObj, "levelGarageStatuses"               , false, true, debugOutputPrefixStr);
			levelGarageStatuses_Null           = JSON_Data.getNullValue     (sslValueObj, "levelGarageStatuses"               , false, true, debugOutputPrefixStr);
			metricSystem                       = JSON_Data.getIntegerValue  (sslValueObj, "metricSystem"                      , debugOutputPrefixStr);
			modTruckOnLevels                   = JSON_Data.getObjectValue   (sslValueObj, "modTruckOnLevels"                  , debugOutputPrefixStr);
			modTruckRefundValues               = JSON_Data.getObjectValue   (sslValueObj, "modTruckRefundValues"              , debugOutputPrefixStr);
			modTruckTypesRefundValues          = JSON_Data.getObjectValue   (sslValueObj, "modTruckTypesRefundValues"         , debugOutputPrefixStr);
			objVersion                         = JSON_Data.getIntegerValue  (sslValueObj, "objVersion"                        , debugOutputPrefixStr);
			objectiveStates                    = JSON_Data.getObjectValue   (sslValueObj, "objectiveStates"                   , debugOutputPrefixStr);
			objectivesValidated                = JSON_Data.getBoolValue     (sslValueObj, "objectivesValidated"               , debugOutputPrefixStr);
			persistentProfileData              = JSON_Data.getObjectValue   (sslValueObj, "persistentProfileData"             , false, true, debugOutputPrefixStr);
			persistentProfileData_Null         = JSON_Data.getNullValue     (sslValueObj, "persistentProfileData"             , false, true, debugOutputPrefixStr);
			saveId                             = JSON_Data.getIntegerValue  (sslValueObj, "saveId"                            , debugOutputPrefixStr);
			saveTime                           = parseTimestamp             (sslValueObj, "saveTime"                          , debugOutputPrefixStr);
			savedCargoNeedToBeRemovedOnRestart = JSON_Data.getObjectValue   (sslValueObj, "savedCargoNeedToBeRemovedOnRestart", debugOutputPrefixStr);
			trackedObjective                   = JSON_Data.getStringValue   (sslValueObj, "trackedObjective"                  , debugOutputPrefixStr);
			tutorialStates                     = JSON_Data.getObjectValue   (sslValueObj, "tutorialStates"                    , false, true, debugOutputPrefixStr);
			tutorialStates_Null                = JSON_Data.getNullValue     (sslValueObj, "tutorialStates"                    , false, true, debugOutputPrefixStr);
			upgradesGiverData                  = JSON_Data.getObjectValue   (sslValueObj, "upgradesGiverData"                 , debugOutputPrefixStr);
			viewedUnactivatedObjectives        = JSON_Data.getArrayValue    (sslValueObj, "viewedUnactivatedObjectives"       , debugOutputPrefixStr);
			visitedLevels                      = JSON_Data.getArrayValue    (sslValueObj, "visitedLevels"                     , debugOutputPrefixStr);
			watchPointsData                    = JSON_Data.getObjectValue   (sslValueObj, "watchPointsData"                   , false, true, debugOutputPrefixStr);
			watchPointsData_Null               = JSON_Data.getNullValue     (sslValueObj, "watchPointsData"                   , false, true, debugOutputPrefixStr);
			waypoints                          = JSON_Data.getObjectValue   (sslValueObj, "waypoints"                         , debugOutputPrefixStr);
			worldConfiguration                 = JSON_Data.getStringValue   (sslValueObj, "worldConfiguration"                , debugOutputPrefixStr);
			/*
				unparsed:
					gameDifficultySettings, gameStat, gameStatByRegion
					
				unparsed, but interesting:
					upgradableGarages
					
				empty:
					forcedModelStates, discoveredObjects, givenTrialRewards, justDiscoveredObjects, modTruckRefundValues, modTruckTypesRefundValues
			 */
			KNOWN_JSON_VALUES_SslValue.scanUnexpectedValues(sslValueObj);
			
			checkObjectOrNull(levelGarageStatuses  , levelGarageStatuses_Null  , debugOutputPrefixStr+".levelGarageStatuses"  );
			checkObjectOrNull(persistentProfileData, persistentProfileData_Null, debugOutputPrefixStr+".persistentProfileData");
			checkObjectOrNull(tutorialStates       , tutorialStates_Null       , debugOutputPrefixStr+".tutorialStates"       );
			checkObjectOrNull(watchPointsData      , watchPointsData_Null      , debugOutputPrefixStr+".watchPointsData"      );
			checkEmptyObject (forcedModelStates        , debugOutputPrefixStr+".forcedModelStates"        );
			checkEmptyArray  (givenTrialRewards        , debugOutputPrefixStr+".givenTrialRewards"        );
			checkEmptyObject (justDiscoveredObjects    , debugOutputPrefixStr+".justDiscoveredObjects"    );
			checkEmptyObject (modTruckRefundValues     , debugOutputPrefixStr+".modTruckRefundValues"     );
			checkEmptyObject (modTruckTypesRefundValues, debugOutputPrefixStr+".modTruckTypesRefundValues");
			
			
			if (persistentProfileData==null)
				ppd = null;
			else
				ppd = new PersistentProfileData(this, persistentProfileData, debugOutputPrefixStr+".persistentProfileData");
			
			this.tutorialStates = new HashMap<>();
			parseObject(tutorialStates, debugOutputPrefixStr+".tutorialStates", (value, tutorialStep, localPrefixStr) -> {
				boolean state = JSON_Data.getBoolValue(value, localPrefixStr);
				
				if (this.tutorialStates.containsKey(tutorialStep))
					System.err.printf("[TutorialStates] Found more than 1 entry with same TutorialStep name: %s%n", localPrefixStr);
				else
					this.tutorialStates.put(tutorialStep, state);
			});
			
			JSON_Object<NV, V> watchPointsData_data = null;
			if (watchPointsData!=null)
			{
				watchPointsData_data = JSON_Data.getObjectValue(watchPointsData, "data", debugOutputPrefixStr+".watchPointsData");
				KNOWN_JSON_VALUES_watchPointsData.scanUnexpectedValues(watchPointsData, debugOutputPrefixStr+".watchPointsData");
			}
			
			MapInfos .parseCargoLoadingCounts                (maps      , cargoLoadingCounts                , debugOutputPrefixStr+".cargoLoadingCounts"                );
			MapInfos .parseDiscoveredObjects                 (maps      , discoveredObjects                 , debugOutputPrefixStr+".discoveredObjects"                 );
			MapInfos .parseGaragesData                       (maps      , garagesData                       , debugOutputPrefixStr+".garagesData"                       );
			MapInfos .parseLevelGarageStatuses               (maps      , levelGarageStatuses               , debugOutputPrefixStr+".levelGarageStatuses"               );
			MapInfos .parseModTruckOnLevels                  (maps      , modTruckOnLevels                  , debugOutputPrefixStr+".modTruckOnLevels"                  );
			MapInfos .parseUpgradesGiverData                 (maps      , upgradesGiverData                 , debugOutputPrefixStr+".upgradesGiverData"                 );
			MapInfos .parseVisitedLevels                     (maps      , visitedLevels                     , debugOutputPrefixStr+".visitedLevels"                     );
			MapInfos .parseWatchPoints                       (maps      , watchPointsData_data              , debugOutputPrefixStr+".watchPointsData.data"              );
			MapInfos .parseWaypoints                         (maps      , waypoints                         , debugOutputPrefixStr+".waypoints"                         );
			Objective.parseDiscoveredObjectives              (objectives, discoveredObjectives              , debugOutputPrefixStr+".discoveredObjectives"              );
			Objective.parseFinishedObjs                      (objectives, finishedObjs                      , debugOutputPrefixStr+".finishedObjs"                      );
			Objective.parseHiddenCargoes                     (objectives, hiddenCargoes                     , debugOutputPrefixStr+".hiddenCargoes"                     );
			Objective.parseObjectiveStates                   (objectives, objectiveStates                   , debugOutputPrefixStr+".objectiveStates"                   );
			Objective.parseSavedCargoNeedToBeRemovedOnRestart(objectives, savedCargoNeedToBeRemovedOnRestart, debugOutputPrefixStr+".savedCargoNeedToBeRemovedOnRestart");
			Objective.parseViewedUnactivatedObjectives       (objectives, viewedUnactivatedObjectives       , debugOutputPrefixStr+".viewedUnactivatedObjectives"       );
		}
		
		private interface ParseObjectAction
		{
			void parse(JSON_Data.Value<NV,V> jsonValue, String fieldName, String local_debugOutputPrefixStr) throws TraverseException;
		}
		
		private static void parseObject(JSON_Object<NV, V> object, String debugOutputPrefixStr, ParseObjectAction action) throws TraverseException
		{
			if (object!=null)
				for (JSON_Data.NamedValue<NV,V> nv : object)
					action.parse(nv.value, nv.name, debugOutputPrefixStr+"."+nv.name);
		}
		
		private interface ParseArrayAction
		{
			void parse(JSON_Data.Value<NV,V> jsonValue, int index, String local_debugOutputPrefixStr) throws TraverseException;
		}
		
		private static void parseArray(JSON_Array<NV, V> array, String debugOutputPrefixStr, ParseArrayAction action) throws TraverseException
		{
			if (array!=null)
				for (int i=0; i<array.size(); i++)
					action.parse(array.get(i), i, debugOutputPrefixStr+"["+i+"]");
		}
		
		private static void checkObjectOrNull(JSON_Object<NV, V> object, JSON_Data.Null null_, String debugOutputPrefixStr) throws TraverseException
		{
			if (object==null && null_==null)
				throw new TraverseException("%s isn't a ObjectValue or Null", debugOutputPrefixStr);
		}
		
		private static void checkEmptyArray(JSON_Array<NV, V> array, String debugOutputPrefixStr)
		{
			if (array!=null && !array.isEmpty())
				System.err.printf("Array %s is not empty as expected.%n", debugOutputPrefixStr);
		}

		private static void checkEmptyObject(JSON_Object<NV, V> object, String debugOutputPrefixStr)
		{
			KNOWN_JSON_VALUES__EMPTY_OBJECT.scanUnexpectedValues(object, debugOutputPrefixStr);
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

		public String getGameTimeStr()
		{
			int h = (int) Math.floor(  gameTime            );
			int m = (int) Math.floor( (gameTime-h)*60      );
			int s = (int) Math.floor(((gameTime-h)*60-m)*60);
			return String.format("%d:%02d:%02d", h,m,s);
		}

		public long getOwnedTruckCount(Truck truck) {
			if (truck==null) return 0;
			if (ppd==null) return 0;
			if (ppd.ownedTrucks==null) return 0;
			Long amount = ppd.ownedTrucks.get(truck.id);
			return amount==null ? 0 : amount.longValue();
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
						if (!truckInSlot.retainedMap.originalMapID().isBlank())
							textOutput.printf(" (Map: %s)%n", truckInSlot.retainedMap.originalMapID());
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
			for (MapInfos map : maps.values())
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
		
		public static class Coord3F
		{
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Coord3F.class)
					.add("x", JSON_Data.Value.Type.Float)
					.add("y", JSON_Data.Value.Type.Float)
					.add("z", JSON_Data.Value.Type.Float);
			
			public final double x;
			public final double y;
			public final double z;
			
			private Coord3F(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				x = JSON_Data.getFloatValue(object, "x", debugOutputPrefixStr);
				y = JSON_Data.getFloatValue(object, "y", debugOutputPrefixStr);
				z = JSON_Data.getFloatValue(object, "z", debugOutputPrefixStr);
				KNOWN_JSON_VALUES.scanUnexpectedValues(object);
			}
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
			
			public final long experience;
			public final long rank;
			public final long money;
			public final Long customizationRefundMoney;
			public final long refundMoney;
			public final HashMap<String, Long> ownedTrucks;
			public final Vector<TruckDesc> trucksInWarehouse;
			
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
				customizationRefundMoney  = JSON_Data.getIntegerValue(object, "customizationRefundMoney", true, false, debugOutputPrefixStr);
				damagableAddons           = JSON_Data.getObjectValue (object, "damagableAddons"         , debugOutputPrefixStr);
				discoveredTrucks          = JSON_Data.getObjectValue (object, "discoveredTrucks"        , debugOutputPrefixStr);
				discoveredUpgrades        = JSON_Data.getObjectValue (object, "discoveredUpgrades"      , debugOutputPrefixStr);
				refundGarageTruckDescs    = JSON_Data.getArrayValue  (object, "refundGarageTruckDescs"  , debugOutputPrefixStr);
				refundMoney               = JSON_Data.getIntegerValue(object, "refundMoney"             , debugOutputPrefixStr);
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
				
				checkEmptyObject(refundTruckDescs      , debugOutputPrefixStr+".refundTruckDescs"      );
				checkEmptyObject(userId                , debugOutputPrefixStr+".userId"                );
				checkEmptyArray (refundGarageTruckDescs, debugOutputPrefixStr+".refundGarageTruckDescs");
				
				this.ownedTrucks = new HashMap<>();
				parseObject(ownedTrucks, debugOutputPrefixStr+".ownedTrucks", (value, name, local_debugOutputPrefixStr) -> {
					long amount = JSON_Data.getIntegerValue(value, local_debugOutputPrefixStr);
					this.ownedTrucks.put(name, amount);
				});
				
				this.trucksInWarehouse = new Vector<TruckDesc>();
				parseArray(trucksInWarehouse, debugOutputPrefixStr+".trucksInWarehouse", (value,i,localPrefixStr) -> {
					JSON_Object<NV, V> obj = JSON_Data.getObjectValue(value, localPrefixStr);
					this.trucksInWarehouse.add(new TruckDesc(obj, localPrefixStr));
				});
				
				MapInfos .parseDiscoveredTrucks  (saveGame.maps      , discoveredTrucks  , debugOutputPrefixStr+".discoveredTrucks"  );
				MapInfos .parseDiscoveredUpgrades(saveGame.maps      , discoveredUpgrades, debugOutputPrefixStr+".discoveredUpgrades");
				Objective.parseContestAttempts   (saveGame.objectives, contestAttempts   , debugOutputPrefixStr+".contestAttempts"   );
				Objective.parseContestLastTimes  (saveGame.objectives, contestLastTimes  , debugOutputPrefixStr+".contestLastTimes"  );
				Objective.parseContestTimes      (saveGame.objectives, contestTimes      , debugOutputPrefixStr+".contestTimes"      );
				Addon    .parseOwned             (saveGame.addons    , addons            , debugOutputPrefixStr+".addons"            );
				Addon    .parseDamagableAddons   (saveGame.addons    , damagableAddons   , debugOutputPrefixStr+".damagableAddons"   );
				
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
			public final MapIndex retainedMap;
			public final boolean isInvalid;
			public final boolean isPacked;
			public final boolean isUnlocked;
			public final Vector<InstalledAddon> addons; // TODO: show TruckDesc.addons
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
				String retainedMapId;
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
				checkEmptyArray (constraints          , debugOutputPrefixStr+".constraints"          );
				checkEmptyArray (controlConstrPosition, debugOutputPrefixStr+".controlConstrPosition");
				checkEmptyArray (isPoweredEngaged     , debugOutputPrefixStr+".isPoweredEngaged"     );
				checkEmptyArray (tmBodies             , debugOutputPrefixStr+".tmBodies"             );
				
				retainedMap = MapIndex.parse(retainedMapId);
				
				this.addons = new Vector<>();
				parseArray(addons, debugOutputPrefixStr+".addons", (value, i, local_debugOutputPrefixStr) -> {
					JSON_Object<NV, V> obj = JSON_Data.getObjectValue(value, local_debugOutputPrefixStr);
					this.addons.add(new InstalledAddon(obj,local_debugOutputPrefixStr));
				});
			}
			
			public static class InstalledAddon
			{
				@SuppressWarnings("unused")
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(InstalledAddon.class)
						.add("name"      , JSON_Data.Value.Type.String)
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
		
		private static class SpreadedValuesHelper<ValueType>
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
			
			void parseStringArray(HashMap<String, ValueType> valueMap, JSON_Array<NV, V> array, String debugOutputPrefixStr, Consumer<ValueType> setValue) throws TraverseException
			{
				parseArray(array, debugOutputPrefixStr, (jsonValue, i, local_debugOutputPrefixStr) -> {
					String valueID = JSON_Data.getStringValue(jsonValue, local_debugOutputPrefixStr);
					ValueType value = get(valueMap, valueID);
					setValue.accept(value);
				});
			}
			
			<Name extends SplittedName> void parseStringArray(HashMap<String, ValueType> valueMap, JSON_Array<NV, V> array, String debugOutputPrefixStr, Function<String,Name> splitName, BiConsumer<ValueType, Name> setValue) throws TraverseException
			{
				parseArray(array, debugOutputPrefixStr, (jsonValue, i, local_debugOutputPrefixStr) -> {
					String str = JSON_Data.getStringValue(jsonValue, local_debugOutputPrefixStr);
					Name name = splitName.apply(str);
					ValueType value = get(valueMap, name.valueID);
					setValue.accept(value,name);
				});
			}
			
			private interface Action<ValueType>
			{
				void parseValues(ValueType value, JSON_Data.Value<NV,V> jsonValue, String local_debugOutputPrefixStr) throws TraverseException;
			}
			
			void parseObject(HashMap<String, ValueType> valueMap, JSON_Object<NV, V> object, String debugOutputPrefixStr, Action<ValueType> parseValues) throws TraverseException
			{
				SaveGame.parseObject(object, debugOutputPrefixStr, (jsonValue, valueID, local_debugOutputPrefixStr) -> {
					ValueType value = get(valueMap, valueID);
					parseValues.parseValues(value, jsonValue, local_debugOutputPrefixStr);
				});
			}
			
			static class SplittedName
			{
				final String valueID;
				SplittedName(String valueID)
				{
					this.valueID = valueID;
				}
				
				static class TwoOrSplitName extends SpreadedValuesHelper.SplittedName
				{
					private final String originalStr;
					private final String secondPart;
				
					TwoOrSplitName(String originalStr, String valueId, String secondPart)
					{
						super(valueId);
						this.originalStr = originalStr;
						this.secondPart = secondPart;
					}
					
					static TwoOrSplitName split(String fieldName)
					{
						// "level_us_03_01 || US_03_01_CR_WD_01"
						// "level_ru_02_01_crop || RU_02_01_BLD_GAS"
						String[] parts = fieldName.split(" \\|\\| ", 2);
						if (parts.length == 2) return new TwoOrSplitName(fieldName, parts[0], parts[1]);
						return new TwoOrSplitName(fieldName, parts[0], null);
					}
				}
			}
			
			private interface Action2<ValueType, Name extends SplittedName>
			{
				void parseValues(ValueType value, Name name, JSON_Data.Value<NV,V> jsonValue, String local_debugOutputPrefixStr) throws TraverseException;
			}
			
			<Name extends SplittedName> void parseObject(HashMap<String, ValueType> valueMap, JSON_Object<NV, V> object, String debugOutputPrefixStr, Function<String,Name> splitName, Action2<ValueType, Name> parseValues) throws TraverseException
			{
				SaveGame.parseObject(object, debugOutputPrefixStr, (jsonValue, jsonValueName, local_debugOutputPrefixStr) -> {
					Name name = splitName.apply(jsonValueName);
					String valueID = name.valueID;
					ValueType value = get(valueMap, valueID);
					parseValues.parseValues(value, name, jsonValue, local_debugOutputPrefixStr);
				});
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
				helper.parseObject(addons, object, debugOutputPrefixStr, (addon, value, local) -> {
					addon.owned = JSON_Data.getIntegerValue(value, local);
				});
			}

			private static void parseDamagableAddons(HashMap<String, Addon> addons, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(addons, object, debugOutputPrefixStr, (addon, value, local) -> {
					JSON_Object<NV, V> datObj = JSON_Data.getObjectValue(value, local);
					addon.damagable = new DamagableData(datObj, local);
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
					
					parseArray(itemsDamage, debugOutputPrefixStr+".itemsDamage", (value1, i, prefixStr1) -> {
						JSON_Array<NV, V> array_unparsed = JSON_Data.getArrayValue(value1, prefixStr1);
						Vector<Long> vector_parsed = new Vector<>();
						itemsDamage_parsed.add(vector_parsed);
						
						parseArray(array_unparsed, prefixStr1, (value2, j, prefixStr2) -> {
							long value = JSON_Data.getIntegerValue(value2, prefixStr2);
							vector_parsed.add(value);
						});
					});
					
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
		
		public static class Objective
		{
			private static final SpreadedValuesHelper<Objective> helper = new SpreadedValuesHelper<>(Objective::new);
			
			public final String objectiveId;
			public Long attempts  = null;
			public Long lastTimes = null;
			public Long times     = null;
			public boolean discovered = false;
			public boolean finished   = false;
			public final HashSet<String> savedCargoNeedToBeRemovedOnRestart = new HashSet<>(); // TODO: show Contest.savedCargoNeedToBeRemovedOnRestart
			public boolean viewedUnactivated = false;
			public ObjectiveStates objectiveStates = null; // TODO: show Contest.objectiveStates
			
			private Objective(String contestId)
			{
				this.objectiveId = contestId;
			}

			private static void parseContestAttempts(HashMap<String, Objective> objectives, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(objectives, object, debugOutputPrefixStr, (objective, value, local) -> {
					objective.attempts  = JSON_Data.getIntegerValue(value, local);
				});
			}

			private static void parseContestLastTimes(HashMap<String, Objective> objectives, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(objectives, object, debugOutputPrefixStr, (objective, value, local) -> {
					objective.lastTimes = JSON_Data.getIntegerValue(value, local);
				});
			}

			private static void parseContestTimes(HashMap<String, Objective> objectives, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(objectives, object, debugOutputPrefixStr, (objective, value, local) -> {
					objective.times     = JSON_Data.getIntegerValue(value, local);
				});
			}

			private static void parseDiscoveredObjectives(HashMap<String, Objective> objectives, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseStringArray(objectives, array, debugOutputPrefixStr, objective -> {
					objective.discovered = true;
				});
			}

			private static void parseFinishedObjs(HashMap<String, Objective> objectives, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseStringArray(objectives, array, debugOutputPrefixStr, objective -> {
					objective.finished = true;
				});
			}

			private static void parseHiddenCargoes(HashMap<String, Objective> objectives, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(objectives, object, debugOutputPrefixStr, (objective, value, local) -> {
					JSON_Array<NV, V> array = JSON_Data.getArrayValue(value, local);
					checkEmptyArray(array, local);
				});
			}

			public static void parseObjectiveStates(HashMap<String, Objective> objectives, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(objectives, object, debugOutputPrefixStr, (objective, value, local) -> {
					JSON_Object<NV, V>      object2 = JSON_Data.getObjectValue(value  , local);
					ObjectiveStates objectiveStates = new ObjectiveStates     (object2, local);
					
					if (objective.objectiveStates!=null)
						System.err.printf("[ObjectiveStates] Found more than 1 objectiveStates block: %s%n", local);
					else
						objective.objectiveStates = objectiveStates;
				});
			}

			private static void parseSavedCargoNeedToBeRemovedOnRestart(HashMap<String, Objective> objectives, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(objectives, object, debugOutputPrefixStr, (objective, value, local1_debugOutputPrefixStr) -> {
					JSON_Array<NV, V> array = JSON_Data.getArrayValue(value, local1_debugOutputPrefixStr);
					parseArray(array, local1_debugOutputPrefixStr, (jsonValue, i, local2_debugOutputPrefixStr) -> {
						
						String cargoType = JSON_Data.getStringValue(jsonValue, local2_debugOutputPrefixStr);
						if (objective.savedCargoNeedToBeRemovedOnRestart.contains(cargoType))
							System.err.printf("[SavedCargoNeedToBeRemovedOnRestart] Found redundant cargoTypes in set: %s%n", local2_debugOutputPrefixStr);
						else
							objective.savedCargoNeedToBeRemovedOnRestart.add(cargoType);
					});
				});
			}

			private static void parseViewedUnactivatedObjectives(HashMap<String, Objective> objectives, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseStringArray(objectives, array, debugOutputPrefixStr, contest -> {
					contest.viewedUnactivated = true;
				});
			}
			
			public static class ObjectiveStates
			{
				@SuppressWarnings("unused")
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(ObjectiveStates.class)
				//		.add("name"      , JSON_Data.Value.Type.String)
						;

				private ObjectiveStates(JSON_Object<NV, V> object, String debugOutputPrefixStr)
				{
					//optionalValues.scan(object, "SaveGame.Contest.ObjectiveStates");
					//id          = JSON_Data.getStringValue(object, "name"         , debugOutputPrefixStr);
					// TODO: parse more values in ObjectiveStates
					//KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				}
			}
		}
		
		public static class MapInfos
		{
			private static final SpreadedValuesHelper<MapInfos> helper = new SpreadedValuesHelper<>(MapInfos::new);
			
			public final MapIndex map;
			public boolean wasVisited = false;
			public Garage garage = null;
			public Long garageStatus = null;
			public DiscoveredObjects discoveredTrucks = null;
			public DiscoveredObjects discoveredUpgrades = null;
			public final HashMap<String, CargoLoadingCounts> cargoLoadingCounts = new HashMap<>(); // TODO: show MapInfos.cargoLoadingCounts
			public final HashMap<String, Long>                upgradesGiverData = new HashMap<>(); // TODO: show MapInfos.upgradesGiverData
			public final HashMap<String, Boolean>                   watchPoints = new HashMap<>(); // TODO: show MapInfos.watchPoints
			public final Vector<Waypoint> waypoints         = new Vector<>();  // TODO: show MapInfos.waypoints
			public final Vector<String  > discoveredObjects = new Vector<>();  // TODO: show MapInfos.discoveredObjects
			
			private MapInfos(String mapId)
			{
				this.map = MapIndex.parse(mapId);
			}
			
			private static void parseCargoLoadingCounts(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, SpreadedValuesHelper.SplittedName.TwoOrSplitName::split, (map, name, value, local_debugOutputPrefixStr) -> {
					
					JSON_Object<NV, V> object2 = JSON_Data.getObjectValue(value, local_debugOutputPrefixStr);
					CargoLoadingCounts station = new CargoLoadingCounts(object2, name.secondPart, local_debugOutputPrefixStr);
					
					if (name.secondPart==null)
						System.err.printf("[CargoLoadingCounts] Found a station with no name on 1 map: %s%n", local_debugOutputPrefixStr);
					else if (map.cargoLoadingCounts.containsKey(name.secondPart))
						System.err.printf("[CargoLoadingCounts] Found more than 1 station with same name on 1 map: %s%n", local_debugOutputPrefixStr);
					else
						map.cargoLoadingCounts.put(name.secondPart, station);
				});
			}

			private static void parseDiscoveredObjects(HashMap<String, MapInfos> maps, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				// "level_us_03_01 || US_03_01_CR_WD_01"
				helper.parseStringArray(maps, array, debugOutputPrefixStr, SpreadedValuesHelper.SplittedName.TwoOrSplitName::split, (map, name) -> {
					if (name.secondPart==null)
						System.err.printf("[DiscoveredObjects] Found a discovered object (\"%s\") with no name: %s%n", name.originalStr, debugOutputPrefixStr);
					else
						map.discoveredObjects.add(name.secondPart);
				});
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
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value, local) -> {
					JSON_Object<NV, V> dataObj = JSON_Data.getObjectValue(value, local);
					setValue.accept(map, new DiscoveredObjects(dataObj, local));
				});
			}
			
			private static void parseGaragesData(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value, local_debugOutputPrefixStr) -> {
					
					JSON_Object<NV, V> garageObject = JSON_Data.getObjectValue(value, local_debugOutputPrefixStr);
					Garage             garage       = new Garage(garageObject, map.map.originalMapID(), local_debugOutputPrefixStr);
					
					if (map.garage!=null)
						System.err.printf("[GaragesData] Found more than 1 garage on 1 map: %s%n", local_debugOutputPrefixStr);
					else
						map.garage = garage;
				});
			}

			private static void parseLevelGarageStatuses(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value, local) -> {
					map.garageStatus = JSON_Data.getIntegerValue(value, local);
				});
			}

			private static void parseModTruckOnLevels(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value, local) -> {
					JSON_Array<NV, V> array = JSON_Data.getArrayValue(value, local);
					checkEmptyArray(array, local);
				});
			}

			private static void parseUpgradesGiverData(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value, local_debugOutputPrefixStr) -> {
					
					JSON_Object<NV, V> object2 = JSON_Data.getObjectValue(value, local_debugOutputPrefixStr);
					parseObject(object2, local_debugOutputPrefixStr, (value2, upgrade, local2_debugOutputPrefixStr) -> {
						
						long state = JSON_Data.getIntegerValue(value2, local2_debugOutputPrefixStr);
						if (map.upgradesGiverData.containsKey(upgrade))
							System.err.printf("[UpgradesGiverData] Found more than 1 entry with same upgrade name in map: %s%n", local2_debugOutputPrefixStr);
						else
							map.upgradesGiverData.put(upgrade, state);
					});
				});
			}

			private static void parseVisitedLevels(HashMap<String, MapInfos> maps, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseStringArray(maps, array, debugOutputPrefixStr, map -> {
					map.wasVisited = true;
				});
			}

			private static void parseWatchPoints(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value, local_debugOutputPrefixStr) -> {
					
					JSON_Object<NV, V> object2 = JSON_Data.getObjectValue(value, local_debugOutputPrefixStr);
					parseObject(object2, local_debugOutputPrefixStr, (value2, watchPointName, local2_debugOutputPrefixStr) -> {
						
						boolean state = JSON_Data.getBoolValue(value2, local2_debugOutputPrefixStr);
						if (map.watchPoints.containsKey(watchPointName))
							System.err.printf("[WatchPoints] Found more than 1 entry with same watchPoint name in map: %s%n", local2_debugOutputPrefixStr);
						else
							map.watchPoints.put(watchPointName, state);
					});
				});
			}

			private static void parseWaypoints(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, (map, value1, local1) -> {
					JSON_Array<NV, V> array = JSON_Data.getArrayValue(value1, local1);
					if (!map.waypoints.isEmpty())
						System.err.printf("[Waypoints] Array MapInfos.waypoints is not empty as expected, because more than 1 waypoint list is stored in SaveGame: %s%n", debugOutputPrefixStr);
					else
					{
						parseArray(array, local1, (value2, i, local2) -> {
							JSON_Object<NV, V> object2 = JSON_Data.getObjectValue(value2, local2);
							Waypoint waypoint = new Waypoint(object2, local2);
							map.waypoints.add(waypoint);
						});
					}
				});
			}

			public static class Waypoint
			{
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Waypoint.class)
						.add("modelHeightBounds", JSON_Data.Value.Type.Null   )
						.add("point"            , JSON_Data.Value.Type.Object )
						.add("type"             , JSON_Data.Value.Type.Integer);
				
				public final long type;
				public final Coord3F point;

				private Waypoint(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					//optionalValues.scan(object, "MapInfos.DiscoveredObjects");
					@SuppressWarnings("unused")
					JSON_Data.Null modelHeightBounds;
					JSON_Object<NV, V> point;
					modelHeightBounds = JSON_Data.getNullValue   (object, "modelHeightBounds", debugOutputPrefixStr);
					point             = JSON_Data.getObjectValue (object, "point"            , debugOutputPrefixStr);
					type              = JSON_Data.getIntegerValue(object, "type"             , debugOutputPrefixStr);
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
					
					this.point = new Coord3F(point, debugOutputPrefixStr+".point");
				}
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
			
				@Override public String toString() { return String.format("%d / %d", current, all); }
			}

			public static class CargoLoadingCounts
			{
				public final String stationName;
				public final HashMap<String, Long> counts;
			
				private CargoLoadingCounts(JSON_Object<NV, V> object, String stationName, String debugOutputPrefixStr) throws TraverseException
				{
					this.stationName = stationName;
					this.counts = new HashMap<>();
					
					parseObject(object, debugOutputPrefixStr, (value, cargoType, local) -> {
						long count = JSON_Data.getIntegerValue(value, local);
						if (counts.containsKey(cargoType))
							System.err.printf("[CargoLoadingCounts] Found more than 1 count entry with same cargoType in 1 station: %s%n", local);
						else
							counts.put(cargoType, count);
					});
				}
			}
		}
	}

}
