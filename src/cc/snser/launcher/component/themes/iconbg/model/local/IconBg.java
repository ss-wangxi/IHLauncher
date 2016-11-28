package cc.snser.launcher.component.themes.iconbg.model.local;

import android.content.Context;
import android.text.TextUtils;
import cc.snser.launcher.ui.utils.PrefConstants;
import cc.snser.launcher.ui.utils.PrefUtils;


/**
 * 背板抽象类.
 * <p>
 * XXX The soft reference cache may cause some problems, see
 * <a href="http://groups.google.com/group/android-developers/browse_thread/thread/ebabb0dadf38acc1">here</a>
 * for further information.</p>
 *
 * @author GuoLin
 *
 */
public class IconBg {
    public static final String NO_BG_ID = "default-icon_bg_none";

    private static String sCurrent;

    public static void setCurrentId(String id, Context context) {
        PrefUtils.setStringPref(context, PrefConstants.KEY_CURRENT_ICON_BG, id);
        sCurrent = id;
    }

    public static void resetCurrentId() {
        sCurrent = null;
    }

    public static String getCurrentId(Context context) {
        if (sCurrent == null) {
            sCurrent = PrefUtils.getStringPref(context, PrefConstants.KEY_CURRENT_ICON_BG, "");
        }
        return sCurrent;
    }

    public static boolean hasSetted(Context context) {
        return !TextUtils.isEmpty(getCurrentId(context));
    }

    public static boolean isUsingNoBg(Context context) {
        if (hasSetted(context)) {
            return IconBg.NO_BG_ID.equals(getCurrentId(context));
        }
        
        return false;
    }
}
