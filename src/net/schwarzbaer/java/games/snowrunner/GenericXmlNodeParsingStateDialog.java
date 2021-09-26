package net.schwarzbaer.java.games.snowrunner;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.schwarzbaer.java.games.snowrunner.XML.NodeType;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode.Source;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Templates;

class GenericXmlNodeParsingStateDialog extends JDialog {
	private static final long serialVersionUID = -1629073120482820570L;

	GenericXmlNodeParsingStateDialog(
			Window owner,
			GenericXmlNode parentTemplate,
			GenericXmlNode template,
			Node xmlNode,
			GenericXmlNode currentState,
			Templates templates, Source source) {
		super(owner, source.getFilePath(), ModalityType.APPLICATION_MODAL);
		
		
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
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		buttonPanel.add(new JLabel(),c);
		c.weightx = 0;
		buttonPanel.add(SnowRunner.createButton("Exit Application", true, e->{ System.exit(0); }),c);
		buttonPanel.add(SnowRunner.createButton("Close", true, e->{ setVisible(false); }),c);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		contentPane.add(centerPanel,BorderLayout.CENTER);
		contentPane.add(buttonPanel,BorderLayout.SOUTH);
		
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(owner);
	}

	private JScrollPane createTreePanel(TreeNode root, String title2) {
		JTree tree = new JTree(root);
		JScrollPane scrollPane = new JScrollPane(tree);
		scrollPane.setBorder(BorderFactory.createTitledBorder(title2));
		return scrollPane;
	}

	void showDialog() {
		setVisible(true);
	}
	
	private static class Templates_TreeNode extends AbstractTreeNode {
		
		@SuppressWarnings("unused")
		final Templates templates;

		Templates_TreeNode(Templates templates) {
			super(null, true, false);
			this.templates = templates;
		}

		@Override
		protected Vector<TreeNode> createChildren() {
			// TODO Auto-generated method stub
			return new Vector<>();
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "Templates";
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
			if (attributes!=null)
				children.add(new AttributesTreeNode(this,attributes));
			
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

		GenericXmlNode_TreeNode(GenericXmlNode_TreeNode parent, GenericXmlNode node) {
			super(parent, true, false);
			this.node = node;
		}

		@Override protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			if (!node.attributes.isEmpty())
				children.add(new AttributesTreeNode(this,node.attributes));
			
			Vector<String> keys = new Vector<>( node.nodes.getKeys() );
			keys.sort(null);
			for (String key:keys)
				for (GenericXmlNode childNode : node.nodes.get(key))
					children.add(new GenericXmlNode_TreeNode(this, childNode));
			
			return children;
		}
		
		@Override public String toString() {
			return node.nodeName;
		}
	}
	
	
	
	private static class AttributesTreeNode extends AbstractTreeNode {
	
		final Vector<TreeNode> children;
	
		private AttributesTreeNode(TreeNode parent) {
			super(parent, true, false);
			children = new Vector<>();
		}
		
		AttributesTreeNode(TreeNode parent, HashMap<String, String> attributes) {
			this(parent);
			Vector<String> keys = new Vector<>( attributes.keySet() );
			keys.sort(null);
			for (String key:keys) {
				String value = attributes.get(key);
				children.add(new AttributeTreeNode(key,value));
			}
		}
	
		AttributesTreeNode(TreeNode parent, NamedNodeMap attributes) {
			this(parent);
			for (Node attrNode : XML.makeIterable(attributes))
				children.add(new AttributeTreeNode(attrNode.getNodeName(), attrNode.getNodeValue()));
		}
	
		@Override protected Vector<TreeNode> createChildren() {
			return children;
		}
		@Override public String toString() {
			return "[Attributes]";
		}
		
		private class AttributeTreeNode extends AbstractTreeNode {
			private final String key;
			private final String value;
			AttributeTreeNode(String key, String value) {
				super(AttributesTreeNode.this,false, true);
				this.key = key;
				this.value = value;
			}
			@Override protected Vector<TreeNode> createChildren() {
				return new Vector<>();
			}
			@Override public String toString() {
				return String.format("%s = %s", key, value);
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
