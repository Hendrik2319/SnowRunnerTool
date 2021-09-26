package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.java.games.snowrunner.XML.NodeType;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode.Source;
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
	
	private static class Templates_TreeNode extends AbstractTreeNode {
		
		final Templates templates;
		final String label;

		private Templates_TreeNode(Templates_TreeNode parent, String label, Templates templates) {
			super(parent, true, false);
			this.label = label;
			this.templates = templates;
		}
		Templates_TreeNode(Templates templates) {
			this(null,"Templates",templates);
		}

		@Override
		protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			if (templates.includedTemplates!=null)
				children.add(new Templates_TreeNode(this, "included:", templates.includedTemplates));
			
			Vector<String> nodeNames = new Vector<>(templates.templates.keySet());
			nodeNames.sort(null);
			for (String nodeName:nodeNames)
				children.add(new TemplatesForNode_TreeNode(this, nodeName, templates.templates.get(nodeName)));
			
			return children;
		}

		@Override public String toString() {
			return label;
		}
		
		private static class TemplatesForNode_TreeNode extends AbstractTreeNode {

			final String nodeName;
			final HashMap<String, GenericXmlNode> templates;

			TemplatesForNode_TreeNode(Templates_TreeNode parent, String nodeName, HashMap<String, GenericXmlNode> templates) {
				super(parent, true, templates.isEmpty());
				this.nodeName = nodeName;
				this.templates = templates;
			}

			@Override
			protected Vector<TreeNode> createChildren() {
				Vector<TreeNode> children = new Vector<>();
				
				Vector<String> templateNames = new Vector<>(templates.keySet());
				templateNames.sort(null);
				for (String templateName : templateNames)
					children.add(new GenericXmlNode_TreeNode(this, "\""+templateName+"\"", templates.get(templateName)));
				
				return children;
			}

			@Override public String toString() {
				return "<"+nodeName+">";
			}
		}
	}
	
	private static class XmlNode_TreeNode extends AbstractTreeNode {
		
		final Node node;

		XmlNode_TreeNode(XmlNode_TreeNode parent, Node node) {
			super(parent, true, false);
			this.node = node;
		}

		@Override
		protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			NamedNodeMap attributes = node.getAttributes();
			if (attributes!=null && attributes.getLength()>0)
				//children.add(new AttributesTreeNode(this,attributes));
				AttributesTreeNode.addAttributesTo(this, attributes, children);
			
			NodeList childNodes = node.getChildNodes();
			for (Node childNode : XML.makeIterable(childNodes)) {
				if (childNode.getNodeType()==Node.TEXT_NODE) {
					String text = childNode.getNodeValue();
					if (!text.trim().isEmpty())
						children.add(new XmlNode_TreeNode(this, childNode));
				} else
					children.add(new XmlNode_TreeNode(this, childNode));
			}
			
			return children;
		}

		@Override
		public String toString() {
			short nodeType = node.getNodeType();
			NodeType nt = XML.NodeType.get(nodeType);
			switch (nt) {
			
			case TEXT_NODE:
				return toReducedString(node.getNodeValue().trim(),30);
				
			case CDATA_SECTION_NODE:
				return "CDATA: "+toReducedString(node.getNodeValue().trim(),30);
				
			case COMMENT_NODE:
				return "<!-- "+node.getNodeValue()+"-->";
				
			case ELEMENT_NODE:
				return "<"+node.getNodeName()+">";
				
			default:
				break;
			}
			String label = nt==null ? String.format("UnknownNodeType[%d]", nodeType) : nt.toString();
			return String.format("{%s} name:\"%s\" value:\"%s\"", label, node.getNodeName(), node.getNodeValue());
		}

		private String toReducedString(String text, int maxLength) {
			if (text==null) return "<null>";
			if (text.length()>maxLength)
				return String.format("\"%s...\" (%d chars total)", text.substring(0, maxLength), text.length());
			return String.format("\"%s\"", text);
		}
	}
	
	private static class GenericXmlNode_TreeNode extends AbstractTreeNode {
		
		final GenericXmlNode node;
		final String label;

		GenericXmlNode_TreeNode(TreeNode parent, GenericXmlNode node) {
			this(parent, null, node);
		}
		GenericXmlNode_TreeNode(TreeNode parent, String label, GenericXmlNode node) {
			super(parent, true, false);
			this.label = label;
			this.node = node;
		}

		@Override protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			if (!node.attributes.isEmpty())
				//children.add(new AttributesTreeNode(this,node.attributes));
				AttributesTreeNode.addAttributesTo(this, node.attributes, children);
			
			Vector<String> keys = new Vector<>( node.nodes.getKeys() );
			keys.sort(null);
			for (String key:keys)
				for (GenericXmlNode childNode : node.nodes.get(key))
					children.add(new GenericXmlNode_TreeNode(this, childNode));
			
			return children;
		}
		
		@Override public String toString() {
			return label!=null ? label : "<"+node.nodeName+">";
		}
	}
	
	
	
	@SuppressWarnings("unused")
	private static class AttributesTreeNode extends AbstractTreeNode {
	
		final Vector<TreeNode> children;
	
		private AttributesTreeNode(TreeNode parent) {
			super(parent, true, false);
			children = new Vector<>();
		}
		
		AttributesTreeNode(TreeNode parent, HashMap<String, String> attributes) {
			this(parent);
			addAttributesTo(this, attributes, children);
		}

		AttributesTreeNode(TreeNode parent, NamedNodeMap attributes) {
			this(parent);
			Vector<TreeNode> children2 = children;
			TreeNode parent2 = this;
			addAttributesTo(parent2, attributes, children2);
		}

		static void addAttributesTo(TreeNode parent, HashMap<String, String> attributes, Vector<TreeNode> children) {
			Vector<String> keys = new Vector<>( attributes.keySet() );
			keys.sort(null);
			for (String key:keys) {
				String value = attributes.get(key);
				children.add(new AttributeTreeNode(parent, key, value));
			}
		}

		static void addAttributesTo(TreeNode parent, NamedNodeMap attributes, Vector<TreeNode> children) {
			for (Node attrNode : XML.makeIterable(attributes))
				children.add(new AttributeTreeNode(parent, attrNode.getNodeName(), attrNode.getNodeValue()));
		}
	
		@Override protected Vector<TreeNode> createChildren() {
			return children;
		}
		@Override public String toString() {
			return "[Attributes]";
		}
		
		static class AttributeTreeNode extends AbstractTreeNode {
			private final String key;
			private final String value;
			AttributeTreeNode(TreeNode parent, String key, String value) {
				super(parent,false, true);
				this.key = key;
				this.value = value;
			}
			@Override protected Vector<TreeNode> createChildren() {
				return new Vector<>();
			}
			@Override public String toString() {
				return String.format("%s = \"%s\"", key, value);
			}
		}
	}

	private static abstract class AbstractTreeNode implements TreeNode {
		
		final TreeNode parent;
		Vector<TreeNode> children;
		final boolean allowsChildren;
		final boolean isLeaf;

		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf) {
			this.parent = parent;
			this.allowsChildren = allowsChildren;
			this.isLeaf = isLeaf;
			children = null;
		}

		protected abstract Vector<TreeNode> createChildren();

		@Override public TreeNode getParent() { return parent; }
		@Override public boolean getAllowsChildren() { return allowsChildren; }
		@Override public boolean isLeaf() { return isLeaf; }

		@Override public int getChildCount() {
			if (children==null) children = createChildren();
			return children.size();
		}

		@Override public TreeNode getChildAt(int childIndex) {
			if (children==null) children = createChildren();
			return childIndex<0 || childIndex>=children.size() ? null : children.get(childIndex);
		}

		@Override public int getIndex(TreeNode node) {
			if (children==null) children = createChildren();
			return children.indexOf(node);
		}

		@SuppressWarnings("rawtypes")
		@Override public Enumeration children() {
			if (children==null) children = createChildren();
			return children.elements();
		}
		
	}
	
}
