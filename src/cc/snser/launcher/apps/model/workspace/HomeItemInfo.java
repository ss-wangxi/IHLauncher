/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.view.View;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.apps.model.ItemInfo;
import cc.snser.launcher.iphone.model.IphoneUtils;

import com.shouxinzm.launcher.util.BitmapUtils;

/**
 * Represents an item in the launcher.
 */
public abstract class HomeItemInfo extends ItemInfo {

    public HomeItemInfo() {
    }
    
    public HomeItemInfo(HomeItemInfo src){
    	this.dbX = src.dbX;
    	this.category = src.category;
    	
    	this.id = src.id;
    	this.itemType = src.itemType;
    	this.container = src.container;
    	this.screen = src.screen;
    	this.cellX = src.cellX;
    	this.cellY = src.cellY;
    	this.spanX = src.spanX;
    	this.spanY = src.spanY;
    }

    /**
     * Write the fields of this item to the DB
     *
     * @param values
     */
    public void onAddToDatabase(ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, itemType);
        values.put(LauncherSettings.Favorites.CONTAINER, container);
        values.put(LauncherSettings.Favorites.SCREEN, screen);
        values.put(LauncherSettings.Favorites.CELLX, cellX);
        values.put(LauncherSettings.Favorites.CELLY, cellY);
        values.put(LauncherSettings.Favorites.SPANX, spanX);
        values.put(LauncherSettings.Favorites.SPANY, spanY);
        
        values.put(IphoneUtils.FavoriteExtension.CATEGORY, category);
    }

    @Override
    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + ")";
    }

    public static void writeBitmap(ContentValues values, String columnName, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = BitmapUtils.flattenBitmap(bitmap);
            values.put(columnName, data);
        } else {
            values.putNull(columnName);
        }
    }

    /**
     * bind时cellX的位置，用于跟踪是否要commit到database
     */
    public int dbX;

    /**
     * 单层使用的数据 分类的id
     * */
    public int category = -1;
    /**
     * 单层使用的数据 分类的id
     * */
    
    
    public final int getCategory() {
        return category;
    }

    public final void setCategory(int category) {
        this.category = category;
    }

    public boolean isRemovePending() {
        return false;
    }
    
    public View getHostView() {
        return null;
    }
    
    public String getHostViewName() {
        final View view = getHostView();
        if (view != null) {
            return view.getClass().getName().replace("cc.snser.launcher.widget.", "");
        } else {
            return null;
        }
    }
    
    public boolean equals(HomeItemInfo info) {
        return info != null 
                && this.id == info.id
                && this.itemType == info.itemType 
                && this.container == info.container 
                && this.screen == info.screen 
                && this.cellX == info.cellX 
                && this.cellY == info.cellY 
                && this.spanX == info.spanX 
                && this.spanY == info.spanY;
    }
    
    public HomeItemInfo cloneSelf() {
        return null;
    }

}
