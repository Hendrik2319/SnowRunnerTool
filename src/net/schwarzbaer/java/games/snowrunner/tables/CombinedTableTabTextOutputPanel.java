package net.schwarzbaer.java.games.snowrunner.tables;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SingleSelectionModel;

import net.schwarzbaer.gui.Tables;

public abstract class CombinedTableTabTextOutputPanel extends JSplitPane {
	private static final long serialVersionUID = -2637203211606881920L;
	
	private final JTabbedPane tabbedPane;
	private int selectedTab;
	private final Vector<Runnable> updateMethods;
	protected final JScrollPane textAreaScrollPane;

	// CombinedTableTabPaneTextAreaPanel
	protected CombinedTableTabTextOutputPanel() {
		super(JSplitPane.VERTICAL_SPLIT, true);
		selectedTab = 0;
		updateMethods = new Vector<Runnable>();
		
		textAreaScrollPane = new JScrollPane();
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
	
	public static class CombinedTableTabPaneTextAreaPanel extends CombinedTableTabTextOutputPanel {
		private static final long serialVersionUID = 4511036794520233512L;
		
		private final JTextArea textArea;
		
		protected CombinedTableTabPaneTextAreaPanel() {
			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			textAreaScrollPane.setViewportView(textArea);
		}
		
		protected <TableModel extends Tables.SimplifiedTableModel<?> & TableSimplifier.TextAreaOutputSource> void addTab(String title, TableModel tableModel) {
			addTab(title, tableModel, textArea);
		}
	}
	
	public static class CombinedTableTabPaneTextPanePanel extends CombinedTableTabTextOutputPanel {
		private static final long serialVersionUID = 4511036794520233512L;
		
		private final JTextPane textPane;
		
		protected CombinedTableTabPaneTextPanePanel() {
			textPane = new JTextPane();
			textPane.setEditable(false);
			textAreaScrollPane.setViewportView(textPane);
		}
		
		protected <TableModel extends Tables.SimplifiedTableModel<?> & TableSimplifier.TextPaneOutputSource> void addTab(String title, TableModel tableModel) {
			addTab(title, tableModel, textPane);
		}
	}
	
	protected <TableModel extends Tables.SimplifiedTableModel<?> & TableSimplifier.TextPaneOutputSource> void addTab(String title, TableModel tableModel, JTextPane textPane) {
		int tabIndex = tabbedPane.getTabCount();
		JComponent panel = TableSimplifier.create(tableModel, textPane, updateMethod->{
			Runnable modifiedUpdateMethod = ()->{ if (selectedTab==tabIndex) updateMethod.run(); };
			if (tabbedPane.getTabCount()!=updateMethods.size())
				throw new IllegalStateException();
			updateMethods.add(modifiedUpdateMethod);
			return modifiedUpdateMethod;
		});
		tabbedPane.addTab(title, panel);
	}
	
	protected <TableModel extends Tables.SimplifiedTableModel<?> & TableSimplifier.TextAreaOutputSource> void addTab(String title, TableModel tableModel, JTextArea textArea) {
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
	
	
}