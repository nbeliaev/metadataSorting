package dev.fr13;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

public class Main {

    private static String sourceDirectory = "";

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        // add exit code
        // сделать длинную опцию path и --help
        parseArgs(args);
        if (sourceDirectory.isEmpty()) {
            printHelp();
            return;
        }

        // Firstly processing file with common objects
        Document mainConfig = domDocument();
        Node parentNode = mainConfig.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        String nodeName = "";
        //List<String> sortedObjectsNames = new ArrayList<String>();
        Map<String, Node> treeMap = new TreeMap<String, Node>();

        List<Node> childNodes = new ArrayList<Node>();
        //Node parentNodeEmpty = parentNode.cloneNode(false);
        Node parentNodeEmpty = mainConfig.importNode(parentNode, false);

        NodeList unsortedObjects = parentNode.getChildNodes();
        for (int i = 0; i < unsortedObjects.getLength(); i++) {

            Node currentUnsortedNode = unsortedObjects.item(i);
            if (currentUnsortedNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!nodeName.equals(currentUnsortedNode.getNodeName())) {

                for (Map.Entry e:treeMap.entrySet()) {
                    Node n = (Node) e.getValue();
                    Node node = n.cloneNode(true);
                    parentNodeEmpty.appendChild(node);
                }

               /* Collections.sort(sortedObjectsNames);

                for (int j = 0; j < sortedObjectsNames.size(); j++) {

                    XPath xPath = XPathFactory.newInstance().newXPath();
                    xPath.setNamespaceContext(new NamespaceResolver(mainConfig));
                    XPathExpression expr = xPath.compile("//ChildObjects/" + nodeName + "[contains(text(), '" + sortedObjectsNames.get(j) + "')]");
                    Object result = expr.evaluate(parentNode, XPathConstants.NODESET);
                    NodeList foundNodes = (NodeList) result;
                    if (foundNodes == null) {
                        // exeption?
                        System.out.println("Could not sort file: Configuration.xml");
                        return;
                    }
                    else if (foundNodes.getLength() != 1){
                        System.out.println("Duplicates found in metadata names: " + sortedObjectsNames.get(j));
                        return;
                    }

                    Node nodeForAdding = foundNodes.item(0).cloneNode(true);
                    parentNodeEmpty.appendChild(nodeForAdding);

                }*/

                //sortedObjectsNames.clear();
                treeMap.clear();

            }

            String currentNodeText = currentUnsortedNode.getTextContent();
            treeMap.put(currentNodeText, currentUnsortedNode);
            //sortedObjectsNames.add(currentNodeText);
            nodeName = currentUnsortedNode.getNodeName();
        }

        if (treeMap.size() != 0) {

            for (Map.Entry e:treeMap.entrySet()) {
                Node n = (Node) e.getValue();
                Node node = n.cloneNode(true);
                parentNodeEmpty.appendChild(node);
            }
        }

        parentNode.getParentNode().replaceChild(parentNodeEmpty, parentNode);

        // del
        NodeList childTemp = mainConfig.getElementsByTagName("ChildObjects");

        if (!childTemp.item(0).hasChildNodes())
            return;

        NodeList listTemp = childTemp.item(0).getChildNodes();
        for (int i = 0; i < listTemp.getLength(); i++) {

            Node node = listTemp.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            System.out.println(listTemp.item(i).getTextContent());

        }
    }

    private static void printHelp() {
        System.out.println("MetadataSorting sorts metadata for 1C:Enterprise solutions");
        System.out.println();
        System.out.println("Use the -p to set directory path with metadata");
    }

    private static String pathToMainConfig() {
        return sourceDirectory + File.separatorChar + "Configuration.xml";
    }

    private static Document domDocument() throws ParserConfigurationException, IOException, SAXException {

        Document document = null;
        try {
            File file = new File(sourceDirectory);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(pathToMainConfig());
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (SAXException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return document;

    }

    private static void parseArgs(String[] args) {

        int argsCount = args.length;

        if (argsCount == 0) {
            printHelp();
            return;
        }

        String pathAsArg = "-p";
        String firstArg = args[0];
        if (firstArg.contains(pathAsArg)) {
            if (firstArg.length() > 2)
                sourceDirectory = firstArg.substring(2, firstArg.length());
            else
                sourceDirectory = args[1];
        }
    }

}
