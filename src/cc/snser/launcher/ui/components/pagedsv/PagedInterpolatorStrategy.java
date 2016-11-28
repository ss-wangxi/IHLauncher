package cc.snser.launcher.ui.components.pagedsv;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import com.shouxinzm.launcher.util.DeviceUtils;

/**
 *  {@link PagedScrollView}}差值策略
 * @author yangkai
 *
 */
public class PagedInterpolatorStrategy {

    private static final String TAG = "Launcher.PagedInterpolatorStrategy";

    public static final int STRATEGY_DEFAULT = 0; // 原来默认
    public static final int STRATEGY_DEFAULT_NO_OVERSHOT = 1; // 原来默认但是去掉回弹
    public static final int STRATEGY_DEFAULT_EFFECT = 2; // 原来先加速在减速
    public static final int STRATEGY_GOOGLE_LAUNCHER_LARGE = 3; // 原生桌面的Large
    public static final int STRATEGY_GOOGLE_LAUNCHER_NORMAL = 4; // 原生桌面的Normal

    public static int getScrollInterpolatorIndex() {
        return STRATEGY_GOOGLE_LAUNCHER_LARGE;
    }

    public static BaseInterpolator getInterpolator() {
        int mode = getScrollInterpolatorIndex();
        BaseInterpolator interpolator = null;

        switch (mode) {
            case STRATEGY_DEFAULT:
                interpolator = new ScrollInterpolatorMiddle();
                break;
            case STRATEGY_DEFAULT_NO_OVERSHOT:
                interpolator = new ScrollInterpolatorMiddleNoOverShoot();
                break;
            case STRATEGY_DEFAULT_EFFECT:
                interpolator = new ScrollInterpolatorMiddle2();
                break;
            case STRATEGY_GOOGLE_LAUNCHER_LARGE:
                interpolator = new ScrollInterpolator();
                break;
            case STRATEGY_GOOGLE_LAUNCHER_NORMAL:
                interpolator = new ScrollInterpolatorForDeovoV5();
                break;
            default:
                interpolator = new ScrollInterpolatorMiddle();
                break;
        }

        return interpolator;
    }

    private PagedInterpolatorStrategy() {
        // do nothing
    }

    abstract static class BaseInterpolator implements Interpolator {
        public abstract void setDistance(int distance);
        public abstract void disableSettle();
    }

    /**
     * 原生的
     * @author yangkai
     *
     */
    static class ScrollInterpolator extends BaseInterpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            return t*t*t*t*t + 1;
        }

        @Override
        public void setDistance(int distance) {

        }

        @Override
        public void disableSettle() {

        }
    }

    /**
     * DeovoV5 专用的(原生桌面在normal时候也用这个)
     * @author yangkai
     *
     */
    static class ScrollInterpolatorForDeovoV5 extends BaseInterpolator {
        private static final float DEFAULT_TENSION = 0f;

        private float mTension;

        public ScrollInterpolatorForDeovoV5() {
            if (DeviceUtils.isDeovoV5()) {
                mTension = 1.3f;
            } else {
                mTension = DEFAULT_TENSION;
            }
        }

        @Override
        public void setDistance(int distance) {
            mTension = distance > 0 ? DEFAULT_TENSION / distance : DEFAULT_TENSION;
        }

        @Override
        public void disableSettle() {
            mTension = 0.f;
        }

        @Override
        public float getInterpolation(float t) {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * ((mTension + 1) * t + mTension) + 1.0f;
        }
    }

    /**
     * 原来的
     * @author yangkai
     *
     */
    private static class ScrollInterpolatorMiddle extends BaseInterpolator {
        private final OvershootInterpolator mOvInterpolator;

        private final DecelerateInterpolator mDeInterpolator;

        public ScrollInterpolatorMiddle() {
            mOvInterpolator = new OvershootInterpolator(0);
            mDeInterpolator = new DecelerateInterpolator(1f);
        }

        @Override
        public void setDistance(int distance) {

        }

        @Override
        public void disableSettle() {

        }

        @Override
        public float getInterpolation(float input) {
            return (mOvInterpolator.getInterpolation(input) + mDeInterpolator.getInterpolation(input)) / 2;
        }
    }

    /**
     * 先加速在减速
     * @author yangkai
     *
     */
    private static class ScrollInterpolatorMiddle2 extends BaseInterpolator {
        private final AccelerateDecelerateInterpolator mADInterpolator;

        public ScrollInterpolatorMiddle2() {
            mADInterpolator = new AccelerateDecelerateInterpolator();
        }

        @Override
        public void setDistance(int distance) {

        }

        @Override
        public void disableSettle() {

        }

        @Override
        public float getInterpolation(float input) {
            return mADInterpolator.getInterpolation(input);
        }
    }

    /**
     * 原来的
     * @author yangkai
     *
     */
    private static class ScrollInterpolatorMiddleNoOverShoot extends BaseInterpolator {
        private final DecelerateInterpolator mDeInterpolator;

        public ScrollInterpolatorMiddleNoOverShoot() {
            mDeInterpolator = new DecelerateInterpolator(1.0f);
        }

        @Override
        public void setDistance(int distance) {

        }

        @Override
        public void disableSettle() {

        }

        @Override
        public float getInterpolation(float input) {
            return mDeInterpolator.getInterpolation(input);
        }
    }
}
