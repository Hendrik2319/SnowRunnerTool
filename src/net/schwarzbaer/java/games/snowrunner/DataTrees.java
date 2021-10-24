package net.schwarzbaer.java.games.snowrunner;

import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
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
import net.schwarzbaer.java.games.snowrunner.SaveGameData.NV;
import net.schwarzbaer.java.games.snowrunner.SaveGameData.V;
import net.schwarzbaer.java.games.snowrunner.XML.NodeType;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Class_.Item;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.GenericXmlNode;
import net.schwarzbaer.java.games.snowrunner.XMLTemplateStructure.Templates;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;

class DataTrees {
	
	interface CachedIcon { Icon getIcon(); }
	
	private static IconSource.CachedIcons<TreeIcons> TreeIconsIS = IconSource.createCachedIcons(16, 16, "/images/TreeIcons.png", TreeIcons.values());
	enum TreeIcons implements CachedIcon {
		Folder, Node, Attribute;
		@Override public Icon getIcon() { return TreeIconsIS.getCachedIcon(this); }
	}
	
	private static IconSource.CachedIcons<JsonTreeIcons> JsonTreeIconsIS = IconSource.createCachedIcons(16, 16, "/images/JsonTreeIcons.png", JsonTreeIcons.values());
	enum JsonTreeIcons implements CachedIcon {
		Object, Array, String, Number, Bool, Null;
		@Override public Icon getIcon() { return JsonTreeIconsIS.getCachedIcon(this); }
	}

	static class GlobalColorizer {
		private static final Color COLOR_ITEM_TRAILER = new Color(0x007FFF);

		static Color getColor(Item item) {
			if (!item.content.nodeName.equals("Truck")) return null;
			String type = item.content.attributes.get("Type");
			if (type==null) return null;
			if (type.equals("Trailer")) return COLOR_ITEM_TRAILER;
			return Color.RED;
		}
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

		@Override boolean hasName() { return class_!=null; }
		@Override String getName() { return class_.name; }

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

			@Override boolean hasName() { return true; }
			@Override String getName() { return name; }

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
			super(parent, true, false, TreeIcons.Folder, GlobalColorizer.getColor(item));
			this.item = item;
		}

		@Override boolean hasName() { return true; }
		@Override String getName() { return item.name; }

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
	
		@Override boolean hasName() { return true; }
		@Override boolean hasValue() { return true; }
		@Override String getName() { return node.getNodeName(); }
		@Override String getValue() { return node.getNodeValue(); }

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
	
		@Override boolean hasPath() { return true; }
		@Override boolean hasName() { return true; }
		@Override String[] getPath() { return node.getPath(); }
		@Override String getName() { return node.nodeName; }
		
		@Override protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			if (!node.attributes.isEmpty())
				//children.add(new AttributesTreeNode(this,node.attributes));
				AttributesTreeNode.addAttributesTo(this, node.attributes, children);
			
			Vector<String> keys = new Vector<>( node.nodes.keySet() );
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
			@Override boolean hasName () { return key!=null; }
			@Override boolean hasValue() { return value!=null; }
			@Override String getName () { return key; }
			@Override String getValue() { return value; }
		}
	}

	static class JsonTreeNode extends AbstractTreeNode {
		
		private final String name;
		private final JSON_Data.Value<NV, V> value;
		private final boolean showNamedValuesSorted;

		JsonTreeNode(JSON_Data.Value<NV,V> value, boolean showNamedValuesSorted) {
			this(null, null, value, showNamedValuesSorted);
		}
		JsonTreeNode(JsonTreeNode parent, String name, JSON_Data.Value<NV,V> value, boolean showNamedValuesSorted) {
			super(parent, allowsChildren(value.type), isLeaf(value), getIcon(value.type));
			this.name = name;
			this.value = value;
			this.showNamedValuesSorted = showNamedValuesSorted;
		}
		
		@Override boolean hasPath() { return true; }
		@Override boolean hasName() { return name!=null; }
		@Override boolean hasValue() { return value!=null; }
		@Override String[] getPath() { return PathElement.toStringArr(getPath(0)); }
		@Override String   getName() { return name; }
		@Override String   getValue() { return getValueString(); }
		
		private PathElement[] getPath(int length) {
			PathElement[] path;
			
			if (parent instanceof JsonTreeNode) {
				JsonTreeNode jsonParent = (JsonTreeNode) parent;
				
				PathElement pe =
						name!=null 
						? new PathElement(name)
						: new PathElement(jsonParent.getIndexOf(this));
				
				path = jsonParent.getPath(length+1);
				path[path.length-length-1] = pe;
				
 			} else {
				PathElement pe =
						name!=null 
						? new PathElement(name)
						: new PathElement("<root>");
				
 				path = new PathElement[length+1];
				path[0] = pe;
 			}
			
			return path;
		}

		private int getIndexOf(JsonTreeNode jsonTreeNode) {
			return children.indexOf(jsonTreeNode);
		}

		static class PathElement {
			final Integer index;
			final String name;
			PathElement(int index) {
				this.name = null;
				this.index = index;
			}
			PathElement(String name) {
				this.name = name;
				this.index = null;
			}
			static String[] toStringArr(PathElement[] path) {
				return Arrays
						.stream(path)
						.<String>map(pe->pe.name!=null ? pe.name : String.format("[%d]", pe.index))
						.toArray(String[]::new);
			}
		}
		
		@Override
		protected Vector<TreeNode> createChildren() {
			Vector<TreeNode> children = new Vector<>();
			
			switch (value.type) {
			case Object:
				Vector<JSON_Data.NamedValue<NV,V>> values;
				if (showNamedValuesSorted) {
					values = new Vector<>(value.castToObjectValue().value);
					values.sort(Comparator.<JSON_Data.NamedValue<NV,V>,String>comparing(nv->nv.name));
				} else
					values = value.castToObjectValue().value;
				
				for (JSON_Data.NamedValue<NV,V> nv : values)
					children.add(new JsonTreeNode(this, nv.name, nv.value, showNamedValuesSorted));
				break;
				
			case Array:
				for (JSON_Data.Value<NV, V> v : value.castToArrayValue().value)
					children.add(new JsonTreeNode(this, null, v, showNamedValuesSorted));
				break;
				
			default:
				break;
			}
			
			return children;
		}
		
		@Override
		public String toString() {
			if (name==null) return getValueString();
			return String.format("%s: %s", name, getValueString());
		}
		
		private String getValueString() {
			switch(value.type) {
			case String : return String.format("\"%s\"", value.castToStringValue ().value);
			case Bool   : return String.format("%s"    , value.castToBoolValue   ().value);
			case Float  : return                      ""+value.castToFloatValue  ().value ;
			case Integer: return String.format("%d"    , value.castToIntegerValue().value);
			case Array  : return String.format("[%d]"  , value.castToArrayValue  ().value.size());
			case Object : return String.format("{%d}"  , value.castToObjectValue ().value.size());
			case Null   : return "<null>";
			}
			return value.toString();
		}
		
		private static boolean allowsChildren(JSON_Data.Value.Type type) {
			switch (type) {
			case Object :
			case Array  :
				return true;
			case String :
			case Float  :
			case Integer:
			case Bool   :
			case Null   :
				break;
			}
			return false;
		}
		
		private static boolean isLeaf(JSON_Data.Value<NV, V> value) {
			switch (value.type) {
			case Object : return value.castToObjectValue().value.isEmpty();
			case Array  : return value.castToArrayValue ().value.isEmpty();
			case String :
			case Float  :
			case Integer:
			case Bool   :
			case Null   :
				return true;
			}
			return true;
		}
		
		private static JsonTreeIcons getIcon(JSON_Data.Value.Type type) {
			switch (type) {
			case Object : return JsonTreeIcons.Object;
			case Array  : return JsonTreeIcons.Array ;
			case String : return JsonTreeIcons.String;
			case Float  : return JsonTreeIcons.Number;
			case Integer: return JsonTreeIcons.Number;
			case Bool   : return JsonTreeIcons.Bool  ;
			case Null   : return JsonTreeIcons.Null  ;
			}
			return null;
		}
	}

	static abstract class AbstractTreeNode implements TreeNode {
		
		final TreeNode parent;
		Vector<TreeNode> children;
		final boolean allowsChildren;
		final boolean isLeaf;
		final CachedIcon icon;
		final Color color;
	
		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf) {
			this(parent, allowsChildren, isLeaf, null, null);
		}
		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf, CachedIcon icon) {
			this(parent, allowsChildren, isLeaf, icon, null);
		}
		AbstractTreeNode(TreeNode parent, boolean allowsChildren, boolean isLeaf, CachedIcon icon, Color color) {
			this.parent = parent;
			this.allowsChildren = allowsChildren;
			this.isLeaf = isLeaf;
			this.icon = icon;
			this.color = color;
			children = null;
		}
		
		boolean hasPath () { return false; }
		boolean hasName () { return false; }
		boolean hasValue() { return false; }
		
		String[] getPath () { return null; }
		String   getName () { return null; }
		String   getValue() { return null; }
		
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
	
		@Override public Enumeration<TreeNode> children() {
			if (children==null) children = createChildren();
			return children.elements();
		}
		
	}

}
