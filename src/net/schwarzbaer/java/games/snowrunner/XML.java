package net.schwarzbaer.java.games.snowrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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

	private static Reader fixDirtyXML(Function<String, String> fixDirtyXML, Reader reader) {
		StringWriter strOut = new StringWriter();
		try (
				BufferedReader in = new BufferedReader(reader);
				PrintWriter out = new PrintWriter(strOut);
		) {
			String line;
			while ( (line=in.readLine())!=null )
				out.println(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String fixedXML = fixDirtyXML.apply( strOut.toString() );
		
		return new StringReader(fixedXML);
	}

	static Document parseUTF8(InputStream input) {
		return parseUTF8(input, null);
	}
	static Document parseUTF8(InputStream input, Function<String,String> fixDirtyXML) {
		Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
		if (fixDirtyXML != null) {
			Reader fixedReader = fixDirtyXML(fixDirtyXML, reader);
			try { reader.close(); } catch (IOException e) {}
			reader = fixedReader;
		}
		return parseReader(reader);
	}
	
	static Document parseReader(Reader reader) {
		return parseInputSource(new InputSource(reader));
	}

	static Document parseInputSource(InputSource is) {
		if (is==null) return null;
		try {
			return DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.parse(is);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
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

}
