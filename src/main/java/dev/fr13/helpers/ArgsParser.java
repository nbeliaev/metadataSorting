package dev.fr13.helpers;

public class ArgsParser {

    public static String getSourceDirectory(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Use the -p or --path to set directory path with metadata");
        }
        return args[1];
    }
}
