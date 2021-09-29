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
import net.schwarzbaer.java.games.snowrunner.DataTrees.AttributesTreeNode.AttributeTreeNode;
import net.schwarzbaer.java.games.snowrunner.DataTrees.GenericXmlNode_TreeNode;
import net.schwarzbaer.java.games.snowrunner.DataTrees.Item_TreeNode;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.system.ClipboardTools;

class RawDataPanel extends JSplitPane implements LanguageListener, DataReceiver {
	private static final long serialVersionUID = 10671596986103400L;
	
	private Data data;
	private Language language;

	private final JTabbedPane globalTemplatesPanel;
	private final JTabbedPane classesPanel;
	private final Window window;

	RawDataPanel(Window window, SnowRunner.Controllers controllers) {
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		this.window = window;
		data = null;
		language = null;
		
		globalTemplatesPanel = new JTabbedPane();
		globalTemplatesPanel.setBorder(BorderFactory.createTitledBorder("Global Templates"));
		
		classesPanel = new JTabbedPane();
		classesPanel.setBorder(BorderFactory.createTitledBorder("Classes"));
		
		setLeftComponent(globalTemplatesPanel);
		setRightComponent(classesPanel);
		
		updatePanels();
		controllers.languageListeners.add(this);
		controllers.dataReceivers.add(this);
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
			
			JMenuItem miCopyItemName = contextMenu.add(SnowRunner.createMenuItem("Copy Item Name", true, e->{
				if (selectedTreeNode instanceof Item_TreeNode) {
					Item_TreeNode treeNode = (Item_TreeNode) selectedTreeNode;
					ClipboardTools.copyStringSelectionToClipBoard(treeNode.item.name);
				}
			}));
			
			JMenuItem miCopyNodeName = contextMenu.add(SnowRunner.createMenuItem("Copy Node Name", true, e->{
				if (selectedTreeNode instanceof GenericXmlNode_TreeNode) {
					GenericXmlNode_TreeNode treeNode = (GenericXmlNode_TreeNode) selectedTreeNode;
					ClipboardTools.copyStringSelectionToClipBoard(treeNode.node.nodeName);
				}
			}));
			
			JMenuItem miCopyNodePath = contextMenu.add(SnowRunner.createMenuItem("Copy Node Path", true, e->{
				if (selectedTreeNode instanceof GenericXmlNode_TreeNode) {
					GenericXmlNode_TreeNode treeNode = (GenericXmlNode_TreeNode) selectedTreeNode;
					ClipboardTools.copyStringSelectionToClipBoard(toString(treeNode.node.getPath()));
				}
			}));
			
			JMenuItem miCopyAttributeName = contextMenu.add(SnowRunner.createMenuItem("Copy Attribute Name", true, e->{
				if (selectedTreeNode instanceof AttributeTreeNode) {
					AttributeTreeNode treeNode = (AttributeTreeNode) selectedTreeNode;
					ClipboardTools.copyStringSelectionToClipBoard(treeNode.key);
				}
			}));
			
			JMenuItem miCopyAttributeValue = contextMenu.add(SnowRunner.createMenuItem("Copy Attribute Value", true, e->{
				if (selectedTreeNode instanceof AttributeTreeNode) {
					AttributeTreeNode treeNode = (AttributeTreeNode) selectedTreeNode;
					ClipboardTools.copyStringSelectionToClipBoard(treeNode.value);
				}
			}));
			
			JMenuItem miAttributeValue2Text = contextMenu.add(SnowRunner.createMenuItem("Use Attribute Value as StringID -> Show Text", true, e->{
				if (selectedTreeNode instanceof AttributeTreeNode) {
					AttributeTreeNode treeNode = (AttributeTreeNode) selectedTreeNode;
					TextAreaDialog.showText(window, String.format("String ID \"%s\"", treeNode.value), 500, 300, true, SnowRunner.solveStringID(treeNode.value, getLanguage.get()));
				}
			}));
			
			contextMenu.addContextMenuInvokeListener((comp, x, y) -> {
				selectedPath = tree.getPathForLocation(x, y);
				selectedTreeNode = null;
				Object selectedNode = null;;
				if (selectedPath!=null)
					selectedNode = selectedPath.getLastPathComponent();
				if (selectedNode instanceof AbstractTreeNode)
					selectedTreeNode = (AbstractTreeNode) selectedNode;
				miCopyItemName       .setEnabled(selectedTreeNode instanceof Item_TreeNode);
				miCopyNodeName       .setEnabled(selectedTreeNode instanceof GenericXmlNode_TreeNode);
				miCopyNodePath       .setEnabled(selectedTreeNode instanceof GenericXmlNode_TreeNode);
				miCopyAttributeName  .setEnabled(selectedTreeNode instanceof AttributeTreeNode);
				miCopyAttributeValue .setEnabled(selectedTreeNode instanceof AttributeTreeNode);
				miAttributeValue2Text.setEnabled(selectedTreeNode instanceof AttributeTreeNode);
			});
		}

		private String toString(String[] path) {
			if (path==null) return "<null>";
			Iterable<String> it = ()->Arrays.stream(path).map(str->"\""+str+"\"").iterator();
			return String.join(", ", it);
		}
	}

}
