package dev.fr13;

import dev.fr13.helpers.DomDocument;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class NodesSorting {
    private final String SOURCE_DIRECTORY;
    private final File file;

    public NodesSorting(String sourceDirectory) {
        SOURCE_DIRECTORY = sourceDirectory;
        file = new File(SOURCE_DIRECTORY + File.separatorChar + "Configuration.xml");
    }

    public void sortMainConfig() {
        final Document document = DomDocument.getDom(file);
        final Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes()) {
            return;
        }

        String nodeName = "";
        final Map<String, Node> sortedNodes = new TreeMap<>();
        final Node parentNodeEmpty = document.importNode(parentNode, false);
        final NodeList unsortedObjects = parentNode.getChildNodes();
        for (int i = 0; i < unsortedObjects.getLength(); i++) {
            final Node currentUnsortedNode = unsortedObjects.item(i);
            if (currentUnsortedNode.getNodeType() == Node.ELEMENT_NODE) {
                if (!nodeName.equals(currentUnsortedNode.getNodeName())) {
                    importSortedNodes(sortedNodes, parentNodeEmpty);
                }
                sortedNodes.put(currentUnsortedNode.getTextContent(), currentUnsortedNode);
                nodeName = currentUnsortedNode.getNodeName();
            }
        }

        if (sortedNodes.size() != 0) {
            importSortedNodes(sortedNodes, parentNodeEmpty);
        }
        parentNode.getParentNode().replaceChild(parentNodeEmpty, parentNode);
        DomDocument.saveDom(document, file);
    }

    public void sortDetails() throws XPathExpressionException, TransformerException {

        NodeList metaObjects = getMetaObjectsDescription();
        if (metaObjects == null) {
            return;
        }

        for (int i = 0; i < metaObjects.getLength(); i++) {

            Node metaObject = metaObjects.item(i);
            if (metaObject.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (!metaObject.hasAttributes()) {
                System.out.println("Invalid format of metadataDescription");
                return;
            }

            Path path = Paths.get(SOURCE_DIRECTORY);
            NamedNodeMap attributes = metaObject.getAttributes();

            String folder = getAttributeValue(attributes, "folder");
            path = Paths.get(SOURCE_DIRECTORY + File.separatorChar + folder);
            if (Files.exists(path)) {

                System.out.println(String.format("Processing of %s", folder));

                File[] listOfFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
                for (File file : listOfFiles) {
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

        Document documentToSort = null;
        documentToSort = DomDocument.getDom(file);
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
            String childPathPrefix = childMetaAttributes.getLength() != 0 ? getAttributeValue(childMetaAttributes, "pathPrefix") : "";
            String currentPathPrefix = childPathPrefix.isEmpty() ? parentPathPrefix : childPathPrefix;
            String category = childMetaAttributes.getLength() != 0 ? getAttributeValue(childMetaAttributes, "category") : "";

            boolean nodeDescriptionHasChild = nodeDescription.hasChildNodes();

            NodeList nodesToSort = getChildNodes(documentToSort, currentPathPrefix + nodeDescription.getNodeName());
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
        DomDocument.saveDom(documentToSort, file);

    }

    private void sortChildFileNodes(Node parentNode, Document documentToSort, Node nodeDescription, String pathToNameNode) throws XPathExpressionException {

        NamedNodeMap attributes = nodeDescription.getChildNodes().item(1).getAttributes();
        String childPathPrefix = attributes.getLength() != 0 ? getAttributeValue(attributes, "pathPrefix") : "";
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
                    System.out.println("Invalid format data");
                    continue;
                }

                sortedNodes.put(nodeToSort.getTextContent(), nodeToSort);

            }

            importSortedNodes(sortedNodes, nodeToReplace);
            parentNode.replaceChild(nodeToReplace, nodesToSort.item(0).getParentNode());

        }

    }

    private String getAttributeValue(NamedNodeMap attributes, String name) {

        Node node = attributes.getNamedItem(name);
        return node != null ? node.getTextContent() : "";

    }

    private NodeList getChildNodes(Node node, String path) throws XPathExpressionException {

        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xPath.compile(path);
        Object result = expr.evaluate(node, XPathConstants.NODESET);
        return (NodeList) result;

    }

    private void importSortedNodes(Map tree, Node node) {

        tree.forEach((k, v) -> {
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
            System.out.println("Parent objects have been sorted. \n" +
                    "To sort the attributes of objects, you need to place the metadataDescription.xml file in the folder with the configuration files.");
            return null;
        }

        try {
            metaDataDescription = DomDocument.getDom(path.toFile()).getElementsByTagName("MetaDataDescription").item(0);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid format of metadataDescription");
            System.exit(1);
        }

        if (!metaDataDescription.hasChildNodes()) {
            return null;
        }

        return metaDataDescription.getChildNodes();

    }
}
