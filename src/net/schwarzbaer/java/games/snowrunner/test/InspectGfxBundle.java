package net.schwarzbaer.java.games.snowrunner.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class InspectGfxBundle {

	public static void main(String[] args) {
//		String path = "c:\\__Games\\_GameData\\_Saves\\SnowRunner\\boot.pak_\\[gfx]\\gfxbundle.gfxbundle";
//		
//		try (FileInputStream in = new FileInputStream(path)) {
//			
//			GfxBundleReader reader = new GfxBundleReader(in);
//			reader.readHeader();
//			
////			reader.readAllItems((name,bytes_)->{
////				File outFile = new File("c:\\__Games\\_GameData\\_Saves\\SnowRunner\\gfxbundle.gfxbundle_\\"+name);
////				try {
////					log("Write item \"%s\" to file \"%s\" ...", name, outFile.getAbsolutePath());
////					Files.write(outFile.toPath(), bytes_, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
////					log(" done%n");
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
////			});
//
////			//String fileToExtract = "icons_lib.gfx";
////			//String fileToExtract = "font_en.gfx";
////			//String fileToExtract = "font_jp.gfx";
////			//String fileToExtract = "ui_root.gfx";
////			String fileToExtract = "trucks_lib.gfx";
////			
////			byte[] bytes = reader.readFile(fileToExtract);
//////			File outFile = new File("c:\\__Games\\_GameData\\_Saves\\SnowRunner\\"+fileToExtract);
//////			Files.write(outFile.toPath(), bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
////			
////			if (bytes!=null)
////				readGfx("icons_lib.gfx", bytes);
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		
		String[] testGFX = new String[] { "icons_lib.gfx", "ui_root.gfx", "trucks_lib.gfx", "exit_menu.gfx", "region_map.gfx" };
		for (String filename : testGFX) {
			
			File testFile = new File("c:\\__Games\\_GameData\\_Saves\\SnowRunner\\gfxbundle.gfxbundle_\\"+filename);
			try {
				log("%nRead content of \"%s\"%n", filename);
				byte[] bytes = Files.readAllBytes(testFile.toPath());
				readGfx(filename, bytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private static void log(String format, Object... args) {
		System.out.printf(format, args);
	}

	private static void readGfx(String label, byte[] bytes) {
		log("%nRead \"%s\"%n", label);
		RawDataReader reader = new RawDataReader(new ByteArrayInputStream(bytes));
		
		try {
			
			log("FileTypeMarker     : %s%n", reader.readFixedString(3));
			log("Unknown Bytes (30) : %s%n", toHexString(reader.readBytes(new byte[30])));
			log("File Label         : %s%n", reader.readVarString8());
			log("Unknown Bytes (2)  : %s%n", toHexString(reader.readBytes(new byte[2])));
			log("Items: [ <unknown count> ]%n");
			for (int i=0; i<100000; i++ ) {
				//log("   [%02X]", i);
				byte[] unknownBytes = reader.readBytes(new byte[13]);
				//log("  %s", toHexString(unknownBytes));
				if (unknownBytes[0]==0x44 && unknownBytes[1]==0x11 && unknownBytes[2]==0x08) {
					log("   [%02X]", i);
					log("  %s", toHexString(unknownBytes));
					log("%n");
					break;
				}
				reader.readVarString8();
				//log("  %s%n", reader.readVarString8());
			}
			log("Unknown Bytes (6)  : %s%n", toHexString(reader.readBytes(new byte[6])));
			log("Unknown Label      : %s%n", reader.readZeroTermString());
			log("Unknown Bytes (20) : %s%n", toHexString(reader.readBytes(new byte[20])));
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RawDataReader.ParseException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String toHexString(byte[] bytes) {
		return toString(bytes, "%02X");
	}

	private static String toString(byte[] bytes, String format) {
		if (bytes.length==0) return "[]";
		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;
		for (byte b:bytes) {
			if (!isFirst) sb.append(", ");
			else isFirst = false;
			sb.append(String.format(format, b));
		}
		return String.format("[ %s ]", sb.toString());
	}

	private static class GfxBundleReader {
		
		private int state;
		private final RawDataReader reader;
		private BundleItem[] items;

		GfxBundleReader(InputStream in) {
			reader = new RawDataReader(in);
			state = 0;
			items = null;
		}
		
		void readHeader() throws IOException {
			if (state!=0)
				throw new IllegalStateException();
			
			log("%nRead GFX Bundle%n");
			
			try {
				
				log("FileTypeMarker    : %s%n", reader.readFixedString(4));
				log("Unknown Bytes (7) : %s%n", toHexString(reader.readBytes(new byte[7])));
				int itemCount = reader.readInt32();
				log("Item Count        : %d%n", itemCount);
				log("Unknown Bytes (5) : %s%n", toHexString(reader.readBytes(new byte[5])));
				
				items = new BundleItem[itemCount];
				
				log("Item Names: [%d]%n", itemCount);
				for (int i=0; i<itemCount; i++) {
					String name = reader.readVarString32();
					log("   [%d] \"%s\"%n", i, name);
					items[i] = new BundleItem(name);
				}
				log("Unknown Bytes (1) : %s%n", toHexString(reader.readBytes(new byte[1])));
				
				int relativeAddress = 0;
				log("Item Lengths: [%d]%n", itemCount);
				for (int i=0; i<itemCount; i++) {
					int length = reader.readInt32();
					log("   [%d] \"%s\" -> %d bytes @%08X:%08X%n", i, items[i].name, length, relativeAddress, relativeAddress+length);
					items[i].length = length;
					items[i].relativeAddress = relativeAddress;
					relativeAddress += length;
				}
				
				
			} catch (RawDataReader.ParseException e) {
				state = -1;
				e.printStackTrace();
			}
			
			state = 1;
		}
		
		byte[] readFile(String fileToExtract) throws IOException {
			if (state!=1)
				throw new IllegalStateException();
			
			try {
				if (fileToExtract!=null) {
					BundleItem requestedItem = null;
					for (BundleItem item:items) {
						if (item.name.equals(fileToExtract)) {
							requestedItem = item;
							break;
						}
					}
					if (requestedItem != null) {
						log("Read content of item \"%s\" ...%n", requestedItem.name);
						reader.skip(requestedItem.relativeAddress);
						byte[] bytes = reader.readBytes(new byte[requestedItem.length]);
						log("... done%n");
						
						state = 2;
						return bytes;
					}
				}
			} catch (RawDataReader.ParseException e) {
				e.printStackTrace();
			}
			
			state = 2;
			return null;
		}

		void readAllItems(BiConsumer<String,byte[]> action) throws IOException {
			if (state!=1)
				throw new IllegalStateException();
			if (action==null)
				throw new IllegalArgumentException();
			
			try {
				for (BundleItem item:items) {
					log("Read content of item \"%s\" ...", item.name);
					byte[] bytes = reader.readBytes(new byte[item.length]);
					log(" done%n");
					action.accept(item.name, bytes);
				}
			} catch (RawDataReader.ParseException e) {
				e.printStackTrace();
			}
			
			state = 2;
		}

		private static class BundleItem {
		
			int relativeAddress;
			int length;
			final String name;
		
			public BundleItem(String name) {
				this.name = name;
				length = -1;
				relativeAddress = -1;
			}
		
		}
		
		
	}

	private static class RawDataReader {

		private BufferedInputStream in;

		public RawDataReader(InputStream in) {
			this.in = new BufferedInputStream(in);
		}

		public String readZeroTermString() throws IOException {
			String str = "";
			for (int b=in.read(); b>0; b=in.read())
				str += (char)b;
			return str;
		}

		public String readVarString8() throws IOException, ParseException {
			int length = readByte("Length of VarString8");
			return readFixedString(length, String.format("VarString8[%d]",length));
		}

		public String readVarString32() throws IOException, ParseException {
			int length = readInt32("Length of VarString32");
			return readFixedString(length, String.format("VarString32[%d]",length));
		}

		public int readInt32() throws IOException, ParseException {
			return readInt32("Int32");
		}

		private int readInt32(String label) throws IOException, ParseException {
			byte[] b = readBytes(new byte[4], label);
			int value = 0;
			value |= (b[0] & 0xFF) <<  0;
			value |= (b[1] & 0xFF) <<  8;
			value |= (b[2] & 0xFF) << 16;
			value |= (b[3] & 0xFF) << 24;
			return value;
		}

		public String readFixedString(int length) throws IOException, ParseException {
			return readFixedString(length, String.format("FixedString(%d)", length));
		}

		private String readFixedString(int length, String label) throws IOException, ParseException {
			byte[] bytes = readBytes(new byte[length], label);
			return new String(bytes);
		}

		public byte readByte() throws IOException, ParseException {
			String label = "Byte";
			return readByte(label);
		}

		private byte readByte(String label) throws IOException, ParseException {
			byte[] bytes = readBytes(new byte[1], label);
			return bytes[0];
		}

		public byte[] readBytes(byte[] bytes) throws IOException, ParseException {
			return readBytes(bytes, String.format("Bytes(%d)", bytes.length));
		}

		private byte[] readBytes(byte[] bytes, String label) throws IOException, ParseException {
			int n = in.read(bytes);
			if (n<0           ) throw new ParseException("EndOfFile reached: Can't read any bytes of %s.", label);
			if (n<bytes.length) throw new ParseException("EndOfFile reached: Can't read all bytes of %s. %d bytes read.", label, n);
			return bytes;
		}

		public void skip(int length) throws IOException, ParseException {
			readBytes(new byte[length], String.format("BytesToSkip(%d)", length));
		}

		private static class ParseException extends Exception {
			private static final long serialVersionUID = -1946298227452094505L;
			
			ParseException(String msg) {
				super(msg);
			}
			ParseException(String format, Object... objects) {
				super(String.format(format, objects));
			}
		}
	
	}
}
