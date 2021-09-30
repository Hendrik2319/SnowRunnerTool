package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.Data.Truck.AddonSockets;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.StringMultiMap;

public class Data {

	private static class HashSetMap<MapKeyType,SetValueType> extends HashMap<MapKeyType,HashSet<SetValueType>> {
		private static final long serialVersionUID = -6897179951968079373L;
		private final Comparator<? super MapKeyType> compMapKeyType;
		private final Comparator<? super SetValueType> compSetValueType;
		
		HashSetMap(Comparator<? super MapKeyType> compMapKeyType, Comparator<? super SetValueType> compSetValueType) {
			this.compMapKeyType = compMapKeyType;
			this.compSetValueType = compSetValueType;
		}
		
		void add(MapKeyType key, SetValueType value) {
			if (key==null) new IllegalArgumentException();
			HashSet<SetValueType> set = get(key);
			if (set==null) put(key, set = new HashSet<>());
			set.add(value);
		}
		
		void print(PrintStream out, String label) {
			out.printf("%s:%n", label);
			Vector<MapKeyType> keys = new Vector<>(keySet());
			keys.sort(compMapKeyType);
			for (MapKeyType key : keys) {
				out.printf("   %s%n", key);
				Vector<SetValueType> values = new Vector<>(get(key));
				values.sort(compSetValueType);
				for (SetValueType value : values)
					out.printf("      %s%n", value);
			}
		}
	}

	private static HashSetMap<String,String> unexpectedValues;
	
	final XMLTemplateStructure rawdata;
	final HashMap<String,Language> languages;
	final AddonCategories addonCategories;
	final HashMap<String,CargoType> cargoTypes;
	final HashMap<String,WheelsDef> wheels;
	final HashMap<String,Truck> trucks;
	final HashMap<String,Trailer> trailers;
	final HashMap<String,TruckAddon> truckAddons;
	final StringMultiMap<Truck> socketIDsUsedByTrucks;
	final StringMultiMap<Trailer> socketIDsUsedByTrailers;
	final StringMultiMap<TruckAddon> socketIDsUsedByTruckAddons;

	Data(XMLTemplateStructure rawdata) {
		this.rawdata = rawdata;
		languages = rawdata.languages;
		
		unexpectedValues = new HashSetMap<>(null,null);
		
		
		
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
				cargoTypes.put(name, new CargoType(item));
			});
		
		
		
		//MultiMap<String> wheelsTypes = new MultiMap<>();
		wheels = new HashMap<>();
		Class_ wheelsClass = rawdata.classes.get("wheels");
		if (wheelsClass!=null)
			wheelsClass.items.forEach((name,item)->{
				//wheelsTypes.add(item.content.nodeName, item.filePath);
				if (item.isMainItem() && item.content.nodeName.equals("TruckWheels"))
					wheels.put(name, new WheelsDef(item));
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
				switch (item.content.nodeName) {
				
				case "Truck":
					String type = item.content.attributes.get("Type");
					
					if (type==null)
						trucks.put(name, new Truck(item, wheels::get));
					
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
					truckAddons.put(name, new TruckAddon(item, addonCategories, cargoTypes));
					break;
					
				default:
					unexpectedValues.add("Class[trucks] <###>", item.content.nodeName);
					break;
				}
			});
		
		
		
		socketIDsUsedByTrucks = new StringMultiMap<>();
		for (Truck truck : trucks.values())
			for (AddonSockets as : truck.addonSockets) {
				if (as.defaultAddonName!=null)
					as.defaultAddonItem = truckAddons.get(as.defaultAddonName);
				for (String socketID : as.compatibleSocketIDs)
					socketIDsUsedByTrucks.add(socketID, truck);
			}

		socketIDsUsedByTrailers = new StringMultiMap<>();
		for (Trailer trailer : trailers.values()) {
			if (trailer.installSocket == null)
				System.err.printf("No InstallSocket for trailer <%s>%n", trailer.id);
			
			else {
				socketIDsUsedByTrailers.add(trailer.installSocket, trailer);
				
				Vector<Truck> trucks = socketIDsUsedByTrucks.get(trailer.installSocket);
				if (trucks != null)
					trailer.usableBy.addAll(trucks);
				//else
				//	System.err.printf("No Trucks are using SocketID \"%s\" of Trailer <%s>%n", trailer.installSocket, trailer.id);
			}
			
		}

		socketIDsUsedByTruckAddons = new StringMultiMap<>();
		for (TruckAddon truckAddon : truckAddons.values())
			if (truckAddon.installSocket!=null) {
				socketIDsUsedByTruckAddons.add(truckAddon.installSocket, truckAddon);
				
				Vector<Truck> trucks = socketIDsUsedByTrucks.get(truckAddon.installSocket);
				if (trucks != null)
					truckAddon.usableBy.addAll(trucks);
				//else
				//	System.err.printf("No Trucks are using SocketID \"%s\" of TruckAddon <%s>%n", truckAddon.installSocket, truckAddon.id);
			}
		
		for (Truck truck : trucks.values()) {
			truck.compatibleTrailers.clear();
			truck.compatibleTruckAddons.clear();
			for (AddonSockets as : truck.addonSockets) {
				for (String socketID : as.compatibleSocketIDs) {
					
					Vector<Trailer> trailers = socketIDsUsedByTrailers.get(socketID);
					if (trailers != null) {
						as.compatibleTrailers.addAll(socketID, trailers);
						truck.compatibleTrailers.addAll(trailers);
					}
					
					Vector<TruckAddon> truckAddons = socketIDsUsedByTruckAddons.get(socketID);
					if (truckAddons != null) {
						as.compatibleTruckAddons.addAll(socketID, truckAddons);
						for (TruckAddon truckAddon : truckAddons) {
							String category = truckAddon.category;
							if (category==null) category = TruckAddon.NULL_CATEGORY;
							truck.compatibleTruckAddons.add(category, truckAddon);
						}
					}
				}
			}
		}
		
		if (!unexpectedValues.isEmpty())
			unexpectedValues.print(System.out,"Unexpected Values");
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

	static class Language {
		final String name;
		final HashMap<String,String> dictionary;
		
		Language(String name) {
			this.name = name;
			this.dictionary = new HashMap<>();
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

	static class ItemBased {
		final String id;
		final String xmlName;
		final String dlcName;
		ItemBased(Item item) {
			id      = item.name;
			xmlName = item.name+".xml";
			dlcName = item.dlcName;
		}
	}

	static class CargoType extends ItemBased {
	
		final String description_StringID;
		final String name_StringID;
		final String icon100;
		final String icon40;
		final String icon20;

		public CargoType(Item item) {
			super(item);
			
			//item.content.attributes.forEach((key,value)->{
			//	unexpectedValues.add(String.format("Class[addons_category] <%s ####=\"...\">", item.content.nodeName), key);
			//});
			
			GenericXmlNode uiDescNode = item.content.getNode("CargoType", "UiDesc");
			description_StringID = getAttribute(uiDescNode, "UiDesc");
			name_StringID        = getAttribute(uiDescNode, "UiName");
			icon100              = getAttribute(uiDescNode, "UiIcon100x100");
			icon40               = getAttribute(uiDescNode, "UiIcon40x40");
			icon20               = getAttribute(uiDescNode, "UiIcon20x20");
			
			//if (uiDescNode!=null)
			//	uiDescNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[addons_category] <CargoType> <UiDesc ####=\"...\">", key);
			//	});
		}
	
	}

	static class AddonCategories extends ItemBased {
	
		final Category[] categories;
		
		AddonCategories(Item item) {
			super(item);

			GenericXmlNode[] categoryNodes = item.content.getNodes("CategoryList", "Category");
			categories = new Category[categoryNodes.length];
			for (int i=0; i<categoryNodes.length; i++) {
				GenericXmlNode node = categoryNodes[i];
				categories[i] = new Category(node);
			}
		}
		
		String getCategoryLabel(String category, Language language) {
			if (category==null || category.equals(TruckAddon.NULL_CATEGORY))
				return "<Unknown Category>";
			
			String label_StringID = null;;
			for (Category c : categories)
				if (category.equals(c.name)) {
					label_StringID = c.label_StringID;
					break;
				}
			
			return SnowRunner.solveStringID(label_StringID, language, String.format("<%s>", category));
		}
		
		static class Category {

			final String name;
			final boolean requiresOneAddonInstalled;
			final String icon;
			final String label_StringID;

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
		}
	
	}

	static class WheelsDef extends ItemBased {
	
		final Vector<TruckTire> truckTires;
	
		public WheelsDef(Item item) {
			super(item);
			
			truckTires = new Vector<>();
			GenericXmlNode[] truckTireNodes = item.content.getNodes("TruckWheels","TruckTires","TruckTire");
			for (int i=0; i<truckTireNodes.length; i++)
				truckTires.add(new TruckTire(truckTireNodes[i], id, i, dlcName));
		}
	
	}

	static class TruckTire {
	
		final String wheelsDefID;
		final int indexInDef;
		final String dlc;
		
		final String tireType_StringID;
		final Float frictionHighway;
		final Float frictionOffroad;
		final Float frictionMud;
		final boolean onIce;
		
		final Integer price;
		final Boolean unlockByExploration;
		final Integer unlockByRank;
		final String description_StringID;
		final String name_StringID;
	
		TruckTire(GenericXmlNode node, String wheelsDefID, int indexInDef, String dlc) {
			this.wheelsDefID = wheelsDefID;
			this.indexInDef = indexInDef;
			this.dlc = dlc;
			
			GenericXmlNode wheelFrictionNode = node.getNode("TruckTire", "WheelFriction");
			
//			BodyFriction="2.0"
//			BodyFrictionAsphalt="0.9"
//			IsIgnoreIce="true"
//			SubstanceFriction="1.1"
//			UiName="UI_TIRE_TYPE_CHAINS_NAME"
			
			tireType_StringID =             wheelFrictionNode.attributes.get("UiName");
			frictionHighway   = parseFloat( wheelFrictionNode.attributes.get("BodyFrictionAsphalt") );
			frictionOffroad   = parseFloat( wheelFrictionNode.attributes.get("BodyFriction"       ) );
			frictionMud       = parseFloat( wheelFrictionNode.attributes.get("SubstanceFriction"  ) );
			onIce             = parseBool ( wheelFrictionNode.attributes.get("IsIgnoreIce"), false );
			
			GenericXmlNode gameDataNode = node.getNode("TruckTire", "GameData"     );
			price               = parseInt (gameDataNode.attributes.get("Price"));
			unlockByExploration = parseBool(gameDataNode.attributes.get("UnlockByExploration"));
			unlockByRank        = parseInt (gameDataNode.attributes.get("UnlockByRank"));
			
			GenericXmlNode uiDescNode = gameDataNode.getNode("GameData", "UiDesc");
			description_StringID = getAttribute(uiDescNode, "UiDesc");
			name_StringID        = getAttribute(uiDescNode, "UiName");
		}
	
		void printValues(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "TireType [StringID]", "<%s>", tireType_StringID);
			out.add(indentLevel, "Friction (highway)", frictionHighway);
			out.add(indentLevel, "Friction (offroad)", frictionOffroad);
			out.add(indentLevel, "Friction (mud)"    , frictionMud);
			out.add(indentLevel, "OnIce", onIce);
			out.add(indentLevel, "Price"                , price);
			out.add(indentLevel, "Unlock By Exploration", unlockByExploration);
			out.add(indentLevel, "Unlock By Rank"       , unlockByRank);
			out.add(indentLevel, "Name"       , "<%s>", name_StringID);
			out.add(indentLevel, "Description", "<%s>", description_StringID);
		}
	
	}

	static class Truck extends ItemBased {
		
		final String type;
		final String country;
		final Integer price;
		final Boolean unlockByExploration;
		final Integer unlockByRank;
		final String description_StringID;
		final String name_StringID;
		final CompatibleWheel[] compatibleWheels;
		final AddonSockets[] addonSockets;
		final HashSet<Trailer> compatibleTrailers;
		final StringMultiMap<TruckAddon> compatibleTruckAddons;
		
		Truck(Item item, Function<String, WheelsDef> getWheelsDef) {
			super(item);
			if (!item.className.equals("trucks"))
				throw new IllegalStateException();
			
			GenericXmlNode truckDataNode = item.content.getNode("Truck", "TruckData");
			type = truckDataNode.attributes.get("TruckType");
			
			GenericXmlNode gameDataNode = item.content.getNode("Truck", "GameData");
			country             =           gameDataNode.attributes.get("Country");
			price               = parseInt (gameDataNode.attributes.get("Price") );
			unlockByExploration = parseBool(gameDataNode.attributes.get("UnlockByExploration") );
			unlockByRank        = parseInt (gameDataNode.attributes.get("UnlockByRank") );
			
			GenericXmlNode uiDescNode = gameDataNode.getNode("GameData","UiDesc");
			description_StringID = uiDescNode.attributes.get("UiDesc");
			name_StringID        = uiDescNode.attributes.get("UiName");
			
			GenericXmlNode[] addonSocketsNodes = gameDataNode.getNodes("GameData","AddonSockets");
			addonSockets = new AddonSockets[addonSocketsNodes.length];
			for (int i=0; i<addonSockets.length; i++)
				addonSockets[i] = new AddonSockets(addonSocketsNodes[i]);
			
			GenericXmlNode[] compatibleWheelsNodes = truckDataNode.getNodes("TruckData", "CompatibleWheels");
			compatibleWheels = new CompatibleWheel[compatibleWheelsNodes.length];
			for (int i=0; i<compatibleWheelsNodes.length; i++)
				compatibleWheels[i] = new CompatibleWheel(compatibleWheelsNodes[i], getWheelsDef);
			
			compatibleTrailers = new HashSet<>();
			compatibleTruckAddons = new StringMultiMap<>();
		}
		
		static class AddonSockets {

			final StringMultiMap<TruckAddon> compatibleTruckAddons;
			final StringMultiMap<Trailer> compatibleTrailers;
			final String defaultAddonName; // <---> item.id
			final Socket[] sockets;
			final HashSet<String> compatibleSocketIDs;
			TruckAddon defaultAddonItem;

			AddonSockets(GenericXmlNode node) {
				if (!node.nodeName.equals("AddonSockets"))
					throw new IllegalStateException();
				
				//node.attributes.forEach((key,value)->{
				//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets ####=\"...\">", key);
				//});
				//   Class[trucks] <Truck> <GameData> <AddonSockets ####="...">
				//      DefaultAddon
				//      ParentFrame
				//      RequiredAddonIfNoConflicts
				
				defaultAddonName = node.attributes.get("DefaultAddon");
				defaultAddonItem = null;
				
				compatibleTrailers = new StringMultiMap<>();
				compatibleTruckAddons = new StringMultiMap<>();
				compatibleSocketIDs = new HashSet<>();
				GenericXmlNode[] socketNodes = node.getNodes("AddonSockets","Socket");
				sockets = new Socket[socketNodes.length];
				for (int i=0; i<sockets.length; i++) {
					sockets[i] = new Socket(socketNodes[i]);
					compatibleSocketIDs.addAll(Arrays.asList(sockets[i].socketIDs));
				}
			}
			
			static class Socket {

				final String[] socketIDs; // "Names" attribute   <--->   <TruckAddon> <GameData> <InstallSocket Type="#####">
				final String[] blockedSocketIDs;
				final boolean isInCockpit;

				public Socket(GenericXmlNode node) {
					if (!node.nodeName.equals("Socket"))
						throw new IllegalStateException();
					
					//node.attributes.forEach((key,value)->{
					//	unexpectedValues.add("Class[trucks] <Truck> <GameData> <AddonSockets> <Socket ####=\"...\">", key);
					//});
					//   Class[trucks] <Truck> <GameData> <AddonSockets> <Socket ####="...">
					//      Dir
					//      InCockpit
					//      Names
					//      NamesBlock
					//      Offset
					//      ParentFrame
					//      UpDir
					
					socketIDs        = splitColonSeparatedIDList(node.attributes.get("Names"));
					blockedSocketIDs = splitColonSeparatedIDList(node.attributes.get("NamesBlock"));
					isInCockpit      = parseBool(node.attributes.get("InCockpit"),false);
				}
			}
		}

		static class CompatibleWheel {
			
			final Float scale;
			final String type;
			final WheelsDef wheelsDef;
			
			CompatibleWheel(GenericXmlNode node, Function<String,WheelsDef> getWheelsDef) {
				if (!node.nodeName.equals("CompatibleWheels"))
					throw new IllegalStateException();
				
				scale = parseFloat( node.attributes.get("Scale") );
				type  =             node.attributes.get("Type");
				wheelsDef = type==null ? null : getWheelsDef.apply(type);
			}
		
			Integer getSize() {
				return computeSize_inch(scale);
			}
		
			public static Integer computeSize_inch(Float scale) {
				return scale==null ? null : Math.round(scale.floatValue()*78.5f);
			}
		
			void printTireList(ValueListOutput out, int indentLevel) {
				if (wheelsDef!=null)
					for (int i=0; i<wheelsDef.truckTires.size(); i++) {
						TruckTire truckTire = wheelsDef.truckTires.get(i);
						out.add(indentLevel, String.format("[%d]", i+1) );
						truckTire.printValues(out, indentLevel+1);
					}
				else
					out.add(indentLevel, "[No TruckTires]" );
				
			}
		}

	}
	
	static class Trailer extends ItemBased {

		final String attachType;
		final Integer price;
		final Boolean unlockByExploration;
		final Integer unlockByRank;
		final Boolean isQuestItem;
		final String[] excludedCargoTypes;
		final String[][] requiredAddons; // (ra[0][0] || ra[0][1] || ... ) && (ra[1][0] || ra[1][1] || ... ) && ...
		final String description_StringID;
		final String name_StringID;
		final String installSocket;
		final Integer cargoSlots;
		final Integer repairsCapacity;
		final Integer wheelRepairsCapacity;
		final Integer fuelCapacity;
		final Vector<Truck> usableBy;

		public Trailer(Item item) {
			super(item);
			if (!item.content.nodeName.equals("Truck"))
				throw new IllegalStateException();
			
			usableBy = new Vector<>();
			attachType = item.content.attributes.get("AttachType");
			
			
			GenericXmlNode truckDataNode = item.content.getNode("Truck", "TruckData");
			repairsCapacity      = parseInt( getAttribute(truckDataNode, "RepairsCapacity"     ) );
			wheelRepairsCapacity = parseInt( getAttribute(truckDataNode, "WheelRepairsCapacity") );
			fuelCapacity         = parseInt( getAttribute(truckDataNode, "FuelCapacity"        ) );
			//if (truckDataNode!=null)
			//	truckDataNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <Truck Type=\"Trailer\"> <TruckData ####=\"...\">", key);
			//	});
			
			
			GenericXmlNode gameDataNode = item.content.getNode("Truck", "GameData");
			//gameDataNode.attributes.forEach((key,value)->{
			//	unexpectedValues.add("Class[trucks] <Truck Type=\"Trailer\"> <GameData ####=\"...\">", key);
			//});
			
			GenericXmlNode uiDescNode = gameDataNode.getNode("GameData", "UiDesc");
			description_StringID = getAttribute(uiDescNode, "UiDesc");
			name_StringID        = getAttribute(uiDescNode, "UiName");
			
			GenericXmlNode installSocketNode = gameDataNode.getNode("GameData", "InstallSocket");
			installSocket = getAttribute(installSocketNode, "Type");
			//if (installSocketNode!=null)
			//	installSocketNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <Truck Type=\"Trailer\"> <GameData> <InstallSocket ####=\"...\">", key);
			//	});
			
			GenericXmlNode addonSlotsNode = gameDataNode.getNode("GameData", "AddonSlots");
			cargoSlots = parseInt( getAttribute(addonSlotsNode, "Quantity") );
			//if (addonSlotsNode!=null)
			//	addonSlotsNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <Truck Type=\"Trailer\"> <GameData> <AddonSlots ####=\"...\">", key);
			//	});
			
			price               = parseInt (gameDataNode.attributes.get("Price") );
			unlockByExploration = parseBool(gameDataNode.attributes.get("UnlockByExploration") );
			unlockByRank        = parseInt (gameDataNode.attributes.get("UnlockByRank") );
			isQuestItem         = parseBool(gameDataNode.attributes.get("IsQuest") );
			excludedCargoTypes  = splitColonSeparatedIDList( gameDataNode.attributes.get("ExcludedCargoTypes") );
			
			GenericXmlNode[] requiredAddonNodes = gameDataNode.getNodes("GameData", "RequiredAddon");
			requiredAddons = new String[requiredAddonNodes.length][];
			for (int i=0; i<requiredAddonNodes.length; i++) {
				GenericXmlNode requiredAddonNode = requiredAddonNodes[i];
				requiredAddons[i] = splitColonSeparatedIDList( getAttribute(requiredAddonNode, "Types") );
			}
		}
	
	}

	static class TruckAddon extends ItemBased {

		static final String NULL_CATEGORY = "NULL_CATEGORY";
		final String category;
		final Integer price;
		final Boolean unlockByExploration;
		final Integer unlockByRank;
		final String description_StringID;
		final String name_StringID;
		final String installSocket;
		final boolean isCargo;
		final Integer cargoLength;
		final String  cargoType;
		final Integer cargoValue;
		final String  cargoName_StringID;
		final String  cargoDescription_StringID;
		final String[] excludedCargoTypes;
		final String[][] requiredAddons; // (ra[0][0] || ra[0][1] || ... ) && (ra[1][0] || ra[1][1] || ... ) && ...
		final Integer repairsCapacity;
		final Integer wheelRepairsCapacity;
		final Integer fuelCapacity;
		final Integer cargoSlots;
		final Boolean enablesAllWheelDrive;
		final Boolean enablesDiffLock;
		final Vector<Truck> usableBy;

		public TruckAddon(Item item, AddonCategories addonCategories, HashMap<String, CargoType> cargoTypes) {
			super(item);
			if (!item.content.nodeName.equals("TruckAddon"))
				throw new IllegalStateException();
			
			usableBy = new Vector<>();
			
			GenericXmlNode[] truckDataNodes = item.content.getNodes("TruckAddon", "TruckData");
			repairsCapacity      = parseInt ( getAttribute(truckDataNodes, "RepairsCapacity"       ) );
			wheelRepairsCapacity = parseInt ( getAttribute(truckDataNodes, "WheelRepairsCapacity"  ) );
			fuelCapacity         = parseInt ( getAttribute(truckDataNodes, "FuelCapacity"          ) );
			enablesAllWheelDrive = parseBool( getAttribute(truckDataNodes, "AllWheelDriveInstalled") );
			enablesDiffLock      = parseBool( getAttribute(truckDataNodes, "DiffLockInstalled"     ) );
			//for (GenericXmlNode truckDataNode : truckDataNodes)
			//	truckDataNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <TruckAddon> <TruckData ####=\"...\">", key);
			//	});
		
			
			GenericXmlNode gameDataNode = item.content.getNode("TruckAddon", "GameData");
			//gameDataNode.attributes.forEach((key,value)->{
			//	unexpectedValues.add("Class[trucks] <TruckAddon> <GameData ####=\"...\">", key);
			//});
			
			//if (gameDataNode.attributes.containsKey("OriginalAddon"))
			//	unexpectedValues.add("Class[trucks] <TruckAddon> <GameData OriginalAddon=\"...\">", item.filePath);
			
			// <TruckAddon> <GameData SaddleType=\"...\"> besser ignorieren, scheint wohl veraltet, ist jedenfalls mindestens einmal falsch
			// <TruckAddon> <GameData LoadPoints=\"...\">  --> für Stämme -> erstmal ignorieren
			// <TruckAddon> <GameData ManualLoads=\"...\">  --> für Stämme -> erstmal ignorieren
			// <TruckAddon> <GameData GaragePoints=\"...\">  --> Keine Ahnung
			//    \[media]\_dlc\dlc_1_1\classes\trucks\addons\frame_addon_sideboard_1.xml
			//    \[media]\classes\trucks\addons\international_loadstar_1700_pickup.xml
			//    \[media]\classes\trucks\addons\international_loadstar_1700_service_body.xml
			// <TruckAddon> <GameData OriginalAddon=\"...\"> --> verwendet bei abgewandelten AddOns --> im Spiel testen
			//    \[media]\classes\trucks\addons\big_crane_us_ws4964.xml
			
			category            =            gameDataNode.attributes.get("Category"           );
			price               = parseInt ( gameDataNode.attributes.get("Price"              ) );
			unlockByExploration = parseBool( gameDataNode.attributes.get("UnlockByExploration") );
			unlockByRank        = parseInt ( gameDataNode.attributes.get("UnlockByRank"       ) );
			excludedCargoTypes  = splitColonSeparatedIDList( gameDataNode.attributes.get("ExcludedCargoTypes") );
			
			GenericXmlNode[] requiredAddonNodes = gameDataNode.getNodes("GameData", "RequiredAddon");
			requiredAddons = new String[requiredAddonNodes.length][];
			for (int i=0; i<requiredAddonNodes.length; i++) {
				GenericXmlNode requiredAddonNode = requiredAddonNodes[i];
				requiredAddons[i] = splitColonSeparatedIDList( getAttribute(requiredAddonNode, "Types") );
			}
			
			GenericXmlNode uiDescNode = gameDataNode.getNode("GameData", "UiDesc"); // normal AddOn
			description_StringID = getAttribute(uiDescNode, "UiDesc");
			name_StringID        = getAttribute(uiDescNode, "UiName");
			
			GenericXmlNode addonSlotsNode = gameDataNode.getNode("GameData", "AddonSlots");
			cargoSlots = parseInt( getAttribute(addonSlotsNode, "Quantity") );
			//if (addonSlotsNode!=null)
			//	addonSlotsNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <TruckAddon> <GameData> <AddonSlots ####=\"...\">", key);
			//	});
			
			GenericXmlNode installSocketNode = gameDataNode.getNode("GameData", "InstallSocket"); // normal AddOn
			installSocket = getAttribute(installSocketNode, "Type");
			
			GenericXmlNode installSlotNode = gameDataNode.getNode("GameData", "InstallSlot"); // Cargo-Addon
			isCargo     = installSlotNode!=null;
			cargoLength = parseInt ( getAttribute(installSlotNode, "CargoLength") );
			cargoType   =            getAttribute(installSlotNode, "CargoType"  );
			cargoValue  = parseInt ( getAttribute(installSlotNode, "CargoValue" ) );
			CargoType ct = null;
			if (cargoType!=null && name_StringID==null)
				ct = cargoTypes.get(cargoType);
			if (ct!=null) {
				cargoName_StringID = ct.name_StringID;
				cargoDescription_StringID = ct.description_StringID;
			} else {
				cargoName_StringID = null;
				cargoDescription_StringID = null;
			}
			
			//if (installSlotNode!=null)
			//	installSlotNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <TruckAddon> <GameData> <InstallSlot ####=\"...\">", key);
			//	});
			
		}

	}
}
