package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Data {
	
	public static Data readInitialPAK(String path) {
		return readInitialPAK(new File(path));
	}

	public static Data readInitialPAK(File file) {
		
		try (ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ); ) {
			ZipEntryTreeNode zipRoot = new ZipEntryTreeNode();
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				zipRoot.addChild(entry);
				String entryName = entry.getName();
				System.out.printf("\"%s\"%n", entryName);
			}
			
			return new Data(zipFile, zipRoot);
		} catch (IOException e) { e.printStackTrace(); }
		
		return null;
	}

	final HashMap<String,Language> languages;
	final TrucksTemplate trucksTemplate;
	final HashMap<String,Wheel> defaultWheels;
	final HashMap<String,Truck> defaultTrucks;
	final Vector<DLC> dlcList;

	Data(ZipFile zipFile, ZipEntryTreeNode zipRoot) throws IOException {
		Predicate<String> isXML = fileName->fileName.endsWith(".xml");
		Predicate<String> isSTR = fileName->fileName.endsWith(".str");
		
		languages = new HashMap<>();
		ZipEntryTreeNode[] languageNodes = zipRoot.getSubFiles("[strings]", isSTR);
		readEntries(zipFile, languageNodes, languages, (name,input)->Language.readFrom(name, input));
		
		ZipEntryTreeNode trucksTemplateNode = zipRoot.getSubFile ("[media]\\_templates\\", "trucks.xml");
		trucksTemplate = TrucksTemplate.readFrom(zipFile.getInputStream(trucksTemplateNode.entry));
		
		defaultWheels = new HashMap<>();
		ZipEntryTreeNode[] defaultWheelNodes = zipRoot.getSubFiles("[media]\\classes\\wheels\\", isXML);
		readEntries(zipFile, defaultWheelNodes, defaultWheels, (name,input)->Wheel.readFrom(name, input, trucksTemplate));
		
		defaultTrucks = new HashMap<>();
		ZipEntryTreeNode[] defaultTruckNodes = zipRoot.getSubFiles("[media]\\classes\\trucks\\", isXML);
		readEntries(zipFile, defaultTruckNodes, defaultTrucks, (name,input)->Truck.readFrom(name, input, trucksTemplate, defaultWheels));
		
		dlcList = new Vector<>();
		ZipEntryTreeNode[] dlcNodes = zipRoot.getSubFolders("[media]\\_dlc");
		for (ZipEntryTreeNode dlcNode:dlcNodes) {
			DLC dlc = new DLC(dlcNode.name);
			dlcList.add(dlc);
			
			ZipEntryTreeNode[] wheelNodes = dlcNode.getSubFiles("classes\\wheels\\", isXML);
			if (wheelNodes!=null)
				readEntries(zipFile, wheelNodes, dlc.wheels, (name,input)->Wheel.readFrom(name, input, trucksTemplate));
			
			ZipEntryTreeNode[] truckNodes = dlcNode.getSubFiles("classes\\trucks\\", isXML);
			if (truckNodes!=null)
				readEntries(zipFile, truckNodes, dlc.trucks, (name,input)->Truck.readFrom(name, input, trucksTemplate, defaultWheels, dlc.wheels));
		}
		
	}
	
	static class DLC {
		final String name;
		final HashMap<String,Wheel> wheels;
		final HashMap<String,Truck> trucks;

		public DLC(String name) {
			this.name = name;
			wheels = new HashMap<>();
			trucks = new HashMap<>();
		}
		
	}
	
	private <ValueType> void readEntries(ZipFile zipFile, ZipEntryTreeNode[] nodes, HashMap<String,ValueType> targetMap, BiFunction<String,InputStream,ValueType> readXML) throws IOException {
		for (ZipEntryTreeNode node:nodes) {
			InputStream input = zipFile.getInputStream(node.entry);
			targetMap.put(node.name, readXML.apply(node.name, input));
		}
	}
	
	private static class ZipEntryTreeNode {
		@SuppressWarnings("unused")
		final ZipEntryTreeNode parent;
		final String name;
		final ZipEntry entry;
		final HashMap<String, ZipEntryTreeNode> folders;
		final HashMap<String, ZipEntryTreeNode> files;

		ZipEntryTreeNode() { // root
			this(null,"root",null);
		}
		
		ZipEntryTreeNode(ZipEntryTreeNode parent, String name, ZipEntry entry) {
			this.parent = parent;
			this.name = name;
			this.entry = entry;
			this.folders = new HashMap<>();
			this.files   = new HashMap<>();
		}

		void addChild(ZipEntry entry) {
			String entryName = entry.getName();
			String[] names = entryName.split("\\\\");
			addChild(names,0,entry);
		}

		private void addChild(String[] names, int index, ZipEntry entry) {
			String name = names[index];
			if (index==names.length-1) {
				files.put(name, new ZipEntryTreeNode(this, name, entry));
				return;
			} else {
				ZipEntryTreeNode folderNode = folders.get(name);
				if (folderNode==null)
					folders.put(name, folderNode = new ZipEntryTreeNode(this, name, null));
				folderNode.addChild(names, index+1, entry);
			}
		}

		ZipEntryTreeNode getSubFile(String folderPath, String fileName) {
			ZipEntryTreeNode folderNode = getSubFolder(folderPath);
			if (folderNode==null) return null;
			return folderNode.files.get(fileName);
		}

		@SuppressWarnings("unused")
		ZipEntryTreeNode[] getSubFiles(String folderPath) {
			return getSubFiles(folderPath, str->true);
		}
		
		ZipEntryTreeNode[] getSubFiles(String folderPath, Predicate<String> checkFileName) {
			ZipEntryTreeNode folderNode = getSubFolder(folderPath);
			if (folderNode==null) return null;
			return folderNode.files
					.values()
					.stream()
					.filter(fileNode->checkFileName.test(fileNode.name))
					.toArray(ZipEntryTreeNode[]::new);
		}

		ZipEntryTreeNode getSubFolder(String folderPath) {
			while (folderPath.endsWith("\\"))
				folderPath = folderPath.substring(0, folderPath.length()-1);
			String[] pathStrs = folderPath.split("\\\\");
			ZipEntryTreeNode folderNode = this;
			for (String str:pathStrs) {
				if (folderNode==null) break;
				folderNode = folderNode.folders.get(str);
			}
			return folderNode;
		}

		ZipEntryTreeNode[] getSubFolders(String folderPath) {
			ZipEntryTreeNode folderNode = getSubFolder(folderPath);
			if (folderNode==null) return null;
			return folderNode.folders.values().stream().toArray(ZipEntryTreeNode[]::new);
		}
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
	
	static class TrucksTemplate {

		static TrucksTemplate readFrom(InputStream input) {
			// TODO Auto-generated method stub
			return null;
		}
	
	}

	static class Wheel {

		static Wheel readFrom(String name, InputStream input, TrucksTemplate trucksTemplate) {
			// TODO Auto-generated method stub
			return null;
		}
	
	}

	static class Truck {

		static Truck readFrom(String name, InputStream input, TrucksTemplate trucksTemplate, HashMap<String, Wheel> defaultWheels) {
			// TODO Auto-generated method stub
			return null;
		}

		static Truck readFrom(String name, InputStream input, TrucksTemplate trucksTemplate, HashMap<String, Wheel> defaultWheels, HashMap<String, Wheel> dlcWheels) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
