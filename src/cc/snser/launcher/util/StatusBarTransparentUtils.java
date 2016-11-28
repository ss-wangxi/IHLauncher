
package cc.snser.launcher.util;

import android.view.View;
import cc.snser.launcher.App;
import cc.snser.launcher.LauncherSettings;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.DeviceUtils;

import java.lang.reflect.Field;

import static cc.snser.launcher.Constant.LOGD_ENABLED;
import static cc.snser.launcher.Constant.LOGE_ENABLED;

/**
 * 状态栏相关公共方法
 *
 * @author yangkai
 */
public class StatusBarTransparentUtils {

    private static final String TAG = "Launcher.StatusBarUtil";

    private StatusBarTransparentUtils() {
        // / do nothing
    }

    /** S4 */
    public static final boolean autoSetSystemUiTransparent(View view) {
        if (LauncherSettings.isEnableStatusBarAutoTransparent() || LauncherSettings.isEnableStatusBarAutoTransparentV2()) {
            return false;
        }

        if (!DeviceUtils.isS4()) {
            return false;
        }

        int value = 0;

        Integer intValue = getUIVisibilityValue();
        System.out.println("XXXXXXXXXXXXXXXXXXX intValue1 " + intValue);
        if (LOGD_ENABLED) {
            XLog.d(TAG, "" + intValue);
        }

        if (intValue == null) {
            return false;
        }

        value = intValue.intValue();

        ViewUtils.setSystemUiVisibility(view, value);

        return true;
    }

    /**
     * 将状态栏设置为透明
     */
    public static final void setSystemUiTransparent(View view, boolean transparent) {

        int value = 0;

        if (transparent) {
            Integer intValue = getUIVisibilityValue();
            System.out.println("XXXXXXXXXXXXXXXXXXX intValue2 " + intValue);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "" + intValue);
            }

            if (intValue == null) {
                return;
            }

            value = intValue.intValue();
        }

        ViewUtils.setSystemUiVisibility(view, value);
    }

    private static final Integer getUIVisibilityValue() {
        String[] libNames = App.getApp().getPackageManager().getSystemSharedLibraryNames();
        String uiVisibilityString = null;
        uiVisibilityString = "SYSTEM_UI_FLAG_TRANSPARENT_BACKGROUND";
        for (String name : libNames) {
            System.out.println("XXXXXXXXXXXXXXXXXXX name " + name);
            if (name.equals("com.sonyericsson.navigationbar")) {
                uiVisibilityString = "SYSTEM_UI_FLAG_TRANSPARENT";
                break;
            }
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "getUIVisibilityValue string " + uiVisibilityString);
        }

        try {
            for (Field field : View.class.getDeclaredFields()) {
                System.out.println("XXXXXXXXXXXXXXXXXXX field name " + field.getName());
            }
            Field visibilityField = View.class.getField(uiVisibilityString);
            System.out.println("XXXXXXXXXXXXXXXXXXX visibilityField " + visibilityField);
            if ((visibilityField != null) && (visibilityField.getType() == Integer.TYPE)) {
                return Integer.valueOf(visibilityField.getInt(null));
            }
        } catch (Exception e) {
            if (LOGE_ENABLED) {
                XLog.e(TAG, "get visibilityField error", e);
            }
        }
        return null;
    }
}
