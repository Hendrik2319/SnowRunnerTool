package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SingleSelectionModel;

import net.schwarzbaer.java.games.snowrunner.tables.TableSimplifier.TextAreaOutputSource;

public class CombinedTableTabPaneTextAreaPanel extends JSplitPane {
	private static final long serialVersionUID = -2637203211606881920L;
	
	private final JTextArea textArea;
	private final JTabbedPane tabbedPane;
	private int selectedTab;
	private final Vector<Runnable> updateMethods;

	protected CombinedTableTabPaneTextAreaPanel() {
		super(JSplitPane.VERTICAL_SPLIT, true);
		selectedTab = 0;
		updateMethods = new Vector<Runnable>();
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		JScrollPane textAreaScrollPane = new JScrollPane(textArea);
		//textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Description"));
		textAreaScrollPane.setPreferredSize(new Dimension(400,50));
		textAreaScrollPane.setMinimumSize(new Dimension(5,5));
		
		tabbedPane = new JTabbedPane();
		tabbedPane.setPreferredSize(new Dimension(400,50));
		tabbedPane.setMinimumSize(new Dimension(5,40));
		SingleSelectionModel tabbedPaneSelectionModel = tabbedPane.getModel();
		tabbedPaneSelectionModel.addChangeListener(e->{
			int i = tabbedPaneSelectionModel.getSelectedIndex();
			selectedTab = i;
			if (i<0 || i>=updateMethods.size()) return;
			updateMethods.get(i).run();
		});
		
		setTopComponent(tabbedPane);
		setBottomComponent(textAreaScrollPane);
		setResizeWeight(1);
	}
	
	protected void removeAllTabs() {
		tabbedPane.removeAll();
		updateMethods.clear();
	}
	
	protected void setTabTitle(int index, String title) {
		tabbedPane.setTitleAt(index, title);
	}
	
	protected void setTabComponentAt(int index, Component component) {
		tabbedPane.setTabComponentAt(index, component);
	}
	
	
	protected <TableModel extends VerySimpleTableModel<?> & TextAreaOutputSource> void addTab(String title, TableModel tableModel) {
		int tabIndex = tabbedPane.getTabCount();
		JComponent panel = TableSimplifier.create(tableModel, textArea, updateMethod->{
			Runnable modifiedUpdateMethod = ()->{ if (selectedTab==tabIndex) updateMethod.run(); };
			if (tabbedPane.getTabCount()!=updateMethods.size())
				throw new IllegalStateException();
			updateMethods.add(modifiedUpdateMethod);
			return modifiedUpdateMethod;
		});
		tabbedPane.addTab(title, panel);
	}
}