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

    public static void userMessage(String msg) {
        System.out.println(msg);
    }

    public static void userMessage() {
        System.out.println();
    }

    private static Document domDocument(String filename) throws ParserConfigurationException, IOException, SAXException {

        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(filename);
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

    private static void importSortedNodes(Map tree, Node node) {

        tree.forEach((k,v)->{
            Node n = (Node) v;
            Node cloneNode = n.cloneNode(true);
            node.appendChild(cloneNode);
            //userMessage((String) k);
        });

        tree.clear();

    }

    private static NodeList getChildNodes(Node node, String path)  throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        //xPath.setNamespaceContext(new NamespaceResolver(node));
        XPathExpression expr = xPath.compile(path);
        Object result = expr.evaluate(node, XPathConstants.NODESET);
        return (NodeList) result;
    }

    private static void sortFileNodes(String fileName) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        Document document = domDocument(fileName);
        Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        Node parentNodeEmpty = document.importNode(parentNode, false);

        Map<String, Node> treeMap = new TreeMap<String, Node>();

        // attributes
        NodeList childNodes = getChildNodes(parentNode, "//Catalog/ChildObjects/Attribute");
        for (int i=0; i < childNodes.getLength(); i++) {

            String key = getChildNodes(childNodes.item(i), "Properties/Name").item(0).getTextContent();
            treeMap.put(key, childNodes.item(i));

        }

        importSortedNodes(treeMap, parentNodeEmpty);

        // tabular sections
        NodeList childNodest = getChildNodes(parentNode, "//Catalog/ChildObjects/TabularSection");
        for (int i=0; i < childNodest.getLength(); i++) {

            String key = getChildNodes(childNodest.item(i), "Properties/Name").item(0).getTextContent();
            treeMap.put(key, childNodest.item(i));

        }

        importSortedNodes(treeMap, parentNodeEmpty);

        // attributes of tabular sections for parentNodeEmpty


    }

    private static void sortMainConfig(String fileName) throws ParserConfigurationException, IOException, SAXException  {

        Document document = domDocument(fileName);
        Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        String nodeName = "";
        Map<String, Node> treeMap = new TreeMap<String, Node>();

        Node parentNodeEmpty = document.importNode(parentNode, false);

        NodeList unsortedObjects = parentNode.getChildNodes();
        for (int i = 0; i < unsortedObjects.getLength(); i++) {

            Node currentUnsortedNode = unsortedObjects.item(i);
            if (currentUnsortedNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!nodeName.equals(currentUnsortedNode.getNodeName())) {

                importSortedNodes(treeMap, parentNodeEmpty);

            }

            String currentNodeText = currentUnsortedNode.getTextContent();
            treeMap.put(currentNodeText, currentUnsortedNode);
            nodeName = currentUnsortedNode.getNodeName();
        }

        if (treeMap.size() != 0)
            importSortedNodes(treeMap, parentNodeEmpty);

        parentNode.getParentNode().replaceChild(parentNodeEmpty, parentNode);

        // print results
        NodeList childTemp = document.getElementsByTagName("ChildObjects");

        if (!childTemp.item(0).hasChildNodes())
            return;

        NodeList listTemp = childTemp.item(0).getChildNodes();
        for (int i = 0; i < listTemp.getLength(); i++) {

            Node node = listTemp.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            userMessage(listTemp.item(i).getTextContent());

        }

    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        // add exit code
        parseArgs(args);
        if (sourceDirectory.isEmpty()) {
            printHelp();
            return;
        }

        Path path;
        FileFilter filter;

        path = Paths.get(sourceDirectory);
        if (Files.notExists(path))
            throw new IllegalArgumentException("no such directory: " + path.toString());

        long startTime = System.currentTimeMillis();

        String fileName;
        fileName = pathToMainConfig();
        //sortMainConfig(fileName);

        path = Paths.get(sourceDirectory + File.separatorChar + "Catalogs");
        if (Files.exists(path)) {
            
            File directoryFiles = new File(path.toString());
            File[] listOfFiles = directoryFiles.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            for (File file:listOfFiles) {
                sortFileNodes(file.getPath());
                
            }
        }

        long estimatedTime  = System.currentTimeMillis() - startTime;
        userMessage("Spent processing time: " + String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(estimatedTime),
                TimeUnit.MILLISECONDS.toSeconds(estimatedTime)));

    }

    private static void printHelp() {
        userMessage("MetadataSorting sorts metadata for 1C:Enterprise solutions");
        userMessage();
        userMessage("Use the -p or --path to set directory path with metadata");
    }

    private static String pathToMainConfig() {
        return sourceDirectory + File.separatorChar + "Configuration.xml";
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