/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DropDownPreference.Callback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import static android.provider.Settings.Secure.CAMERA_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.UiModeManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.benzo.DisplayRotation;
import com.android.settings.dashboard.DashboardContainerView;
import com.android.settings.util.Helpers;

import java.util.ArrayList;
import java.util.List;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener, Indexable {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    private static final String KEY_DOZE_FRAGMENT = "doze_fragment";
    private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_CAMERA_GESTURE = "camera_gesture";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";

    private static final String ROTATION_LOCKSCREEN = "Lockscreen";
    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";

    private static final String DASHBOARD_COLUMNS = "dashboard_columns";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private FontDialogPreference mFontSizePref;
    private PreferenceScreen mDisplayRotationPreference;

    private final Configuration mCurConfig = new Configuration();

    private ListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private PreferenceScreen mDozeFragement;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mAutoBrightnessPreference;
    private SwitchPreference mCameraGesturePreference;
    private SwitchPreference mWakeWhenPluggedOrUnplugged;

    private ListPreference mDashboardColumns;

    private ContentObserver mAccelerometerRotationObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mFontSizePref = (FontDialogPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        mDashboardColumns = (ListPreference) findPreference(DASHBOARD_COLUMNS);
        mDashboardColumns.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.DASHBOARD_COLUMNS, DashboardContainerView.mDashboardValue)));
        mDashboardColumns.setSummary(mDashboardColumns.getEntry());
        mDashboardColumns.setOnPreferenceChangeListener(this);

        if (isAutomaticBrightnessAvailable(getResources())) {
            mAutoBrightnessPreference = (SwitchPreference) findPreference(KEY_AUTO_BRIGHTNESS);
            mAutoBrightnessPreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_BRIGHTNESS);
        }

        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }

        mDozeFragement = (PreferenceScreen) findPreference(KEY_DOZE_FRAGMENT);
        if (!isDozeAvailable(activity)) {
            getPreferenceScreen().removePreference(mDozeFragement);
            mDozeFragement = null;
        }

        if (isTapToWakeAvailable(getResources())) {
            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            mTapToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TAP_TO_WAKE);
        }

        boolean proximityCheckOnWait = getResources().getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWait) {
            removePreference(KEY_PROXIMITY_WAKE);
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 0);
        }

        if (isCameraGestureAvailable(getResources())) {
            mCameraGesturePreference = (SwitchPreference) findPreference(KEY_CAMERA_GESTURE);
            mCameraGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_CAMERA_GESTURE);
        }

        if (RotationPolicy.isRotationSupported(activity)) {
            mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        } else {
            removePreference(KEY_DISPLAY_ROTATION);
        }

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }

        mWakeWhenPluggedOrUnplugged =
                (SwitchPreference) findPreference(KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED);
    }

    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private static boolean isCameraGestureAvailable(Resources res) {
        boolean configSet = res.getInteger(
                com.android.internal.R.integer.config_cameraLaunchGestureSensorType) != -1;
        return configSet &&
                !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void updateDisplayRotationPreferenceDescription() {
        if (mDisplayRotationPreference == null) {
            return;
        }
        PreferenceScreen preference = mDisplayRotationPreference;
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE
                |DisplayRotation.ROTATION_270_MODE);

        boolean configEnableLockRotation = getResources().
                        getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation);
        boolean lockScreenRotationEnabled = Settings.System.getInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_ROTATION, configEnableLockRotation ? 1 : 0) != 0;

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
            if (lockScreenRotationEnabled) {
                rotationList.add(ROTATION_LOCKSCREEN);
            }
            if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_0);
            }
            if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_90);
            }
            if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_180);
            }
            if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_270);
            }
            for (int i = 0; i < rotationList.size(); i++) {
                summary.append(delim).append(rotationList.get(i));
                if ((rotationList.size() - i) > 2) {
                    delim = ", ";
                } else {
                    delim = " & ";
                }
            }
            summary.append(" " + getString(R.string.display_rotation_unit));
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Default value for wake-on-plug behavior from config.xml
        boolean wakeUpWhenPluggedOrUnpluggedConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);

        mWakeWhenPluggedOrUnplugged.setChecked(Settings.Global.getInt(getContentResolver(),
                Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                (wakeUpWhenPluggedOrUnpluggedConfig ? 1 : 0)) == 1);
        updateState();

        boolean dozeEnabled = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.DOZE_ENABLED, 1) != 0;
        if (mDozeFragement != null) {
            mDozeFragement.setSummary(dozeEnabled
                    ? R.string.summary_doze_enabled : R.string.summary_doze_disabled);
        }

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);
        updateDisplayRotationPreferenceDescription();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();

        // Update auto brightness if it is available.
        if (mAutoBrightnessPreference != null) {
            int brightnessMode = Settings.System.getInt(getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            mAutoBrightnessPreference.setChecked(brightnessMode != SCREEN_BRIGHTNESS_MODE_MANUAL);
        }

        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }

        // Update tap to wake if it is available.
        if (mTapToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, 0);
            mTapToWakePreference.setChecked(value != 0);
        }

        // Update camera gesture #1 if it is available.
        if (mCameraGesturePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), CAMERA_GESTURE_DISABLED, 0);
            mCameraGesturePreference.setChecked(value == 0);
        }
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    /**
     * Reads the current font size and sets the value in the summary text
     */
    public void readFontSizePreference(Preference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // report the current size in the summary text
        final Resources res = getResources();
        String fontDesc = FontDialogPreference.getFontSizeDescription(res, mCurConfig.fontScale);
        pref.setSummary(getString(R.string.summary_font_size, fontDesc));
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mWakeWhenPluggedOrUnplugged) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                    mWakeWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (preference == mAutoBrightnessPreference) {
            boolean auto = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mTapToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        }
        if (preference == mCameraGesturePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), CAMERA_GESTURE_DISABLED,
                    value ? 0 : 1 /* Backwards because setting is for disabling */);
        }
        if (preference == mDashboardColumns) {
            Settings.System.putInt(getContentResolver(), Settings.System.DASHBOARD_COLUMNS,
                    Integer.valueOf((String) objValue));
            mDashboardColumns.setValue(String.valueOf(objValue));
            mDashboardColumns.setSummary(mDashboardColumns.getEntry());
            return true;
        }
        if (preference == mNightModePreference) {
            try {
                final int value = Integer.parseInt((String) objValue);
                final UiModeManager uiManager = (UiModeManager) getSystemService(
                        Context.UI_MODE_SERVICE);
                uiManager.setNightMode(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isAutomaticBrightnessAvailable(context.getResources())) {
                        result.add(KEY_AUTO_BRIGHTNESS);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE_FRAGMENT);
                    }
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    if (!isTapToWakeAvailable(context.getResources())) {
                        result.add(KEY_TAP_TO_WAKE);
                    }
                    if (!isCameraGestureAvailable(context.getResources())) {
                        result.add(KEY_CAMERA_GESTURE);
                    }
                    return result;
                }
            };
}
