package dev.fr13;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

class NodesSorting {

    private final String SOURCE_DIRECTORY;

    NodesSorting(String sourceDirectory) {
        SOURCE_DIRECTORY = sourceDirectory;
    }

    void sortMainConfig() throws TransformerException {

        File file = new File(SOURCE_DIRECTORY + File.separatorChar + "Configuration.xml");
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

    void sortDetails() throws XPathExpressionException, TransformerException {

        NodeList metaObjects = getMetaObjectsDescription();
        if (metaObjects == null) {
            return;
        }

        for (int i = 0; i < metaObjects.getLength(); i++) {

            Node metaObject = metaObjects.item(i);
            if (metaObject.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!metaObject.hasAttributes()) {
                MetadataSorting.userMessage("Invalid format of metadataDescription");
                return;
            }

            Path path = Paths.get(SOURCE_DIRECTORY);
            NamedNodeMap attributes = metaObject.getAttributes();

            String folder = getAttributeValue(attributes, "folder");
            path = Paths.get(SOURCE_DIRECTORY + File.separatorChar + folder);
            if (Files.exists(path)) {

                MetadataSorting.userMessage(String.format("Processing of %s", folder));

                File[] listOfFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
                for (File file:listOfFiles) {
                    sortFileNodes(file, metaObject);
                }
            }

        }

    }

    /*
    @file object metadata in xml
    @metaObject parent node from ConfigDescription for current object
    */
    private void sortFileNodes(File file, Node metaObject) throws XPathExpressionException, TransformerException {

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

    private void sortChildFileNodes(Node parentNode, Document documentToSort,  Node nodeDescription, String pathToNameNode) throws XPathExpressionException {

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
                    MetadataSorting.userMessage("Invalid format data");
                    continue;
                }

                sortedNodes.put(nodeToSort.getTextContent(), nodeToSort);

            }

            importSortedNodes(sortedNodes, nodeToReplace);
            parentNode.replaceChild(nodeToReplace, nodesToSort.item(0).getParentNode());

        }

    }

    private String getAttributeValue(NamedNodeMap attributes, String name){

        Node node = attributes.getNamedItem(name);
        return node != null ? node.getTextContent():"";

    }

    private NodeList getChildNodes(Node node, String path) throws XPathExpressionException {

        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xPath.compile(path);
        Object result = expr.evaluate(node, XPathConstants.NODESET);
        return (NodeList) result;

    }

    private Document getDomDocument(File file) {

        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException | SAXException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return document;

    }

    private void importSortedNodes(Map tree, Node node) {

        tree.forEach((k,v)->{
            Node n = (Node) v;
            Node cloneNode = n.cloneNode(true);
            node.appendChild(cloneNode);
        });

        tree.clear();

    }

    private NodeList getMetaObjectsDescription() {

        Node metaDataDescription = null;

        Path path = Paths.get(SOURCE_DIRECTORY + File.separatorChar + "metadataDescription.xml");
        if (Files.notExists(path)) {
            MetadataSorting.userMessage("Parent objects have been sorted. \n" +
                    "To sort the attributes of objects, you need to place the metadataDescription.xml file in the folder with the configuration files.");
            return null;
        }

        try {
            metaDataDescription = getDomDocument(path.toFile()).getElementsByTagName("MetaDataDescription").item(0);
        } catch (IllegalArgumentException e) {
            MetadataSorting.userMessage("Invalid format of metadataDescription");
            System.exit(1);
        }

        if (!metaDataDescription.hasChildNodes()) {
            return null;
        }

        return metaDataDescription.getChildNodes();

    }

    private void saveToFile(Document document, File file) throws TransformerException {

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);

    }

}
