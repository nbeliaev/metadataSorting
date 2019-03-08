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

    /*
    @file object metadata in xml
    @metaObject parent node from ConfigDescription for current object
    */
    private static void sortFileNodes(File file, Node metaObject) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {

        Document document = domDocument(file);
        Node parentNode = document.getElementsByTagName("ChildObjects").item(0);
        if (!parentNode.hasChildNodes())
            return;

        Node parentNodeEmpty = document.importNode(parentNode, false);

        NamedNodeMap parentMetaAttributes = metaObject.getAttributes();
        String parentPathPrefix = getAttributeValue(parentMetaAttributes, "pathPrefix");
        String parentName = getAttributeValue(parentMetaAttributes, "name");

        NodeList metaNodes = metaObject.getChildNodes();
        for (int i = 0; i < metaNodes.getLength(); i++) {

            Node metaNode = metaNodes.item(i);
            if (metaNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            NamedNodeMap childMetaAttributes = metaNode.getAttributes();

            String childPathPrefix = childMetaAttributes.getLength() != 0 ? getAttributeValue(childMetaAttributes, "pathPrefix"):"";
            String childName = childMetaAttributes.getLength() != 0 ? getAttributeValue(childMetaAttributes, "category"):"";
            // TODO
            if (childName.isEmpty()) {
                System.out.println(metaNode.getNodeName());
            } else {
                System.out.println(metaNode.getNodeName() + " simple");
            }
            String currentPathPrefix = childPathPrefix.isEmpty() ? parentPathPrefix : childPathPrefix;
            String currentName = childName.isEmpty() ? parentName : childName;

            Map<String, Node> treeMap = new TreeMap<>();

            //userMessage(currentPathPrefix+metaNode.getNodeName());
            //userMessage(Boolean.toString(metaNode.hasChildNodes()));
            Boolean metaNodeHasChild = metaNode.hasChildNodes();

            NodeList nodes = getChildNodes(document,currentPathPrefix+metaNode.getNodeName());
            //userMessage(Integer.toString(nodes.getLength()));
            for (int j = 0; j < nodes.getLength(); j++) {

                // attributes, forms, templates etc.
                Node node = nodes.item(j);

                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;



                if (metaNodeHasChild) {
                    NamedNodeMap chmetaattr = metaNode.getChildNodes().item(1).getAttributes();
                    String childPathPrefix1 = chmetaattr.getLength() != 0 ? getAttributeValue(chmetaattr, "pathPrefix"):"";
                    String uuid = getAttributeValue(node.getAttributes(), "uuid");
                    childPathPrefix1 = childPathPrefix1.replace("$uuid", uuid);

                    NodeList tabularSectionRows = getChildNodes(node, childPathPrefix1);
                    if (tabularSectionRows.getLength() != 0) {

                        Node tabularSectionRowsToImport = document.importNode(tabularSectionRows.item(0).getParentNode(), false);

                        Map<String, Node> rowsTree = new TreeMap<>();

                        for (int m = 0; m < tabularSectionRows.getLength(); m++) {

                            Node attribute = tabularSectionRows.item(m);

                            if (attribute.getNodeType() != Node.ELEMENT_NODE)
                                continue;

                            NodeList attributeRowsNames = getChildNodes(attribute, currentName);
                            if (attributeRowsNames.getLength() != 1) {
                                userMessage("Invalid format data");
                                continue;
                            }

                            //userMessage(attributeRowsNames.item(0).getTextContent());
                            //userMessage(node.getNodeName() + ": " + attribute.getTextContent());
                            rowsTree.put(attribute.getTextContent(), attribute);

                        }

                        importSortedNodes(rowsTree, tabularSectionRowsToImport);
                        node.replaceChild(tabularSectionRowsToImport, tabularSectionRows.item(0).getParentNode());

                        //System.out.println(node.getTextContent());
                        //System.out.println(node.getNodeName());
                        //userMessage(childPathPrefix1);
                        //System.out.println(getChildNodes(node, childPathPrefix1).getLength());
                        //System.out.println("----------------------------");

                        // в node вся таб часть. нужно найти в ней childObjects (реквизиты таб. части)
                        // отсортировать их и добавить в пустой узел ChildObjects узла node
                        // после этого node можно добавлять в treeMap
                    }

                }

                NodeList list = getChildNodes(node, currentName);
                //userMessage(currentPathPrefix+metaNode.getTextContent());
                //userMessage(Integer.toString(getChildNodes(node, currentPathPrefix+metaNode.getTextContent()).getLength()));
                for (int k = 0; k < list.getLength(); k++) {

                    Node attribute = list.item(k);

                    if (attribute.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    //userMessage(node.getNodeName() + ": " + attribute.getTextContent());
                    treeMap.put(attribute.getTextContent(), node);

                }



            }

            importSortedNodes(treeMap, parentNodeEmpty);
            treeMap.clear();

        }

        parentNode.getParentNode().replaceChild(parentNodeEmpty, parentNode);
        saveToFile(document);
 
    /*    NodeList l = parentNodeEmpty.getChildNodes();
        for (int m = 0; m < l.getLength(); m++) {

            Node n = l.item(m);

            if (n.getNodeType() != Node.ELEMENT_NODE)
                continue;

            userMessage(n.getTextContent());
        }
*/
    }

    private static void saveToFile(Document document) throws TransformerException {

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        String strResult = writer.toString();
        //userMessage("**********************************************");
        //userMessage(strResult);

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

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {

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

                File[] listOfFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".xml"));

                for (File file:listOfFiles) {
                    sortFileNodes(file, metaObject);
                }
            }

        }

        long estimatedTime  = System.currentTimeMillis() - startTime;
        userMessage("Spent processing time: " + String.format(
                "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(estimatedTime),
                TimeUnit.MILLISECONDS.toSeconds(estimatedTime)));

    }

    private static String getAttributeValue(NamedNodeMap attributes, String name){

        Node node = attributes.getNamedItem(name);
        return node != null ? node.getTextContent():"";

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