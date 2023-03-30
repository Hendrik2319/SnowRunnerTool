package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class PAKReader {

	static <ValueType> ValueType readPAK(File pakFile, NextParsingStage<ValueType> nextParsingStage) {
		try (ZipFile zipFile = new ZipFile(pakFile, ZipFile.OPEN_READ); ) {
			System.out.printf("Read \"%s\" ...%n", pakFile.getAbsolutePath());
			ZipEntryTreeNode zipRoot = new ZipEntryTreeNode();
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				zipRoot.addChild(entry);
				//String entryName = entry.getName();
				//System.out.printf("   \"%s\"%n", entryName);
			}
			
			ValueType data = nextParsingStage.parse(zipFile, zipRoot);
			System.out.printf("... done%n");
			return data;
			
		} catch (IOException e) { e.printStackTrace(); }
		
		System.out.printf("... done with error%n");
		return null;
	}
	
	interface NextParsingStage<ValueType> {
		ValueType parse(ZipFile zipFile, ZipEntryTreeNode zipRoot) throws IOException;
	}

	static class ZipEntryTreeNode {
		final ZipEntryTreeNode parent;
		final String name;
		final String path;
		final ZipEntry entry;
		final HashMap<String, ZipEntryTreeNode> folders;
		final HashMap<String, ZipEntryTreeNode> files;

		private ZipEntryTreeNode() { // root
			this(null,"",null);
		}
		
		private ZipEntryTreeNode(ZipEntryTreeNode parent, String name, ZipEntry entry) {
			this.parent = parent;
			this.name = name;
			this.entry = entry;
			this.folders = this.entry!=null ? null : new HashMap<>();
			this.files   = this.entry!=null ? null : new HashMap<>();
			if (this.parent==null) path = name;
			else path = this.parent.path+"\\"+name;
		}
		
		boolean isfile() {
			return entry!=null;
		}

		//String getPath() {
		//	return path;
		//	//if (parent==null) return name;
		//	//return parent.getPath()+"\\"+name;
		//}

		void forEachChild(Consumer<ZipEntryTreeNode> action) {
			files.values().forEach(action);
			folders.values().forEach(action);
		}

		void traverseFiles(Consumer<ZipEntryTreeNode> action) {
			files.values().forEach(action);
			folders.values().forEach(node->node.traverseFiles(action));
		}

		private void addChild(ZipEntry entry) {
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
	
}
