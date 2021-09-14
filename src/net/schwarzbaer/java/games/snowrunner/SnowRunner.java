package net.schwarzbaer.java.games.snowrunner;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListDataListener;

import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.snowrunner.Data.Truck;
import net.schwarzbaer.system.Settings;

public class SnowRunner {

	private static final AppSettings settings = new AppSettings();

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new SnowRunner().initialize();
	}


	private final StandardMainWindow mainWindow;
	private final JFileChooser folderChooser;
	private Data data;
	private final JList<TruckListModel.Item> truckList;

	SnowRunner() {
		data = null;
		
		mainWindow = new StandardMainWindow("SnowRunner Tool");
		
		folderChooser = new JFileChooser("./");
		folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		folderChooser.setMultiSelectionEnabled(false);
		
		truckList = new JList<>();
		JScrollPane truckListScrollPane = new JScrollPane(truckList);
		truckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		truckList.addListSelectionListener(e->{
			TruckListModel.Item selectedValue = truckList.getSelectedValue();
			// TODO Auto-generated method stub
		});
		
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weighty = 1;
		contentPane.add(truckListScrollPane,c);
		
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = menuBar.add(new JMenu("File"));
		fileMenu.add(createMenuItem("Reload \"initial.pak\"", true, e->{
			boolean changed = reloadInitialPAK();
			if (changed) updateAfterDataChanged();
		}));
		
		mainWindow.startGUI(contentPane, menuBar);
		
		if (settings.isSet(AppSettings.ValueGroup.WindowPos )) mainWindow.setLocation(settings.getWindowPos ());
		if (settings.isSet(AppSettings.ValueGroup.WindowSize)) mainWindow.setSize    (settings.getWindowSize());
		
		mainWindow.addComponentListener(new ComponentListener() {
			@Override public void componentShown  (ComponentEvent e) {}
			@Override public void componentHidden (ComponentEvent e) {}
			@Override public void componentResized(ComponentEvent e) { settings.setWindowSize( mainWindow.getSize() ); }
			@Override public void componentMoved  (ComponentEvent e) { settings.setWindowPos ( mainWindow.getLocation() ); }
		});
	}
	
	private void initialize() {
		boolean changed = reloadInitialPAK();
		if (changed) updateAfterDataChanged();
	}

	private boolean reloadInitialPAK() {
		File folder = getSteamLibraryFolder();
		if (folder!=null) {
			File file = new File(folder,"steamapps/common/SnowRunner/preload/paks/client/initial.pak");
			Data newData = Data.readInitialPAK(file);
			if (newData!=null) {
				data = newData;
				return true;
			}
		}
		return false;
	}


	private File getSteamLibraryFolder() {
		File folder = settings.getFile(AppSettings.ValueKey.SteamLibraryFolder, null);
		if (folder==null) {
			folderChooser.setDialogTitle("Select SteamLibrary Folder");
			if (folderChooser.showOpenDialog(mainWindow)!=JFileChooser.APPROVE_OPTION)
				return null;
			folder = folderChooser.getSelectedFile();
			if (!folder.isDirectory())
				return null;
			settings.putFile(AppSettings.ValueKey.SteamLibraryFolder, folder);
		}
		return folder;
	}

	private void updateAfterDataChanged() {
		truckList.setModel(new TruckListModel(data));
	}

	private JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		comp.setEnabled(isEnabled);
		return comp;
	}
	
	private static class TruckListModel implements ListModel<TruckListModel.Item> {
		
		private final Vector<ListDataListener> listDataListeners;
		private final Vector<Item> items;
		
		public TruckListModel(Data data) {
			listDataListeners = new Vector<>();
			items = new Vector<>();
			
			data.defaultTrucks.forEach((name,truck)->{
				items.add(new Item(null, name,truck));
			});
			
			data.dlcList.forEach(dlc->{
				dlc.trucks.forEach((name,truck)->{
					items.add(new Item(dlc.name, name,truck));
				});
			});
			
			items.sort(Comparator.<Item,String>comparing(item->item.name));
		}

		@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
		@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l); }

		@Override public int getSize() { return items.size(); }
		@Override public Item getElementAt(int index) { return items.elementAt(index); }

		private static class Item {

			private final String dlcName;
			private final String name;
			private final Truck truck;

			public Item(String dlcName, String name, Truck truck) {
				this.dlcName = dlcName;
				this.name = name;
				this.truck = truck;
			}

			@Override public String toString() {
				if (dlcName==null) return name;
				return String.format("[%s] %s", dlcName, name);
			}
		}
		
	}
	
	private static class AppSettings extends Settings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		enum ValueKey {
			WindowX, WindowY, WindowWidth, WindowHeight, SteamLibraryFolder,
		}

		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			WindowPos (ValueKey.WindowX, ValueKey.WindowY),
			WindowSize(ValueKey.WindowWidth, ValueKey.WindowHeight),
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		public AppSettings() { super(SnowRunner.class); }
		public Point     getWindowPos (              ) { return getPoint(ValueKey.WindowX,ValueKey.WindowY); }
		public void      setWindowPos (Point location) {        putPoint(ValueKey.WindowX,ValueKey.WindowY,location); }
		public Dimension getWindowSize(              ) { return getDimension(ValueKey.WindowWidth,ValueKey.WindowHeight); }
		public void      setWindowSize(Dimension size) {        putDimension(ValueKey.WindowWidth,ValueKey.WindowHeight,size); }
	}
}
