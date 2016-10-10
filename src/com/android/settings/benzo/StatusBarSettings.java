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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.EditText;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.preference.SystemSettingSwitchPreference;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBarSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
    private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
    private static final String STATUS_BAR_CLOCK_DATE_DISPLAY = "clock_date_display";
    private static final String STATUS_BAR_CLOCK_DATE_STYLE = "clock_date_style";
    private static final String STATUS_BAR_CLOCK_DATE_FORMAT = "clock_date_format";
    private static final String PREF_CLOCK_DATE_POSITION = "clock_date_position";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";

    private static final String PREF_BATT_BAR = "battery_bar_list";
    private static final String PREF_BATT_BAR_STYLE = "battery_bar_style";
    private static final String PREF_BATT_BAR_WIDTH = "battery_bar_thickness";
    private static final String PREF_BATT_ANIMATE = "battery_bar_animate";

    public static final int CLOCK_DATE_STYLE_LOWERCASE = 1;
    public static final int CLOCK_DATE_STYLE_UPPERCASE = 2;
    private static final int CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18;

    private static final String STATUSBAR_BATTERY_STYLE = "statusbar_battery_style";
    private static final String STATUSBAR_BATTERY_PERCENT = "statusbar_battery_percent";
    private static final String STATUSBAR_CHARGING_COLOR = "statusbar_battery_charging_color";
    private static final String STATUSBAR_BATTERY_PERCENT_INSIDE = "statusbar_battery_percent_inside";
    private static final String STATUSBAR_BATTERY_SHOW_BOLT = "statusbar_battery_charging_image";
    private static final String STATUSBAR_BATTERY_ENABLE = "statusbar_battery_enable";
    private static final String STATUSBAR_SHOW_CHARGING = "statusbar_battery_charging_color_enable";
    private static final String STATUSBAR_CATEGORY_CHARGING = "statusbar_category_charging";

    private static final String STATUS_BAR_BENZO_LOGO_SHOW = "status_bar_benzo_logo_show";
    private static final String STATUS_BAR_BENZO_LOGO_SHOW_ON_LOCK_SCREEN = "status_bar_benzo_logo_show_on_lock_screen";
    private static final String KEY_BENZO_LOGO_STYLE = "status_bar_benzo_logo_style";
    private static final String KEY_BENZO_LOGO_COLOR = "status_bar_benzo_logo_color";
    private static final String STATUS_BAR_BENZO_LOGO_COLOR_DARK_MODE = "status_bar_benzo_logo_color_dark_mode";
    private static final String PREF_NUMBER_OF_NOTIFICATION_ICONS = "logo_number_of_notification_icons";
    private static final String PREF_HIDE_LOGO = "logo_hide_logo";

    private ListPreference mStatusBarClock;
    private ListPreference mStatusBarAmPm;
    private ListPreference mClockDateDisplay;
    private ListPreference mClockDateStyle;
    private ListPreference mClockDateFormat;
    private ListPreference mClockDatePosition;
    private SwitchPreference mStatusBarBrightnessControl;

    private ListPreference mBatteryBar;
    private ListPreference mBatteryBarStyle;
    private ListPreference mBatteryBarThickness;
    private SwitchPreference mBatteryBarChargingAnimation;

    private ListPreference mBatteryStyle;
    private ListPreference mBatteryPercent;
    private ColorPickerPreference mChargingColor;
    private SwitchPreference mPercentInside;
    private SwitchPreference mChargingShow;
    private SwitchPreference mShowBolt;
    private int mShowPercent;
    private int mBatteryStyleValue;
    private ListPreference mBatteryEnable;
    private int mShowBattery = 1;

    private SwitchPreference mShowLogo;
    private SwitchPreference mShowLogoKeyguard;
    private ListPreference mLogoStyle;
    private ColorPickerPreference mLogoColor;
    private ColorPickerPreference mLogoColorDarkMode;
    private SwitchPreference mHideLogo;
    private ListPreference mNumberOfNotificationIcons;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.BENZO;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.statusbar_settings);
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        int intColor;
        String hexColor;

        mBatteryBar = (ListPreference) findPreference(PREF_BATT_BAR);
        mBatteryBar.setOnPreferenceChangeListener(this);
        mBatteryBar.setValue((Settings.System.getInt(resolver,
                        Settings.System.STATUSBAR_BATTERY_BAR, 0)) + "");

        mBatteryBarStyle = (ListPreference) findPreference(PREF_BATT_BAR_STYLE);
        mBatteryBarStyle.setOnPreferenceChangeListener(this);
        mBatteryBarStyle.setValue((Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_STYLE, 0)) + "");

        mBatteryBarChargingAnimation = (SwitchPreference) findPreference(PREF_BATT_ANIMATE);
        mBatteryBarChargingAnimation.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE, 0) == 1);

        mBatteryBarThickness = (ListPreference) findPreference(PREF_BATT_BAR_WIDTH);
        mBatteryBarThickness.setOnPreferenceChangeListener(this);
        mBatteryBarThickness.setValue((Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, 1)) + "");

        mStatusBarClock = (ListPreference) findPreference(STATUS_BAR_CLOCK_STYLE);
        mStatusBarAmPm = (ListPreference) findPreference(STATUS_BAR_AM_PM);
        mClockDateDisplay = (ListPreference) findPreference(STATUS_BAR_CLOCK_DATE_DISPLAY);
        mClockDateStyle = (ListPreference) findPreference(STATUS_BAR_CLOCK_DATE_STYLE);

        int clockStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CLOCK, 1);
        mStatusBarClock.setValue(String.valueOf(clockStyle));
        mStatusBarClock.setSummary(mStatusBarClock.getEntry());
        mStatusBarClock.setOnPreferenceChangeListener(this);

        if (DateFormat.is24HourFormat(getActivity())) {
            mStatusBarAmPm.setEnabled(false);
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
        } else {
            int statusBarAmPm = Settings.System.getInt(resolver,
                    Settings.System.STATUS_BAR_AM_PM, 2);
            mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
            mStatusBarAmPm.setOnPreferenceChangeListener(this);
        }

        int clockDateDisplay = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, 0);
        mClockDateDisplay.setValue(String.valueOf(clockDateDisplay));
        mClockDateDisplay.setSummary(mClockDateDisplay.getEntry());
        mClockDateDisplay.setOnPreferenceChangeListener(this);

        int clockDateStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0);
        mClockDateStyle.setValue(String.valueOf(clockDateStyle));
        mClockDateStyle.setSummary(mClockDateStyle.getEntry());
        mClockDateStyle.setOnPreferenceChangeListener(this);

        mClockDateFormat = (ListPreference) findPreference(STATUS_BAR_CLOCK_DATE_FORMAT);
        mClockDateFormat.setOnPreferenceChangeListener(this);
        if (mClockDateFormat.getValue() == null) {
            mClockDateFormat.setValue("EEE");
        }

        mClockDatePosition = (ListPreference) findPreference(PREF_CLOCK_DATE_POSITION);
        mClockDatePosition.setOnPreferenceChangeListener(this);
        mClockDatePosition.setValue(Integer.toString(Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_CLOCK_DATE_POSITION, 0)));
        mClockDatePosition.setSummary(mClockDatePosition.getEntry());

        mStatusBarBrightnessControl = (SwitchPreference) findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBrightnessControl.setOnPreferenceChangeListener(this);
        int statusBarBrightnessControl = Settings.System.getInt(getContentResolver(),
                STATUS_BAR_BRIGHTNESS_CONTROL, 0);
        mStatusBarBrightnessControl.setChecked(statusBarBrightnessControl != 0);
        try {
            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_brightness_control_info);
            }
        } catch (SettingNotFoundException e) {
        }

        mBatteryStyle = (ListPreference) findPreference(STATUSBAR_BATTERY_STYLE);
        mBatteryStyleValue = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_STYLE, 0);

        mBatteryStyle.setValue(Integer.toString(mBatteryStyleValue));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercent = (ListPreference) findPreference(STATUSBAR_BATTERY_PERCENT);
        mShowPercent = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_PERCENT, 2);

        mBatteryPercent.setValue(Integer.toString(mShowPercent));
        mBatteryPercent.setSummary(mBatteryPercent.getEntry());
        mBatteryPercent.setOnPreferenceChangeListener(this);

        mChargingColor = (ColorPickerPreference) findPreference(STATUSBAR_CHARGING_COLOR);
        mChargingColor.setOnPreferenceChangeListener(this);

        mPercentInside = (SwitchPreference) findPreference(STATUSBAR_BATTERY_PERCENT_INSIDE);
        mChargingShow = (SwitchPreference) findPreference(STATUSBAR_SHOW_CHARGING);
        mShowBolt = (SwitchPreference) findPreference(STATUSBAR_BATTERY_SHOW_BOLT);

        mBatteryEnable = (ListPreference) findPreference(STATUSBAR_BATTERY_ENABLE);
        mShowBattery = Settings.System.getInt(resolver,
                Settings.System.STATUSBAR_BATTERY_ENABLE, 1);

        mBatteryEnable.setValue(Integer.toString(mShowBattery));
        mBatteryEnable.setSummary(mBatteryEnable.getEntry());
        mBatteryEnable.setOnPreferenceChangeListener(this);

        parseClockDateFormats();
        updateEnablement();

        mShowLogo =
                (SwitchPreference) findPreference(STATUS_BAR_BENZO_LOGO_SHOW);
        mShowLogo.setChecked(Settings.System.getInt(resolver,
                "status_bar_benzo_logo_show", 0) == 1);
        mShowLogo.setOnPreferenceChangeListener(this);

        mShowLogoKeyguard =
                (SwitchPreference) findPreference(STATUS_BAR_BENZO_LOGO_SHOW_ON_LOCK_SCREEN);
        mShowLogoKeyguard.setChecked(Settings.System.getInt(resolver,
                "status_bar_benzo_logo_show_on_lock_screen", 0) == 1);
        mShowLogoKeyguard.setOnPreferenceChangeListener(this);

        mLogoStyle = (ListPreference) findPreference(KEY_BENZO_LOGO_STYLE);
        int LogoStyle = Settings.System.getInt(resolver,
                "status_bar_benzo_logo_style", 1);
        mLogoStyle.setValue(String.valueOf(LogoStyle));
        mLogoStyle.setSummary(mLogoStyle.getEntry());
        mLogoStyle.setOnPreferenceChangeListener(this);

        mHideLogo =
                (SwitchPreference) findPreference(PREF_HIDE_LOGO);
        mHideLogo.setChecked(Settings.System.getInt(resolver,
               "status_bar_benzo_logo_hide_logo", 1) == 1);
        mHideLogo.setOnPreferenceChangeListener(this);

        mNumberOfNotificationIcons =
                (ListPreference) findPreference(PREF_NUMBER_OF_NOTIFICATION_ICONS);
        int numberOfNotificationIcons = Settings.System.getInt(resolver,
               "status_bar_benzo_logo_number_of_notification_icons", 4);
        mNumberOfNotificationIcons.setValue(String.valueOf(numberOfNotificationIcons));
        mNumberOfNotificationIcons.setSummary(mNumberOfNotificationIcons.getEntry());
        mNumberOfNotificationIcons.setOnPreferenceChangeListener(this);

        // logo color
        mLogoColor =
            (ColorPickerPreference) findPreference(KEY_BENZO_LOGO_COLOR);
        mLogoColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(resolver,
                "status_bar_benzo_logo_color", 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLogoColor.setSummary(hexColor);
        mLogoColor.setNewPreviewColor(intColor);
        mLogoColor.setAlphaSliderVisible(true);

        // logo color dark mode
        mLogoColorDarkMode =
                (ColorPickerPreference) findPreference(STATUS_BAR_BENZO_LOGO_COLOR_DARK_MODE);
        mLogoColorDarkMode.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(resolver,
                "status_bar_benzo_logo_color_dark_mode", 0xffffffff);
        hexColor = String.format("#%08x", (0xffffffff & intColor));
        mLogoColorDarkMode.setSummary(hexColor);
        mLogoColorDarkMode.setNewPreviewColor(intColor);
        mLogoColorDarkMode.setAlphaSliderVisible(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Configuration config = getResources().getConfiguration();
        if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                mStatusBarClock.setEntries(getActivity().getResources().getStringArray(
                        R.array.status_bar_clock_style_entries_rtl));
                mStatusBarClock.setSummary(mStatusBarClock.getEntry());
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        AlertDialog dialog;
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mBatteryBar) {
            int val = Integer.parseInt((String) newValue);
            return Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_BAR, val);
        } else if (preference == mBatteryBarStyle) {
            int val = Integer.parseInt((String) newValue);
            return Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_BAR_STYLE, val);
        } else if (preference == mBatteryBarThickness) {
            int val = Integer.parseInt((String) newValue);
            return Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_BAR_THICKNESS, val);
        } else if (preference == mStatusBarBrightnessControl) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(
                     resolver, STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mBatteryStyle) {
            mBatteryStyleValue = Integer.valueOf((String) newValue);
            int index = mBatteryStyle.findIndexOfValue((String) newValue);
            mBatteryStyle.setSummary(
                    mBatteryStyle.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_STYLE, mBatteryStyleValue);
            updateEnablement();
            return true;
        } else if (preference == mBatteryPercent) {
            mShowPercent = Integer.valueOf((String) newValue);
            int index = mBatteryPercent.findIndexOfValue((String) newValue);
            mBatteryPercent.setSummary(
                    mBatteryPercent.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_PERCENT, mShowPercent);
            updateEnablement();
            return true;
        } else if (preference == mChargingColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer
                    .valueOf(String.valueOf(newValue)));
            mChargingColor.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_CHARGING_COLOR, intHex);
            return true;
        } else if (preference == mBatteryEnable) {
            mShowBattery = Integer.valueOf((String) newValue);
            int index = mBatteryEnable.findIndexOfValue((String) newValue);
            mBatteryEnable.setSummary(
                    mBatteryEnable.getEntries()[index]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUSBAR_BATTERY_ENABLE, mShowBattery);
            updateEnablement();
            return true;
        } else if (preference == mStatusBarClock) {
            int clockStyle = Integer.parseInt((String) newValue);
            int index = mStatusBarClock.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_CLOCK, clockStyle);
            mStatusBarClock.setSummary(mStatusBarClock.getEntries()[index]);
            if (clockStyle == 0) {
                mClockDateDisplay.setEnabled(false);
            } else {
                mClockDateDisplay.setEnabled(true);
            }
            return true;
        } else if (preference == mStatusBarAmPm) {
            int statusBarAmPm = Integer.valueOf((String) newValue);
            int index = mStatusBarAmPm.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, Settings.System.STATUS_BAR_AM_PM, statusBarAmPm);
            mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
            return true;
        } else if (preference == mClockDateDisplay) {
            int clockDateDisplay = Integer.valueOf((String) newValue);
            int index = mClockDateDisplay.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, clockDateDisplay);
            mClockDateDisplay.setSummary(mClockDateDisplay.getEntries()[index]);
            if (clockDateDisplay == 0) {
                mClockDateStyle.setEnabled(false);
                mClockDateFormat.setEnabled(false);
            } else {
                mClockDateStyle.setEnabled(true);
                mClockDateFormat.setEnabled(true);
            }
            return true;
        } else if (preference == mClockDateStyle) {
            int clockDateStyle = Integer.valueOf((String) newValue);
            int index = mClockDateStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, clockDateStyle);
            mClockDateStyle.setSummary(mClockDateStyle.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mClockDatePosition) {
            int val = Integer.parseInt((String) newValue);
            int index = mClockDatePosition.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_CLOCK_DATE_POSITION, val);
            mClockDatePosition.setSummary(mClockDatePosition.getEntries()[index]);
            parseClockDateFormats();
            return true;
        } else if (preference == mShowLogo) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    "status_bar_benzo_logo_show",
                    value ? 1 : 0);
            return true;
        } else if (preference == mShowLogoKeyguard) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    "status_bar_benzo_logo_show_on_lock_screen",
                    value ? 1 : 0);
            return true;
        } else if (preference == mLogoStyle) {
            int LogoStyle = Integer.valueOf((String) newValue);
            int index = mLogoStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(
                    resolver, "status_bar_benzo_logo_style", LogoStyle);
            mLogoStyle.setSummary(mLogoStyle.getEntries()[index]);
            return true;
        } else if (preference == mHideLogo) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver,
                    "status_bar_benzo_logo_hide_logo",
                    value ? 1 : 0);
            return true;
        } else if (preference == mNumberOfNotificationIcons) {
            int intValue = Integer.valueOf((String) newValue);
            int index = mNumberOfNotificationIcons.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver,
                    "status_bar_benzo_logo_number_of_notification_icons",
                    intValue);
            preference.setSummary(mNumberOfNotificationIcons.getEntries()[index]);
            return true;
        } else if (preference == mLogoColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    "status_bar_benzo_logo_color", intHex);
            return true;
        } else if (preference == mLogoColorDarkMode) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(resolver,
                    "status_bar_benzo_logo_color_dark_mode", intHex);
            return true;
        } else if (preference == mClockDateFormat) {
            int index = mClockDateFormat.findIndexOfValue((String) newValue);

            if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setTitle(R.string.clock_date_string_edittext_title);
                alert.setMessage(R.string.clock_date_string_edittext_summary);

                final EditText input = new EditText(getActivity());
                String oldText = Settings.System.getString(
                    getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT);
                if (oldText != null) {
                    input.setText(oldText);
                }
                alert.setView(input);

                alert.setPositiveButton(R.string.menu_save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals("")) {
                            return;
                        }
                        Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, value);

                        return;
                    }
                });

                alert.setNegativeButton(R.string.menu_cancel,
                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        return;
                    }
                });
                dialog = alert.create();
                dialog.show();
            } else {
                if ((String) newValue != null) {
                    Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, (String) newValue);
                }
            }
            return true;
      }
      return false;
    }

    private void parseClockDateFormats() {
        // Parse and repopulate mClockDateFormats's entries based on current date.
        String[] dateEntries = getResources().getStringArray(R.array.clock_date_format_entries_values);
        CharSequence parsedDateEntries[];
        parsedDateEntries = new String[dateEntries.length];
        Date now = new Date();

        int lastEntry = dateEntries.length - 1;
        int dateFormat = Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0);
        for (int i = 0; i < dateEntries.length; i++) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i];
            } else {
                String newDate;
                CharSequence dateString = DateFormat.format(dateEntries[i], now);
                if (dateFormat == CLOCK_DATE_STYLE_LOWERCASE) {
                    newDate = dateString.toString().toLowerCase();
                } else if (dateFormat == CLOCK_DATE_STYLE_UPPERCASE) {
                    newDate = dateString.toString().toUpperCase();
                } else {
                    newDate = dateString.toString();
                }

                parsedDateEntries[i] = newDate;
            }
        }
        mClockDateFormat.setEntries(parsedDateEntries);
    }

    private void updateEnablement() {
        mPercentInside.setEnabled(mShowBattery != 0 && mBatteryStyleValue < 3 && mShowPercent != 0);
        mShowBolt.setEnabled(mBatteryStyleValue < 3);
        mBatteryStyle.setEnabled(mShowBattery != 0);
        mBatteryPercent.setEnabled(mShowBattery != 0);
        mChargingShow.setEnabled(mShowBattery != 0 && mBatteryStyleValue != 3);
        //mChargingCategory.setEnabled(mShowBattery != 0);
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        ContentResolver resolver = getActivity().getContentResolver();
        boolean value;

        if (preference == mBatteryBarChargingAnimation) {
            Settings.System.putInt(resolver,
                    Settings.System.STATUSBAR_BATTERY_BAR_ANIMATE,
                    ((SwitchPreference) preference).isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }
}

