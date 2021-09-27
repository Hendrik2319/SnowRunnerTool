package net.schwarzbaer.java.games.snowrunner;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

import javax.swing.JButton;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.schwarzbaer.gui.TextAreaDialog;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode.InheritRemoveException;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode.Source;
import net.schwarzbaer.system.DateTimeFormatter;

@SuppressWarnings("unused")
class XMLTemplateStructure {
	
	private static boolean SHOW_UNEXPECTED_TEXT = false; 

	private static TestingGround testingGround;
	
	final HashMap<String, Templates> globalTemplates;
	final HashMap<String, Class_> classes;
	final Vector<String> ignoredFiles;
	final HashMap<String, Data.Language> languages;

	static XMLTemplateStructure readPAK(File pakFile, Window mainWindow) {
		return PAKReader.readPAK(pakFile, (zipFile, zipRoot) -> {
			try {
				return new XMLTemplateStructure(zipFile, zipRoot, mainWindow);
			} catch (EntryStructureException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			return null;
		});
	}
	
	XMLTemplateStructure(ZipFile zipFile, ZipEntryTreeNode zipRoot, Window mainWindow) throws IOException, EntryStructureException, ParseException {
		
		languages = new HashMap<>();
		Data.readLanguages(zipFile, zipRoot, languages);
		
		ZipEntryTreeNode contentRootFolder = zipRoot.getSubFolder("[media]");
		if (contentRootFolder==null)
			throw new EntryStructureException("Found no content root folder \"[media]\" in \"%s\"", zipFile.getName());
		
		testingGround = new TestingGround(zipFile, contentRootFolder, mainWindow);
		testingGround.testContentRootFolder();
		
		ClassStructur classStructur = new ClassStructur(contentRootFolder);
		
		ignoredFiles = new Vector<>();
		globalTemplates = readGlobalTemplates(zipFile, classStructur.templates);
		if (globalTemplates==null) throw new IllegalStateException();
		
		classes = new HashMap<>();
		for (ClassStructur.StructClass structClass : classStructur.classes.values())
			classes.put(structClass.className, new Class_(zipFile, structClass, globalTemplates, ignoredFiles));
		
		if (!ignoredFiles.isEmpty()) {
			System.err.printf("IgnoredFiles: [%d]%n", ignoredFiles.size());
			for (int i=0; i<ignoredFiles.size(); i++)
				System.err.printf("   [%d] %s%n", i+1, ignoredFiles.get(i));
		}
		
		testingGround.writeToFile("TestingGround.results.txt");
	}

	private HashMap<String,Templates> readGlobalTemplates(ZipFile zipFile, HashMap<String,ZipEntryTreeNode> templates) throws EntryStructureException, IOException, ParseException {
		if (zipFile==null) throw new IllegalArgumentException();
		if (templates==null) throw new IllegalArgumentException();
		
		HashMap<String,Templates> globalTemplates = new HashMap<>();
		for (String templateName : templates.keySet()) {
			ZipEntryTreeNode fileNode = templates.get(templateName);
			readGlobalTemplates_(zipFile, globalTemplates, templateName, fileNode);
		}
		
		return globalTemplates;
	}

	private void readGlobalTemplates_(ZipFile zipFile, HashMap<String, Templates> globalTemplates, String templateName, ZipEntryTreeNode fileNode)
			throws EntryStructureException, IOException, ParseException {
		//System.out.printf("Read template \"%s\" ...%n", fileNode.getPath());
		
		NodeList nodes = readXML(zipFile, fileNode);
		if (nodes==null) {
			System.err.printf("Can't read xml file \"%s\" --> Templates file will be ignored%n", fileNode.getPath());
			ignoredFiles.add(fileNode.getPath());
			return;
		}
		
		Node templatesNode = null; 
		for (Node node:XML.makeIterable(nodes)) {
			//showNode(node);
			if (node.getNodeType()==Node.COMMENT_NODE) {
				// is Ok, do nothing
					
			} else if (isEmptyTextNode(node, ()->String.format("template file \"%s\"", fileNode.getPath()))) {
				// is Ok, do nothing
				
			} else if (node.getNodeType()==Node.ELEMENT_NODE) {
				if (!node.getNodeName().equals("_templates"))
					throw new ParseException("Found unexpected element node in template file \"%s\": %s", fileNode.getPath(), XML.toDebugString(node));
				if (templatesNode!=null)
					throw new ParseException("Found more than one <_templates> node in template file \"%s\"", fileNode.getPath());
				templatesNode = node;
				
			} else
				throw new ParseException("Found unexpected node in template file \"%s\": %s", fileNode.getPath(), XML.toDebugString(node));
		}
		if (templatesNode==null)
			throw new ParseException("Found no <_templates> node in template file \"%s\"", fileNode.getPath());
		
		globalTemplates.put(templateName, new Templates(templatesNode, null, fileNode, null));
		//System.out.printf("... done [read template]%n");
	}

	private static boolean isEmptyTextNode(Node node, Supplier<String> getNodeLabel) throws ParseException {
		if (node.getNodeType() != Node.TEXT_NODE)
			return false;
		
		String nodeValue = node.getNodeValue();
		if (nodeValue!=null && !nodeValue.trim().isEmpty() && SHOW_UNEXPECTED_TEXT)
			throwParseException(false, "Found unexpected text in %s: \"%s\" %s", getNodeLabel.get(), nodeValue.trim(), toHexString(nodeValue.getBytes()));
			
		return true;
	}

	private static void showNode(Node node) {
		String nodeValue = node.getNodeValue();
		String nodeType = XML.getNodeTypeStr(node.getNodeType());
		if (nodeValue==null)
			System.out.printf("<%s> [%s]%n", node.getNodeName(), nodeType);
		else
			System.out.printf("<%s> [%s]: \"%s\" -> trimmed -> \"%s\" %n", node.getNodeName(), nodeType, nodeValue, nodeValue==null ? null : nodeValue.trim());
	}

	private static void showNodes(NodeList nodes) {
		for (Node node:XML.makeIterable(nodes))
			showNode(node);
	}
	
	private static void showBytes(ZipFile zipFile, ZipEntryTreeNode fileNode, int length) {
		
		try (BufferedInputStream in = new BufferedInputStream(zipFile.getInputStream(fileNode.entry));) {
			byte[] bytes = new byte[length];
			int n, pos=0;
			while ( pos<bytes.length && (n=in.read(bytes,pos,bytes.length-pos))>=0 ) pos += n;
			if (pos<bytes.length) bytes = Arrays.copyOfRange(bytes, 0, pos);
			System.err.printf("First %d bytes of file \"%s\":%n  %s%n", bytes.length, fileNode.getPath(), toHexString(bytes));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static String toHexString(byte[] bytes) {
		String str = String.join(", ", (Iterable<String>)()->new Iterator<String>() {
			private int index = 0;
			@Override public boolean hasNext() { return index<bytes.length; }
			@Override public String next() { return String.format("0x%02X", bytes[index++]); }
			
		});
		return str.isEmpty() ? "[]" : String.format("[ %s ]", str);
	}

	private static NodeList readXML(ZipFile zipFile, ZipEntryTreeNode fileNode) throws EntryStructureException, IOException {
		if (zipFile ==null) throw new IllegalArgumentException();
		if (fileNode==null) throw new IllegalArgumentException();
		if (!fileNode.isfile())
			throw new EntryStructureException("Given ZipEntryTreeNode isn't a file: %s", fileNode.getPath());
		if (!fileNode.name.endsWith(".xml"))
			throw new EntryStructureException("Found a Non XML File: %s", fileNode.getPath());
		
		try {
			return readXML(zipFile.getInputStream(fileNode.entry));
		} catch (ParseException e) {
			System.err.printf("ParseException while basic reading of \"%s\":%n   %s%n", fileNode.getPath(), e.getMessage());
			return null;
		}
	}
	
	private static NodeList readXML(InputStream input) throws ParseException {
		Document doc = XML.parseUTF8(input,content->String.format("<%1$s>%2$s</%1$s>", "BracketNode", content));
		if (doc==null) return null;
		Node bracketNode = null;
		for (Node node : XML.makeIterable(doc.getChildNodes())) {
			if (node.getNodeType() != Node.ELEMENT_NODE)
				throw new ParseException("Found unexpected node in read XML document: wrong node type");
			if (!node.getNodeName().equals("BracketNode"))
				throw new ParseException("Found unexpected node in read XML document: node name != <BracketNode>");
			bracketNode = node;
		}
		if (bracketNode==null)
			throw new ParseException("Found no <BracketNode> in read XML document");
		return bracketNode.getChildNodes();
	}
	
	private static class ClassStructur {
	
		final HashMap<String, ZipEntryTreeNode> templates;
		final HashMap<String, StructClass> classes;
	
		ClassStructur(ZipEntryTreeNode contentRootFolder) throws EntryStructureException {
			templates = scanTemplates(contentRootFolder.getSubFolder("_templates"));
			classes = new HashMap<>();
			scanClasses(classes,null,contentRootFolder.getSubFolder("classes"   ));
			scanDLCs   (classes,     contentRootFolder.getSubFolder("_dlc"      ));
		}
		
		private static void scanDLCs(HashMap<String, StructClass> classes, ZipEntryTreeNode dlcsFolder) throws EntryStructureException {
			if ( dlcsFolder==null) throw new EntryStructureException("No DLCs folder");
			if (!dlcsFolder.files  .isEmpty()) throw new EntryStructureException("Found unexpected files in DLCs folder \"%s\"", dlcsFolder.getPath());
			if ( dlcsFolder.folders.isEmpty()) throw new EntryStructureException("Found no DLC folders in DLCs folder \"%s\"", dlcsFolder.getPath());
			
			for (ZipEntryTreeNode dlcNode:dlcsFolder.folders.values()) {
				String dlcName = dlcNode.name;
				if (!dlcNode.files.isEmpty()) throw new EntryStructureException("Found unexpected files in DLC folder \"%s\"", dlcNode.getPath());
				
				ZipEntryTreeNode classesFolder = null;
				for (ZipEntryTreeNode subFolder:dlcNode.folders.values()) {
					if (!subFolder.name.equals("classes"))
						throw new EntryStructureException("Found unexpected folder (\"%s\") in DLC folder \"%s\"", subFolder.name, dlcNode.getPath());
					if (classesFolder != null)
						throw new EntryStructureException("Found dublicate \"classes\" folder in DLC folder \"%s\"", dlcNode.getPath());
					classesFolder = subFolder;
				}
				if (classesFolder == null)
					throw new EntryStructureException("Found no \"classes\" folder in DLC folder \"%s\"", dlcNode.getPath());
				
				scanClasses(classes,dlcName, classesFolder);
			}
		}
	
		private static void scanClasses(HashMap<String, StructClass> classes, String dlc, ZipEntryTreeNode classesFolder) throws EntryStructureException {
			if ( classesFolder==null) throw new EntryStructureException("No classes folder");
			if (!classesFolder.files  .isEmpty()) throw new EntryStructureException("Found unexpected files in classes folder \"%s\"", classesFolder.getPath());
			if ( classesFolder.folders.isEmpty()) throw new EntryStructureException("Found no sub folders in classes folder \"%s\"", classesFolder.getPath());
			
			for (ZipEntryTreeNode classFolder : classesFolder.folders.values()) {
				String className = classFolder.name;
				StructClass class_ = classes.get(className);
				if (class_==null) classes.put(className, class_ = new StructClass(className));
				scanClass(class_, dlc, classFolder);
			}
		}
		
		private static void scanClass(StructClass class_, String dlc, ZipEntryTreeNode classFolder) throws EntryStructureException {
			for (ZipEntryTreeNode subClassFolder : classFolder.folders.values()) {
				if (!subClassFolder.folders.isEmpty()) throw new EntryStructureException("Found unexpected folders in sub class folder \"%s\"", subClassFolder.getPath());
				if ( subClassFolder.files  .isEmpty()) throw new EntryStructureException("Found no item files in sub class folder \"%s\"", subClassFolder.getPath());
				
				scanItems(class_, dlc, subClassFolder.name, subClassFolder);
			}
			
			scanItems(class_, dlc, null, classFolder);
		}

		private static void scanItems(StructClass class_, String dlc, String subClassName, ZipEntryTreeNode folder) throws EntryStructureException {
			for (ZipEntryTreeNode itemFile : folder.files.values()) {
				String itemName = getXmlItemName(itemFile);
				StructItem otherItem = class_.items.get(itemName);
				if (otherItem!=null)
					throw new EntryStructureException("Found more than one item file with name \"%s\" in class folder \"%s\"", itemName, folder.getPath());
				class_.items.put(itemName, new StructItem(dlc, class_.className, subClassName, itemName, itemFile));
			}
		}

		private static HashMap<String,ZipEntryTreeNode> scanTemplates(ZipEntryTreeNode templatesFolder) throws EntryStructureException {
			if ( templatesFolder==null) throw new EntryStructureException("No templates folder");
			if (!templatesFolder.folders.isEmpty()) throw new EntryStructureException("Found unexpected folders in templates folder \"%s\"", templatesFolder.getPath());
			if ( templatesFolder.files  .isEmpty()) throw new EntryStructureException("Found no files in templates folder \"%s\"", templatesFolder.getPath());
			
			HashMap<String,ZipEntryTreeNode> templates = new HashMap<>();
			for (ZipEntryTreeNode templatesNode:templatesFolder.files.values()) {
				String itemName = getXmlItemName(templatesNode);
				if (templates.containsKey(itemName))
					throw new EntryStructureException("Found more than one templates file with name \"%s\" in templates folder \"%s\"", itemName, templatesFolder.getPath());
				templates.put(itemName, templatesNode);
			}
			return templates;
		}
	
		private static String getXmlItemName(ZipEntryTreeNode node) throws EntryStructureException {
			if (!node.name.endsWith(".xml"))
				throw new EntryStructureException("Found a Non XML File: %s", node.getPath());
			return node.name.substring(0, Math.max(0, node.name.length()-".xml".length()));
		}
	
		static class StructClass {
	
			final String className;
			final HashMap<String, StructItem> items;
	
			StructClass(String className) {
				this.className = className;
				items = new HashMap<>();
			}
		}
		
		static class StructItem {
	
			final String dlcName;
			final String className;
			final String subClassName;
			final String itemName;
			final ZipEntryTreeNode itemFile;
			final String itemFilePath;
	
			StructItem(String dlcName, String className, String subClassName, String itemName, ZipEntryTreeNode itemFile) {
				this.dlcName = dlcName;
				this.className = className;
				this.subClassName = subClassName;
				this.itemName = itemName;
				this.itemFile = itemFile;
				this.itemFilePath = itemFile.getPath();
			}
			
		}
		
	}

	private static class TestingGround {
		
		private final ZipFile zipFile;
		private final ZipEntryTreeNode contentRootFolder;
		private final HashMap<String,Vector<String>> parentRelations;
		private final HashMap<String,HashMap<String,Integer>> specialAttributes;
		private final Window mainWindow;

		TestingGround(ZipFile zipFile, ZipEntryTreeNode contentRootFolder, Window mainWindow) {
			this.zipFile = zipFile;
			this.contentRootFolder = contentRootFolder;
			this.mainWindow = mainWindow;
			parentRelations = new HashMap<>();
			specialAttributes = new HashMap<>();
			
		}

		void writeToFile(String filename) {
			
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
				
				out.printf("File: %s%n", zipFile.getName());
				out.printf("Date: %s%n", new DateTimeFormatter().getTimeStr(System.currentTimeMillis(), true, true, true, true, true));
				out.println();
				
				out.printf("[ParentRelations]%n");
				Vector<String> parents = new Vector<>(parentRelations.keySet());
				parents.sort(null);
				for (String p:parents) {
					out.printf("     Parent:   \"%s\"%n", p);
					String parentFolder = getFolderPath(p);
					String parentClass = getClass(p);
					Vector<String> children = parentRelations.get(p);
					children.sort(null);
					for (String c:children) {
						String childFolder = getFolderPath(c);
						String childClass = getClass(c);
						String marker1 = " ";
						if (!parentFolder.equals(childFolder)) marker1 = "#";
						String marker2 = " ";
						if (!parentClass.equals(childClass)) marker2 = "C";
						out.printf("%s %s     Child: \"%s\"%n", marker1, marker2, c);
					}
				}
				out.println();
				
				out.printf("[SpecialAttributes]%n");
				out.println(specialAttributesToString());
					
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}

		private String getClass(String path) {
			String str = "\\classes\\";
			
			int start = path.indexOf(str);
			if (start<0) return "";
			start += str.length();
			
			int end = path.indexOf("\\", start);
			if (end<0) end = path.length();
			
			return path.substring(start, end);
		}

		private String getFolderPath(String path) {
			int pos = path.lastIndexOf('\\');
			if (pos<0) return "";
			return path.substring(0, pos);
		}

		void addParentFileInfo(ZipEntryTreeNode itemFile, String parentFile) {
			if (itemFile==null) throw new IllegalArgumentException();
			if (parentFile==null) return;
			
			String filename = parentFile+".xml";
			String itemPath = itemFile.getPath();
			
			Vector<String> possibleParents = new Vector<>();
			contentRootFolder.traverseFiles(node->{
				if (node.name.equalsIgnoreCase(filename))
					possibleParents.add(node.getPath());
			});
			if (possibleParents.isEmpty())
				System.err.printf("Can't find parent \"%s\" for itme \"%s\"%n", parentFile, itemPath);
			else if (possibleParents.size()>1) {
				System.err.printf("Found more thasn one parent \"%s\" for itme \"%s\":%n", parentFile, itemPath);
				for (String p:possibleParents)
					System.err.printf("    \"%s\"%n", p);
			} else {
				String parent = possibleParents.get(0);
				Vector<String> children = parentRelations.get(parent);
				if (children==null) parentRelations.put(parent, children = new Vector<>());
				children.add(itemPath);
			}
		}

		void testContentRootFolder() {
			HashMap<String,ZipEntryTreeNode> fileNames = new HashMap<>();
			contentRootFolder.traverseFiles(fileNode->{
				
				//if (fileNode.getPath().equals("\\[media]\\classes\\trucks\\step_310e.xml"))
				//	showBytes(zipFile, fileNode, 30);
			
				// all files are XML
				if (!fileNode.name.endsWith(".xml"))
					System.err.printf("Found a Non XML File: %s%n", fileNode.getPath());
				
				// dublicate filenames
				//ZipEntryTreeNode existingNode = fileNames.get(fileNode.name);
				//if (existingNode!=null)
				//	System.err.printf("Found dublicate filenames:%n   \"%s\"%n   \"%s\"%n", existingNode.getPath(), fileNode.getPath());
				//else
				//	fileNames.put(fileNode.name,fileNode);
			});
			
			contentRootFolder.forEachChild(node->{
				if (node.isfile())
					System.err.printf("Found a File in conten root folder: %s%n", node.getPath());
				else {
					if (!node.name.equals("_dlc") && !node.name.equals("_templates") && !node.name.equals("classes"))
						System.err.printf("Found a unexpected subfolder in content root folder: %s%n", node.getPath());
				}
			});
		}

		void showCurrentState(GenericXmlNode parentTemplate, GenericXmlNode template, Node xmlNode, GenericXmlNode currentState, Templates templates, Source source, String reason) {
			JButton[] extraButtons = new JButton[] {
				SnowRunner.createButton("Show Special Attributes", true, e->{
					TextAreaDialog.showText(mainWindow, "Special Attributes", 600, 800, true, specialAttributesToString());
				})
			};
			GenericXmlNodeParsingStateDialog dlg = new GenericXmlNodeParsingStateDialog(mainWindow, parentTemplate, template, xmlNode, currentState, templates, source, reason, extraButtons);
			boolean toBreakPoint = dlg.showDialog();
			if (toBreakPoint) {
				System.out.println("BreakPoint");
			}
		}

		private String specialAttributesToString() {
			StringBuilder sb = new StringBuilder();
			Vector<String> attrNames = new Vector<>(specialAttributes.keySet());
			attrNames.sort(null);
			for (String attrName : attrNames) {
				HashMap<String, Integer> attrValuesMap = specialAttributes.get(attrName);
				Vector<String> attrValues = new Vector<>(attrValuesMap.keySet());
				attrValues.sort(null);
				sb.append(String.format("\"%s\": [%d]%n", attrName, attrValues.size()));
				for (String attrValue : attrValues)
					sb.append(String.format("   = \"%s\" (%dx)%n", attrValue, attrValuesMap.get(attrValue)));
			}
			return sb.toString();
		}

		void addSpecialAttribute(String attrName, String attrValue) {
			HashMap<String, Integer> values = specialAttributes.get(attrName);
			if (values == null) specialAttributes.put(attrName, values = new HashMap<>());
			Integer count = values.get(attrValue);
			if (count==null) count = 0;
			values.put(attrValue, count+1);
		}
		
	}

	private static class EntryStructureException extends Exception {
		private static final long serialVersionUID = -8853147512351787891L;
		EntryStructureException(String msg) { super(msg); }
		EntryStructureException(String format, Object... objects) { super(String.format(format, objects)); }
	}

	private static class ParseException extends Exception {
		private static final long serialVersionUID = 7047000831781614584L;
		ParseException(String msg) { super(msg); }
		ParseException(String format, Object... objects) { super(String.format(format, objects)); }
	}

	private static void throwParseException(boolean throwException, String format, Object... objects) throws ParseException {
		if (throwException)
			throw new ParseException(format, objects);
		System.err.printf(format+"%n", objects);
	}

	static class Class_ {
		
		final String name;
		final HashMap<String,Item> items;
	
		public Class_(ZipFile zipFile, ClassStructur.StructClass structClass, HashMap<String, Templates> globalTemplates, Vector<String> ignoredFiles) throws IOException, EntryStructureException, ParseException {
			if (zipFile==null) throw new IllegalArgumentException();
			if ( structClass==null) throw new IllegalArgumentException();
			if (globalTemplates==null) throw new IllegalArgumentException();
			if (ignoredFiles==null) throw new IllegalArgumentException();
			
			name = structClass.className;
			
			items = new HashMap<>();
			new ItemLoader(zipFile, structClass.items, globalTemplates, items, ignoredFiles).run();
		}
		
		private interface LoadMethod {
			Item readItem(ZipEntryTreeNode itemFile) throws IOException, EntryStructureException, ParseException;
		}
		
		private static class ItemLoader implements Item.ParentFinder {
			
			private final ZipFile zipFile;
			private final HashMap<String,ClassStructur.StructItem> structItems;
			private final HashMap<String, Templates> globalTemplates;
			private final HashMap<String, Item> loadedItems;
			private final HashSet<String> blockedItems;
			private final Vector<String> ignoredFiles;

			ItemLoader(ZipFile zipFile, HashMap<String,ClassStructur.StructItem> structItems, HashMap<String, Templates> globalTemplates, HashMap<String, Item> loadedItems, Vector<String> ignoredFiles) {
				this.zipFile = zipFile;
				this.structItems = structItems;
				this.globalTemplates = globalTemplates;
				this.loadedItems = loadedItems;
				this.ignoredFiles = ignoredFiles;
				blockedItems = new HashSet<>();
			}
			
			void run() throws IOException, EntryStructureException, ParseException {
				for (ClassStructur.StructItem structItem:structItems.values()) {
					if (!loadedItems.containsKey(structItem.itemName))
						readItem(structItem);
				}
			}

			private Item readItem(ClassStructur.StructItem structItem) throws IOException, EntryStructureException, ParseException {
				
				blockedItems.add(structItem.itemFilePath);
				
				Item item = Item.read(zipFile, structItem, globalTemplates, this);
				if (item == null) ignoredFiles.add(structItem.itemFilePath);
				else loadedItems.put(structItem.itemName, item);
				
				blockedItems.remove(structItem.itemFilePath);
				
				return item;
			}

			@Override
			public Item getParent(String parentFile) throws IOException, EntryStructureException, ParseException {
				Item parentItem = loadedItems.get(parentFile);
				if (parentItem!=null) return parentItem;
				
				ClassStructur.StructItem parentStructItem = structItems.get(parentFile);
				if (parentStructItem == null)
					throw new EntryStructureException("Can't find parent item \"%s\"", parentFile);
				
				if (blockedItems.contains(parentStructItem.itemFilePath))
					throw new EntryStructureException("Found \"parent loop\" for parent item \"%s\"", parentFile);
				
				parentItem = readItem(parentStructItem);
				if (parentItem==null)
					throw new EntryStructureException("Can't read parent item \"%s\"", parentFile);
				
				return parentItem;
			}
		}
		
		static class Item {
	
			final String name;
			final String filePath;
			final String dlcName;
			final String className;
			final String subClassName;
			final GenericXmlNode content;

			static Item read(ZipFile zipFile, ClassStructur.StructItem structItem, HashMap<String,Templates> globalTemplates, ParentFinder parentFinder) throws IOException, EntryStructureException, ParseException {
				if (globalTemplates==null) throw new IllegalArgumentException();
				if (structItem==null) throw new IllegalArgumentException();
				if (!structItem.itemFile.isfile()) throw new IllegalStateException();
				
				//System.out.printf("Read Item \"%s\" ...%n", itemFile.getPath());
				
				NodeList nodes = readXML(zipFile, structItem.itemFile);
				if (nodes==null) {
					System.err.printf("Can't read xml file \"%s\" --> Item will be ignored%n", structItem.itemFilePath);
					return null;
				}
				
				Item item = new Item(nodes, structItem, globalTemplates, parentFinder);
				
				//System.out.printf("... done [read item]%n");
				return item;
			}
			
			interface ParentFinder {
				Item getParent(String parentFile) throws IOException, EntryStructureException, ParseException;
			}
			
			Item(NodeList nodes, ClassStructur.StructItem structItem, HashMap<String,Templates> globalTemplates, ParentFinder parentFinder) throws IOException, EntryStructureException, ParseException {
				if (globalTemplates==null) throw new IllegalArgumentException();
				if (structItem==null) throw new IllegalArgumentException();
				if (nodes==null) throw new IllegalArgumentException();
				if (parentFinder==null) throw new IllegalArgumentException();
				
				this.name = structItem.itemName;
				this.filePath = structItem.itemFilePath;
				this.dlcName = structItem.dlcName;
				this.className = structItem.className;
				this.subClassName = structItem.subClassName;
				
				Node templatesNode = null;
				Node parentNode = null;
				Node contentNode = null;
				
				for (Node node:XML.makeIterable(nodes)) {
					if (node.getNodeType()==Node.COMMENT_NODE) {
						// is Ok, do nothing
							
					} else if (isEmptyTextNode(node, ()->String.format("item file \"%s\"", structItem.itemFilePath))) {
						// is Ok, do nothing
						
					} else if (node.getNodeType()==Node.ELEMENT_NODE) {
						
						switch (node.getNodeName()) {
						
						case "_templates":
							if (templatesNode!=null)
								throw new ParseException("Found more than one <_templates> node in item file \"%s\"", structItem.itemFilePath);
							templatesNode = node;
							break;
							
						case "_parent":
							if (parentNode!=null)
								throw new ParseException("Found more than one <_parent> node in item file \"%s\"", structItem.itemFilePath);
							parentNode = node;
							break;
							
						default: 
							if (contentNode!=null)
								throw new ParseException("Found more than one content node in item file \"%s\": <%s>, <%s>", structItem.itemFilePath, node.getNodeName(), contentNode.getNodeName());
							contentNode = node;
							break;
						}
						
					} else
						throw new ParseException("Found unexpected node in item file \"%s\": %s", structItem.itemFilePath, XML.toDebugString(node));
				}
				if (contentNode==null)
					throw new ParseException("Found no content node in item file \"%s\"", structItem.itemFilePath);
				
				Templates.OdditiesHandler templatesOdditiesHandler = new Templates.OdditiesHandler();
				Templates localTemplates;
				if (templatesNode==null)
					// if no <_templates> node defined but "_template" attributes are used in sub nodes
					localTemplates = new Templates(structItem.className, globalTemplates, structItem.itemFile, templatesOdditiesHandler);
				else
					localTemplates = new Templates(templatesNode, globalTemplates, structItem.itemFile, templatesOdditiesHandler);
				
				if (templatesOdditiesHandler.parentNode!=null) {
					if (parentNode!=null)
						throw new ParseException("Found an oddity (<_parent> node in <_templates> node) in file \"%s\" but also a <_parent> node outside of <_templates> node%n", structItem.itemFilePath);
					parentNode = templatesOdditiesHandler.parentNode;
				}
				
				String parentFile = getParentFile(parentNode, structItem.itemFilePath);
				
				testingGround.addParentFileInfo(structItem.itemFile,parentFile);
				Item parentItem = parentFile==null ? null : parentFinder.getParent(parentFile);
				
				try {
					content = new GenericXmlNode(contentNode, localTemplates, parentItem==null ? null : parentItem.content, GenericXmlNode.Source.create(structItem.itemFile));
				} catch (InheritRemoveException e) {
					throw new ParseException("Found unexpected attribute (\"%s\") in content node in file \"%s\"", GenericXmlNode.ATTR_INHERIT_REMOVE, structItem.itemFile.getPath());
				}
			}
			
			private static String getParentFile(Node node, String itemFilePath) throws ParseException {
				if (node==null) return null;
				if (!node.getNodeName().equals("_parent")) throw new IllegalStateException();
				if (node.getChildNodes().getLength()!=0)
					throw new ParseException("Found unexpected child nodes in <_parent> node in item file \"%s\"", itemFilePath);
				
				NamedNodeMap attributes = node.getAttributes();
				
				String parentFile = null;
				for (Node attrNode : XML.makeIterable(attributes)) {
					if (!attrNode.getNodeName().equals("File"))
						throw new ParseException("Found unexpected attribute (\"%s\") in <_parent> node in item file \"%s\"", attrNode.getNodeName(), itemFilePath);
					parentFile = attrNode.getNodeValue();
				}
				if (parentFile == null)
					throw new ParseException("Can't find \"File\" attribute in <_parent> node in item file \"%s\"", itemFilePath);
				
				//System.err.printf("[INFO] Found <_parent> node (->\"%s\") in item file \"%s\"%n", parentFile, itemFilePath);
				
				return parentFile;
			}

			boolean isMainItem() {
				return subClassName==null;
			}
		}
	}
	
	interface TemplatesInterface {
		GenericXmlNode getTemplate(String nodeName, String templateName) throws ParseException;
		Templates getInstance();
	}
	static class Templates implements TemplatesInterface {
	
		final HashMap<String, HashMap<String, GenericXmlNode>> templates;
		final Templates includedTemplates;
	
		Templates(String className, HashMap<String, Templates> globalTemplates, ZipEntryTreeNode sourceFile, OdditiesHandler templatesOdditiesHandler) throws ParseException, EntryStructureException {
			includedTemplates = getIncludedTemplates(globalTemplates, sourceFile, className, false);
			templates = new HashMap<>();
		}

		Templates(Node templatesNode, HashMap<String,Templates> globalTemplates, ZipEntryTreeNode sourceFile, OdditiesHandler odditiesHandler) throws ParseException, EntryStructureException {
			if (templatesNode==null) throw new IllegalArgumentException();
			if (!templatesNode.getNodeName().equals("_templates")) throw new IllegalStateException();
			if (templatesNode.getNodeType()!=Node.ELEMENT_NODE) throw new IllegalStateException();
			
			String includeFile = getIncludeFile(templatesNode, sourceFile);
			includedTemplates = getIncludedTemplates(globalTemplates, sourceFile, includeFile, true);
			
			templates = new HashMap<>();
			NodeList childNodes = templatesNode.getChildNodes();
			for (Node node : XML.makeIterable(childNodes)) {
				if (node.getNodeType()==Node.COMMENT_NODE) {
					// is Ok, do nothing
						
				} else if (isEmptyTextNode(node, ()->String.format("<_templates> node in file \"%s\"", sourceFile.getPath()))) {
					// is Ok, do nothing
					
				} else if (node.getNodeType()==Node.ELEMENT_NODE) {
					
					String originalNodeName = node.getNodeName();
					if (originalNodeName.equals("_parent")) {
						if (odditiesHandler == null)
							throw new ParseException("Found an oddity (<_parent> node in <_templates> node) in file \"%s\" but no OdditiesHandler to handle it%n", sourceFile.getPath());
						odditiesHandler.parentNode = node;
						continue;
					}
					if (originalNodeName.equals("Mudguard")) {
						// my guess: <Mudguard> should be a template in template list <Body>
						HashMap<String, GenericXmlNode> templateList = createNewTemplateList("Body", sourceFile.getPath());
						addNewTemplate_unchecked(node, "Body", "Mudguard", templateList, sourceFile, this);
						continue;
					}
					//if (originalNodeName.equals("Mudguard") && sourceFilePath.equals("\\[media]\\classes\\trucks\\trailers\\semitrailer_gooseneck_4.xml")) {
					//	System.err.printf("Found an expected oddity (templates \"%s\") in <_templates> node in file \"%s\" --> ignored%n", originalNodeName, sourceFilePath);
					//	continue;
					//}
					//if (originalNodeName.equals("Mudguard") && sourceFilePath.equals("\\[media]\\_dlc\\dlc_2_1\\classes\\trucks\\trailers\\semitrailer_special_w_cat_770.xml")) {
					//	System.err.printf("Found an expected oddity (templates \"%s\") in <_templates> node in file \"%s\" --> ignored%n", originalNodeName, sourceFilePath);
					//	continue;
					//}
					
					HashMap<String, GenericXmlNode> templateList = createNewTemplateList(originalNodeName, sourceFile.getPath());
					parseTemplates(node, templateList, originalNodeName, sourceFile);
					
					
				} else
					throw new ParseException("Found unexpected node in <_templates> node in file \"%s\": %s", sourceFile.getPath(), XML.toDebugString(node));
			}
		}

		private static Templates getIncludedTemplates(HashMap<String, Templates> globalTemplates, ZipEntryTreeNode sourceFile, String includeFile, boolean expectingExistance) throws ParseException, EntryStructureException {
			if (includeFile==null)
				return null;
			
			if (globalTemplates==null)
				throw new ParseException("Found \"Include\" attribute (\"%s\") in <_templates> node in file \"%s\" but no globalTemplates are defined", includeFile, sourceFile.getPath());
			
			Templates includedTemplates = globalTemplates.get(includeFile);
			if (includedTemplates==null && expectingExistance)
				throw new EntryStructureException("Can't find templates to include (\"%s\") in <_templates> node in file \"%s\"", includeFile, sourceFile.getPath());
			
			return includedTemplates;
		}
	
		private HashMap<String, GenericXmlNode> createNewTemplateList(String originalNodeName, String sourceFilePath) throws ParseException {
			HashMap<String, GenericXmlNode> templateList = templates.get(originalNodeName);
			if (templateList!=null)
				throw new ParseException("Found more than one template list for node name \"%s\" in <_templates> node in file \"%s\"", originalNodeName, sourceFilePath);
			
			templates.put(originalNodeName, templateList = new HashMap<>());
			return templateList;
		}
		
		private void parseTemplates(Node listNode, HashMap<String, GenericXmlNode> templatesList, String originalNodeName, ZipEntryTreeNode sourceFile) throws ParseException {
			if (listNode==null) throw new IllegalArgumentException();
			if (!listNode.getNodeName().equals(originalNodeName)) throw new IllegalStateException();
			if (listNode.getNodeType()!=Node.ELEMENT_NODE) throw new IllegalStateException();
			if (listNode.hasAttributes())
				throw new ParseException("Found unexpected attriutes in template list for node name \"%s\" in <_templates> node in file \"%s\"", originalNodeName, sourceFile.getPath());
			
			NodeList childNodes = listNode.getChildNodes();
			HashMap<String, Node> templateNodes = new HashMap<>();
			for (Node node : XML.makeIterable(childNodes)) {
				if (node.getNodeType()==Node.COMMENT_NODE) {
					// is Ok, do nothing
						
				} else if (isEmptyTextNode(node, ()->String.format("template list for node name \"%s\" in <_templates> node in file \"%s\"", originalNodeName, sourceFile.getPath()))) {
					// is Ok, do nothing
					
				} else if (node.getNodeType()==Node.ELEMENT_NODE) {
					String templateName = node.getNodeName();
					Node existingTemplate = templateNodes.get(templateName);
					if (existingTemplate!=null)
						throwParseException(false,"Found more than one template with name \"%s\" for node name \"%s\" in <_templates> node in file \"%s\" --> new template ignored", templateName, originalNodeName, sourceFile.getPath());
					else
						templateNodes.put(templateName, node);
					
				} else
					throw new ParseException("Found unexpected node in <_templates> node in file \"%s\": %s", sourceFile.getPath(), XML.toDebugString(node));
			}
			
			new TemplateLoader(this, templateNodes, originalNodeName, templatesList, sourceFile).load();
		}
		
		static class TemplateLoader implements TemplatesInterface {
			
			private final Templates templates;
			private final HashMap<String, Node> templateNodes;
			private final String originalNodeName;
			private final HashMap<String, GenericXmlNode> templatesList;
			private final ZipEntryTreeNode sourceFile;
			private final HashSet<String> loadedNodes;
			private final HashSet<String> blockedNodes;

			TemplateLoader(Templates templates, HashMap<String, Node> templateNodes, String originalNodeName, HashMap<String, GenericXmlNode> templatesList, ZipEntryTreeNode sourceFile) {
				this.templates = templates;
				this.templateNodes = templateNodes;
				this.originalNodeName = originalNodeName;
				this.templatesList = templatesList;
				this.sourceFile = sourceFile;
				loadedNodes = new HashSet<>();
				blockedNodes = new HashSet<>();
			}
			
			void load() throws ParseException {
				for (String templateName : templateNodes.keySet()) {
					if (loadedNodes.contains(templateName)) continue;
					Node node = templateNodes.get(templateName);
					addNewTemplate(node, templateName);
				}
			}

			private GenericXmlNode addNewTemplate(Node node, String templateName) throws ParseException {
				blockedNodes.add(templateName);
				GenericXmlNode template = addNewTemplate_unchecked(node, originalNodeName, templateName, templatesList, sourceFile, this);
				blockedNodes.remove(templateName);
				loadedNodes.add(templateName);
				return template;
			}

			@Override
			public GenericXmlNode getTemplate(String nodeName, String templateName) throws ParseException {
				GenericXmlNode template = templates.getTemplate(nodeName, templateName);
				if (template!=null) return template;
				
				if (!originalNodeName.equals(nodeName))
					throw new IllegalStateException();
				
				Node otherNode = templateNodes.get(templateName);
				if (otherNode==null) return null;
				
				if (blockedNodes.contains(templateName))
					throw new ParseException("Found _template cycle (\"%s\") in <_templates> node in file \"%s\"", templateName, sourceFile.getPath());
				
				return addNewTemplate(otherNode, templateName);
			}

			@Override public Templates getInstance() {
				return templates;
			}
			
		}
	
		private static GenericXmlNode addNewTemplate_unchecked(Node node, String originalNodeName, String templateName, HashMap<String, GenericXmlNode> templatesList, ZipEntryTreeNode sourceFile, TemplatesInterface ti) throws ParseException {
			GenericXmlNode template;
			try {
				template = new GenericXmlNode(originalNodeName, node, ti, null, GenericXmlNode.Source.create(sourceFile));
			} catch (InheritRemoveException e) {
				throw new ParseException("Found unexpected attribute (\"%s\") in <_templates> node in file \"%s\"", GenericXmlNode.ATTR_INHERIT_REMOVE, sourceFile.getPath());
			}
			templatesList.put(templateName, template);
			return template;
		}
	
		private String getIncludeFile(Node node, ZipEntryTreeNode file) throws ParseException {
			if (node==null) return null;
			if (!node.getNodeName().equals("_templates")) throw new IllegalStateException();
			//if (node.getChildNodes().getLength()!=0)
			//	throw new ParseException("Found unexpected child nodes in <_templates> node in file \"%s\"", file.getPath());
			
			NamedNodeMap attributes = node.getAttributes();
			
			String includeFile = null;
			for (Node attrNode : XML.makeIterable(attributes)) {
				if (!attrNode.getNodeName().equals("Include"))
					throw new ParseException("Found unexpected attribute (\"%s\") in <_templates> node in file \"%s\"", attrNode.getNodeName(), file.getPath());
				includeFile = attrNode.getNodeValue();
			}
			//if (includeFile == null)
			//	throw new ParseException("Can't find \"Include\" attribute in <_templates> node in item file \"%s\"", file.getPath());
			
			return includeFile;
		}
		
		@Override public Templates getInstance() {
			return this;
		}

		@Override public GenericXmlNode getTemplate(String nodeName, String templateName) {
			HashMap<String, GenericXmlNode> templateList = templates.get(nodeName);
			GenericXmlNode template = templateList==null ? null : templateList.get(templateName);
			if (template != null) return template;
			return includedTemplates==null ? null : includedTemplates.getTemplate(nodeName, templateName);
		}

		private static class OdditiesHandler {
			Node parentNode = null;
		}
	}

	static class GenericXmlNode {
		private static final String ATTR_NOINHERIT      = "_noinherit";
		private static final String ATTR_INHERIT_REMOVE = "_inheritRemove";
		private static final String ATTR_TEMPLATE       = "_template";
		
		static class InheritRemoveException extends Exception {
			private static final long serialVersionUID = -9221801644995970360L;
		}
		
		final String nodeName;
		final HashMap<String, String> attributes;
		final MultiMap<GenericXmlNode> nodes;
		
		GenericXmlNode(GenericXmlNode sourceNode) {
			if (sourceNode==null) throw new IllegalArgumentException();
			
			this.nodeName = sourceNode.nodeName;
			attributes = new HashMap<>(sourceNode.attributes);
			nodes = new MultiMap<>();
			sourceNode.nodes.forEach((key,childNode)->nodes.add(key, new GenericXmlNode(childNode)));
		}
		
		GenericXmlNode(GenericXmlNode sourceNode, GenericXmlNode templateNode, Source source) throws ParseException {
			if (sourceNode==null) throw new IllegalArgumentException();
			
			this.nodeName = sourceNode.nodeName;
			attributes = new HashMap<>();
			nodes = new MultiMap<>();
			
			
			if (templateNode!=null)
				attributes.putAll(templateNode.attributes);
			attributes.putAll(sourceNode.attributes);

			if (templateNode == null)
				sourceNode.nodes.forEach((key,childNode)->nodes.add(key, new GenericXmlNode(childNode)));
			
			else {
				Consumer<String> debugOutput = str->testingGround.showCurrentState(templateNode, sourceNode, null, this, null, source, str);
				mergeNodes(sourceNode.nodes, templateNode.nodes, this.nodes, GenericXmlNodeConstructor.createGenericNodeBased(), source);
			}
		}
		
		GenericXmlNode(Node xmlNode, TemplatesInterface templates, GenericXmlNode parentNode, Source source) throws ParseException, InheritRemoveException {
			this(xmlNode.getNodeName(), xmlNode, templates, parentNode, source);
		}
		
		GenericXmlNode(String nodeName, Node sourceNode, TemplatesInterface templates, GenericXmlNode parentNode, Source source) throws ParseException, InheritRemoveException {
			if (sourceNode==null) throw new IllegalArgumentException();
			
			this.nodeName = nodeName;
			attributes = new HashMap<>();
			nodes = new MultiMap<>();
			
			if (parentNode!=null && !parentNode.nodeName.equals(this.nodeName))
				throw new ParseException("Parent node has different name (\"%s\") than this node (\"%s\") [File:%s]", parentNode.nodeName, this.nodeName, source.getFilePath());
			
			GenericXmlNode templateNode = null;
			NamedNodeMap xmlAttributes = sourceNode.getAttributes();
			if (xmlAttributes!=null) {
				
				String templateName = getAttribute(xmlAttributes, ATTR_TEMPLATE);
				if (templateName!=null) {
					if (templates==null)
						throwParseException(false, "Found \"_template\" attribute but no Templates are defined  [Node:%s, File:%s]", this.nodeName, source.getFilePath());
					else {
						templateNode = templates.getTemplate(this.nodeName, templateName);
						if (templateNode==null)
							throw new ParseException("Can't find template \"%s\" for node \"%s\" [File:%s]", templateName, this.nodeName, source.getFilePath());
					}
				}
				
				String inheritRemoveValue = getAttribute(xmlAttributes, ATTR_INHERIT_REMOVE);
				if (inheritRemoveValue!=null && inheritRemoveValue.toLowerCase().equals("true")) {
					throw new InheritRemoveException();
					//testingGround.showCurrentState(parentNode, templateNode, sourceNode, this, templates==null ? null : templates.getInstance(), source, "Found _inheritRemove attribute");
				}
				
				String noinheritValue = getAttribute(xmlAttributes, ATTR_NOINHERIT);
				if (noinheritValue!=null && noinheritValue.toLowerCase().equals("true"))
					parentNode = null;
			}
			
			if (parentNode!=null)
				attributes.putAll(parentNode.attributes);
			if (templateNode!=null)
				attributes.putAll(templateNode.attributes);
			for (Node attrNode : XML.makeIterable(xmlAttributes)) {
				String attrName = attrNode.getNodeName();
				String attrValue = attrNode.getNodeValue();
				if (attrName.startsWith("_") && !attrName.equals(ATTR_TEMPLATE))
					testingGround.addSpecialAttribute(attrName,attrValue);
				if (!attrName.equals(ATTR_TEMPLATE) && !attrName.equals(ATTR_NOINHERIT))
					attributes.put(attrName,attrValue);
			}
			
			MultiMap<Node> elementNodes = getElementNodes(sourceNode, source);
			
			if (parentNode==null && templateNode==null)
				for (String key : elementNodes.getKeys())
					for (Node xmlSubNode : elementNodes.get(key))
						nodes.add(key, new GenericXmlNode(xmlSubNode, templates, null, source));
			
			else if (parentNode!=null && templateNode!=null) {
				MultiMap<GenericXmlNode> temporary = new MultiMap<>();
				mergeNodes(elementNodes, templateNode.nodes, temporary , GenericXmlNodeConstructor.createXmlNodeBased(templates), source);
				mergeNodes(temporary   , parentNode  .nodes, this.nodes, GenericXmlNodeConstructor.createGenericNodeBased()     , source);
				
			} else if (parentNode!=null) {
				mergeNodes(elementNodes, parentNode.nodes, this.nodes, GenericXmlNodeConstructor.createXmlNodeBased(templates), source);
				
			} else if (templateNode!=null) {
				mergeNodes(elementNodes, templateNode.nodes, this.nodes, GenericXmlNodeConstructor.createXmlNodeBased(templates), source);
				
			}
		}
		
		private static <SourceNodeType> void mergeNodes(
				MultiMap<SourceNodeType> sourceNodes,
				MultiMap<GenericXmlNode> templateNodes,
				MultiMap<GenericXmlNode> targetNodes,
				GenericXmlNodeConstructor<SourceNodeType> nodeConstructor,
				Source source)
				throws ParseException {
			
			for (String key : templateNodes.getKeys())
				if (!sourceNodes.containsKey(key))
					for (GenericXmlNode tempChildNode : templateNodes.get(key))
						targetNodes.add(key, new GenericXmlNode(tempChildNode));
			
			for (String key : sourceNodes.getKeys()) {
				Vector<GenericXmlNode> tempNodes = templateNodes.get(key);
				Vector<SourceNodeType> srcNodes = sourceNodes.get(key);
				for (int i=0; i<srcNodes.size(); i++) {
					SourceNodeType srcChildNode = srcNodes.get(i);
					GenericXmlNode tempChildNode = tempNodes==null || i>=tempNodes.size() ? null : tempNodes.get(i);
					try {
						targetNodes.add(key, nodeConstructor.construct(srcChildNode, tempChildNode, source));
					} catch (InheritRemoveException e) {
						if (tempChildNode==null)
							throwParseException(false, "Found unexpected attribute (\"%s\") in <%s> node in file \"%s\"", GenericXmlNode.ATTR_INHERIT_REMOVE, key, source.getFilePath());
					}
				}
			}
		}
		
		interface GenericXmlNodeConstructor<SourceNodeType> {
			GenericXmlNode construct(SourceNodeType sourceNode, GenericXmlNode templateNode, Source source) throws ParseException, InheritRemoveException;
			
			static GenericXmlNodeConstructor<Node> createXmlNodeBased(TemplatesInterface templates) {
				return (sourceNode, templateNode, source) -> new GenericXmlNode(sourceNode, templates, templateNode, source);
			}
			
			static GenericXmlNodeConstructor<GenericXmlNode> createGenericNodeBased() {
				return (sourceNode, templateNode, source) -> new GenericXmlNode(sourceNode, templateNode, source);
			}
		}
		
//		private static String toString(String label, Vector<String> strings) {
//			StringBuilder sb = new StringBuilder();
//			sb.append(label).append(":\r\n");
//			for (String str : strings)
//				sb.append("   \"").append(str).append("\"\r\n");
//			return sb.toString();
//		}
		
		private static MultiMap<Node> getElementNodes(Node xmlNode, Source source) throws ParseException {
			MultiMap<Node> elementNodes = new MultiMap<>();
			
			NodeList nodes = xmlNode.getChildNodes();
			for (Node childNode : XML.makeIterable(nodes)) {
				if (childNode.getNodeType()==Node.COMMENT_NODE) {
					// is Ok, do nothing
						
				} else if (isEmptyTextNode(childNode, ()->String.format("node \"%s\" in file \"%s\"", xmlNode.getNodeName(), source.getFilePath()))) {
					// is Ok, do nothing
					
				} else if (childNode.getNodeType()==Node.ELEMENT_NODE) {
					elementNodes.add(childNode.getNodeName(),childNode);
					
				} else
					throw new ParseException("Found unexpected node in <_templates> node in file \"%s\": %s", source.getFilePath(), XML.toDebugString(childNode));
			}
			return elementNodes;
		}
		
		private static String getAttribute(NamedNodeMap attributes, String name) {
			String template;
			Node templateAttrNode = attributes.getNamedItem(name);
			if (templateAttrNode!=null)
				template = templateAttrNode.getNodeValue();
			else template = null;
			return template;
		}
		
		interface Source {
			String getFilePath();
			static Source create(ZipEntryTreeNode sourceFile) {
				return ()->sourceFile.getPath();
			}
		}

		GenericXmlNode getNode(String... path) {
			GenericXmlNode[] nodes = getNodes(path);
			if (nodes.length>1) throw new IllegalStateException();
			return nodes.length<1 ? null : nodes[0];
		}
		
		GenericXmlNode[] getNodes(String... path) {
			if (path.length<2) throw new IllegalArgumentException();
			if (!nodeName.equals(path[0]))
				throw new IllegalArgumentException();
			Vector<GenericXmlNode> nodes = new Vector<>();
			getSubnodes(path,1,nodes);
			return nodes.toArray(new GenericXmlNode[nodes.size()]);
		}

		private void getSubnodes(String[] path, int index, Vector<GenericXmlNode> nodes) {
			Vector<GenericXmlNode> childNodes = this.nodes.get(path[index]);
			if (childNodes==null) return;
			for (GenericXmlNode childNode : childNodes) {
				if (index+1>=path.length)
					nodes.add(childNode);
				else
					childNode.getSubnodes(path, index+1, nodes);
			}
		}
	}
	
	static class MultiMap<ValueType> {
		final HashMap<String,Vector<ValueType>> map = new HashMap<>();
		
		void add(String key, ValueType value) {
			Vector<ValueType> list = map.get(key);
			if (list==null) map.put(key, list = new Vector<>());
			list.add(value);
		}
		
		boolean containsKey(String key) {
			return map.containsKey(key);
		}

		void forEach(BiConsumer<String,ValueType> action) {
			map.forEach((key,list)->list.forEach(value->action.accept(key, value)));
		}

		Vector<ValueType> get(String key) {
			return map.get(key);
		}

		Set<String> getKeys() {
			return map.keySet();
		}

		void printTo(PrintStream out, Function<ValueType,String> toString) {
			Vector<String> keys = new Vector<>( map.keySet() );
			keys.sort(null);
			for (String key : keys) {
				Vector<ValueType> list = map.get(key);
				out.printf("   \"%s\" [%d]%n", key, list.size());
				for (ValueType value : list)
					out.printf("      %s%n", toString.apply(value));
			}
		}
	}
}
