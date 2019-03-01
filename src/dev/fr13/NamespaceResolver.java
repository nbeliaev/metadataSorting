package dev.fr13;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;
import org.w3c.dom.Document;

public class NamespaceResolver implements NamespaceContext {

    private Document sourceDocument;

    public NamespaceResolver(Document document) {
        sourceDocument = document;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix.contains(XMLConstants.DEFAULT_NS_PREFIX))
            return sourceDocument.lookupNamespaceURI(null);
        else
            return sourceDocument.lookupNamespaceURI(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return null;
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return null;
    }
}
