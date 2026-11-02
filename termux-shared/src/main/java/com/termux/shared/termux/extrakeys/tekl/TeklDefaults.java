package com.termux.shared.termux.extrakeys.tekl;

/** Built-in tekl layouts shipped with the app. */
public final class TeklDefaults {

    private TeklDefaults() {}

    /**
     * Default extra-keys layout: physical-keyboard inspired, inverted-T arrows on the right,
     * ESC in the top-left corner, popups paired like the shifted symbols of a real keyboard.
     */
    public static final String DEFAULT_LAYOUT =
        "ESC.DRAWER \"`\".\"~\" \"[\".\"{\" \"]\".\"}\" APOSTROPHE.QUOTE UP.HOME \"=\".\"+\" KEYBOARD;" +
        "TAB.\"^\" CTRL ALT BACKSLASH.\"|\" LEFT.\"<\" DOWN.END RIGHT.\">\" ENTER.PASTE";
}
