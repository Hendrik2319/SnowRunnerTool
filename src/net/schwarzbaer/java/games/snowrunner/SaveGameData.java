package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;

@SuppressWarnings("unused")
class SaveGameData {
	
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
				saveGames.put(indexStr, new SaveGameData.SaveGame(value));
			}
		}
	}

	private JSON_Data.Value<NV, V> readJsonFile(File file) {
		if (!file.isFile()) return null;
		JSON_Parser<NV,V> parser = new JSON_Parser<>(file, null);
		try {
			return parser.parse_withParseException();
		} catch (ParseException e) {
			System.err.printf("ParseException while parsing JSON file \"%s\": %s%n", file.getAbsolutePath(), e.getMessage());
			//e.printStackTrace();
			return null;
		}
	}

	static class SaveGame {
	
		SaveGame(JSON_Data.Value<NV, V> value) {
			// TODO Auto-generated constructor stub
		}
	
	}

}
