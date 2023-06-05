package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.w3c.dom.Node;

import net.schwarzbaer.java.games.snowrunner.DataTrees.GenericXmlNode_TreeNode;
import net.schwarzbaer.java.games.snowrunner.DataTrees.Templates_TreeNode;
import net.schwarzbaer.java.games.snowrunner.DataTrees.XmlNode_TreeNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode.Source;
import net.schwarzbaer.java.lib.gui.ContextMenu;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Templates;

class GenericXmlNodeParsingStateDialog extends JDialog {
	private static final long serialVersionUID = -1629073120482820570L;
	private boolean toBreakPoint;

	GenericXmlNodeParsingStateDialog(
			Window owner,
			GenericXmlNode parentTemplate,
			GenericXmlNode template,
			Node xmlNode,
			GenericXmlNode currentState,
			Templates templates, Source source, String reason, AbstractButton... extraButtons) {
		super(owner, source.getFilePath(), ModalityType.APPLICATION_MODAL);
		toBreakPoint = false;
		
		JPanel centerPanel = new JPanel(new GridLayout(1,0));
		if (parentTemplate!=null) {
			TreeNode root = new GenericXmlNode_TreeNode(null, parentTemplate);
			centerPanel.add(createTreePanel(root, "Parent"));
		}
		if (template!=null) {
			TreeNode root = new GenericXmlNode_TreeNode(null, template);
			centerPanel.add(createTreePanel(root, "Template"));
		}
		if (xmlNode!=null) {
			TreeNode root = new XmlNode_TreeNode(null, xmlNode);
			centerPanel.add(createTreePanel(root, "XML"));
		}
		if (currentState!=null) {
			TreeNode root = new GenericXmlNode_TreeNode(null, currentState);
			centerPanel.add(createTreePanel(root, "Current State"));
		}
		if (templates!=null) {
			TreeNode root = new Templates_TreeNode(templates);
			centerPanel.add(createTreePanel(root, "Templates"));
		}
		
		JScrollPane reasonPanel = null;
		if (reason!=null && !reason.isEmpty()) {
			JTextArea textArea = new JTextArea(reason);
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			reasonPanel = new JScrollPane(textArea);
			reasonPanel.setBorder(BorderFactory.createTitledBorder("Reason"));
			reasonPanel.setPreferredSize(new Dimension(100,100));
		}
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		buttonPanel.add(new JLabel(),c);
		c.weightx = 0;
		if (extraButtons!=null && extraButtons.length>0) {
			for (AbstractButton btn : extraButtons) buttonPanel.add(btn,c);
			buttonPanel.add(new JLabel("   "),c);
		}
		buttonPanel.add(SnowRunner.createButton("Exit Application", true, e->{ System.exit(0); }),c);
		buttonPanel.add(SnowRunner.createButton("To BreakPoint", true, e->{ toBreakPoint = true; setVisible(false); }),c);
		buttonPanel.add(SnowRunner.createButton("Close", true, e->{ setVisible(false); }),c);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		if (reasonPanel!=null) contentPane.add(reasonPanel,BorderLayout.NORTH);
		contentPane.add(centerPanel,BorderLayout.CENTER);
		contentPane.add(buttonPanel,BorderLayout.SOUTH);
		
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(owner);
	}

	private JScrollPane createTreePanel(TreeNode root, String title2) {
		JTree tree = new JTree(root);
		ContextMenu treeContextMenu = new ContextMenu();
		treeContextMenu.addTo(tree);
		treeContextMenu.add(SnowRunner.createMenuItem("Show Full Tree", true, e->{
			for (int i=0; i<tree.getRowCount(); i++)
				tree.expandRow(i);
		}));
		
		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setBorder(BorderFactory.createTitledBorder(title2));
		return scrollPane;
	}

	boolean showDialog() {
		setVisible(true);
		return toBreakPoint;
	}
	
}
