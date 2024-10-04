package net.schwarzbaer.java.games.snowrunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class XML {
	
	interface FixXMLFunction {
		String fixXML(String rawXML) throws FixXMLException;
	}

	static class FixXMLException extends Exception {
		private static final long serialVersionUID = -8293945385664767233L;
		FixXMLException(String msg) { super(msg); }
		FixXMLException(String format, Object... objects) { super(String.format(format, objects)); }
		FixXMLException(Throwable cause, String format, Object... objects) { super(String.format(format, objects), cause); }
	}

	private static StringReader fixXML(FixXMLFunction fixXML, InputStream input) throws FixXMLException {
		
		byte[] bytes;
		try
		{
			bytes = input.readAllBytes();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		try { input.close(); }
		catch (IOException e) {}
		
		
		String string;
		if (bytes.length>=3 && (bytes[0]&0xFF)==0xEF && (bytes[1]&0xFF)==0xBB && (bytes[2]&0xFF)==0xBF) // UTF8 marker bytes
			string = new String(bytes, 3, bytes.length-3, StandardCharsets.UTF_8);
		else
			string = new String(bytes, StandardCharsets.UTF_8);
		
		String fixedXML = fixXML.fixXML( string );
		
		return new StringReader(fixedXML);
	}

	static Document parseUTF8(InputStream input) throws FixXMLException {
		return parseUTF8(input, null);
	}
	static Document parseUTF8(InputStream input, FixXMLFunction fixXML) throws FixXMLException {
		if (fixXML != null) {
			StringReader reader = fixXML(fixXML, input);
			if (reader==null) return null;
			return parseReader(reader);
		} else
			return parseReader(new InputStreamReader(input, StandardCharsets.UTF_8));
	}
	
	static Document parseReader(Reader reader) {
		return parseInputSource(new InputSource(reader));
	}

	static Document parseInputSource(InputSource is) {
		if (is==null) return null;
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			System.err.printf("Exception while parsing XML -> %s: %s%n", e.getClass().getName(), e.getMessage());
			//e.printStackTrace();
			return null;
		}
	}

	static Node[] getChildren(Node node, String nodeName) {
		Vector<Node> children = new Vector<>();
		NodeList childNodes = node.getChildNodes();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equals(nodeName))
				children.add(childNode);
		}
		return children.toArray(new Node[children.size()]);
	}

	static Node getChild(Node node, String nodeName) {
		NodeList childNodes = node.getChildNodes();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeName().equals(nodeName))
				return childNode;
		}
		return null;
	}

	static String getAttribute(Node node, String name) {
		NamedNodeMap attributes = node.getAttributes();
		if (attributes==null) return null;
		Node namedItem = attributes.getNamedItem(name);
		if (namedItem==null) return null;
		return namedItem.getNodeValue();
	}

	public static boolean hasAttribute(Node node, String name) {
		NamedNodeMap attributes = node.getAttributes();
		if (attributes==null) return false;
		Node namedItem = attributes.getNamedItem(name);
		return namedItem!=null;
	}

	static void forEachChild(Node node, Consumer<Node> action) {
		NodeList childNodes = node.getChildNodes();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType()==Node.ELEMENT_NODE)
			action.accept(childNode);
		}
	}
	
	static Iterable<Node> makeIterable(NamedNodeMap nodes) {
		return ()->new Iterator<>() {
			private int nextIndex = 0;
			
			@Override public boolean hasNext() {
				return nodes!=null && nextIndex<nodes.getLength();
			}
			@Override public Node next() {
				return nodes.item(nextIndex++);
			}
		};
	}

	static Iterable<Node> makeIterable(NodeList nodes) {
		return ()->new Iterator<>() {
			private int nextIndex = 0;
			
			@Override public boolean hasNext() {
				return nodes!=null && nextIndex<nodes.getLength();
			}
			@Override public Node next() {
				return nodes.item(nextIndex++);
			}
		};
	}
	
	enum NodeType {
		ATTRIBUTE_NODE              ( Node.ATTRIBUTE_NODE              ),
		CDATA_SECTION_NODE          ( Node.CDATA_SECTION_NODE          ),
		COMMENT_NODE                ( Node.COMMENT_NODE                ),
		DOCUMENT_FRAGMENT_NODE      ( Node.DOCUMENT_FRAGMENT_NODE      ),
		DOCUMENT_NODE               ( Node.DOCUMENT_NODE               ),
		DOCUMENT_TYPE_NODE          ( Node.DOCUMENT_TYPE_NODE          ),
		ELEMENT_NODE                ( Node.ELEMENT_NODE                ),
		ENTITY_NODE                 ( Node.ENTITY_NODE                 ),
		ENTITY_REFERENCE_NODE       ( Node.ENTITY_REFERENCE_NODE       ),
		NOTATION_NODE               ( Node.NOTATION_NODE               ),
		PROCESSING_INSTRUCTION_NODE ( Node.PROCESSING_INSTRUCTION_NODE ),
		TEXT_NODE                   ( Node.TEXT_NODE                   ),
		;
		final short value;
		NodeType(short value) {
			this.value = value;
		}
		static NodeType get(short nodeType) {
			for (NodeType nt : values())
				if (nt.value == nodeType)
					return nt;
			return null;
		}
	}

	static String getNodeTypeStr(short nodeType) {
		NodeType nt = NodeType.get(nodeType);
		if (nt!=null) return nt.toString();
		return String.format("Unkown Node Type (%d)", nodeType);
	}

	public static String toDebugString(Node node) {
		String nodeValue = node.getNodeValue();
		String nodeType = XML.getNodeTypeStr(node.getNodeType());
		if (nodeValue==null)
			return String.format("<%s> [%s]", node.getNodeName(), nodeType);
		return String.format("<%s> [%s]: \"%s\"", node.getNodeName(), nodeType, nodeValue);
	}

}
