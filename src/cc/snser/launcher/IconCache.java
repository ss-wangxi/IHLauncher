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

package cc.snser.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import cc.snser.launcher.apps.model.AppInfo;
import cc.snser.launcher.component.themes.iconbg.model.local.IconBg;
import cc.snser.launcher.ui.utils.IconMetrics;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;
import cc.snser.launcher.util.ResourceUtils;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;
import com.shouxinzm.launcher.util.ArrayMap;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {

	public static final String EXTRA_ACTION_TYPE = "extra_action_type";
	public static final String THEME_ICON_RES_NAME = "app_cc_snser_launcher_theme";

    public static final String THEME_STORE_ICON_RES_NAME = "app_cc_snser_launcher_theme_store";

    private static final String TAG = "Launcher.Model.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static final int TRUNSLUCENT_PAINT_ALPHA = 200;
    
    private Map<String, String> mPkgNameSwitch = new HashMap<String, String>();

    private static class CacheEntry {
        public FastBitmapDrawable icon;
        public String title;
    }

    private final Context mContext;

    private final PackageManager mPackageManager;

    private final ArrayMap<ComponentName, CacheEntry> mCache =
            new ArrayMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);

    private final ArrayList<Bitmap> mCachesToRecycle = new ArrayList<Bitmap>();

    private Set<String> mIgnoreThemeComponents = new HashSet<String>();

    private Bitmap mDefaultIcon;
    private Bitmap mIconInLoading;
    private Bitmap mFolderIconBg;

    private boolean mIconBgInitialized;
    private Drawable mBackDrawable;
    private Drawable mIconMaskDrawable;
    private Drawable mFrontDrawable;

    private Paint mSolidPaint;
    private Paint mTranslucentPaint;

    private static IconCache mInstance = null;
    private static IconCache mInstanceSecondLayer = null;
    private static Object mSync = new Object();

    private IconCache(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.Settings$TetherSettingsActivity");//Bug 205793, Uniscope_U1203 设置和网络共享图标
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.MiuiPasswordGuardActivity");//Bug 188779, miui 设置和密码
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.VirusScanActivity");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.wifi.WifiApSettings");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.wifi.WifiApInfoService");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.TetherSettings");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.profilemode.AudioProfileSettings");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.Settings$WifiSettingsActivity");//Bug 205304, u795 设置和网络共享图标
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.wifi.MobileApSettings");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.wifi.WifiSettings");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.wifi.tethersettings");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.Settings$ProfileSettingsActivity");
        mIgnoreThemeComponents.add("com.android.settings/com.android.settings.wfd.WifiDisplaySettingsActivity");
        mIgnoreThemeComponents.add("com.android.deskclock/com.android.deskclock.AlarmClock");//Bug 200799, Sony ST18i时钟和闹钟
        mIgnoreThemeComponents.add("com.android.deskclock/com.android.deskclock.DeskClockTabActivity");
        mIgnoreThemeComponents.add("com.android.deskclock/com.android.deskclock.AlarmsMainActivity");
        mIgnoreThemeComponents.add("com.android.deskclock/com.android.deskclock.DeskClock");
        mIgnoreThemeComponents.add("com.google.android.deskclock/com.android.deskclock.DeskClock");
        mIgnoreThemeComponents.add("com.sec.android.app.clockpackage/com.sec.android.app.clockpackage.ClockPackage");
        
        
        mPkgNameSwitch.put("com.google.android.contacts", "com.android.contacts");
        mPkgNameSwitch.put("com.google.android.email", "com.android.email");
        mPkgNameSwitch.put("com.htc.android.mail", "com.android.email");
        mPkgNameSwitch.put("com.lenovo.calendar", "com.android.calendar");
        mPkgNameSwitch.put("com.lenovo.ideafriend", "com.android.contacts");
    }

    public static IconCache getInstance(Context ctx) {
        synchronized (mSync) {
            if (mInstance == null) {
                mInstance = new IconCache(ctx.getApplicationContext());
            }
        }
        return mInstance;
    }
    
    public static IconCache getInstanceSecondLayer(Context ctx) {
        synchronized (mSync) {
            if (mInstanceSecondLayer == null) {
            	mInstanceSecondLayer = new IconCache(ctx.getApplicationContext());
            }
        }
        return mInstanceSecondLayer;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    public Bitmap getIconInLoading() {
        if (mIconInLoading == null) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "Init mIconInLoading");
            }
            Drawable drawable = Utils.getDrawableFromResources(mContext, R.drawable.icon_in_loading);
            mIconInLoading = WorkspaceIconUtils.createIconBitmap(drawable, mContext, false, false);
        }
        return mIconInLoading;
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "flush");
        }
        synchronized (mCache) {
            for (CacheEntry entry : mCache.values()) {
                if (entry.icon != null) {
                    mCachesToRecycle.add(entry.icon.getBitmap());
                }
            }
            mCache.clear();

            mCachesToRecycle.add(mDefaultIcon);
            mDefaultIcon = null;

            mCachesToRecycle.add(mIconInLoading);
            mIconInLoading = null;

//            mCachesToRecycle.add(mFolderIconBg);
            mFolderIconBg = null;


            IconMetrics.flush();

            mIconBgInitialized = false;
        }
    }

    public void recycle() {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "recycle");
        }
        synchronized (mCache) {
        	
        	for (Bitmap bitmap : mCachesToRecycle) {
                BitmapUtils.recycleBitmap(bitmap);
            }
            mCachesToRecycle.clear();
        }
    }

    public Drawable getBackDrawable() {
        initIconBgResources();
        return this.mBackDrawable;
    }

    public Drawable getIconMaskDrawable() {
        initIconBgResources();
        return this.mIconMaskDrawable;
    }

    public Drawable getFrontDrawable() {
        initIconBgResources();
        return this.mFrontDrawable;
    }

    private void initIconBgResources() {
        if (!mIconBgInitialized) {
            mBackDrawable = null;
            mIconMaskDrawable = null;
            mFrontDrawable = null;

            final Context context = mContext;

            if (IconBg.hasSetted(context)) {
                // no bg
            } else {
                mBackDrawable = Utilities.getDrawableDefault(context, "icon_bg", true);
            } 

            mIconBgInitialized = true;
        }
    }

    public Bitmap getFolderIconBg() {
        if (mFolderIconBg == null) {
            Drawable drawable = null;

            if (drawable == null) {
                drawable = Utilities.getDrawableDefault(mContext, "folder_icon_bg", true);
            }
            
            mFolderIconBg = WorkspaceIconUtils.createIconBitmap(drawable, mContext, false, false);
        }
        return mFolderIconBg;
    }

    public Paint getSolidPaint() {
        if (mSolidPaint == null) {
            mSolidPaint = new Paint();
        }
        return mSolidPaint;
    }

    /**
     * return a paint with alpha {@link #TRUNSLUCENT_PAINT_ALPHA}.
     * @return
     */
    public Paint getTrunclucentPaint() {
        if (mTranslucentPaint == null) {
            mTranslucentPaint = new Paint();
            mTranslucentPaint.setAlpha(TRUNSLUCENT_PAINT_ALPHA);
        }
        return mTranslucentPaint;
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(AppInfo application, ResolveInfo info) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.getIntent().getComponent(), info, null);

            application.setTitle(entry.title);
            application.setIcon(entry.icon);
        }
    }

    public FastBitmapDrawable getIcon(Intent intent) {
    	return getIcon(intent, -1);
    }
    
    public FastBitmapDrawable getIcon(Intent intent,int itemType) {
    	synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return new FastBitmapDrawable(getDefaultIcon());
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, null,itemType);
            return entry.icon;
        }
    }
    

    public FastBitmapDrawable getIcon(Intent intent, ResolveInfo resolveInfo) {
        return getIcon(intent.getComponent(), resolveInfo, intent.getStringExtra(EXTRA_ACTION_TYPE));
    }

    public FastBitmapDrawable getIcon(ComponentName component, ResolveInfo resolveInfo) {
        return getIcon(component, resolveInfo, null);
    }

    public FastBitmapDrawable getIcon(ComponentName component, ResolveInfo resolveInfo, String actionType) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, actionType);
            return entry.icon;
        }
    }

    public void putCachedIcon(ComponentName component, FastBitmapDrawable icon) {
        synchronized (mCache) {
            if (component == null || icon == null) {
                return;
            }

            CacheEntry entry = mCache.get(component);
            if (entry == null) {
                entry = new CacheEntry();

                mCache.put(component, entry);
            }

            entry.icon = icon;
        }
    }

    public FastBitmapDrawable getCachedIcon(ComponentName component) {
        synchronized (mCache) {
            if (component == null) {
                return null;
            }

            CacheEntry entry = mCache.get(component);
            return entry == null ? null : entry.icon;
        }
    }

    public String getTitle(Intent intent, ResolveInfo resolveInfo) {
        return getTitle(intent.getComponent(), resolveInfo, intent.getStringExtra(EXTRA_ACTION_TYPE));
    }

    public String getTitle(ComponentName component, ResolveInfo resolveInfo) {
        return getTitle(component, resolveInfo, null);
    }

    public String getTitle(ComponentName component, ResolveInfo resolveInfo, String actionType) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, actionType);
            return entry.title;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return getDefaultIcon() == icon;
    }

    public Bitmap getDefaultIcon() {
        if (mDefaultIcon == null) {
            try {
            	Drawable defaultDrawable = mContext.getResources().getDrawable(R.drawable.icon_in_loading);
            	mDefaultIcon = WorkspaceIconUtils.createIconBitmap(defaultDrawable, mContext, false, true);//mPackageManager.getDefaultActivityIcon()	
                
            } catch (Exception e) {
                XLog.e(TAG, "Failed to get the default icon.", e);
            }
        }
        return mDefaultIcon;
    }
    
    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info, String actionType,int itemType) {
    	CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);

            initTitle(entry, componentName, info, actionType);
            initIcon(entry, componentName, info, actionType,itemType);
        } else if (entry.title == null) {
            initTitle(entry, componentName, info, actionType);
        } else if (entry.icon == null) {
            initIcon(entry, componentName, info, actionType,itemType);
        }
        return entry;

    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info, String actionType) {
    	return cacheLocked(componentName, info, actionType,-1);
    }

    private void initTitle(CacheEntry entry, ComponentName componentName, ResolveInfo info, String actionType) {
        entry.title = createIconTitle(componentName, info, actionType);
    }

    private void initIcon(CacheEntry entry, ComponentName componentName, ResolveInfo info, String actionType,int itemType){
    	entry.icon = createIconBitmap(componentName, info, actionType,itemType);
    }
    
//    private void initIcon(CacheEntry entry, ComponentName componentName, ResolveInfo info, String actionType) {
//        entry.icon = createIconBitmap(componentName, info, actionType);
//    }

    private String getIconResNameInTheme(ComponentName componentName, String actionType) {
    	//红米安全中心与设置的包名一致，并且两个activity都是主activity无法区分
    	if(DeviceUtils.isHongMi() || DeviceUtils.isMiTwo()){
    		if(componentName != null && componentName.getClassName().equals("com.miui.securitycenter.Main")){
    			return null;
    		}
    	}
    	
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();

        if(packageName != null && packageName.contains("com.android.calendar")){
        	return null;
        }
        
        if (mIgnoreThemeComponents.contains(packageName + "/" + className)) {
            return null;
        }

        Map<String, String> packageNameMappings = getPackageNameMappings();

        //产品要求除加速图标、最常使用、全部应用、一键锁屏、桌面设置之外都复位到系统图标
        if (packageNameMappings.containsKey(packageName + "/" + className)) {
        	packageName = packageNameMappings.get(packageName + "/" + className);
        } else if (packageNameMappings.containsKey(packageName)) {
        	packageName = packageNameMappings.get(packageName);
        }
       
        if ("com.google.android.apps.maps".equals(packageName)) {
            if (!"com.google.android.maps.MapsActivity".equals(className)) {
                packageName = null;
            }
        }

        if (packageName != null) {
            String iconName = packageName;
            //google原生系统有些包名有*google*标签导致从本地找不到对应资源
            if(mPkgNameSwitch.containsKey(packageName)){
            	iconName = mPkgNameSwitch.get(packageName);
            }
            
            iconName = "app_" + iconName.toLowerCase().replace(".", "_");
            return iconName;
        }
        return null;
    }

    private PackageMapping mPackageMapping = null;

    public Map<String, String> getPackageNameMappings() {
        if (mPackageMapping == null) {
            mPackageMapping = new PackageMapping();
        }
        return mPackageMapping.loadPackageNameMappings(mContext);
    }

    public PackageMapping getPackageMappingsObject() {
        if (mPackageMapping == null) {
            mPackageMapping = new PackageMapping();
        }
        return mPackageMapping;
    }

    private String createIconTitle(ComponentName componentName, ResolveInfo info, String actionType) {
        String title = null;

        try {
            title = info.loadLabel(mPackageManager).toString();
        } catch (Exception e) {
            // ignore
        }
        if (title == null) {
            title = info.activityInfo.name;
        }

        return StringUtils.trimString(title);
    }

    private FastBitmapDrawable createIconBitmap(ComponentName componentName, ResolveInfo info, String actionType,int itemType){
    	 String iconName = getIconResNameInTheme(componentName, actionType);

         Drawable drawable = null;
         boolean isUsingIconFromTheme = false;
         boolean isThemeIcon = false;

         if (iconName != null) {
             if (isSpecialIconResName(iconName)) {
                 isThemeIcon = true;
             }

             drawable = Utilities.getDrawableDefault(mContext, isThemeIcon ? "icon_themes" : iconName, true);

             if (drawable != null) {
            	 isUsingIconFromTheme = true;
             }
             
         }
         
         if (drawable == null) {
             drawable = ResourceUtils.loadDrawable(mContext, mPackageManager, info.activityInfo, false);
         }

         if (drawable == null) {
             return null;
         }

         boolean addBoard = false;
         if (!isUsingIconFromTheme) {
             if (!isThemeIcon) {
                 addBoard = true;
             }
         }
         
         return new FastBitmapDrawable(WorkspaceIconUtils.createIconBitmap(drawable, mContext, addBoard,
                 !Constant.PACKAGE_NAME.equals(componentName.getPackageName())));
    }
    
    private FastBitmapDrawable createIconBitmap(ComponentName componentName, ResolveInfo info, String actionType) {
    	return createIconBitmap(componentName,info,actionType,-1);
    }

    private boolean isSpecialIconResName(String resName) {
        return THEME_ICON_RES_NAME.equals(resName) || THEME_STORE_ICON_RES_NAME.equals(resName);
    }

    public static class PackageMapping {
        private Map<String, String> mPackageNameMappings;


        private Map<String, String> loadPackageNameMappings(Context context) {
            if (mPackageNameMappings  == null) {
                ArrayMap<String, String> ret = new ArrayMap<String, String>();
                String databasePath = Environment.getDataDirectory().getPath() + "/data/" + Constant.PACKAGE_NAME + "/databases/packages.db";
                SQLiteDatabase db = null;
                Cursor c = null;

                try {
                    File databaseFile = new File(databasePath);
                    if (!databaseFile.getParentFile().exists()) {
                        databaseFile.getParentFile().mkdirs();
                    }

                    if (!databaseFile.exists()) {
                        com.shouxinzm.launcher.util.FileUtils.copyInputStreamToFile(context.getResources().openRawResource(R.raw.packages), databaseFile);
                        db = SQLiteDatabase.openOrCreateDatabase(databasePath, null);
                        db.setVersion(Constant.PACKAGES_DATABASE_VERSION);
                    } else {
                        db = SQLiteDatabase.openOrCreateDatabase(databasePath, null);
                        if (db.getVersion() < Constant.PACKAGES_DATABASE_VERSION) {
                            db.close();
                            databaseFile.delete();
                            com.shouxinzm.launcher.util.FileUtils.copyInputStreamToFile(context.getResources().openRawResource(R.raw.packages), databaseFile);

                            db = SQLiteDatabase.openOrCreateDatabase(databasePath, null);
                            db.setVersion(Constant.PACKAGES_DATABASE_VERSION);
                        }
                    }

                    String sql = "select * from packages";
                    c = db.rawQuery(sql, null);
                    if (c.getCount() > 0 && c.moveToFirst()) {
                        final int sourceIndex = c.getColumnIndexOrThrow("source");
                        final int targetIndex = c.getColumnIndexOrThrow("target");
                        final int activityIndex = c.getColumnIndexOrThrow("activity");
                        do {
                            String activity = c.getString(activityIndex);
                            if (activity != null && activity.length() != 0) {
                                if (activity.startsWith("#")) {
                                    ret.put(c.getString(sourceIndex).trim() + "/" + activity.trim().substring(1), c.getString(targetIndex).trim());
                                } else {
                                    ret.put(c.getString(sourceIndex).trim() + "/" + c.getString(sourceIndex).trim() + "." + activity.trim(), c
                                            .getString(targetIndex).trim());
                                }
                            } else {
                                ret.put(c.getString(sourceIndex).trim(), c.getString(targetIndex).trim());
                            }
                        } while (c.moveToNext());
                    }

                    if (LOGD_ENABLED) {
                        XLog.d(TAG, "Loaded package mappings size: " + ret.size());
                    }
                } catch (Exception e) {
                    XLog.e(TAG, "Failed to laod the package mappings", e);
                } finally {
                    if (c != null) {
                        try {
                            c.close();
                        } catch (Exception e) {
                            //do nothing
                        }
                    }
                    if (db != null) {
                        try {
                            db.close();
                        } catch (Exception e) {
                            //do nothing
                        }
                    }
                }
                mPackageNameMappings = ret;
            }

            return mPackageNameMappings;

        }
    }
}
