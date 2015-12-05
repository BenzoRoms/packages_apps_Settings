/*
* Copyright (C) 2016 Benzo Rom
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = ButtonSettings.class.getSimpleName();

    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";

    // volume rocker reorient
    private static final String SWAP_VOLUME_BUTTONS = "swap_volume_buttons";

    private ListPreference mVolumeKeyCursorControl;
    private SwitchPreference mSwapVolumeButtons;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.BENZO;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.button_settings);

        final Resources res = getResources();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        // Cursor volume keys
        int cursorControlAction = Settings.System.getInt(resolver,
                Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
        mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                cursorControlAction);

        // volume rocker reorient
        mSwapVolumeButtons = (SwitchPreference) findPreference(SWAP_VOLUME_BUTTONS);
        mSwapVolumeButtons.setOnPreferenceChangeListener(this);
        int swapVolumeButtons = Settings.System.getInt(getContentResolver(),
                SWAP_VOLUME_BUTTONS, 0);
        mSwapVolumeButtons.setChecked(swapVolumeButtons != 0);
    }

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, objValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        } else if (preference == mSwapVolumeButtons) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SWAP_VOLUME_BUTTONS,
                    value ? 1 : 0);
            return true;
        }
        return false;
    }

}
