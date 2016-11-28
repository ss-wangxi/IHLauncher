package cc.snser.launcher.ui.effects;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

/**
 * 从现在位置到屏幕边缘的动画、可以设置动画向屏幕哪个方向运动（暂时只有上下两种）
 * @author yangkai
 *
 */
public class TranslateAndAlphaAnimation extends Animation {

    private float mFromXDelta;

    private float mToXDelta;

    private float mFromYDelta;

    private float mToYDelta;

    private boolean mToBottom;

    private boolean mShow;

    private final Interpolator mAlphaInterpolator;

    private final IAnimationCallback mCallback;

    private final boolean mFastHide;

    public TranslateAndAlphaAnimation(Interpolator alphaInterpolator, IAnimationCallback callback, boolean toBottom, boolean show, boolean fastHide) {
        mAlphaInterpolator = alphaInterpolator;
        mCallback = callback;
        mToBottom = toBottom;
        mShow = show;
        mFastHide = fastHide;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mFromXDelta = 0;
        mToXDelta = 0;

        if (mToBottom) {
            mFromYDelta = mShow ? height : 0;
            mToYDelta = mShow ? 0 : height;
        } else {
            mFromYDelta = mShow ? -height : 0;
            mToYDelta = mShow ? 0 : -height;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dx = mFromXDelta;
        float dy = mFromYDelta;
        if (mFromXDelta != mToXDelta) {
            dx = mFromXDelta + ((mToXDelta - mFromXDelta) * interpolatedTime);
        }
        if (mFromYDelta != mToYDelta) {
            dy = mFromYDelta + ((mToYDelta - mFromYDelta) * interpolatedTime);
        }
        t.getMatrix().setTranslate(dx, dy);

        if (mAlphaInterpolator != null) {
            float a = mAlphaInterpolator.getInterpolation(mShow ? interpolatedTime : (mFastHide ? Math.max(1.0f - interpolatedTime * 2, 0) : 1.0f - interpolatedTime) );
            t.setAlpha(a);
        }

        if (mCallback != null) {
            mCallback.onTransformation(interpolatedTime, dx, dy);
        }
    }
}
