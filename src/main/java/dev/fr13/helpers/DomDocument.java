package dev.fr13.helpers;

import dev.fr13.exeptions.ParserDomException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class DomDocument {
    public static Document getDom(File file) throws ParserDomException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new ParserDomException(file.getAbsolutePath());
        }
    }
}
