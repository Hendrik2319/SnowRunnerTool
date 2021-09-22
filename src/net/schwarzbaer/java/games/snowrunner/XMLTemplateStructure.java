package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.schwarzbaer.java.games.snowrunner.PAKReader.ZipEntryTreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;

@SuppressWarnings("unused")
class XMLTemplateStructure {

	final HashMap<String, Templates> globalTemplates;
	final HashMap<String, Class_> classes;
	final HashMap<String, HashMap<String, Class_>> dlcs;

	static XMLTemplateStructure readPAK(File pakFile) {
		return PAKReader.readPAK(pakFile, (zipFile, zipRoot) -> {
			try {
				return new XMLTemplateStructure(zipFile, zipRoot);
			} catch (EntryStructureException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			return null;
		});
	}
	
	XMLTemplateStructure(ZipFile zipFile, ZipEntryTreeNode zipRoot) throws IOException, EntryStructureException, ParseException {
		ZipEntryTreeNode contentRootFolder = zipRoot.getSubFolder("[media]");
		if (contentRootFolder==null)
			throw new EntryStructureException("Found no content root folder \"[media]\" in \"%s\"", zipFile.getName());
		
		testContentRootFolder(zipFile, contentRootFolder);
		
		globalTemplates = readGlobalTemplates(zipFile,       contentRootFolder.getSubFolder("_templates"));
		classes         = readClasses        (zipFile, null, contentRootFolder.getSubFolder("classes"   ), globalTemplates);
		dlcs            = readDLCs           (zipFile,       contentRootFolder.getSubFolder("_dlc"      ), globalTemplates);
	}
	
	private static HashMap<String, HashMap<String, Class_>> readDLCs(ZipFile zipFile, ZipEntryTreeNode dlcsFolder, HashMap<String, Templates> globalTemplates) throws IOException, EntryStructureException, ParseException {
		if ( dlcsFolder==null) throw new EntryStructureException("No DLCs folder");
		if (!dlcsFolder.files  .isEmpty()) throw new EntryStructureException("Found unexpected files in DLCs folder \"%s\"", dlcsFolder.getPath());
		if ( dlcsFolder.folders.isEmpty()) throw new EntryStructureException("Found no DLC folders in DLCs folder \"%s\"", dlcsFolder.getPath());
		
		HashMap<String, HashMap<String, Class_>> dlcs = new HashMap<>();
		for (ZipEntryTreeNode dlcNode:dlcsFolder.folders.values()) {
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
			
			HashMap<String, Class_> classes = readClasses(zipFile, dlcNode.name, classesFolder, globalTemplates);
			if (classes!=null)
				dlcs.put(dlcNode.name, classes);
		}
		
		return dlcs;
	}

	private static HashMap<String,Templates> readGlobalTemplates(ZipFile zipFile, ZipEntryTreeNode templatesFolder) throws EntryStructureException, IOException, ParseException {
		if (zipFile==null) throw new IllegalArgumentException();
		if ( templatesFolder==null) throw new EntryStructureException("No templates folder");
		if (!templatesFolder.folders.isEmpty()) throw new EntryStructureException("Found unexpected folders in templates folder \"%s\"", templatesFolder.getPath());
		if ( templatesFolder.files  .isEmpty()) throw new EntryStructureException("Found no files in templates folder \"%s\"", templatesFolder.getPath());
		
		HashMap<String,Templates> globalTemplates = new HashMap<>();
		for (ZipEntryTreeNode fileNode:templatesFolder.files.values()) {
			//System.out.printf("Read template \"%s\" ...%n", fileNode.getPath());
			
			NodeList nodes = readXML(zipFile, fileNode);
			if (nodes==null) {
				System.err.printf("Can't read xml file \"%s\" --> Templates file will be ignored%n", fileNode.getPath());
				continue;
			}
			
			Node templatesNode = null; 
			for (Node node:XML.makeIterable(nodes)) {
				//showNode(node);
				if (node.getNodeType()==Node.ELEMENT_NODE && node.getNodeName().equals("_templates")) {
					if (templatesNode!=null)
						throw new ParseException("Found dublicate <_templates> node in template file \"%s\"", fileNode.getPath());
					templatesNode = node;
					
				} else if (node.getNodeType()==Node.TEXT_NODE) {
					String nodeValue = node.getNodeValue();
					if (nodeValue!=null && !nodeValue.trim().isEmpty())
						throw new ParseException("Found unexpected text in template file \"%s\": \"%s\"", fileNode.getPath(), nodeValue);
					
				} else if (node.getNodeType()==Node.ELEMENT_NODE)
					throw new ParseException("Found unexpected element node in template file \"%s\": %s", fileNode.getPath(), XML.toDebugString(node));
				else
					throw new ParseException("Found unexpected node in template file \"%s\": %s", fileNode.getPath(), XML.toDebugString(node));
			}
			if (templatesNode==null)
				throw new ParseException("Found no <_templates> node in template file \"%s\"", fileNode.getPath());
			
			globalTemplates.put(fileNode.name, new Templates(templatesNode, null));
			//System.out.printf("... done [read template]%n");
		}
		
		return globalTemplates;
	}
	
	private static HashMap<String, Class_> readClasses(ZipFile zipFile, String dlc, ZipEntryTreeNode classesFolder, HashMap<String,Templates> globalTemplates) throws IOException, EntryStructureException, ParseException {
		if (globalTemplates==null) throw new IllegalArgumentException();
		if ( classesFolder==null) throw new EntryStructureException("No classes folder");
		if (!classesFolder.files  .isEmpty()) throw new EntryStructureException("Found unexpected files in classes folder \"%s\"", classesFolder.getPath());
		if ( classesFolder.folders.isEmpty()) throw new EntryStructureException("Found no sub folders in classes folder \"%s\"", classesFolder.getPath());
		
		HashMap<String,Class_> classes = new HashMap<>();
		
		for (ZipEntryTreeNode folderNode : classesFolder.folders.values()) {
			Class_ class_ = new Class_(zipFile, dlc, folderNode, globalTemplates);
			if (class_!=null)
				classes.put(folderNode.name, class_);
		}
		return classes;
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

	private static void testContentRootFolder(ZipFile zipFile, ZipEntryTreeNode contentRootFolder) {
		HashMap<String,ZipEntryTreeNode> fileNames = new HashMap<>();
		contentRootFolder.traverseFiles(fileNode->{
			
			//if (fileNode.getPath().equals("\\[media]\\classes\\trucks\\step_310e.xml"))
			//	showBytes(zipFile, fileNode, 30);
		
			// all files are XML
			if (!fileNode.name.endsWith(".xml"))
				System.err.printf("Found a Non XML File: %s%n", fileNode.getPath());
			
			//// dublicate filenames
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

	static class Class_ {
		
		final String dlc;
		final String name;
		final HashMap<String,HashMap<String,Item>> subItems;
		final HashMap<String,Item> mainItems;
	
		Class_(ZipFile zipFile, String dlc, ZipEntryTreeNode classFolder, HashMap<String,Templates> globalTemplates) throws IOException, EntryStructureException, ParseException {
			this.dlc = dlc;
			if (globalTemplates==null) throw new IllegalArgumentException();
			if ( classFolder==null) throw new IllegalArgumentException();
			name = classFolder.name;
			
			//if (!classFolder.folders.isEmpty()) {
			//	System.err.printf("Found unexpected folders in class folder \"%s\"%n", classFolder.getPath());
			//	//throw new EntryStructureException("Found unexpected folders in class folder \"%s\"", classFolder.getPath());
			//}
			//if ( classFolder.files.isEmpty()) {
			//	System.err.printf("Found no files in class folder \"%s\"%n", classFolder.getPath());
			//	//throw new EntryStructureException("Found no files in class folder \"%s\"", classFolder.getPath());
			//}
			
			subItems = new HashMap<>();
			for (ZipEntryTreeNode subItemFolder : classFolder.folders.values()) {
				if (!subItemFolder.folders.isEmpty()) throw new EntryStructureException("Found unexpected folders in sub item folder \"%s\"", subItemFolder.getPath());
				if ( subItemFolder.files  .isEmpty()) throw new EntryStructureException("Found no item files in sub item folder \"%s\"", subItemFolder.getPath());
				
				HashMap<String, Item> subItems1 = new HashMap<>();
				for (ZipEntryTreeNode itemFile : subItemFolder.files.values()) {
					Item item = Item.read(zipFile, itemFile, globalTemplates);
					if (item!=null) subItems1.put(itemFile.name, item);
				}
				subItems.put(subItemFolder.name, subItems1);
			}
			
			mainItems = new HashMap<>();
			for (ZipEntryTreeNode itemFile : classFolder.files.values()) {
				Item item = Item.read(zipFile, itemFile, globalTemplates);
				if (item!=null) mainItems.put(itemFile.name, item);
			}
		}
		
		static class Item {
	
			final String name;
			final Templates localTemplates;

			static Item read(ZipFile zipFile, ZipEntryTreeNode itemFile, HashMap<String,Templates> globalTemplates) throws IOException, EntryStructureException, ParseException {
				if (globalTemplates==null) throw new IllegalArgumentException();
				if (itemFile==null) throw new IllegalArgumentException();
				if (!itemFile.isfile()) throw new IllegalStateException();
				
				//System.out.printf("Read Item \"%s\" ...%n", itemFile.getPath());
				
				NodeList nodes = readXML(zipFile, itemFile);
				if (nodes==null) {
					System.err.printf("Can't read xml file \"%s\" --> Item will be ignored%n", itemFile.getPath());
					return null;
				}
				
				Item item = new Item(nodes, itemFile, globalTemplates);
				
				//System.out.printf("... done [read item]%n");
				return item;
			}
			Item(NodeList nodes, ZipEntryTreeNode itemFile, HashMap<String,Templates> globalTemplates) throws IOException, EntryStructureException, ParseException {
				if (globalTemplates==null) throw new IllegalArgumentException();
				if (itemFile==null) throw new IllegalArgumentException();
				if (nodes==null) throw new IllegalArgumentException();
				
				Node templatesNode = null;
				Node parentNode = null;
				Node contentNode = null;
				
				for (Node node:XML.makeIterable(nodes)) {
					if (node.getNodeType()==Node.COMMENT_NODE) {
						// is Ok, do nothing
							
					} else if (node.getNodeType()==Node.TEXT_NODE) {
						String nodeValue = node.getNodeValue();
						if (nodeValue!=null && !nodeValue.trim().isEmpty()) {
							showNodes(nodes);
							throw new ParseException("Found unexpected text in item file \"%s\": \"%s\" %s", itemFile.getPath(), nodeValue, Arrays.toString(nodeValue.getBytes()));
						}
						
					} else if (node.getNodeType()==Node.ELEMENT_NODE) {
						
						if (node.getNodeName().equals("_templates")) {
							if (templatesNode!=null)
								throw new ParseException("Found more than one <_templates> node in item file \"%s\"", itemFile.getPath());
							templatesNode = node;
							
						} else if (node.getNodeName().equals("_parent")) {
							if (parentNode!=null)
								throw new ParseException("Found more than one <_parent> node in item file \"%s\"", itemFile.getPath());
							parentNode = node;
							
						} else {
							if (contentNode!=null)
								throw new ParseException("Found more than one content node in item file \"%s\": <%s>, <%s>", itemFile.getPath(), node.getNodeName(), contentNode.getNodeName());
							contentNode = node;
						}
						
					} else
						throw new ParseException("Found unexpected node in item file \"%s\": %s", itemFile.getPath(), XML.toDebugString(node));
				}
				if (contentNode==null)
					throw new ParseException("Found no content node in item file \"%s\"", itemFile.getPath());
				
				localTemplates = templatesNode==null ? null : new Templates(templatesNode, globalTemplates);
				
				this.name = contentNode.getNodeName();
				parseItemTontent(contentNode);
				
			}

			private void parseItemTontent(Node contentNode) {
				// TODO Auto-generated method stub
				
			}
			
		}
	}

	static class Templates {
	
		Templates(Node templatesNode, HashMap<String,Templates> globalTemplates) {
			if (templatesNode==null) throw new IllegalArgumentException();
			
			// TODO Auto-generated method stub
		}
	}

}
