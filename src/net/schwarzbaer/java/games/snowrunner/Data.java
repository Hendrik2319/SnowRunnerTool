package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.schwarzbaer.gui.ValueListOutput;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;

public class Data {

	public static Data readInitialPAK(File initialPAK) {
		return PAKReader.readPAK(initialPAK, Data::new);
	}

	final XMLTemplateStructure rawdata;
	final HashMap<String,Language> languages;
	final TrucksTemplates trucksTemplate;
	final HashMap<String,WheelsDef> wheels;
	final HashMap<String,Truck> trucks;

	Data(XMLTemplateStructure rawdata) {
		this.rawdata = rawdata;
		languages = rawdata.languages;
		trucksTemplate = null;
		
		//MultiMap<String> wheelsTypes = new MultiMap<>();
		wheels = new HashMap<>();
		Class_ wheelsClass = rawdata.classes.get("wheels");
		wheelsClass.items.forEach((name,item)->{
			//wheelsTypes.add(item.content.nodeName, item.filePath);
			if (item.isMainItem() && item.content.nodeName.equals("TruckWheels"))
				wheels.put(name+".xml", new WheelsDef(item));
		});
		//System.err.println("WheelsTypes:");
		//wheelsTypes.printTo(System.err, str->str);
		
		trucks = new HashMap<>();
		Class_ trucksClass = rawdata.classes.get("trucks");
		trucksClass.items.forEach((name,item)->{
			if (item.isMainItem()) {
				trucks.put(name+".xml", new Truck(item, wheelType->{
					WheelsDef wheelsDef = wheels.get(wheelType+".xml");
					if (wheelsDef==null) return null;
					return wheelsDef.truckTires;
				}));
			}
		});
	}
	
	Data(ZipFile zipFile, ZipEntryTreeNode zipRoot) throws IOException {
		this.rawdata = null;
		Predicate<String> isXML = fileName->fileName.endsWith(".xml");
		
		languages = new HashMap<>();
		HashMap<String, Language> languages2 = languages;
		readLanguages(zipFile, zipRoot, languages2);
		
		ZipEntryTreeNode trucksTemplateNode = zipRoot.getSubFile ("[media]\\_templates\\", "trucks.xml");
		trucksTemplate = readXMLEntry(zipFile, trucksTemplateNode, (name,doc)->TrucksTemplates.readFromXML(name, doc));
		//if (trucksTemplate==null)
		//	System.out.printf("TrucksTemplate: <null>%n");
		//else {
		//	System.out.printf("TrucksTemplate:%n");
		//	System.out.println(trucksTemplate.toString());
		//}
		
		wheels = new HashMap<>();
		ZipEntryTreeNode[] defaultWheelNodes = zipRoot.getSubFiles("[media]\\classes\\wheels\\", isXML);
		readXMLEntries(zipFile, defaultWheelNodes, wheels, "wheels", (name,doc)->WheelsDef.readFromXML(name, null, doc, trucksTemplate));
		
		ZipEntryTreeNode[] dlcNodes = zipRoot.getSubFolders("[media]\\_dlc");
		for (ZipEntryTreeNode dlcNode:dlcNodes) {
			ZipEntryTreeNode[] wheelNodes = dlcNode.getSubFiles("classes\\wheels\\", isXML);
			if (wheelNodes!=null)
				readXMLEntries(zipFile, wheelNodes, wheels, "wheels", (name,doc)->WheelsDef.readFromXML(name, dlcNode.name, doc, trucksTemplate));
		}
		
		trucks = new HashMap<>();
		ZipEntryTreeNode[] defaultTruckNodes = zipRoot.getSubFiles("[media]\\classes\\trucks\\", isXML);
		readXMLEntries(zipFile, defaultTruckNodes, trucks, "trucks", (name,doc)->Truck.readFromXML(name, null, doc, trucksTemplate, wheels));
		
		for (ZipEntryTreeNode dlcNode:dlcNodes) {
			ZipEntryTreeNode[] truckNodes = dlcNode.getSubFiles("classes\\trucks\\", isXML);
			if (truckNodes!=null)
				readXMLEntries(zipFile, truckNodes, trucks, "trucks", (name,doc)->Truck.readFromXML(name, dlcNode.name, doc, trucksTemplate, wheels));
		}
		
	}

	static class DLC {
		final String name;
		final HashMap<String,WheelsDef> wheels;
		final HashMap<String,Truck> trucks;

		public DLC(String name) {
			this.name = name;
			wheels = new HashMap<>();
			trucks = new HashMap<>();
		}
		
	}
	
	interface ParseXMLFunction<ValueType> {
		ValueType parse(String name, Document doc) throws ParseException;
	}
	
	private <ValueType> ValueType readXMLEntry(ZipFile zipFile, ZipEntryTreeNode node, ParseXMLFunction<ValueType> readXML) throws IOException {
		return readEntry(zipFile, node, (name,input)->{
			try {
				return readXML.parse(name, XML.parseUTF8(input,content->"<root>"+content+"</root>"));
			} catch (ParseException ex) {
				System.err.printf("[%s] ParseException: %s%n", name, ex.getMessage());
				//ex.printStackTrace();
				return null;
			}
		});
	}
	
	private <ValueType> void readXMLEntries(ZipFile zipFile, ZipEntryTreeNode[] nodes, HashMap<String,ValueType> targetMap, String targetMapLabel, ParseXMLFunction<ValueType> readXML) throws IOException {
		readEntries(zipFile, nodes, targetMap, targetMapLabel, (name,input)->{
			try {
				return readXML.parse(name, XML.parseUTF8(input,content->"<root>"+content+"</root>"));
			} catch (ParseException ex) {
				System.err.printf("[%s] ParseException: %s%n", name, ex.getMessage());
				//ex.printStackTrace();
				return null;
			}
		});
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
	
	static class TrucksTemplates {

		final String xmlName;
		final HashMap<String, WheelFriction> wheelFrictionTemplates;
		final HashMap<String, TruckTire> truckTireTemplates;

		TrucksTemplates(String xmlName, Document doc) throws ParseException {
			this.xmlName = xmlName;
			
			Node rootNode = XML.getChild(doc,"root");
			if (rootNode==null)
				throw new ParseException("Can't find <root> node.");
			
			Node templatesNode = XML.getChild(rootNode,"_templates");
			if (templatesNode==null)
				throw new ParseException("Can't find <_templates> node.");
			
			wheelFrictionTemplates = new HashMap<>();
			Node wheelFrictionNode = XML.getChild(templatesNode,"WheelFriction");
			if (wheelFrictionNode!=null)
				XML.forEachChild(wheelFrictionNode, node->{
					String id = node.getNodeName();
					WheelFriction value = new WheelFriction(id,node);
					wheelFrictionTemplates.put(id, value);
				});
			
			truckTireTemplates = new HashMap<String,TruckTire>();
			Node truckTireNode = XML.getChild(templatesNode,"TruckTire");
			if (truckTireNode!=null)
				XML.forEachChild(truckTireNode, node->{
					String id = node.getNodeName();
					try {
						TruckTire value = new TruckTire(id,node,this);
						truckTireTemplates.put(id, value);
					} catch (ParseException ex) {
						System.err.printf("[%s] ParseException: %s%n", xmlName, ex.getMessage());
						// ex.printStackTrace();
					}
				});
		}

		@Override
		public String toString() {
			ValueListOutput out = new ValueListOutput();
			out.add(1, "XML Name", xmlName);
			out.add(1, "Tire Types", wheelFrictionTemplates.size());
			Vector<String> ids = new Vector<>(wheelFrictionTemplates.keySet());
			ids.sort(null);
			for (int i=0; i<ids.size(); i++) {
				String id = ids.get(i);
				out.add(2, String.format("[%d] %s", i+1, id));
				WheelFriction tireType = wheelFrictionTemplates.get(id);
				tireType.printValues(out,3);
			}
			return out.generateOutput();
		}

		static TrucksTemplates readFromXML(String name, Document doc) throws ParseException {
			return new TrucksTemplates(name, doc);
		}
	}

	static class WheelFriction {
	
		final String id;
		final String name_StringID;
		final Float frictionHighway;
		final Float frictionOffroad;
		final Float frictionMud;
		final boolean onIce;
		final WheelFriction template;

		WheelFriction(GenericXmlNode node) {
			id = null;
			template = null;
			name_StringID   =             node.attributes.get("UiName");
			frictionHighway = parseFloat( node.attributes.get("BodyFrictionAsphalt") );
			frictionOffroad = parseFloat( node.attributes.get("BodyFriction"       ) );
			frictionMud     = parseFloat( node.attributes.get("SubstanceFriction"  ) );
			onIce           = parseBool ( node.attributes.get("IsIgnoreIce"), false );
		}

		WheelFriction(String id, Node node) { // as template
			this.id = id;
			template = null;
			
//			BodyFriction="2.0"
//			BodyFrictionAsphalt="0.9"
//			IsIgnoreIce="true"
//			SubstanceFriction="1.1"
//			UiName="UI_TIRE_TYPE_CHAINS_NAME"
			
			name_StringID   = XML.getAttribute(node,"UiName");
			frictionHighway = parseFloat( XML.getAttribute(node,"BodyFrictionAsphalt") );
			frictionOffroad = parseFloat( XML.getAttribute(node,"BodyFriction"       ) );
			frictionMud     = parseFloat( XML.getAttribute(node,"SubstanceFriction"  ) );
			onIce           = parseBool ( XML.getAttribute(node,"IsIgnoreIce"), false );
		}
		
		WheelFriction(Node node, HashMap<String,WheelFriction> templates) throws ParseException { // based on template
			this.id = null;
			
			// <WheelFriction _template="Mudtires" BodyFriction="3.0" SubstanceFriction="8.0" />
			
			String templateID = XML.getAttribute(node,"_template");
			if (templateID==null)
				template = null;
			else {
				template = templates.get(templateID);
				if (template==null)
					throw new ParseException("Can't find template \"%s\" for current <WheelFriction> node.", templateID);
			}
			
			name_StringID   = XML.hasAttribute(node,"UiName"             ) ? XML.getAttribute(node,"UiName")                            : template==null ? null  : template.name_StringID;
			frictionHighway = XML.hasAttribute(node,"BodyFrictionAsphalt") ? parseFloat( XML.getAttribute(node,"BodyFrictionAsphalt") ) : template==null ? null  : template.frictionHighway;
			frictionOffroad = XML.hasAttribute(node,"BodyFriction"       ) ? parseFloat( XML.getAttribute(node,"BodyFriction"       ) ) : template==null ? null  : template.frictionOffroad;
			frictionMud     = XML.hasAttribute(node,"SubstanceFriction"  ) ? parseFloat( XML.getAttribute(node,"SubstanceFriction"  ) ) : template==null ? null  : template.frictionMud    ;
			onIce           = XML.hasAttribute(node,"IsIgnoreIce"        ) ? parseBool ( XML.getAttribute(node,"IsIgnoreIce"), false  ) : template==null ? false : template.onIce;
		}

		void printValues(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "ID", id);
			out.add(indentLevel, "name [StringID]", name_StringID);
			out.add(indentLevel, "friction (highway)", frictionHighway);
			out.add(indentLevel, "friction (offroad)", frictionOffroad);
			out.add(indentLevel, "friction (mud)"    , frictionMud);
			out.add(indentLevel, "onIce", onIce);
			if (template!=null) {
				out.add(indentLevel, "template");
				template.printValues(out, indentLevel+1);
			}
		}
		
	}

	static class ParentFile {

		final String file;

		ParentFile(Node node) {
			file = XML.getAttribute(node,"File");
		}
	}

	static class WheelsDef {

		final ParentFile parent;
		final String xmlName;
		final String dlcName;
		final Vector<TruckTire> truckTires;

		public WheelsDef(Item item) {
			parent = null;
			xmlName = item.name+".xml";
			dlcName = item.dlcName;
			
			truckTires = new Vector<>();
			GenericXmlNode[] truckTireNodes = item.content.getNodes("TruckWheels","TruckTires","TruckTire");
			for (int i=0; i<truckTireNodes.length; i++)
				truckTires.add(new TruckTire(truckTireNodes[i], xmlName, i, dlcName));
		}

		WheelsDef(String xmlName, String dlcName, Document doc, TrucksTemplates trucksTemplates) throws ParseException {
			this.xmlName = xmlName;
			this.dlcName = dlcName;
			
			Node rootNode = XML.getChild(doc,"root");
			if (rootNode==null)
				throw new ParseException("Can't find <root> node.");
			
			Node parentNode = XML.getChild(rootNode,"_parent");
			if (parentNode!=null)
				parent = new ParentFile(parentNode);
			else
				parent = null;
			
			HashMap<String,TruckTire> localTruckTireTemplates = new HashMap<>();
			Node templatesNode = XML.getChild(rootNode,"_templates");
			if (templatesNode!=null) {
				Node truckTireTemplatesNode = XML.getChild(templatesNode,"TruckTire");
				if (truckTireTemplatesNode!=null) {
					XML.forEachChild(truckTireTemplatesNode, node->{
						String id = node.getNodeName();
						try {
							TruckTire value = new TruckTire(id,node,trucksTemplates);
							localTruckTireTemplates.put(id, value);
						} catch (ParseException ex) {
							System.err.printf("[%s] ParseException: %s%n", xmlName, ex.getMessage());
							//ex.printStackTrace();
						}
					});
				}
			}
			
			truckTires = new Vector<>();
			Node truckWheelsNode = XML.getChild(rootNode,"TruckWheels"); // for normal trucks
			if (truckWheelsNode!=null) {
				
				Node truckTiresNode = XML.getChild(truckWheelsNode,"TruckTires");
				if (truckTiresNode!=null) {
					Node[] truckTireNodes = XML.getChildren(truckTiresNode, "TruckTire");
					for (int i=0; i<truckTireNodes.length; i++) {
						Node node = truckTireNodes[i];
						truckTires.add(new TruckTire(node,this.xmlName,i,this.dlcName,trucksTemplates,localTruckTireTemplates));
					}
				}
				
			} else {
				Node truckWheelNode = XML.getChild(rootNode,"TruckWheel"); // for trailers
				if (truckWheelNode!=null)
					truckTires.add(new TruckTire(truckWheelNode,this.xmlName,0,this.dlcName,trucksTemplates));
			}
			
		}
		
		static WheelsDef readFromXML(String name, String dlcName, Document doc, TrucksTemplates trucksTemplates) throws ParseException {
			return new WheelsDef(name, dlcName, doc, trucksTemplates);
		}
	
	}

	static class TruckTire {
	
		final String id;
		final TruckTire template;
		final String definingXML;
		final int indexInDef;
		final String dlc;
		final WheelFriction wheelFriction;
		final GameData gameData;
	
		TruckTire(GenericXmlNode node, String definingXML, int indexInDef, String dlc) {
			this.id = null;
			this.template = null;
			this.definingXML = definingXML;
			this.indexInDef = indexInDef;
			this.dlc = dlc;
			this.wheelFriction = new WheelFriction( node.getNode("TruckTire", "WheelFriction") );
			this.gameData      = new GameData     ( node.getNode("TruckTire", "GameData"     ) );
		}

		private TruckTire(String id, TruckTire template, Node node, String definingXML, int indexInDef, String dlc, TrucksTemplates trucksTemplates) throws ParseException { // base constructor
			this.id = id;
			this.template = template;
			this.definingXML = definingXML;
			this.indexInDef = indexInDef;
			this.dlc = dlc;
			
			Node wheelFrictionNode = XML.getChild(node,"WheelFriction");
			if (wheelFrictionNode!=null)
				wheelFriction = new WheelFriction(wheelFrictionNode, trucksTemplates.wheelFrictionTemplates);
			
			else if (this.template!=null)
				wheelFriction = this.template.wheelFriction;
			
			else
				wheelFriction = null;
			
			Node gameDataNode = XML.getChild(node,"GameData");
			if (gameDataNode!=null)
				gameData = new GameData(gameDataNode);
			
			else if (this.template!=null)
				gameData = this.template.gameData;
			
			else
				gameData = null;
		}
		
		TruckTire(String id, Node node, TrucksTemplates trucksTemplates) throws ParseException { // as template
			this(id, null, node, null, 0, null, trucksTemplates);
		}
	
		TruckTire(Node node, String definingXML, int indexInDef, String dlc, TrucksTemplates trucksTemplates, HashMap<String, TruckTire> localTruckTireTemplates) throws ParseException { // as truck tire
			// <TruckTire _template="Highway" Mesh="wheels/tire_medium_highway_double_1" Name="highway_1">
			this(null, getTemplate(node, localTruckTireTemplates, trucksTemplates.truckTireTemplates), node, definingXML, indexInDef, dlc, trucksTemplates);
		}
	
		TruckTire(Node node, String definingXML, int indexInDef, String dlc, TrucksTemplates trucksTemplates) throws ParseException { // as trailer tire
			this(null, null, node, definingXML, indexInDef, dlc, trucksTemplates);
		}
	
		private static TruckTire getTemplate(Node node, HashMap<String, TruckTire> map1, HashMap<String, TruckTire> map2) throws ParseException {
			String templateID = XML.getAttribute(node,"_template");
			if (templateID==null) return null;
			
			TruckTire template = null;
			if (template==null) template = map1.get(templateID);
			if (template==null) template = map2.get(templateID);
			if (template==null) throw new ParseException("Can't find template \"%s\" for current <TruckTire> node.", templateID);
			
			return template;
		}
	
		void printValues(ValueListOutput out, int indentLevel) {
			out.add(indentLevel, "ID", id);
			out.add(indentLevel, "template", template==null ? null : template.id);
			out.add(indentLevel, "wheelFriction");
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

			GameData(Node node) {
				price               = parseInt (XML.getAttribute(node,"Price"));
				unlockByExploration = parseBool(XML.getAttribute(node,"UnlockByExploration"));
				unlockByRank        = parseInt (XML.getAttribute(node,"UnlockByRank"));
				
				Node uiDescNode = XML.getChild(node,"UiDesc");
				if (uiDescNode!=null) {
					description_StringID = XML.getAttribute(uiDescNode,"UiDesc");
					name_StringID        = XML.getAttribute(uiDescNode,"UiName");
					
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
		
		interface GetTruckTires {
			Vector<TruckTire> get(String id) throws ParseException;
		}
		
		Truck(Item item, Function<String, Vector<TruckTire>> getTruckTires) {
			if (!item.className.equals("trucks"))
				throw new IllegalStateException();
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

		Truck(String xmlName, String dlcName, Document doc, GetTruckTires getTruckTires) throws ParseException {
			this.xmlName = xmlName;
			this.dlcName = dlcName;
			
			Node rootNode = XML.getChild(doc,"root");
			if (rootNode==null)
				throw new ParseException("Can't find <root> node.");
			
			Node truckNode = XML.getChild(rootNode,"Truck");
			if (truckNode==null)
				throw new ParseException("Can't find <Truck> node.");
			
			Node truckDataNode = XML.getChild(truckNode,"TruckData");
			if (truckDataNode==null)
				throw new ParseException("Can't find <TruckData> node.");
			
			type = XML.getAttribute(truckDataNode,"TruckType");
			
			compatibleWheels = new Vector<CompatibleWheel>();
			Node[] compatibleWheelNodes = XML.getChildren(truckDataNode,"CompatibleWheels");
			for (int i=0; i<compatibleWheelNodes.length; i++) {
				Node node = compatibleWheelNodes[i];
				CompatibleWheel cw = new CompatibleWheel(node, getTruckTires);
				compatibleWheels.add(cw);
				if (cw.truckTires==null)
					System.out.printf("[%s] CompatibleWheels[%d]<%s> -> <null> tire list%n", this.xmlName, i, cw.type);
				else if (cw.truckTires.isEmpty())
					System.out.printf("[%s] CompatibleWheels[%d]<%s> -> empty tire list%n", this.xmlName, i, cw.type);
			}
			
			Node gameDataNode = XML.getChild(truckNode,"GameData");
			if (gameDataNode==null)
				throw new ParseException("Can't find <GameData> node.");
			
			country = XML.getAttribute(gameDataNode,"Country");
			price   = parseInt(XML.getAttribute(gameDataNode,"Price"));
			unlockByExploration = parseBool(XML.getAttribute(gameDataNode,"UnlockByExploration"));
			unlockByRank        = parseInt (XML.getAttribute(gameDataNode,"UnlockByRank"));
			
			Node uiDescNode = XML.getChild(gameDataNode,"UiDesc");
			if (uiDescNode==null)
				throw new ParseException("Can't find <UiDesc> node.");
			
			description_StringID = XML.getAttribute(uiDescNode,"UiDesc");
			name_StringID = XML.getAttribute(uiDescNode,"UiName");
			
			expandedCompatibleWheels = ExpandedCompatibleWheel.expand(compatibleWheels);
		}

		static Truck readFromXML(String name, String dlcName, Document doc, TrucksTemplates trucksTemplate, HashMap<String, WheelsDef> wheels) throws ParseException {
			return new Truck(name, dlcName, doc, type->{
				WheelsDef wheelsDef = wheels.get(type+".xml");
				return getTruckTires(wheelsDef, wheels);
			});
		}

		private static Vector<TruckTire> getTruckTires(WheelsDef wheelsDef, HashMap<String, WheelsDef> wheels) throws ParseException {
			if (wheelsDef!=null && wheelsDef.parent!=null) {
				WheelsDef parentWheelsDef = wheels.get(wheelsDef.parent.file+".xml");
				if (parentWheelsDef==null)
					throw new ParseException("Can't find parent file \"%s.xml\" to WheelsDef \"%s\"node.", wheelsDef.parent.file, wheelsDef.xmlName);
				
				Vector<TruckTire> truckTires = new Vector<>();
				if (wheelsDef      .truckTires!=null) truckTires.addAll(wheelsDef      .truckTires);
				if (parentWheelsDef.truckTires!=null) truckTires.addAll(parentWheelsDef.truckTires);
				return truckTires;
				
			} else
				return wheelsDef!=null ? wheelsDef.truckTires : null;
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

			CompatibleWheel(Node node, GetTruckTires getTruckTires) throws ParseException {
				scale = parseFloat( XML.getAttribute(node, "Scale") );
				type  = XML.getAttribute(node, "Type");
				truckTires = type==null ? null : getTruckTires.get(type);
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
						String label = String.format("[%d] ID:%s, Template:%s", i+1, truckTire.id, truckTire.template==null ? null : truckTire.template.id);
						out.add(indentLevel, label );
						truckTire.printValues(out, indentLevel+1);
					}
				else
					out.add(indentLevel, "[No TruckTires]" );
				
			}
		}
	}
	
	private static class ParseException extends Exception {
		private static final long serialVersionUID = -5149129627690101282L;
		
		ParseException(String msg) {
			super(msg);
		}
		ParseException(String format, Object... objects) {
			super(String.format(format, objects));
		}
	}
}
