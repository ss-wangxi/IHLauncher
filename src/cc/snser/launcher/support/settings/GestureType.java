
package cc.snser.launcher.support.settings;

import cc.snser.launcher.features.shortcut.CustomShortcutAction;
import cc.snser.launcher.style.SettingPreferences;

import com.btime.launcher.R;

import android.content.Context;
import android.content.SharedPreferences;

public enum GestureType {
    UP("pref_k_workspace_gesture_up_action") {
        @Override
        public GestureSettings getDefault(Context context) {
            return new GestureSettings(this, GestureSettings.GESTURE_WORKSPACE_DEFAULT_SHORTCUT, context.getString(R.string.custom_shortcut_action_type_workspace_menu),
                    CustomShortcutAction.ACTION_TYPE_WORKSPACE_MENU);
        }

        @Override
        public GestureSettings restoreFromOld(Context context) {
            return null;
        }
    },
    DOWN("pref_k_workspace_gesture_down_action") {
        @Override
        public GestureSettings getDefault(Context context) {
            return new GestureSettings(this, GestureSettings.GESTURE_WORKSPACE_DEFAULT_SHORTCUT, context.getString(R.string.custom_shortcut_action_type_open_notification_bar),
                    CustomShortcutAction.ACTION_TYPE_OPEN_NOTIFICATION_BAR);
        }

        @Override
        public GestureSettings restoreFromOld(Context context) {
            return null;
        }
    },
    DOUBLE_CLICK("pref_k_workspace_gesture_double_click_action") {
        @Override
        public GestureSettings getDefault(Context context) {
        	return new GestureSettings(this, GestureSettings.GESTURE_WORKSPACE_DEFAULT_SHORTCUT, context.getString(R.string.custom_shortcut_action_type_search_by_index),
        			CustomShortcutAction.ACTION_TYPE_SERACH_BY_INDEX);
        }

        @Override
        public GestureSettings restoreFromOld(Context context) {
            String key = "pref_app_search_type";

            SharedPreferences sharedPreferences = SettingPreferences.getPreferences();
            String action = sharedPreferences.getString(key, null);
            if (action == null) {
                return null;
            }

            sharedPreferences.edit().remove(key).commit();

            return null;
        }
    };

    private final String prefKey;

    private GestureType(String prefKey) {
        this.prefKey = prefKey;
    }

    public String getPrefKey(Context context) {
        return prefKey;
    }

    public abstract GestureSettings getDefault(Context context);

    public abstract GestureSettings restoreFromOld(Context context);
}
