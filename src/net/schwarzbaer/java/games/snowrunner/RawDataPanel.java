package net.schwarzbaer.java.games.snowrunner;

import java.awt.Window;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.TextAreaDialog;
import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.DataTrees.AbstractTreeNode;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.NV;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.V;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.Controllers.ListenerSource;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.system.ClipboardTools;

class RawDataPanel extends JTabbedPane implements LanguageListener, DataReceiver, ListenerSource {
	private static final long serialVersionUID = 10671596986103400L;
	
	private Data data;
	private Language language;
	private boolean isShowingSaveGameDataSorted;

	private final JTabbedPane globalTemplatesPanel;
	private final JTabbedPane classesPanel;
	private final Window window;
	private final JTabbedPane saveGameDataPanel;
	private SaveGameData saveGameData;

	RawDataPanel(Window window, SnowRunner.Controllers controllers) {
		super();
		this.window = window;
		data = null;
		language = null;
		saveGameData = null;
		isShowingSaveGameDataSorted = SnowRunner.settings.getBool(SnowRunner.AppSettings.ValueKey.ShowingSaveGameDataSorted, false);
		
		globalTemplatesPanel = new JTabbedPane();
		globalTemplatesPanel.setBorder(BorderFactory.createTitledBorder("Global Templates"));
		
		classesPanel = new JTabbedPane();
		classesPanel.setBorder(BorderFactory.createTitledBorder("Classes"));
		
		JSplitPane xmlTempStructPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		xmlTempStructPanel.setLeftComponent(globalTemplatesPanel);
		xmlTempStructPanel.setRightComponent(classesPanel);
		
		saveGameDataPanel = new JTabbedPane();
		
		addTab("XML Template Structure", xmlTempStructPanel);
		addTab("SaveGame Data", saveGameDataPanel);
		
		updatePanels();
		controllers.languageListeners.add(this,this);
		controllers.dataReceivers.add(this,this);
	}
	
	void showSaveGameDataSorted(boolean isShowingSaveGameDataSorted) {
		this.isShowingSaveGameDataSorted = isShowingSaveGameDataSorted;
		SnowRunner.settings.putBool(SnowRunner.AppSettings.ValueKey.ShowingSaveGameDataSorted, this.isShowingSaveGameDataSorted);
		rebuildSaveGameDataPanel();
	}

	boolean isShowingSaveGameDataSorted() {
		return isShowingSaveGameDataSorted;
	}

	@Override
	public void setLanguage(Language language) {
		this.language = language;
	}

	@Override
	public void setData(Data data) {
		this.data = data;
		updatePanels();
	}

	void setData(SaveGameData saveGameData) {
		this.saveGameData = saveGameData;
		rebuildSaveGameDataPanel();
	}

	private void rebuildSaveGameDataPanel() {
		saveGameDataPanel.removeAll();
		addTreeTabs(saveGameDataPanel, this.saveGameData.rawJsonData, this::createJsonTreeNode);
	}
	
	private DataTrees.JsonTreeNode createJsonTreeNode(JSON_Data.Value<NV,V> value) {
		return new DataTrees.JsonTreeNode(value, isShowingSaveGameDataSorted);
	}

	private void updatePanels() {
		globalTemplatesPanel.removeAll();
		classesPanel.removeAll();
		
		if (data!=null && data.rawdata!=null) {
			addTreeTabs(globalTemplatesPanel, data.rawdata.globalTemplates, DataTrees.Templates_TreeNode::new);
			addTreeTabs(classesPanel        , data.rawdata.classes        , DataTrees.    Class_TreeNode::new);
		}
	}

	private <ValueType> void addTreeTabs(JTabbedPane panel, HashMap<String, ValueType> map, Function<ValueType,TreeNode> treeNodeContructor) {
		Vector<String> keys = new Vector<>( map.keySet() );
		keys.sort(null);
		for (String key : keys)
			panel.addTab(key, new TreePanel(treeNodeContructor.apply(map.get(key)), window, ()->language));
	}

	private static class TreePanel extends JScrollPane {
		private static final long serialVersionUID = 3719796829224851047L;
		private TreePath selectedPath;
		private AbstractTreeNode selectedTreeNode;

		TreePanel(TreeNode root, Window window, Supplier<Language> getLanguage) {
			selectedPath = null;
			
			JTree tree = new JTree(root);
			tree.setCellRenderer(new DataTrees.TreeNodeRenderer());
			
			setViewportView(tree);
			setBorder(null);
			
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.addTo(tree);
			
			JMenuItem miCopyPath = contextMenu.add(SnowRunner.createMenuItem("Copy Path", true, e->{
				ClipboardTools.copyStringSelectionToClipBoard(toString(selectedTreeNode.getPath()));
			}));
			
			JMenuItem miCopyName = contextMenu.add(SnowRunner.createMenuItem("Copy Name", true, e->{
				ClipboardTools.copyStringSelectionToClipBoard(selectedTreeNode.getName());
			}));
			
			JMenuItem miCopyValue = contextMenu.add(SnowRunner.createMenuItem("Copy Value", true, e->{
				ClipboardTools.copyStringSelectionToClipBoard(selectedTreeNode.getValue());
			}));
			
			JMenuItem miValue2Text = contextMenu.add(SnowRunner.createMenuItem("Use Value as StringID -> Show Text", true, e->{
				String value = selectedTreeNode.getValue();
				TextAreaDialog.showText(window, String.format("String ID \"%s\"", value), 500, 300, true, SnowRunner.solveStringID(value, getLanguage.get()));
			}));
			
			contextMenu.addContextMenuInvokeListener((comp, x, y) -> {
				selectedPath = tree.getPathForLocation(x, y);
				selectedTreeNode = null;
				Object selectedNode = null;;
				if (selectedPath!=null)
					selectedNode = selectedPath.getLastPathComponent();
				if (selectedNode instanceof AbstractTreeNode)
					selectedTreeNode = (AbstractTreeNode) selectedNode;
				miCopyPath  .setEnabled(selectedTreeNode!=null && selectedTreeNode.hasPath());
				miCopyName  .setEnabled(selectedTreeNode!=null && selectedTreeNode.hasName());
				miCopyValue .setEnabled(selectedTreeNode!=null && selectedTreeNode.hasValue());
				miValue2Text.setEnabled(selectedTreeNode!=null && selectedTreeNode.hasValue());
			});
		}

		private String toString(String[] path) {
			if (path==null) return "<null>";
			Iterable<String> it = ()->Arrays.stream(path).map(str->"\""+str+"\"").iterator();
			return String.join(", ", it);
		}
	}

}
