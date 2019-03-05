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

    private static void userMessage(String msg) {
        System.out.println(msg);
    }

    private static void userMessage() {
        System.out.println();
    }

    private static Document domDocument(File file) {

        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
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

    private static void sortFileNodes(File file, Node metaObject) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        Document document = domDocument(file);
        Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        Node parentNodeEmpty = document.importNode(parentNode, false);

        NodeList childNodes = metaObject.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {

            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            NamedNodeMap attributes = metaObject.getAttributes();
            String pathPrefix = getAttributeValue(attributes, "pathPrefix");

            // TODO
            userMessage(pathPrefix +node.getTextContent());
        }



        Map<String, Node> treeMap = new TreeMap<>();



        // attributes
        /*NodeList childNodes = getChildNodes(parentNode, "//Catalog/ChildObjects/Attribute");
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

        importSortedNodes(treeMap, parentNodeEmpty);*/

        // attributes of tabular sections for parentNodeEmpty


    }

    private static void sortMainConfig(File file) throws ParserConfigurationException, IOException, SAXException  {

        Document document = domDocument(file);
        Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        String nodeName = "";
        Map<String, Node> treeMap = new TreeMap<>();

        // TODO skip subsystems
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

        path = Paths.get(sourceDirectory);
        if (Files.notExists(path))
            throw new IllegalArgumentException("no such directory: " + path.toString());

        long startTime = System.currentTimeMillis();

        String fileName;
        //sortMainConfig(sourceDirectory + File.separatorChar + "Configuration.xml");

        path = Paths.get(sourceDirectory + File.separatorChar + "ConfigDescription.xml");
        if (Files.notExists(path)) {
            userMessage("Parent objects have been sorted. \n" +
                    "To sort the attributes of objects, you need to place the ConfigDescription.xml file in the folder with the configuration files.");
            return;
        }

        Document configDescription = domDocument(path.toFile());
        NodeList list = configDescription.getElementsByTagName("MetaDataDescription");
        if (list.getLength() != 1) {
            userMessage("Invalid format of ConfigDescription");
            return;
        }

        Node metaDataDescription = list.item(0);
        if (!metaDataDescription.hasChildNodes()) {
            return;
        }

        NodeList metaObjects = metaDataDescription.getChildNodes();
        for (int i = 0; i < metaObjects.getLength(); i++) {

            Node metaObject = metaObjects.item(i);
            if (metaObject.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!metaObject.hasAttributes()) {
                userMessage("Invalid format of ConfigDescription");
                return;
            }

            NamedNodeMap attributes = metaObject.getAttributes();

            String folder = getAttributeValue(attributes, "folder");
            path = Paths.get(sourceDirectory + File.separatorChar + folder);
            if (Files.exists(path)) {

                 /*ObjectDescription objectDescription = new ObjectDescription(
                        getAttributeValue(attributes, "pathPrefix"),
                        getAttributeValue(attributes, "name"));*/

                // TODO add to class method sort

                File[] listOfFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".xml"));

                for (File file:listOfFiles) {
                    sortFileNodes(file, metaObject);
                }
            }

            //objectDescription description = new objectDescription(folder);


        }

        long estimatedTime  = System.currentTimeMillis() - startTime;
        userMessage("Spent processing time: " + String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(estimatedTime),
                TimeUnit.MILLISECONDS.toSeconds(estimatedTime)));

    }

    private static String getAttributeValue(NamedNodeMap attributes, String name) throws IllegalArgumentException{

        Node node = attributes.getNamedItem(name);
        if (node == null) {
            throw new IllegalArgumentException("Invalid format of ConfigDescription");
        }
        return node.getTextContent();

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

        boolean printHelp = false;
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

    private static class ObjectDescription {

        private String pathPrefix;
        private String name;

        ObjectDescription(String pathPrefix, String name) {
            this.pathPrefix = pathPrefix;
            this.name = name;
        }

        ObjectDescription(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

      void setAttributes(String pathPrefix, String name) {
          this.pathPrefix = pathPrefix;
          this.name = name;
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