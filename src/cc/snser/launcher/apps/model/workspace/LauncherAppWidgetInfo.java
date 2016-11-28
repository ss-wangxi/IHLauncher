/*
 * Copyright (C) 2009 The Android Open Source Project
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

package cc.snser.launcher.apps.model.workspace;

import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.screens.DeleteZone.Deletable;
import cc.snser.launcher.widget.LauncherAppWidgetHostView;

import com.btime.launcher.R;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;

/**
 * 系统Widget
 * Represents a widget, which just contains an identifier.
 */
public class LauncherAppWidgetInfo extends HomeItemInfo implements Deletable {

    /**
     * Identifier for this widget when talking with
     * {@link android.appwidget.AppWidgetManager} for updates.
     */
    public int appWidgetId;
    
    public ComponentName appWidgetCn;

    /**
     * View that holds this widget after it's been created.  This view isn't created
     * until Launcher knows it's needed.
     */
    private LauncherAppWidgetHostView mHostView = null;
    
    public LauncherAppWidgetInfo(int appWidgetId) {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
        this.appWidgetId = appWidgetId;
    }
    
    public LauncherAppWidgetInfo(LauncherAppWidgetInfo src) {
        super(src);
        
        this.appWidgetId = src.appWidgetId;
        this.appWidgetCn = src.appWidgetCn.clone();
        this.mHostView = src.mHostView;
    }

    @Override
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.APPWIDGET_ID, appWidgetId);
    }

    @Override
    public String toString() {
        return "AppWidget(id=" + Integer.toString(appWidgetId) + ")";
    }

    @Override
    public void unbind() {
        super.unbind();
        mHostView = null;
    }
    
    @Override
    public LauncherAppWidgetHostView getHostView() {
        return mHostView;
    }
    
    public void setHostView(LauncherAppWidgetHostView hostView) {
        mHostView = hostView;
    }
    
    public void clearHostView() {
        if (mHostView != null) {
            mHostView.onRemoved();
            mHostView = null;
        }
    }

    public boolean acceptByFolder() {
        return false;
    }

    public boolean acceptByDockbar() {
        return false;
    }

    @Override
    public boolean isDeletable(Context context) {
        return true;
    }

    @Override
    public String getLabel(Context context) {
        return context.getString(R.string.global_delete);
    }

    @Override
    public int getDeleteZoneIcon(Context context) {
        return 0;
    }

    @Override
    public boolean onDelete(Context context) {
        return false;
    }
    
    @Override
    public LauncherAppWidgetInfo cloneSelf() {
        return new LauncherAppWidgetInfo(this);
    }
    
}
