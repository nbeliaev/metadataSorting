package dev.fr13.exeptions;

public class ParserDomException extends Exception {
    public ParserDomException(String fileName) {
        super(String.format("Couldn't parse %s", fileName));
    }
}
