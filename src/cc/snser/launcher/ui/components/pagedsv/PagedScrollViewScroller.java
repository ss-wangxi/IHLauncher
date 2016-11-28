package cc.snser.launcher.ui.components.pagedsv;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import cc.snser.launcher.ui.effects.EffectInfo;

/**
 * {@link PagedScrollView}}中scroller的代理类，处理用不同scroller类来实现滚动
 * @author yangkai
 *
 */
public class PagedScrollViewScroller {
    private final Context mContext;

    private final Interpolator mInterpolator;

    private Scroller mScroller;

    PagedScrollViewScroller(Context context, Interpolator interpolator) {
        mContext = context;
        mInterpolator = interpolator;
        mScroller = new Scroller(mContext, mInterpolator);
    }

    /**
     * 某些特效需要更换scroller
     * @param info
     */
    public void onEffectChanged(EffectInfo info) {
    }

    public boolean isFinished() {
        return mScroller.isFinished();
    }

    public void abortAnimation() {
        mScroller.abortAnimation();
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        mScroller.startScroll(startX, startY, dx, dy, duration);
    }

    public boolean computeScrollOffset() {
        return mScroller.computeScrollOffset();
    }

    public int getCurrX() {
        return mScroller.getCurrX();
    }

    public int getCurrY() {
        return mScroller.getCurrY();
    }

    public int getFinalX() {
        return mScroller.getFinalX();
    }

    public int getFlingDistance(int velocityX, int velocityY) {
        float ppi = mContext.getResources().getDisplayMetrics().density * 160.0f;
        float mDeceleration = SensorManager.GRAVITY_EARTH   // g (m/s^2)
                * 39.37f                        // inch/meter
                * ppi                           // pixels per inch
                * ViewConfiguration.getScrollFriction();
        float velocity = (float)Math.hypot(velocityX, velocityY);
        int totalDistance = (int) ((velocity * velocity) / (2 * mDeceleration));

        float mCoeffX = velocity == 0 ? 1.0f : velocityX / velocity;

        return Math.round(totalDistance * mCoeffX);
    }
}
