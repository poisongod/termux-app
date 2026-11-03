package com.termux.shared.termux.extrakeys;

import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS;
import com.termux.shared.termux.extrakeys.tekl.TeklCompiler;
import com.termux.shared.termux.extrakeys.tekl.TeklException;
import com.termux.shared.termux.extrakeys.tekl.TeklKey;
import com.termux.shared.termux.extrakeys.tekl.TeklParser;
import com.termux.shared.termux.extrakeys.tekl.TeklProgram;
import com.termux.shared.termux.terminal.io.TerminalExtraKeys;

import java.util.List;

/**
 * A {@link Class} that defines the info needed by {@link ExtraKeysView} to display the extra key
 * views.
 *
 * <p>The tekl source passed to the constructors of this class is parsed directly into keys;
 * there is no JSON intermediate. Semicolons separate rows; newlines and {@code #} comments are
 * ignored. Each element of a row is one key:</p>
 *
 * <pre>
 * KEY                 plain key                 ESC
 * KEY:Label           custom display             ESC:Quit
 * MODIFIER(KEY)       macro / combination        CTRL(C)
 * MODIFIER(KEY):Label macro with custom display   CTRL(C):copy
 * KEY.POPUP           popup on swipe up          ESC.F1
 * "literal"           raw text key               "|"
 * </pre>
 *
 * <p>Examples:</p>
 *
 * <pre>
 * # Empty:
 *
 * # Single row:
 * ESC TAB CTRL ALT "-"."|" DOWN UP
 *
 * # Two rows:
 * ESC "/" "-"."|" HOME UP END PGUP;
 * TAB CTRL ALT LEFT DOWN RIGHT PGDN
 *
 * # Macros, labels and popups:
 * CTRL(C):copy ESC.F1 CTRL(F):tmux-exit
 * </pre>
 *
 * <p>Named keys are normalized to uppercase. Literals and unknown identifiers keep their case.
 * Escaping is not needed and not supported inside quotes: {@code "\\"} is two backslashes.</p>
 *
 * <p>Aliases are also allowed for the keys that you can pass as {@code extraKeyAliasMap}. Check
 * {@link ExtraKeysConstants#CONTROL_CHARS_ALIASES}.</p>
 *
 * <p>Its up to the {@link ExtraKeysView.IExtraKeysView} client on how to handle individual key values
 * of an {@link ExtraKeyButton}. They are sent as is via
 * {@link ExtraKeysView.IExtraKeysView#onExtraKeyButtonClick(View, ExtraKeyButton, MaterialButton)}. The
 * {@link TerminalExtraKeys} which is an implementation of the interface,
 * checks if the key is one of {@link ExtraKeysConstants#PRIMARY_KEY_CODES_FOR_STRINGS} and generates
 * a {@link android.view.KeyEvent} for it, and if its not, then converts the key to code points by
 * calling {@link CharSequence#codePoints()} and passes them to the terminal as literal strings.</p>
 *
 * <p>Examples:</p>
 *
 * <pre>
 * "ENTER" will trigger the ENTER keycode
 * "LEFT" will trigger the LEFT keycode and be displayed as "←"
 * "→" will input a "→" character
 * "−" will input a "−" character
 * "-_-" will input the string "-_-"
 * </pre>
 *
 * <p>For more info, check https://wiki.termux.com/wiki/Touch_Keyboard.</p>
 */
public class ExtraKeysInfo {

    /**
     * Matrix of buttons to be displayed in {@link ExtraKeysView}.
     */
    private final ExtraKeyButton[][] mButtons;

    /**
     * Initialize {@link ExtraKeysInfo}.
     *
     * @param propertiesInfo The {@link String} containing the tekl source to create the
     *                       {@link ExtraKeysInfo}. Check the class javadoc for details.
     * @param style The style to pass to {@link #getCharDisplayMapForStyle(String)} to get the
     *              {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the display text
     *              mapping for the keys if a custom label is not defined for a key.
     * @param extraKeyAliasMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           aliases for the actual key names. You can create your own or
     *                           optionally pass {@link ExtraKeysConstants#CONTROL_CHARS_ALIASES}.
     */
    public ExtraKeysInfo(@NonNull String propertiesInfo, String style,
                         @NonNull ExtraKeysConstants.ExtraKeyDisplayMap extraKeyAliasMap) throws TeklException {
        mButtons = initExtraKeysInfo(propertiesInfo, getCharDisplayMapForStyle(style), extraKeyAliasMap);
    }

    /**
     * Initialize {@link ExtraKeysInfo}.
     *
     * @param propertiesInfo The {@link String} containing the tekl source to create the
     *                       {@link ExtraKeysInfo}. Check the class javadoc for details.
     * @param extraKeyDisplayMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           display text mapping for the keys if a custom label is not defined
     *                           for a key. You can create your own or optionally pass one of the
     *                           values defined in {@link #getCharDisplayMapForStyle(String)}.
     * @param extraKeyAliasMap The {@link ExtraKeysConstants.ExtraKeyDisplayMap} that defines the
     *                           aliases for the actual key names. You can create your own or
     *                           optionally pass {@link ExtraKeysConstants#CONTROL_CHARS_ALIASES}.
     */
    public ExtraKeysInfo(@NonNull String propertiesInfo,
                         @NonNull ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap,
                         @NonNull ExtraKeysConstants.ExtraKeyDisplayMap extraKeyAliasMap) throws TeklException {
        mButtons = initExtraKeysInfo(propertiesInfo, extraKeyDisplayMap, extraKeyAliasMap);
    }

    private ExtraKeyButton[][] initExtraKeysInfo(@NonNull String propertiesInfo,
                                                 @NonNull ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap,
                                                 @NonNull ExtraKeysConstants.ExtraKeyDisplayMap extraKeyAliasMap)
        throws TeklException {
        TeklProgram program = TeklParser.parse(propertiesInfo);
        List<List<TeklKey>> matrix = TeklCompiler.compile(program.rows, extraKeyDisplayMap, extraKeyAliasMap);

        ExtraKeyButton[][] buttons = new ExtraKeyButton[matrix.size()][];
        for (int i = 0; i < matrix.size(); i++) {
            List<TeklKey> row = matrix.get(i);
            buttons[i] = new ExtraKeyButton[row.size()];
            for (int j = 0; j < row.size(); j++) {
                buttons[i][j] = toButton(row.get(j));
            }
        }

        return buttons;
    }

    private static ExtraKeyButton toButton(TeklKey key) {
        ExtraKeyButton popup = key.popup == null ? null : toButton(key.popup);
        return new ExtraKeyButton(key.key, key.macro, key.display, popup);
    }

    public ExtraKeyButton[][] getMatrix() {
        return mButtons;
    }

    @NonNull
    public static ExtraKeysConstants.ExtraKeyDisplayMap getCharDisplayMapForStyle(String style) {
        switch (style) {
            case "arrows-only":
                return EXTRA_KEY_DISPLAY_MAPS.ARROWS_ONLY_CHAR_DISPLAY;
            case "arrows-all":
                return EXTRA_KEY_DISPLAY_MAPS.LOTS_OF_ARROWS_CHAR_DISPLAY;
            case "all":
                return EXTRA_KEY_DISPLAY_MAPS.FULL_ISO_CHAR_DISPLAY;
            case "none":
                return new ExtraKeysConstants.ExtraKeyDisplayMap();
            default:
                return EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY;
        }
    }

}
