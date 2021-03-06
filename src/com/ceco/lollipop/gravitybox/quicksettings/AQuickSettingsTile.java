/*
* Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
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

package com.ceco.lollipop.gravitybox.quicksettings;

import com.ceco.lollipop.gravitybox.BroadcastSubReceiver;
import com.ceco.lollipop.gravitybox.GravityBoxSettings;
import com.ceco.lollipop.gravitybox.ModQuickSettings.TileLayout;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public abstract class AQuickSettingsTile implements OnClickListener, BroadcastSubReceiver {
    protected static final String PACKAGE_NAME = "com.android.systemui";

    protected Context mContext;
    protected Context mGbContext;
    protected FrameLayout mTile;
    protected OnClickListener mOnClick;
    protected OnLongClickListener mOnLongClick;
    protected Resources mResources;
    protected Resources mGbResources;
    protected Object mStatusBar;
    protected Object mPanelBar;
    protected Object mQuickSettings;
    protected ViewGroup mContainer;
    protected boolean mSupportsHideOnChange;
    private boolean mHideOnChange;

    public AQuickSettingsTile(Context context, Context gbContext, Object statusBar, Object panelBar) {
        mContext = context;
        mGbContext = gbContext;
        mResources = mContext.getResources();
        mGbResources = mGbContext.getResources();
        mStatusBar = statusBar;
        mPanelBar = panelBar;
        mSupportsHideOnChange = true;
    }

    public void setupQuickSettingsTile(ViewGroup viewGroup, LayoutInflater inflater, 
            XSharedPreferences prefs, Object quickSettings) {
        mContainer = viewGroup;
        mQuickSettings = quickSettings;
        int layoutId = mResources.getIdentifier("quick_settings_tile", "layout", PACKAGE_NAME);
        mTile = (FrameLayout) inflater.inflate(layoutId, viewGroup, false);
        onTileCreate();
        viewGroup.addView(mTile);
        if (prefs != null) {
            onPreferenceInitialize(prefs);
        }
        updateResources();
        mTile.setOnClickListener(this);
        mTile.setOnLongClickListener(mOnLongClick);
        onTilePostCreate();
    }

    public void updateLayout(TileLayout tileLayout) {
        if (mTile != null) {
            onLayoutUpdated(tileLayout);
        }
    }

    protected abstract void onTileCreate();

    protected void onTilePostCreate() { };

    protected abstract void onLayoutUpdated(TileLayout tileLayout);

    protected abstract void updateTile();

    protected void onPreferenceInitialize(XSharedPreferences prefs) {
        mHideOnChange = prefs.getBoolean(GravityBoxSettings.PREF_KEY_QUICK_SETTINGS_HIDE_ON_CHANGE, false);
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED) &&
                intent.hasExtra(GravityBoxSettings.EXTRA_QS_HIDE_ON_CHANGE)) {
            mHideOnChange = intent.getBooleanExtra(GravityBoxSettings.EXTRA_QS_HIDE_ON_CHANGE, false);
        }
    }

    public void updateResources() {
        if (mTile != null) {
            updateTile();
        }
    }

    @Override
    public void onClick(View v) {
        if (mOnClick != null) {
            mOnClick.onClick(v);
        }
        if (mSupportsHideOnChange && mHideOnChange) {
            collapsePanels();
        }
    }

    protected void startActivity(String action){
        Intent intent = new Intent(action);
        startActivity(intent);
    }

    protected void startActivity(Intent intent) {
        try {
            XposedHelpers.callMethod(mQuickSettings, "startSettingsActivity", intent);
        } catch (Throwable t) {
            // fallback in case of troubles
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
            collapsePanels();
        }
    }

    protected void collapsePanels() {
        try {
            XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels");
        } catch (Throwable t) {
            XposedBridge.log("Error calling animateCollapsePanels: " + t.getMessage());
        }
    }
}
