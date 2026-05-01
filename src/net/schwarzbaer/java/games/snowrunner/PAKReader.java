package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PAKReader {

	static ZipEntryTreeNode readPAK(File pakFile) {
		try (ZipFile zipFile = new ZipFile(pakFile, ZipFile.OPEN_READ); ) {
			System.out.printf("Read \"%s\" ...%n", pakFile.getAbsolutePath());
			ZipEntryTreeNode zipRoot = new ZipEntryTreeNode();
			
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				
				InputStream stream = zipFile.getInputStream(entry);
				byte[] rawBytes = stream!=null ? stream.readAllBytes() : null;
				
				zipRoot.addChild(entry, rawBytes);
				
				//String entryName = entry.getName();
				//System.out.printf("   \"%s\"%n", entryName);
			}
			
			System.out.printf("... done%n");
			return zipRoot;
			
		} catch (IOException e) { e.printStackTrace(); }
		
		System.out.printf("... done with error%n");
		return null;
	}

	public static class ZipEntryTreeNode
	{
		       final ZipEntryTreeNode parent;
		public final String name;
		public final String path;
		       final ZipEntry entry;
		public final HashMap<String, ZipEntryTreeNode> folders;
		public final HashMap<String, ZipEntryTreeNode> files;
		public final byte[] rawBytes;
		       String textContent;
		       boolean wasParsed;

		private ZipEntryTreeNode() { // root
			this(null,"",null,null);
		}
		
		private ZipEntryTreeNode(ZipEntryTreeNode parent, String name) { // folder
			this(parent,name,null,null);
		}
		
		private ZipEntryTreeNode(ZipEntryTreeNode parent, String name, ZipEntry entry, byte[] rawBytes) {
			this.parent = parent;
			this.name = name;
			this.entry = entry;
			this.rawBytes = rawBytes;
			this.textContent = null;
			this.wasParsed = false;
			this.folders = this.entry!=null ? null : new HashMap<>();
			this.files   = this.entry!=null ? null : new HashMap<>();
			if (this.parent==null) path = name;
			else path = this.parent.path+"\\"+name;
		}
		
		boolean isfile() {
			return entry!=null;
		}
		
		void setTextContent(String textContent, boolean wasParsed)
		{
			this.textContent = textContent;
			this.wasParsed = wasParsed;
		}
		
		public boolean hasTextContent()
		{
			return textContent!=null;
		}
		
		public String getTextContent()
		{
			return textContent;
		}
		
		public boolean wasParsed()
		{
			return wasParsed;
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

		private void addChild(ZipEntry entry, byte[] rawBytes) {
			addChild(
					entry.getName().split("\\\\"), 0,
					entry, rawBytes
			);
		}

		private void addChild(String[] names, int index, ZipEntry entry, byte[] rawBytes) {
			String name = names[index];
			if (index==names.length-1)
				files
					.put(name, new ZipEntryTreeNode(this, name, entry, rawBytes));
			else
				folders
					.computeIfAbsent(name, n -> new ZipEntryTreeNode(this, n))
					.addChild(names, index+1, entry, rawBytes);
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
