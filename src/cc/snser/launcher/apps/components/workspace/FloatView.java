package cc.snser.launcher.apps.components.workspace;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import cc.snser.launcher.Constant;
import cc.snser.launcher.Launcher;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.support.v4.util.SimpleAnimator;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.DeviceUtils;

public class FloatView extends View {

    private static final String TAG = "Launcher.FloatView";

    private final ViewManager mViewManager;
    private WindowManager.LayoutParams mWindowParams;
    private ViewGroup.MarginLayoutParams mLayoutParams;
    protected int mType;

    private boolean mIsShow = false;
    private boolean mInitialized = false;

    private Bitmap mBitmap;
    private Paint mDefaultPaint;
    protected Paint mPaint;
    protected Rect src = new Rect();
    protected Rect dst = new Rect();
    protected int mWidth, mHeight;

    // animation variables
    protected boolean isAnimating = false;
    private int mDuration = 300;
    private float mAnimationRatio = 0;
    private float mAnimationScale = 1;
    private Rect mStartRect = new Rect();
    private Rect mTargetRect = new Rect();
    private TDInterpolator mTDInterpolator;
    private Interpolator mInterpolator;
    private SimpleAnimator mAnimator;
    private final float[] mAnimPosition = new float[2];

    private boolean mDestoryAfter = false;
    private boolean mDismissAfter = true;

    private int mWarningBkColor;
    private boolean mSetWarningFilter;

    public FloatView(Context context, Bitmap bmp) {
        this(context, bmp, WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL);
    }

    public FloatView(Context context, Bitmap bmp, int type) {
        super(context);
        mType = type;

        if (context instanceof Launcher && DeviceUtils.isIceCreamSandwich()) {
            mViewManager = ((Launcher) context).getDragLayer();
        } else {
            mViewManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }

        mBitmap = bmp;
        mDefaultPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        mPaint = new Paint(mDefaultPaint);

        mWarningBkColor = 0;

        src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        dst.set(src);

        mWidth = getHorizontalSpace();
        mHeight = getVerticalSpace();
    }

    public void setSize(int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    public void setDrawingFrame(int l, int t, int r, int b) {
        dst.set(l, t, r, b);
        invalidate();
    }

    public void clipRect(Rect rect) {
        src.intersect(rect);
    }

    /**
     * 是否初始化完成
     * @return
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mWidth, mHeight);
        if (Constant.LOGD_ENABLED) {
            XLog.d(TAG, " onMeasure mw=" + getMeasuredWidth() + " mh=" + getMeasuredHeight());
        }
        mInitialized = true;
    }

    protected int getHorizontalSpace() {
        return mBitmap == null ? 0 : mBitmap.getWidth();
    }

    protected int getVerticalSpace() {
        return mBitmap == null ? 0 : mBitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public int getType() {
        return mType;
    }

    /**
     * Create a window containing this view and show it.
     *
     * @param windowToken obtained from v.getWindowToken() from one of your views
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    public void show(IBinder windowToken, int x, int y) {
        this.setVisibility(View.VISIBLE);

        if (mIsShow) {
            setLayoutPosition(x, y);
            return;
        }

        ViewGroup.LayoutParams param;
        if (mViewManager instanceof WindowManager) {
            WindowManager.LayoutParams lp;
            int pixelFormat;

            pixelFormat = PixelFormat.TRANSLUCENT;

            lp = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    x, y,
                    mType,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    /*| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM*/,
                    pixelFormat);
//          lp.token = mStatusBarView.getWindowToken();
            lp.gravity = Gravity.LEFT | Gravity.TOP;
            lp.token = windowToken;

            mWindowParams = lp;
            param = lp;

            onCreateLayoutParams(lp);
        } else {

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = x;
            lp.topMargin = y;

            mLayoutParams = lp;
            param = lp;
        }

        try {
            mViewManager.addView(this, param);
        } catch (Exception e) {
            if (Constant.LOGE_ENABLED) {
                XLog.e(TAG, "exception", e);
            }
        }
        mIsShow = true;
    }

    protected void onCreateLayoutParams(WindowManager.LayoutParams params) {
        params.setTitle("FloatView");
    }

    /**
     * Move the window containing this view.
     *
     * @param x the x coordinate of the left top corner
     * @param y the y coordinate of the left top corner
     */
    public final void moveTo(int x, int y) {
        if (!mIsShow) {
            return;
        }

        setLayoutPosition(x, y);
    }

    public void setLayoutPosition(int x, int y) {
    	if(!isShowing()) return;
        ViewGroup.LayoutParams lp;
        if (mLayoutParams != null) {
            mLayoutParams.leftMargin = x;
            mLayoutParams.topMargin = y;
            lp = mLayoutParams;
        } else {
            lp = mWindowParams;
            mWindowParams.x = x;
            mWindowParams.y = y;
        }
        mViewManager.updateViewLayout(this, lp);
    }
    
    public final boolean isShowing() {
        return mIsShow;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!BitmapUtils.isValidBitmap(mBitmap)) {
            return;
        }

        final boolean scaled = mAnimationScale != 1;
        if (scaled) {
            canvas.save();
            canvas.scale(mAnimationScale, mAnimationScale, getCenterXOffset(), getCenterYOffset());
        }

        if(mSetWarningFilter && mPaint != null){
            if(mWarningBkColor > 0){
                canvas.drawColor(mWarningBkColor);
            }
            mPaint.setColorFilter(new PorterDuffColorFilter(Color.argb(180,255,0,0),PorterDuff.Mode.SRC_ATOP));
        }else{
            mPaint.setColorFilter(null);
        }
        canvas.drawBitmap(mBitmap, src, dst, mPaint);

        if (scaled) {
            canvas.restore();
        }

    }

    /**
     * @return the canvas save count
     * */
    protected int onPredraw(Canvas canvas) {
        return -1;
    }

    /**
     * @param count the canvas save count
     * */
    protected void onPostDraw(Canvas canvas, int count) {
    }

    public void setPaint(Paint paint) {
        if (paint == null) {
            mPaint.set(mDefaultPaint);
        } else {
            mPaint.set(paint);
        }

        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);

        invalidate();
    }

    public void remove() {
        if (isAnimating) {
            return;
        }

        if (!mIsShow) {
            return;
        }

        post(new Runnable() {
            public void run() {
                dismiss();
            }
        });
    }

    private void dismiss() {
        try {
            if (!mIsShow) {
                return;
            }
            mViewManager.removeView(this);
            mIsShow = false;
        } catch (Throwable e) {
            XLog.e(TAG, "Failed to remove the float view.", e);
        }
    }

    public void closeInstance() {
        if (isAnimating || !mDestoryAfter) {
            return;
        }
        BitmapUtils.recycleBitmap(mBitmap);
        mBitmap = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsShow = false;
        if (mDestoryAfter) {
            BitmapUtils.recycleBitmap(mBitmap);
        }
    }

    public float getX() {
        return mLayoutParams != null ? mLayoutParams.leftMargin : mWindowParams.x;
    }

    public float getY() {
        return mLayoutParams != null ? mLayoutParams.topMargin : mWindowParams.y;
    }

    public float getCenterXOffset() {
        return dst.exactCenterX();
    }

    public float getCenterYOffset() {
        return dst.exactCenterY();
    }

    public final boolean animateTo(int x, int y, final AnimationCallback callback) {
        if (!mIsShow) {
            return false;
        }

        if (x == getX() && y == getY() || isAnimating && x == mTargetRect.left && y == mTargetRect.top) {
            return false;
        }

        mStartRect.setEmpty();
        mStartRect.offsetTo((int)getX(), (int) getY());
        mTargetRect.setEmpty();
        mTargetRect.offsetTo(x, y);

        if (mTDInterpolator != null) {
            mTDInterpolator.initialize(getX(), getY(),
                    x, y);
        }

        if (mAnimator != null) {
            mAnimator.stop();
        }

        final TweenValueCallback tween;
        if (callback instanceof TweenValueCallback) {
            tween = (TweenValueCallback) callback;
        } else {
            tween = null;
        }

            mAnimator = new SimpleAnimator(new SimpleAnimator.Callback() {

                @Override
                public void onAnimationFrame(float value) {
                    mAnimationRatio = value;

                    if (tween != null) {
                        tween.onValueChanged(value);
                    }

                    if (mInterpolator != null) {
                        value = mInterpolator.getInterpolation(value);
                    }

                    float x, y;
                    if (mTDInterpolator != null) {
                        final float[] pos = mAnimPosition;
                        mTDInterpolator.getInterpolation(pos, value);
                        x = pos[0];
                        y = pos[1];
                    } else {
                        final float fromX = mStartRect.left;
                        final float fromY = mStartRect.top;

                        final float toX = mTargetRect.left;
                        final float toY = mTargetRect.top;
                        x = fromX + (toX - fromX) * value;
                        y = fromY + (toY - fromY) * value;
                    }

                    moveTo((int)x, (int)y);
                }

                @Override
                public void onAnimationFinished() {
                    endAnimation(callback);
                }
            });
            mAnimator.setDuration(mDuration).start();
        isAnimating = true;
        return true;
    }

    public final boolean animateTo(Rect target, final AnimationCallback callback) {
        if (!mIsShow) {
            return false;
        }

        if (target.width() == dst.width() && target.height() == dst.height()) {
            return animateTo((int) (target.exactCenterX() - dst.exactCenterX()), (int) (target.exactCenterY() - dst.exactCenterY()), callback);
        }

        mTargetRect.set(target);

        mStartRect.set(dst);
        mStartRect.offset((int) getX(), (int) getY());

        if (mTDInterpolator != null) {
            mTDInterpolator.initialize(mStartRect.exactCenterX(), mStartRect.exactCenterY(),
                    mTargetRect.exactCenterX(), mTargetRect.exactCenterY());
        }

        if (mAnimator != null) {
            mAnimator.stop();
        }

        final TweenValueCallback tween;
        if (callback instanceof TweenValueCallback) {
            tween = (TweenValueCallback) callback;
        } else {
            tween = null;
        }

        mAnimator = new SimpleAnimator(new SimpleAnimator.Callback() {

            @Override
            public void onAnimationFrame(float value) {
                mAnimationRatio = value;

                if (tween != null) {
                    tween.onValueChanged(value);
                }

                if (mInterpolator != null) {
                    value = mInterpolator.getInterpolation(value);
                }

                final float x, y;
                if (mTDInterpolator != null) {
                    final float[] pos = mAnimPosition;
                    mTDInterpolator.getInterpolation(pos, value);
                    x = pos[0];
                    y = pos[1];
                } else {
                    final float fromX = mStartRect.exactCenterX();
                    final float fromY = mStartRect.exactCenterY();
                    final float toX = mTargetRect.exactCenterX();
                    final float toY = mTargetRect.exactCenterY();
                    x = fromX + (toX - fromX) * value;
                    y = fromY + (toY - fromY) * value;
                }

                final float fromW = mStartRect.width();
                //final float fromH = mStartRect.height();
                final float toW = mTargetRect.width();
                //final float toH = mTargetRect.height();

                final float w = fromW + (toW - fromW) * value;
                //final float h = fromH + (toH - fromH) * value;

                final int cx = (int) getCenterXOffset();
                final int cy = (int) getCenterYOffset();

                mAnimationScale = w / dst.width();

                //dst.set(cx - (int) (w / 2), cy - (int) (h / 2), cx + (int) (w / 2), cy + (int) (h / 2));
                moveTo((int) (x - cx), (int) (y - cy));
                invalidate();
            }

            @Override
            public void onAnimationFinished() {
                endAnimation(callback);
            }
        });
        mAnimator.setDuration(mDuration).start();
        isAnimating = true;
        return true;
    }

    private void endAnimation(AnimationCallback callback) {
        isAnimating = false;

        if (mDismissAfter) {
            dismiss();
        }

        if (callback != null) {
            callback.onAnimationFinished();
        }

        mAnimator = null;
    }


    public void setWarningFilter(boolean bsetWarningFilter){
        if(mSetWarningFilter == bsetWarningFilter) return;
        mSetWarningFilter = bsetWarningFilter;
        invalidate();
    }

    public void setWarningBkColor(int color){
        mWarningBkColor = color;
        invalidate();
    }

    /**
     * 设置二维插值器
     * */
    public void setInterpolator(TDInterpolator i) {
        mTDInterpolator = i;
        if (mTDInterpolator != null && isAnimating) {
            mTDInterpolator.initialize(mStartRect.exactCenterX(), mStartRect.exactCenterY(),
                    mTargetRect.exactCenterX(), mTargetRect.exactCenterY());
        }
    }

    /**
     * 设置动画插值器
     * */
    public void setInterpolator(Interpolator i) {
        mInterpolator = i;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public float getAnimationRatio() {
        return isAnimating ? mAnimationRatio : 0;
    }

    public void setDestroyAfterAnimationEnd(boolean destroy) {
        mDestoryAfter = destroy;
    }

    public void setDismissAfterAnimationEnd(boolean dismiss) {
        mDismissAfter = dismiss;
    }

    /**
     * 二维平面插值器，初始化时接收起始x,y以及目标x,y，可以任意曲线连接两点作为运动路径
     * */
    public interface TDInterpolator {
        public void initialize(float fromX, float fromY, float toX, float toY);
        public void getInterpolation(float[] pos, float ratio);
    }

    public interface AnimationCallback {
        public void onAnimationFinished();
    }

    public interface TweenValueCallback extends AnimationCallback {
        /**
         * @param animation tween value, from 0.0 to 1.0
         * */
        public void onValueChanged(float value);
    }


}
