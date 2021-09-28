package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.schwarzbaer.gui.IconSource;
import net.schwarzbaer.java.games.snowrunner.XML.NodeType;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Templates;

class DataTrees {
	
	private static IconSource.CachedIcons<TreeIcons> TreeIconsIS = IconSource.createCachedIcons(16, 16, "/images/TreeIcons.png", TreeIcons.values());
	enum TreeIcons {
		Folder, Node, Attribute;
		Icon getIcon() { return TreeIconsIS.getCachedIcon(this); }
	}

	static class TreeNodeRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 4669699537680450275L;
	
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean isExpanded, boolean isLeaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, isSelected, isExpanded, isLeaf, row, hasFocus);
			
			if (value instanceof AbstractTreeNode) {
				AbstractTreeNode treeNode = (AbstractTreeNode) value;
				if (!isSelected) {
					Color color = treeNode.getColor();
					setForeground(color!=null ? color : tree.getForeground());
				}
				Icon icon = treeNode.getIcon();
				if (icon!=null) setIcon(icon);
				
			} else {
				if (!isSelected) setForeground(tree.getForeground());
				//setIcon(null);
			}
			
			return this;
		}
	}

	static class Class_TreeNode extends AbstractTreeNode {

		final Class_ class_;

		Class_TreeNode(Class_ class_) {
			super(null, true, class_.items.isEmpty(), TreeIcons.Folder);
			this.class_ = class_;
		}

		@Override
		protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			HashMap<String,Vector<Item>> subClasses = new HashMap<>();
			for (Item item : class_.items.values()) {
				
				String subClassName = "???";
				if (item.content != null) subClassName = item.content.nodeName;
				
				Vector<Item> list = subClasses.get(subClassName);
				if (list==null) subClasses.put(subClassName, list = new Vector<>());
				
				list.add(item);
			}
			
			Vector<String> keys = new Vector<>( subClasses.keySet() );
			keys.sort(null);
			for (String key : keys) {
				Vector<Item> list = subClasses.get(key);
				children.add(new SubClass_TreeNode(this, key, list));
			}
			
//			Vector<String> keys = new Vector<>( class_.items.keySet() );
//			for (String key : keys)
//				children.add(new Item_TreeNode(this,class_.items.get(key)));
			return children;
		}

		@Override
		public String toString() {
			return class_==null ? "Class <null>" : class_.name;
		}
		
		static class SubClass_TreeNode extends AbstractTreeNode {

			final String name;
			final Vector<Item> list;

			public SubClass_TreeNode(Class_TreeNode class_TreeNode, String name, Vector<Item> list) {
				super(class_TreeNode, true, list.isEmpty(), TreeIcons.Folder);
				this.name = name;
				this.list = list;
			}

			@Override public String toString() { return name; }

			@Override
			protected Vector<TreeNode> createChildren() {
				list.sort(Comparator.<Item,String>comparing(item->item.name));
				Vector<TreeNode> children = new Vector<>();
				for (Item item : list)
					children.add(new Item_TreeNode(this, item));
				return children;
			}
			
		}
	}
	
	static class Item_TreeNode extends AbstractTreeNode {

		final Item item;

		public Item_TreeNode(TreeNode parent, Item item) {
			super(parent, true, false, TreeIcons.Folder);
			this.item = item;
		}

		@Override
		public String toString() {
			if (item.dlcName==null) return item.name;
			return String.format("%s [%s]", item.name, item.dlcName);
		}

		@Override
		protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			if (item!=null) {
				if (item.dlcName     !=null) children.add(new AttributesTreeNode.AttributeTreeNode(this, "DLC"     , item.dlcName     ));
				if (item.className   !=null) children.add(new AttributesTreeNode.AttributeTreeNode(this, "Class"   , item.className   ));
				if (item.subClassName!=null) children.add(new AttributesTreeNode.AttributeTreeNode(this, "SubClass", item.subClassName));
				if (item.name        !=null) children.add(new AttributesTreeNode.AttributeTreeNode(this, "Name"    , item.name        ));
				if (item.filePath    !=null) children.add(new AttributesTreeNode.AttributeTreeNode(this, "File"    , item.filePath    ));
				if (item.content     !=null) children.add(new GenericXmlNode_TreeNode(this, item.content));
			}
			
			return children;
		}
	}
	
	static class Templates_TreeNode extends AbstractTreeNode {
		
		final Templates templates;
		final String label;
	
		private Templates_TreeNode(Templates_TreeNode parent, String label, Templates templates) {
			super(parent, true, false, TreeIcons.Folder);
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
				super(parent, true, templates.isEmpty(), TreeIcons.Folder);
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

	static class XmlNode_TreeNode extends AbstractTreeNode {
		
		final Node node;
	
		XmlNode_TreeNode(XmlNode_TreeNode parent, Node node) {
			super(parent, true, false, TreeIcons.Node);
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

	static class GenericXmlNode_TreeNode extends AbstractTreeNode {
		
		final GenericXmlNode node;
		final String label;
	
		GenericXmlNode_TreeNode(TreeNode parent, GenericXmlNode node) {
			this(parent, null, node);
		}
		GenericXmlNode_TreeNode(TreeNode parent, String label, GenericXmlNode node) {
			super(parent, true, false, TreeIcons.Node);
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

	static class AttributesTreeNode extends AbstractTreeNode {
	
		final Vector<TreeNode> children;
	
		private AttributesTreeNode(TreeNode parent) {
			super(parent, true, false, TreeIcons.Folder);
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
			final String key;
			final String value;
			AttributeTreeNode(TreeNode parent, String key, String value) {
				super(parent,false, true, TreeIcons.Attribute);
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

	static abstract class AbstractTreeNode implements TreeNode {
		
		final TreeNode parent;
		Vector<TreeNode> children;
		final boolean allowsChildren;
		final boolean isLeaf;
		final TreeIcons icon;
		final Color color;
	
		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf) {
			this(parent, allowsChildren, isLeaf, null, null);
		}
		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf, TreeIcons icon) {
			this(parent, allowsChildren, isLeaf, icon, null);
		}
		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf, TreeIcons icon, Color color) {
			this.parent = parent;
			this.allowsChildren = allowsChildren;
			this.isLeaf = isLeaf;
			this.icon = icon;
			this.color = color;
			children = null;
		}
		
		Icon getIcon() { return icon==null ? null : icon.getIcon(); }
		Color getColor() { return color; }

		@Override public abstract String toString();
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
