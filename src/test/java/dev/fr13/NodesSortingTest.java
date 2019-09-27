package dev.fr13;

import dev.fr13.exeptions.ParserDomException;
import dev.fr13.helpers.DomDocument;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.File;

public class NodesSortingTest {

    @Test
    public void sortMainConfig() throws TransformerException {
        NodesSorting sorting = new NodesSorting("in");
        sorting.sortMainConfig();

        try {
            final Document sampleDocument = DomDocument.getDom(new File("out" + File.separatorChar + "Configuration.xml"));
            final Document sortedDocument = DomDocument.getDom(new File("in" + File.separatorChar + "Configuration.xml"));

            final Node sampleParent = sampleDocument.getElementsByTagName("ChildObjects").item(0);
            final Node sortedParent = sortedDocument.getElementsByTagName("ChildObjects").item(0);

            if (!sortedParent.hasChildNodes()) {
                Assert.fail("Sorted document has no items");
            }

            final NodeList sampleChildNodes = sampleParent.getChildNodes();
            final NodeList sortedChildNodes = sortedParent.getChildNodes();
            for (int i = 0; i < sortedChildNodes.getLength(); i++) {
                final Node sortedItem = sortedChildNodes.item(i);
                if (sortedItem.getNodeType() == Node.ELEMENT_NODE) {
                    Assert.assertEquals(
                            sortedItem.getNodeName(),
                            sampleChildNodes.item(i).getTextContent(),
                            sortedItem.getTextContent());
                }
            }
        } catch (ParserDomException e) {
            throw new IllegalStateException(e);
        }
    }
}