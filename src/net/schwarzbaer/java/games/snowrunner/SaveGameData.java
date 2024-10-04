package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
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
	
	private static final String packagePrefix = "net.schwarzbaer.java.games.snowrunner.";
	private static final List<String> DEFAULT_FILES = Arrays.asList( "CommonSslSave.cfg", "CompleteSave.cfg", "user_profile.cfg", "user_settings.cfg", "user_social_data.cfg" );
	private static final String SAVEGAME_PREFIX = "CompleteSave";
	private static final String SAVEGAME_SUFFIX = ".cfg";
	
	private static final JSON_Helper.KnownJsonValuesFactory<NV, V> KJV_FACTORY = new JSON_Helper.KnownJsonValuesFactory<>(packagePrefix);
	private static final JSON_Helper.OptionalValues<NV, V> optionalValues = new JSON_Helper.OptionalValues<>();
	
	private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES__EMPTY_OBJECT = KJV_FACTORY.create();
	private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Timestamp     = KJV_FACTORY.create()
			.add("timestamp"      , JSON_Data.Value.Type.String);
	private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_NameValue     = KJV_FACTORY.create()
			.add("name"           , JSON_Data.Value.Type.String );
	
	@SuppressWarnings("unused")
	private static void scanJSON(JSON_Object<NV, V> object, Object source) {
		scanJSON(object, source.getClass());
	}
	private static void scanJSON(JSON_Object<NV, V> object, Class<?> source) {
		String prefixStr = source.getCanonicalName();// getName();
		if (prefixStr.startsWith(packagePrefix))
			prefixStr = prefixStr.substring(packagePrefix.length());
		prefixStr = "["+prefixStr+"]";
		optionalValues.scan(object, prefixStr);
	}
	
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
	
	private static long[] parseArray_Integer(JSON_Array<NV, V> jsonArray, String debugOutputPrefixStr) throws TraverseException
	{
		if (jsonArray==null) return null;
		long[] arr = new long[jsonArray.size()];
		parseArray(jsonArray, debugOutputPrefixStr, (jsonValue, index, local_debugOutputPrefixStr) -> arr[index] = JSON_Data.getIntegerValue(jsonValue, local_debugOutputPrefixStr) );
		return arr;
	}
	
	private static double[] parseArray_Float(JSON_Array<NV, V> jsonArray, String debugOutputPrefixStr) throws TraverseException
	{
		if (jsonArray==null) return null;
		double[] arr = new double[jsonArray.size()];
		parseArray(jsonArray, debugOutputPrefixStr, (jsonValue, index, local_debugOutputPrefixStr) -> arr[index] = JSON_Data.getFloatValue(jsonValue, local_debugOutputPrefixStr) );
		return arr;
	}
	
	private static boolean[] parseArray_Bool(JSON_Array<NV, V> jsonArray, String debugOutputPrefixStr) throws TraverseException
	{
		if (jsonArray==null) return null;
		boolean[] arr = new boolean[jsonArray.size()];
		parseArray(jsonArray, debugOutputPrefixStr, (jsonValue, index, local_debugOutputPrefixStr) -> arr[index] = JSON_Data.getBoolValue(jsonValue, local_debugOutputPrefixStr) );
		return arr;
	}
	
	private interface Constructor<Type>
	{
		Type create(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException;
	}
	
	private static <Type, TargetCollection extends Collection<Type>> TargetCollection parseArray_Object(JSON_Array<NV, V> array, String debugOutputPrefixStr, Constructor<Type> constructor, TargetCollection targetCollection) throws TraverseException
	{
		parseArray(array, debugOutputPrefixStr, (value,i,localPrefixStr) -> {
			JSON_Object<NV, V> obj = JSON_Data.getObjectValue(value, localPrefixStr);
			targetCollection.add(constructor.create(obj, localPrefixStr));
		});
		return targetCollection;
	}
	
	private static <TargetCollection extends Collection<String>> TargetCollection parseArray_String(JSON_Array<NV, V> array, String debugOutputPrefixStr, TargetCollection targetCollection) throws TraverseException
	{
		parseArray(array, debugOutputPrefixStr, (value,i,localPrefixStr) -> {
			String str = JSON_Data.getStringValue(value, localPrefixStr);
			targetCollection.add(str);
		});
		return targetCollection;
	}
	
	private static <Type> Type parseObjectOrNull(JSON_Object<NV, V> object, JSON_Data.Null null_, Constructor<Type> constructor, String debugOutputPrefixStr) throws TraverseException
	{
		checkObjectOrNull(object, null_, debugOutputPrefixStr);
		if (object == null) return null;
		return constructor.create(object, debugOutputPrefixStr);
	}
	
	private static void checkObjectOrNull(JSON_Object<NV, V> object, JSON_Data.Null null_, String debugOutputPrefixStr) throws TraverseException
	{
		if (object==null && null_==null)
			throw new TraverseException("%s isn't a ObjectValue or Null", debugOutputPrefixStr);
	}
	
	private static void checkEmptyArrayOrUnset(JSON_Array<NV, V> array, String debugOutputPrefixStr)
	{
		if (array!=null && !array.isEmpty())
			System.err.printf("Array %s is not empty as expected.%n", debugOutputPrefixStr);
	}
	
	private static void checkEmptyObjectOrUnsetOrNull(JSON_Data.Value<NV, V> objectBaseValue, String debugOutputPrefixStr) throws TraverseException
	{
		if (objectBaseValue==null)
			return; // unset -> OK
		if (objectBaseValue.type!=JSON_Data.Value.Type.Object && objectBaseValue.type!=JSON_Data.Value.Type.Null)
			throw new TraverseException("%s isn't a ObjectValue or Null", debugOutputPrefixStr);
		if (objectBaseValue.type==JSON_Data.Value.Type.Object)
		{
			JSON_Data.ObjectValue<NV, V> objectValue = objectBaseValue.castToObjectValue();
			if (objectValue==null) throw new IllegalStateException();
			if (!objectValue.value.isEmpty())
				System.err.printf("Object %s is not empty as expected.%n", debugOutputPrefixStr);
		}
	}
	
	private static void checkEmptyArrayOrUnsetOrNull(JSON_Data.Value<NV, V> arrayBaseValue, String debugOutputPrefixStr) throws TraverseException
	{
		if (arrayBaseValue==null)
			return; // unset -> OK
		if (arrayBaseValue.type!=JSON_Data.Value.Type.Array && arrayBaseValue.type!=JSON_Data.Value.Type.Null)
			throw new TraverseException("%s isn't a ArrayValue or Null", debugOutputPrefixStr);
		if (arrayBaseValue.type==JSON_Data.Value.Type.Array)
		{
			JSON_Data.ArrayValue<NV, V> arrayValue = arrayBaseValue.castToArrayValue();
			if (arrayValue==null) throw new IllegalStateException();
			if (!arrayValue.value.isEmpty())
				System.err.printf("Array %s is not empty as expected.%n", debugOutputPrefixStr);
		}
	}

	private static void checkEmptyObject(JSON_Object<NV, V> object, String debugOutputPrefixStr)
	{
		KNOWN_JSON_VALUES__EMPTY_OBJECT.scanUnexpectedValues(object, debugOutputPrefixStr);
	}

	public static class SaveGame {
		
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Root = KJV_FACTORY.create("[SaveGame]<root>")
		//		.add("CompleteSave#"  , JSON_Data.Value.Type.Object)
				.add("cfg_version"    , JSON_Data.Value.Type.Integer);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_CompleteSave = KJV_FACTORY.create("[SaveGame]<root>.CompleteSave#")
				.add("SslValue"       , JSON_Data.Value.Type.Object)
				.add("SslType"        , JSON_Data.Value.Type.String);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_watchPointsData = KJV_FACTORY.create()
				.add("data"           , JSON_Data.Value.Type.Object);
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_gameStatByRegion = KJV_FACTORY.create()
				.add("PAYMENTS_RECEIVED", JSON_Data.Value.Type.Object);
		
		private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_SslValue = KJV_FACTORY.create("[SaveGame]<root>.CompleteSave#.SslValue")
				.add("birthVersion"                      , JSON_Data.Value.Type.Integer)
				.add("cargoLoadingCounts"                , JSON_Data.Value.Type.Object )
				.add("discoveredObjectives"              , JSON_Data.Value.Type.Array  )
				.add("discoveredObjects"                 , JSON_Data.Value.Type.Array  )
				.add("finishedObjs"                      , JSON_Data.Value.Type.Array  )
				.add("forcedModelStates"                 , JSON_Data.Value.Type.Object )
				.add("gameDifficultyMode"                , JSON_Data.Value.Type.Integer)
				.add("gameDifficultySettings"            , JSON_Data.Value.Type.Object ) // unparsed
				.add("gameDifficultySettings"            , JSON_Data.Value.Type.Null   ) // unparsed
				.add("gameStat"                          , JSON_Data.Value.Type.Object )
				.add("gameStatByRegion"                  , JSON_Data.Value.Type.Object )
				.add("gameTime"                          , JSON_Data.Value.Type.Float  )
				.add("garagesData"                       , JSON_Data.Value.Type.Object )
				.add("garagesShopData"                   , JSON_Data.Value.Type.Object ) // empty
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
				.add("upgradableGarages"                 , JSON_Data.Value.Type.Object )
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
		public final HashMap<String, RegionInfos> regions;
		public final HashMap<String, MapInfos   > maps;
		public final HashMap<String, Objective  > objectives;
		public final HashMap<String, Addon      > addons;
		public final HashMap<String, TruckInfos > trucks;
		
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
		public final HashMap<String, String> forcedModelStates;
		public final GameStat gameStat;

		private SaveGame(String fileName, String indexStr, JSON_Data.Value<NV, V> data) throws TraverseException {
			if (data==null)
				throw new IllegalArgumentException();
			
			this.fileName = fileName;
			this.indexStr = indexStr;
			this.data = data;
			regions    = new HashMap<>();
			maps       = new HashMap<>();
			objectives = new HashMap<>();
			addons     = new HashMap<>();
			trucks     = new HashMap<>();
			
			JSON_Object<NV, V> rootObject      = JSON_Data.getObjectValue(this.data      ,                          "SaveGame.<root>");
			JSON_Object<NV, V> complSaveObject = JSON_Data.getObjectValue(rootObject     , "CompleteSave"+indexStr, "SaveGame.<root>");
			JSON_Object<NV, V> sslValueObj     = JSON_Data.getObjectValue(complSaveObject, "SslValue"             , "SaveGame.<root>.CompleteSave"+indexStr);
			KNOWN_JSON_VALUES_Root.add("CompleteSave"+indexStr, JSON_Data.Value.Type.Object); // adding known fields dynamically :)
			KNOWN_JSON_VALUES_Root        .scanUnexpectedValues(rootObject     );
			KNOWN_JSON_VALUES_CompleteSave.scanUnexpectedValues(complSaveObject);
			
			String debugOutputPrefixStr = "CompleteSave"+indexStr+".SslValue";
			
			@SuppressWarnings("unused")
			JSON_Object<NV, V> garagesShopData;
			
			JSON_Object<NV, V> cargoLoadingCounts, forcedModelStates, gameStat, gameStatByRegion, garagesData, hiddenCargoes, justDiscoveredObjects, levelGarageStatuses,
				modTruckOnLevels, modTruckRefundValues, modTruckTypesRefundValues, objectiveStates, persistentProfileData,
				savedCargoNeedToBeRemovedOnRestart, tutorialStates, upgradableGarages, upgradesGiverData, watchPointsData, waypoints;
			
			JSON_Array<NV, V> visitedLevels, discoveredObjectives, discoveredObjects, finishedObjs, givenTrialRewards, viewedUnactivatedObjectives;
			
			JSON_Data.Null levelGarageStatuses_Null, persistentProfileData_Null, tutorialStates_Null, watchPointsData_Null;
			JSON_Data.Value<NV,V> garagesShopData_Value;
			
			birthVersion                       = JSON_Data.getIntegerValue  (sslValueObj, "birthVersion"                      , debugOutputPrefixStr);
			cargoLoadingCounts                 = JSON_Data.getObjectValue   (sslValueObj, "cargoLoadingCounts"                , debugOutputPrefixStr);
			discoveredObjectives               = JSON_Data.getArrayValue    (sslValueObj, "discoveredObjectives"              , debugOutputPrefixStr);
			discoveredObjects                  = JSON_Data.getArrayValue    (sslValueObj, "discoveredObjects"                 , debugOutputPrefixStr);
			finishedObjs                       = JSON_Data.getArrayValue    (sslValueObj, "finishedObjs"                      , true, false, debugOutputPrefixStr);
			forcedModelStates                  = JSON_Data.getObjectValue   (sslValueObj, "forcedModelStates"                 , debugOutputPrefixStr);
			gameDifficultyMode                 = JSON_Data.getIntegerValue  (sslValueObj, "gameDifficultyMode"                , true, false, debugOutputPrefixStr);
			gameStat                           = JSON_Data.getObjectValue   (sslValueObj, "gameStat"                          , debugOutputPrefixStr);
			gameStatByRegion                   = JSON_Data.getObjectValue   (sslValueObj, "gameStatByRegion"                  , debugOutputPrefixStr);
			gameTime                           = JSON_Data.getFloatValue    (sslValueObj, "gameTime"                          , debugOutputPrefixStr);
			garagesData                        = JSON_Data.getObjectValue   (sslValueObj, "garagesData"                       , debugOutputPrefixStr);
			garagesShopData                    = JSON_Data.getObjectValue   (sslValueObj, "garagesShopData"                   , true, true, debugOutputPrefixStr);
			garagesShopData_Value              = JSON_Data.getValue         (sslValueObj, "garagesShopData"                   , true, debugOutputPrefixStr);
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
			upgradableGarages                  = JSON_Data.getObjectValue   (sslValueObj, "upgradableGarages"                 , true, false, debugOutputPrefixStr);
			upgradesGiverData                  = JSON_Data.getObjectValue   (sslValueObj, "upgradesGiverData"                 , debugOutputPrefixStr);
			viewedUnactivatedObjectives        = JSON_Data.getArrayValue    (sslValueObj, "viewedUnactivatedObjectives"       , debugOutputPrefixStr);
			visitedLevels                      = JSON_Data.getArrayValue    (sslValueObj, "visitedLevels"                     , debugOutputPrefixStr);
			watchPointsData                    = JSON_Data.getObjectValue   (sslValueObj, "watchPointsData"                   , false, true, debugOutputPrefixStr);
			watchPointsData_Null               = JSON_Data.getNullValue     (sslValueObj, "watchPointsData"                   , false, true, debugOutputPrefixStr);
			waypoints                          = JSON_Data.getObjectValue   (sslValueObj, "waypoints"                         , debugOutputPrefixStr);
			worldConfiguration                 = JSON_Data.getStringValue   (sslValueObj, "worldConfiguration"                , debugOutputPrefixStr);
			/*
				unparsed:
					gameDifficultySettings
					
				unparsed, but interesting:
					
				empty:
					garagesShopData, givenTrialRewards, justDiscoveredObjects, modTruckRefundValues, modTruckTypesRefundValues
			 */
			KNOWN_JSON_VALUES_SslValue.scanUnexpectedValues(sslValueObj);
			
			checkObjectOrNull(levelGarageStatuses  , levelGarageStatuses_Null  , debugOutputPrefixStr+".levelGarageStatuses"  );
			checkObjectOrNull(tutorialStates       , tutorialStates_Null       , debugOutputPrefixStr+".tutorialStates"       );
			checkObjectOrNull(watchPointsData      , watchPointsData_Null      , debugOutputPrefixStr+".watchPointsData"      );
			checkEmptyArrayOrUnset(givenTrialRewards        , debugOutputPrefixStr+".givenTrialRewards"        );
			checkEmptyObject      (justDiscoveredObjects    , debugOutputPrefixStr+".justDiscoveredObjects"    );
			checkEmptyObject      (modTruckRefundValues     , debugOutputPrefixStr+".modTruckRefundValues"     );
			checkEmptyObject      (modTruckTypesRefundValues, debugOutputPrefixStr+".modTruckTypesRefundValues");
			checkEmptyObjectOrUnsetOrNull(garagesShopData_Value, debugOutputPrefixStr+".garagesShopData");
			
			ppd = parseObjectOrNull(persistentProfileData, persistentProfileData_Null, PersistentProfileData::new, debugOutputPrefixStr+".persistentProfileData");
			this.gameStat = new GameStat(gameStat, debugOutputPrefixStr+".gameStat");
			
			this.forcedModelStates = new HashMap<>();
			parseObject(forcedModelStates, debugOutputPrefixStr+".forcedModelStates", (value, modelName, localPrefixStr) -> {
				String state = JSON_Data.getStringValue(value, localPrefixStr);
				
				if (this.forcedModelStates.containsKey(modelName))
					System.err.printf("[ForcedModelStates] Found more than 1 entry with same Model name \"%s\" in %s%n", modelName, localPrefixStr);
				else
					this.forcedModelStates.put(modelName, state);
			});
			
			this.tutorialStates = new HashMap<>();
			parseObject(tutorialStates, debugOutputPrefixStr+".tutorialStates", (value, tutorialStep, localPrefixStr) -> {
				boolean state = JSON_Data.getBoolValue(value, localPrefixStr);
				
				if (this.tutorialStates.containsKey(tutorialStep))
					System.err.printf("[TutorialStates] Found more than 1 entry with same TutorialStep name \"%s\" in %s%n", tutorialStep, localPrefixStr);
				else
					this.tutorialStates.put(tutorialStep, state);
			});
			
			JSON_Object<NV, V> watchPointsData_data = null;
			if (watchPointsData!=null)
			{
				watchPointsData_data = JSON_Data.getObjectValue(watchPointsData, "data", debugOutputPrefixStr+".watchPointsData");
				KNOWN_JSON_VALUES_watchPointsData.scanUnexpectedValues(watchPointsData, debugOutputPrefixStr+".watchPointsData");
			}
			
			parseObject(gameStatByRegion, debugOutputPrefixStr+".gameStatByRegion", (value, name, localPrefixStr) -> {
				JSON_Object<NV, V> object;
				switch (name)
				{
					case "PAYMENTS_RECEIVED":
						object = JSON_Data.getObjectValue(value, localPrefixStr);
						RegionInfos.parsePaymentsReceived(regions, object, localPrefixStr);
						break;
				}
			});
			KNOWN_JSON_VALUES_gameStatByRegion.scanUnexpectedValues(gameStatByRegion, debugOutputPrefixStr+".gameStatByRegion");
			
			MapInfos .parseCargoLoadingCounts                (maps      , cargoLoadingCounts                , debugOutputPrefixStr+".cargoLoadingCounts"                );
			MapInfos .parseDiscoveredObjects                 (maps      , discoveredObjects                 , debugOutputPrefixStr+".discoveredObjects"                 );
			MapInfos .parseGaragesData                       (maps      , garagesData                       , debugOutputPrefixStr+".garagesData"                       );
			MapInfos .parseLevelGarageStatuses               (maps      , levelGarageStatuses               , debugOutputPrefixStr+".levelGarageStatuses"               );
			MapInfos .parseModTruckOnLevels                  (maps      , modTruckOnLevels                  , debugOutputPrefixStr+".modTruckOnLevels"                  );
			MapInfos .parseUpgradableGarages                 (maps      , upgradableGarages                 , debugOutputPrefixStr+".upgradableGarages"                 );
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

		public String getGameTimeStr()
		{
			int h = (int) Math.floor(  gameTime            );
			int m = (int) Math.floor( (gameTime-h)*60      );
			int s = (int) Math.floor(((gameTime-h)*60-m)*60);
			return String.format("%d:%02d:%02d", h,m,s);
		}

		public long getOwnedTruckCount(Truck truck) {
			if (truck==null) return 0;
			return getOwnedTruckCount(truck.id);
		}

		public long getOwnedTruckCount(String truckId)
		{
			TruckInfos truckInfos = trucks.get(truckId);
			return truckInfos==null || truckInfos.owned==null ? 0 : truckInfos.owned.longValue();
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
		
		public static Boolean isUnlockedItem(SaveGame saveGame, String id)
		{
			return saveGame==null ? null : saveGame.isUnlockedItem(id);
		}

		public Boolean isUnlockedItem(String id)
		{
			if (id==null) return null;
			if (ppd==null) return null;
			return ppd.unlockedItemNames.get(id);
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

			public boolean isZero() { return x==0.0 && y==0.0 && z==0.0; }
		}

		public class GameStat
		{
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(GameStat.class)
					.add("ADDON_BOUGHT"                 , JSON_Data.Value.Type.Integer)
					.add("ADDON_SOLD"                   , JSON_Data.Value.Type.Integer)
					.add("MONEY_EARNED"                 , JSON_Data.Value.Type.Integer)
					.add("MONEY_SPENT"                  , JSON_Data.Value.Type.Integer)
					.add("MULTIPLAYER_MISSIONS_FINISHED", JSON_Data.Value.Type.Integer)
					.add("MULTIPLAYER_MONEY_EARNED"     , JSON_Data.Value.Type.Integer)
					.add("MULTIPLAYER_SESSIONS_PLAYED"  , JSON_Data.Value.Type.Integer)
					.add("SESSION_NUMBER"               , JSON_Data.Value.Type.Integer)
					.add("TRAILER_BOUGHT"               , JSON_Data.Value.Type.Integer)
					.add("TRAILER_SOLD"                 , JSON_Data.Value.Type.Integer)
					.add("TRUCK_BOUGHT"                 , JSON_Data.Value.Type.Integer)
					.add("TRUCK_SOLD"                   , JSON_Data.Value.Type.Integer)
					;
			/*
			    Block "[SaveGameData.SaveGame.GameStat]" [12]
			        ADDON_BOUGHT                 :[Integer, <unset>]
			        ADDON_SOLD                   :[Integer, <unset>]
			        MONEY_EARNED                 :[Integer, <unset>]
			        MONEY_SPENT                  :[Integer, <unset>]
			        MULTIPLAYER_MISSIONS_FINISHED:[Integer, <unset>]
			        MULTIPLAYER_MONEY_EARNED     :[Integer, <unset>]
			        MULTIPLAYER_SESSIONS_PLAYED  :[Integer, <unset>]
			        SESSION_NUMBER               :[Integer, <unset>]
			        TRAILER_BOUGHT               :[Integer, <unset>]
			        TRAILER_SOLD                 :[Integer, <unset>]
			        TRUCK_BOUGHT                 :[Integer, <unset>]
			        TRUCK_SOLD                   :[Integer, <unset>]
			 */
			public final Long addonsBought;
			public final Long addonsSold;
			public final Long moneyEarned;
			public final Long moneySpent;
			public final Long multiplayerMissionsFinished;
			public final Long multiplayerMoneyEarned;
			public final Long multiplayerSessionsPlayed;
			public final Long sessionNumber;
			public final Long trailersBought;
			public final Long trailersSold;
			public final Long trucksBought;
			public final Long trucksSold;
			
			private GameStat(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				//scanJSON(object, this);
				
				addonsBought                = JSON_Data.getIntegerValue(object, "ADDON_BOUGHT"                 , true, false, debugOutputPrefixStr);
				addonsSold                  = JSON_Data.getIntegerValue(object, "ADDON_SOLD"                   , true, false, debugOutputPrefixStr);
				moneyEarned                 = JSON_Data.getIntegerValue(object, "MONEY_EARNED"                 , true, false, debugOutputPrefixStr);
				moneySpent                  = JSON_Data.getIntegerValue(object, "MONEY_SPENT"                  , true, false, debugOutputPrefixStr);
				multiplayerMissionsFinished = JSON_Data.getIntegerValue(object, "MULTIPLAYER_MISSIONS_FINISHED", true, false, debugOutputPrefixStr);
				multiplayerMoneyEarned      = JSON_Data.getIntegerValue(object, "MULTIPLAYER_MONEY_EARNED"     , true, false, debugOutputPrefixStr);
				multiplayerSessionsPlayed   = JSON_Data.getIntegerValue(object, "MULTIPLAYER_SESSIONS_PLAYED"  , true, false, debugOutputPrefixStr);
				sessionNumber               = JSON_Data.getIntegerValue(object, "SESSION_NUMBER"               , true, false, debugOutputPrefixStr);
				trailersBought              = JSON_Data.getIntegerValue(object, "TRAILER_BOUGHT"               , true, false, debugOutputPrefixStr);
				trailersSold                = JSON_Data.getIntegerValue(object, "TRAILER_SOLD"                 , true, false, debugOutputPrefixStr);
				trucksBought                = JSON_Data.getIntegerValue(object, "TRUCK_BOUGHT"                 , true, false, debugOutputPrefixStr);
				trucksSold                  = JSON_Data.getIntegerValue(object, "TRUCK_SOLD"                   , true, false, debugOutputPrefixStr);
				
				KNOWN_JSON_VALUES.scanUnexpectedValues(object);
			}
		}

		public class PersistentProfileData
		{
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(PersistentProfileData.class)
					.add("experience"              , JSON_Data.Value.Type.Integer)
					.add("money"                   , JSON_Data.Value.Type.Integer)
					.add("rank"                    , JSON_Data.Value.Type.Integer)
					.add("ownedTrucks"             , JSON_Data.Value.Type.Object )
					.add("trucksInWarehouse"       , JSON_Data.Value.Type.Array  )
					.add("contestAttempts"         , JSON_Data.Value.Type.Object )
					.add("contestLastTimes"        , JSON_Data.Value.Type.Object )
					.add("contestTimes"            , JSON_Data.Value.Type.Object )
					.add("addons"                  , JSON_Data.Value.Type.Object )
					.add("customizationRefundMoney", JSON_Data.Value.Type.Integer)
					.add("damagableAddons"         , JSON_Data.Value.Type.Object )
					.add("discoveredTrucks"        , JSON_Data.Value.Type.Object )
					.add("discoveredUpgrades"      , JSON_Data.Value.Type.Object )
					.add("distance"                , JSON_Data.Value.Type.Object )
					.add("dlcNotes"                , JSON_Data.Value.Type.Array  )
					.add("isNewProfile"            , JSON_Data.Value.Type.Bool   )
					.add("knownRegions"            , JSON_Data.Value.Type.Array  )
					.add("newTrucks"               , JSON_Data.Value.Type.Array  )
					.add("refundGarageTruckDescs"  , JSON_Data.Value.Type.Array  ) // empty array
					.add("refundMoney"             , JSON_Data.Value.Type.Integer)
					.add("refundTruckDescs"        , JSON_Data.Value.Type.Object ) // empty object
					.add("unlockedItemNames"       , JSON_Data.Value.Type.Object )
					.add("userId"                  , JSON_Data.Value.Type.Object ) // empty object
					;
			
			public final long experience;
			public final long rank;
			public final long money;
			public final Long customizationRefundMoney;
			public final long refundMoney;
			public final Boolean isNewProfile;
			public final Vector<TruckDesc> trucksInWarehouse;
			public final Vector<String> dlcNotes;
			public final HashMap<String, Boolean> unlockedItemNames;
			
			private PersistentProfileData(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				JSON_Object<NV, V> ownedTrucks, contestAttempts, contestLastTimes, contestTimes, addons, damagableAddons,
					discoveredTrucks, discoveredUpgrades, distance, refundTruckDescs, unlockedItemNames, userId;
				JSON_Array<NV, V> dlcNotes, knownRegions, newTrucks, refundGarageTruckDescs, trucksInWarehouse;
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
				distance                  = JSON_Data.getObjectValue (object, "distance"                , debugOutputPrefixStr);
				dlcNotes                  = JSON_Data.getArrayValue  (object, "dlcNotes"                , debugOutputPrefixStr);
				isNewProfile              = JSON_Data.getBoolValue   (object, "isNewProfile"            , true, false, debugOutputPrefixStr);
				knownRegions              = JSON_Data.getArrayValue  (object, "knownRegions"            , debugOutputPrefixStr);
				newTrucks                 = JSON_Data.getArrayValue  (object, "newTrucks"            , debugOutputPrefixStr);
				refundGarageTruckDescs    = JSON_Data.getArrayValue  (object, "refundGarageTruckDescs"  , debugOutputPrefixStr);
				refundMoney               = JSON_Data.getIntegerValue(object, "refundMoney"             , debugOutputPrefixStr);
				refundTruckDescs          = JSON_Data.getObjectValue (object, "refundTruckDescs"        , debugOutputPrefixStr);
				unlockedItemNames         = JSON_Data.getObjectValue (object, "unlockedItemNames"       , debugOutputPrefixStr);
				userId                    = JSON_Data.getObjectValue (object, "userId"                  , debugOutputPrefixStr);
				/*
					unparsed:
						
					unparsed, but interesting:
						
					empty:
						refundGarageTruckDescs, refundTruckDescs, userId
				 */
				
				KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				
				checkEmptyObject      (refundTruckDescs      , debugOutputPrefixStr+".refundTruckDescs"      );
				checkEmptyObject      (userId                , debugOutputPrefixStr+".userId"                );
				checkEmptyArrayOrUnset(refundGarageTruckDescs, debugOutputPrefixStr+".refundGarageTruckDescs");
				
				this.dlcNotes          = parseArray_String(dlcNotes         , debugOutputPrefixStr+".dlcNotes"         , new Vector<>());
				this.trucksInWarehouse = parseArray_Object(trucksInWarehouse, debugOutputPrefixStr+".trucksInWarehouse", TruckDesc::new, new Vector<>());
				
				this.unlockedItemNames = new HashMap<>();
				parseObject(unlockedItemNames, debugOutputPrefixStr+".unlockedItemNames", (value, name, localPrefixStr) -> {
					boolean boolValue = JSON_Data.getBoolValue(value, localPrefixStr);
					this.unlockedItemNames.put(name, boolValue);
				});
				
				TruckInfos .parseOwnedTrucks       (SaveGame.this.trucks    , ownedTrucks       , debugOutputPrefixStr+".ownedTrucks"       );
				TruckInfos .parseNewTrucks         (SaveGame.this.trucks    , newTrucks         , debugOutputPrefixStr+".newTrucks"         );
				RegionInfos.parseDistances         (SaveGame.this.regions   , distance          , debugOutputPrefixStr+".distance"          );
				RegionInfos.parseKnownRegions      (SaveGame.this.regions   , knownRegions      , debugOutputPrefixStr+".knownRegions"      );
				MapInfos   .parseDiscoveredTrucks  (SaveGame.this.maps      , discoveredTrucks  , debugOutputPrefixStr+".discoveredTrucks"  );
				MapInfos   .parseDiscoveredUpgrades(SaveGame.this.maps      , discoveredUpgrades, debugOutputPrefixStr+".discoveredUpgrades");
				Objective  .parseContestAttempts   (SaveGame.this.objectives, contestAttempts   , debugOutputPrefixStr+".contestAttempts"   );
				Objective  .parseContestLastTimes  (SaveGame.this.objectives, contestLastTimes  , debugOutputPrefixStr+".contestLastTimes"  );
				Objective  .parseContestTimes      (SaveGame.this.objectives, contestTimes      , debugOutputPrefixStr+".contestTimes"      );
				Addon      .parseOwned             (SaveGame.this.addons    , addons            , debugOutputPrefixStr+".addons"            );
				Addon      .parseDamagableAddons   (SaveGame.this.addons    , damagableAddons   , debugOutputPrefixStr+".damagableAddons"   );
				
			}
		}
		
		public static class Garage
		{
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_Garage = KJV_FACTORY.create(Garage.class)
					.add("selectedSlot"  , JSON_Data.Value.Type.String)
					.add("slotsDatas"    , JSON_Data.Value.Type.Object);
			
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_slotsDatas = KJV_FACTORY.create(Garage.class, " -> slotsDatas")
					.add("garage_interior_slot_1", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_2", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_3", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_4", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_5", JSON_Data.Value.Type.Object)
					.add("garage_interior_slot_6", JSON_Data.Value.Type.Object);
			
			private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES_garageSlot = KJV_FACTORY.create(Garage.class, " -> slotsDatas.garageSlot")
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
			public final Vector<InstalledAddon> addons;
			public final HashSet<String> addonIDs;
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
			public final Double  water;
			public final long    wheelRepairs;
			public final double  wheelsScale;
			public final String  wheels;
			public final String  winch;
			public final CustomizationPreset customizationPreset;
			public final double[] damageDecals;
			public final long[]   wheelsDamage;
			public final double[] wheelsSuspHeight;

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
					.add("water"                     , JSON_Data.Value.Type.Float  )
					.add("wheelRepairs"              , JSON_Data.Value.Type.Integer)
					.add("wheelsDamage"              , JSON_Data.Value.Type.Array  )
					.add("wheelsScale"               , JSON_Data.Value.Type.Float  )
					.add("wheelsSuspHeight"          , JSON_Data.Value.Type.Array  )
					.add("wheelsType"                , JSON_Data.Value.Type.String )
					.add("winchUpgrade"              , JSON_Data.Value.Type.Object )
					;
		
			private TruckDesc(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				// scanJSON(object, this);
				
				JSON_Object<NV, V> customizationPreset;
				JSON_Array<NV, V> addons, constraints, controlConstrPosition, damageDecals, isPoweredEngaged, tmBodies, wheelsDamage, wheelsSuspHeight;
				
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
				customizationPreset        = JSON_Data.getObjectValue (object, "customizationPreset"       , debugOutputPrefixStr);
				damage                     = JSON_Data.getIntegerValue(object, "damage"                    , debugOutputPrefixStr);
				damageDecals               = JSON_Data.getArrayValue  (object, "damageDecals"              , debugOutputPrefixStr);
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
				water                      = JSON_Data.getFloatValue  (object, "water"                     , true, false, debugOutputPrefixStr);
				wheelRepairs               = JSON_Data.getIntegerValue(object, "wheelRepairs"              , debugOutputPrefixStr);
				wheelsDamage               = JSON_Data.getArrayValue  (object, "wheelsDamage"              , debugOutputPrefixStr);
				wheelsScale                = JSON_Data.getFloatValue  (object, "wheelsScale"               , debugOutputPrefixStr);
				wheelsSuspHeight           = JSON_Data.getArrayValue  (object, "wheelsSuspHeight"          , debugOutputPrefixStr);
				wheels                     = JSON_Data.getStringValue (object, "wheelsType"                , debugOutputPrefixStr);
				winch                      = parseNameValue           (object, "winchUpgrade"              , debugOutputPrefixStr);
				/*
					unparsed:
						
					unparsed, but interesting:
						
					empty:
						constraints, controlConstrPosition, isPoweredEngaged, tmBodies
				 */

				KNOWN_JSON_VALUES.scanUnexpectedValues(object);
				checkEmptyArrayOrUnset(constraints          , debugOutputPrefixStr+".constraints"          );
				checkEmptyArrayOrUnset(controlConstrPosition, debugOutputPrefixStr+".controlConstrPosition");
				checkEmptyArrayOrUnset(isPoweredEngaged     , debugOutputPrefixStr+".isPoweredEngaged"     );
				checkEmptyArrayOrUnset(tmBodies             , debugOutputPrefixStr+".tmBodies"             );
				
				this.addons              = parseArray_Object      (addons             , debugOutputPrefixStr+".addons"             , InstalledAddon::new, new Vector<>());
				this.customizationPreset = new CustomizationPreset(customizationPreset, debugOutputPrefixStr+".customizationPreset");
				this.damageDecals        = parseArray_Float       (damageDecals       , debugOutputPrefixStr+".damageDecals"       );
				this.retainedMap         = MapIndex.parse(retainedMapId);
				this.wheelsDamage        = parseArray_Integer     (wheelsDamage       , debugOutputPrefixStr+".wheelsDamage"       );
				this.wheelsSuspHeight    = parseArray_Float       (wheelsSuspHeight   , debugOutputPrefixStr+".wheelsSuspHeight"   );
				
				this.addonIDs = new HashSet<>();
				for (InstalledAddon addon : this.addons)
					addonIDs.add(addon.name);
			}
			
			public static class CustomizationPreset
			{
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(CustomizationPreset.class)
						.add("gameDataXmlNode"     , JSON_Data.Value.Type.Null   )
						.add("id"                  , JSON_Data.Value.Type.Integer)
						.add("isSpecialSkin"       , JSON_Data.Value.Type.Bool   )
						.add("overrideMaterialName", JSON_Data.Value.Type.String )
						.add("tintsColors"         , JSON_Data.Value.Type.Array  )
						.add("uiName"              , JSON_Data.Value.Type.String )
						;
				public final long id;
				public final boolean isSpecialSkin;
				public final String overrideMaterialName;
				public final String uiName;
				public final Vector<Tint> colors;
				/*
				    Block "[SaveGameData.SaveGame.TruckDesc.CustomizationPreset]" [6]
				        gameDataXmlNode      : Null
				        id                   : Integer
				        isSpecialSkin        : Bool
				        overrideMaterialName : String
				        tintsColors          : Array
				        uiName               : String
				        tintsColors[]:Object -> Tint
				        
				 */
				public CustomizationPreset(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					//scanJSON(object, this);
					
					@SuppressWarnings("unused")
					JSON_Data.Null gameDataXmlNode_Null;
					JSON_Array<NV, V> tintsColors;
					gameDataXmlNode_Null = JSON_Data.getNullValue   (object, "gameDataXmlNode"     , debugOutputPrefixStr);
					id                   = JSON_Data.getIntegerValue(object, "id"                  , debugOutputPrefixStr);
					isSpecialSkin        = JSON_Data.getBoolValue   (object, "isSpecialSkin"       , debugOutputPrefixStr);
					overrideMaterialName = JSON_Data.getStringValue (object, "overrideMaterialName", debugOutputPrefixStr);
					tintsColors          = JSON_Data.getArrayValue  (object, "tintsColors"         , debugOutputPrefixStr);
					uiName               = JSON_Data.getStringValue (object, "uiName"              , debugOutputPrefixStr);
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
					
					colors = parseArray_Object(tintsColors, debugOutputPrefixStr, Tint::new, new Vector<>());
				}
				
				public Color[] toColorArray()
				{
					if (colors==null || id<0) return null;
					
					Color[] colorArr = new Color[colors.size()];
					for (int i=0; i<colors.size(); i++)
					{
						TruckDesc.CustomizationPreset.Tint t = colors.get(i);
						colorArr[i] = new Color(
								(int) Math.round(t.r),
								(int) Math.round(t.g),
								(int) Math.round(t.b)
						);
					}
					
					return colorArr;
				}

				public static class Tint
				{
					private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Tint.class)
							.add("a", JSON_Data.Value.Type.Float)
							.add("r", JSON_Data.Value.Type.Float)
							.add("g", JSON_Data.Value.Type.Float)
							.add("b", JSON_Data.Value.Type.Float);
					/*
					Block "[SaveGameData.SaveGame.TruckDesc.CustomizationPreset].tintsColors[]" [4]
					    a:Float
					    b:Float
					    g:Float
					    r:Float
					 */
					public final double a;
					public final double r;
					public final double g;
					public final double b;
					
					public Tint(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
					{
						//scanJSON(object, this);
						a = JSON_Data.getFloatValue(object, "a", debugOutputPrefixStr);
						r = JSON_Data.getFloatValue(object, "r", debugOutputPrefixStr);
						g = JSON_Data.getFloatValue(object, "g", debugOutputPrefixStr);
						b = JSON_Data.getFloatValue(object, "b", debugOutputPrefixStr);
						KNOWN_JSON_VALUES.scanUnexpectedValues(object);
					}
				}
			}
			
			public static class InstalledAddon
			{
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(InstalledAddon.class)
						.add("addonCRC"             , JSON_Data.Value.Type.Integer)
						.add("constraints"          , JSON_Data.Value.Type.Null   )
						.add("constraints"          , JSON_Data.Value.Type.Array  )
						.add("controlConstrPosition", JSON_Data.Value.Type.Null   )
						.add("controlConstrPosition", JSON_Data.Value.Type.Array  )
						.add("eulerAngles"          , JSON_Data.Value.Type.Object )
						.add("extraParents"         , JSON_Data.Value.Type.Array  )
						.add("firstSlot"            , JSON_Data.Value.Type.Integer)
						.add("fuel"                 , JSON_Data.Value.Type.Float  )
						.add("isInCockpit"          , JSON_Data.Value.Type.Bool   )
						.add("isPoweredEngaged"     , JSON_Data.Value.Type.Null   )
						.add("isPoweredEngaged"     , JSON_Data.Value.Type.Array  )
						.add("name"                 , JSON_Data.Value.Type.String )
						.add("overrideMaterial"     , JSON_Data.Value.Type.String )
						.add("parentAddonType"      , JSON_Data.Value.Type.String )
						.add("parentFrame"          , JSON_Data.Value.Type.String )
						.add("position"             , JSON_Data.Value.Type.Object )
						.add("repairs"              , JSON_Data.Value.Type.Integer)
						.add("tmBodies"             , JSON_Data.Value.Type.Null   )
						.add("tmBodies"             , JSON_Data.Value.Type.Array  )
						.add("water"                , JSON_Data.Value.Type.Float  )
						.add("wheelRepairs"         , JSON_Data.Value.Type.Integer)
						;
				/*
					Block "[SaveGameData.SaveGame.TruckDesc.InstalledAddon]" [18]
					    addonCRC             :[Integer, <unset>]
					    constraints          :[Array, Null, <unset>]
					    controlConstrPosition:[Array, Null, <unset>]
					    eulerAngles     : Object
					    extraParents    : Array
					    firstSlot       : Integer
					    fuel            : Float
					    isInCockpit     : Bool
					    isPoweredEngaged:[Array , Null, <unset>]
					    name            : String
					    overrideMaterial: String
					    parentAddonType : String
					    parentFrame     : String
					    position        : Object
					    repairs         : Integer
					    tmBodies        :[Array  , Null, <unset>]
					    water           :[Float  , <unset>]
					    wheelRepairs    : Integer
					    
					    constraints[]:empty array
					    controlConstrPosition[]:empty array
					    extraParents[]:Object or empty array
					    isPoweredEngaged[]:empty array
					    tmBodies[]:empty array
					    
					Block "[SaveGameData.SaveGame.TruckDesc.InstalledAddon].eulerAngles" [3]
					    x:Float
					    y:Float
					    z:Float
					Block "[SaveGameData.SaveGame.TruckDesc.InstalledAddon].position" [3]
					    x:Float
					    y:Float
					    z:Float
				 */
				
				public final Vector<ExtraParent> extraParents;
				public final Long    addonCRC;
				public final Coord3F eulerAngles;
				public final long    firstSlot;
				public final double  fuel;
				public final boolean isInCockpit;
				public final String  name;
				public final String  overrideMaterial;
				public final String  parentAddonType;
				public final String  parentFrame;
				public final Coord3F position;
				public final long    repairs;
				public final Double  water;
				public final long    wheelRepairs;

				public InstalledAddon(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					//scanJSON(object, this);
					
					JSON_Data.Value<NV,V> constraints_Value, controlConstrPosition_Value, isPoweredEngaged_Value, tmBodies_Value;
					JSON_Object<NV,V> eulerAngles, position;
					@SuppressWarnings("unused")
					JSON_Array<NV,V> constraints, controlConstrPosition, extraParents, isPoweredEngaged, tmBodies;
					
					addonCRC                    = JSON_Data.getIntegerValue(object, "addonCRC"             , true, false, debugOutputPrefixStr);
					constraints                 = JSON_Data.getArrayValue  (object, "constraints"          , true,  true, debugOutputPrefixStr);
					constraints_Value           = JSON_Data.getValue       (object, "constraints"          , true,  debugOutputPrefixStr);
					controlConstrPosition       = JSON_Data.getArrayValue  (object, "controlConstrPosition", true,  true, debugOutputPrefixStr);
					controlConstrPosition_Value = JSON_Data.getValue       (object, "controlConstrPosition", true,  debugOutputPrefixStr);
					eulerAngles                 = JSON_Data.getObjectValue (object, "eulerAngles"          , debugOutputPrefixStr);
					extraParents                = JSON_Data.getArrayValue  (object, "extraParents"         , debugOutputPrefixStr);
					firstSlot                   = JSON_Data.getIntegerValue(object, "firstSlot"            , debugOutputPrefixStr);
					fuel                        = JSON_Data.getFloatValue  (object, "fuel"                 , debugOutputPrefixStr);
					isInCockpit                 = JSON_Data.getBoolValue   (object, "isInCockpit"          , debugOutputPrefixStr);
					isPoweredEngaged            = JSON_Data.getArrayValue  (object, "isPoweredEngaged"     , true,  true, debugOutputPrefixStr);
					isPoweredEngaged_Value      = JSON_Data.getValue       (object, "isPoweredEngaged"     , true,  debugOutputPrefixStr);
					name                        = JSON_Data.getStringValue (object, "name"                 , debugOutputPrefixStr);
					overrideMaterial            = JSON_Data.getStringValue (object, "overrideMaterial"     , debugOutputPrefixStr);
					parentAddonType             = JSON_Data.getStringValue (object, "parentAddonType"      , debugOutputPrefixStr);
					parentFrame                 = JSON_Data.getStringValue (object, "parentFrame"          , debugOutputPrefixStr);
					position                    = JSON_Data.getObjectValue (object, "position"             , debugOutputPrefixStr);
					repairs                     = JSON_Data.getIntegerValue(object, "repairs"              , debugOutputPrefixStr);
					tmBodies                    = JSON_Data.getArrayValue  (object, "tmBodies"             , true,  true, debugOutputPrefixStr);
					tmBodies_Value              = JSON_Data.getValue       (object, "tmBodies"             , true,  debugOutputPrefixStr);
					water                       = JSON_Data.getFloatValue  (object, "water"                , true, false, debugOutputPrefixStr);
					wheelRepairs                = JSON_Data.getIntegerValue(object, "wheelRepairs"         , debugOutputPrefixStr);
					
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
					
					checkEmptyArrayOrUnsetOrNull(constraints_Value          , debugOutputPrefixStr+".constraints"          );
					checkEmptyArrayOrUnsetOrNull(controlConstrPosition_Value, debugOutputPrefixStr+".controlConstrPosition");
					checkEmptyArrayOrUnsetOrNull(isPoweredEngaged_Value     , debugOutputPrefixStr+".isPoweredEngaged"     );
					checkEmptyArrayOrUnsetOrNull(tmBodies_Value             , debugOutputPrefixStr+".tmBodies"             );
					this.position     = new Coord3F(position   , debugOutputPrefixStr+".position"   );
					this.eulerAngles  = new Coord3F(eulerAngles, debugOutputPrefixStr+".eulerAngles");
					this.extraParents = parseArray_Object(extraParents, debugOutputPrefixStr+".extraParents", ExtraParent::new, new Vector<>());
				}
				
				public static class ExtraParent
				{
					private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(ExtraParent.class)
							.add("eulerAngles", JSON_Data.Value.Type.Object)
							.add("frame"      , JSON_Data.Value.Type.String)
							.add("position"   , JSON_Data.Value.Type.Object);
					/*
						Block "[SaveGameData.SaveGame.TruckDesc.InstalledAddon].extraParents[]" [3]
						    eulerAngles:Object
						    frame:String
						    position:Object
						Block "[SaveGameData.SaveGame.TruckDesc.InstalledAddon].extraParents[].eulerAngles" [3]
						    x:Float
						    y:Float
						    z:Float
						Block "[SaveGameData.SaveGame.TruckDesc.InstalledAddon].extraParents[].position" [3]
						    x:Float
						    y:Float
						    z:Float
					 */
					
					public final Coord3F eulerAngles;
					public final String frame;
					public final Coord3F position;
					
					private ExtraParent(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
					{
						//scanJSON(object, this);
						
						JSON_Object<NV, V> eulerAngles, position;
						
						eulerAngles = JSON_Data.getObjectValue (object, "eulerAngles", debugOutputPrefixStr);
						frame       = JSON_Data.getStringValue (object, "frame"      , debugOutputPrefixStr);
						position    = JSON_Data.getObjectValue (object, "position"   , debugOutputPrefixStr);
						
						KNOWN_JSON_VALUES.scanUnexpectedValues(object);
						
						this.position     = new Coord3F(position   , debugOutputPrefixStr+".position"   );
						this.eulerAngles  = new Coord3F(eulerAngles, debugOutputPrefixStr+".eulerAngles");
					}
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
				SaveGameData.parseObject(object, debugOutputPrefixStr, (jsonValue, valueID, local_debugOutputPrefixStr) -> {
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
				
				static class SplitName_TwoOrs extends SpreadedValuesHelper.SplittedName
				{
					private final String originalStr;
					private final String secondPart;
				
					SplitName_TwoOrs(String originalStr, String valueId, String secondPart)
					{
						super(valueId);
						this.originalStr = originalStr;
						this.secondPart = secondPart;
					}
					
					static SplitName_TwoOrs split(String fieldName)
					{
						// "level_us_03_01 || US_03_01_CR_WD_01"
						// "level_ru_02_01_crop || RU_02_01_BLD_GAS"
						String[] parts = fieldName.split(" \\|\\| ", 2);
						if (parts.length == 2) return new SplitName_TwoOrs(fieldName, parts[0], parts[1]);
						return new SplitName_TwoOrs(fieldName, parts[0], null);
					}
				}
			}
			
			private interface Action2<ValueType, Name extends SplittedName>
			{
				void parseValues(ValueType value, Name name, JSON_Data.Value<NV,V> jsonValue, String local_debugOutputPrefixStr) throws TraverseException;
			}
			
			<Name extends SplittedName> void parseObject(HashMap<String, ValueType> valueMap, JSON_Object<NV, V> object, String debugOutputPrefixStr, Function<String,Name> splitName, Action2<ValueType, Name> parseValues) throws TraverseException
			{
				SaveGameData.parseObject(object, debugOutputPrefixStr, (jsonValue, jsonValueName, local_debugOutputPrefixStr) -> {
					Name name = splitName.apply(jsonValueName);
					String valueID = name.valueID;
					ValueType value = get(valueMap, valueID);
					parseValues.parseValues(value, name, jsonValue, local_debugOutputPrefixStr);
				});
			}
		}
		
		public static class TruckInfos
		{
			private static final SpreadedValuesHelper<TruckInfos> helper = new SpreadedValuesHelper<>(TruckInfos::new);
			
			public final String truckId;
			public Long    owned = null;
			public boolean isNew = false;
			
			private TruckInfos(String truckId)
			{
				this.truckId = truckId;
			}
			
			private static void parseNewTrucks(HashMap<String, TruckInfos> trucks, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseStringArray(trucks, array, debugOutputPrefixStr, truck -> {
					truck.isNew = true;
				});
			}
			
			private static void parseOwnedTrucks(HashMap<String, TruckInfos> trucks, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(trucks, object, debugOutputPrefixStr, (truck, value, local) -> {
					truck.owned = JSON_Data.getIntegerValue(value, local);
				});
			}
		}
		
		public static class RegionInfos
		{
			private static final SpreadedValuesHelper<RegionInfos> helper = new SpreadedValuesHelper<>(RegionInfos::new);
			
			public final MapIndex region;
			public Long distance = null;
			public Boolean isKnown = null;
			public Long paymentsReceived = null;
			
			private RegionInfos(String regionId)
			{
				this.region = MapIndex.parse(regionId);
			}
			
			private static void parseKnownRegions(HashMap<String, RegionInfos> regions, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseStringArray(regions, array, debugOutputPrefixStr, map -> {
					map.isKnown = true;
				});
			}
			
			private static void parseDistances(HashMap<String, RegionInfos> regions, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(regions, object, debugOutputPrefixStr, (region, value, local) -> {
					region.distance = JSON_Data.getIntegerValue(value, local);
				});
			}
			
			private static void parsePaymentsReceived(HashMap<String, RegionInfos> regions, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(regions, object, debugOutputPrefixStr, (region, value, local) -> {
					region.paymentsReceived = JSON_Data.getIntegerValue(value, local);
				});
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
			public final HashMap<String, CargoLoadingCounts> cargoLoadingCounts = new HashMap<>();
			public final HashMap<String, UpgradableGarage>    upgradableGarages = new HashMap<>();
			public final HashMap<String, Long>                upgradesGiverData = new HashMap<>();
			public final HashMap<String, Boolean>                   watchPoints = new HashMap<>();
			public final Vector<Waypoint> waypoints         = new Vector<>();
			public final Vector<String  > discoveredObjects = new Vector<>();
			
			private MapInfos(String mapId)
			{
				this.map = MapIndex.parse(mapId);
			}
			
			private static void parseCargoLoadingCounts(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, SpreadedValuesHelper.SplittedName.SplitName_TwoOrs::split, (map, name, value, local_debugOutputPrefixStr) -> {
					
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
			
			private static void parseUpgradableGarages(HashMap<String, MapInfos> maps, JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
			{
				helper.parseObject(maps, object, debugOutputPrefixStr, SpreadedValuesHelper.SplittedName.SplitName_TwoOrs::split, (map, name, value, local_debugOutputPrefixStr) -> {
					
					JSON_Object<NV, V> object2 = JSON_Data.getObjectValue(value, local_debugOutputPrefixStr);
					UpgradableGarage garage = new UpgradableGarage(object2, name.secondPart, local_debugOutputPrefixStr);
					
					if (name.secondPart==null)
						System.err.printf("[UpgradableGarages] Found a garage with no name on 1 map: %s%n", local_debugOutputPrefixStr);
					else if (map.upgradableGarages.containsKey(name.secondPart))
						System.err.printf("[UpgradableGarages] Found more than 1 garage with same name on 1 map: %s%n", local_debugOutputPrefixStr);
					else
						map.upgradableGarages.put(name.secondPart, garage);
				});
			}
		
			private static void parseDiscoveredObjects(HashMap<String, MapInfos> maps, JSON_Array<NV, V> array, String debugOutputPrefixStr) throws TraverseException
			{
				// "level_us_03_01 || US_03_01_CR_WD_01"
				helper.parseStringArray(maps, array, debugOutputPrefixStr, SpreadedValuesHelper.SplittedName.SplitName_TwoOrs::split, (map, name) -> {
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
					checkEmptyArrayOrUnset(array, local);
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
						parseArray_Object(array, local1, Waypoint::new, map.waypoints);
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
					// scanJSON(object, this);
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
					// scanJSON(object, this);
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
			
			public static class UpgradableGarage
			{
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(UpgradableGarage.class)
						.add("featureStates", JSON_Data.Value.Type.Array )
						.add("isUpgradable" , JSON_Data.Value.Type.Bool  )
						.add("zoneGlobalId" , JSON_Data.Value.Type.String)
						;
				/*
				    Block "[SaveGameData.SaveGame.MapInfos.UpgradableGarage]" [3]
				        featureStates:Array
				        featureStates[]:Bool
				        isUpgradable:Bool
				        zoneGlobalId:String
				 */
				public final String zoneName;
				public final boolean isUpgradable;
				public final String zoneGlobalId;
				public final boolean[] featureStates;
			
				private UpgradableGarage(JSON_Object<NV, V> object, String zoneName, String debugOutputPrefixStr) throws TraverseException
				{
					this.zoneName = zoneName;
					//scanJSON(object, this);
					
					JSON_Array<NV, V> featureStates;
					featureStates = JSON_Data.getArrayValue (object, "featureStates", debugOutputPrefixStr);
					isUpgradable  = JSON_Data.getBoolValue  (object, "isUpgradable" , debugOutputPrefixStr);
					zoneGlobalId  = JSON_Data.getStringValue(object, "zoneGlobalId" , debugOutputPrefixStr);
					
					this.featureStates = parseArray_Bool(featureStates, debugOutputPrefixStr+".featureStates");
					
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
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
					// scanJSON(object, this);
					
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
			public final HashSet<String> savedCargoNeedToBeRemovedOnRestart = new HashSet<>();
			public boolean viewedUnactivated = false;
			public ObjectiveStates objectiveStates = null;
			
			private Objective(String objectiveId)
			{
				this.objectiveId = objectiveId;
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
					checkEmptyArrayOrUnset(array, local);
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
				private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(ObjectiveStates.class)
						.add("failReasons"            , JSON_Data.Value.Type.Object)
						.add("id"                     , JSON_Data.Value.Type.String)
						.add("isFinished"             , JSON_Data.Value.Type.Bool  )
						.add("isTimerStarted"         , JSON_Data.Value.Type.Bool  )
						.add("spentTime"              , JSON_Data.Value.Type.Float )
						.add("stagesState"            , JSON_Data.Value.Type.Array )
						.add("wasCompletedAtLeastOnce", JSON_Data.Value.Type.Bool  )
						;
				/*
				    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates]" [7]
				        failReasons            :  Object
				        id                     :  String
				        isFinished             :  Bool
				        isTimerStarted         : [Bool  , <unset>]
				        spentTime              :  Float
				        stagesState            :  Array
				        wasCompletedAtLeastOnce: [Bool  , <unset>]
				        stagesState[]:Object
				    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates].failReasons" [0]
				 */
				public final String  id;
				public final boolean isFinished;
				public final Boolean isTimerStarted;
				public final double  spentTime;
				public final Boolean wasCompletedAtLeastOnce;
				public final Vector<StagesState> stagesState;

				private ObjectiveStates(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
				{
					// scanJSON(object, this);
					
					JSON_Object<NV, V> failReasons;
					JSON_Array<NV, V> stagesState;
					
					failReasons            = JSON_Data.getObjectValue(object, "failReasons"            , debugOutputPrefixStr);
					id                     = JSON_Data.getStringValue(object, "id"                     , debugOutputPrefixStr);
					isFinished             = JSON_Data.getBoolValue  (object, "isFinished"             , debugOutputPrefixStr);
					isTimerStarted         = JSON_Data.getBoolValue  (object, "isTimerStarted"         , true, false, debugOutputPrefixStr);
					spentTime              = JSON_Data.getFloatValue (object, "spentTime"              , debugOutputPrefixStr);
					stagesState            = JSON_Data.getArrayValue (object, "stagesState"            , debugOutputPrefixStr);
					wasCompletedAtLeastOnce= JSON_Data.getBoolValue  (object, "wasCompletedAtLeastOnce", true, false, debugOutputPrefixStr);
					
					KNOWN_JSON_VALUES.scanUnexpectedValues(object);
					
					checkEmptyObject(failReasons, debugOutputPrefixStr+".failReasons");
					
					this.stagesState = parseArray_Object(stagesState, debugOutputPrefixStr+".stagesState", StagesState::new, new Vector<>());
				}
				
				public static class StagesState
				{
					private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(StagesState.class)
							.add("cargoDeliveryActions", JSON_Data.Value.Type.Array )
							.add("cargoSpawnState"     , JSON_Data.Value.Type.Array )
							.add("changeTruckState"    , JSON_Data.Value.Type.Null  )
							.add("farmingState"        , JSON_Data.Value.Type.Null  )
							.add("livingAreaState"     , JSON_Data.Value.Type.Null  )
							.add("livingAreaState"     , JSON_Data.Value.Type.Object)
							.add("makeActionInZone"    , JSON_Data.Value.Type.Null  )
							.add("truckDeliveryStates" , JSON_Data.Value.Type.Array )
							.add("truckRepairStates"   , JSON_Data.Value.Type.Array )
							.add("visitAllZonesState"  , JSON_Data.Value.Type.Object)
							.add("visitAllZonesState"  , JSON_Data.Value.Type.Null  )
							;
					/*
					    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState]" [9]
					        cargoDeliveryActions: Array 
					        cargoSpawnState     : Array 
					        changeTruckState    : Null  
					        farmingState        :[Null  , <unset>]
					        livingAreaState     :[Object, Null]  ->  LivingAreaState  
					        makeActionInZone    : Null  
					        truckDeliveryStates : Array 
					        truckRepairStates   : Array 
					        visitAllZonesState  :[Object, Null]  ->  VisitAllZonesState
					        
					        cargoDeliveryActions[] : Object or empty array  ->  CargoDeliveryAction
					        cargoSpawnState     [] : Object or empty array  ->  CargoSpawnState
					        truckDeliveryStates [] : Object or empty array  ->  TruckDeliveryState
					        truckRepairStates   [] : Object or empty array  ->  TruckRepairState
					 */
					
					public final LivingAreaState livingAreaState;
					public final VisitAllZonesState visitAllZonesState;
					public final Vector<CargoDeliveryAction> cargoDeliveryActions;
					public final Vector<CargoSpawnState> cargoSpawnState;
					public final Vector<TruckDeliveryState> truckDeliveryStates;
					public final Vector<TruckRepairState> truckRepairStates;
					
					private StagesState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
					{
						//scanJSON(object, this);
						
						@SuppressWarnings("unused")
						JSON_Data.Null changeTruckState_Null, farmingState_Null, livingAreaState_Null, makeActionInZone_Null, visitAllZonesState_Null;
						JSON_Object<NV, V> livingAreaState, visitAllZonesState;
						JSON_Array<NV, V> cargoDeliveryActions, cargoSpawnState, truckDeliveryStates, truckRepairStates;
						
						cargoDeliveryActions    = JSON_Data.getArrayValue (object, "cargoDeliveryActions", debugOutputPrefixStr);
						cargoSpawnState         = JSON_Data.getArrayValue (object, "cargoSpawnState"     , debugOutputPrefixStr);
						changeTruckState_Null   = JSON_Data.getNullValue  (object, "changeTruckState"    , debugOutputPrefixStr);
						farmingState_Null       = JSON_Data.getNullValue  (object, "farmingState"        , true, false, debugOutputPrefixStr);
						livingAreaState         = JSON_Data.getObjectValue(object, "livingAreaState"     , false, true, debugOutputPrefixStr);
						livingAreaState_Null    = JSON_Data.getNullValue  (object, "livingAreaState"     , false, true, debugOutputPrefixStr);
						makeActionInZone_Null   = JSON_Data.getNullValue  (object, "makeActionInZone"    , debugOutputPrefixStr);
						truckDeliveryStates     = JSON_Data.getArrayValue (object, "truckDeliveryStates" , debugOutputPrefixStr);
						truckRepairStates       = JSON_Data.getArrayValue (object, "truckRepairStates"   , debugOutputPrefixStr);
						visitAllZonesState      = JSON_Data.getObjectValue(object, "visitAllZonesState"  , false, true, debugOutputPrefixStr);
						visitAllZonesState_Null = JSON_Data.getNullValue  (object, "visitAllZonesState"  , false, true, debugOutputPrefixStr);
						
						KNOWN_JSON_VALUES.scanUnexpectedValues(object);
						
						this.livingAreaState      = parseObjectOrNull(livingAreaState   , livingAreaState_Null   , LivingAreaState   ::new, debugOutputPrefixStr+".livingAreaState");
						this.visitAllZonesState   = parseObjectOrNull(visitAllZonesState, visitAllZonesState_Null, VisitAllZonesState::new, debugOutputPrefixStr+".visitAllZonesState");
						this.cargoDeliveryActions = parseArray_Object(cargoDeliveryActions, debugOutputPrefixStr+".cargoDeliveryActions", CargoDeliveryAction::new, new Vector<>());
						this.cargoSpawnState      = parseArray_Object(cargoSpawnState     , debugOutputPrefixStr+".cargoSpawnState"     , CargoSpawnState    ::new, new Vector<>());
						this.truckDeliveryStates  = parseArray_Object(truckDeliveryStates , debugOutputPrefixStr+".truckDeliveryStates" , TruckDeliveryState ::new, new Vector<>());
						this.truckRepairStates    = parseArray_Object(truckRepairStates   , debugOutputPrefixStr+".truckRepairStates"   , TruckRepairState   ::new, new Vector<>());
					}
					
					public static class LivingAreaState
					{
						private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(LivingAreaState.class)
								.add("currentLivingAreaValue", JSON_Data.Value.Type.Integer)
								.add("isSynced"              , JSON_Data.Value.Type.Bool   )
								.add("neededLivingAreaValue" , JSON_Data.Value.Type.Integer)
								.add("zoneGlobalId"          , JSON_Data.Value.Type.String )
								;
						/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.LivingAreaState]" [4]
						        currentLivingAreaValue:Integer
						        isSynced              :Bool
						        neededLivingAreaValue :Integer
						        zoneGlobalId          :String
						 */
						public final long currentLivingAreaValue;
						public final boolean isSynced;
						public final long neededLivingAreaValue;
						public final String zoneGlobalId;
						
						private LivingAreaState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
						{
							//scanJSON(object, this);
							currentLivingAreaValue = JSON_Data.getIntegerValue(object, "currentLivingAreaValue", debugOutputPrefixStr);
							isSynced               = JSON_Data.getBoolValue   (object, "isSynced"              , debugOutputPrefixStr);
							neededLivingAreaValue  = JSON_Data.getIntegerValue(object, "neededLivingAreaValue" , debugOutputPrefixStr);
							zoneGlobalId           = JSON_Data.getStringValue (object, "zoneGlobalId"          , debugOutputPrefixStr);
							KNOWN_JSON_VALUES.scanUnexpectedValues(object);
						}
					}
					
					public static class VisitAllZonesState
					{
						private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(VisitAllZonesState.class)
								.add("map"       , JSON_Data.Value.Type.String)
								.add("zoneStates", JSON_Data.Value.Type.Array )
								;
						/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.VisitAllZonesState]" [2]
						        map       :String
						        zoneStates:Array
						        
						        zoneStates[]:Object  ->  ZoneState
						 */
						public final String map;
						public final Vector<ZoneState> zoneStates;
						
						private VisitAllZonesState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
						{
							//scanJSON(object, this);
							JSON_Array<NV, V> zoneStates;
							map        = JSON_Data.getStringValue(object, "map"       , debugOutputPrefixStr);
							zoneStates = JSON_Data.getArrayValue (object, "zoneStates", debugOutputPrefixStr);
							KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							this.zoneStates = parseArray_Object(zoneStates, debugOutputPrefixStr+".zoneStates", ZoneState::new, new Vector<>());
						}
						
						public static class ZoneState
						{
							private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(ZoneState.class)
									.add("isVisitWithCertainTruck", JSON_Data.Value.Type.Bool  )
									.add("isVisited"              , JSON_Data.Value.Type.Bool  )
									.add("truckUid"               , JSON_Data.Value.Type.String)
									.add("zone"                   , JSON_Data.Value.Type.String)
									;
							/*
							    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.VisitAllZonesState.ZoneState]" [4]
							        isVisitWithCertainTruck:Bool
							        isVisited              :Bool
							        truckUid               :String
							        zone                   :String
							 */
							public final boolean isVisitWithCertainTruck;
							public final boolean isVisited;
							public final String truckUid;
							public final String zone;
							
							private ZoneState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
							{
								//scanJSON(object, this);
								isVisitWithCertainTruck = JSON_Data.getBoolValue  (object, "isVisitWithCertainTruck", debugOutputPrefixStr);
								isVisited               = JSON_Data.getBoolValue  (object, "isVisited"              , debugOutputPrefixStr);
								truckUid                = JSON_Data.getStringValue(object, "truckUid"               , debugOutputPrefixStr);
								zone                    = JSON_Data.getStringValue(object, "zone"                   , debugOutputPrefixStr);
								KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							}
						}
					}
					
					public static class CargoDeliveryAction
					{
						private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(CargoDeliveryAction.class)
								.add("cargoState"        , JSON_Data.Value.Type.Object )
								.add("isNeedVisitOnTruck", JSON_Data.Value.Type.Bool   )
								.add("map"               , JSON_Data.Value.Type.String )
								.add("modelBuildingTag"  , JSON_Data.Value.Type.String )
								.add("platformId"        , JSON_Data.Value.Type.String )
								.add("truckUid"          , JSON_Data.Value.Type.String )
								.add("unloadingMode"     , JSON_Data.Value.Type.Integer)
								.add("zones"             , JSON_Data.Value.Type.Array  )
								;
						/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.CargoDeliveryAction]" [8]
						        cargoState        :Object  ->  CargoState
						        isNeedVisitOnTruck:Bool
						        map               :String
						        modelBuildingTag  :String
						        platformId        :String
						        truckUid          :String
						        unloadingMode     :Integer
						        zones             :Array
						        
						        zones[]:String
						 */
						public final CargoState cargoState;
						public final boolean isNeedVisitOnTruck;
						public final String map;
						public final String modelBuildingTag;
						public final String platformId;
						public final String truckUid;
						public final long unloadingMode;
						public final Vector<String> zones;
						
						private CargoDeliveryAction(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
						{
							//scanJSON(object, this);
							JSON_Object<NV, V> cargoState;
							JSON_Array<NV, V> zones;
							cargoState         = JSON_Data.getObjectValue (object, "cargoState"        , debugOutputPrefixStr);
							isNeedVisitOnTruck = JSON_Data.getBoolValue   (object, "isNeedVisitOnTruck", debugOutputPrefixStr);
							map                = JSON_Data.getStringValue (object, "map"               , debugOutputPrefixStr);
							modelBuildingTag   = JSON_Data.getStringValue (object, "modelBuildingTag"  , debugOutputPrefixStr);
							platformId         = JSON_Data.getStringValue (object, "platformId"        , debugOutputPrefixStr);
							truckUid           = JSON_Data.getStringValue (object, "truckUid"          , debugOutputPrefixStr);
							unloadingMode      = JSON_Data.getIntegerValue(object, "unloadingMode"     , debugOutputPrefixStr);
							zones              = JSON_Data.getArrayValue  (object, "zones"             , debugOutputPrefixStr);
							KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							
							this.cargoState = new CargoState   (cargoState, debugOutputPrefixStr+".cargoState");
							this.zones      = parseArray_String(zones     , debugOutputPrefixStr+".zones", new Vector<>());
						}
						
						public static class CargoState
						{
							private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(CargoState.class)
									.add("aimValue", JSON_Data.Value.Type.Integer)
									.add("curValue", JSON_Data.Value.Type.Integer)
									.add("type"    , JSON_Data.Value.Type.String )
									;
							/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.CargoDeliveryAction.CargoState]" [3]
						        aimValue:Integer
						        curValue:Integer
						        type    :String
							 */
							public final long aimValue;
							public final long curValue;
							public final String type;
							
							private CargoState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
							{
								//scanJSON(object, this);
								aimValue = JSON_Data.getIntegerValue(object, "aimValue", debugOutputPrefixStr);
								curValue = JSON_Data.getIntegerValue(object, "curValue", debugOutputPrefixStr);
								type     = JSON_Data.getStringValue (object, "type"    , debugOutputPrefixStr);
								KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							}
						}
					}
					
					public static class CargoSpawnState
					{
						private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(CargoSpawnState.class)
								.add("cargos"                             , JSON_Data.Value.Type.Array )
								.add("ignoreHidingCargoWhenNotTracked"    , JSON_Data.Value.Type.Bool  )
								.add("needToBeDiscoveredByMetallodetector", JSON_Data.Value.Type.Bool  )
								.add("spawned"                            , JSON_Data.Value.Type.Bool  )
								.add("zone"                               , JSON_Data.Value.Type.Object)
								;
						/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.CargoSpawnState]" [4]
						        cargos                             :Array
						        ignoreHidingCargoWhenNotTracked    :Bool
						        needToBeDiscoveredByMetallodetector:Bool
						        spawned                            :Bool
						        zone                               :Object   ->  Zone
						        
						        cargos[]:Object   ->  Cargo
						 */
						public final Vector<Cargo> cargos;
						public final boolean ignoreHidingCargoWhenNotTracked;
						public final boolean needToBeDiscoveredByMetallodetector;
						public final boolean spawned;
						public final Zone zone;
						
						private CargoSpawnState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
						{
							//scanJSON(object, this);
							JSON_Object<NV, V> zone;
							JSON_Array<NV, V> cargos;
							cargos                              = JSON_Data.getArrayValue (object, "cargos"                             , debugOutputPrefixStr);
							ignoreHidingCargoWhenNotTracked     = JSON_Data.getBoolValue  (object, "ignoreHidingCargoWhenNotTracked"    , debugOutputPrefixStr);
							needToBeDiscoveredByMetallodetector = JSON_Data.getBoolValue  (object, "needToBeDiscoveredByMetallodetector", debugOutputPrefixStr);
							spawned                             = JSON_Data.getBoolValue  (object, "spawned"                            , debugOutputPrefixStr);
							zone                                = JSON_Data.getObjectValue(object, "zone"                               , debugOutputPrefixStr);
							KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							this.cargos = parseArray_Object(cargos, debugOutputPrefixStr+".cargos", Cargo::new, new Vector<>());
							this.zone   = new Zone         (zone  , debugOutputPrefixStr+".zone"  );
						}
						
						public static class Cargo
						{
							private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Cargo.class)
									.add("count", JSON_Data.Value.Type.Integer)
									.add("name" , JSON_Data.Value.Type.String )
									;
							/*
							    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.CargoSpawnState.Cargo]" [2]
							        count:Integer
							        name :String
							 */
							public final long count;
							public final String name;
							
							private Cargo(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
							{
								//scanJSON(object, this);
								count = JSON_Data.getIntegerValue(object, "count", debugOutputPrefixStr);
								name  = JSON_Data.getStringValue (object, "name" , debugOutputPrefixStr);
								KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							}
						}
						
						public static class Zone
						{
							private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(Zone.class)
									.add("cached"      , JSON_Data.Value.Type.Bool  )
									.add("globalZoneId", JSON_Data.Value.Type.String)
									.add("map"         , JSON_Data.Value.Type.String)
									.add("zoneLocal"   , JSON_Data.Value.Type.String)
									;
							/*
							    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.CargoSpawnState.Zone]" [4]
							        cached      :Bool
							        globalZoneId:String
							        map         :String
							        zoneLocal   :String
							 */
							public final boolean cached;
							public final String globalZoneId;
							public final String map;
							public final String zoneLocal;
							
							private Zone(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
							{
								//scanJSON(object, this);
								cached       = JSON_Data.getBoolValue  (object, "cached"      , debugOutputPrefixStr);
								globalZoneId = JSON_Data.getStringValue(object, "globalZoneId", debugOutputPrefixStr);
								map          = JSON_Data.getStringValue(object, "map"         , debugOutputPrefixStr);
								zoneLocal    = JSON_Data.getStringValue(object, "zoneLocal"   , debugOutputPrefixStr);
								KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							}
						}
					}
					
					public static class TruckDeliveryState
					{
						private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(TruckDeliveryState.class)
								.add("deliveryZones", JSON_Data.Value.Type.Array )
								.add("isDelivered"  , JSON_Data.Value.Type.Bool  )
								.add("mapDelivery"  , JSON_Data.Value.Type.String)
								.add("truckId"      , JSON_Data.Value.Type.String)
								;
						/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.TruckDeliveryState]" [4]
						        deliveryZones : Array
						        isDelivered   : Bool
						        mapDelivery   : String
						        truckId       : String
						        
						        deliveryZones[]:String
						 */
						public final boolean isDelivered;
						public final String mapDelivery;
						public final String truckId;
						public final Vector<String> deliveryZones;
						
						private TruckDeliveryState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
						{
							//scanJSON(object, this);
							JSON_Array<NV, V> deliveryZones;
							deliveryZones = JSON_Data.getArrayValue (object, "deliveryZones", debugOutputPrefixStr);
							isDelivered   = JSON_Data.getBoolValue  (object, "isDelivered"  , debugOutputPrefixStr);
							mapDelivery   = JSON_Data.getStringValue(object, "mapDelivery"  , debugOutputPrefixStr);
							truckId       = JSON_Data.getStringValue(object, "truckId"      , debugOutputPrefixStr);
							KNOWN_JSON_VALUES.scanUnexpectedValues(object);
							
							this.deliveryZones = parseArray_String(deliveryZones, debugOutputPrefixStr+".deliveryZones", new Vector<>());
						}
					}
					
					public static class TruckRepairState
					{
						private static final KnownJsonValues<NV, V> KNOWN_JSON_VALUES = KJV_FACTORY.create(TruckRepairState.class)
								.add("isRefueled", JSON_Data.Value.Type.Bool  )
								.add("isRepaired", JSON_Data.Value.Type.Bool  )
								.add("truckId"   , JSON_Data.Value.Type.String)
								;
						/*
						    Block "[SaveGameData.SaveGame.Objective.ObjectiveStates.StagesState.TruckRepairState]" [3]
						        isRefueled : Bool
						        isRepaired : Bool
						        truckId    : String
						 */
						public final boolean isRefueled;
						public final boolean isRepaired;
						public final String truckId;
						
						private TruckRepairState(JSON_Object<NV, V> object, String debugOutputPrefixStr) throws TraverseException
						{
							//scanJSON(object, this);
							isRefueled = JSON_Data.getBoolValue  (object, "isRefueled", debugOutputPrefixStr);
							isRepaired = JSON_Data.getBoolValue  (object, "isRepaired", debugOutputPrefixStr);
							truckId    = JSON_Data.getStringValue(object, "truckId"   , debugOutputPrefixStr);
							KNOWN_JSON_VALUES.scanUnexpectedValues(object);
						}
					}
				}
			}
		}
	}

}
