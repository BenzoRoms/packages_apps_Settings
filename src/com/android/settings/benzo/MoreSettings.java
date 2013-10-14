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

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.benzo.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.Utils;

public class MoreSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String MEDIA_SCANNER_ON_BOOT = "media_scanner_on_boot";
    private static final String PREF_TRANSPARENT_VOLUME_DIALOG = "transparent_volume_dialog";
    private static final String SHOW_CPU_INFO_KEY = "show_cpu_info";

    private ListPreference mMsob;
    private SeekBarPreference mVolumeDialogAlpha;
    private SwitchPreference mShowCpuInfo;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DONT_TRACK_ME_BRO;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final ContentResolver resolver = getActivity().getContentResolver();
        addPreferencesFromResource(R.xml.more_settings);

        mShowCpuInfo = (SwitchPreference) findPreference(SHOW_CPU_INFO_KEY);
        mShowCpuInfo.setChecked(Settings.Global.getInt(getActivity().getContentResolver(),
            Settings.Global.SHOW_CPU, 0) == 1);

        mMsob = (ListPreference) findPreference(MEDIA_SCANNER_ON_BOOT);
        mMsob.setValue(String.valueOf(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MEDIA_SCANNER_ON_BOOT, 0)));
        mMsob.setSummary(mMsob.getEntry());
        mMsob.setOnPreferenceChangeListener(this);

        // Volume dialog alpha
        mVolumeDialogAlpha = (SeekBarPreference) findPreference(PREF_TRANSPARENT_VOLUME_DIALOG);
        int volumeDialogAlpha = Settings.System.getInt(resolver,
                Settings.System.TRANSPARENT_VOLUME_DIALOG, 255);
        mVolumeDialogAlpha.setValue(volumeDialogAlpha / 1);
        mVolumeDialogAlpha.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mShowCpuInfo) {
            writeCpuInfoOptions();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mMsob) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.MEDIA_SCANNER_ON_BOOT,
                    Integer.valueOf(String.valueOf(newValue)));
            mMsob.setValue(String.valueOf(newValue));
            mMsob.setSummary(mMsob.getEntry());
            return true;
        } else if (preference == mVolumeDialogAlpha) {
            int alpha = (Integer) newValue;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.TRANSPARENT_VOLUME_DIALOG, alpha * 1);
            return true;
        }
        return false;
    }

    private void writeCpuInfoOptions() {
        boolean value = mShowCpuInfo.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(),
                Settings.Global.SHOW_CPU, value ? 1 : 0);
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.CPUInfoService");
        if (value) {
            getActivity().startService(service);
        } else {
            getActivity().stopService(service);
        }
    }
}
