package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import net.schwarzbaer.java.games.snowrunner.MapTypes.SetMap;
import net.schwarzbaer.java.games.snowrunner.MapTypes.StringVectorMap;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.SpecialTruckAddons;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.TextOutput;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.lib.gui.ValueListOutput;

public class Data {

	private static SetMap<String,String> unexpectedValues;
	
	public final XMLTemplateStructure rawdata;
	public final HashMap<String,Language> languages;
	public final AddonCategories addonCategories;
	public final HashMap<String,CargoType> cargoTypes;
	public final HashMap<String,WheelsDef> wheels;
	public final HashMap<String,Truck> trucks;
	public final HashMap<String,Trailer> trailers;
	public final HashMap<String,TruckAddon> truckAddons;
	final StringVectorMap<Truck> socketIDsUsedByTrucks;
	final StringVectorMap<Trailer> socketIDsUsedByTrailers;
	final StringVectorMap<TruckAddon> socketIDsUsedByTruckAddons;
	public final TruckComponentSets<Engine    > engines    ;
	public final TruckComponentSets<Gearbox   > gearboxes  ;
	public final TruckComponentSets<Suspension> suspensions;
	public final TruckComponentSets<Winch     > winches    ;

	Data(XMLTemplateStructure rawdata) {
		this.rawdata = rawdata;
		languages = rawdata.languages;
		
		unexpectedValues = new SetMap<>(null,null);
		
		//HashSetMap<String,String> baseNodesInClasses = new HashSetMap<>(null,null);
		//rawdata.classes.forEach((className,class_)->{
		//	class_.items.forEach((itemName,item)->{
		//		baseNodesInClasses.add(item.content.nodeName, className);
		//	});
		//});
		//baseNodesInClasses.print(System.out, "NodeNames in Classes");
		
		
		
		Class_ addonCategoriesClass = rawdata.classes.get("addons_category");
		if (addonCategoriesClass == null)
			addonCategories = null;
		else {
			Item addonCategoriesItem = addonCategoriesClass.items.get("addons_category");
			if (addonCategoriesItem == null)
				addonCategories = null;
			else
				addonCategories = new AddonCategories(addonCategoriesItem);
		}
		
		
		
		cargoTypes = new HashMap<>();
		Class_ cargoTypesClass = rawdata.classes.get("cargo_types");
		if (cargoTypesClass!=null)
			cargoTypesClass.items.forEach((name,item)->{
				expectRuntimeException(
						item.filePath,
						() -> {
							cargoTypes.put(name, new CargoType(item));
						}
				);
			});
		
		
		
		    engines = new TruckComponentSets<>();
		  gearboxes = new TruckComponentSets<>();
		suspensions = new TruckComponentSets<>();
		    winches = new TruckComponentSets<>();
		TruckComponent.parseSet( rawdata,     "engines",     engines,     Engine::new,        "EngineVariants",        "Engine");
		TruckComponent.parseSet( rawdata,   "gearboxes",   gearboxes,    Gearbox::new,       "GearboxVariants",       "Gearbox");		
		TruckComponent.parseSet( rawdata, "suspensions", suspensions, Suspension::new, "SuspensionSetVariants", "SuspensionSet");
		TruckComponent.parseSet( rawdata,     "winches",     winches,      Winch::new,         "WinchVariants",         "Winch");
		
		
		
		//MultiMap<String> wheelsTypes = new MultiMap<>();
		wheels = new HashMap<>();
		Class_ wheelsClass = rawdata.classes.get("wheels");
		if (wheelsClass!=null)
			wheelsClass.items.forEach((name,item)->{
				expectRuntimeException(
						item.filePath,
						() -> {
							//wheelsTypes.add(item.content.nodeName, item.filePath);
							if (item.isMainItem() && item.content.nodeName.equals("TruckWheels"))
								wheels.put(name, new WheelsDef(item));
						}
				);
			});
		//System.err.println("WheelsTypes:");
		//wheelsTypes.printTo(System.err, str->str);
		
		
		
		// Truck      Attributes: {AttachType=[Saddle], Type=[Trailer]}
		// TruckAddon Attributes: {IsChassisFullOcclusion=[true]}
		trucks = new HashMap<>();
		trailers = new HashMap<>();
		truckAddons = new HashMap<>();
		
		Class_ trucksClass = rawdata.classes.get("trucks");
		if (trucksClass!=null)
			trucksClass.items.forEach((name,item)->{
				expectRuntimeException(
						item.filePath,
						() -> {
							switch (item.content.nodeName) {
							
							case "Truck":
								String type = item.content.attributes.get("Type");
								
								if (type==null)
									trucks.put(name, new Truck(item, this));
								
								else
									switch (type) {
									case "Trailer":
										trailers.put(name, new Trailer(item));
										break;
										
									default:
										unexpectedValues.add("Class[trucks] <Truck Type=\"###\">", type);
										break;
									}
								break;
								
							case "TruckAddon":
								truckAddons.put(name, new TruckAddon(item, cargoTypes));
								break;
								
							default:
								unexpectedValues.add("Class[trucks] <###>", item.content.nodeName);
								break;
							}
						}
				);
			});
		
		
		
		socketIDsUsedByTrucks = new StringVectorMap<>();
		for (Truck truck : trucks.values()) {
			
			truck.defaultAddons.clear();
			for (String id : truck.defaultAddonIDs) {
				TruckAddon addon = truckAddons.get(id);
				if (addon!=null) truck.defaultAddons.add(addon);
			}
			
			for (Truck.AddonSockets as : truck.addonSockets) {
				if (as.defaultAddonID!=null) {
					as.defaultAddonItem = truckAddons.get(as.defaultAddonID);
					if (as.defaultAddonItem!=null) {
						if ("awd".equals(as.defaultAddonItem.gameData.category))
							truck.defaultAWD = as.defaultAddonItem;
						if ("diff_lock".equals(as.defaultAddonItem.gameData.category))
							truck.defaultDiffLock = as.defaultAddonItem;
					}
				}
				for (String socketID : as.compatibleSocketIDs)
					socketIDsUsedByTrucks.add(socketID, truck);
			}
		}

		socketIDsUsedByTrailers = new StringVectorMap<>();
		for (Trailer trailer : trailers.values())
			if (trailer.gameData.installSocket != null)
				socketIDsUsedByTrailers.add(trailer.gameData.installSocket, trailer);
			else
				System.err.printf("No InstallSocket for trailer <%s>%n", trailer.id);
		
		socketIDsUsedByTruckAddons = new StringVectorMap<>();
		for (TruckAddon truckAddon : truckAddons.values())
			if (truckAddon.gameData.installSocket!=null)
				socketIDsUsedByTruckAddons.add(truckAddon.gameData.installSocket, truckAddon);
			//else
			//	System.err.printf("No InstallSocket for truck addon <%s>%n", truckAddon.id);
		
		
		for (Truck truck : trucks.values()) {
			truck.compatibleTrailers.clear();
			truck.compatibleTruckAddons.clear();
			
			// add all Addons & Trailers by SocketID
			for (Truck.AddonSockets as : truck.addonSockets) {
				for (String socketID : as.compatibleSocketIDs) {
					
					Vector<Trailer> trailers = socketIDsUsedByTrailers.get(socketID);
					if (trailers != null) {
						as.compatibleTrailers.addAll(socketID, trailers);
						truck.compatibleTrailers.addAll(trailers);
					}
					
					Vector<TruckAddon> truckAddons = socketIDsUsedByTruckAddons.get(socketID);
					if (truckAddons != null) {
						as.compatibleTruckAddons.addAll(socketID, truckAddons);
						for (TruckAddon truckAddon : truckAddons)
							truck.compatibleTruckAddons.add(truckAddon.getCategory(), truckAddon);
					}
				}
			}
			
			// remove some Addons & Trailers because of RequiredAddOns
			boolean haveSomeRemoved = true;
			while (haveSomeRemoved) {
				haveSomeRemoved = false;
				for (Vector<TruckAddon> list : truck.compatibleTruckAddons.values()) {
					Vector<TruckAddon> addonsToRemove = new Vector<>();
					for (TruckAddon addon : list) {
						if (	findAddon(addon.id, truck.gameData.excludeAddons) ||
								!findRequiredAddons(addon.gameData.requiredAddons,truck.compatibleTruckAddons,truck.compatibleTrailers)) {
							addonsToRemove.add(addon);
							haveSomeRemoved = true;
							//System.out.printf("Truck<%-20s>: Remove Addon <%s>%n", truck.id, addon.id);
						}
					}
					list.removeAll(addonsToRemove);
				}
				Vector<Trailer> trailersToRemove = new Vector<>();
				for (Trailer trailer : truck.compatibleTrailers) {
					if (	findAddon(trailer.id, truck.gameData.excludeAddons) ||
							!findRequiredAddons(trailer.gameData.requiredAddons,truck.compatibleTruckAddons,truck.compatibleTrailers)) {
						trailersToRemove.add(trailer);
						haveSomeRemoved = true;
						//System.out.printf("Truck<%-20s>: Remove Trailer <%s>%n", truck.id, trailer.id);
					}
				}
				truck.compatibleTrailers.removeAll(trailersToRemove);
			}
			truck.compatibleTruckAddons.removeEmptyLists();
			
			for (Trailer trailer : truck.compatibleTrailers)
				trailer.usableBy.add(truck);
			
			truck.hasCompatibleAWD = false;
			truck.hasCompatibleDiffLock = false;
			for (Vector<TruckAddon> list : truck.compatibleTruckAddons.values())
				for (TruckAddon addon : list) {
					addon.usableBy.add(truck);
					if (addon.enablesAllWheelDrive!=null && addon.enablesAllWheelDrive.booleanValue())
						truck.hasCompatibleAWD = true;
					if (addon.enablesDiffLock!=null && addon.enablesDiffLock.booleanValue())
						truck.hasCompatibleDiffLock = true;
				}
		}
		
		for (TruckAddon cargo : truckAddons.values())
			if (cargo.gameData.isCargo)
			{
				for (Trailer trailer : trailers.values())
					if (isCompatibleCarrier(cargo, trailer.gameData))
						trailer.compatibleCargo.add(cargo);
				
				for (TruckAddon truckAddon : truckAddons.values())
					if (isCompatibleCarrier(cargo, truckAddon.gameData))
						truckAddon.compatibleCargo.add(cargo);
			}
		
		
		if (!unexpectedValues.isEmpty())
			unexpectedValues.print(System.out,"Unexpected Values");
	}
	
	private void expectRuntimeException(String filePath, Runnable runnable)
	{
		try {
			runnable.run();
		}
		catch (RuntimeException ex) {
			throw new RuntimeException("%s while parsing file \"%s\"".formatted(
					ex.getClass().getName(),
					filePath
			), ex);
		}
	}

	private static boolean isCompatibleCarrier(TruckAddon cargo, GameData.GameDataT3NonTruck carrierGameData)
	{
		if (carrierGameData==null) return false;
		if ( cargo==null) return false;
		if (!cargo.gameData.isCargo) return false;
		if ( cargo.gameData.cargoLength==null) return false;
		
		if (carrierGameData.cargoSlots==null) return false;
		if (carrierGameData.cargoSlots.intValue() < cargo.gameData.cargoLength.intValue()) return false;
		
		if (contains(carrierGameData.excludedCargoTypes, cargo.gameData.cargoType)) return false;
		
		if (cargo.gameData.cargoAddonSubtype!=null)
			if (!hasItemACompatibleLoadArea(()->carrierGameData, cargo.gameData.cargoType, cargo.gameData.cargoAddonSubtype, false))
				return false;
			
		return true;
	}

	public static class UserDefinedValues extends SnowRunner.Initializable
	{
		private final HashMap<String,Truck.UDV> truckValues = new HashMap<>();

		void read()
		{
			DataFiles.DataSource ds = DataFiles.DataFile.UserDefinedValuesFile.getDataSourceForReading();
			System.out.printf("Read UserDefinedValues from %s ...%n", ds);
			truckValues.clear();
			
			try (BufferedReader in = new BufferedReader(new InputStreamReader(ds.createInputStream(), StandardCharsets.UTF_8))) {
				
				Truck.UDV truckValues = null;
				String line, valueStr;
				while ( (line=in.readLine())!=null ) {
					
					if (line.equals(""))
						truckValues = null;
					if (line.equals("[Truck]"))
						truckValues = new Truck.UDV();
					
					if ( (valueStr=getLineValue(line,"id="))!=null && truckValues!=null) {
						String id = valueStr;
						this.truckValues.put(id, truckValues);
					}
					
					if ( (valueStr=getLineValue(line,"AWD="))!=null && truckValues!=null)
						truckValues.realAWD = Truck.UDV.ItemState.parse(valueStr);
					
					if ( (valueStr=getLineValue(line,"DiffLock="))!=null && truckValues!=null)
						truckValues.realDiffLock = Truck.UDV.ItemState.parse(valueStr);
					
				}
				
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//showValues();
			System.out.printf("... done%n");
			setInitialized();
		}
		
		public void write()
		{
			checkInitialized();
			File file = DataFiles.DataFile.UserDefinedValuesFile.getFileForWriting();
			System.out.printf("Write UserDefinedValues to file \"%s\" ...%n", file.getAbsolutePath());
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				
				Vector<String> keys = new Vector<>(truckValues.keySet());
				keys.sort(null);
				for (String key : keys) {
					Truck.UDV values = truckValues.get(key);
					if (!values.isEmpty()) {
						out.printf("[Truck]%n");
						out.printf("id=%s%n", key);
						if (values.realAWD     !=null) out.printf("AWD="     +"%s%n", values.realAWD     .name());
						if (values.realDiffLock!=null) out.printf("DiffLock="+"%s%n", values.realDiffLock.name());
						out.printf("%n");
					}
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.out.printf("... done%n");
		}

		@SuppressWarnings("unused")
		private void showValues() {
			checkInitialized();
			System.out.printf("   TruckValues: [%d]%n", truckValues.size());
			Vector<String> keys = new Vector<>(truckValues.keySet());
			keys.sort(null);
			for (String key : keys)
				System.out.printf("      <%s>: %s%n", key, truckValues.get(key).toString());
		}

		public Truck.UDV getOrCreateTruckValues(String id) {
			checkInitialized();
			Truck.UDV values = truckValues.get(id);
			if (values==null) truckValues.put(id, values = new Truck.UDV());
			return values;
		}

		public Truck.UDV getTruckValues(String id) {
			checkInitialized();
			Truck.UDV values = truckValues.get(id);
			if (values==null) values = new Truck.UDV();
			return values ;
		}
	}
	
	public static String getLineValue(String line, String prefix) {
		if (!line.startsWith(prefix)) return null;
		return line.substring(prefix.length());
	}

	private static boolean findRequiredAddons(String[][] requiredAddons, StringVectorMap<TruckAddon> compatibleAddons, HashSet<Trailer> compatibleTrailers) {
		for (int andIndex=0; andIndex<requiredAddons.length; andIndex++) {
			boolean foundARequiredAddon = false;
			for (int orIndex=0; orIndex<requiredAddons[andIndex].length; orIndex++) {
				String requiredAddon = requiredAddons[andIndex][orIndex];
				Predicate<ItemBasedNonTruck> predicate = item -> requiredAddon.equals(item.id);
				if (	findOne(compatibleAddons  , predicate)!=null ||
						findOne(compatibleTrailers, predicate)!=null	) {
					foundARequiredAddon = true;
					break;
				}
			}
			if (!foundARequiredAddon)
				return false;
		}
		return true;
	}

	private static <ItemType extends ItemBased> ItemType findOne(StringVectorMap<ItemType> items, Predicate<? super ItemType> predicate)
	{
		if (items!=null)
			for (Vector<ItemType> list : items.values())
				for (ItemType item : list)
					if (predicate.test(item))
						return item;
		return null;
	}

	private static <ItemType extends ItemBased> ItemType findOne(Collection<ItemType> items, Predicate<? super ItemType> predicate)
	{
		if (items!=null)
			for (ItemType item : items)
				if (predicate.test(item))
					return item;
		return null;
	}

	private static boolean findAddon(String id, String[] idList) {
		if (idList!=null)
			for (String id_ : idList)
				if (id_.equals(id))
					return true;
		return false;
	}

	private static <ValueType> void readEntries(ZipFile zipFile, ZipEntryTreeNode[] nodes, HashMap<String,ValueType> targetMap, String targetMapLabel, BiFunction<String,InputStream,ValueType> readInput) throws IOException {
		for (ZipEntryTreeNode node:nodes) {
			ValueType value = readEntry(zipFile, node, readInput);
			if (value!=null) {
				if (targetMap.containsKey(node.name))
					System.err.printf("[%s] Found redundant key in HashMap: %s%n", targetMapLabel, node.name);
				targetMap.put(node.name, value);
			}
		}
	}

	private static <ValueType> ValueType readEntry(ZipFile zipFile, ZipEntryTreeNode node, BiFunction<String, InputStream, ValueType> readInput) throws IOException {
		InputStream input = zipFile.getInputStream(node.entry);
		return readInput.apply(node.name, input);
	}
	
	static void readLanguages(ZipFile zipFile, ZipEntryTreeNode zipRoot, HashMap<String, Language> languages) throws IOException {
		Predicate<String> isSTR = fileName->fileName.endsWith(".str");
		ZipEntryTreeNode[] languageNodes = zipRoot.getSubFiles("[strings]", isSTR);
		readEntries(zipFile, languageNodes, languages, "languages", (name,input)->Language.readFrom(name, input));
	}

	public static class Language {
		public  String name;
		private final HashMap<String,String> dictionary;
		public  RegionNames regionNames;
		
		Language(String name) {
			this.name = name;
			this.dictionary = new HashMap<>();
			regionNames = null;
		}
		
		public String get(String id)
		{
			return dictionary.get(id);
		}
		
		void scanRegionNames(boolean verbose)
		{
			if (regionNames == null)
			{
				regionNames = new RegionNames();
				regionNames.scanRegionNames(this, verbose);
			}
		}

		static Language readFrom(String name, InputStream input) {
			
			Language language = new Language(name);
			
			try (BufferedReader in = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_16LE))) {
				
				String line;
				int lineNumber = 0;
				while ( (line=in.readLine())!=null ) {
					lineNumber++;
					
					int pos = line.indexOf('\t');
					if (pos<0) {
						System.err.printf("Wrong line (%d) in language file (\"%s\"). Will be ignored.%n", lineNumber, name);
						continue;
					}
					
					String key = line.substring(0, pos);
					String value = line.substring(pos).trim();
					if (value.startsWith("\"")) value = value.substring(1);
					if (value.endsWith  ("\"")) value = value.substring(0,value.length()-1);
					
					language.dictionary.put(key, value);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return language;
		}
	}
	
	public record MapIndex(String country, Integer region, Integer map, String originalMapID, String extra)
	{
		private static final String[] countries = new String[] {"US","RU"};
		
		public boolean isMap    () { return country!=null && region!=null && map!=null; }
		public boolean isRegion () { return country!=null && region!=null && map==null; }
		public boolean isCountry() { return country!=null && region==null && map==null; }
		
		public static MapIndex parse(String mapID)
		{
			// mapID: level_ru_02_03
			String  country     = null;
			Integer regionIndex = null;
			Integer mapIndex    = null;
			String  extra       = null;
			String  original    = mapID;
			
			if (mapID==null)
				return new MapIndex(null, null, null, null, null);
			
			if (mapID.toLowerCase().startsWith("level_"))
				mapID = mapID.substring("level_".length());
			
			String[] parts = mapID.split("_");
			int lastParsedPart = -1;
			
			for (String str : countries)
				if (parts[0].equalsIgnoreCase(str))
				{
					country = str;
					lastParsedPart = 0;
				}
			
			if (country!=null && parts.length>1)
			{
				regionIndex = parseInt(parts[1]);
				if (regionIndex!=null)
					lastParsedPart = 1;
			}
			
			if (regionIndex!=null && parts.length>2)
			{
				mapIndex = parseInt(parts[2]);
				if (mapIndex!=null)
					lastParsedPart = 2;
			}
			
			if (parts.length > lastParsedPart+2)
				extra = String.join("_", Arrays.copyOfRange(parts, lastParsedPart+1, parts.length));
			
			return new MapIndex(country, regionIndex, mapIndex, original, extra);
		}
	}
	
	public static class RegionNames
	{
		private static final int maxRegion = 16;
		private static final int maxMap    = 6;
		private final HashMap<String, NameDesc[][]> data;
		
		private RegionNames() {
			data = new HashMap<>();
		}

		public String getName(MapIndex mapIndex)
		{
			Supplier<String> getDefaultIfNoName = ()->String.format("No Name for \"%s\"", mapIndex.originalMapID);
			Supplier<String> getDefaultIfNotMapOrRegion  = getDefaultIfNoName;
			if (mapIndex.isMap())
			{
				getDefaultIfNoName          = ()->String.format("No Name for Map (%s,%02d,%02d)", mapIndex.country, mapIndex.region, mapIndex.map);
				getDefaultIfNotMapOrRegion  = ()->String.format("Unknown Map \"%s\"", mapIndex.originalMapID);
			}
			else if (mapIndex.isRegion())
			{
				getDefaultIfNoName          = ()->String.format("No Name for Region (%s,%02d)", mapIndex.country, mapIndex.region);
				getDefaultIfNotMapOrRegion  = ()->String.format("Unknown Region \"%s\"", mapIndex.originalMapID);
			}
			return getName( mapIndex, getDefaultIfNoName, getDefaultIfNotMapOrRegion );
		}

		public String getName(
				MapIndex mapIndex,
				Supplier<String> getDefault)
		{
			return getName(mapIndex, getDefault, getDefault);
		}

		public String getName(
				MapIndex mapIndex,
				Supplier<String> getDefaultIfNoName,
				Supplier<String> getDefaultIfNotMapOrRegion)
		{
			if (mapIndex.isMap() || mapIndex.isRegion())
			{
				String name = getName(mapIndex.country, mapIndex.region, mapIndex.map);
				if (name != null) return name;
				return getDefaultIfNoName.get();
			}

			return getDefaultIfNotMapOrRegion.get();
		}

		public String getName(String country, int regionIndex, Integer mapIndex)
		{
			NameDesc[][] regions = data.get(country);
			if (regions == null) return null;
			
			if (regionIndex<1 || regionIndex>regions.length) return null;
			NameDesc[] maps = regions[regionIndex-1];
			
			if (maps==null) return null;
			NameDesc region = maps.length==0 ? null : maps[0];
			
			NameDesc map = null;
			if (mapIndex != null)
			{
				if (mapIndex<1 || mapIndex>=maps.length) return null;
				map = maps[mapIndex];
			}
			
			String regionStr = region!=null && region.name!=null ? region.name : null;
			String mapStr    = map   !=null && map   .name!=null ? map   .name : null;
			
			if (regionStr==null && mapStr==null) return null;
			if (regionStr==null) return mapStr;
			if (mapStr   ==null) return regionStr;
			return mapStr+" / "+regionStr;
		}

		private void scanRegionNames(Language language, boolean verbose)
		{
			for (String country : MapIndex.countries) {
				if (verbose) System.out.printf("Country: %s%n", country);
				
				NameDesc[][] names = new NameDesc[maxRegion][maxMap+1];
				for (int i=0; i<maxRegion; i++) Arrays.fill(names[i], null);
				data.put(country, names);
				
				for (int region=1; region<=maxRegion; region++) {
					String regionName_ID = String.format("%s_%02d", country, region);
					String regionName    = null;
					
					if (regionName==null) regionName = language.dictionary.get(regionName_ID);
					if (regionName==null) regionName = language.dictionary.get(regionName_ID.toLowerCase());
					
					if (regionName!=null)
					{
						names[region-1][0] = new NameDesc(regionName_ID, regionName, null, null);
						
						if (verbose)
							System.out.printf("   Region[%d]:  %s%n", region, names[region-1][0]);
						//	System.out.printf("   Region[%d]:  %s  <%s>%n", region, regionName, regionName_ID);
					}
					else
						if (verbose) System.out.printf("   Region[%d] - Name not found%n", region);
					
					for (int map=1; map<=maxMap; map++) {
						String mapName_ID = null;
						String mapName = null;
						String mapDesc_ID = null;
						String mapDesc = null;
						
						if (mapName==null) {
							mapName_ID = String.format("%s_%02d_%02d_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase();
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase()+"_NAME";
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("LEVEL_%s_%02d_%02d_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						if (mapName==null) {
							mapName_ID = String.format("%s_%02d_%02d_NEW_NAME", country, region, map);
							mapName = language.dictionary.get(mapName_ID);
						}
						
						if (mapDesc==null) {
							mapDesc_ID = String.format("%s_%02d_%02d_DESC", country, region, map);
							mapDesc = language.dictionary.get(mapDesc_ID);
						}
						if (mapDesc==null) {
							mapDesc_ID = String.format("LEVEL_%s_%02d_%02d", country, region, map).toLowerCase()+"_DESC";
							mapDesc = language.dictionary.get(mapDesc_ID);
						}
						if (mapDesc==null) {
							mapDesc_ID = String.format("LEVEL_%s_%02d_%02d_DESC", country, region, map);
							mapDesc = language.dictionary.get(mapDesc_ID);
						}
						if (mapDesc==null) {
							mapDesc_ID = String.format("%s_%02d_%02d_NEW_DESC", country, region, map);
							mapDesc = language.dictionary.get(mapDesc_ID);
						}
						
						if (mapName!=null || mapDesc!=null)
						{
							if (mapName==null) mapName_ID = null;
							if (mapDesc==null) mapDesc_ID = null;
							names[region-1][map] = new NameDesc(mapName_ID, mapName, mapDesc_ID, mapDesc);
							
							if (verbose)
								System.out.printf("      Map[%d,%d]:  %s%n", region, map, names[region-1][map]);
							//	System.out.printf("      Map[%d,%d]:  %s  <%s>%n", region, map, mapName, mapName_ID);
						}
					}
				}
			}
		}
	
		public record NameDesc(String nameID, String name, String descID, String desc)
		{
			@Override public String toString()
			{
				StringBuilder sb = new StringBuilder();
				if (name!=null || nameID!=null)
					sb.append(name+" <"+nameID+">");
				
				if (desc!=null || descID!=null)
				{
					if (!sb.isEmpty())
						sb.append(", ");
					String trimmedDesc = desc==null ? null : desc.length()<30 ? desc : desc.substring(0, 30)+"...";
					sb.append(trimmedDesc+" <"+descID+">");
				}
				
				return sb.toString();
			}
		}
		
	}

	public static <V> Map<V, Integer> countDistinctValues(V[] array)
	{
		if (array==null) return null;
		if (array.length==0) return null;
		Map<V,Integer> map = new HashMap<>();
		for (V value : array)
		{
			Integer n = map.get(value);
			if (n==null) map.put(value,1);
			else         map.put(value,n+1);
		}
		return map;
	}

	private static Integer add(Integer value1, Integer value2)
	{
		if (value1==null) return value2;
		if (value2==null) return value1;
		return value1 + value2;
	}

	private static Integer parseInt(String str) {
		if (str==null) return null;
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private static Float parseFloat(String str) {
		if (str==null) return null;
		try {
			float f = Float.parseFloat(str);
			if (!Float.isNaN(f)) return f;
		} catch (NumberFormatException e) {}
		return null;
	}

	private static Boolean parseBool(String str) {
		if (str==null) return null;
		if ("true" .equalsIgnoreCase(str)) return true;
		if ("false".equalsIgnoreCase(str)) return false;
		return null;
	}

	private static boolean parseBool(String str, boolean defaultValue) {
		if (str==null) return defaultValue;
		if ("true" .equalsIgnoreCase(str)) return true;
		if ("false".equalsIgnoreCase(str)) return false;
		return defaultValue;
	}

	private static String getAttribute(GenericXmlNode[] nodes, String key) {
		if (key==null) throw new IllegalArgumentException();
		if (nodes==null) throw new IllegalArgumentException();
		for (GenericXmlNode node : nodes) {
			String value = node.attributes.get(key);
			if (value!=null) return value;
		}
		return null;
	}
	private static String getAttribute(GenericXmlNode node, String key) {
		if (key==null) throw new IllegalArgumentException();
		if (node==null) return null;
		return node.attributes.get(key);
	}

	private static String[] splitColonSeparatedIDList(String value) {
		if (value==null) return new String[0];
		String[] strs = value.split(",");
		for (int i=0; i<strs.length; i++)
			strs[i] = strs[i].trim();
		return strs;
	}
	
	public static boolean contains(String[] strs, String str)
	{
		for (String str1 : strs)
		{
			if (str1==null && str==null)
				return true;
			if (str1!=null && str1.equals(str))
				return true;
		}
		return false;
	}

	public static <E extends Enum<E>> E parseEnum(String str, String enumLabel, E[] values) {
		if (str==null) return null;
		for (E e : values)
			if (e.toString().equals(str))
				return e;
		unexpectedValues.add(String.format("New Value for Enum <%s>", enumLabel), str);
		return null;
	}

	public static <E extends Enum<E>> EnumSet<E> parseEnumSet(String[] strs, String enumLabel, E[] values) {
		if (strs==null) return null;
		List<E> foundValues = Arrays
			.stream(strs)
			.map(str -> parseEnum(str, enumLabel, values))
			.filter(str -> str!=null)
			.toList();
		return EnumSet.copyOf(foundValues);
	}

	private static Map<String, GenericXmlNode> getGameRegionSubNodes(GenericXmlNode node)
	{
		if (node == null)
			return null;
		
		Map<String,GenericXmlNode> regions = new HashMap<>();
		node.nodes.forEachPair((name,subnode) -> {
			if (subnode.nodeName.startsWith("region:"))
				regions.put(
						subnode.nodeName.substring("region:".length()),
						subnode
				);
		});
		
		return regions;
	}

	private static <K,V> Map<K,V> parseMap(Map<K, GenericXmlNode> nodes, BiFunction<K, GenericXmlNode,V> createV)
	{
		if (nodes == null)
			return null;
		
		HashMap<K, V> result = new HashMap<>();
		nodes.forEach((key,node) -> result.put( key, createV.apply(key,node) ));
		
		return result;
	}
	
	public interface BooleanWithText
	{
		boolean getBool();
		String getText();
	}
	
	public static class Capability implements BooleanWithText
	{
		public boolean isCapable;
		public boolean byTruck;
		public boolean byTrailer;

		Capability(boolean isCapable, boolean byTruck, boolean byTrailer)
		{
			super();
			this.isCapable = isCapable;
			this.byTruck = byTruck;
			this.byTrailer = byTrailer;
		}
		@Override public String toString()
		{
			return String.format("%s, %s", getBool(), getText());
		}

		@Override public boolean getBool() { return isCapable; }
		@Override public String  getText() { return getText(", "); }
		
		private String getText(String glue)
		{
			if (byTruck && byTrailer)
				return "Truck"+glue+"Trailer";
			if (byTruck)
				return "Truck";
			if (byTrailer)
				return "Trailer";
			return "";
		}
	}

	public Capability isCapable(Truck truck, SpecialTruckAddons.AddonCategory listID, SpecialTruckAddons specialTruckAddons, TextOutput textOutput) {
		SpecialTruckAddons.SpecialTruckAddonList addonList = specialTruckAddons.getList(listID);
		if (textOutput!=null) textOutput.printf("Is Truck <%s> capable of %s?%n", truck.id, listID.label);
		
		switch (listID.type) {
			case Addon:
				for (Vector<TruckAddon> list : truck.compatibleTruckAddons.values())
					for (TruckAddon addon : list)
						if (addonList.contains(addon)) {
							if (textOutput!=null)
								textOutput.printf("   Yes. Found addon <%s>%n", addon.id);
							return new Capability(true, true, false);
						}
				if (textOutput!=null)
					textOutput.printf("   No. Can't find any needed addon (%s) in trucks compatible addon list%n", addonList.toString());
				return new Capability(false, false, false);
			
			case LoadAreaCargo:
				Capability result = addonList.forEachAddon(
					
					()->null,
					
					(r1, r2) -> {
						if (r1==null && r2==null) return null;
						if (r1==null) return r2;
						if (r2==null) return r1;
						return new Capability(
							r1.isCapable || r2.isCapable,
							r1.byTruck   || r2.byTruck,
							r1.byTrailer || r2.byTrailer
						);
					},
					
					r -> false, //r!=null && r.booleanValue(),
					
					id -> determineLoadAreaCargoCapability(truck, id, textOutput)
				);
				if (textOutput!=null)
				{
					if (result==null)
						textOutput.printf("   Result: Cant' decide%n");
					else
						textOutput.printf("   Result: %s (by %s)%n", result.isCapable ? "YES" : "NO", result.getText(" and "));
				}
				return result;
		}
		return null;
	}

	private Capability determineLoadAreaCargoCapability(Truck truck, String id, TextOutput textOutput)
	{
		TruckAddon addon = truckAddons.get(id);
		if (addon==null) {
			if (textOutput!=null) textOutput.printf("   Unknown TruckAddon <%s>%n", id);
			return null;
		}
		if (textOutput!=null) {
			String type = addon.gameData.isCargo ? "Cargo" : "Addon";
			textOutput.printf("   Test <%s> [%s]%n", id, type);
		}
		
		if (!findRequiredAddons(addon.gameData.requiredAddons, truck.compatibleTruckAddons, truck.compatibleTrailers)) {
			if (textOutput!=null) {
				textOutput.printf("      Can't find required addons in list of compatible addons and trailers.%n", id);
				textOutput.printf("         required addons: %s%n", SnowRunner.joinRequiredAddonsToString_OneLine(addon.gameData.requiredAddons));
			}
			return new Capability(false, false, false);
		}
		
		// cargoType & cargoAddonSubtype ??
		if (addon.gameData.cargoAddonSubtype==null) {
			if (textOutput!=null) textOutput.printf("      This addon has no CargoAddonSubtype.%n", id);
			return null;
		}
		if (textOutput!=null) textOutput.printf("      CargoAddonSubtype: \"%s\"%n", addon.gameData.cargoAddonSubtype);
		
		Predicate<ItemBasedNonTruck> predicate = item -> hasItemACompatibleLoadArea(item, addon.gameData.cargoType, addon.gameData.cargoAddonSubtype, false);
		TruckAddon truckAddon = findOne(truck.compatibleTruckAddons, predicate);
		Trailer    trailer    = findOne(truck.compatibleTrailers   , predicate);
		
		Capability result = new Capability(false,false,false);
		if (truckAddon!=null)
		{
			result.isCapable = true;
			result.byTruck   = true;
		}
		
		if (trailer!=null)
		{
			result.isCapable = true;
			result.byTrailer = true;
		}
		
		if (textOutput!=null)
		{
			if (truckAddon!=null)
				textOutput.printf("      Found addon <%s> for CargoAddonSubtype \"%s\" truck's list of compatible addons.%n", truckAddon.id, addon.gameData.cargoAddonSubtype);
			else
				textOutput.printf("      No addon for CargoAddonSubtype \"%s\" found truck's list of compatible addons.%n", addon.gameData.cargoAddonSubtype);
			if (trailer!=null)
				textOutput.printf("      Found trailer <%s> for CargoAddonSubtype \"%s\" truck's list of compatible trailers.%n", trailer.id, addon.gameData.cargoAddonSubtype);
			else
				textOutput.printf("      No trailer for CargoAddonSubtype \"%s\" found truck's list of compatible trailers.%n", addon.gameData.cargoAddonSubtype);
		}
		
		return result;
	}

	public static boolean hasItemACompatibleLoadArea(GameData.GameDataT3NonTruckContainer item, String requiredCargoType, String requiredCargoAddonSubtype, boolean ignoreTrailerLoad)
	{
		GameData.GameDataT3NonTruck gameData = item.getGameData();
		if (gameData == null) throw new IllegalStateException();
		
		for (GameData.GameDataT3NonTruck.LoadArea loadArea : gameData.loadAreas)
			if (
					(requiredCargoType         == null || requiredCargoType        .equals(loadArea.type   )) && // Cat 745C can only M logs and no S logs
					(requiredCargoAddonSubtype == null || requiredCargoAddonSubtype.equals(loadArea.subtype)) &&
					(ignoreTrailerLoad || (loadArea.trailerLoad != null && loadArea.trailerLoad.booleanValue()))
			)
				return true;
		return false;
	}

	public static boolean hasItemACompatibleLoadArea(GameData.GameDataT3NonTruckContainer item, HashSet<CargoTypePair> cargoTypes, boolean ignoreTrailerLoad)
	{
		for (CargoTypePair ct : cargoTypes)
			if (hasItemACompatibleLoadArea(item, ct.cargoType, ct.cargoAddonSubtype, ignoreTrailerLoad))
				return true;
		return false;
	}
	
	public record CargoTypePair(String cargoType, String cargoAddonSubtype) {}
	
	public HashSet<CargoTypePair> getTruckAddonCargoTypes(Collection<String> truckAddonIDs)
	{
		HashSet<CargoTypePair> set = new HashSet<>();
		for (String id : truckAddonIDs)
		{
			TruckAddon truckAddon = truckAddons.get(id);
			if (truckAddon.gameData.isCargo)
				set.add(new CargoTypePair(
						truckAddon.gameData.cargoType,
						truckAddon.gameData.cargoAddonSubtype
				));
		}
		
		return set;
	}

	public boolean canTruckCombineCompatibleAddons(Truck truck, Predicate<TruckAddon> addonPredicate1, Predicate<TruckAddon> addonPredicate2)
	{
		HashSet<String> socketIDs1 = getSocketIDsOfCompatibleAddons(truck, addonPredicate1);
		HashSet<String> socketIDs2 = getSocketIDsOfCompatibleAddons(truck, addonPredicate2);
		
		if (socketIDs1.isEmpty() || socketIDs2.isEmpty())
			return false;
		
		Vector<FoundSocket> sockets1 = FoundSocket.find(truck, socketIDs1);
		Vector<FoundSocket> sockets2 = FoundSocket.find(truck, socketIDs2);
		
		for (FoundSocket socket1 : sockets1) {
			for (FoundSocket socket2 : sockets2) {
				
				if (socket1.addonSocket == socket2.addonSocket) continue;
				if (socketBIsBlockedBySocketA(socket1, socket2)) continue;
				if (socketBIsBlockedBySocketA(socket2, socket1)) continue;
				
				//      different addonSockets
				// AND  socket1 don't blocks socket2 
				// AND  socket2 don't blocks socket1
				return true;
			}
		}
		
		return false;
	}

	private static HashSet<String> getSocketIDsOfCompatibleAddons(Truck truck, Predicate<TruckAddon> addonPredicate)
	{
		HashSet<String> socketIDs = new HashSet<>();
		for (Vector<TruckAddon> list : truck.compatibleTruckAddons.values())
			for (TruckAddon addon : list)
				if (addonPredicate.test(addon))
					socketIDs.add(addon.gameData.installSocket);
		return socketIDs;
	}

	private static boolean socketBIsBlockedBySocketA(FoundSocket socketA, FoundSocket socketB)
	{
		Vector<String[]> blockCombis = socketB.socket.isBlockedBy.get(socketB.socketID);
		if (blockCombis!=null)
			for (String[] blockCombi : blockCombis)
				if (blockCombi.length==1 && contains(blockCombi, socketA.socketID))
					return true;
		
		return false;
	}
	
	private record FoundSocket(Truck.AddonSockets addonSocket, Truck.AddonSockets.Socket socket, String socketID)
	{
		static Vector<FoundSocket> find(Truck truck, Collection<String> socketIDs)
		{
			Vector<FoundSocket> foundSockets = new Vector<>();
			for (Truck.AddonSockets addonSocket : truck.addonSockets)
				for (Truck.AddonSockets.Socket socket : addonSocket.sockets)
					for (String socketID : socketIDs)
						if (contains(socket.socketIDs, socketID))
							foundSockets.add( new FoundSocket(addonSocket, socket, socketID) );
			return foundSockets;
		}
	}
	
	private static void scanNode(GenericXmlNode node, String prefix, String nodeName)
	{
		if (node!=null) {
			node.attributes.forEach((key,value)->{
				unexpectedValues.add(String.format("%s <%s ####=\"...\">", prefix, nodeName), key);
			});
			node.nodes.keySet().forEach(key->{
				unexpectedValues.add(String.format("%s <%s> <####>", prefix, nodeName), key);
			});
		}
	}
	
	public static class EnumSetContainer<E extends Enum<E>>
	{
		private final EnumSet<E> set;
		EnumSetContainer(Class<E> enumClass) {
			set = EnumSet.noneOf(enumClass);
		}
		public EnumSet<E> getSet() { return set; }
		@Override public String toString() { return set.toString(); }
		public static <V extends EnumSetContainer<E>, E extends Enum<E>> V create(Supplier<V> createV, Collection<E> values)
		{
			if (values==null) return null;
			V set = createV.get();
			set.getSet().addAll(values);
			return set;
		}
	}

	public interface HasNameAndID {
		String getName_StringID();
		String getID();
	}

	static class ItemBased {
		public final String id;
		public final String xmlName;
		public final String updateLevel;
		ItemBased(Item item) {
			id      = item.name;
			xmlName = item.name+".xml";
			updateLevel = item.updateLevel;
		}
	}

	public static class UiDesc {
		/*
	    <UiDesc ####="...">
	      TruckImage
	      UiDesc
	      UiIcon100x100
	      UiIcon20x20
	      UiIcon30x30
	      UiIcon328x458
	      UiIcon40x40
	      UiIconLogo
	      UiName
		 */
		
		public final String description_StringID;
		public final String name_StringID;
		public final String truckImage ;
		public final String icon100x100;
		public final String icon20x20  ;
		public final String icon30x30  ;
		public final String icon328x458;
		public final String icon40x40  ;
		public final String iconLogo   ;
		
		UiDesc(GenericXmlNode uiDescNode, String debugOutputPrefix, String nodeName) {
			//scanNode(uiDescNode, debugOutputPrefix, nodeName);
			//scanNode(uiDescNode, "[General]", "UiDesc");
			
			description_StringID = getAttribute(uiDescNode, "UiDesc");
			name_StringID        = getAttribute(uiDescNode, "UiName");
			
			truckImage  = getAttribute(uiDescNode, "TruckImage"   );
			icon100x100 = getAttribute(uiDescNode, "UiIcon100x100");
			icon20x20   = getAttribute(uiDescNode, "UiIcon20x20"  );
			icon30x30   = getAttribute(uiDescNode, "UiIcon30x30"  );
			icon328x458 = getAttribute(uiDescNode, "UiIcon328x458");
			icon40x40   = getAttribute(uiDescNode, "UiIcon40x40"  );
			iconLogo    = getAttribute(uiDescNode, "UiIconLogo"   );
		}
	}

	static class CargoType extends ItemBased implements HasNameAndID {
	
		final UiDesc uiDesc;

		public CargoType(Item item) {
			super(item);
			//scanNode(item.content, "[CargoType]", "CargoType");
			
			GenericXmlNode uiDescNode = item.content.getNode("CargoType", "UiDesc");
			uiDesc = new UiDesc(uiDescNode, "[CargoType] <CargoType>", "UiDesc");
		}

		public String getDescription_StringID() { return uiDesc==null ? null : uiDesc.description_StringID; }
		@Override public String getName_StringID() { return uiDesc==null ? null : uiDesc.name_StringID; }
		@Override public String getID() { return id; }
	}

	public static class AddonCategories extends ItemBased {
		static final String NULL_CATEGORY = "#### NULL ####";
		static final String CARGO_CATEGORY = "#### CARGO ####";
	
		public final HashMap<String,Category> categories;
		
		AddonCategories(Item item) {
			super(item);

			GenericXmlNode[] categoryNodes = item.content.getNodes("CategoryList", "Category");
			categories = new HashMap<>();
			for (int i=0; i<categoryNodes.length; i++) {
				Category category = new Category(categoryNodes[i]);
				categories.put(category.name, category);
			}
		}
		
		String getCategoryLabel(String category, Language language) {
			if (category==null || category.equals(NULL_CATEGORY))
				return "<Unknown Category>";
			if (category.equals(CARGO_CATEGORY))
				return "Cargo";
			
			return SnowRunner.solveStringID(categories.get(category), category, language);
		}
		
		static String getCategoryLabel(String category) {
			if (category==null || category.equals(NULL_CATEGORY))
				return "<Unknown Category>";
			if (category.equals(CARGO_CATEGORY))
				return "Cargo";
			
			return category;
		}

		public static String getCategoryLabel(String category, AddonCategories addonCategories, Language language) {
			if (addonCategories!=null)
				return addonCategories.getCategoryLabel(category, language);
			return AddonCategories.getCategoryLabel(category);
		}

		public static class Category implements HasNameAndID {

			public final String name;
			public final boolean requiresOneAddonInstalled;
			public final String icon;
			public final String label_StringID;

			Category(GenericXmlNode node) {
				//node.attributes.forEach((key,value)->{
				//	unexpectedValues.add("Class[addons_category] <CategoryList> <Category ####=\"...\">", key);
				//});
				//   Class[addons_category] <CategoryList> <Category ####="...">
				//      Name
				//      RequiresOneAddonInstalled
				//      UiIcon
				//      UiName
				
				name                      =             node.attributes.get("Name");
				requiresOneAddonInstalled = parseBool ( node.attributes.get("RequiresOneAddonInstalled"), false );
				icon                      =             node.attributes.get("UiIcon");
				label_StringID            =             node.attributes.get("UiName");
			}

			@Override public String getName_StringID() { return label_StringID; }
			@Override public String getID() { return name; }
		}
	
	}
	
	public static class GameData {
		
		public final Integer price;
		public final Boolean unlockByExploration;
		public final Integer unlockByRank;
		public final UiDesc uiDesc;
		public final Map<String, UiDesc> uiDescRegions;
		
		GameData(GenericXmlNode gameDataNode, String debugOutputPrefix) {
			//showAttrsAndSubNodes(gameDataNode, debugOutputPrefix, null);
			
			price               = parseInt (gameDataNode.attributes.get("Price") );
			unlockByExploration = parseBool(gameDataNode.attributes.get("UnlockByExploration") );
			unlockByRank        = parseInt (gameDataNode.attributes.get("UnlockByRank") );
			
			GenericXmlNode uiDescNode = gameDataNode.getNode("GameData", "UiDesc");
			uiDescRegions = parseMap(
					getGameRegionSubNodes(uiDescNode),
					(region,node) -> new UiDesc(node, debugOutputPrefix+" <GameData>", "UiDesc> <region:"+region)
			);
			
			uiDesc = uiDescRegions==null || uiDescRegions.isEmpty()
					? new UiDesc(uiDescNode, debugOutputPrefix+" <GameData>", "UiDesc")
					: uiDescRegions.get("default");
		}
		
		public String getDescriptionStringID()
		{
			return uiDesc==null ? null : uiDesc.description_StringID;
		}

		public String getNameStringID()
		{
			return uiDesc==null ? null : uiDesc.name_StringID;
		}

		@SuppressWarnings("unused")
		private static void showAttr(String debugOutputPrefix, String nodeName, String attrName, String value) {
			if (value==null) return;
			unexpectedValues.add(debugOutputPrefix+" <GameData"+(nodeName==null ? "" : "> <"+nodeName)+" "+attrName+"=\"####\">", value);
		}

		private static void showAttrsAndSubNodes(GenericXmlNode node, String debugOutputPrefix, String nodeName) {
			scanNode(node, debugOutputPrefix, "GameData"+(nodeName==null ? "" : "> <"+nodeName));
		}
		
		@SuppressWarnings("unused")
		private static void showAttrsAndSubNodes(GenericXmlNode[] nodes, String debugOutputPrefix, String nodeName) {
			unexpectedValues.add(debugOutputPrefix+" <GameData"+(nodeName==null ? "" : "> <"+nodeName)+">", String.format("N = %d", nodes.length));
			for (GenericXmlNode node : nodes)
				showAttrsAndSubNodes(node, debugOutputPrefix, nodeName);
		}
	
		public static class GameDataT3 extends GameData {
	
			public final String[] excludedCargoTypes;
			public final Integer cargoSlots;
	
			GameDataT3(GenericXmlNode gameDataNode, String debugOutputPrefix) {
				super(gameDataNode, debugOutputPrefix);
				excludedCargoTypes  = splitColonSeparatedIDList( gameDataNode.attributes.get("ExcludedCargoTypes") );
				
				GenericXmlNode[] addonSlotsNodes = gameDataNode.getNodes("GameData", "AddonSlots");
				Integer sum = null;
				for (GenericXmlNode addonSlotsNode : addonSlotsNodes)
					sum = Data.add(sum, parseInt( getAttribute(addonSlotsNode, "Quantity") ));
				cargoSlots = sum;
				//showAttrsAndSubNodes(addonSlotsNode, "[General]", "AddonSlots");
				//   [General] <GameData> <AddonSlots ####="...">
				//    - InitialOffset
				//    - OffsetStep
				//    - ParentFrames
				//    + Quantity
			}
			
		}
		
		interface GameDataT3NonTruckContainer
		{
			GameDataT3NonTruck getGameData();
		}
		
		public static class GameDataT3NonTruck extends GameDataT3 {
	
			public final String addonType;
			public final String installSocket;
			public final String[][] requiredAddons; // (ra[0][0] || ra[0][1] || ... ) && (ra[1][0] || ra[1][1] || ... ) && ...
			public final LoadArea[] loadAreas;
			public final boolean isCargoCarrier;
	
			GameDataT3NonTruck(GenericXmlNode gameDataNode, String debugOutputPrefix) {
				super(gameDataNode, debugOutputPrefix);
				@SuppressWarnings("unused")
				String str;
				
				GenericXmlNode addonTypeNode = gameDataNode.getNode("GameData", "AddonType");
				addonType = getAttribute(addonTypeNode, "Name");
				//showAttrsAndSubNodes(addonTypeNode, "[General]", "AddonType");
				//   [General] <GameData> <AddonType ####="...">
				//      Name
				
				GenericXmlNode installSocketNode = gameDataNode.getNode("GameData", "InstallSocket");
				installSocket = getAttribute(installSocketNode, "Type");
				//showAttrsAndSubNodes(installSocketNode, "[General]", "InstallSocket");
				//   [General] <GameData> <InstallSocket ####="...">
				//    - CablesPos
				//    - CameraPreset
				//    - Offset
				//    - ParentFrame
				//    ? Price
				//    + Type
				//    ? UnlockByExploration
				//    ? UnlockByRank
				
				GenericXmlNode[] loadAreaNodes = gameDataNode.getNodes("GameData", "LoadArea");
				loadAreas = Arrays.stream(loadAreaNodes).map(LoadArea::new).toArray(LoadArea[]::new);
				isCargoCarrier = loadAreas.length==0 && cargoSlots!=null && cargoSlots>0;
				
				GenericXmlNode[] requiredAddonNodes = gameDataNode.getNodes("GameData", "RequiredAddon");
				requiredAddons = new String[requiredAddonNodes.length][];
				for (int i=0; i<requiredAddonNodes.length; i++) {
					GenericXmlNode requiredAddonNode = requiredAddonNodes[i];
					requiredAddons[i] = splitColonSeparatedIDList( getAttribute(requiredAddonNode, "Types") );
				}
				//showAttrsAndSubNodes(requiredAddonNodes, "[General]", "RequiredAddon");
				//   [General] <GameData> <RequiredAddon ####="...">
				//      Types
				//   [General] <GameData> <RequiredAddon>
				//      N = 0
				//      N = 1
				//      N = 2
			}
			
			public String getLoadAreas()
			{
				return LoadArea.toString(loadAreas);
			}
			
			public static class LoadArea {

				private static final Comparator<String > STRING_NULLS_LAST  = Comparator.nullsLast(Comparator.naturalOrder());
				private static final Comparator<Boolean> BOOLEAN_NULLS_LAST = Comparator.nullsLast(Comparator.naturalOrder());
				private static final Comparator<LoadArea> COMPARATOR = Comparator
						.<LoadArea,String>comparing(la -> la.type       , STRING_NULLS_LAST )
						.thenComparing             (la -> la.subtype    , STRING_NULLS_LAST )
						.thenComparing             (la -> la.trailerLoad, BOOLEAN_NULLS_LAST);
				
				public final String  type;
				public final String  subtype;
				public final Boolean trailerLoad;

				public LoadArea(GenericXmlNode loadAreaNode) {
					type        =            loadAreaNode.attributes.get("Type"       );
					subtype     =            loadAreaNode.attributes.get("Subtype"    );
					trailerLoad = parseBool( loadAreaNode.attributes.get("TrailerLoad") );
					
					//str = loadAreaNode.attributes.get("Max"        ); showAttr("[General]", "LoadArea", "Max"        , str);
					//   [General] <GameData> <LoadArea Max="####">
					//      (-0.178; 2.145; 1.198)
					//      ...
					//str = loadAreaNode.attributes.get("Min"        ); showAttr("[General]", "LoadArea", "Min"        , str);
					//   [General] <GameData> <LoadArea Min="####">
					//      (-0.134; 1.3; -1.23)
					//      ...
					//str = loadAreaNode.attributes.get("ParentFrame"); showAttr("[General]", "LoadArea", "ParentFrame", str);
					//   [General] <GameData> <LoadArea ParentFrame="####">
					//      BoneFront_cdt
					//      ...
					//str = loadAreaNode.attributes.get("Subtype"    ); showAttr("[General]", "LoadArea", "Subtype"    , str);
					//   [General] <GameData> <LoadArea Subtype="####">
					//      Cat745LogBunk
					//      FrameAddonLog
					//      TrailerLogMedium
					//      TrailerPoleLogLong
					//str = loadAreaNode.attributes.get("TrailerLoad"); showAttr("[General]", "LoadArea", "TrailerLoad", str);
					//   [General] <GameData> <LoadArea TrailerLoad="####">
					//      true
					//str = loadAreaNode.attributes.get("Type"       ); showAttr("[General]", "LoadArea", "Type"       , str);
					//   [General] <GameData> <LoadArea Type="####">
					//      CargoLogsLong
					//      CargoLogsMedium
					//      CargoLogsShort
					
					//showAttrsAndSubNodes(loadAreaNodes, "[General]", "LoadArea");
					//   [General] <GameData> <LoadArea ####="...">
					//      Max
					//      Min
					//      ParentFrame
					//      Subtype
					//      TrailerLoad
					//      Type
					//   [General] <GameData> <LoadArea>
					//      N = 0
					//      N = 1
					//      N = 2
				}
				
				
				@Override
				public int hashCode()
				{
					return Objects.hash(type, subtype, trailerLoad);
				}

				@Override
				public boolean equals(Object obj)
				{
					if (this == obj) return true;
					if (!(obj instanceof LoadArea)) return false;
					LoadArea other = (LoadArea) obj;
					return Objects.equals(subtype, other.subtype)
							&& Objects.equals(trailerLoad, other.trailerLoad)
							&& Objects.equals(type, other.type);
				}

				@Override public String toString() {
					String types = String.format("%s|%s", type==null ? "<null>" : type, subtype==null ? "<null>" : subtype);
					if (trailerLoad!=null)
						types += " <"+(trailerLoad ? "" : "No ")+"TrailerLoad>";
					return types;
				}

				public static String toString(LoadArea[] loadAreas)
				{
					Map<LoadArea, Integer> map = countDistinctValues(loadAreas);
					if (map==null) return null;
					return map.keySet()
						.stream()
						.sorted(COMPARATOR)
						.map(la -> "%s %dx".formatted( la, map.get(la) ))
						.collect(Collectors.joining(", ", "[ ", " ]"));
				}
			}
		}
		
		public static class GameDataTruck extends GameDataT3 {
	
			public final Truck.CountrySet country;
			public final String[] excludeAddons;
			public final Boolean recallable_obsolete;
			public final String unlockByObjective;
			public final boolean isCargoCarrier;

			GameDataTruck(GenericXmlNode gameDataNode, String debugOutputPrefix) {
				super(gameDataNode, debugOutputPrefix);
				country       = Truck.CountrySet.create( parseEnumSet( splitColonSeparatedIDList( gameDataNode.attributes.get("Country") ), "Country", Truck.Country.values()) );
				excludeAddons = splitColonSeparatedIDList( gameDataNode.attributes.get("ExcludeAddons") );
				recallable_obsolete    = parseBool                ( gameDataNode.attributes.get("Recallable") );
				unlockByObjective = gameDataNode.attributes.get("UnlockByObjective");
				//showAttr("[General]", null, "ExcludeAddons", excludeAddons);
				//   [General] <GameData ExcludeAddons="####">
				//      ford_clt9000_top_fender, semitrailer_m747
				//      frame_addon_flatbed_2
				//      frame_addon_sideboard_2
				//      ...
				//showAttr("[General]", null, "Recallable"   , recallable);
				//   [General] <GameData Recallable="####">
				//      true
				isCargoCarrier = cargoSlots!=null && cargoSlots>0;
			}
		}
		
		public static class GameDataTrailer extends GameDataT3NonTruck {
	
			public final Boolean isQuestItem;

			GameDataTrailer(GenericXmlNode gameDataNode, String debugOutputPrefix) {
				super(gameDataNode, debugOutputPrefix);
				isQuestItem = parseBool( gameDataNode.attributes.get("IsQuest") );
			}
		}
		
		public static class GameDataTruckAddon extends GameDataT3NonTruck {
	
			public final String  category;
			public final boolean isCargo;
			public final Integer cargoLength;
			public final String  cargoType;
			public final String  cargoAddonSubtype;
			public final Integer cargoValue_obsolete;
			public final String  cargoName_StringID;
			public final String  cargoDescription_StringID;
			public final Integer garagePoints_obsolete;
			public final Boolean isCustomizable_obsolete;
			public final Integer loadPoints;
			public final Integer manualLoads;
			public final Integer manualLoads_IS;
			public final String  originalAddon;
			public final String  saddleType_obsolete;
			public final Boolean showPackingStoppers_obsolete;
			public final Integer trialsToUnlock;
			public final Boolean unpackOnTrailerDetach;
			public final Integer wheelToPack_obsolete;
			public final String  requiredAddonType;
			public final String  requiredAddonType_StringID;

			GameDataTruckAddon(GenericXmlNode gameDataNode, String debugOutputPrefix, HashMap<String, CargoType> cargoTypes) {
				super(gameDataNode, debugOutputPrefix);
				@SuppressWarnings("unused")
				String str;
				
				category                     =            gameDataNode.attributes.get("Category"             );
				garagePoints_obsolete        = parseInt ( gameDataNode.attributes.get("GaragePoints"         ) );
				isCustomizable_obsolete      = parseBool( gameDataNode.attributes.get("IsCustomizable"       ) );
				loadPoints                   = parseInt ( gameDataNode.attributes.get("LoadPoints"           ) );
				manualLoads                  = parseInt ( gameDataNode.attributes.get("ManualLoads"          ) );
				originalAddon                =            gameDataNode.attributes.get("OriginalAddon"        );
				saddleType_obsolete          =            gameDataNode.attributes.get("SaddleType"           );
				showPackingStoppers_obsolete = parseBool( gameDataNode.attributes.get("ShowPackingStoppers"  ) );
				trialsToUnlock               = parseInt ( gameDataNode.attributes.get("TrialsToUnlock"       ) );
				unpackOnTrailerDetach        = parseBool( gameDataNode.attributes.get("UnpackOnTrailerDetach") );
				wheelToPack_obsolete         = parseInt ( gameDataNode.attributes.get("WheelToPack"          ) );
				
				//str = gameDataNode.attributes.get("GaragePoints"         ); showAttr("[General]", null, "GaragePoints"         , str);
				//   [General] <GameData GaragePoints="####">
				//      2
				//str = gameDataNode.attributes.get("IsCustomizable"       ); showAttr("[General]", null, "IsCustomizable"       , str);
				//   [General] <GameData IsCustomizable="####">
				//      false
				//      true
				//str = gameDataNode.attributes.get("LoadPoints"           ); showAttr("[General]", null, "LoadPoints"           , str);
				//   [General] <GameData LoadPoints="####">
				//      6
				//str = gameDataNode.attributes.get("ManualLoads"          ); showAttr("[General]", null, "ManualLoads"          , str);
				//   [General] <GameData ManualLoads="####">
				//      2
				//str = gameDataNode.attributes.get("OriginalAddon"        ); showAttr("[General]", null, "OriginalAddon"        , str);
				//   [General] <GameData OriginalAddon="####">
				//      big_crane_ru
				//      big_crane_us
				//      crane_loglift
				//      frame_addon_flatbed_2
				//      ...
				//str = gameDataNode.attributes.get("SaddleType"           ); showAttr("[General]", null, "SaddleType"           , str);
				//   [General] <GameData SaddleType="####">
				//      low
				//str = gameDataNode.attributes.get("ShowPackingStoppers"  ); showAttr("[General]", null, "ShowPackingStoppers"  , str);
				//   [General] <GameData ShowPackingStoppers="####">
				//      false
				//str = gameDataNode.attributes.get("TrialsToUnlock"       ); showAttr("[General]", null, "TrialsToUnlock"       , str);
				//   [General] <GameData TrialsToUnlock="####">
				//      4
				//str = gameDataNode.attributes.get("UnpackOnTrailerDetach"); showAttr("[General]", null, "UnpackOnTrailerDetach", str);
				//   [General] <GameData UnpackOnTrailerDetach="####">
				//      True
				//str = gameDataNode.attributes.get("WheelToPack"          ); showAttr("[General]", null, "WheelToPack"          , str);
				//   [General] <GameData WheelToPack="####">
				//      2
				
				// <TruckAddon> <GameData SaddleType=\"...\"> besser ignorieren, scheint wohl veraltet, ist jedenfalls mindestens einmal falsch
				// <TruckAddon> <GameData LoadPoints=\"...\">  --> für Stämme -> erstmal ignorieren
				// <TruckAddon> <GameData ManualLoads=\"...\">  --> für Stämme -> erstmal ignorieren
				// <TruckAddon> <GameData GaragePoints=\"...\">  --> Keine Ahnung
				//    \[media]\_dlc\dlc_1_1\classes\trucks\addons\frame_addon_sideboard_1.xml
				//    \[media]\classes\trucks\addons\international_loadstar_1700_pickup.xml
				//    \[media]\classes\trucks\addons\international_loadstar_1700_service_body.xml
				// <TruckAddon> <GameData OriginalAddon=\"...\"> --> verwendet bei abgewandelten AddOns --> im Spiel testen
				//    \[media]\classes\trucks\addons\big_crane_us_ws4964.xml
				
				GenericXmlNode installSlotNode = gameDataNode.getNode("GameData", "InstallSlot"); // Cargo-Addon
				isCargo             = installSlotNode!=null;
				cargoLength         = parseInt ( getAttribute(installSlotNode, "CargoLength") );
				cargoType           =            getAttribute(installSlotNode, "CargoType"  );
				cargoValue_obsolete = parseInt ( getAttribute(installSlotNode, "CargoValue" ) );
				CargoType ct = null;
				if (cargoType!=null)
					ct = cargoTypes.get(cargoType);
				if (ct!=null) {
					cargoName_StringID = ct.getName_StringID();
					cargoDescription_StringID = ct.getDescription_StringID();
				} else {
					cargoName_StringID = null;
					cargoDescription_StringID = null;
				}
				cargoAddonSubtype =            getAttribute(installSlotNode, "CargoAddonSubtype"  );
				manualLoads_IS    = parseInt ( getAttribute(installSlotNode, "ManualLoads" ) );
				//str = getAttribute(installSlotNode, "CargoAddonSubtype"); showAttr("[General]", "InstallSlot", "CargoAddonSubtype", str);
				//   [General] <GameData> <InstallSlot CargoAddonSubtype="####">
				//      Cat745LogBunk
				//      FrameAddonLog
				//      TrailerLogMedium
				//      TrailerPoleLogLong
				//str = getAttribute(installSlotNode, "ManualLoads"      ); showAttr("[General]", "InstallSlot", "ManualLoads"      , str);
				//   [General] <GameData> <InstallSlot ManualLoads="####">
				//      3
				//showAttrsAndSubNodes(installSlotNode, "[General]", "InstallSlot");
				//   [General] <GameData> <InstallSlot ####="...">
				//      CargoAddonSubtype
				//    + CargoLength
				//    + CargoType
				//    + CargoValue
				//      ManualLoads
				//    - Offset
				
				GenericXmlNode requiredAddonTypeNode = gameDataNode.getNode("GameData", "RequiredAddonType");
				requiredAddonType          = getAttribute(requiredAddonTypeNode, "Type"  );
				requiredAddonType_StringID = getAttribute(requiredAddonTypeNode, "TypeUiName"  );
				//str = getAttribute(requiredAddonTypeNode, "Type"      ); showAttr("[General]", "RequiredAddonType", "Type"      , str);
				//   [General] <GameData> <RequiredAddonType Type="####">
				//      SaddleHigh
				//      SaddleLow
				//str = getAttribute(requiredAddonTypeNode, "TypeUiName"); showAttr("[General]", "RequiredAddonType", "TypeUiName", str);
				//   [General] <GameData> <RequiredAddonType TypeUiName="####">
				//      UI_REQUIRED_ADDON_TYPE_SADDLE_HIGH
				//      UI_REQUIRED_ADDON_TYPE_SADDLE_LOW
				//showAttrsAndSubNodes(requiredAddonTypeNodes, "[General]", "RequiredAddonType");
				//   [General] <GameData> <RequiredAddonType ####="...">
				//      Type
				//      TypeUiName
				//   [General] <GameData> <RequiredAddonType>
				//      N = 0
				//      N = 1
				
				//GenericXmlNode spawnLoadOriginNode = gameDataNode.getNode("GameData", "SpawnLoadOrigin");
				//str = getAttribute(spawnLoadOriginNode, "Position"); showAttr("[General]", "SpawnLoadOrigin", "Position", str);
				//   [General] <GameData> <SpawnLoadOrigin Position="####">
				//      (-.4; 0; 0)
				//showAttrsAndSubNodes(spawnLoadOriginNodes, "[General]", "SpawnLoadOrigin");
				//   [General] <GameData> <SpawnLoadOrigin ####="...">
				//      Position
				//   [General] <GameData> <SpawnLoadOrigin>
				//      N = 0
				//      N = 1
			}
		}
	}
	
	public static class TruckComponentSets<Instance extends TruckComponent>
	{
		private final HashMap<String,HashMap<String,Instance>> data = new HashMap<>();
	
		private void addInstance(Instance instance, String instanceNodeName)
		{
			if (instance      ==null) throw new IllegalArgumentException();
			if (instance.setID==null) throw new IllegalStateException();
			if (instance.id   ==null) throw new IllegalStateException();
			
			HashMap<String, Instance> set = data.get(instance.setID);
			if (set==null) data.put(instance.setID, set = new HashMap<>());
			if (set.containsKey(instance.id))
			{
				System.err.printf("Found more than one %s instances with the same ID \"%s\" in set \"%s\" --> subsequent instances will be ignored%n", instanceNodeName, instance.id, instance.setID);
				Instance other = set.get(instance.id);
				System.err.printf("   stored instance: [%s]%n", other   .getName_StringID());
				System.err.printf("   new    instance: [%s]%n", instance.getName_StringID());
			}
			else
				set.put(instance.id, instance);
		}
		
		public Instance findInstance(String instanceID)
		{
			String[] setIDs = data.keySet().toArray(String[]::new);
			return getInstance(instanceID, setIDs, false);
		}
		
		private Instance getInstance(String instanceID, String[] setIDs)
		{
			return getInstance(instanceID, setIDs, true);
		}
	
		private Instance getInstance(String instanceID, String[] setIDs, boolean exceptionIfNotFound)
		{
			for (String setID : setIDs)
			{
				HashMap<String, Instance> set = data.get(setID);
				if (set==null) throw new IllegalStateException();
				Instance instance = set.get(instanceID);
				if (instance!=null)
					return instance;
			}
			if (exceptionIfNotFound)
				throw new IllegalStateException();
			else
				return null;
		}
	
		private Collection<Instance> getInstancesFromSets(String[] setIDs)
		{
			Vector<Instance> allInstances = new Vector<>();
			if (setIDs!=null)
				for (String setID : setIDs) {
					HashMap<String, Instance> set = data.get(setID);
					if (set!=null)
						allInstances.addAll(set.values());
				}
			return allInstances;
		}
	
		public Collection<Instance> getAllInstances()
		{
			Vector<Instance> allInstances = new Vector<>();
			for (HashMap<String, Instance> set : data.values())
				allInstances.addAll(set.values());
			return allInstances;
		}
	}

	public static class TruckComponent implements HasNameAndID {
		
		public final String setID;
		public final String id;
		public final Vector<Truck> usableBy;
		protected final GenericXmlNode gameDataNode;
		public final GameData gameData;
		
		protected TruckComponent(String setID, GenericXmlNode node, String instanceNodeName) {
			this.setID = setID;
			usableBy = new Vector<>();
			id = node.attributes.get("Name");
			
			gameDataNode = node.getNode(instanceNodeName, "GameData");
			gameData = new GameData(gameDataNode,"[TruckComponent] <"+instanceNodeName+">");
		}

		@Override public String getName_StringID() { return gameData.getNameStringID(); }
		@Override public String getID() { return id; }

		void addUsingTruck(Truck truck) {
			usableBy.add(truck);
		}

		static <InstanceType extends TruckComponent> void parseSet(
				XMLTemplateStructure rawdata, String className,
				TruckComponentSets<InstanceType> sets,
				BiFunction<String,GenericXmlNode,InstanceType> constructor,
				String setNodeName, String instanceNodeName) {
			
			Class_ class_ = rawdata.classes.get(className);
			if (class_ == null) return;
			
			class_.items.forEach((setID,item)->{
				if (!item.content.nodeName.equals(setNodeName))
					return;
				
				item.content.attributes.forEach((key,value)->{
					unexpectedValues.add(String.format("Class[%s] <%s ####=\"...\">", className, setNodeName), key);
				});
				
				GenericXmlNode[] nodes = item.content.getNodes(setNodeName, instanceNodeName);
				for (GenericXmlNode node : nodes) {
					InstanceType instance = constructor.apply(setID,node);
					sets.addInstance(instance, instanceNodeName);
				}
			});
			
		}
		
	}

	public static class Winch extends TruckComponent {

		public final boolean isEngineIgnitionRequired;
		public final Integer length;
		public final Float strengthMult;

		Winch(String setName, GenericXmlNode node) {
			super(setName, node, "Winch");
			
			//node.attributes.forEach((key,value)->{
			//	unexpectedValues.add("[VariantSetInstance] <Winch ####=\"...\">", key);
			//});
			//   [VariantSetInstance] <Winch ####="...">
			//      IsEngineIgnitionRequired
			//      Length
			//      Name
			//      StrengthMult
			
			isEngineIgnitionRequired = parseBool ( node.attributes.get("IsEngineIgnitionRequired"), false );
			length                   = parseInt  ( node.attributes.get("Length") );
			strengthMult             = parseFloat( node.attributes.get("StrengthMult") );
			
			GenericXmlNode winchParamsNode = gameDataNode.getNode("GameData", "WinchParams");
			if (winchParamsNode!=null)
				winchParamsNode.attributes.forEach((key,value)->{
					unexpectedValues.add("[VariantSetInstance] <Winch> <GameData> <WinchParams ####=\"...\">", key);
				});
		}

	}

	public static class Suspension extends TruckComponent {

		public final Integer damageCapacity;
		public final String type_StringID;

		Suspension(String setName, GenericXmlNode node) {
			super(setName, node, "SuspensionSet");
			
			//node.attributes.forEach((key,value)->{
			//	unexpectedValues.add("[VariantSetInstance] <SuspensionSet ####=\"...\">", key);
			//});
			//   [VariantSetInstance] <SuspensionSet ####="...">
			//      BrokenWheelDamageMultiplier
			//      CriticalDamageThreshold
			//      DamageCapacity
			//      DeviationDelta
			//      Name
			//      UiName
			
			damageCapacity = parseInt  ( node.attributes.get("DamageCapacity") );
			type_StringID  =             node.attributes.get("UiName");
		}

	}

	public static class Gearbox extends TruckComponent {

		public final Integer damageCapacity;
		public final Float fuelConsumption;
		public final Float awdConsumptionModifier;
		public final Float idleFuelModifier;
		public final boolean existsHighGear;
		public final boolean existsLowerGear;
		public final boolean existsLowerMinusGear;
		public final boolean existsLowerPlusGear;
		public final boolean isManualLowGear;

		Gearbox(String setName, GenericXmlNode node) {
			super(setName, node, "Gearbox");
			
			//node.attributes.forEach((key,value)->{
			//	unexpectedValues.add("[VariantSetInstance] <Gearbox ####=\"...\">", key);
			//});
			//   [VariantSetInstance] <Gearbox ####="...">
			//      AWDConsumptionModifier
			//      CriticalDamageThreshold
			//      DamageCapacity
			//      DamagedConsumptionModifier
			//      FuelConsumption
			//      IdleFuelModifier
			//      MaxBreakFreq
			//      MinBreakFreq
			//      Name
			damageCapacity         = parseInt  ( node.attributes.get("DamageCapacity") );
			fuelConsumption        = parseFloat( node.attributes.get("FuelConsumption") );
			awdConsumptionModifier = parseFloat( node.attributes.get("AWDConsumptionModifier") );
			idleFuelModifier       = parseFloat( node.attributes.get("IdleFuelModifier") );
			
			GenericXmlNode paramsNode = gameDataNode.getNode("GameData", "GearboxParams");
			//if (paramsNode!=null)
			//	paramsNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("[VariantSetInstance] <Winch> <GameData> <GearboxParams ####=\"...\">", key);
			//	});
			//   [VariantSetInstance] <Winch> <GameData> <GearboxParams ####="...">
			//      IsHighGearExists
			//      IsLowerGearExists
			//      IsLowerMinusGearExists
			//      IsLowerPlusGearExists
			//      IsManualLowGear
			existsHighGear       = parseBool ( getAttribute( paramsNode, "IsHighGearExists"), false );
			existsLowerGear      = parseBool ( getAttribute( paramsNode, "IsLowerGearExists"), false );
			existsLowerMinusGear = parseBool ( getAttribute( paramsNode, "IsLowerMinusGearExists"), false );
			existsLowerPlusGear  = parseBool ( getAttribute( paramsNode, "IsLowerPlusGearExists"), false );
			isManualLowGear      = parseBool ( getAttribute( paramsNode, "IsManualLowGear"), false );
		}

	}

	public static class Engine extends TruckComponent {

		public final Integer damageCapacity;
		public final Float fuelConsumption;
		public final Float brakesDelay;
		public final Integer torque;
		public final Float engineResponsiveness;

		Engine(String setName, GenericXmlNode node) {
			super(setName, node, "Engine");
			
			//node.attributes.forEach((key,value)->{
			//	unexpectedValues.add("[VariantSetInstance] <Engine ####=\"...\">", key);
			//});
			//   [VariantSetInstance] <Engine ####="...">
			//      BrakesDelay
			//      CriticalDamageThreshold
			//      DamageCapacity
			//      DamagedConsumptionModifier
			//      DamagedMaxTorqueMultiplier
			//      DamagedMinTorqueMultiplier
			//      EngineResponsiveness
			//      FuelConsumption
			//      MaxDeltaAngVel
			//      Name
			//      Torque
			damageCapacity       = parseInt  ( node.attributes.get("DamageCapacity") );
			fuelConsumption      = parseFloat( node.attributes.get("FuelConsumption") );
			brakesDelay          = parseFloat( node.attributes.get("BrakesDelay") );
			torque               = parseInt  ( node.attributes.get("Torque") );
			engineResponsiveness = parseFloat( node.attributes.get("EngineResponsiveness") );
		}
		
	}

	public static class WheelsDef extends ItemBased {
	
		private final Vector<TruckTire> truckTires;
	
		private WheelsDef(Item item) {
			super(item);
			
			truckTires = new Vector<>();
			GenericXmlNode[] truckTireNodes = item.content.getNodes("TruckWheels","TruckTires","TruckTire");
			for (int i=0; i<truckTireNodes.length; i++)
				truckTires.add(new TruckTire(truckTireNodes[i], id, i, updateLevel));
		}
		
		public interface ForEachTireAction {
			void accept(int i, TruckTire tire);
		}
		
		public void forEachTire(ForEachTireAction action) {
			for (int i=0; i<truckTires.size(); i++)
				action.accept(i, truckTires.get(i));
		}
	}

	public static class TruckTire {
		
		public static int getTypeOrder(String tireType_StringID) {
			if (tireType_StringID==null) return 0;
			switch (tireType_StringID) {
			case "UI_TIRE_TYPE_HIGHWAY_NAME"   : return 1;
			case "UI_TIRE_TYPE_ALLTERRAIN_NAME": return 2;
			case "UI_TIRE_TYPE_OFFROAD_NAME"   : return 3;
			case "UI_TIRE_TYPE_MUDTIRES_NAME"  : return 4;
			case "UI_TIRE_TYPE_CHAINS_NAME"    : return 5;
			}
			return 0;
		}
	
		public final String id;
		public final String wheelsDefID;
		public final int indexInDef;
		public final String updateLevel;
		
		public final String tireType_StringID;
		public final Float frictionHighway;
		public final Float frictionOffroad;
		public final Float frictionMud;
		public final boolean onIce;
		
		public final GameData gameData;
		public final String tireDefID;
	
		private TruckTire(GenericXmlNode node, String wheelsDefID, int indexInDef, String updateLevel) {
			this.id = String.format("%s|%s[%d]", updateLevel==null ? "----" : updateLevel, wheelsDefID, indexInDef);
			this.wheelsDefID = wheelsDefID;
			this.indexInDef = indexInDef;
			this.updateLevel = updateLevel;
			
			//scanNode(node, "Class[wheels] <TruckWheels> <TruckTires>", "TruckTire");
			/*
			   Class[wheels] <TruckWheels> <TruckTires> <TruckTire ####="...">
			      Mass
			      Mesh
			      Name
			      RearMassScale
			      Width
			   Class[wheels] <TruckWheels> <TruckTires> <TruckTire> <####>
			      GameData
			      WheelFriction
			      WheelSoftness
			      WheelTracks
			 */
			tireDefID = node.attributes.get("Name") ;
			
			GenericXmlNode wheelFrictionNode = node.getNode("TruckTire", "WheelFriction");
			/*
				BodyFriction="2.0"
				BodyFrictionAsphalt="0.9"
				IsIgnoreIce="true"
				SubstanceFriction="1.1"
				UiName="UI_TIRE_TYPE_CHAINS_NAME"
			 */
			
			tireType_StringID =             wheelFrictionNode.attributes.get("UiName");
			frictionHighway   = parseFloat( wheelFrictionNode.attributes.get("BodyFrictionAsphalt") );
			frictionOffroad   = parseFloat( wheelFrictionNode.attributes.get("BodyFriction"       ) );
			frictionMud       = parseFloat( wheelFrictionNode.attributes.get("SubstanceFriction"  ) );
			onIce             = parseBool ( wheelFrictionNode.attributes.get("IsIgnoreIce"), false );
			
			GenericXmlNode gameDataNode = node.getNode("TruckTire", "GameData"     );
			gameData = new GameData(gameDataNode, "<TruckTire>");
		}
	
		void printValues(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "TireDef ID", tireDefID);
			out.add(indentLevel, "TireType [StringID]", "<%s>", tireType_StringID);
			out.add(indentLevel, "Friction (highway)", frictionHighway);
			out.add(indentLevel, "Friction (offroad)", frictionOffroad);
			out.add(indentLevel, "Friction (mud)"    , frictionMud);
			out.add(indentLevel, "OnIce", onIce);
			out.add(indentLevel, "Price"                , gameData.price);
			out.add(indentLevel, "Unlock By Exploration", gameData.unlockByExploration);
			out.add(indentLevel, "Unlock By Rank"       , gameData.unlockByRank);
			out.add(indentLevel, "Name"       , "<%s>", gameData.getNameStringID());
			out.add(indentLevel, "Description", "<%s>", gameData.getDescriptionStringID());
		}
	
	}
	
	public static class Truck extends ItemBased implements HasNameAndID {

		public enum DiffLockType {
			None, Uninstalled, Installed, Always;
			static String toString(DiffLockType diffLockType) { return diffLockType==null ? null : diffLockType.toString(); }
		}
		public enum Country {
			US, RU, CAS, CE, NE;
			static String toString(Country country) {
				return country==null ? null : country.toString();
			}
		}
		public enum TruckType {
			HEAVY, OFFROAD, HEAVY_DUTY, HIGHWAY, SCOUT;
			static String toString(TruckType truckType) { return truckType==null ? null : truckType.toString(); }
		}
		public static class CountrySet extends EnumSetContainer<Country>{
			public CountrySet()
			{
				super(Country.class);
			}
			public static CountrySet create(Collection<Country> values)
			{
				return create(CountrySet::new, values);
			}
		}
		
		public final TruckType type;
		public final String image;
		
		public final Integer fuelCapacity;
		public final DiffLockType diffLockType;
		public final Boolean  isWinchUpgradable;
		public final String   maxWheelRadiusWithoutSuspension;
		
		public final CompatibleWheel[] compatibleWheels;
		public final AddonSockets[] addonSockets;
		public final HashSet<Trailer> compatibleTrailers;
		public final StringVectorMap<TruckAddon> compatibleTruckAddons;
		
		public final String   defaultEngine_ItemID;
		public final Engine   defaultEngine;
		public final String[] compatibleEngines_SetIDs;
		public final Collection<Engine> compatibleEngines;
		
		public final String   defaultGearbox_ItemID;
		public final Gearbox  defaultGearbox;
		public final String[] compatibleGearboxes_SetIDs;
		public final Collection<Gearbox> compatibleGearboxes;
		
		public final String     defaultSuspension_ItemID;
		public final Suspension defaultSuspension;
		public final String[]   compatibleSuspensions_SetIDs;
		public final Collection<Suspension> compatibleSuspensions;
		
		public final String   defaultWinch_ItemID;
		public final Winch    defaultWinch;
		public final String[] compatibleWinches_SetIDs;
		public final Collection<Winch> compatibleWinches;
		
		public final HashSet<String> defaultAddonIDs;
		public final Vector<TruckAddon> defaultAddons;

		public boolean hasCompatibleAWD;
		public boolean hasCompatibleDiffLock;
		public final boolean hasCompatibleAutomaticWinch;

		public TruckAddon defaultAWD;
		public TruckAddon defaultDiffLock;
		
		public final GameData.GameDataTruck gameData;
		
		private Truck(Item item, Data data) {
			super(item);
			if (!item.className.equals("trucks"))
				throw new IllegalStateException();
			
			hasCompatibleDiffLock = false;
			hasCompatibleAWD = false;
			defaultDiffLock = null;
			defaultAWD = null;
			
			GenericXmlNode truckDataNode = item.content.getNode("Truck", "TruckData");
			//truckDataNode.attributes.forEach((key,value)->{
			//	unexpectedValues.add("Class[trucks] <Truck> <TruckData ####=\"...\">", key);
			//});
			//   Class[trucks] <Truck> <TruckData ####="...">
			//      BackSteerSpeed
			//      DiffLockType
			//      EngineIconMesh
			//      EngineIconScale
			//      EngineMarkerOffset
			//      EngineStartDelay
			//      ExhaustStartTime
			//      FuelCapacity
			//      FueltankMarkerOffset
			//      Responsiveness
			//      SetHandbrakeOnWinchRelease  // new
			//      SteerSpeed
			//      SuspensionMarkerOffset
			//      TruckImage
			//      TruckType
			//      WheelToPack  // removed
			type         = parseEnum( truckDataNode.attributes.get("TruckType"), "TruckType", TruckType.values());
			image        =            truckDataNode.attributes.get("TruckImage");
			fuelCapacity = parseInt ( truckDataNode.attributes.get("FuelCapacity") );
			diffLockType = parseEnum( truckDataNode.attributes.get("DiffLockType"), "DiffLockType", DiffLockType.values() );
			
			
			GenericXmlNode gameDataNode = item.content.getNode("Truck", "GameData");
			gameData = new GameData.GameDataTruck(gameDataNode, "Class[trucks] <Truck>");
			
			
			GenericXmlNode engineSocketNode = truckDataNode.getNode("TruckData","EngineSocket");
			//   Class[trucks] <Truck> <TruckData> <EngineSocket ####="...">
			//      Default
			//      Type
			defaultEngine_ItemID     = getAttribute(engineSocketNode, "Default");
			compatibleEngines_SetIDs = splitColonSeparatedIDList( getAttribute(engineSocketNode, "Type") );
			defaultEngine     = data.engines.getInstance(defaultEngine_ItemID, compatibleEngines_SetIDs);
			compatibleEngines = data.engines.getInstancesFromSets(compatibleEngines_SetIDs);
			compatibleEngines.forEach(item_->item_.usableBy.add(this));
			
			GenericXmlNode gearboxSocketNode = truckDataNode.getNode("TruckData","GearboxSocket");
			//   Class[trucks] <Truck> <TruckData> <GearboxSocket ####="...">
			//      Default
			//      Type
			defaultGearbox_ItemID      = getAttribute(gearboxSocketNode, "Default");
			compatibleGearboxes_SetIDs = splitColonSeparatedIDList( getAttribute(gearboxSocketNode, "Type") );
			defaultGearbox      = data.gearboxes.getInstance(defaultGearbox_ItemID, compatibleGearboxes_SetIDs);
			compatibleGearboxes = data.gearboxes.getInstancesFromSets(compatibleGearboxes_SetIDs);
			compatibleGearboxes.forEach(item_->item_.usableBy.add(this));
			
			GenericXmlNode suspensionSocketNode = truckDataNode.getNode("TruckData","SuspensionSocket");
			//   Class[trucks] <Truck> <TruckData> <SuspensionSocket ####="...">
			//      Default
			//      HardpointY
			//      MaxWheelRadiusWithoutSuspension
			//      Type
			defaultSuspension_ItemID     = getAttribute(suspensionSocketNode, "Default");
			compatibleSuspensions_SetIDs = splitColonSeparatedIDList( getAttribute(suspensionSocketNode, "Type") );
			defaultSuspension     = data.suspensions.getInstance(defaultSuspension_ItemID, compatibleSuspensions_SetIDs);
			compatibleSuspensions = data.suspensions.getInstancesFromSets(compatibleSuspensions_SetIDs);
			compatibleSuspensions.forEach(item_->item_.usableBy.add(this));
			maxWheelRadiusWithoutSuspension = getAttribute(suspensionSocketNode, "MaxWheelRadiusWithoutSuspension");
			
			GenericXmlNode winchUpgradeSocketNode = truckDataNode.getNode("TruckData","WinchUpgradeSocket");
			//   Class[trucks] <Truck> <TruckData> <WinchUpgradeSocket ####="...">
			//      Default
			//      IsUpgradable
			//      Type
			defaultWinch_ItemID      = getAttribute(winchUpgradeSocketNode, "Default");
			compatibleWinches_SetIDs = splitColonSeparatedIDList( getAttribute(winchUpgradeSocketNode, "Type") );
			defaultWinch      = data.winches.getInstance(defaultWinch_ItemID, compatibleWinches_SetIDs);
			compatibleWinches = data.winches.getInstancesFromSets(compatibleWinches_SetIDs);
			compatibleWinches.forEach(item_->item_.usableBy.add(this));
			isWinchUpgradable = parseBool( getAttribute(winchUpgradeSocketNode, "IsUpgradable") );
			boolean automaticWinch = false;
			for (Winch winch : compatibleWinches)
				if (!winch.isEngineIgnitionRequired)
					automaticWinch = true;
			hasCompatibleAutomaticWinch = automaticWinch;
			
			defaultAddonIDs = new HashSet<>();
			defaultAddons = new Vector<>();
			
			Context subNodeContext = new Context().add(this);
			GenericXmlNode[] addonSocketsNodes = gameDataNode.getNodes("GameData","AddonSockets");
			addonSockets = new AddonSockets[addonSocketsNodes.length];
			for (int i=0; i<addonSockets.length; i++)
			{
				addonSockets[i] = new AddonSockets(addonSocketsNodes[i], i, subNodeContext);
				if (addonSockets[i].defaultAddonID!=null)
					defaultAddonIDs.add(addonSockets[i].defaultAddonID);
			}
			
			forEachSocket((as1,s1) -> {
				for (String blocked_id : s1.raw_BlockedSocketIDs)
				{
					if (as1.compatibleSocketIDs.contains(blocked_id))
						continue; // this socket blocks all sockets of the same group by default
					
					// blocked socket also blocks this socket
					for (String s1_id : s1.socketIDs)
						SnowRunner.getOrCreate(s1.isBlockedBy, s1_id, Vector<String[]>::new).add(new String[] { blocked_id });
					
					forEachSocket((as2,s2) -> {
						if (contains(s2.socketIDs, blocked_id))
							for (String s1_id : s1.socketIDs)
								SnowRunner.getOrCreate(s2.isBlockedBy, blocked_id, Vector<String[]>::new).add(new String[] { s1_id });
					});
				}
			});
			
			GenericXmlNode[] compatibleWheelsNodes = truckDataNode.getNodes("TruckData", "CompatibleWheels");
			compatibleWheels = new CompatibleWheel[compatibleWheelsNodes.length];
			for (int i=0; i<compatibleWheelsNodes.length; i++)
				compatibleWheels[i] = new CompatibleWheel(compatibleWheelsNodes[i], data.wheels::get);
			
			compatibleTrailers = new HashSet<>();
			compatibleTruckAddons = new StringVectorMap<>();
		}
		
		public void forEachSocket(BiConsumer<AddonSockets, AddonSockets.Socket> action)
		{
			for (AddonSockets as : addonSockets)
				for (AddonSockets.Socket s : as.sockets)
					action.accept(as, s);
		}
		
		public interface AddonSocketPositionCreator<V>
		{
			V create(int groupIndex, int socketIndex);
		}

		public <Result> Result getAddonSocketPosition(String installSocket, AddonSocketPositionCreator<Result> createResult)
		{
			for (AddonSockets as : addonSockets)
				for (AddonSockets.Socket s : as.sockets)
					if (contains(s.socketIDs, installSocket))
						return createResult.create(as.index, s.index);
			return null;
		}

		@Override public String getName_StringID() { return gameData.getNameStringID(); }
		@Override public String getID() { return id; }
		
		public static class UDV {
			
			public enum ItemState {
				None, Able, Installed, Permanent;
				final String label;
				
				ItemState() { this(null); }
				ItemState(String label) { this.label = label;}
				
				@Override public String toString() { return label==null ? name() : label; }
				
				static ItemState parse(String valueStr) {
					try { return valueOf(valueStr); }
					catch (Exception e) { return null; }
				}
			}
			
			public ItemState realDiffLock;
			public ItemState realAWD;
			
			private UDV() {
				realDiffLock = null;
				realAWD = null;
			}
			
			boolean isEmpty() {
				return
					realDiffLock == null &&
					realAWD == null;
			}
			
			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				boolean isFirst = true;
				if (realDiffLock != null) {
					if (!isFirst) builder.append(", ");
					builder.append("DiffLock=");
					builder.append(realDiffLock);
					isFirst = false;
				}
				if (realAWD != null) {
					if (!isFirst) builder.append(", ");
					builder.append("AWD=");
					builder.append(realAWD);
					isFirst = false;
				}
				return builder.toString();
			}
		}
		
		public static class AddonSockets {

			public final int index;
			public final StringVectorMap<TruckAddon> compatibleTruckAddons;
			public final StringVectorMap<Trailer> compatibleTrailers;
			public final String defaultAddonID;
			public final Socket[] sockets;
			public final HashSet<String> compatibleSocketIDs;
			public TruckAddon defaultAddonItem;

			private AddonSockets(GenericXmlNode node, int index, Context context) {
				this.index = index;
				
				if (!node.nodeName.equals("AddonSockets"))
					throw new IllegalStateException();
				
				//node.attributes.forEach((key,value)->{
				//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets ####=\"...\">", key);
				//});
				//   Class[trucks] <Truck> <GameData> <AddonSockets ####="...">
				//      DefaultAddon
				//      ParentFrame
				//      RequiredAddonIfNoConflicts
				
				defaultAddonID = node.attributes.get("DefaultAddon");
				defaultAddonItem = null;
				
				Context subNodeContext = context.add(this);
				compatibleTrailers = new StringVectorMap<>();
				compatibleTruckAddons = new StringVectorMap<>();
				compatibleSocketIDs = new HashSet<>();
				GenericXmlNode[] socketNodes = node.getNodes("AddonSockets","Socket");
				sockets = new Socket[socketNodes.length];
				for (int i=0; i<sockets.length; i++) {
					sockets[i] = new Socket(socketNodes[i], i, subNodeContext);
					compatibleSocketIDs.addAll(Arrays.asList(sockets[i].socketIDs));
				}
			}
			
			public static class Socket {

				public final int index;
				public final String[] socketIDs; // "Names" attribute   <--->   <TruckAddon> <GameData> <InstallSocket Type="#####">
				public final Boolean isInCockpit;
				public final String dir_;
				public final String upDir_;
				public final String offset_;
				public final String parentFrame_;
				public final HashMap<String,Vector<String[]>> isBlockedBy;
				public final Vector<AddonsShift> isShiftedBy;
				public final String[] raw_BlockedSocketIDs;
				public final RawAddonsShift[] raw_AddonsShifts;

				private Socket(GenericXmlNode node, int index, Context context) {
					this.index = index;
					
					if (!node.nodeName.equals("Socket"))
						throw new IllegalStateException();
					
					//scanNode(node, "Class[trucks] <Truck> <GameData> <AddonSockets>", "Socket");
					/*
					   Class[trucks] <Truck> <GameData> <AddonSockets> <Socket ####="...">
					   -  Dir
					   #  InCockpit
					   #  Names
					   #  NamesBlock
					   -  Offset
					   -  ParentFrame
					   -  UpDir
					   
					   Class[trucks] <Truck> <GameData> <AddonSockets> <Socket> <####>
					      AddonsShift
					      ExtraParent
					*/
					socketIDs            = splitColonSeparatedIDList(node.attributes.get("Names"      ));
					raw_BlockedSocketIDs = splitColonSeparatedIDList(node.attributes.get("NamesBlock" ));
					isInCockpit          = parseBool                (node.attributes.get("InCockpit"  ));
					dir_                 =                           node.attributes.get("Dir"        ) ;
					upDir_               =                           node.attributes.get("UpDir"      ) ;
					offset_              =                           node.attributes.get("Offset"     ) ;
					parentFrame_         =                           node.attributes.get("ParentFrame") ;
					//if (node.attributes.containsKey("Dir"))
					//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket Dir=\"...\">", truck.id);
					//if (node.attributes.containsKey("UpDir"))
					//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket UpDir=\"...\">", truck.id);
					
					isBlockedBy = new HashMap<>();
					isShiftedBy   = new Vector<>();
					
					GenericXmlNode[] addonsShiftNodes = node.getNodes("Socket","AddonsShift");
					raw_AddonsShifts = new RawAddonsShift[addonsShiftNodes.length];
					for (int i=0; i<raw_AddonsShifts.length; i++)
					{
						RawAddonsShift ras = raw_AddonsShifts[i] = new RawAddonsShift(addonsShiftNodes[i]);
						
						//Truck truck = context.get(Truck.class);
						//AddonSockets addonSockets = context.get(AddonSockets.class);
						//@SuppressWarnings("unused")
						//String position = String.format("%s [%d,%d,%d]", truck.id, addonSockets.index, index, i);
						
						if (ras.trailerNamesBlock_!=null)
						{
							//if (!isIn(ras.trailerNamesBlock_, socketIDs))
							//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket> <AddonsShift> - TrailerNamesBlock !in Socket.Names", position);
							
							//if (!"(0; 0; 0)".equals(ras.offset_))
							//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket> <AddonsShift> - Offset != \"(0; 0; 0)\"", position);
							
							if (contains(socketIDs, ras.trailerNamesBlock_))
								SnowRunner.getOrCreate(isBlockedBy, ras.trailerNamesBlock_, Vector<String[]>::new).add(ras.types_);
						}
						else
						{
							//if (ras.types_==null)
							//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket> <AddonsShift> - no Types attribute", position);
							//else
							//if (ras.types_.length!=1)
							//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket> <AddonsShift> - Types.length != 1", position);
							
							isShiftedBy.add(new AddonsShift(ras.types_, offset_));
						}
					}
				}
				
				public record AddonsShift(String[] types, String offset) {}
				
				public static class RawAddonsShift
				{
					public final String offset_;
					public final String trailerNamesBlock_;
					public final String[] types_;
					
					private RawAddonsShift(GenericXmlNode node)
					{
						//scanNode(node, "Class[trucks] <Truck> <GameData> <AddonSockets> <Socket>", "AddonsShift");
						/*
						   Class[trucks] <Truck> <GameData> <AddonSockets> <Socket> <AddonsShift ####="...">
						      Offset
						      TrailerNamesBlock
						      Types
						 */
						offset_            =                           node.attributes.get("Offset"           );
						trailerNamesBlock_ =                           node.attributes.get("TrailerNamesBlock");
						types_             = splitColonSeparatedIDList(node.attributes.get("Types"            ));
					}
				}
			}
		}

		public static class CompatibleWheel {
			
			public final Float scale;
			public final String type;
			public final WheelsDef wheelsDef;
			
			private CompatibleWheel(GenericXmlNode node, Function<String,WheelsDef> getWheelsDef) {
				if (!node.nodeName.equals("CompatibleWheels"))
					throw new IllegalStateException();
				
				scale = parseFloat( node.attributes.get("Scale") );
				type  =             node.attributes.get("Type");
				wheelsDef = type==null ? null : getWheelsDef.apply(type);
			}
		
			public Integer getSize() {
				return computeSize_inch(scale);
			}
		
			public static Integer computeSize_inch(Float scale) {
				return scale==null ? null : Math.round(scale.floatValue()*78.5f);
			}
			public static Integer computeSize_inch(Double scale) {
				return scale==null ? null : (int)Math.round(scale.doubleValue()*78.5);
			}
		
			public void printTireList(ValueListOutput out, int indentLevel) {
				if (wheelsDef!=null)
					wheelsDef.forEachTire((i,truckTire) -> {
						out.add(indentLevel, String.format("[%d]", i+1) );
						truckTire.printValues(out, indentLevel+1);
					});
				else
					out.add(indentLevel, "[No TruckTires]" );
				
			}
		}
	}
	
	private static class Context
	{
		private final HashMap<Class<?>, Object> parents = new HashMap<>();

		Context add(Object parent)
		{
			Context new_ = new Context();
			new_.parents.putAll(parents);
			new_.parents.put(parent.getClass(), parent);
			return new_;
		}
		
		@SuppressWarnings("unused")
		<Type> Type get(Class<Type> class_)
		{
			Object parent = parents.get(class_);
			if (parent==null) return null;
			if (!class_.isInstance(parent))
				throw new IllegalStateException();
			return class_.cast(parent);
		}
	}
	
	private static abstract class ItemBasedNonTruck extends ItemBased implements HasNameAndID, GameData.GameDataT3NonTruckContainer
	{
		ItemBasedNonTruck(Item item) { super(item); }
	}
	
	public static class Trailer extends ItemBasedNonTruck {

		public final String attachType;
		public final Integer repairsCapacity;
		public final Integer wheelRepairsCapacity;
		public final Integer fuelCapacity;
		public final Integer waterCapacity;
		public final Vector<Truck> usableBy;
		public final Vector<TruckAddon> compatibleCargo;
		
		public final GameData.GameDataTrailer gameData;

		private Trailer(Item item) {
			super(item);
			if (!item.content.nodeName.equals("Truck"))
				throw new IllegalStateException();
			
			usableBy = new Vector<>();
			attachType = item.content.attributes.get("AttachType");
			compatibleCargo = new Vector<>();
			
			GenericXmlNode truckDataNode = item.content.getNode("Truck", "TruckData");
			repairsCapacity      = parseInt( getAttribute(truckDataNode, "RepairsCapacity"     ) );
			wheelRepairsCapacity = parseInt( getAttribute(truckDataNode, "WheelRepairsCapacity") );
			fuelCapacity         = parseInt( getAttribute(truckDataNode, "FuelCapacity"        ) );
			waterCapacity        = parseInt( getAttribute(truckDataNode, "WaterCapacity"       ) );
			//if (truckDataNode!=null)
			//	truckDataNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <Truck Type=\"Trailer\"> <TruckData ####=\"...\">", key);
			//	});
			
			GenericXmlNode gameDataNode = item.content.getNode("Truck", "GameData");
			gameData = new GameData.GameDataTrailer(gameDataNode, "Class[trucks] <Truck Type=\"Trailer\">");
		}

		@Override public String getName_StringID() { return gameData.getNameStringID(); }
		@Override public String getID() { return id; }
		@Override public GameData.GameDataT3NonTruck getGameData() { return gameData; }
	}

	public static class TruckAddon extends ItemBasedNonTruck {
		
		public final Integer repairsCapacity;
		public final Integer wheelRepairsCapacity;
		public final Integer fuelCapacity;
		public final Integer waterCapacity;
		public final Boolean enablesAllWheelDrive;
		public final Boolean enablesDiffLock;
		public final Vector<Truck> usableBy;
		public final Vector<TruckAddon> compatibleCargo;
		public final GameData.GameDataTruckAddon gameData;

		private TruckAddon(Item item, HashMap<String, CargoType> cargoTypes) {
			super(item);
			if (!item.content.nodeName.equals("TruckAddon"))
				throw new IllegalStateException();
			
			usableBy = new Vector<>();
			compatibleCargo = new Vector<>();
			
			GenericXmlNode[] truckDataNodes = item.content.getNodes("TruckAddon", "TruckData");
			repairsCapacity      = parseInt ( getAttribute(truckDataNodes, "RepairsCapacity"       ) );
			wheelRepairsCapacity = parseInt ( getAttribute(truckDataNodes, "WheelRepairsCapacity"  ) );
			fuelCapacity         = parseInt ( getAttribute(truckDataNodes, "FuelCapacity"          ) );
			waterCapacity        = parseInt ( getAttribute(truckDataNodes, "WaterCapacity"         ) );
			enablesAllWheelDrive = parseBool( getAttribute(truckDataNodes, "AllWheelDriveInstalled") );
			enablesDiffLock      = parseBool( getAttribute(truckDataNodes, "DiffLockInstalled"     ) );
			//for (GenericXmlNode truckDataNode : truckDataNodes)
			//	truckDataNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <TruckAddon> <TruckData ####=\"...\">", key);
			//	});
		
			
			GenericXmlNode gameDataNode = item.content.getNode("TruckAddon", "GameData");
			gameData = new GameData.GameDataTruckAddon(gameDataNode, "Class[trucks] <TruckAddon>", cargoTypes);
			//gameDataNode.attributes.forEach((key,value)->{
			//	unexpectedValues.add("Class[trucks] <TruckAddon> <GameData ####=\"...\">", key);
			//});
			
			//if (gameDataNode.attributes.containsKey("OriginalAddon"))
			//	unexpectedValues.add("Class[trucks] <TruckAddon> <GameData OriginalAddon=\"...\">", item.filePath);
			
		}

		@Override public String getName_StringID() { return SnowRunner.getFirstNonNull( gameData.getNameStringID(), gameData.cargoName_StringID ); }
		@Override public String getID() { return id; }
		@Override public GameData.GameDataT3NonTruck getGameData() { return gameData; }

		String getCategory() {
			if (gameData.isCargo) return AddonCategories.CARGO_CATEGORY;
			if (gameData.category==null) return AddonCategories.NULL_CATEGORY;
			return gameData.category;
		}

	}

}
