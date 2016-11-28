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

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import cc.snser.launcher.Constant;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.Utils;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.apps.model.Cellable;
import cc.snser.launcher.apps.model.DockBarItemInfo;
import cc.snser.launcher.screens.DeleteZone;
import cc.snser.launcher.screens.DeleteZone.Deletable;

import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

import java.util.List;

import static cc.snser.launcher.LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
import static cc.snser.launcher.LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;

/**
 * Represents a launchable icon on the workspaces and in folders.
 */
public class HomeDesktopItemInfo extends HomeItemInfo implements AppInfo, DockBarItemInfo, Deletable {
    /**
     * The intent used to start the application.
     */
    public Intent intent;

    public String defaultTitle;
    public FastBitmapDrawable defaultIcon;

    public int iconType;

    /**
     * Indicates whether we're using the default fallback icon instead of
     * something from the app.
     */
    public boolean usingFallbackIcon;

    public boolean isDragging;

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    public Intent.ShortcutIconResource iconResource;

    public Intent.ShortcutIconResource titleResource;

    public Bitmap shortcutIconBitmap;

    public long lastUpdateTime;

    public boolean system = false;

    public boolean syncUpdated = false;

    /**
     * 临时实现
     */
    public int mCustomType;
    public static final int CUSTOM_TYPE_UNDEF = 0;
    public static final int CUSTOM_TYPE_SHORTCUT = 1;
    public static final int CUSTOM_TYPE_WIDGET = 2;

    /**
     * 是否独一无二的
     */
    public boolean mForceUnique;

    public HomeDesktopItemInfo() {
        itemType = ITEM_TYPE_SHORTCUT;
        mForceUnique = false;
    }
    
    public HomeDesktopItemInfo(HomeDesktopItemInfo src){
    	super(src);
    	
    	this.intent = src.intent;
    	this.defaultTitle = src.defaultTitle;
    	this.defaultIcon = src.defaultIcon;
    	this.lastCalledTime = src.lastCalledTime;
    	this.lastUpdateTime = src.lastUpdateTime;
    	this.iconType = src.iconType;
    	this.shortcutIconBitmap = src.shortcutIconBitmap;
    	this.system = src.system;
    	this.syncUpdated = src.syncUpdated;
    }
    
    public HomeDesktopItemInfo(Context context, Intent intent, IconCache iconCache) {
        itemType = ITEM_TYPE_APPLICATION;

        this.intent = intent;

        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        if (apps.size() > 0) {
            ResolveInfo info = apps.get(0);
            init(context, info, iconCache);
        }
    }

    private void init(Context context, ResolveInfo info, IconCache iconCache) {
        container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        lastUpdateTime = Utils.getLastUpdateTime(info);
        if (this.intent != null) {
            iconCache.getTitleAndIcon(this, info);
        }
        storage = Utils.getApplicationStorage(info);
        system = (!Constant.DEBUG && Constant.PACKAGE_NAME.equals(info.activityInfo.applicationInfo.packageName))
                || (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    @Override
    public FastBitmapDrawable getIcon(IconCache iconCache) {
        if (defaultIcon == null) {
            defaultIcon = iconCache.getIcon(intent,itemType);
            usingFallbackIcon = iconCache.isDefaultIcon(defaultIcon == null ? null : defaultIcon.getBitmap());
        }
        return defaultIcon;
    }

    @Override
    public FastBitmapDrawable getIcon() {
        return defaultIcon;
    }

    @Override
    public void setIcon(FastBitmapDrawable icon) {
        defaultIcon = icon;
    }

    @Override
    public String getTitle() {
        return defaultTitle;
    }

    @Override
    public void setTitle(String title) {
        defaultTitle = title;
    }

    @Override
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public long getLastCalledTime() {
        return lastCalledTime;
    }

    @Override
    public int getCalledNum() {
        return calledNum;
    }

    @Override
    public int getStorage() {
        return storage;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    /**
     * Creates the application intent based on a component name and various
     * launch flags. Sets {@link #itemType} to
     * {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_APPLICATION}.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    public void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
        itemType = ITEM_TYPE_APPLICATION;
    }

    @Override
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);

        values.put(LauncherSettings.BaseLauncherColumns.TITLE, defaultTitle);

        Intent clonedIntent = null;
        if (intent != null) {
            clonedIntent = new Intent(intent);
            clonedIntent.setSourceBounds(null);//避免设置了sourceBound更改了数据库中的intent值导致数据库匹配失败
        }

        String uri = clonedIntent != null ? clonedIntent.toUri(0) : null;
        values.put(LauncherSettings.BaseLauncherColumns.INTENT, uri);

        values.put(LauncherSettings.BaseLauncherColumns.ICON_TYPE, iconType);

        if (itemType == ITEM_TYPE_SHORTCUT) {
            if (iconType == LauncherSettings.BaseLauncherColumns.ICON_TYPE_BITMAP) {
                if (shortcutIconBitmap != null) {
                    writeBitmap(values, LauncherSettings.Favorites.ICON, shortcutIconBitmap);
                } else {
                    writeBitmap(values, LauncherSettings.Favorites.ICON, defaultIcon == null ? null : defaultIcon.getBitmap());
                }
            } else {
                if (!usingFallbackIcon) {
                    if (iconResource != null && Constant.PACKAGE_NAME.equals(iconResource.packageName)) {
                        // ignore
                    } else {
                        writeBitmap(values, LauncherSettings.Favorites.ICON, defaultIcon == null ? null : defaultIcon.getBitmap());
                    }
                }
            }
        }

        if (titleResource != null) {
            values.put(LauncherSettings.BaseLauncherColumns.TITLE_PACKAGE,
                    titleResource.packageName);
            values.put(LauncherSettings.BaseLauncherColumns.TITLE_RESOURCE,
                    titleResource.resourceName);
        }

        if (iconResource != null) {
            values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE,
                    iconResource.packageName);
            values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE,
                    iconResource.resourceName);
        }
    }

    @Override
    public String toString() {
        return "HomeDesktopItemInfo(title=" + getTitle() + ", itemType=" + itemType + ")";
    }

    @Override
    public void unbind() {
        super.unbind();
    }

    @Override
    public Intent getIntent() {
        return intent;
    }


    public long getId() {
        return id;
    }

    public boolean isShortcut() {
        return itemType == ITEM_TYPE_SHORTCUT;
    }

    @Override
    public Cellable getCellable() {
        return this;
    }

    /**
     * 匹配组件部分是否相等
     * @param componentName
     * @param onlyMatchPackage
     * @return
     */
    public final boolean matchComponentName(final ComponentName componentName, boolean onlyMatchPackage) {
        if (componentName == null) {
            return false;
        }
        final ComponentName cn = getComponentName();
        if (cn == null) {
            return false;
        }

        if (onlyMatchPackage) {
            if (!cn.getPackageName().equals(componentName.getPackageName())) {
                return false;
            }
            return true;
        }

        if (!cn.equals(componentName)) {
            return false;
        }
        return true;
    }

    //给单层板使用
    public int storage;
    public long lastCalledTime;
    public int calledNum;

    /**
     * 获取Intent原始的ComponentName
     * @return
     */
    public final ComponentName getComponentName() {
        return intent != null ? intent.getComponent() : null;
    }

    public final void updateInvoke() {
        calledNum++;
        lastCalledTime = System.currentTimeMillis();
    }

    @Override
    public boolean isDeletable(Context context) {
        return true;
    }

    @Override
    public String getLabel(Context context) {
    	//设置不允许删除
    	if(intent != null && intent.getAction()!=null &&
    			intent.getAction().compareToIgnoreCase(Constant.LAUNCHER_CUSTOM_SHORTCUT_ACTION) == 0){
    		return "";//context.getString(R.string.global_hide);
    	}
        if (isShortcut() ) {
            return context.getString(R.string.global_delete);
        } else if (!system) {
            return context.getString(R.string.global_uninstall);
        } else {
            return context.getString(R.string.global_hide);
        }
    }

    @Override
    public int getDeleteZoneIcon(Context context) {
        if (isShortcut()) {
            return 0;
        } else if (!system) {
            return DeleteZone.DELETE_DELETE;
        } else {
            return DeleteZone.DELETE_HIDDEN;
        }
    }

    @Override
    public boolean onDelete(Context context) {
        return false;
    }

    public void mergeAdComponentFrom(HomeDesktopItemInfo homeDesktopItemInfo) {
        this.category = homeDesktopItemInfo.category;
        this.itemType = homeDesktopItemInfo.itemType;
        this.defaultIcon = homeDesktopItemInfo.defaultIcon;
        this.defaultTitle = homeDesktopItemInfo.defaultTitle;
        this.iconResource = homeDesktopItemInfo.iconResource;
        this.iconType = homeDesktopItemInfo.iconType;
        this.intent = homeDesktopItemInfo.intent;
        this.storage = homeDesktopItemInfo.storage;
        this.system = homeDesktopItemInfo.system;
        this.lastUpdateTime = homeDesktopItemInfo.lastUpdateTime;
        this.titleResource = homeDesktopItemInfo.titleResource;
        this.usingFallbackIcon = homeDesktopItemInfo.usingFallbackIcon;
    }
    
    @Override
    public HomeDesktopItemInfo cloneSelf() {
        return new HomeDesktopItemInfo(this);
    }
}
