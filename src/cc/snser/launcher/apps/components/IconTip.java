package cc.snser.launcher.apps.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import cc.snser.launcher.App;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.R;

/**
 * {@link #IconView}右上角的小角标枚举.
 *
 * @author shixiaolei
 */
public enum IconTip {

    /** 新应用提示  */
    TIP_NEW(getSize(1), getSize(-1), R.drawable.new_install_app, 1);

    private int offsetX;
    private int offsetY;
    private int res;
    private int priority;

    private IconTip(int offsetX, int offsetY, int res, int priority) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.res = res;
        this.priority = priority;
    }

    public Drawable getDrawable(Context context) {
        return context.getResources().getDrawable(res);
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getPriority() {
        return priority;
    }

    private static int getSize(float size) {
        return Utilities.dip2px(App.getApp(), size);
    }
}
