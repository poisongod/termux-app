package com.termux.shared.termux.extrakeys.tekl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recursive-descent parser for the tekl extra-keys grammar.
 *
 * <pre>
 * N ::= ESC | TAB | ... | '"' any '"'
 * S ::= CTRL | ALT | FN | SHIFT
 * A ::= N | S | A '(' A ')' | A ':' N
 * M ::= A | A '.' A
 * P ::= { M | M ';' | newline }
 * </pre>
 *
 * <p>Comments start with {@code #} and run to the end of the line. {@code ;} terminates a row;
 * newlines are only formatting. Global property assignments ({@code height = 2}, {@code theme = ...})
 * are part of the language and are parsed for validity but ignored: the extra-keys matrix does not
 * use them, and rejecting them would discard the whole keyboard over a harmless line.</p>
 */
public final class TeklParser {

    private static final String[] NAMED_KEYS = {
        "ESC", "TAB", "HOME", "END", "PGUP", "PGDN",
        "INS", "DEL", "BKSP",
        "UP", "LEFT", "RIGHT", "DOWN",
        "ENTER", "BACKSLASH", "QUOTE", "APOSTROPHE",
        "F1", "F2", "F3", "F4", "F5", "F6", "F7",
        "F8", "F9", "F10", "F11", "F12",
        "KEYBOARD", "DRAWER", "SCROLL", "SPACE", "PASTE",
    };

    private static final String[] SPECIAL_KEYS = {"CTRL", "ALT", "FN", "SHIFT"};

    private static final String[] GLOBAL_KEYS = {"height", "theme", "layout", "reload"};

    private final List<TeklToken> tokens;
    private int pos;

    private TeklParser(List<TeklToken> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parse tekl source into a matrix of rows.
     *
     * @throws TeklException on any tokenizer or parser error, with line and column.
     */
    public static TeklProgram parse(String source) throws TeklException {
        return new TeklParser(TeklTokenizer.tokenize(source)).parseProgram();
    }

    private TeklProgram parseProgram() throws TeklException {
        List<List<TeklNode>> rows = new ArrayList<>();
        List<TeklNode> row = new ArrayList<>();

        while (true) {
            TeklToken token = peek();

            switch (token.kind) {
                case EOF:
                    if (!row.isEmpty()) {
                        rows.add(row);
                    }
                    validateRows(rows);
                    return new TeklProgram(rows);
                case NEWLINE:
                    next();
                    break;
                case SEMI:
                    next();
                    if (!row.isEmpty()) {
                        rows.add(row);
                        row = new ArrayList<>();
                    }
                    break;
                case IDENT:
                    if (isGlobalKey(token.text)) {
                        parseGlobal();
                    } else {
                        row.add(parseEntry());
                    }
                    break;
                case LITERAL:
                    row.add(parseEntry());
                    break;
                default:
                    throw new TeklException("unexpected token " + token.kindName(),
                        token.line, token.column);
            }
        }
    }

    /** M ::= A | A '.' A */
    private TeklNode parseEntry() throws TeklException {
        TeklNode main = parseAtom();

        if (peek().kind == TeklToken.Kind.DOT) {
            next();
            TeklNode popup = parseAtom();
            if (popup.kind == TeklNode.Kind.POPUP) {
                throw new TeklException("nested popup is not supported",
                    peek().line, peek().column);
            }
            return TeklNode.popup(main, popup);
        }

        return main;
    }

    /**
     * G ::= "height" '=' NUMBER | "theme" '=' IDENT | "layout" '=' IDENT | "reload"
     *
     * <p>The value is validated exactly like the reference CLI parser, then discarded. Keeping the
     * grammar identical means a file that {@code tekl check} accepts also loads in the app.</p>
     */
    private void parseGlobal() throws TeklException {
        TeklToken keyToken = next();
        String key = keyToken.text.toLowerCase(Locale.ROOT);
        if ("reload".equals(key)) {
            return;
        }
        expect(TeklToken.Kind.EQ);
        if ("height".equals(key)) {
            expect(TeklToken.Kind.NUMBER);
        } else {
            expect(TeklToken.Kind.IDENT);
        }
    }

    /** A ::= N | S | A '(' A ')' | A ':' N */
    private TeklNode parseAtom() throws TeklException {
        TeklNode node = parseBaseKey();

        while (true) {
            TeklToken token = peek();

            if (token.kind == TeklToken.Kind.LPAREN) {
                next();
                TeklNode inner = parseAtom();
                expect(TeklToken.Kind.RPAREN);
                node = TeklNode.combo(node, inner);
                continue;
            }

            if (token.kind == TeklToken.Kind.COLON) {
                next();
                TeklToken labelToken = peek();
                if (labelToken.kind == TeklToken.Kind.LITERAL) {
                    throw new TeklException("label must not be quoted",
                        labelToken.line, labelToken.column);
                }
                if (labelToken.kind == TeklToken.Kind.IDENT) {
                    next();
                    node = TeklNode.label(node, labelToken.text);
                } else {
                    node = TeklNode.label(node, "");
                }
                continue;
            }

            break;
        }

        return node;
    }

    private TeklNode parseBaseKey() throws TeklException {
        TeklToken token = next();
        switch (token.kind) {
            case IDENT:
                return TeklNode.key(token.text, false, isSpecialKey(token.text));
            case LITERAL:
                return TeklNode.key(token.text, true, false);
            default:
                throw new TeklException("expected key, got " + token.kindName(),
                    token.line, token.column);
        }
    }

    private TeklToken peek() {
        return tokens.get(pos);
    }

    private TeklToken next() {
        TeklToken token = tokens.get(pos);
        if (token.kind != TeklToken.Kind.EOF) {
            pos++;
        }
        return token;
    }

    private void expect(TeklToken.Kind kind) throws TeklException {
        TeklToken token = next();
        if (token.kind != kind) {
            throw new TeklException("expected " + kind.name() + ", got " + token.kindName(),
                token.line, token.column);
        }
    }

    /** A row may only contain a given modifier (CTRL/ALT/FN/SHIFT) once. */
    private static void validateRows(List<List<TeklNode>> rows) throws TeklException {
        for (int r = 0; r < rows.size(); r++) {
            Set<String> specials = new HashSet<>();
            for (TeklNode entry : rows.get(r)) {
                TeklNode keyNode = entry;
                if (entry.kind == TeklNode.Kind.LABEL) {
                    keyNode = entry.node;
                }
                if (keyNode.kind != TeklNode.Kind.KEY || !keyNode.special) {
                    continue;
                }
                String upper = keyNode.key.toUpperCase(Locale.ROOT);
                if (!specials.add(upper)) {
                    throw new TeklException(
                        "special key '" + keyNode.key + "' appears more than once in row " + (r + 1),
                        0, 0);
                }
            }
        }
    }

    private static boolean isGlobalKey(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String global : GLOBAL_KEYS) {
            if (global.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    static boolean isSpecialKey(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        for (String key : SPECIAL_KEYS) {
            if (key.equals(upper)) {
                return true;
            }
        }
        return false;
    }

    static boolean isNamedKey(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        for (String key : NAMED_KEYS) {
            if (key.equals(upper)) {
                return true;
            }
        }
        return false;
    }
}
