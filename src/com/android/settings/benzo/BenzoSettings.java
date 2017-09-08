/*
 * Copyright (C) 2017 Benzo Rom
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

package com.android.settings.benzo;

import android.content.Context;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;

public class BenzoSettings extends DashboardFragment {
    private static final String TAG = "BenzoSettings";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.BENZO;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mProgressiveDisclosureMixin.setTileLimit(4);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.benzo_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getLifecycle());
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private static List<PreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<PreferenceController> controllers = new ArrayList<>();
        return controllers;
    }
}
