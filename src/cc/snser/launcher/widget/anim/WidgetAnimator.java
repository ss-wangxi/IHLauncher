package cc.snser.launcher.widget.anim;

import cc.snser.launcher.widget.IScreenCtrlWidget;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.view.View;

public class WidgetAnimator implements IScreenCtrlWidget {
    
    public static final String TAG = "WidgetAnimator";
    
    private static final long DURATION = Integer.MAX_VALUE;
    private static final float FROM_ALPHA = 1.0f;
    private static final float TO_ALPHA = 0.20f;
    private static final float FROM_XSCALE = 1.0f;
    private static final float TO_XSCALE = 0.75f;
    private static final float FROM_YSCALE = 1.0f;
    private static final float TO_YSCALE = 0.75f;
    
    private View mView;
    
    private ObjectAnimator mAnimAlpha;
    private ObjectAnimator mAnimScaleX;
    private ObjectAnimator mAnimScaleY;
    private ObjectAnimator mAnimTranslationX;
    
    private Rect mInitRegion = new Rect();
    private int mScreen;
    private int mWorkspaceWidth;
    private int mMinScrollX = Integer.MIN_VALUE;
    private int mMaxScrollX = Integer.MAX_VALUE;
    
    private Rect mWidget2x3Size = new Rect();
    private Rect mWidget2x3Margin = new Rect();
    
    //workspace虚拟轴心的位置，位于workspace水平正中
    private int mPivotX;
    //workspace虚拟轴心的宽度，为2x3Widget的宽度加上2x3widget之间的视觉距离
    private int mHalfPivotWidth;
    
    private int mInitCenterXInScreen;
    private int mInitCenterX;
    
    private int mLastScrollX = Integer.MIN_VALUE;
    
    
    public WidgetAnimator(View view) {
        mView = view;
        mAnimAlpha = ObjectAnimator.ofFloat(mView, "alpha", FROM_ALPHA, TO_ALPHA).setDuration(DURATION);
        mAnimScaleX = ObjectAnimator.ofFloat(mView, "scaleX", FROM_XSCALE, TO_XSCALE).setDuration(DURATION);
        mAnimScaleY = ObjectAnimator.ofFloat(mView, "scaleY", FROM_YSCALE, TO_YSCALE).setDuration(DURATION);
    }
    
    
    /**
     * 设置动画当前的进度
     * @param progress 0.0(尺寸最大) 到 1.0(尺寸最小)
     */
    private void setProgress(float progress) {
        if (progress >= 0.0f && progress <= 1.0f) {
            long playTime = (long)(DURATION * progress);
            mAnimScaleX.setCurrentPlayTime(playTime);
            mAnimScaleY.setCurrentPlayTime(playTime);
            mAnimAlpha.setCurrentPlayTime(playTime);
            if (mAnimTranslationX != null) {
                mAnimTranslationX.setCurrentPlayTime(playTime);
            }
        }
    }
    
    public void setCenterX(int centerX) {
        mInitCenterX = centerX;
    }
    
    public float getFromXScale() {
        return FROM_XSCALE;
    }
    
    public float getToXScale() {
        return TO_XSCALE;
    }
    
    public void setTranslationX(float fromTranslationX, float toTranslationX) {
        mAnimTranslationX = ObjectAnimator.ofFloat(mView, "translationX", fromTranslationX, toTranslationX).setDuration(DURATION);
    }
    
    @Override
    public void setWorkspaceInfo(int workspaceWidth, int minScrollX, int maxScrollX) {
        mWorkspaceWidth = workspaceWidth;
        mPivotX = workspaceWidth / 2;
        mMinScrollX = minScrollX;
        mMaxScrollX = maxScrollX;
    }
    
    @Override
    public void setWidget2x3Info(Rect size, Rect margin) {
        mWidget2x3Size.set(size);
        mWidget2x3Margin.set(margin);
    }
    
    @Override
    public void setVisibleRegion(Rect region, int screen) {
        mInitRegion.set(region);
        mScreen = screen;
        mHalfPivotWidth = (mWidget2x3Size.right + mWidget2x3Margin.left) / 2;
        mInitCenterXInScreen = (mInitRegion.left + mInitRegion.right) / 2;
        mInitCenterX = mInitCenterXInScreen + mWorkspaceWidth * screen;
/*        String name = mView.getClass().getName().replace("cc.snser.launcher.widget.", "");
        name = name.split("\\.").length > 1 ? name.split("\\.")[1] : name;
        Log.d(TAG, "setRegion " + name + " s=" + screen + " wsw=" + workspaceWidth + " ww=" + mWidget2x3Size.right + " r=" + region);*/
    }
    
    @Override
    public void onWorkspaceScroll(int x, int y) {
        if (x != mLastScrollX) {
            mLastScrollX = x;
            //滑动过程中，widget中轴在workspace中的位置。当widget滑到可见时，该值在[0, mWorkspaceWidth]范围内。
            final int centerX = mInitCenterX - x;
            //滑动过程中，widget中轴离workspace虚拟轴心的距离。当widget滑到可见时，该值在[0, mHalfPivotWidth]范围内。
            final int dist = Math.abs(centerX - mPivotX);
            
            //是否为首屏的widget
            final boolean isInFirstScreen = (mScreen == 0);
            //是否为所在屏幕左侧的widget
            final boolean isInLeftScreenStatic = (mInitCenterXInScreen < mWorkspaceWidth / 2);
            //滑动时widget是否处于屏幕左侧
            final boolean isInLeftScreenDynamic = (centerX < mWorkspaceWidth / 2);
            
            if (x < mMinScrollX || x > mMaxScrollX) {
                //交由OverScrollView处理
            } else if (dist <= mHalfPivotWidth) {
                //widget的轴心如果在Pivot范围内，则不进行缩放
                setProgress(0.0f);
            } else if (dist > mWorkspaceWidth / 2 + mHalfPivotWidth) {
                //widget中轴在Workspace之外，一律缩放为最小
                setProgress(1.0f);
            } else if (isInFirstScreen && !isInLeftScreenStatic && dist > mWorkspaceWidth / 2) {
                //首屏右侧的widget，widget边缘在Workspace之外，就缩放为最小
                setProgress(1.0f);
            } else if (!isInFirstScreen && isInLeftScreenStatic && dist > mWorkspaceWidth / 2) {
                //非首屏左侧的widget，widget边缘在Workspace之外，就缩放为最小
                setProgress(1.0f);
            } else if (!isInFirstScreen && !isInLeftScreenStatic && centerX < 0) {
                //非首屏右侧的widget，中轴滑到屏幕左侧之外，就缩放为最小
                setProgress(1.0f);
            } else if (isInFirstScreen && !isInLeftScreenDynamic) {
                //首屏的widget，滑到屏幕右侧，就不进行缩放了
                setProgress(0.0f);
            } else if (centerX >= 0 && centerX <= mInitCenterXInScreen) {
                //非LeftScreenWidget，中轴滑到初始中轴左侧，进行缩小淡出动画
                final float animRange = mInitCenterXInScreen < mPivotX - mHalfPivotWidth ? mInitCenterXInScreen : mPivotX - mHalfPivotWidth;
                final float percent = (animRange - centerX) / animRange;
                setProgress(percent);
            } else if (!isInFirstScreen && isInLeftScreenDynamic) {
                //非首屏的widget，滑到屏幕左侧，就不进行缩放了
                setProgress(0.0f);
            } else if (!isInFirstScreen && !isInLeftScreenStatic) {
                //非首屏右侧的widget，应用单独的规则，根据widget中轴距离workspace右侧参考轴心的距离算比例
                final int pivotRight = mWorkspaceWidth + mHalfPivotWidth;
                final float percent = 1.0f - (pivotRight - centerX) * 1.0f / (pivotRight - mInitCenterXInScreen);
                setProgress(percent);
            } else {
                //默认规则，根据widget中轴离workspace虚拟轴心的距离算比例
                final float percent = (dist - mHalfPivotWidth) * 1.0f / (mPivotX - mHalfPivotWidth);
                setProgress(percent);
            }
        }
    }

    @Override
    public void onWorkspaceScrollStart(int currentScreen, int scrollX, int scrollY) {
    }
    
    @Override
    public void onWorkspaceScrollStop(int currentScreen, int scrollX, int scrollY) {
        setProgress(0.0f);
    }
    
    @Override
    public void onWorkspaceDragStart() {
    }
    
    @Override
    public void onWorkspaceDragStop() {
    }
    
    @Override
    public void onWidgetScreenIn() {
    }
    
    @Override
    public void onWidgetScreenOut() {
    }

    @Override
    public void onActionDown(int currentScreen) {
    }
    
    @Override
    public void onActionUp(int currentScreen) {
    }

    @Override
    public void onAccOn() {
    }

    @Override
    public void onAccOff() {
    }

}
