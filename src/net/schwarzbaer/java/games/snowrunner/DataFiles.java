package net.schwarzbaer.java.games.snowrunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class DataFiles
{
	private static final File dataFolder = new File("data");
	
	public enum DataFile
	{
		DLCAssignmentsFile     ("DLCAssignments.dat"     ),
		UserDefinedValuesFile  ("UserDefinedValues.dat"  ),
		FilterRowsPresetsFile  ("FilterRowsPresets.dat"  ),
		ColumnHidePresetsFile  ("ColumnHidePresets.dat"  ),
		RowColoringsFile       ("RowColorings.dat"       ),
		SpecialTruckAddonsFile ("SpecialTruckAddons.dat" ),
		WheelsQualityRangesFile("WheelsQualityRanges.dat"),
		;
		private final String name;
		private DataFile( String name ) { this.name = name; }
		
		private File getFile() { return new File(dataFolder, name); }
		private String getResourcePath() { return String.format("/%s", name); }
		
		public File       getFileForWriting      () { return DataFiles.getFileForWriting      (this); }
		public DataSource getDataSourceForReading() { return DataFiles.getDataSourceForReading(this); }
	}
	
	public static void checkDataFolder()
	{
		if (!dataFolder.isDirectory() && dataFolder.exists())
		{
			System.err.printf("WARNING:%n");
			System.err.printf("    Will not be able to create data folder at \"%s\".%n", dataFolder.getAbsolutePath());
			System.err.printf("    A file or something else exists at this position.%n");
			System.err.printf("    Writing of any data files will cause an exception, that will stop the application.%n");
		}
	}
	
	public static File getFileForWriting(DataFile dataFile)
	{
		if (dataFile==null)
			throw new IllegalArgumentException();
		
		if (!dataFolder.isDirectory())
		{
			if (dataFolder.exists())
				throw new RuntimeException(String.format("Can't write \"%s\". Could not create data folder at \"%s\". A file or something else exists at this position.", dataFile, dataFolder.getAbsolutePath()));
			
			boolean success = dataFolder.mkdir();
			if (!success)
				throw new RuntimeException(String.format("Can't write \"%s\". Could not create data folder at \"%s\".", dataFile, dataFolder.getAbsolutePath()));
		}
		
		return dataFile.getFile();
	}
	
	public static DataSource getDataSourceForReading(DataFile dataFile)
	{
		if (dataFile==null)
			throw new IllegalArgumentException();
		
		System.out.printf("getDataSourceForReading: %s%n", dataFile.getFile().getAbsolutePath());
		if (dataFile.getFile().isFile())
			return new DataSource(dataFile.getFile());
		
		return new DataSource(dataFile.getResourcePath());
	}


	public static class DataSource
	{
		private final File file;
		private final String resourcePath;

		private DataSource(File file)
		{
			this.file = Objects.requireNonNull(file);
			this.resourcePath = null;
		}

		private DataSource(String resourcePath)
		{
			this.resourcePath = Objects.requireNonNull(resourcePath);
			this.file = null;
		}
		
		public InputStream createInputStream() throws FileNotFoundException
		{
			if (file        !=null) return new FileInputStream(file);
			if (resourcePath!=null) return getClass().getResourceAsStream(resourcePath);
			throw new IllegalStateException();
		}

		@Override public String toString()
		{
			if (file        !=null) return String.format("file \"%s\"", file.getAbsolutePath());
			if (resourcePath!=null) return String.format("resource \"%s\"", resourcePath);
			throw new IllegalStateException();
		}
	}
}
