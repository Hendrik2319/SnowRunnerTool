package net.schwarzbaer.java.games.snowrunner.test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class InspectGfxBundle {

	public static void main(String[] args) {
		String path = "c:\\__Games\\_GameData\\_Saves\\SnowRunner\\boot.pak_\\[gfx]\\gfxbundle.gfxbundle";
		
		try (FileInputStream in = new FileInputStream(path)) {
			
			readGfxBundle(in);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readGfxBundle(FileInputStream in) throws IOException {
		RawDataReader reader = new RawDataReader(in);
		
		try {
			String fileTypeMarker = reader.readFixedString(4);
			System.out.printf("FileTypeMarker: %s%n", fileTypeMarker); 
			
		} catch (RawDataReader.ParseException e) {
			e.printStackTrace();
		}
		
	}

	private static class RawDataReader {

		private BufferedInputStream in;

		public RawDataReader(InputStream in) {
			this.in = new BufferedInputStream(in);
		}
		
		@SuppressWarnings("unused")
		private static class ParseException extends Exception {
			private static final long serialVersionUID = -1946298227452094505L;
			
			ParseException(String msg) {
				super(msg);
			}
			ParseException(String format, Object... objects) {
				super(String.format(format, objects));
			}
		}

		public String readFixedString(int length) throws IOException, ParseException {
			byte[] bytes = new byte[4];
			int n = in.read(bytes);
			if (n<0     ) throw new ParseException("EndOfFile reached: Can't read any bytes of FixedString(%d)", length);
			if (n<length) throw new ParseException("EndOfFile reached: Can't read all bytes of FixedString(%d). %d bytes read.", length, n);
			return new String(bytes);
		}
	
	}
}
