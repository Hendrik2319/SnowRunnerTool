package net.schwarzbaer.java.games.snowrunner;

import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;

import net.schwarzbaer.java.games.snowrunner.Data.Language;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.DataReceiver;
import net.schwarzbaer.java.games.snowrunner.SnowRunner.LanguageListener;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Templates;

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
			Vector<String> keys;
			
			keys = new Vector<>( data.rawdata.globalTemplates.keySet() );
			keys.sort(null);
			for (String key : keys) {
				Templates templates = data.rawdata.globalTemplates.get(key);
				JTree tree = new JTree(new DataTrees.Templates_TreeNode(templates));
				JScrollPane scrollPane = new JScrollPane(tree);
				scrollPane.setBorder(null);
				globalTemplatesPanel.addTab(key, scrollPane);
			}
			
			keys = new Vector<>( data.rawdata.classes.keySet() );
			keys.sort(null);
			for (String key : keys) {
				Class_ class_ = data.rawdata.classes.get(key);
				JTree tree = new JTree(new DataTrees.Class_TreeNode(class_));
				JScrollPane scrollPane = new JScrollPane(tree);
				scrollPane.setBorder(null);
				classesPanel.addTab(key, scrollPane);
			}
		}
	}

	@Override
	public void setLanguage(Language language) {
		this.language = language;
	}

}
