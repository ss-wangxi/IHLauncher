package cc.snser.launcher.ui.components.pagedsv;

/**
 * 定义{@link PagedScrollView}}的划屏策略供测试
 * @author yangkai
 *
 */
public class PagedScrollStrategy {

    static final int STRATEGY_DEFAULT = 0; // 原来的DEFAULT_MODE
    static final int STRATEGY_DEFAULT_X_LARGE = 1; // 原来的X_LARGE_MODE
    static final int STRATEGY_GOOGLE_LAUNCHER_NORMAL = 2; // 原生桌面Normal
    static final int STRATEGY_GOOGLE_LAUNCHER_X_LARGE = 3; // 原生桌面Large

    static int mMode = getScrollStrategy();

    public static int getScrollStrategy() {
        return STRATEGY_DEFAULT;
    }

    static int getScrollDuration(int whichScreen, int velocity, boolean settle, boolean isSnapDirectly, PagedScrollView pagedView, int screenDelta, int delta) {
        switch (mMode) {
            case STRATEGY_DEFAULT:
            case STRATEGY_DEFAULT_X_LARGE:
//                return getScrollDurationDefault(whichScreen, velocity, settle, isSnapDirectly, pagedView, screenDelta, delta);
            case STRATEGY_GOOGLE_LAUNCHER_X_LARGE:
                return getScrollDurationGoogleLarge(whichScreen, velocity, settle, isSnapDirectly, pagedView, screenDelta, delta);
            case STRATEGY_GOOGLE_LAUNCHER_NORMAL:
                return getScrollDurationGoogleNormal(whichScreen, 0, settle, isSnapDirectly, pagedView, screenDelta, delta);
            default:
                return getScrollDurationDefault(whichScreen, velocity, settle, isSnapDirectly, pagedView, screenDelta, delta);
        }
    }

    private static int getScrollDurationDefault(int whichScreen, int velocity, boolean settle, boolean isSnapDirectly, PagedScrollView pagedView, int screenDelta, int delta) {
        int duration = (screenDelta + 1) * 100;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration += 1000.0f * duration / velocity;
        } else {
            duration += 100;
        }

        if (screenDelta <= 1) {
            duration = Math.min(duration, 600);
            if (velocity > 0 && Math.abs(delta) < pagedView.getWidth() / 3) { // 防止快速划屏中，有时滑动距离过短而时间过长的问题
                duration = (int) Math.max(100, Math.abs(delta) *1f / pagedView.getWidth() * duration);
            }
        }

        return duration;
    }

    private static final int MIN_SNAP_VELOCITY = 1500;

    protected static int mMinSnapVelocity = 0;

    private static int getScrollDurationGoogleLarge(int whichScreen, int velocity, boolean settle, boolean isSnapDirectly, PagedScrollView pagedView, int screenDelta, int delta) {
        int halfScreenSize = pagedView.getWidth() / 2;

        // Here we compute a "distance" that will be used in the computation of the overall
        // snap duration. This is a function of the actual distance that needs to be traveled;
        // we keep this value close to half screen size in order to reduce the variance in snap
        // duration as a function of the distance the page needs to travel.
        float distanceRatio = Math.min(1f, 1.0f * Math. abs(delta) / (2 * halfScreenSize));
        float distance = halfScreenSize + halfScreenSize *
                distanceInfluenceForSnapDuration(distanceRatio);

        if (mMinSnapVelocity  == 0) {
            float mDensity = pagedView.getContext().getResources().getDisplayMetrics().density;
            mMinSnapVelocity = (int) (MIN_SNAP_VELOCITY * mDensity);
        }

        velocity = Math. abs(velocity);
        velocity = Math. max(mMinSnapVelocity, velocity);

        // we want the page's snap velocity to approximately match the velocity at which the
        // user flings, so we scale the duration by a value near to the derivative of the scroll
        // interpolator at zero, ie. 5. We use 4 to make it a little slower.
        int duration = 4 * Math.round(1000 * Math. abs(distance / velocity));
        return duration;
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    private static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    private static int getScrollDurationGoogleNormal(int whichScreen, int velocity, boolean settle, boolean isSnapDirectly, PagedScrollView pagedView, int screenDelta, int delta) {
        int duration = (screenDelta + 1) * 100;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration += 1000.0f * duration / velocity;
        } else {
            duration += 100;
        }

        return duration;
    }

    static boolean ableToComputeScroll() {
        switch (mMode) {
            case STRATEGY_DEFAULT:
            case STRATEGY_GOOGLE_LAUNCHER_NORMAL:
                return true;
            case STRATEGY_DEFAULT_X_LARGE:
            case STRATEGY_GOOGLE_LAUNCHER_X_LARGE:
                return false;
            default:
                return true;
        }
    }

    static void computeScroll(PagedScrollView pagedView, float mTouchX, int mScrollX) {
        switch (mMode) {
            case STRATEGY_DEFAULT:
                computeScrollDefault(pagedView, mTouchX, mScrollX);
                break;
            case STRATEGY_GOOGLE_LAUNCHER_NORMAL:
                computeScrollGoogleNormal(pagedView, mTouchX, mScrollX);
                break;
            case STRATEGY_DEFAULT_X_LARGE:
            case STRATEGY_GOOGLE_LAUNCHER_X_LARGE:
                // do nothing
                break;
            default:
                computeScrollDefault(pagedView, mTouchX, mScrollX);
                break;
        }
    }

    private static void computeScrollDefault(PagedScrollView pagedView, float mTouchX, int mScrollX) {
        final float now = System.nanoTime() / PagedScrollView.NANOTIME_DIV;
        final float e = (float) Math.exp((now - pagedView.mSmoothingTime) / PagedScrollView.SMOOTHING_CONSTANT);
        final float dx = mTouchX - mScrollX;
        pagedView.setPagedScrollX(Math.round(mScrollX + dx * e));
        pagedView.mSmoothingTime = now;

        // Keep generating points as long as we're more than 1px away from the target
        if (dx > 1.f || dx < -1.f) {
            pagedView.postInvalidate();
        }
    }

    private static void computeScrollGoogleNormal(PagedScrollView pagedView, float mTouchX, int mScrollX) {
        final float now = System.nanoTime() / PagedScrollView.NANOTIME_DIV;
        final float e = (float) Math.exp((now - pagedView.mSmoothingTime) / PagedScrollView.SMOOTHING_CONSTANT);

        final float dx = mTouchX - mScrollX; // TODO mUnboundedScrollX? is what?

        pagedView.scrollTo(Math.round(mScrollX + dx * e), pagedView.getScrollY());
        pagedView.mSmoothingTime = now;

        // Keep generating points as long as we're more than 1px away from the target
        if (dx > 1.f || dx < -1.f) {
            pagedView.invalidate();
        }
    }

    static boolean handleTouchMoveScrollBy() {
        switch (mMode) {
            case STRATEGY_DEFAULT:
            case STRATEGY_GOOGLE_LAUNCHER_NORMAL:
                return false;
            case STRATEGY_DEFAULT_X_LARGE:
            case STRATEGY_GOOGLE_LAUNCHER_X_LARGE:
                return true;
            default:
                return false;
        }
    }

    static void offsetScroll(PagedScrollView pagedView, int scrollX) {
        switch (mMode) {
            case STRATEGY_DEFAULT:
                offsetScrollDefault(pagedView, scrollX);
                break;
            case STRATEGY_GOOGLE_LAUNCHER_NORMAL:
            case STRATEGY_DEFAULT_X_LARGE:
            case STRATEGY_GOOGLE_LAUNCHER_X_LARGE:
            default:
                offsetScrollNormal(pagedView, scrollX);
                break;
        }
    }

    private static void offsetScrollDefault(PagedScrollView pagedView, int scrollX) {
        pagedView.setPagedScrollX(scrollX);
        pagedView.postInvalidate();
    }

    private static void offsetScrollNormal(PagedScrollView pagedView, int scrollX) {
        pagedView.scrollTo(scrollX, 0);
        pagedView.invalidate();
    }
}
