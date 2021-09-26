package net.schwarzbaer.java.games.snowrunner;

import java.awt.Window;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode.Source;
import net.schwarzbaer.system.DateTimeFormatter;

@SuppressWarnings("unused")
class XMLTemplateStructure {

	private static TestingGround testingGround;
	
	final HashMap<String, Templates> globalTemplates;
	final HashMap<String, Class_> classes;
	final Vector<String> ignoredFiles;

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
		if (nodeValue!=null && !nodeValue.trim().isEmpty())
			throwParseException(false, "Found unexpected text in %s: \"%s\" %s", getNodeLabel.get(), nodeValue, toHexString(nodeValue.getBytes()));
			
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
				if (class_==null) classes.put(className, class_ = new StructClass(className,dlc));
				scanClass(class_, classFolder);
			}
		}
		
		private static void scanClass(StructClass class_, ZipEntryTreeNode classFolder) throws EntryStructureException {
			for (ZipEntryTreeNode subClassFolder : classFolder.folders.values()) {
				if (!subClassFolder.folders.isEmpty()) throw new EntryStructureException("Found unexpected folders in sub class folder \"%s\"", subClassFolder.getPath());
				if ( subClassFolder.files  .isEmpty()) throw new EntryStructureException("Found no item files in sub class folder \"%s\"", subClassFolder.getPath());
				
				scanItems(class_, subClassFolder.name, subClassFolder);
			}
			
			scanItems(class_, null, classFolder);
		}

		private static void scanItems(StructClass class_, String subClassName, ZipEntryTreeNode folder) throws EntryStructureException {
			for (ZipEntryTreeNode itemFile : folder.files.values()) {
				String itemName = getXmlItemName(itemFile);
				StructItem otherItem = class_.items.get(itemName);
				if (otherItem!=null)
					throw new EntryStructureException("Found more than one item file with name \"%s\" in class folder \"%s\"", itemName, folder.getPath());
				class_.items.put(itemName, new StructItem(class_.dlcName,class_.className,subClassName,itemName,itemFile));
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
			final String dlcName;
			final HashMap<String, StructItem> items;
	
			StructClass(String className, String dlc) {
				this.className = className;
				this.dlcName = dlc;
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
		private final Window mainWindow;

		TestingGround(ZipFile zipFile, ZipEntryTreeNode contentRootFolder, Window mainWindow) {
			this.zipFile = zipFile;
			this.contentRootFolder = contentRootFolder;
			this.mainWindow = mainWindow;
			parentRelations = new HashMap<>();
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
				ZipEntryTreeNode existingNode = fileNames.get(fileNode.name);
				if (existingNode!=null)
					System.err.printf("Found dublicate filenames:%n   \"%s\"%n   \"%s\"%n", existingNode.getPath(), fileNode.getPath());
				else
					fileNames.put(fileNode.name,fileNode);
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

		void showCurrentState(GenericXmlNode parentTemplate, GenericXmlNode template, Node xmlNode, GenericXmlNode currentState, Templates templates, Source source) {
			GenericXmlNodeParsingStateDialog dlg = new GenericXmlNodeParsingStateDialog(mainWindow, parentTemplate, template, xmlNode, currentState, templates, source);
			dlg.showDialog();
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
		
		final String dlc;
		final String name;
		final HashMap<String,Item> items;
	
		public Class_(ZipFile zipFile, ClassStructur.StructClass structClass, HashMap<String, Templates> globalTemplates, Vector<String> ignoredFiles) throws IOException, EntryStructureException, ParseException {
			if (zipFile==null) throw new IllegalArgumentException();
			if ( structClass==null) throw new IllegalArgumentException();
			if (globalTemplates==null) throw new IllegalArgumentException();
			if (ignoredFiles==null) throw new IllegalArgumentException();
			
			this.dlc = structClass.dlcName;
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
				Templates localTemplates = templatesNode==null ? null : new Templates(templatesNode, globalTemplates, structItem.itemFile, templatesOdditiesHandler);
				if (templatesOdditiesHandler.parentNode!=null) {
					if (parentNode!=null)
						throw new ParseException("Found an oddity (<_parent> node in <_templates> node) in file \"%s\" but also a <_parent> node outside of <_templates> node%n", structItem.itemFilePath);
					parentNode = templatesOdditiesHandler.parentNode;
				}
				
				String parentFile = getParentFile(parentNode, structItem.itemFilePath);
				
				testingGround.addParentFileInfo(structItem.itemFile,parentFile);
				Item parentItem = parentFile==null ? null : parentFinder.getParent(parentFile);
				
				content = new GenericXmlNode(contentNode, localTemplates, parentItem==null ? null : parentItem.content, GenericXmlNode.Source.create(structItem.itemFile));
			}
			
			private String getParentFile(Node node, String itemFilePath) throws ParseException {
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
		}
	}
	
	static class Templates {
	
		final HashMap<String, HashMap<String, GenericXmlNode>> templates;
		final Templates includedTemplates;
	
		Templates(Node templatesNode, HashMap<String,Templates> globalTemplates, ZipEntryTreeNode sourceFile, OdditiesHandler odditiesHandler) throws ParseException, EntryStructureException {
			if (templatesNode==null) throw new IllegalArgumentException();
			if (!templatesNode.getNodeName().equals("_templates")) throw new IllegalStateException();
			if (templatesNode.getNodeType()!=Node.ELEMENT_NODE) throw new IllegalStateException();
			
			String includeFile = getIncludeFile(templatesNode, sourceFile);
			if (includeFile==null)
				includedTemplates = null;
			
			else {
				if (globalTemplates==null)
					throw new ParseException("Found \"Include\" attribute (\"%s\") in <_templates> node in file \"%s\" but no globalTemplates are defined", includeFile, sourceFile.getPath());
				
				includedTemplates = globalTemplates.get(includeFile);
				if (includedTemplates==null)
					throw new EntryStructureException("Can't find templates to include (\"%s\") in <_templates> node in file \"%s\"", includeFile, sourceFile.getPath());
			}
			
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
						addNewTemplate_unchecked(node, "Body", templateList, sourceFile);
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
	
		GenericXmlNode getTemplate(String nodeName, String templateName) {
			HashMap<String, GenericXmlNode> templateList = templates.get(nodeName);
			GenericXmlNode template = templateList==null ? null : templateList.get(templateName);
			if (template != null) return template;
			return includedTemplates==null ? null : includedTemplates.getTemplate(nodeName, templateName);
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
			for (Node node : XML.makeIterable(childNodes)) {
				if (node.getNodeType()==Node.COMMENT_NODE) {
					// is Ok, do nothing
						
				} else if (isEmptyTextNode(node, ()->String.format("template list for node name \"%s\" in <_templates> node in file \"%s\"", originalNodeName, sourceFile.getPath()))) {
					// is Ok, do nothing
					
				} else if (node.getNodeType()==Node.ELEMENT_NODE) {
					addNewTemplate_unchecked(node, originalNodeName, templatesList, sourceFile);
					
				} else
					throw new ParseException("Found unexpected node in <_templates> node in file \"%s\": %s", sourceFile.getPath(), XML.toDebugString(node));
			}
		}
	
		private void addNewTemplate_unchecked(Node node, String originalNodeName, HashMap<String, GenericXmlNode> templatesList, ZipEntryTreeNode sourceFile) throws ParseException {
			String templateName = node.getNodeName();
			GenericXmlNode template = templatesList.get(templateName);
			if (template!=null)
				throwParseException(false,"Found more than one template with name \"%s\" for node name \"%s\" in <_templates> node in file \"%s\" --> new template ignored", templateName, originalNodeName, sourceFile.getPath());
			else
				templatesList.put(templateName, new GenericXmlNode(originalNodeName, node, this, null, GenericXmlNode.Source.create(sourceFile)));
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
		
		private static class OdditiesHandler {
			Node parentNode = null;
		}
	}

	static class GenericXmlNode {
		private static final String TEMPLATE_ATTR_NAME = "_template";
		
		final String nodeName;
		final HashMap<String, String> attributes;
		final MultiMap<GenericXmlNode> nodes;
		
		GenericXmlNode(Node xmlNode, Templates templates, GenericXmlNode parentTemplate, Source source) throws ParseException {
			this(xmlNode.getNodeName(), xmlNode, templates, parentTemplate, source);
		}
		GenericXmlNode(String nodeName, Node xmlNode, Templates templates, GenericXmlNode parentTemplate, Source source) throws ParseException {
			this.nodeName = nodeName;
			attributes = new HashMap<>();
			nodes = new MultiMap<>();
			
			if (xmlNode==null) throw new IllegalArgumentException();
			if (parentTemplate!=null && parentTemplate.nodeName.equals(this.nodeName))
				throw new ParseException("Parent node has different name (\"%s\") than this node (\"%s\") [File:%s]", parentTemplate.nodeName, this.nodeName, source.getFilePath());
			
			GenericXmlNode template = null;
			NamedNodeMap xmlAttributes = xmlNode.getAttributes();
			if (xmlAttributes!=null) {
				
				String templateName = getAttribute(xmlAttributes, TEMPLATE_ATTR_NAME);
				if (templateName!=null) {
					if (templates==null)
						throw new ParseException("Found \"_template\" attribute but no Templates are defined  [Node:%s, File:%s]", this.nodeName, source.getFilePath());
					
					template = templates.getTemplate(this.nodeName, templateName);
					if (templates==null)
						throw new ParseException("Can't find template \"%s\" for node \"%s\" [File:%s]", templateName, this.nodeName, source.getFilePath());
				}
			}
			
			if (parentTemplate!=null)
				attributes.putAll(parentTemplate.attributes);
			if (template!=null)
				attributes.putAll(template.attributes);
			for (Node attrNode : XML.makeIterable(xmlAttributes)) {
				String attrName = attrNode.getNodeName();
				if (!attrName.equals(TEMPLATE_ATTR_NAME))
					attributes.put(attrName,attrNode.getNodeValue());
			}
			
			MultiMap<Node> elementNodes = getElementNodes(xmlNode, source);
			
			if (parentTemplate != null || template != null) {
				testingGround.showCurrentState(parentTemplate, template, xmlNode, this, templates, source);
				if (parentTemplate!=null && template!=null){
					// TODO
				}
				
			} else
				for (String key : elementNodes.getKeys())
					for (Node xmlSubNode : elementNodes.get(key))
						nodes.add(key, new GenericXmlNode(xmlSubNode, templates, null, source));
			
			// TODO Auto-generated constructor stub
		}
		
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
	}
	
	static class MultiMap<ValueType> {
		final HashMap<String,Vector<ValueType>> map = new HashMap<>();
		
		void add(String key, ValueType value) {
			Vector<ValueType> list = map.get(key);
			if (list==null) map.put(key, list = new Vector<>());
			list.add(value);
		}
		
		Vector<ValueType> get(String key) {
			return map.get(key);
		}

		Set<String> getKeys() {
			return map.keySet();
		}
	}
}
