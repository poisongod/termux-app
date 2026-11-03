package com.termux.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.errors.Error;
import com.termux.shared.file.FileUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysConstants;
import com.termux.shared.termux.extrakeys.ExtraKeysInfo;
import com.termux.shared.termux.extrakeys.tekl.TeklDefaults;
import com.termux.shared.termux.extrakeys.tekl.TeklException;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.settings.properties.TermuxSharedProperties;
import com.termux.shared.termux.terminal.io.TerminalExtraKeys;
import com.termux.view.TerminalView;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class TermuxTerminalExtraKeys extends TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final TermuxActivity mActivity;
    final TermuxTerminalViewClient mTermuxTerminalViewClient;
    final TermuxTerminalSessionActivityClient mTermuxTerminalSessionActivityClient;

    private static final String LOG_TAG = "TermuxTerminalExtraKeys";

    public TermuxTerminalExtraKeys(TermuxActivity activity, @NonNull TerminalView terminalView,
                                   TermuxTerminalViewClient termuxTerminalViewClient,
                                   TermuxTerminalSessionActivityClient termuxTerminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mTermuxTerminalViewClient = termuxTerminalViewClient;
        mTermuxTerminalSessionActivityClient = termuxTerminalSessionActivityClient;

        setExtraKeys();
    }


    /**
     * Set the terminal extra keys and style.
     *
     * <p>The layout is read from {@link TermuxConstants#TERMUX_EXTRA_KEYS_FILE_PATH}
     * ({@code ~/.termux/termux.tekl}) as tekl source. If the file does not exist, the built-in
     * default layout is used. An empty file means "no extra keys" and hides the toolbar.</p>
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        String extraKeysStyle = (String) mActivity.getProperties().getInternalPropertyValue(TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE, true);

        ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle);
        if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY.equals(extraKeyDisplayMap) && !TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE.equals(extraKeysStyle)) {
            Logger.logError(TermuxSharedProperties.LOG_TAG, "The style \"" + extraKeysStyle + "\" for the key \"" + TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE + "\" is invalid. Using default style instead.");
            extraKeysStyle = TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
        }

        String tekl = readExtraKeysFile();
        try {
            mExtraKeysInfo = new ExtraKeysInfo(tekl, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (TeklException e) {
            Logger.showToast(mActivity, "Could not parse \"" + TermuxConstants.TERMUX_EXTRA_KEYS_FILE_PATH + "\": " + e.getMessage(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not parse \"" + TermuxConstants.TERMUX_EXTRA_KEYS_FILE_PATH + "\": ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(TeklDefaults.DEFAULT_LAYOUT, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (TeklException e2) {
                Logger.showToast(mActivity, "Can't create default extra keys",true);
                Logger.logStackTraceWithMessage(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    /**
     * Read the tekl layout file, falling back to {@link TeklDefaults#DEFAULT_LAYOUT} if it is
     * missing or cannot be read.
     */
    @NonNull
    private String readExtraKeysFile() {
        String path = TermuxConstants.TERMUX_EXTRA_KEYS_FILE_PATH;

        if (!new File(path).isFile()) {
            Logger.logDebug(LOG_TAG, "Extra keys file not found at \"" + path + "\", using default layout.");
            return TeklDefaults.DEFAULT_LAYOUT;
        }

        StringBuilder data = new StringBuilder();
        Error error = FileUtils.readTextFromFile("extra keys", path, StandardCharsets.UTF_8, data, false);
        if (error != null) {
            Logger.showToast(mActivity, "Could not read \"" + path + "\": " + error.getMinimalErrorString(), true);
            Logger.logError(LOG_TAG, "Could not read \"" + path + "\": " + error.getMinimalErrorString());
            return TeklDefaults.DEFAULT_LAYOUT;
        }

        return data.toString();
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mTermuxTerminalViewClient != null)
                mTermuxTerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mTermuxTerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mTermuxTerminalSessionActivityClient != null)
                mTermuxTerminalSessionActivityClient.onPasteTextFromClipboard(null);
        }  else if ("SCROLL".equals(key)) {
            TerminalView terminalView = mTermuxTerminalViewClient.getActivity().getTerminalView();
            if (terminalView != null && terminalView.mEmulator != null)
                terminalView.mEmulator.toggleAutoScrollDisabled();
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
