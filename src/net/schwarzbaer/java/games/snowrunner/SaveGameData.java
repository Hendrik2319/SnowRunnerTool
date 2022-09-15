package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.NamedValue;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Null;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;

@SuppressWarnings("unused")
public class SaveGameData {
	
	private static final List<String> DEFAULT_FILES = Arrays.asList( "CommonSslSave.cfg", "CompleteSave.cfg", "user_profile.cfg", "user_settings.cfg", "user_social_data.cfg" );
	private static final String SAVEGAME_PREFIX = "CompleteSave";
	private static final String SAVEGAME_SUFFIX = ".cfg";
	
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
			return !name.equals(SAVEGAME_PREFIX+SAVEGAME_SUFFIX);
		});
		for (File file : saveFiles) {
			JSON_Data.Value<NV, V> value = readJsonFile(file);
			if (value!=null) {
				rawJsonData.put(file.getName(), value);
				String name = file.getName();
				String indexStr = name.substring(SAVEGAME_PREFIX.length(), name.length()-SAVEGAME_SUFFIX.length());
				try {
					saveGames.put(indexStr, new SaveGameData.SaveGame(indexStr, value));
				} catch (TraverseException e) {
					System.err.printf("Can't parse SaveGame \"%s\": %s", indexStr, e.getMessage());
					//e.printStackTrace();
				}
			}
		}
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
	
		public final String indexStr;
		public final JSON_Data.Value<NV, V> data;
		public final HashMap<String, Integer> ownedTrucks;
		public final Long experience;
		public final Long money;
		public final Long rank;
		public final long saveTime;
		public final double gameTime;
		public final boolean isHardMode;
		public final String worldConfiguration;

		private SaveGame(String indexStr, JSON_Data.Value<NV, V> data) throws TraverseException {
			if (data==null)
				throw new IllegalArgumentException();
			
			this.indexStr = indexStr;
			this.data = data;
			
			JSON_Data.Value<NV, V> sslValue = JSON_Data.getSubNode(this.data, "CompleteSave"+indexStr, "SslValue");
			
			String debugOutputPrefixStr = "CompleteSave"+indexStr+".SslValue";
			JSON_Object<NV, V> sslValueObj = JSON_Data.getObjectValue(sslValue, debugOutputPrefixStr);
			
			gameTime           = JSON_Data.getFloatValue (sslValueObj, "gameTime"          , debugOutputPrefixStr);
			isHardMode         = JSON_Data.getBoolValue  (sslValueObj, "isHardMode"        , debugOutputPrefixStr);
			worldConfiguration = JSON_Data.getStringValue(sslValueObj, "worldConfiguration", debugOutputPrefixStr);
			
			JSON_Object<NV, V> saveTime = JSON_Data.getObjectValue(sslValueObj, "saveTime", debugOutputPrefixStr);
			String timestampStr = JSON_Data.getStringValue(saveTime, "timestamp", debugOutputPrefixStr+".saveTime");
			if (!timestampStr.startsWith("0x"))
				throw new JSON_Data.TraverseException("Unexpected string value in %s: %s", debugOutputPrefixStr+".saveTime.timestamp", timestampStr);
			
			this.saveTime = Long.parseUnsignedLong(timestampStr.substring(2), 16);
			
			
			JSON_Object<NV, V> persistentProfileData      = JSON_Data.getObjectValue(sslValueObj, "persistentProfileData", false, true, debugOutputPrefixStr);
			JSON_Data.Null     persistentProfileData_Null = JSON_Data.getNullValue  (sslValueObj, "persistentProfileData", false, true, debugOutputPrefixStr);
			if (persistentProfileData==null && persistentProfileData_Null==null)
				throw new JSON_Data.TraverseException("Unexpected type of <persistentProfileData>");
			
			if (persistentProfileData!=null) {
				String local_debugOutputPrefixStr = debugOutputPrefixStr+".persistentProfileData";
				
				experience = JSON_Data.getIntegerValue(persistentProfileData, "experience", local_debugOutputPrefixStr);
				money      = JSON_Data.getIntegerValue(persistentProfileData, "money"     , local_debugOutputPrefixStr);
				rank       = JSON_Data.getIntegerValue(persistentProfileData, "rank"      , local_debugOutputPrefixStr);
				
				JSON_Object<NV, V> ownedTrucks = JSON_Data.getObjectValue(persistentProfileData, "ownedTrucks", local_debugOutputPrefixStr);
				this.ownedTrucks = new HashMap<>();
				for (JSON_Data.NamedValue<NV, V> nv : ownedTrucks) {
					int amount = (int) JSON_Data.getIntegerValue(nv.value, local_debugOutputPrefixStr+".ownedTrucks."+nv.name);
					this.ownedTrucks.put(nv.name, amount);
				}
				
			} else {
				ownedTrucks = null;
				experience = null;
				money      = null;
				rank       = null;
			}
		}

		public int getOwnedTruckCount(Truck truck) {
			if (truck==null) return 0;
			if (ownedTrucks==null) return 0;
			Integer amount = ownedTrucks.get(truck.id);
			return amount==null ? 0 : amount.intValue();
		}

		public boolean playerOwnsTruck(Truck truck) {
			return getOwnedTruckCount(truck)>0;
		}
	
	}

}
