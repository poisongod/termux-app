package com.termux.shared.termux.extrakeys;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single key shown in the {@link ExtraKeysView}.
 *
 * <p>Instances are built by {@link ExtraKeysInfo} from a tekl matrix; this class no longer deals
 * with JSON. Aliases and display names are resolved before construction.</p>
 */
public class ExtraKeyButton {

    /**
     * The key that will be sent to the terminal, either a control character, like defined in
     * {@link ExtraKeysConstants#PRIMARY_KEY_CODES_FOR_STRINGS} (LEFT, RIGHT, PGUP...) or some text.
     */
    private final String key;

    /**
     * If the key is a macro, i.e. a sequence of keys separated by space.
     */
    private final boolean macro;

    /**
     * The text that will be displayed on the button.
     */
    private final String display;

    /**
     * The {@link ExtraKeyButton} containing the information of the popup button (triggered by swipe up).
     */
    @Nullable
    private final ExtraKeyButton popup;

    /**
     * Initialize a {@link ExtraKeyButton}.
     *
     * @param key The key sequence to send. For macros, space-separated key names.
     * @param macro Whether {@code key} is a macro (multiple keys).
     * @param display The text to show on the button.
     * @param popup Optional popup button triggered by swipe up.
     */
    public ExtraKeyButton(@NonNull String key, boolean macro, @NonNull String display,
                          @Nullable ExtraKeyButton popup) {
        this.key = key;
        this.macro = macro;
        this.display = display;
        this.popup = popup;
    }

    /** Get {@link #key}. */
    public String getKey() {
        return key;
    }

    /** Check whether a {@link #macro} is defined or not. */
    public boolean isMacro() {
        return macro;
    }

    /** Get {@link #display}. */
    public String getDisplay() {
        return display;
    }

    /** Get {@link #popup}. */
    @Nullable
    public ExtraKeyButton getPopup() {
        return popup;
    }

}
