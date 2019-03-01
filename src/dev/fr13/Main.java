package dev.fr13;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

    private static String sourceDirectory = "";

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        // add exit code
        parseArgs(args);
        if (sourceDirectory.isEmpty()) {
            printHelp();
            return;
        }

        // check valid data
        Path path = Paths.get(sourceDirectory);
        if (Files.notExists(path))
            throw new IllegalArgumentException("no such directory: " + path.toString());

        long startTime = System.currentTimeMillis();

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

        long estimatedTime  = System.currentTimeMillis() - startTime;
        System.out.println("Spent processing time: " + String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(estimatedTime),
                TimeUnit.MILLISECONDS.toSeconds(estimatedTime)));

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
        System.out.println("Use the -p or --path to set directory path with metadata");
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

        Boolean printHelp = false;
        for (int i=0; i < args.length; i++) {

            switch (args[i]) {
                case "-p":
                    sourceDirectory = args[i + 1];
                    break;
                case "--path":
                    sourceDirectory = args[i + 1];
                    break;
                case "-h":
                    printHelp = true;
                    break;
                case "--help":
                    printHelp = true;
                    break;
            }

            if (printHelp) {
                sourceDirectory = "";
                break;
            }
        }

    }

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