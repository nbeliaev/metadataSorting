package dev.fr13;

import javax.xml.transform.*;
import javax.xml.xpath.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MetadataSorting {

    public static void main(String[] args) throws XPathExpressionException, TransformerException {

        String sourceDirectory = parseArgs(args);
        if (sourceDirectory.isEmpty()) {
            printHelp();
            return;
        }

        Path path = Paths.get(sourceDirectory);
        if (Files.notExists(path))
            throw new IllegalArgumentException("no such directory: " + path.toString());

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
            printHelp();
            return sourceDirectory;
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

        return sourceDirectory;

    }

    private static void printHelp() {
        userMessage("MetadataSorting sorts metadata for 1C:Enterprise solutions \n" +
                "Use the -p or --path to set directory path with metadata");
    }

    static void userMessage(String msg) {
        System.out.println(msg);
    }

}