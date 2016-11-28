
package cc.snser.launcher.support.settings;

import cc.snser.launcher.Launcher;
import cc.snser.launcher.features.shortcut.CustomShortcutAction;
import cc.snser.launcher.features.shortcut.CustomShortcutActionInfo;

import com.btime.launcher.R;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class GestureSettings {
    public static final String GESTURE_WORKSPACE_NONE = "0";

    public static final String GESTURE_WORKSPACE_DEFAULT_SHORTCUT = "1";

    public final GestureType gestureType;

    public final String category;

    public final String actionName;

    public final String actionValue;

    public GestureSettings(GestureType gestureType, String category, String actionName, String actionValue) {
        this.gestureType = gestureType;
        this.category = category;
        this.actionName = actionName;
        this.actionValue = actionValue;
    }

    public String getSummary(Context context) {
        if (GestureSettings.GESTURE_WORKSPACE_DEFAULT_SHORTCUT.equals(category)) {
            return actionName;
        }

        return context.getString(R.string.settings_gesture_none);
    }

    public void fireAction(GestureType gestureType, Launcher launcher) {
    }

    public String toString() {
        return category + "," + actionValue + "," + actionName;
    }

    public static GestureSettings from(Context context, GestureType gestureType, String value) {
        int pos1 = value.indexOf(",");
        int pos2 = value.indexOf(",", pos1 + 1);
        if (pos1 >= 0 && pos2 >= 0) {
            return new GestureSettings(gestureType, value.substring(0, pos1), value.substring(pos2 + 1), value.substring(pos1 + 1, pos2));
        }
        return gestureType.getDefault(context);
    }

    public static List<GestureSettings> all(Context context, GestureType gestureType) {
        List<GestureSettings> ret = new ArrayList<GestureSettings>();

        ret.add(new GestureSettings(gestureType, GestureSettings.GESTURE_WORKSPACE_NONE, context.getString(R.string.settings_gesture_none), ""));
        ret.addAll(CustomShortcutActionInfo.allForGestureSetting(context, gestureType));

        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actionValue == null) ? 0 : actionValue.hashCode());
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GestureSettings other = (GestureSettings) obj;
        if (actionValue == null) {
            if (other.actionValue != null)
                return false;
        } else if (!actionValue.equals(other.actionValue))
            return false;
        if (category == null) {
            if (other.category != null)
                return false;
        } else if (!category.equals(other.category))
            return false;
        return true;
    }
}
