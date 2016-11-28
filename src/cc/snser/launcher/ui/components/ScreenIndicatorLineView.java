
package cc.snser.launcher.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import cc.snser.launcher.ui.components.ScreenIndicator.OnIndicatorChangedListener;
import cc.snser.launcher.ui.utils.UiConstants;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.util.XLog;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * 当{@link ScreenIndicator} 为一条线时，这个View被加载出来
 *
 * @author yangkai
 */
public class ScreenIndicatorLineView extends View {

    private static final String TAG = "Launcher.ScreenIndicatorLineView";

    private Drawable mSelectedDrawable;

    private int mScreenCount;

    private int mCurrentScreen;

    private float mScrollX;

    private OnIndicatorChangedListener mOnClickListener;

    private RectF mRectF = new RectF();
    private Context mContext;

    public ScreenIndicatorLineView(Context context, Drawable selectDrawable, Drawable unselectDrawable, int screenCount, int currentScreen, OnIndicatorChangedListener onClickListener) {
        super(context);
        mContext = context;
        mSelectedDrawable = selectDrawable;

        mScreenCount = screenCount;
        mCurrentScreen = currentScreen;

        mOnClickListener = onClickListener;
        mTouchSlot = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setBackgroundDrawable(unselectDrawable);
    }

    @Override
    protected void onDraw(Canvas canvas) { // TODO 4.0 用更快的硬件做
        super.onDraw(canvas);

        if (mSelectedDrawable == null) {
            return;
        }

        float startX = 0;
        float startY = 0;
        float drawableBoundsWidth = 0;
        float drawableBoundsHeight = 0;

        final float selectWidth = getWidth() * 1f / mScreenCount;
        startX += selectWidth * (mCurrentScreen + mScrollX);

        drawableBoundsWidth = selectWidth;
        drawableBoundsHeight = getHeight();

        if (mSelectedDrawable.getMinimumWidth() <= drawableBoundsWidth) {
            mSelectedDrawable.setBounds(0, 0, (int) drawableBoundsWidth, (int) drawableBoundsHeight);
            canvas.translate(startX, startY);
            mSelectedDrawable.draw(canvas);
            canvas.translate(-startX, -startY);
        } else { // 屏幕过多，nine-patch图已经无法显示，我们画方块
            mRectF.left = startX;
            mRectF.top = startY + Utilities.dip2px(mContext, 3.5f);
            mRectF.right = startX + drawableBoundsWidth;
            mRectF.bottom = startY + drawableBoundsHeight - Utilities.dip2px(mContext, 9);

            final Paint p = UiConstants.TEMP_PAINT;
            p.setColor(0xfff6f9fc);
            canvas.drawRoundRect(mRectF, 4, 4, p);
            p.reset();
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "onDraw startX " + startX + " startY " + startY + " mCurrentScreen " + mCurrentScreen + " mScreenCount " + mScreenCount + " drawableBoundsWidth " + drawableBoundsWidth
                    + " drawableBoundsHeight " + drawableBoundsHeight + " mScrollX " + mScrollX);
        }
    }

    boolean updateScreens(int screenCount, int currentScreen, float scrollX) {
        if (screenCount == mScreenCount && currentScreen == mCurrentScreen && Math.abs(scrollX - mScrollX) < 0.001f) {
            return false;
        }

        mScreenCount = screenCount;
        mCurrentScreen = currentScreen;
        mScrollX = scrollX;
        return true;
    }

    void updateDrawables(Drawable selectDrawable, Drawable unselectDrawable) {
        mSelectedDrawable = selectDrawable;
        setBackgroundDrawable(unselectDrawable);
    }

    private float mTouchDownX;

    private float mTouchDownY;

    private long mTouchDownTime;

    private boolean mInClick;

    private int mTouchSlot;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (event.getPointerCount() > 1) {
            if (action != MotionEvent.ACTION_DOWN) {
                clearTouch();
            }
            return super.onTouchEvent(event);
        }
        if (LOGD_ENABLED) {
            XLog.d(TAG, "onTouchEvent action " + action + " getHeight " + getHeight() + " getWidth " + getWidth());
        }

        final float x = event.getX();
        final float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = x;
                mTouchDownY = y;

                mTouchDownTime = System.currentTimeMillis();
                mInClick = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(getDis(x, y) - getDis(mTouchDownX, mTouchDownY)) > mTouchSlot) {
                    mInClick = false;
                    int screenIn = getTouchScreen(x);

                    if (screenIn != mCurrentScreen) {
                        // scroll to
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                int screen = getTouchScreen(x);
                if (LOGD_ENABLED) {
                    XLog.d(TAG, "ACTION_UP mInClick " + mInClick + " System.currentTimeMillis() - mTouchDownTime " + (System.currentTimeMillis() - mTouchDownTime) + " screen " + screen);
                }

                if (mInClick && System.currentTimeMillis() - mTouchDownTime <= 200) {
                    if (mOnClickListener != null) {
                        mOnClickListener.snapToScreen(screen);
                    }
                } else {
                    if (screen != mCurrentScreen) {
                        // scroll to
                    }
                }

                clearTouch();
                break;

            default:
                break;
        }

        return true;
    }

    private int getDis(float x, float y) {
        return (int) Math.sqrt(x * x + y * y);
    }

    private int getTouchScreen(float x) {
        return (int) (x - getLeft()) / (getWidth() / mScreenCount);
    }

    private void clearTouch() {
        mTouchDownX = 0;
        mTouchDownY = 0;
        mTouchDownTime = -1;
        mInClick = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSelectedDrawable = null;
        setBackgroundDrawable(null);
    }
}
