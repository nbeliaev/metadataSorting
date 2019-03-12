package dev.fr13;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MetadataSorting {

    public static void main(String[] args) throws XPathExpressionException, TransformerException {

        parseArgs(args);
        if (sourceDirectory.isEmpty()) {
            printHelp();
            return;
        }

        Path path = Paths.get(sourceDirectory);
        if (Files.notExists(path))
            throw new IllegalArgumentException("no such directory: " + path.toString());

        PerformanceMeasurement.setStartTime();

        sortMainConfig();

        sortDetails();

        PerformanceMeasurement.printSpentTime();

    }

    private static String sourceDirectory = "";

    private static void printHelp() {
        userMessage("MetadataSorting sorts metadata for 1C:Enterprise solutions \n" +
                "Use the -p or --path to set directory path with metadata");
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

    private static void userMessage(String msg) {
        System.out.println(msg);
    }

    private static String getAttributeValue(NamedNodeMap attributes, String name){

        Node node = attributes.getNamedItem(name);
        return node != null ? node.getTextContent():"";

    }

    private static Document getDomDocument(File file) {

        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException|SAXException e) {
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
        });

        tree.clear();

    }

    private static NodeList getChildNodes(Node node, String path) throws XPathExpressionException {

        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xPath.compile(path);
        Object result = expr.evaluate(node, XPathConstants.NODESET);
        return (NodeList) result;

    }

    private static void sortChildFileNodes(Node parentNode, Document documentToSort,  Node nodeDescription, String pathToNameNode) throws XPathExpressionException {

        NamedNodeMap attributes = nodeDescription.getChildNodes().item(1).getAttributes();
        String childPathPrefix = attributes.getLength() != 0 ? getAttributeValue(attributes, "pathPrefix"):"";
        String uuid = getAttributeValue(parentNode.getAttributes(), "uuid");
        childPathPrefix = childPathPrefix.replace("$uuid", uuid);

        NodeList nodesToSort = getChildNodes(parentNode, childPathPrefix);
        if (nodesToSort.getLength() != 0) {

            Node nodeToReplace = documentToSort.importNode(nodesToSort.item(0).getParentNode(), false);
            Map<String, Node> sortedNodes = new TreeMap<>();
            for (int m = 0; m < nodesToSort.getLength(); m++) {

                Node nodeToSort = nodesToSort.item(m);
                if (nodeToSort.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                // make sure there is only one node with uuid
                if (getChildNodes(nodeToSort, pathToNameNode).getLength() != 1) {
                    userMessage("Invalid format data");
                    continue;
                }

                sortedNodes.put(nodeToSort.getTextContent(), nodeToSort);

            }

            importSortedNodes(sortedNodes, nodeToReplace);
            parentNode.replaceChild(nodeToReplace, nodesToSort.item(0).getParentNode());

        }

    }
    /*
    @file object metadata in xml
    @metaObject parent node from ConfigDescription for current object
    */
    private static void sortFileNodes(File file, Node metaObject) throws XPathExpressionException, TransformerException {

        Document documentToSort = getDomDocument(file);
        Node parentNode = documentToSort.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes()) {
            return;
        }

        Node nodeToReplace = documentToSort.importNode(parentNode, false);

        NamedNodeMap parentMetaAttributes = metaObject.getAttributes();
        String parentPathPrefix = getAttributeValue(parentMetaAttributes, "pathPrefix");
        String pathToNameNode = getAttributeValue(parentMetaAttributes, "pathName");

        Map<String, Node> sortedNodes = new TreeMap<>();

        NodeList nodesDescription = metaObject.getChildNodes();
        for (int i = 0; i < nodesDescription.getLength(); i++) {

            Node nodeDescription = nodesDescription.item(i);
            if (nodeDescription.getNodeType() != Node.ELEMENT_NODE)
                continue;

            NamedNodeMap childMetaAttributes = nodeDescription.getAttributes();
            String childPathPrefix   = childMetaAttributes.getLength() != 0 ? getAttributeValue(childMetaAttributes, "pathPrefix"):"";
            String currentPathPrefix = childPathPrefix.isEmpty() ? parentPathPrefix : childPathPrefix;
            String category          = childMetaAttributes.getLength() != 0 ? getAttributeValue(childMetaAttributes, "category"):"";

            boolean nodeDescriptionHasChild = nodeDescription.hasChildNodes();

            NodeList nodesToSort = getChildNodes(documentToSort,currentPathPrefix+nodeDescription.getNodeName());
            for (int j = 0; j < nodesToSort.getLength(); j++) {

                // attributes, forms, templates etc.
                Node nodeToSort = nodesToSort.item(j);

                if (nodeToSort.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                if (nodeDescriptionHasChild) {
                    sortChildFileNodes(nodeToSort, documentToSort, nodeDescription, pathToNameNode);
                }

                if (category.isEmpty()) {
                    NodeList list = getChildNodes(nodeToSort, pathToNameNode);
                    for (int k = 0; k < list.getLength(); k++) {

                        Node attribute = list.item(k);
                        if (attribute.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        sortedNodes.put(attribute.getTextContent(), nodeToSort);

                    }
                } else {
                    sortedNodes.put(nodeToSort.getTextContent(), nodeToSort);
                }


            }

            importSortedNodes(sortedNodes, nodeToReplace);
            sortedNodes.clear();

        }

        if (!sortedNodes.isEmpty()) {
            importSortedNodes(sortedNodes, parentNode);
        }

        parentNode.getParentNode().replaceChild(nodeToReplace, parentNode);
        saveToFile(documentToSort, file);

    }

    private static void saveToFile(Document document, File file) throws TransformerException {

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);

    }

    private static NodeList getMetaObjectsDescription() {

        Node metaDataDescription = null;

        Path path = Paths.get(sourceDirectory + File.separatorChar + "metadataDescription.xml");
        if (Files.notExists(path)) {
            userMessage("Parent objects have been sorted. \n" +
                    "To sort the attributes of objects, you need to place the metadataDescription.xml file in the folder with the configuration files.");
            return null;
        }

        try {
            metaDataDescription = getDomDocument(path.toFile()).getElementsByTagName("MetaDataDescription").item(0);
        } catch (IllegalArgumentException e) {
            userMessage("Invalid format of metadataDescription");
            System.exit(1);
        }

        if (!metaDataDescription.hasChildNodes()) {
            return null;
        }

        return metaDataDescription.getChildNodes();

    }

    private static void sortDetails() throws XPathExpressionException, TransformerException {

        NodeList metaObjects = getMetaObjectsDescription();
        if (metaObjects == null) {
            PerformanceMeasurement.printSpentTime();
            return;
        }

        for (int i = 0; i < metaObjects.getLength(); i++) {

            Node metaObject = metaObjects.item(i);
            if (metaObject.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!metaObject.hasAttributes()) {
                userMessage("Invalid format of metadataDescription");
                PerformanceMeasurement.printSpentTime();
                return;
            }

            Path path = Paths.get(sourceDirectory);
            NamedNodeMap attributes = metaObject.getAttributes();

            String folder = getAttributeValue(attributes, "folder");
            path = Paths.get(sourceDirectory + File.separatorChar + folder);
            if (Files.exists(path)) {

                userMessage(String.format("Processing of %s", folder));

                File[] listOfFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
                for (File file:listOfFiles) {
                    sortFileNodes(file, metaObject);
                }
            }

        }

    }

    private static void sortMainConfig() throws TransformerException  {

        File file = new File(sourceDirectory + File.separatorChar + "Configuration.xml");
        Document document = getDomDocument(file);
        Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        String nodeName = "";
        Map<String, Node> sortedNodes = new TreeMap<>();

        Node parentNodeEmpty = document.importNode(parentNode, false);

        NodeList unsortedObjects = parentNode.getChildNodes();
        for (int i = 0; i < unsortedObjects.getLength(); i++) {

            Node currentUnsortedNode = unsortedObjects.item(i);
            if (currentUnsortedNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!nodeName.equals(currentUnsortedNode.getNodeName())) {
                importSortedNodes(sortedNodes, parentNodeEmpty);
            }

            String currentNodeText = currentUnsortedNode.getTextContent();
            sortedNodes.put(currentNodeText, currentUnsortedNode);
            nodeName = currentUnsortedNode.getNodeName();
        }

        if (sortedNodes.size() != 0)
            importSortedNodes(sortedNodes, parentNodeEmpty);

        parentNode.getParentNode().replaceChild(parentNodeEmpty, parentNode);

        saveToFile(document, file);

    }

}