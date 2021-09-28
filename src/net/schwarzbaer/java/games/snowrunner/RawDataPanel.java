package net.schwarzbaer.java.games.snowrunner;

import java.util.HashMap;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;

class RawDataPanel extends JSplitPane implements LanguageListener, DataReceiver {
	private static final long serialVersionUID = 10671596986103400L;
	
	private Data data;
	@SuppressWarnings("unused")
	private Language language;

	private JTabbedPane globalTemplatesPanel;

	private JTabbedPane classesPanel;

	RawDataPanel() {
		super(JSplitPane.HORIZONTAL_SPLIT, true);
		data = null;
		language = null;
		
		globalTemplatesPanel = new JTabbedPane();
		globalTemplatesPanel.setBorder(BorderFactory.createTitledBorder("Global Templates"));
		
		classesPanel = new JTabbedPane();
		classesPanel.setBorder(BorderFactory.createTitledBorder("Classes"));
		
		setLeftComponent(globalTemplatesPanel);
		setRightComponent(classesPanel);
		
		updatePanels();
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
		for (String key : keys) {
			ValueType templates = map.get(key);
			JTree tree = new JTree(treeNodeContructor.apply(templates));
			tree.setCellRenderer(new DataTrees.TreeNodeRenderer());
			JScrollPane scrollPane = new JScrollPane(tree);
			scrollPane.setBorder(null);
			panel.addTab(key, scrollPane);
		}
	}

	@Override
	public void setLanguage(Language language) {
		this.language = language;
	}

}
