
package cc.snser.launcher;

import android.os.Build;
import android.text.format.DateUtils;

public class Constant {
	public static final boolean BUILD_FOR_MILESTONE_1 = false;
	
    public static final boolean DEBUG_LAYOUT = false;

    public static final boolean DEBUG = true;

    public static final boolean PROFILE_STARTUP = false;

    public static final boolean ENABLE_WIDGET_SCROLLABLE = true;

    public static final boolean LOGD_ENABLED = true;

    public static final boolean LOGI_ENABLED = true;

    public static final boolean LOGW_ENABLED = true;

    public static final boolean LOGE_ENABLED = true;

    public static final boolean CATCH_UNEXPECTED_EXCEPTION = false;

    public static final String PACKAGE_NAME = "com.caros.launcher";

    public static final String LAUNCHER_CUSTOM_SHORTCUT_ACTION = "cc.snser.launcher.custom_shortcut_action";

    public static final int ANDROID_SDK_VERSION = Build.VERSION.SDK_INT;

    public static final String LAUNCHER_PREF_FILE = "launcher_preferences";

    public static final String LAUNCHER_THEME_PREF_FILE_PREFIX = "launcher_theme_";

    public static final String LAUNCHER_THEME_PREF_FILE_SUFFIX = "_preferences";

    public static final int PACKAGES_DATABASE_VERSION = 37;

    public static final String LAUNCHER_THEME_APK_PACKAGE_NAME_PREFIX = "cc.snser.launcher.theme.";

    public static final String LAUNCHER_THEME_APK_ACTION = "cc.snser.launcher.theme.apk_action";

    public static final String PREF_STAT_PREFIX = "STAT_START_PREFIX_";

    public static final int CONTAINER_OTHER = 0;

    public static final int CONTAINER_HOME = 1;

    public static final int CONTAINER_THEME = 2;

    public static final int CONTAINER_CLOCKWEATHER = 3;

    public static final int CONTAINER_FOLDER_ADD = 4;

    public static final int ITEM_AREA_UNKOWN = 0;

    public static final int ITEM_AREA_LEFT = 1;

    public static final int ITEM_AREA_MID = 2;

    public static final int ITEM_AREA_RIGHT = 3;

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final int DEFAULT_NEW_INSTALLED_APP_TIP_MINUTE = 60;

    public static final long DEFAULT_NEW_INSTALLED_APP_TIP_MILLISECONDS = DEFAULT_NEW_INSTALLED_APP_TIP_MINUTE * DateUtils.MINUTE_IN_MILLIS;

    public static final int APPLICATION_INTERNAL = 0;

    public static final int APPLICATION_EXTERNAL = 1;

    public static final int FLAG_STOP_LAUNCHER = 1;

    public static final int FLAG_RESTART_LAUNCHER = 2;

    public static final int FLAG_RELOAD_LAUNCHER_FOR_THEME = 4;

    public static final int FLAG_RESTART_LAUNCHER_FOR_WALLPAPER = 6;

    public static final int FLAG_START_LAUNCHER_FOR_SETTING_DEFAULT = 11;

    public static final int FLAG_RESTART_LAUNCHER_FOR_ICON_TEXT_SIZE = 14;

    public static final int FLAG_RESTART_ALL_PROCESS = 16;

    public static final String ASSETS_PATH_PREFIX = "file:///android_asset/";

    public static final String WALLPAPER_TYPE_DEFAULT = "0";

    public static final String WALLPAPER_TYPE_SINGLESCREEN = "1";

    public static final String WALLPAPER_TYPE_AUTO = "2";

    public static final String WALLPAPER_TYPE_UNKNOWN = "-1";

    public static final String ANDROID_SETTINGS_PACKAGENAME = "com.android.settings";

    public static final String THEME_FILE_DATA = "957897B2653954C27133956";

    public static final String  DYNAMIC_THEME_THREAD_PREFIX = "DynamicTheme_";
	
    public static final String  PREF_NAME_INSTALL_DATE = "InstallDate";	
}
