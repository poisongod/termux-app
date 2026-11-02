package com.termux.shared.termux.extrakeys.tekl;

/**
 * A compiled tekl key, ready to become an {@code ExtraKeyButton}.
 *
 * <p>Aliases have already been applied to {@link #key}; {@link #display} has already been resolved
 * through the extra-key display map (or set explicitly by a {@code :Label}).</p>
 */
public final class TeklKey {

    /** Key sequence sent to the terminal. For macros, space-separated keys. */
    public final String key;
    /** Whether {@link #key} is a combination of multiple keys. */
    public final boolean macro;
    /** Text shown on the button. */
    public final String display;
    /** Swipe-up key, or {@code null}. */
    public final TeklKey popup;

    public TeklKey(String key, boolean macro, String display, TeklKey popup) {
        this.key = key;
        this.macro = macro;
        this.display = display;
        this.popup = popup;
    }
}
