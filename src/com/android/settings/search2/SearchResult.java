/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.search2;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Dataclass as an interface for all Search Results.
 */
public class SearchResult implements Comparable<SearchResult> {
    @Override
    public int compareTo(SearchResult searchResult) {
        if (searchResult == null) {
            return -1;
        }
        return this.rank - searchResult.rank;
    }

    public static class Builder {
        protected String mTitle;
        protected String mSummary;
        protected ArrayList<String> mBreadcrumbs;
        protected int mRank = -1;
        protected ResultPayload mResultPayload;
        protected Drawable mIcon;

        public Builder addTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder addSummary(String summary) {
            mSummary = summary;
            return this;
        }

        public Builder addBreadcrumbs(ArrayList<String> breadcrumbs) {
            mBreadcrumbs = breadcrumbs;
            return this;
        }

        public Builder addRank(int rank) {
            if (rank < 0 || rank > 9) {
                rank = 42;
            }
            mRank = rank;
            return this;
        }

        public Builder addIcon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        public Builder addPayload(ResultPayload payload) {
            mResultPayload = payload;
            return this;
        }

        public SearchResult build() {
            // Check that all of the mandatory fields are set.
            if (mTitle == null) {
                throw new IllegalArgumentException("SearchResult missing title argument");
            } else if (mSummary == null ) {
                throw new IllegalArgumentException("SearchResult missing summary argument");
            } else if (mBreadcrumbs == null){
                throw new IllegalArgumentException("SearchResult missing breadcrumbs argument");
            } else if (mRank == -1) {
                throw new IllegalArgumentException("SearchResult missing rank argument");
            } else if (mIcon == null) {
                throw new IllegalArgumentException("SearchResult missing icon argument");
            } else if (mResultPayload == null) {
                throw new IllegalArgumentException("SearchResult missing Payload argument");
            }
            return new SearchResult(this);
        }
    }

    /**
     * The title of the result and main text displayed.
     * Intent Results: Displays as the primary
     */
    public final String title;

    /**
     * Summary / subtitle text
     * Intent Results: Displays the text underneath the title
     */
    final public String summary;

    /**
     * An ordered list of the information hierarchy.
     * Intent Results: Displayed a hierarchy of selections to reach the setting from the home screen
     */
    public final ArrayList<String> breadcrumbs;

    /**
     * A suggestion for the ranking of the result.
     * Based on Settings Rank:
     * 1 is a near perfect match
     * 9 is the weakest match
     * TODO subject to change
     */
    public final int rank;

    /**
     * Identifier for the recycler view adapter.
     */
    @ResultPayload.PayloadType public final int viewType;

    /**
     * Metadata for the specific result types.
     */
    public final ResultPayload payload;

    /**
     * Result's icon.
     */
    public final Drawable icon;

    private SearchResult(Builder builder) {
        title = builder.mTitle;
        summary = builder.mSummary;
        breadcrumbs = builder.mBreadcrumbs;
        rank = builder.mRank;
        icon = builder.mIcon;
        payload = builder.mResultPayload;
        viewType = payload.getType();
    }
}