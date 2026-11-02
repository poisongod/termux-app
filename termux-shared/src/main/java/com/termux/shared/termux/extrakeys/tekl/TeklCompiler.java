package com.termux.shared.termux.extrakeys.tekl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Compiles a parsed tekl matrix into {@link TeklKey} objects.
 *
 * <p>This class is deliberately free of Android dependencies so it can be unit tested on the JVM.
 * The caller supplies the display map ({@code extra-keys-style}) and the alias map
 * ({@code CONTROL_CHARS_ALIASES}), exactly like the previous JSON path did.</p>
 */
public final class TeklCompiler {

    private TeklCompiler() {}

    /**
     * Compile all rows.
     *
     * @param rows             Parsed tekl rows.
     * @param extraKeyDisplayMap Maps key names to the glyph shown on the key (may be empty).
     * @param extraKeyAliasMap   Maps aliases to real key names, e.g. {@code BACKSLASH -> \}.
     */
    public static List<List<TeklKey>> compile(List<List<TeklNode>> rows,
                                              Map<String, String> extraKeyDisplayMap,
                                              Map<String, String> extraKeyAliasMap)
        throws TeklException {
        List<List<TeklKey>> compiled = new ArrayList<>(rows.size());
        for (List<TeklNode> row : rows) {
            List<TeklKey> keys = new ArrayList<>(row.size());
            for (TeklNode entry : row) {
                keys.add(compileEntry(entry, extraKeyDisplayMap, extraKeyAliasMap));
            }
            compiled.add(keys);
        }
        return compiled;
    }

    /** M ::= A | A '.' A */
    private static TeklKey compileEntry(TeklNode node,
                                        Map<String, String> displayMap,
                                        Map<String, String> aliasMap) throws TeklException {
        if (node.kind != TeklNode.Kind.POPUP) {
            return compileAtom(node, displayMap, aliasMap);
        }

        TeklKey main = compileAtom(node.node, displayMap, aliasMap);
        TeklKey popup = compileAtom(node.popup, displayMap, aliasMap);
        return new TeklKey(main.key, main.macro, main.display, popup);
    }

    /** A ::= N | S | A '(' A ')' | A ':' N */
    private static TeklKey compileAtom(TeklNode node,
                                       Map<String, String> displayMap,
                                       Map<String, String> aliasMap) throws TeklException {
        switch (node.kind) {
            case KEY: {
                String key = applyAlias(aliasMap, keyName(node));
                return new TeklKey(key, false, displayFor(displayMap, key), null);
            }
            case LABEL: {
                TeklKey base = compileAtom(node.node, displayMap, aliasMap);
                return new TeklKey(base.key, base.macro, node.label, base.popup);
            }
            case COMBO: {
                List<String> keys = new ArrayList<>();
                collectComboKeys(node.node, keys);
                collectComboKeys(node.inner, keys);
                if (keys.size() < 2) {
                    throw new TeklException(
                        "combo '" + join(keys) + "' must contain the modifier and at least one key",
                        0, 0);
                }

                StringBuilder sent = new StringBuilder();
                StringBuilder display = new StringBuilder();
                for (int i = 0; i < keys.size(); i++) {
                    String key = applyAlias(aliasMap, keys.get(i));
                    if (i > 0) {
                        sent.append(' ');
                        display.append(' ');
                    }
                    sent.append(key);
                    display.append(displayFor(displayMap, key));
                }
                return new TeklKey(sent.toString(), true, display.toString(), null);
            }
            case POPUP:
            default:
                throw new TeklException("popup is not valid inside '()'", 0, 0);
        }
    }

    private static void collectComboKeys(TeklNode node, List<String> out) throws TeklException {
        switch (node.kind) {
            case COMBO:
                collectComboKeys(node.node, out);
                collectComboKeys(node.inner, out);
                break;
            case KEY:
                out.add(keyName(node));
                break;
            case LABEL:
                throw new TeklException("combo contains 'label' (only keys allowed inside '()')",
                    0, 0);
            case POPUP:
            default:
                throw new TeklException("combo contains 'popup' (only keys allowed inside '()')",
                    0, 0);
        }
    }

    /**
     * Named/special keys normalize to uppercase; quoted literals and unknown identifiers keep
     * their original case. Mirrors the reference implementation.
     */
    private static String keyName(TeklNode node) {
        if (node.literal) {
            return node.key;
        }
        if (TeklParser.isNamedKey(node.key) || TeklParser.isSpecialKey(node.key)) {
            return node.key.toUpperCase(Locale.ROOT);
        }
        return node.key;
    }

    private static String applyAlias(Map<String, String> aliasMap, String key) {
        String aliased = aliasMap.get(key);
        return aliased != null ? aliased : key;
    }

    private static String displayFor(Map<String, String> displayMap, String key) {
        String display = displayMap.get(key);
        return display != null ? display : key;
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
