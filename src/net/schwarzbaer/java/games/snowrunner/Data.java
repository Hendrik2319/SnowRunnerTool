package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;

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

	final XMLTemplateStructure rawdata;
	final HashMap<String,Language> languages;
	final HashMap<String,WheelsDef> wheels;
	final HashMap<String,Truck> trucks;
	final HashMap<String,Trailer> trailers;
	final HashMap<String,TruckAddon> truckAddons;
	private static HashSetMap<String,String> unexpectedValues;

	Data(XMLTemplateStructure rawdata) {
		this.rawdata = rawdata;
		languages = rawdata.languages;
		
		//MultiMap<String> wheelsTypes = new MultiMap<>();
		wheels = new HashMap<>();
		Class_ wheelsClass = rawdata.classes.get("wheels");
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
		
		unexpectedValues = new HashSetMap<>(null,null);
		Class_ trucksClass = rawdata.classes.get("trucks");
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
				truckAddons.put(name, new TruckAddon(item));
				break;
				
			default:
				unexpectedValues.add("Class[trucks] <###>", item.content.nodeName);
				break;
			}
		});
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
	
	@SuppressWarnings("unused")
	private static String selectNonNull(String... strings) {
		for (String str : strings)
			if (str!=null)
				return str;
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

	static class WheelFriction {
	
		final String name_StringID;
		final Float frictionHighway;
		final Float frictionOffroad;
		final Float frictionMud;
		final boolean onIce;

		WheelFriction(GenericXmlNode node) {
			
//			BodyFriction="2.0"
//			BodyFrictionAsphalt="0.9"
//			IsIgnoreIce="true"
//			SubstanceFriction="1.1"
//			UiName="UI_TIRE_TYPE_CHAINS_NAME"
			
			name_StringID   =             node.attributes.get("UiName");
			frictionHighway = parseFloat( node.attributes.get("BodyFrictionAsphalt") );
			frictionOffroad = parseFloat( node.attributes.get("BodyFriction"       ) );
			frictionMud     = parseFloat( node.attributes.get("SubstanceFriction"  ) );
			onIce           = parseBool ( node.attributes.get("IsIgnoreIce"), false );
		}

		void printValues(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "Name [StringID]", name_StringID);
			out.add(indentLevel, "Friction (highway)", frictionHighway);
			out.add(indentLevel, "Friction (offroad)", frictionOffroad);
			out.add(indentLevel, "Friction (mud)"    , frictionMud);
			out.add(indentLevel, "OnIce", onIce);
		}
		
	}

	static class WheelsDef extends ItemBased {

		final Vector<TruckTire> truckTires;

		public WheelsDef(Item item) {
			super(item);
			
			truckTires = new Vector<>();
			GenericXmlNode[] truckTireNodes = item.content.getNodes("TruckWheels","TruckTires","TruckTire");
			for (int i=0; i<truckTireNodes.length; i++)
				truckTires.add(new TruckTire(truckTireNodes[i], xmlName, i, dlcName));
		}
	
	}

	static class TruckTire {
	
		final String definingXML;
		final int indexInDef;
		final String dlc;
		final WheelFriction wheelFriction;
		final GameData gameData;
	
		TruckTire(GenericXmlNode node, String definingXML, int indexInDef, String dlc) {
			this.definingXML = definingXML;
			this.indexInDef = indexInDef;
			this.dlc = dlc;
			this.wheelFriction = new WheelFriction( node.getNode("TruckTire", "WheelFriction") );
			this.gameData      = new GameData     ( node.getNode("TruckTire", "GameData"     ) );
		}
	
		void printValues(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "WheelFriction");
			wheelFriction.printValues(out, indentLevel+1);
			out.add(indentLevel, "GameData");
			gameData.printValues(out, indentLevel+1);
		}

		static class GameData {
		
			final Integer price;
			final Boolean unlockByExploration;
			final Integer unlockByRank;
			final String description_StringID;
			final String name_StringID;
		
			GameData(GenericXmlNode node) {
				price               = parseInt (node.attributes.get("Price"));
				unlockByExploration = parseBool(node.attributes.get("UnlockByExploration"));
				unlockByRank        = parseInt (node.attributes.get("UnlockByRank"));
				
				GenericXmlNode uiDescNode = node.getNode("GameData", "UiDesc");
				if (uiDescNode!=null) {
					description_StringID = uiDescNode.attributes.get("UiDesc");
					name_StringID        = uiDescNode.attributes.get("UiName");
					
				} else {
					description_StringID = null;
					name_StringID        = null;
				}
			}

			void printValues(ValueListOutput out, int indentLevel) {
				out.add(indentLevel, "Price"                , price);
				out.add(indentLevel, "Unlock By Exploration", unlockByExploration);
				out.add(indentLevel, "Unlock By Rank"       , unlockByRank);
				out.add(indentLevel, "Name"       , "<%s>", name_StringID);
				out.add(indentLevel, "Description", "<%s>", description_StringID);
			}
		
		}
	
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

	static class Truck extends ItemBased {
		
		final String type;
		final String country;
		final Integer price;
		final Boolean unlockByExploration;
		final Integer unlockByRank;
		final String description_StringID;
		final String name_StringID;
		final CompatibleWheel[] compatibleWheels;
		final ExpandedCompatibleWheel[] expandedCompatibleWheels;
		final AddonSockets[] addonSockets;
		
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
			for (int i1=0; i1<addonSockets.length; i1++)
				addonSockets[i1] = new AddonSockets(addonSocketsNodes[i1]);
			
			GenericXmlNode[] compatibleWheelsNodes = truckDataNode.getNodes("TruckData", "CompatibleWheels");
			compatibleWheels = new CompatibleWheel[compatibleWheelsNodes.length];
			for (int i=0; i<compatibleWheelsNodes.length; i++)
				compatibleWheels[i] = new CompatibleWheel(compatibleWheelsNodes[i], getWheelsDef);
			
			expandedCompatibleWheels = ExpandedCompatibleWheel.expand(compatibleWheels);
		}
		
		static class AddonSockets {

			final String defaultAddon; // <---> item.id

			AddonSockets(GenericXmlNode node) {
				if (!node.nodeName.equals("AddonSockets"))
					throw new IllegalStateException();
				
				defaultAddon = node.attributes.get("DefaultAddon");
				
				GenericXmlNode[] socketNodes = node.getNodes("AddonSockets","Socket");
				Socket[] sockets = new Socket[socketNodes.length];
				for (int i=0; i<sockets.length; i++)
					sockets[i] = new Socket(socketNodes[i]);
			}
			
			static class Socket {

				final String socketID; // "Names" attribute   <--->   <TruckAddon> <GameData> <InstallSocket Type="#####">
				final String[] blockedSocketID;
				final boolean isInCockpit;

				public Socket(GenericXmlNode node) {
					if (!node.nodeName.equals("Socket"))
						throw new IllegalStateException();
					
					socketID        = node.attributes.get("Names");
					blockedSocketID = splitColonSeparatedIDList(node.attributes.get("NamesBlock"));
					isInCockpit     = parseBool(node.attributes.get("InCockpit"),false);
				}
			}
		}

		static class CompatibleWheel {
			
			final Float scale;
			final String type;
			final Vector<TruckTire> truckTires;
			
			CompatibleWheel(GenericXmlNode node, Function<String,WheelsDef> getWheelsDef) {
				if (!node.nodeName.equals("CompatibleWheels"))
					throw new IllegalStateException();
				
				scale = parseFloat( node.attributes.get("Scale") );
				type  =             node.attributes.get("Type");
				WheelsDef wheelsDef = type==null ? null : getWheelsDef.apply(type);
				truckTires = wheelsDef==null ? null : wheelsDef.truckTires;
			}
		
			Integer getSize() {
				return computeSize_inch(scale);
			}
		
			public static Integer computeSize_inch(Float scale) {
				return scale==null ? null : Math.round(scale.floatValue()*78.5f);
			}
		
			void printTireList(ValueListOutput out, int indentLevel) {
				if (truckTires!=null)
					for (int i=0; i<truckTires.size(); i++) {
						TruckTire truckTire = truckTires.get(i);
						out.add(indentLevel, String.format("[%d]", i+1) );
						truckTire.printValues(out, indentLevel+1);
					}
				else
					out.add(indentLevel, "[No TruckTires]" );
				
			}
		}

		static class ExpandedCompatibleWheel {

			final Float scale;
			final String definingXML;
			final int indexInDef;
			final String dlc;
			final String name_StringID;
			final String description_StringID;
			final String type_StringID;
			final Float frictionHighway;
			final Float frictionOffroad;
			final Float frictionMud;
			final Boolean onIce;
			final Integer price;
			final Boolean unlockByExploration;
			final Integer unlockByRank;

			public ExpandedCompatibleWheel(Float scale, TruckTire tire) {
				this.scale = scale;
				
				if (tire!=null) {
					this.definingXML  = tire.definingXML;
					this.indexInDef = tire.indexInDef;
					this.dlc = tire.dlc;
				} else {
					this.definingXML  = null;
					this.indexInDef = 0;
					this.dlc = null;
				}
				
				if (tire!=null && tire.gameData!=null) {
					price                = tire.gameData.price;
					unlockByExploration  = tire.gameData.unlockByExploration;
					unlockByRank         = tire.gameData.unlockByRank;
					description_StringID = tire.gameData.description_StringID;
					name_StringID        = tire.gameData.name_StringID;
				} else {
					price                = null;
					unlockByExploration  = null;
					unlockByRank         = null;
					description_StringID = null;
					name_StringID        = null;
				}
				
				if (tire!=null && tire.wheelFriction!=null) {
					type_StringID   = tire.wheelFriction.name_StringID;
					frictionHighway = tire.wheelFriction.frictionHighway;
					frictionOffroad = tire.wheelFriction.frictionOffroad;
					frictionMud     = tire.wheelFriction.frictionMud;
					onIce           = tire.wheelFriction.onIce;
				} else {
					type_StringID   = null;
					frictionHighway = null;
					frictionOffroad = null;
					frictionMud     = null;
					onIce           = null;
				}
			}

			static ExpandedCompatibleWheel[] expand(CompatibleWheel[] compatibleWheels) {
				Vector<ExpandedCompatibleWheel> expanded = new Vector<>();
				
				for (CompatibleWheel cw:compatibleWheels)
					if (cw.truckTires!=null)
						for (TruckTire tire : cw.truckTires)
							expanded.add( new ExpandedCompatibleWheel(cw.scale,tire) );
				
				return expanded.toArray(new ExpandedCompatibleWheel[expanded.size()]);
			}

			Integer getSize() {
				return CompatibleWheel.computeSize_inch(scale);
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
		final String[] requiredAddons;

		public Trailer(Item item) {
			super(item);
			if (!item.content.nodeName.equals("Truck"))
				throw new IllegalStateException();
			
			attachType = item.content.attributes.get("AttachType");

			GenericXmlNode gameDataNode = item.content.getNode("Truck", "GameData");
			//gameDataNode.attributes.forEach((key,value)->{
			//	unexpectedValues.add("Class[trucks] <Truck Type=\"Trailer\"> <GameData ####=\"...\">", key);
			//});
			price               = parseInt (gameDataNode.attributes.get("Price") );
			unlockByExploration = parseBool(gameDataNode.attributes.get("UnlockByExploration") );
			unlockByRank        = parseInt (gameDataNode.attributes.get("UnlockByRank") );
			isQuestItem         = parseBool(gameDataNode.attributes.get("IsQuest") );
			excludedCargoTypes  = splitColonSeparatedIDList( gameDataNode.attributes.get("ExcludedCargoTypes") );
			
			GenericXmlNode requiredAddonNode = gameDataNode.getNode("GameData", "RequiredAddon");
			requiredAddons = splitColonSeparatedIDList( getAttribute(requiredAddonNode, "Types") );
		}
	
	}

	static class TruckAddon extends ItemBased {

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
		final String[] excludedCargoTypes;
		final String[][] requiredAddons; // (ra[0][0] || ra[0][1] || ... ) && (ra[1][0] || ra[1][1] || ... ) && ...

		public TruckAddon(Item item) {
			super(item);
			if (!item.content.nodeName.equals("TruckAddon"))
				throw new IllegalStateException();
			
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
			// <TruckAddon> <GameData OriginalAddon=\"...\"> --> verwendet bei angewandelten AddOns --> im Spiel testen
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
			
			GenericXmlNode installSocketNode = gameDataNode.getNode("GameData", "InstallSocket"); // normal AddOn
			installSocket = getAttribute(installSocketNode, "Type");
			
			GenericXmlNode installSlotNode = gameDataNode.getNode("GameData", "InstallSlot"); // Cargo-Addon
			isCargo     = installSlotNode!=null;
			cargoLength = parseInt ( getAttribute(installSlotNode, "CargoLength") );
			cargoType   =            getAttribute(installSlotNode, "CargoType"  );
			cargoValue  = parseInt ( getAttribute(installSlotNode, "CargoValue" ) );
			
			//if (installSlotNode!=null)
			//	installSlotNode.attributes.forEach((key,value)->{
			//		unexpectedValues.add("Class[trucks] <TruckAddon> <GameData> <InstallSlot ####=\"...\">", key);
			//	});
			
		}

	}
}
