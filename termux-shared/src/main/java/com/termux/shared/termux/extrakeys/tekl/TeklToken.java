package com.termux.shared.termux.extrakeys.tekl;

/** A single token produced by {@link TeklTokenizer}. */
final class TeklToken {

    enum Kind {
        NEWLINE,
        SEMI,
        DOT,
        LPAREN,
        RPAREN,
        COLON,
        EQ,
        NUMBER,
        LITERAL,
        IDENT,
        EOF
    }

    final Kind kind;
    /** Value for {@link Kind#IDENT} and {@link Kind#LITERAL}. */
    final String text;
    /** Value for {@link Kind#NUMBER}. */
    final long number;
    final int line;
    final int column;

    private TeklToken(Kind kind, String text, long number, int line, int column) {
        this.kind = kind;
        this.text = text;
        this.number = number;
        this.line = line;
        this.column = column;
    }

    static TeklToken simple(Kind kind, int line, int column) {
        return new TeklToken(kind, "", 0, line, column);
    }

    static TeklToken ofString(Kind kind, String text, int line, int column) {
        return new TeklToken(kind, text, 0, line, column);
    }

    static TeklToken ofNumber(long value, int line, int column) {
        return new TeklToken(Kind.NUMBER, "", value, line, column);
    }

    String kindName() {
        return kind.name();
    }
}
