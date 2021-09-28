package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
	
	@SuppressWarnings("unused")
	private static class HashSetMap<MapKeyType,SetValueType> extends HashMap<MapKeyType,HashSet<SetValueType>> {
		private static final long serialVersionUID = -6897179951968079373L;
		void add(MapKeyType key, SetValueType value) {
			if (key==null) new IllegalArgumentException();
			HashSet<SetValueType> set = get(key);
			if (set==null) put(key, set = new HashSet<>());
			set.add(value);
		}
	}

	final XMLTemplateStructure rawdata;
	final HashMap<String,Language> languages;
	final HashMap<String,WheelsDef> wheels;
	final HashMap<String,Truck> trucks;

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
//		HashSetMap<String,String> truckAttributes = new HashSetMap<>();
//		HashSetMap<String,String> truckAddonAttributes = new HashSetMap<>();
		trucks = new HashMap<>();
		Class_ trucksClass = rawdata.classes.get("trucks");
		trucksClass.items.forEach((name,item)->{
			if (!item.content.nodeName.equals("Truck")) return;
			String type = item.content.attributes.get("Type");
			if (type!=null) return;
//			if (item.content.nodeName.equals("Truck"))
//				item.content.attributes.forEach(truckAttributes::add);
//			if (item.content.nodeName.equals("TruckAddon"))
//				item.content.attributes.forEach(truckAddonAttributes::add);
			trucks.put(name, new Truck(item, wheelType->{
				WheelsDef wheelsDef = wheels.get(wheelType);
				if (wheelsDef==null) return null;
				return wheelsDef.truckTires;
			}));
		});
//		System.out.printf("Truck      Attributes: %s%n", truckAttributes.toString());
//		System.out.printf("TruckAddon Attributes: %s%n", truckAddonAttributes.toString());
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

	static class WheelsDef {

		final String name;
		final String xmlName;
		final String dlcName;
		final Vector<TruckTire> truckTires;

		public WheelsDef(Item item) {
			name    = item.name;
			xmlName = item.name+".xml";
			dlcName = item.dlcName;
			
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

	static class Truck {
		
		final String id;
		final String xmlName;
		final String dlcName;
		final String type;
		final String country;
		final Integer price;
		final Boolean unlockByExploration;
		final Integer unlockByRank;
		final String description_StringID;
		final String name_StringID;
		final Vector<CompatibleWheel> compatibleWheels;
		final Vector<ExpandedCompatibleWheel> expandedCompatibleWheels;
		
		Truck(Item item, Function<String, Vector<TruckTire>> getTruckTires) {
			if (!item.className.equals("trucks"))
				throw new IllegalStateException();
			id      = item.name;
			xmlName = item.name+".xml";
			dlcName = item.dlcName;
			
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
			
			compatibleWheels = new Vector<>();
			GenericXmlNode[] compatibleWheelsNodes = truckDataNode.getNodes("TruckData", "CompatibleWheels");
			for (GenericXmlNode compatibleWheelsNode : compatibleWheelsNodes)
				compatibleWheels.add(new CompatibleWheel(compatibleWheelsNode, getTruckTires));
			
			expandedCompatibleWheels = ExpandedCompatibleWheel.expand(compatibleWheels);
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

			static Vector<ExpandedCompatibleWheel> expand(Vector<CompatibleWheel> compatibleWheels) {
				Vector<ExpandedCompatibleWheel> expanded = new Vector<>();
				
				for (CompatibleWheel cw:compatibleWheels)
					if (cw.truckTires!=null)
						for (TruckTire tire : cw.truckTires)
							expanded.add( new ExpandedCompatibleWheel(cw.scale,tire) );
				
				return expanded;
			}

			Integer getSize() {
				return CompatibleWheel.computeSize_inch(scale);
			}
		}

		static class CompatibleWheel {
			
			final Float scale;
			final String type;
			final Vector<TruckTire> truckTires;
			
			CompatibleWheel(GenericXmlNode node, Function<String,Vector<TruckTire>> getTruckTires) {
				if (!node.nodeName.equals("CompatibleWheels"))
					throw new IllegalStateException();
				
				scale = parseFloat( node.attributes.get("Scale") );
				type  =             node.attributes.get("Type");
				truckTires = type==null ? null : getTruckTires.apply(type);
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
	}
}
