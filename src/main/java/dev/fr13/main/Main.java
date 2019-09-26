package dev.fr13.main;

import dev.fr13.NodesSorting;
import dev.fr13.PerformanceMeasurement;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws XPathExpressionException, TransformerException, FileNotFoundException {

        String sourceDirectory = parseArgs(args);
        if (sourceDirectory.isEmpty()) {
            printHelp();
            return;
        }

        Path path = Paths.get(sourceDirectory);
        if (Files.notExists(path))
            throw new FileNotFoundException("no such directory: " + path.toString());

        NodesSorting nodesSorting = new NodesSorting(sourceDirectory);

        PerformanceMeasurement performanceMeasurement = new PerformanceMeasurement();

        nodesSorting.sortMainConfig();
        nodesSorting.sortDetails();

        performanceMeasurement.printSpentTime();

    }

    private static String parseArgs(String[] args) {

        String sourceDirectory = "";
        int argsCount = args.length;

        if (argsCount == 0) {
            return sourceDirectory;
        }

        boolean printHelp = false;
        for (int i = 0; i < args.length; i++) {

            switch (args[i]) {
                case "-p":
                    sourceDirectory = args.length > 1 ? args[i + 1] : "";
                    break;
                case "--path":
                    sourceDirectory = args.length > 1 ? args[i + 1] : "";
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

        return sourceDirectory;

    }

    private static void printHelp() {
        System.out.println(("This tool sorts metadata objects by alphabet for solutions based on 1C:Enterprise platform. \n" +
                "Works with xml files of configuration. \n" +
                "Use the -p or --path to set directory path with metadata"));
    }
}