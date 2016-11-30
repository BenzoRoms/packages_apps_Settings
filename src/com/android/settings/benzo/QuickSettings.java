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
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.benzo.SeekBarPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.Date;

public class QuickSettings  extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String PREF_TILE_ANIM_STYLE = "qs_tile_animation_style";
    private static final String PREF_TILE_ANIM_DURATION = "qs_tile_animation_duration";
    private static final String PREF_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator";
    private static final String KEY_SYSUI_QQS_COUNT = "sysui_qqs_count_key";

    private static final String PREF_CAT_LANDSCAPE =
            "qs_cat_landscape";
    private static final String PREF_ROWS_PORTRAIT =
            "qs_rows_portrait";
    private static final String PREF_COLUMNS_PORTRAIT =
            "qs_columns_portrait";
    private static final String PREF_ROWS_LANDSCAPE =
            "qs_rows_landscape";
    private static final String PREF_COLUMNS_LANDSCAPE =
            "qs_columns_landscape";

    private ListPreference mTileAnimationStyle;
    private ListPreference mTileAnimationDuration;
    private ListPreference mTileAnimationInterpolator;
    private ListPreference mSysuiQqsCount;

    private SeekBarPreference mRowsPortrait;
    private SeekBarPreference mColumnsPortrait;
    private SeekBarPreference mRowsLandscape;
    private SeekBarPreference mColumnsLandscape;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.BENZO;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.quick_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        // QS animation
        mTileAnimationStyle = (ListPreference) findPreference(PREF_TILE_ANIM_STYLE);
        int tileAnimationStyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.ANIM_TILE_STYLE, 0,
                UserHandle.USER_CURRENT);
        mTileAnimationStyle.setValue(String.valueOf(tileAnimationStyle));
        updateTileAnimationStyleSummary(tileAnimationStyle);
        updateAnimTileStyle(tileAnimationStyle);
        mTileAnimationStyle.setOnPreferenceChangeListener(this);

        mTileAnimationDuration = (ListPreference) findPreference(PREF_TILE_ANIM_DURATION);
        int tileAnimationDuration = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.ANIM_TILE_DURATION, 2000,
                UserHandle.USER_CURRENT);
        mTileAnimationDuration.setValue(String.valueOf(tileAnimationDuration));
        updateTileAnimationDurationSummary(tileAnimationDuration);
        mTileAnimationDuration.setOnPreferenceChangeListener(this);

        mTileAnimationInterpolator = (ListPreference) findPreference(PREF_TILE_ANIM_INTERPOLATOR);
        int tileAnimationInterpolator = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.ANIM_TILE_INTERPOLATOR, 0,
                UserHandle.USER_CURRENT);
        mTileAnimationInterpolator.setValue(String.valueOf(tileAnimationInterpolator));
        updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
        mTileAnimationInterpolator.setOnPreferenceChangeListener(this);

        mSysuiQqsCount = (ListPreference) findPreference(KEY_SYSUI_QQS_COUNT);
        if (mSysuiQqsCount != null) {
           mSysuiQqsCount.setOnPreferenceChangeListener(this);
           int SysuiQqsCount = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.QQS_COUNT, 5);
           mSysuiQqsCount.setValue(Integer.toString(SysuiQqsCount));
           mSysuiQqsCount.setSummary(mSysuiQqsCount.getEntry());
        }

        mRowsPortrait = (SeekBarPreference) findPreference(PREF_ROWS_PORTRAIT);
        int rowsPortrait = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_ROWS_PORTRAIT, 3);
        mRowsPortrait.setValue(rowsPortrait);
        mRowsPortrait.setOnPreferenceChangeListener(this);

        mColumnsPortrait = (SeekBarPreference) findPreference(PREF_COLUMNS_PORTRAIT);
        int columnsPortrait = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_COLUMNS_PORTRAIT, 3);
        mColumnsPortrait.setValue(columnsPortrait);
        mColumnsPortrait.setOnPreferenceChangeListener(this);

        mRowsLandscape = (SeekBarPreference) findPreference(PREF_ROWS_LANDSCAPE);
        int rowsLandscape = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_ROWS_LANDSCAPE, 2);
        mRowsLandscape.setValue(rowsLandscape);
        mRowsLandscape.setOnPreferenceChangeListener(this);

        mColumnsLandscape = (SeekBarPreference) findPreference(PREF_COLUMNS_LANDSCAPE);
        int columnsLandscape = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_COLUMNS_LANDSCAPE, 5);
        mColumnsLandscape.setValue(columnsLandscape);
        mColumnsLandscape.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
       int intValue;
       int index;
       if (preference == mTileAnimationStyle) {
            int tileAnimationStyle = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(), Settings.System.ANIM_TILE_STYLE,
                    tileAnimationStyle, UserHandle.USER_CURRENT);
            updateTileAnimationStyleSummary(tileAnimationStyle);
            updateAnimTileStyle(tileAnimationStyle);
            return true;
       } else if (preference == mTileAnimationDuration) {
            int tileAnimationDuration = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(), Settings.System.ANIM_TILE_DURATION,
                    tileAnimationDuration, UserHandle.USER_CURRENT);
            updateTileAnimationDurationSummary(tileAnimationDuration);
            return true;
       } else if (preference == mTileAnimationInterpolator) {
            int tileAnimationInterpolator = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(), Settings.System.ANIM_TILE_INTERPOLATOR,
                    tileAnimationInterpolator, UserHandle.USER_CURRENT);
            updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
            return true;
       } else if (preference == mSysuiQqsCount) {
            String SysuiQqsCount = (String) objValue;
            int SysuiQqsCountValue = Integer.parseInt(SysuiQqsCount);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.QQS_COUNT, SysuiQqsCountValue);
            int SysuiQqsCountIndex = mSysuiQqsCount.findIndexOfValue(SysuiQqsCount);
            mSysuiQqsCount.setSummary(mSysuiQqsCount.getEntries()[SysuiQqsCountIndex]);
            return true;
        } else if (preference == mRowsPortrait) {
            int rowsPortrait = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_ROWS_PORTRAIT, rowsPortrait);
            return true;
        } else if (preference == mColumnsPortrait) {
            int columnsPortrait = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_COLUMNS_PORTRAIT, columnsPortrait);
            return true;
        } else if (preference == mRowsLandscape) {
            int rowsLandscape = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_ROWS_LANDSCAPE, rowsLandscape);
            return true;
        } else if (preference == mColumnsLandscape) {
            int columnsLandscape = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QS_COLUMNS_LANDSCAPE, columnsLandscape);
            return true;
      }
      return false;
    }

    private void updateTileAnimationStyleSummary(int tileAnimationStyle) {
        String prefix = (String) mTileAnimationStyle.getEntries()[mTileAnimationStyle.findIndexOfValue(String
                .valueOf(tileAnimationStyle))];
        mTileAnimationStyle.setSummary(getResources().getString(R.string.qs_set_animation_style, prefix));
    }

    private void updateTileAnimationDurationSummary(int tileAnimationDuration) {
        String prefix = (String) mTileAnimationDuration.getEntries()[mTileAnimationDuration.findIndexOfValue(String
                .valueOf(tileAnimationDuration))];
        mTileAnimationDuration.setSummary(getResources().getString(R.string.qs_set_animation_duration, prefix));
    }

    private void updateTileAnimationInterpolatorSummary(int tileAnimationInterpolator) {
        String prefix = (String) mTileAnimationInterpolator.getEntries()[mTileAnimationInterpolator.findIndexOfValue(String
                .valueOf(tileAnimationInterpolator))];
        mTileAnimationInterpolator.setSummary(getResources().getString(R.string.qs_set_animation_interpolator, prefix));
    }

    private void updateAnimTileStyle(int tileAnimationStyle) {
        if (mTileAnimationDuration != null) {
            if (tileAnimationStyle == 0) {
                mTileAnimationDuration.setSelectable(false);
                mTileAnimationInterpolator.setSelectable(false);
            } else {
                mTileAnimationDuration.setSelectable(true);
                mTileAnimationInterpolator.setSelectable(true);
            }
        }
    }
}
