package com.termux.shared.termux.extrakeys.tekl;

/**
 * Thrown when a tekl extra-keys definition cannot be tokenized, parsed or compiled.
 *
 * <p>The message always includes the 1-based line and column of the offending token so that
 * the error can be shown directly in a toast instead of a generic JSON failure.</p>
 */
public class TeklException extends Exception {

    private final int line;
    private final int column;

    public TeklException(String message, int line, int column) {
        super(line > 0
            ? "tekl: " + message + " (line " + line + ", column " + column + ")"
            : "tekl: " + message);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
