package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class XML {

	private static StringReader fixDirtyXML(Function<String, String> fixDirtyXML, InputStream input) {
		
		byte[] bytes;
		try (
				BufferedInputStream in = new BufferedInputStream(input);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
		) {
			byte[] buffer = new byte[100000];
			int n;
			while ( (n=in.read(buffer))>=0 )
				if (n>0) out.write(buffer, 0, n);
			
			bytes = out.toByteArray();
			
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		
		String string;
		if (bytes.length>=3 && (bytes[0]&0xFF)==0xEF && (bytes[1]&0xFF)==0xBB && (bytes[2]&0xFF)==0xBF) // UTF8 marker bytes
			string = new String(bytes, 3, bytes.length-3, StandardCharsets.UTF_8);
		else
			string = new String(bytes, StandardCharsets.UTF_8);
		
		String fixedXML = fixDirtyXML.apply( string );
		
		return new StringReader(fixedXML);
	}

	static Document parseUTF8(InputStream input) {
		return parseUTF8(input, null);
	}
	static Document parseUTF8(InputStream input, Function<String,String> fixDirtyXML) {
		if (fixDirtyXML != null) {
			StringReader reader = fixDirtyXML(fixDirtyXML, input);
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
	
	static Iterable<Node> makeIterable(NodeList nodes) {
		return ()->new Iterator<Node>() {
			private int nextIndex = 0;
			
			@Override public boolean hasNext() {
				return nodes!=null && nextIndex<nodes.getLength();
			}
			@Override public Node next() {
				return nodes.item(nextIndex++);
			}
		};
	}

	static String getNodeTypeStr(short nodeType) {
		switch (nodeType) {
		case Node.ATTRIBUTE_NODE             : return "ATTRIBUTE_NODE";
		case Node.CDATA_SECTION_NODE         : return "CDATA_SECTION_NODE";
		case Node.COMMENT_NODE               : return "COMMENT_NODE";
		case Node.DOCUMENT_FRAGMENT_NODE     : return "DOCUMENT_FRAGMENT_NODE";
		case Node.DOCUMENT_NODE              : return "DOCUMENT_NODE";
		case Node.DOCUMENT_TYPE_NODE         : return "DOCUMENT_TYPE_NODE";
		case Node.ELEMENT_NODE               : return "ELEMENT_NODE";
		case Node.ENTITY_NODE                : return "ENTITY_NODE";
		case Node.ENTITY_REFERENCE_NODE      : return "ENTITY_REFERENCE_NODE";
		case Node.NOTATION_NODE              : return "NOTATION_NODE";
		case Node.PROCESSING_INSTRUCTION_NODE: return "PROCESSING_INSTRUCTION_NODE";
		case Node.TEXT_NODE                  : return "TEXT_NODE";
		}
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
