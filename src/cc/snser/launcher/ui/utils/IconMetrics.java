package cc.snser.launcher.ui.utils;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Rect;

import com.btime.launcher.R;

public class IconMetrics {

    public int iconWidth;
    public int iconHeight;

    private int flag = -1;

    private static IconMetrics sInstance;

    public static IconMetrics getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new IconMetrics();
        }
        sInstance.init(context);
        return sInstance;
    }

    private void init(Context context) {
        if (flag != -1) {
            return;
        }

        iconWidth = iconHeight = WorkspaceIconUtils.getIconSizeWithPadding(context, -1);

        flag = 0;
    }

    public static void flush() {
        if (sInstance != null) {
            sInstance.flag = -1;
        }
    }

    public static int getIconWidth(Context context) {
        return getInstance(context).iconWidth;
    }

    public static int getIconHeight(Context context) {
        return getInstance(context).iconHeight;
    }
}
