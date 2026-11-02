package com.termux.shared.termux.extrakeys.tekl;

/**
 * AST node for the tekl extra-keys grammar.
 *
 * <pre>
 * A ::= N | S | A '(' A ')' | A ':' N
 * M ::= A | A '.' A
 * </pre>
 */
public final class TeklNode {

    public enum Kind {
        /** A plain key, either an identifier (ESC, CTRL, F1...) or a quoted literal. */
        KEY,
        /** {@code MODIFIER(KEY)} — becomes a macro. */
        COMBO,
        /** {@code KEY:Label} — overrides the display text. */
        LABEL,
        /** {@code KEY.POPUP} — swipe-up key. */
        POPUP
    }

    public final Kind kind;

    /** For {@link Kind#KEY}: the raw key text without quotes. */
    public final String key;
    /** For {@link Kind#KEY}: whether the key was written as a quoted literal. */
    public final boolean literal;
    /** For {@link Kind#KEY}: whether the key is one of CTRL/ALT/FN/SHIFT. */
    public final boolean special;

    /** For {@link Kind#COMBO}: the outer (modifier) atom. For {@link Kind#LABEL}: the inner atom. */
    public final TeklNode node;
    /** For {@link Kind#COMBO}: the inner atom. */
    public final TeklNode inner;
    /** For {@link Kind#LABEL}: the label text. */
    public final String label;
    /** For {@link Kind#POPUP}: the popup atom. */
    public final TeklNode popup;

    private TeklNode(Kind kind, String key, boolean literal, boolean special,
                     TeklNode node, TeklNode inner, String label, TeklNode popup) {
        this.kind = kind;
        this.key = key;
        this.literal = literal;
        this.special = special;
        this.node = node;
        this.inner = inner;
        this.label = label;
        this.popup = popup;
    }

    static TeklNode key(String key, boolean literal, boolean special) {
        return new TeklNode(Kind.KEY, key, literal, special, null, null, null, null);
    }

    static TeklNode combo(TeklNode outer, TeklNode inner) {
        return new TeklNode(Kind.COMBO, null, false, false, outer, inner, null, null);
    }

    static TeklNode label(TeklNode inner, String label) {
        return new TeklNode(Kind.LABEL, null, false, false, inner, null, label, null);
    }

    static TeklNode popup(TeklNode main, TeklNode popup) {
        return new TeklNode(Kind.POPUP, null, false, false, main, null, null, popup);
    }
}
