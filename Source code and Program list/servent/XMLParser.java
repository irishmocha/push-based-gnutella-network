package servent;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class XMLParser {

	private File configFile;
	
	private DocumentBuilderFactory documentBuilderFactory;
	private DocumentBuilder documentBuilder;
	private Document document;
	private NodeList nodeList;
	
	public XMLParser(String configFilePath) {
		
		configFile = new File(configFilePath);
		documentBuilderFactory = DocumentBuilderFactory.newInstance();
		
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			document = documentBuilder.parse(configFile);
			document.getDocumentElement().normalize();
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Parse();
	}
	
	private void Parse() {
		nodeList = document.getElementsByTagName("servent");
		
		for (int i=0; i<nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				
				ServerInfo serverInfo = new ServerInfo();
				serverInfo.setServerID(element.getAttribute("id"));
				serverInfo.setServerIP(element.getElementsByTagName("ip").item(0).getTextContent());
				serverInfo.setServerPort(element.getElementsByTagName("port").item(0).getTextContent());
				serverInfo.setServerTTR(element.getElementsByTagName("TTR").item(0).getTextContent());
				
				Gnutella.STATIC_NETWORK.add(serverInfo);
			}
		}
		System.out.println("Server topology is Ready");
	}
}