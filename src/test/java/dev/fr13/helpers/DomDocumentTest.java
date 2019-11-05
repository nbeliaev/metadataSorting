package dev.fr13.helpers;

import org.junit.Test;

import java.io.File;

public class DomDocumentTest {

    @Test(expected = IllegalStateException.class)
    public void getDom() {
        DomDocument.getDom(new File("dummy"));
    }
}