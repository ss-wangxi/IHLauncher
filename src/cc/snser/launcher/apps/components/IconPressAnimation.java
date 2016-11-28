package cc.snser.launcher.apps.components;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;

public final class IconPressAnimation extends ScaleAnimation {

    private IconPressAnimation(float from, float to) {
        super(from, to, from, to, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    private static final IconPressAnimation ICON_PRESS = new IconPressAnimation(1.0f, 0.95f);
    private static final IconPressAnimation ICON_RESET = new IconPressAnimation(0.95f, 1.0f);

    private static final int DURATION = 100;

    static {
        ICON_PRESS.setDuration(DURATION);
        ICON_PRESS.setInterpolator(new DecelerateInterpolator());
        ICON_PRESS.setFillAfter(true);
        ICON_RESET.setDuration(DURATION / 2);
        ICON_RESET.setInterpolator(new AccelerateInterpolator());
    }

    public static IconPressAnimation obtain(boolean pressed) {
        return pressed ? ICON_PRESS : ICON_RESET;
    }

    /**
     * for 4.1, avoid memory leak due to a reference to view's handler
     * */
    public static void release() {
        ICON_PRESS.reset();
        ICON_RESET.reset();
    }
}
