/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.notificationlight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.benzo.ColorUtils;

import com.android.settings.R;
import com.android.settings.preference.SystemSettingSwitchPreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.AppSelectListPreference;
import com.android.settings.widget.AppSelectListPreference.PackageItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, NotificationLightPreference.ItemLongClickListener, Indexable {
    private static final String TAG = "NotificationLightSettings";

    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR = "notification_light_pulse_default_color";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON = "notification_light_pulse_default_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF = "notification_light_pulse_default_led_off";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_COLOR = "notification_light_pulse_call_color";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_ON = "notification_light_pulse_call_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF = "notification_light_pulse_call_led_off";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR = "notification_light_pulse_vmail_color";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON = "notification_light_pulse_vmail_led_on";
    private static final String NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF = "notification_light_pulse_vmail_led_off";
    private static final String DEFAULT_PREF = "default";
    private static final String MISSED_CALL_PREF = "missed_call";
    private static final String VOICEMAIL_PREF = "voicemail";
    public static final int ACTION_TEST = 0;
    public static final int ACTION_DELETE = 1;
    private static final int MENU_ADD = 0;
    private static final int DIALOG_APPS = 0;

    private int mDefaultColor;
    private int mDefaultLedOn;
    private int mDefaultLedOff;
    private PackageManager mPackageManager;
    private PreferenceGroup mApplicationPrefList;
    private SystemSettingSwitchPreference mEnabledPref;
    private SystemSettingSwitchPreference mScreenOnLightsPref;
    private SystemSettingSwitchPreference mCustomEnabledPref;
    private NotificationLightPreference mDefaultPref;
    private NotificationLightPreference mCallPref;
    private NotificationLightPreference mVoicemailPref;
    private Menu mMenu;
    private AppSelectListPreference mPackageAdapter;
    private String mPackageList;
    private Map<String, Package> mPackages;
    private boolean mMultiColorLed;
    private boolean mLedCanPulse;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.BENZO;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_light_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        Resources resources = getResources();

        PreferenceGroup mAdvancedPrefs = (PreferenceGroup) prefSet.findPreference("advanced_section");
        PreferenceGroup mGeneralPrefs = (PreferenceGroup) prefSet.findPreference("general_section");

        // Get the system defined default notification color
        mDefaultColor =
                resources.getColor(com.android.internal.R.color.config_defaultNotificationColor, null);

        mDefaultLedOn = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
        mDefaultLedOff = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);

        mLedCanPulse = resources.getBoolean(com.android.internal.R.bool.config_ledCanPulse);
        mMultiColorLed = resources.getBoolean(
                com.android.internal.R.bool.config_multiColorNotificationLed);

        mEnabledPref = (SystemSettingSwitchPreference)
                findPreference(Settings.System.NOTIFICATION_LIGHT_PULSE);
        mEnabledPref.setOnPreferenceChangeListener(this);

        mDefaultPref = (NotificationLightPreference) findPreference(DEFAULT_PREF);

        mScreenOnLightsPref = (SystemSettingSwitchPreference)
                findPreference(Settings.System.NOTIFICATION_LIGHT_SCREEN_ON);
        mScreenOnLightsPref.setOnPreferenceChangeListener(this);

        // Advanced light settings
        mCustomEnabledPref = (SystemSettingSwitchPreference)
                findPreference(Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE);

        if (!mLedCanPulse && !mMultiColorLed) {
            mGeneralPrefs.removePreference(mDefaultPref);
            mAdvancedPrefs.removePreference(mCustomEnabledPref);
        } else {
            mCustomEnabledPref.setOnPreferenceChangeListener(this);
            mDefaultPref.setOnPreferenceChangeListener(this);
        }

        // Missed call and Voicemail preferences should only show on devices with a voice capabilities
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE
                || (!mLedCanPulse && !mMultiColorLed)) {
            removePreference("phone_list");
        } else {
            mCallPref = (NotificationLightPreference) findPreference(MISSED_CALL_PREF);
            mCallPref.setOnPreferenceChangeListener(this);

            mVoicemailPref = (NotificationLightPreference) findPreference(VOICEMAIL_PREF);
            mVoicemailPref.setOnPreferenceChangeListener(this);
        }

        if (!mLedCanPulse && !mMultiColorLed) {
            removePreference("applications_list");
        } else {
            mApplicationPrefList = (PreferenceGroup) findPreference("applications_list");
            mApplicationPrefList.setOrderingAsAdded(false);
        }

        // Get launch-able applications
        mPackageManager = getPackageManager();
        mPackageAdapter = new AppSelectListPreference(getActivity());

        mPackages = new HashMap<String, Package>();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
        refreshCustomApplicationPrefs();
        getActivity().invalidateOptionsMenu();
    }

    private void refreshDefault() {
        ContentResolver resolver = getContentResolver();
        int color = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, mDefaultColor);
        int timeOn = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, mDefaultLedOn);
        int timeOff = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, mDefaultLedOff);

        mDefaultPref.setAllValues(color, timeOn, timeOff);

        // Get Missed call and Voicemail values
        if (mCallPref != null) {
            int callColor = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR, mDefaultColor);
            int callTimeOn = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, mDefaultLedOn);
            int callTimeOff = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, mDefaultLedOff);

            mCallPref.setAllValues(callColor, callTimeOn, callTimeOff);
        }

        if (mVoicemailPref != null) {
            int vmailColor = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, mDefaultColor);
            int vmailTimeOn = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, mDefaultLedOn);
            int vmailTimeOff = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, mDefaultLedOff);

            mVoicemailPref.setAllValues(vmailColor, vmailTimeOn, vmailTimeOff);
        }

        if (mLedCanPulse || mMultiColorLed) {
            mApplicationPrefList = (PreferenceGroup) findPreference("applications_list");
            mApplicationPrefList.setOrderingAsAdded(false);
        }
    }

    private void refreshCustomApplicationPrefs() {
        Context context = getActivity();

        if (!parsePackageList()) {
            return;
        }

        // Add the Application Preferences
        if (mApplicationPrefList != null) {
            mApplicationPrefList.removeAll();

            for (Package pkg : mPackages.values()) {
                try {
                    PackageInfo info = mPackageManager.getPackageInfo(pkg.name,
                            PackageManager.GET_META_DATA);
                    NotificationLightPreference pref =
                            new NotificationLightPreference(context, pkg.color, pkg.timeon, pkg.timeoff);

                    pref.setKey(pkg.name);
                    pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
                    pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
                    pref.setPersistent(false);
                    pref.setOnPreferenceChangeListener(this);
                    pref.setOnLongClickListener(this);

                    mApplicationPrefList.addPreference(pref);
                } catch (NameNotFoundException e) {
                    // Do nothing
                }
            }

            /* Display a pref explaining how to add apps */
            if (mApplicationPrefList.getPreferenceCount() == 0) {
                String summary = getResources().getString(
                        R.string.notification_light_no_apps_summary);
                String useCustom = getResources().getString(
                        R.string.notification_light_use_custom);
                Preference pref = new Preference(context);
                pref.setSummary(String.format(summary, useCustom));
                pref.setEnabled(false);
                mApplicationPrefList.addPreference(pref);
            }
        }
    }

    private int getInitialColorForPackage(String packageName) {
        boolean autoColor = true;
        int color = mDefaultColor;
        if (autoColor) {
            try {
                Drawable icon = mPackageManager.getApplicationIcon(packageName);
                color = ColorUtils.getIconColorFromDrawable(icon);
            } catch (NameNotFoundException e) {
                // shouldn't happen, but just return default
            }
        }
        return color;
    }

    private void addCustomApplicationPref(String packageName) {
        Package pkg = mPackages.get(packageName);
        if (pkg == null) {
            int color = getInitialColorForPackage(packageName);
            pkg = new Package(packageName, color, mDefaultLedOn, mDefaultLedOff);
            mPackages.put(packageName, pkg);
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private void removeCustomApplicationPref(String packageName) {
        if (mPackages.remove(packageName) != null) {
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {
        final String baseString = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

        if (TextUtils.equals(mPackageList, baseString)) {
            return false;
        }

        mPackageList = baseString;
        mPackages.clear();

        if (baseString != null) {
            final String[] array = TextUtils.split(baseString, "\\|");
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                Package pkg = Package.fromString(item);
                if (pkg != null) {
                    mPackages.put(pkg.name, pkg);
                }
            }
        }

        return true;
    }

    private void savePackageList(boolean preferencesUpdated) {
        List<String> settings = new ArrayList<String>();
        for (Package app : mPackages.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            mPackageList = value;
        }
        Settings.System.putString(getContentResolver(),
                                  Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, value);
    }

    /**
     * Updates the default or package specific notification settings.
     *
     * @param packageName Package name of application specific settings to update
     * @param color
     * @param timeon
     * @param timeoff
     */
    protected void updateValues(String packageName, Integer color, Integer timeon, Integer timeoff) {
        ContentResolver resolver = getContentResolver();

        if (packageName.equals(DEFAULT_PREF)) {
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, color);
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, timeon);
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (packageName.equals(MISSED_CALL_PREF)) {
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR, color);
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, timeon);
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (packageName.equals(VOICEMAIL_PREF)) {
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, color);
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, timeon);
            Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, timeoff);
            refreshDefault();
            return;
        }

        // Find the custom package and sets its new values
        Package app = mPackages.get(packageName);
        if (app != null) {
            app.color = color;
            app.timeon = timeon;
            app.timeoff = timeoff;
            savePackageList(true);
        }
    }

    protected void resetColors() {
        ContentResolver resolver = getContentResolver();

        // Reset to the framework default colors
        Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, mDefaultColor);
        Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR, mDefaultColor);
        Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, mDefaultColor);

        refreshDefault();
    }

    public boolean onItemLongClick(final String key) {
        if (mApplicationPrefList.findPreference(key) == null) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeCustomApplicationPref(key);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mEnabledPref || preference == mCustomEnabledPref ||
            preference == mScreenOnLightsPref) {
            getActivity().invalidateOptionsMenu();
        } else {
            NotificationLightPreference lightPref = (NotificationLightPreference) preference;
            updateValues(lightPref.getKey(), lightPref.getColor(),
                    lightPref.getOnValue(), lightPref.getOffValue());
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenu = menu;
        mMenu.add(0, MENU_ADD, 0, R.string.profiles_add)
                .setIcon(R.drawable.ic_menu_add_white)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean enableAddButton = mEnabledPref.isChecked() && mCustomEnabledPref.isChecked();
        menu.findItem(MENU_ADD).setVisible(enableAddButton);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADD:
                showDialog(DIALOG_APPS);
                return true;
        }
        return false;
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                final ListView list = new ListView(getActivity());
                list.setAdapter(mPackageAdapter);

                builder.setTitle(R.string.profile_choose_app);
                builder.setView(list);
                dialog = builder.create();

                list.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Add empty application definition, the user will be able to edit it later
                        PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                        addCustomApplicationPref(info.packageName);
                        dialog.cancel();
                    }
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    @Override
    public int getDialogMetricsCategory(int id) {
        if (id == DIALOG_APPS) {
            return MetricsEvent.BENZO;
        }
        return 0;
    }

    /**
     * Application class
     */
    private static class Package {
        public String name;
        public Integer color;
        public Integer timeon;
        public Integer timeoff;

        /**
         * Stores all the application values in one call
         * @param name
         * @param color
         * @param timeon
         * @param timeoff
         */
        public Package(String name, Integer color, Integer timeon, Integer timeoff) {
            this.name = name;
            this.color = color;
            this.timeon = timeon;
            this.timeoff = timeoff;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append("=");
            builder.append(color);
            builder.append(";");
            builder.append(timeon);
            builder.append(";");
            builder.append(timeoff);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            String[] app = value.split("=", -1);
            if (app.length != 2)
                return null;

            String[] values = app[1].split(";", -1);
            if (values.length != 3)
                return null;

            try {
                Package item = new Package(app[0], Integer.parseInt(values[0]), Integer
                        .parseInt(values[1]), Integer.parseInt(values[2]));
                return item;
            } catch (NumberFormatException e) {
                return null;
            }
        }

    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.notification_light_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
