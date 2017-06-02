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
package com.android.settings.benzo;

import android.os.Bundle;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.benzo.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockScreenSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_LOCKSCREEN_SHORTCUTS_LAUNCH_TYPE =
            "lockscreen_shortcuts_launch_type";
    private static final String LOCKSCREEN_MAX_NOTIF_CONFIG = "lockscreen_max_notif_cofig";

    private static final String LOCK_SCREEN_VISUALIZER_SHOW = "lock_screen_visualizer_show";
    private static final String LOCK_SCREEN_VISUALIZER_USE_CUSTOM_COLOR =
	   "lock_screen_visualizer_use_custom_color";
    private static final String LOCK_SCREEN_VISUALIZER_CUSTOM_COLOR =
	   "lock_screen_visualizer_custom_color";

    private ListPreference mLockscreenShortcutsLaunchType;
    private SeekBarPreference mMaxKeyguardNotifConfig;
    private SwitchPreference mVisualizer;
    private SwitchPreference mVisualizerShowColor;
    private ColorPickerPreference mVisualizerColor;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.BENZO;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_settings);
        ContentResolver resolver = getActivity().getContentResolver();

        PreferenceScreen prefSet = getPreferenceScreen();

        int intColor;
        String hexColor;

        mMaxKeyguardNotifConfig = (SeekBarPreference) findPreference(LOCKSCREEN_MAX_NOTIF_CONFIG);
        int kgconf = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, 5);
        mMaxKeyguardNotifConfig.setValue(kgconf);
        mMaxKeyguardNotifConfig.setOnPreferenceChangeListener(this);

        mLockscreenShortcutsLaunchType = (ListPreference) findPreference(
                PREF_LOCKSCREEN_SHORTCUTS_LAUNCH_TYPE);
        mLockscreenShortcutsLaunchType.setOnPreferenceChangeListener(this);

        mVisualizer =
                (SwitchPreference) findPreference(LOCK_SCREEN_VISUALIZER_SHOW);
        mVisualizer.setChecked(Settings.System.getInt(resolver,
                "lock_screen_visualizer_show", 0) == 1);
        mVisualizer.setOnPreferenceChangeListener(this);

        mVisualizerShowColor =
                (SwitchPreference) findPreference(LOCK_SCREEN_VISUALIZER_USE_CUSTOM_COLOR);
        mVisualizerShowColor.setChecked(Settings.System.getInt(resolver,
                "lock_screen_visualizer_use_custom_color", 0) == 1);
        mVisualizerShowColor.setOnPreferenceChangeListener(this);

        mVisualizerColor =
            (ColorPickerPreference) findPreference(LOCK_SCREEN_VISUALIZER_CUSTOM_COLOR);
        mVisualizerColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(resolver,
                "lock_screen_visualizer_custom_color", 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mVisualizerColor.setSummary(hexColor);
        mVisualizerColor.setNewPreviewColor(intColor);
        mVisualizerColor.setAlphaSliderVisible(true);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        // our container already takes care of the padding
        if (list != null) {
            int paddingTop = list.getPaddingTop();
            int paddingBottom = list.getPaddingBottom();
            list.setPadding(0, paddingTop, 0, paddingBottom);
        }
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mLockscreenShortcutsLaunchType) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SHORTCUTS_LONGPRESS,
                    Integer.valueOf((String) newValue));
            return true;
        } else if (preference == mMaxKeyguardNotifConfig) {
            int kgconf = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.LOCKSCREEN_MAX_NOTIF_CONFIG, kgconf);
            return true;

        } else if (preference == mVisualizer) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    "lock_screen_visualizer_show",
                    value ? 1 : 0);
            return true;
        } else if (preference == mVisualizerShowColor) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    "lock_screen_visualizer_use_custom_color",
                    value ? 1 : 0);
            return true;
        } else if (preference == mVisualizerColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    "lock_screen_visualizer_custom_color", intHex);
            return true;
        }
        return false;
    }
}
