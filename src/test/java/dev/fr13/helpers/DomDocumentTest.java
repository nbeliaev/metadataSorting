package dev.fr13.helpers;

import dev.fr13.exeptions.ParserDomException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class DomDocumentTest {

    @Test(expected = IllegalStateException.class)
    public void getDom() {
        try {
            DomDocument.getDom(new File("dummy"));
        } catch (ParserDomException e) {
            throw new IllegalStateException(e);
        }
    }
}