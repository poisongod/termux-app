package com.termux.shared.termux.extrakeys.tekl;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts tekl source into a flat token list.
 *
 * <p>Mirrors the reference Rust tokenizer: {@code #} starts a line comment, {@code '...'} and
 * {@code "..."} are raw literals (no escape sequences, cannot span a newline), identifiers are
 * ASCII letters/digits/underscore, and {@code . ( ) : ; =} are punctuation.</p>
 */
final class TeklTokenizer {

    private TeklTokenizer() {}

    static List<TeklToken> tokenize(String src) throws TeklException {
        List<TeklToken> tokens = new ArrayList<>();
        int i = 0;
        int line = 1;
        int column = 1;
        int n = src.length();

        while (i < n) {
            char c = src.charAt(i);

            if (c == '\n') {
                tokens.add(TeklToken.simple(TeklToken.Kind.NEWLINE, line, column));
                line++;
                column = 1;
                i++;
                continue;
            }

            if (c == ' ' || c == '\t' || c == '\r') {
                column++;
                i++;
                continue;
            }

            if (c == '#') {
                while (i < n && src.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }

            if (c == ';') {
                tokens.add(TeklToken.simple(TeklToken.Kind.SEMI, line, column));
                column++;
                i++;
                continue;
            }

            if (c == '.') {
                tokens.add(TeklToken.simple(TeklToken.Kind.DOT, line, column));
                column++;
                i++;
                continue;
            }

            if (c == '(') {
                tokens.add(TeklToken.simple(TeklToken.Kind.LPAREN, line, column));
                column++;
                i++;
                continue;
            }

            if (c == ')') {
                tokens.add(TeklToken.simple(TeklToken.Kind.RPAREN, line, column));
                column++;
                i++;
                continue;
            }

            if (c == ':') {
                tokens.add(TeklToken.simple(TeklToken.Kind.COLON, line, column));
                column++;
                i++;
                continue;
            }

            if (c == '=') {
                tokens.add(TeklToken.simple(TeklToken.Kind.EQ, line, column));
                column++;
                i++;
                continue;
            }

            // Numbers are only meaningful as a global property value (`height = 2`).
            if (isDigit(c) && !tokens.isEmpty()
                && tokens.get(tokens.size() - 1).kind == TeklToken.Kind.EQ) {
                int start = i;
                while (i < n && isDigit(src.charAt(i))) {
                    i++;
                }
                long value = Long.parseLong(src.substring(start, i));
                tokens.add(TeklToken.ofNumber(value, line, column));
                column += i - start;
                continue;
            }

            if (c == '\'' || c == '"') {
                char quote = c;
                int j = i + 1;
                while (j < n && src.charAt(j) != quote && src.charAt(j) != '\n') {
                    j++;
                }
                if (j >= n || src.charAt(j) != quote) {
                    throw new TeklException("unterminated string literal", line, column);
                }
                tokens.add(TeklToken.ofString(
                    TeklToken.Kind.LITERAL, src.substring(i + 1, j), line, column));
                column += j - i + 1;
                i = j + 1;
                continue;
            }

            if (isIdentChar(c)) {
                int start = i;
                while (i < n && isIdentChar(src.charAt(i))) {
                    i++;
                }
                tokens.add(TeklToken.ofString(
                    TeklToken.Kind.IDENT, src.substring(start, i), line, column));
                column += i - start;
                continue;
            }

            throw new TeklException("unexpected character '" + c + "'", line, column);
        }

        tokens.add(TeklToken.simple(TeklToken.Kind.EOF, line, column));
        return tokens;
    }

    private static boolean isIdentChar(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || isDigit(c)
            || c == '_';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
