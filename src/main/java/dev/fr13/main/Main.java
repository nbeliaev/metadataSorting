package dev.fr13.main;

import dev.fr13.NodesSorting;
import dev.fr13.PerformanceMeasurement;
import dev.fr13.helpers.ArgsParser;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws XPathExpressionException, TransformerException, FileNotFoundException {
        final String sourceDirectory = ArgsParser.getSourceDirectory(args);
        if (Files.notExists(Paths.get(sourceDirectory))) {
            throw new FileNotFoundException("No such directory: " + sourceDirectory);
        }

        PerformanceMeasurement performanceMeasurement = new PerformanceMeasurement();
        performanceMeasurement.start();
        NodesSorting nodesSorting = new NodesSorting(sourceDirectory);
        nodesSorting.sortMainConfig();
        nodesSorting.sortDetails();
        performanceMeasurement.printSpentTime();

    }
}