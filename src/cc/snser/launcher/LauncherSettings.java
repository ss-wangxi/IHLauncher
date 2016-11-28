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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.BaseColumns;
import cc.snser.launcher.iphone.model.IphoneUtils;
import cc.snser.launcher.iphone.model.LauncherProvider;
import cc.snser.launcher.style.SettingPreferences;
import cc.snser.launcher.ui.utils.SettingsConstants;

import com.shouxinzm.launcher.util.DeviceUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Settings related utilities.
 */
public class LauncherSettings {
    public static interface BaseLauncherColumns extends BaseColumns {
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String TITLE = "title";

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to
         * {@link android.content.Intent#parseUri(String, int)} to create an
         * Intent that can be launched.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String INTENT = "intent";

        /**
         * The type of the gesture
         *
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String ITEM_TYPE = "itemType";

        /**
         * The gesture is an application
         */
        public static final int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
        public static final int ITEM_TYPE_SHORTCUT = 1;

        /**
         * The icon type.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String ICON_TYPE = "iconType";

        /**
         * The icon is a resource identified by a package name and an integer
         * id.
         */
        public static final int ICON_TYPE_RESOURCE = 0;

        /**
         * The icon is a bitmap.
         */
        public static final int ICON_TYPE_BITMAP = 1;

        /**
         * The icon package name, if icon type is ICON_TYPE_RESOURCE.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ICON_PACKAGE = "iconPackage";

        /**
         * The icon resource id, if icon type is ICON_TYPE_RESOURCE.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String ICON_RESOURCE = "iconResource";

        /**
         * The title package name.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String TITLE_PACKAGE = "titlePackage";

        /**
         * The title resource id.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String TITLE_RESOURCE = "titleResource";

        /**
         * The container holding the application
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String DECREASE_COLUMN = "decrease_column";

        /**
         * The container holding the application
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String INCREASE_COLUMN = "increase_column";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>
         * Type: BLOB
         * </P>
         */
        public static final String ICON = "icon";

        /**
         * The container holding the application
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String CONTAINER = "container";

        public static final int CONTAINER_DESKTOP = -100;
    }

    /**
     * Favorites.
     */
    public static class Favorites implements BaseLauncherColumns {
        /**
         * The content:// style URL for this table
         *
         * @param notify
         *            True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(Context context, boolean notify) {
    		return IphoneUtils.FavoriteExtension.getContentUri(context, notify);
        }
        
        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id
         *            The row id.
         * @param notify
         *            True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(Context context, long id, boolean notify) {
    		return IphoneUtils.FavoriteExtension.getContentUri(context, id, notify);
        }
        
        /**
         * The screen holding the favorite (if container is CONTAINER_DESKTOP)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SCREEN = "screen";

        /**
         * The X coordinate of the cell holding the favorite (if container is
         * CONTAINER_DESKTOP or CONTAINER_DOCK)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String CELLX = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite (if container is
         * CONTAINER_DESKTOP)
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String CELLY = "cellY";

        /**
         * The X span of the cell holding the favorite
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SPANX = "spanX";

        /**
         * The Y span of the cell holding the favorite
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String SPANY = "spanY";

        /**
         * The favorite is a user created folder
         */
        public static final int ITEM_TYPE_USER_FOLDER = 2;

        /**
         * The favorite is a widget
         */
        public static final int ITEM_TYPE_APPWIDGET = 4;

        /**
         * The favorite is a widget view
         */
        public static final int ITEM_TYPE_WIDGET_VIEW = 5;

        /**
         * The appWidgetId of the widget
         *
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String APPWIDGET_ID = "appWidgetId";
    }
   
    /**
     * Hidden Applications.
     */
    public static final class AppHideList implements BaseColumns {

        /**
         * The content:// style URL for this table
         *
         * @param notify
         *            True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(boolean notify) {
            
        	return Uri.parse("content://" + LauncherProvider.getAuthority() + "/"
        			+ LauncherProvider.TABLE_APPHIDELIST + "?"
        			+ LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
        }

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id
         *            The row id.
         * @param notify
         *            True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + LauncherProvider.getAuthority() + "/"
                    + LauncherProvider.TABLE_APPHIDELIST + "/" + id + "?"
                    + LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
        }

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to
         * {@link android.content.Intent#parseUri(String, int)} to create an
         * Intent that can be launched.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String INTENT = "intent";
    }
    
    /**
     * Hidden Applications SecondLayer
     * 注意这里的uri是不对的，在第二层尚未实现隐藏app相关功能
     */
    public static final class AppHideListSecondLayer implements BaseColumns {

        /**
         * The content:// style URL for this table
         *
         * @param notify
         *            True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUriSecondLayer(boolean notify) {
            
        	return Uri.parse("content://" + LauncherProvider.getAuthority() + "/"
        			+ LauncherProvider.TABLE_APPHIDELIST + "?"
        			+ LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
        }

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to
         * {@link android.content.Intent#parseUri(String, int)} to create an
         * Intent that can be launched.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String INTENT = "intent";
    }
    
    /**
     * Folder Screens
     * 
     * Track the mapping of screens exported from folder
     */
    public static final class FolderScreens implements BaseColumns {
        

        /**
         * ID of folder which exported from.
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_ID = "folderId";

        /**
         * The time of the last update to this row.
         * <P>Type: INTEGER</P>
         */
        public static final String MODIFIED = "modified";
        
        public static Uri getContentUri(){
        	return Uri.parse("content://" + LauncherProvider.getAuthority() + "/"
        			+ LauncherProvider.TABLE_FOLDER_SCREENS + "?"
        			+ LauncherProvider.PARAMETER_NOTIFY + "=false");
        			
        }
    }
    
    /**
     * Folder Hidden
     * 
     */
    public static final class FolderHidden implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static Uri getContentUri(){
        	return Uri.parse("content://" + LauncherProvider.getAuthority() + "/"
        			+ LauncherProvider.TABLE_FOLDERHIDELIST +
                    "?" + LauncherProvider.PARAMETER_NOTIFY + "=false");
        }
        
        /**
         * ID of folder.
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_ID = "folderId";
        
        /**
         * Is folder hidden
         * <P>Type: BOOLEAN</P>
         */
        public static final String IS_FOLDER_HIDDEN = "is_folder_hidden";
        
        /**
         * folder hidden counts
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_HIDDEN_COUNT = "folder_hidden_count";
        
        /**
         * folder hidden schedule begin hour
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_HIDDEN_SCHEDULE_BEGIN_HOUR = "folder_hidden_schedule_begin_hour";
        
        /**
         * folder hidden schedule begin min
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_HIDDEN_SCHEDULE_BEGIN_MIN = "folder_hidden_schedule_begin_min";

        /**
         * folder hidden schedule end hour
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_HIDDEN_SCHEDULE_END_HOUR = "folder_hidden_schedule_end_hour";
        
        /**
         * folder hidden schedule end min
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_HIDDEN_SCHEDULE_END_MIN = "folder_hidden_schedule_end_min";
        
        /**
         * folder hidden schedule enable
         * <P>Type: INTEGER</P>
         */
        public static final String FOLDER_HIDDEN_SCHEDULE_ENABLE = "folder_hidden_schedule_enable";
        
        /**
         * The time of the last update to this row.
         * <P>Type: INTEGER</P>
         */
        public static final String MODIFIED = "modified";
    }

    static final String TAG = "LauncherSettings";

    private static Boolean sLoopHomeScreen;

    

    /**
     * 空
     */
    public static final int RENDER_PERFORMANCE_MODE_NONE = 0;

    /**
     * 质量优先
     */
    public static final int RENDER_PERFORMANCE_MODE_PREFER_QUALITY = 1;

    /**
     * 速度优先
     */
    public static final int RENDER_PERFORMANCE_MODE_PREFER_SPEED = 2;

    /**
     * 最小内存
     */
    public static final int RENDER_PERFORMANCE_MODE_MINIMAL_MEMORY = 3;

    private static Integer sHomeScreenTransformationType;

    private static int[] sHomeLayout;

    private static Integer sHomeLayoutType;

    private static Integer sDefaultHomeLayoutType;

    private static Set<String> sWhiteListMap = new HashSet<String>();

    private static Boolean sWhiteListChanged;

    private static Boolean sFolderRecommend;


    /**
     * 当前渲染的性能模式，返回如下值： <br>
     * {@link #RENDER_PERFORMANCE_MODE_NONE} <br>
     * {@link #RENDER_PERFORMANCE_MODE_PREFER_QUALITY} <br>
     * {@link #RENDER_PERFORMANCE_MODE_PREFER_SPEED} <br>
     * {@link #RENDER_PERFORMANCE_MODE_MINIMAL_MEMORY} <br>
     * @param context
     * @return
     */
    public static synchronized int getRenderPerformanceMode(Context context) {
        return RENDER_PERFORMANCE_MODE_PREFER_SPEED;
    }

    /**
     * 检测是否可以打开draw cache：依据渲染性能模式和进程剩余内存等等
     * @param context
     * @return
     */
    public static boolean canEnableDrawCache(Context context) {
        boolean enabled = true;
        final int mode = getRenderPerformanceMode(context);
        if (mode == LauncherSettings.RENDER_PERFORMANCE_MODE_MINIMAL_MEMORY) {
            enabled = false;
        }
        return enabled;
    }


    public static synchronized void clearHomeLayout() {
        sHomeLayoutType = null;
        sHomeLayout = null;
    }

   
    public static final boolean supportMultiScreensForScreenManager(Context context) {
        return true;
    }

    public static String getHomeLayoutTypeKey(Context context) {
        return SettingsConstants.KEY_HOME_LAYOUT_TYPE;
    }

    public static boolean isEnableStatusBarAutoTransparent() {
        return DeviceUtils.isAfterApiLevel19();
    }

    public static boolean isEnableNavigationBarAutoTransparent() {
        return DeviceUtils.isAfterApiLevel19();
    }

    public static boolean isEnableStatusBarAutoTransparentV2() {
        return !DeviceUtils.isAfterApiLevel19();
    }

    public static boolean isAutoCategory(Context context) {
        return true;
    }

    public static boolean isFolderRecommend(Context context) {
        if (sFolderRecommend == null) {
            SharedPreferences sharedPreferences = SettingPreferences.getPreferences();
            sFolderRecommend = sharedPreferences.getBoolean(SettingsConstants.KEY_FOLDER_PROMOTION, true);
        }
        return sFolderRecommend;
    }

    public static synchronized void onFolderRecommendChanged(boolean folderRecommend) {
        sFolderRecommend = folderRecommend;
    }
}
