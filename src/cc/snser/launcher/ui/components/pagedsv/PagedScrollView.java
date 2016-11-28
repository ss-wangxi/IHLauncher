package cc.snser.launcher.ui.components.pagedsv;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.RuntimeConfig;
import cc.snser.launcher.reflect.FieldUtils;
import cc.snser.launcher.screens.WorkspaceCellLayout;
import cc.snser.launcher.ui.dragdrop.DragController;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.DeviceUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

import mobi.intuitit.android.widget.WidgetSpace;
import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * 分页滚动显示视图容器
 * @author songzhaochun
 *
 */
public class PagedScrollView extends WidgetSpace implements GestureDetector.OnGestureListener, OnHierarchyChangeListener {

    public static final String TAG = "Launcher.PagedScrollView";

    private ArrayList<PageSwitchListener> mScrollListeners;

    private CacheHandler mCacheHandler;
    //剁掉下拉悬停
    protected final boolean mDisablePullPause = true;

    //TOUCH_STATE_SCROLLING时也向Listener通知Moveing回调（此时确实有Moving）
    //此消息可能会导致onPageMoving调用的频繁，因此默认关闭
    private boolean mNotifyMovingInScrolling;

    protected PagedScrollViewScroller mScroller;

    /**
     * 默认状态
     */
    protected static final int TOUCH_STATE_REST = 0;

    /**
     * 正在滑动
     */
    protected static final int TOUCH_STATE_SCROLLING = 1;

    /**
     * 向下手势
     */
    protected static final int TOUCH_STATE_SWIPE_DOWN_GESTURE = 2;

    /**
     * 向上手势
     */
    protected static final int TOUCH_STATE_SWIPE_UP_GESTURE = 3;

    /**
     * widget滚动手势
     */
    protected static final int TOUCH_STATE_VIEW_SCROLLING_GESTURE = 4;

    /**
     * 触屏状态
     */
    protected int mTouchState = TOUCH_STATE_REST;

    public static final int DIRECTION_RIGHT = 1;

    public static final int DIRECTION_LEFT = -1;

    protected int mMoveDirection;

    protected GestureDetector mGestureDetector;

    protected int mWidth;

    private float mDownMotionX;

    private long mDownTime = 0;

    private float mLastMotionX;

    private float mLastMotionY;

    private float mLastMotionRawY;

    protected int mTouchSlop;

    private boolean mCanLoopScreen = false;

    protected PagedInterpolatorStrategy.BaseInterpolator mScrollInterpolator;

    // for second finger fling
    private GestureDetector mSecondGestureDetector;

    private int mSecondFingerState = TOUCH_STATE_REST;

    private float mLastSecondMotionX = 0;

    private float mLastSecondMotionY = 0;

    protected DragController mDragController;

    static final float NANOTIME_DIV = 1000000000.0f;
    private static final float SMOOTHING_SPEED = 0.75f;
    static final float SMOOTHING_CONSTANT = (float) (0.016 / Math.log(SMOOTHING_SPEED));
    float mSmoothingTime;
    float mTouchX;

    protected boolean mTouchDownAbort = false;
    protected boolean mTouchDownAbortScroll = false; // 滑动时用户按下时停止动画，保持状�

    private int mChildCount;

    protected int mOverScrollX = 0;
    private boolean mIgnoreFireSwipeUp = false;

    public PagedScrollView(Context context) {
        this(context, null);
    }

    public PagedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(true);
        setOnHierarchyChangeListener(this);

        // 特工机deovo V5的划屏运动时每帧渲染消耗时间不均匀，因此采用更加陡峭的运动曲线，避免看起来卡顿
        // songzhaochun, 2012.11.15
        if (DeviceUtils.isDeovoV5()) {
            mScrollInterpolator = new PagedInterpolatorStrategy.ScrollInterpolatorForDeovoV5();
        } else {
            mScrollInterpolator = PagedInterpolatorStrategy.getInterpolator();
        }

        mScroller = new PagedScrollViewScroller(getContext(), mScrollInterpolator);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        mGestureDetector = new GestureDetector(configuration, this);

        mScrollListeners = new ArrayList<PagedScrollView.PageSwitchListener>();
    }

    public void setDragController(DragController dragController) {
        mDragController = dragController;
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);

        mTouchX = x;
        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
    }

    @Override
    public void onFling(int directionX, int directionY, int velocityX, int velocityY) {
        if (mTouchState == TOUCH_STATE_SCROLLING) {
            handleFling(directionX, velocityX, velocityY, false);
        } else if (mTouchState == TOUCH_STATE_SWIPE_DOWN_GESTURE) {
            //fireSwipeDownAction();
            handleFling(directionY, velocityX, velocityY, false);
        } else if (mTouchState == TOUCH_STATE_SWIPE_UP_GESTURE) {
//        	if(!mDisablePullPause){
        		View v = getChildAt(getCurrentScreen());
        		if(v instanceof WorkspaceCellLayout){
        			WorkspaceCellLayout layout = (WorkspaceCellLayout)v;
                    if(layout.getScrollY() == 0 && !mIgnoreFireSwipeUp) {
                        fireSwipeUpAction();
                    } else {
                        handleFling(directionY, velocityX, velocityY, false);
                    }
        		}
//        	}
        }
    }

    protected int fixFlingVelocity(int velocity) {
        return velocity;
    }

    /**
     * handleFling gesture
     *
     * @param direction
     * @param velocityX
     * @param velocityY
     * @param ignore may ignore by {@link #canFlingToScroll}
     */
    protected void handleFling(int direction, int velocityX, int velocityY, boolean ignore) {
        final int screenWidth = mWidth;
        final int whichScreen = (int) FloatMath.floor((getScrollX() + (screenWidth / 2)) * 1.0F / screenWidth);
        final int childCount = getChildCount();

        int flingScreen = 1;
        if (isCanScrollMoreScreen()) {
            int flingDistance = mScroller.getFlingDistance(fixFlingVelocity(velocityX), fixFlingVelocity(velocityY));
            flingScreen = Math.abs((int) FloatMath.floor(((getScrollX() + (screenWidth / 2)) * 1.0F + flingDistance) / screenWidth) - whichScreen);
            flingScreen = Math.max(1, flingScreen);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "fling screen is:" + flingScreen + " fling distance is:" + flingDistance);
            }
        }

        if (direction == GestureDetector.FLING_LEFT) {
            // Fling hard enough to move left.
            if (mCurrentScreen > 0) {
                int targetScreen = Math.max(0, whichScreen < mCurrentScreen ? whichScreen - flingScreen + 1 : whichScreen - flingScreen);
                flingToScroll(targetScreen, velocityX, false, ignore, DragController.SCROLL_LEFT);
            } else if (mCurrentScreen == 0) {
                if (this.isCanLoopScreen()) {
                    if (getScrollX() > 0 && getScrollX() < screenWidth * childCount - screenWidth) {
                        flingToScroll(mCurrentScreen, velocityX, true, ignore, DragController.SCROLL_LEFT);
                    } else {
                        flingToScroll(childCount - 1, velocityX, true, ignore, DragController.SCROLL_LEFT);
                    }
                } else {
                    flingToScroll(mCurrentScreen, velocityX, false, ignore, DragController.SCROLL_LEFT);
                }
            }
        } else if (direction == GestureDetector.FLING_RIGHT) {
            // Fling hard enough to move right
            if (mCurrentScreen < childCount - 1) {
                int targetScreen = Math.min(getChildCount() - 1, whichScreen > mCurrentScreen ? whichScreen + flingScreen - 1 : whichScreen + flingScreen);
                flingToScroll(targetScreen, velocityX, false, ignore, DragController.SCROLL_RIGHT);
            } else if (mCurrentScreen == childCount - 1) {
                if (this.isCanLoopScreen()) {
                    if (getScrollX() > 0 && getScrollX() < screenWidth * childCount - screenWidth) {
                        flingToScroll(mCurrentScreen, velocityX, true, ignore, DragController.SCROLL_RIGHT);
                    } else {
                        flingToScroll(0, velocityX, true, ignore, DragController.SCROLL_RIGHT);
                    }
                } else {
                    flingToScroll(mCurrentScreen, velocityX, false, ignore, DragController.SCROLL_RIGHT);
                }
            }
        } else if (direction == GestureDetector.FLING_DOWN) {
        	if(!mDisablePullPause){
        		View view = getChildAt(getCurrentScreen());
	        	if(view instanceof WorkspaceCellLayout){
	        		WorkspaceCellLayout layout = (WorkspaceCellLayout)view;
	            	layout.flingTo(false);
	                layout.setChildAlpha(false);
	        	}
        	}
        	
        } else if (direction == GestureDetector.FLING_UP) {
        	if(!mDisablePullPause){
        		View view = getChildAt(getCurrentScreen());
            	if(view instanceof WorkspaceCellLayout){
            		WorkspaceCellLayout layout = (WorkspaceCellLayout)view;
                    layout.flingTo(true);
                    layout.setChildAlpha(false);
            	}	
        	}
        } else {
            if (velocityX > 0) {
                if (whichScreen > mCurrentScreen || whichScreen < 0 || whichScreen >= childCount) {
                    if (this.isCanLoopScreen()) {
                        flingToScroll((whichScreen + childCount) % childCount, 0, true, ignore, DragController.SCROLL_RIGHT);
                    } else {
                        flingToScroll(mCurrentScreen, 0, false, ignore, DragController.SCROLL_RIGHT);
                    }
                } else {
                    flingToScroll(whichScreen, 0, false, ignore, DragController.SCROLL_RIGHT);
                }
            } else {
                if (whichScreen < mCurrentScreen || whichScreen < 0 || whichScreen >= childCount) {
                    if (this.isCanLoopScreen()) {
                        flingToScroll((whichScreen + childCount) % childCount, 0, true, ignore, DragController.SCROLL_LEFT);
                    } else {
                        flingToScroll(mCurrentScreen, 0, false, ignore, DragController.SCROLL_LEFT);
                    }
                } else {
                    flingToScroll(whichScreen, 0, false, ignore, DragController.SCROLL_LEFT);
                }
            }
        }
    }

    private void flingToScroll(int whichScreen, int velocity, boolean isSnapDirectly, boolean ignore, int direction) {
        if (ignore) {
            if (!canScroll(mCurrentScreen, direction)) {
                return;
            }
        }
        scrollToScreen(whichScreen, velocity, false, isSnapDirectly);
    }

    protected boolean canScroll(int currentScreen, int direction) {
        return true;
    }

    /**
     * 设置页面切换监听者
     * @param listener
     */
    public void addPageSwitchListener(PageSwitchListener listener) {
        if (mScrollListeners != null) {
            mScrollListeners.add(listener);
            if (LOGD_ENABLED) {
                XLog.d(TAG, "added " + listener + ", and current listeners are " + mScrollListeners);
            }
        }
    }

    public int getMoveDirection() {
        return mMoveDirection;
    }

    /**
     * Returns the count of the screen
     *
     * @return The count of the screen
     */
    public int getScreenCount() {
        return getChildCount();
    }

    /**
     * Returns the index of the last displayed screen.
     *
     * @return The index of the last displayed screen.
     */
    public int getLastScreen() {
        return mLastScreen;
    }

    /**
     * Returns the index of the currently displayed screen.
     *
     * @return The index of the currently displayed screen.
     */
    public int getCurrentScreen() {
        return mCurrentScreen;
    }

    public int getCurrentWidth() {
        return mWidth;
    }

    /**
     * Sets the current screen.
     *
     * @param currentScreen
     */
    public void setCurrentScreen(int currentScreen) {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }

        mNextScreen = currentScreen;

        if (LOGD_ENABLED) {
            XLog.d(TAG, "setCurrentScreen mNextScreen = " + mNextScreen + ", mCurrentScreen = " + mCurrentScreen);
        }

        if (!mScrollListeners.isEmpty()) {
            for (PageSwitchListener listener : mScrollListeners) {
                listener.onPageFrom(mCurrentScreen);
            }
        }

        finishScreen();

        scrollTo(mCurrentScreen * mWidth, 0);

        if (!mScrollListeners.isEmpty()) {
            for (PageSwitchListener listener : mScrollListeners) {
                listener.onPageTo(mCurrentScreen);
            }
        }

        // 设置滑动状态
        RuntimeConfig.sLauncherInScrolling = false;
    }

    public boolean isScrolling() {
        final PagedScrollViewScroller scroller = mScroller;
        return ((scroller != null && !scroller.isFinished()) || mTouchState == TOUCH_STATE_SCROLLING);
    }

    public void scrollToScreen(int whichScreen) {
        scrollToScreen(whichScreen, false);
    }
    
    public void scrollToScreen(int whichScreen, boolean isSnapDirectly) {
        scrollToScreen(whichScreen, 0, false, isSnapDirectly);
    }

    protected int getScrollToScreenDuration(int whichScreen, int velocity, boolean settle, boolean isSnapDirectly) {
        final int current = mCurrentScreen;
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        final int childCount = getChildCount();

        int scrollX = getScrollX();

        if (isSnapDirectly) {
            final int width = mWidth;
            if (whichScreen == childCount - 1 && scrollX <= 0) {
                scrollX += width * childCount;
            } else if (whichScreen == 0 && scrollX >= width * childCount - width) {
                scrollX -= width * childCount;
            }
        }

        final int screenDelta = isSnapDirectly ? Math.min(1, Math.abs(whichScreen - current)) : Math.max(1, Math.abs(whichScreen - current));
        final int newX = whichScreen * mWidth;
        final int delta = newX - scrollX;

        int duration = PagedScrollStrategy.getScrollDuration(whichScreen, velocity, settle, isSnapDirectly, this, screenDelta, delta);

        // 特工机 MANUFACTURER: BOVO, MODEL: S-F16 时间调短，以便看起来快
        if (DeviceUtils.isBOVO()) {
            duration = duration * 80 / 100;
        }

        duration = fixDuration(duration);
        return duration;
    }

    public void scrollToScreen(int whichScreen, int velocity, boolean settle, boolean isSnapDirectly) {
        final int current = mCurrentScreen;

        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));

        enableChildrenCache(current, whichScreen, isSnapDirectly);

        mNextScreen = whichScreen;

        final int childCount = getChildCount();

        int scrollX = getScrollX();

        if (isSnapDirectly) {
            final int width = mWidth;
            if (whichScreen == childCount - 1 && scrollX <= 0) {
                scrollX += width * childCount;
            } else if (whichScreen == 0 && scrollX >= width * childCount - width) {
                scrollX -= width * childCount;
            }
        }

        final int screenDelta = isSnapDirectly ? Math.min(1, Math.abs(whichScreen - current)) : Math.max(1, Math.abs(whichScreen - current));
        final int newX = whichScreen * mWidth;
        final int delta = newX - scrollX;

        int duration = getScrollToScreenDuration(whichScreen, velocity, settle, isSnapDirectly);

        if (mMoveDirection == 0) {
            if (delta > 0) {
                mMoveDirection = DIRECTION_RIGHT;
            } else if (delta < 0) {
                mMoveDirection = DIRECTION_LEFT;
            }
        }

        if (settle) {
            mScrollInterpolator.setDistance(screenDelta);
        } else {
            mScrollInterpolator.disableSettle();
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "snapToScreen page=" + whichScreen + ", velocity=" + velocity + ", deltaX=" + delta + ", newX=" + newX + ", duration=" + duration);
        }

        final PagedScrollViewScroller scroller = mScroller;
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }

        // 设置滑动状态
        RuntimeConfig.sLauncherInScrolling = false;

        scroller.startScroll(scrollX, 0, delta, 0, duration);
        invalidate();
        
        if (mTouchState == TOUCH_STATE_REST) {
            for (PageSwitchListener listener : mScrollListeners) {
                listener.onPageFrom(mCurrentScreen);
            }
        }
    }

    protected int fixDuration(int duration) {
        return duration;
    }

    /**
     * 设置缓存机制实现者
     * @param handler
     */
    public void setCacheHandler(CacheHandler handler) {
        mCacheHandler = handler;
    }

    /**
     * 立即完成页面切换
     */
    protected void finishScreen() {
        if (mNextScreen != INVALID_SCREEN) {
            mNextScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));
            if (mNextScreen != mCurrentScreen) {
                for (PageSwitchListener listener : mScrollListeners) {
                    listener.onPageSwitched(mCurrentScreen, mNextScreen);
                }
                
                resetScreenVerticalScroller();
                
                mLastScreen = mCurrentScreen;
                mCurrentScreen = mNextScreen;
            }

            clearChildrenCache(false, false, false);
            mNextScreen = INVALID_SCREEN;

            mMoveDirection = 0;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }

        if (this.getMeasuredWidth() != mWidth) {
            mWidth = this.getMeasuredWidth();

            setHorizontalScrollBarEnabled(false);
            scrollTo(mCurrentScreen * mWidth, 0);
            setHorizontalScrollBarEnabled(true);
        }
    }

    @Override
    public void computeScroll() {
        boolean ret = computeScrollHelper();

        if (PagedScrollStrategy.ableToComputeScroll() && !ret && mTouchState == TOUCH_STATE_SCROLLING) {
            PagedScrollStrategy.computeScroll(this, mTouchX, getScrollX());
        }

    }

    protected boolean computeScrollHelper() {
        final PagedScrollViewScroller scroller = mScroller;

        if (scroller.computeScrollOffset()) {
            PagedScrollStrategy.offsetScroll(this, scroller.getCurrX());

            // 设置滑动状态
            RuntimeConfig.sLauncherInScrolling = true;

            return true;
        } else if (mNextScreen != INVALID_SCREEN) {
            if (LOGD_ENABLED) {
                XLog.d(TAG, "computeScroll mNextScreen = " + mNextScreen + ", mCurrentScreen = " + mCurrentScreen);
            }

            for (PageSwitchListener scrollListener : mScrollListeners) {
                scrollListener.onPageFrom(mCurrentScreen);
            }

            finishScreen();

            if (mTouchDownAbort) {
                PagedScrollStrategy.offsetScroll(this, getScrollX());
            } else {
                PagedScrollStrategy.offsetScroll(this, mCurrentScreen * mWidth);
            }
            mTouchDownAbort = false;
            mTouchDownAbortScroll = false;

            for (PageSwitchListener scrollListener : mScrollListeners) {
                scrollListener.onPageTo(mCurrentScreen);
            }

            // 设置滑动状态
            RuntimeConfig.sLauncherInScrolling = false;

            return true;
        }

        // 设置滑动状态
        RuntimeConfig.sLauncherInScrolling = mTouchState == TOUCH_STATE_SCROLLING;

        return false;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // ViewGroup.dispatchDraw() supports many features we don't need:
        // clip to padding, layout animation, animation listener, disappearing
        // children, etc. The following implementation attempts to fast-track
        // the drawing dispatch by drawing only what we know needs to be drawn.
        final long drawingTime = getDrawingTime();
        final int currentScreen = mCurrentScreen;
        final int nextScreen = mNextScreen;

        clearAnimationGroupFlags();

        boolean fastDraw = mTouchState != TOUCH_STATE_SCROLLING && nextScreen == INVALID_SCREEN;
        // If we are not scrolling or flinging, draw only the current screen
        if (fastDraw) {
            drawChild(canvas, getChildAt(currentScreen), drawingTime);
        } else {
            final float scrollPos = (float) getScrollX() / mWidth;
            final int leftScreen = (int) FloatMath.floor(scrollPos);
            final int rightScreen = leftScreen + 1;
            int drawLeftScreenIndex = Integer.MIN_VALUE;
            int drawRightScreenIndex = Integer.MIN_VALUE;
            boolean drawLeftFirst = true;

            final boolean useMoveDirectionOrder = drawChildrenOrderByMoveDirection(getChildAt(leftScreen));

            if (useMoveDirectionOrder) {
                if ((leftScreen == currentScreen && rightScreen == nextScreen) || (isScrolling() && getMoveDirection() == DIRECTION_RIGHT)) { // 从右向左�
                    int index = leftScreen;
                    if (index < 0 || index >= getChildCount()) {
                        index = 0;
                    }

                    if (Math.abs(getCurrentScrollRadio(getChildAt(index), 0)) < 0.5) {
                        drawLeftFirst = false;
                    }
                } else if ((leftScreen == nextScreen && rightScreen == currentScreen) || (isScrolling() && getMoveDirection() == DIRECTION_LEFT)) {
                    int index = rightScreen;
                    if (index < 0 || index >= getChildCount()) {
                        index = getChildCount() - 1;
                    }

                    if (Math.abs(getCurrentScrollRadio(getChildAt(index), 0)) > 0.5) {
                        drawLeftFirst = false;
                    }
                }
            }

            if (drawLeftFirst) {
                drawLeftScreenIndex = dispatchDrawLeft(canvas, leftScreen, drawLeftScreenIndex, drawingTime);
                if (scrollPos != drawLeftScreenIndex) { // 画右�
                    drawRightScreenIndex = dispatchDrawRight(canvas, rightScreen, drawLeftScreenIndex, drawingTime);
                }
            } else {
                if (scrollPos != leftScreen) { // 画右�
                    drawRightScreenIndex = dispatchDrawRight(canvas, rightScreen, drawLeftScreenIndex, drawingTime);
                }
                drawLeftScreenIndex = dispatchDrawLeft(canvas, leftScreen, drawLeftScreenIndex, drawingTime);
            }

            if (useMoveDirectionOrder) {
                if ((leftScreen == currentScreen && rightScreen == nextScreen) || (isScrolling() && getMoveDirection() == DIRECTION_RIGHT)) { // 从右向左�
                    setChildDrawingOrder(drawRightScreenIndex, true);
                    setChildDrawingOrder(drawLeftScreenIndex, false);
                } else if ((leftScreen == nextScreen && rightScreen == currentScreen) || (isScrolling() && getMoveDirection() == DIRECTION_LEFT)) { // 从左向右�
                    setChildDrawingOrder(drawRightScreenIndex, true);
                    setChildDrawingOrder(drawLeftScreenIndex, false);
                }
            }
        }

        invalidateIfAnimationGroupFlagsChanged();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == null) {
            return true;
        }
        if (child.getVisibility() != View.VISIBLE) {
            if (child.getAnimation() == null) {
                return true;
            }
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void setChildDrawingOrder(int index, boolean reverse) {
        if (index < 0 || index >= getChildCount()) {
            return;
        }

        View child = getChildAt(index);
        if (child instanceof CellLayout) {
            ((CellLayout) child).reverseChildDrawingRowOrder(reverse);
        }
    }

    /**
     * 返回true时，dispatchDraw会根据滑动方向决定child的绘制顺序，而不一定是从左往右
     * @return
     */
    protected boolean drawChildrenOrderByMoveDirection(View childView) {
        return false;
    }

    private int dispatchDrawLeft(Canvas canvas, int leftScreen, int drawScreen, final long drawingTime) {
        if (leftScreen >= 0 && leftScreen < getChildCount()) { // 正常�
            drawScreen = leftScreen;
            drawChild(canvas, getChildAt(drawScreen), drawingTime);
        } else if (this.isCanLoopScreen()) { // 循环滑屏�
            if (getChildCount() > 0) {
                drawScreen = getChildCount() - 1; // 左屏为最右一�
                final int width = mWidth;

                canvas.translate(-width * this.getChildCount(), 0);
                drawChild(canvas, getChildAt(drawScreen), drawingTime);
                canvas.translate(width * this.getChildCount(), 0);
            }
        }
        return drawScreen;
    }

    private int dispatchDrawRight(Canvas canvas, int rightScreen, int drawScreen, final long drawingTime) {
        int screen = -1;
        if (rightScreen >= 0 && rightScreen < getChildCount()) { // 正常�
            if (drawScreen != rightScreen) {
                drawChild(canvas, getChildAt(rightScreen), drawingTime);
                screen = rightScreen;
            }
        } else if (this.isCanLoopScreen()) { // 循环滑屏�
            if (getChildCount() > 0) {
                if (drawScreen != 0) {
                    final int width = mWidth;

                    canvas.translate(width * this.getChildCount(), 0);
                    drawChild(canvas, getChildAt(0), drawingTime);
                    canvas.translate(-width * this.getChildCount(), 0);

                    screen = 0;
                }
            }
        }
        return screen;
    }

    protected float getCurrentScrollRadio(View childView, int offset) {
        return 0f;
    }

    protected float getCurrentScrollRadio(int childIndex, int childWidth, int offset) {
        return 0f;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (filterTouchEvent()) {
                return false;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void enableCurrentScreenCache() {
        final int count = getChildCount();
        if (this.mCurrentScreen >= 0 && this.mCurrentScreen < count) {
            if (mCacheHandler != null) {
                mCacheHandler.enableCache(this.mCurrentScreen);
            }
        }
    }

    /**
     * 更新缓存
     * @param state
     */
    public void enableChildrenCache(int fromScreen, int toScreen, boolean isSnapDirectly) {
        if (fromScreen > toScreen) {
            final int temp = fromScreen;
            fromScreen = toScreen;
            toScreen = temp;
        }

        final int count = getChildCount();

        if (count == 0) {
            return;
        }

        for (int i = fromScreen; i <= toScreen; i++) {
            if (!isSnapDirectly || i == fromScreen || i == toScreen) {
                if (mCacheHandler != null) {
                    mCacheHandler.enableCache((i + count) % count);
                }
            }
        }
    }

    /**
     * 释放缓存
     *
     * @param state
     */
    public void clearChildrenCache(boolean immediately, boolean all, boolean destroyCache) {
        if (mCacheHandler != null) {
            mCacheHandler.clearCache(immediately, all, destroyCache);
        }
    }

    /**
     * @return True is long presses are still allowed for the current touch
     */
    public boolean allowLongPress() {
        return mAllowLongPress;
    }

    /**
     * Set true to allow long-press events to be triggered, usually checked by
     * {@link Launcher} to accept or block dpad-initiated long-presses.
     */
    public void setAllowLongPress(boolean allowLongPress) {
        mAllowLongPress = allowLongPress;
    }

    protected boolean filterTouchEvent() {
        return false;
    }

    protected boolean isViewAtLocationScrollable(int x, int y) {
        return false;
    }

    protected void fireSwipeDownAction() {
    }

    protected void fireSwipeUpAction() {
    }

    protected void fireDoubleClickAction() {
    }

    protected boolean isCanLoopScreen() {
        return mCanLoopScreen && mChildCount > 1;
    }

    protected boolean isCanOverScrollScreen() {
        return true;
    }

    protected boolean isCanScrollMoreScreen() {
        return false;
    }

    public void setCanLoopScreen(boolean canLoopScreen) {
        mCanLoopScreen = canLoopScreen;
    }

    public boolean isNeedScrollSlowlyOnTheVerge() {
        return false;
    }

    protected void handleTouchDown(final MotionEvent ev) {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }

        mDownMotionX = mLastMotionX = ev.getX();
        mLastMotionY = ev.getY();
        if (mTouchState == TOUCH_STATE_SCROLLING) {
            mTouchX = getScrollX();
            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
            enableChildrenCache(mCurrentScreen - 1, mCurrentScreen + 1, false);
        }
    }

    /**
     * 移动处理
     * @param event
     */
    protected void handleTouchMove(final MotionEvent ev) {
        if (mTouchState == TOUCH_STATE_SCROLLING) {
            // Scroll to follow the motion event
            final float x = ev.getX();
            float deltaX = mLastMotionX - x;
            final int width = mWidth;
            mOverScrollX = (int) (x - mDownMotionX);
            float touchX = -1;
            if (deltaX < 0) {
                float availableToScroll = width + getScrollX();
                if (!this.isCanLoopScreen()) {
                    if (isCanOverScrollScreen()) {
                        availableToScroll -= width * 5 / 9;
                    } else {
                        availableToScroll -= width;
                    }
                }
                if (availableToScroll > 0) {
                    deltaX = -Math.min(availableToScroll, -deltaX);
                } else {
                    return;
                }
            } else if (deltaX > 0) {
                float availableToScroll = width * getChildCount() - getScrollX();
                if (!this.isCanLoopScreen()) {
                    if (isCanOverScrollScreen()) {
                        availableToScroll -= width * 5 / 9;
                    } else {
                        availableToScroll -= width;
                    }
                }
                if (availableToScroll > 0) {
                    deltaX = Math.min(availableToScroll, deltaX);
                } else {
                    return;
                }
            }

            mLastMotionX = x;
            mLastMotionY = ev.getY();

            if (mDownMotionX - x < 0) {
                mMoveDirection = DIRECTION_LEFT;
            } else if (mDownMotionX - x > 0) {
                mMoveDirection = DIRECTION_RIGHT;
            } else {
                mMoveDirection = 0;
            }

            if (!mScrollListeners.isEmpty()) {
                final int whichScreen = (int) FloatMath.floor((getScrollX() + (mWidth / 2)) * 1.0F / mWidth);
                for (PageSwitchListener listener : mScrollListeners) {
                    listener.onPageMoving(mCurrentScreen, whichScreen, mMoveDirection);
                }
            }
            if ((getScrollX() < 0 || getScrollX() > (getChildCount() - 1 ) * getWidth()) && !isCanLoopScreen() && isNeedScrollSlowlyOnTheVerge()) {
                touchX = deltaX * 0.1f;
            } else {
                touchX = deltaX * 1.5f;
            }
            mTouchX += touchX;
            if (!isCanScrollMoreScreen()) {
                mTouchX = Math.max(Math.min(mTouchX, (mCurrentScreen + 1) * mWidth), (mCurrentScreen - 1) * mWidth);
            }
            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;

            if (PagedScrollStrategy.handleTouchMoveScrollBy()) {
                scrollBy((int) deltaX, 0);
            } else {
                invalidate();
            }
        } else if (mTouchState == TOUCH_STATE_SWIPE_DOWN_GESTURE || mTouchState == TOUCH_STATE_SWIPE_UP_GESTURE) {
        	final float y = ev.getRawY();
            float deltaY = mLastMotionY - y;
            int scrolly = getChildAt(getCurrentScreen()).getScrollY();
            
            if(!mDisablePullPause){
            	View v = getChildAt(getCurrentScreen());
                if(v instanceof WorkspaceCellLayout){
                	WorkspaceCellLayout layout = (WorkspaceCellLayout)v;
                	if(deltaY < 0) {
                    	layout.scrollBy(0, (-scrolly-deltaY < getHeight() / 2.5) ? (int)deltaY : 0);
                    } else {
                    	if(scrolly < 0 ) {
                    	    layout.scrollBy(0, (scrolly + deltaY > getHeight() / 6) ? 0 : (int) deltaY);
                    	}
                    }
                    if(scrolly != 0) {
                    	mIgnoreFireSwipeUp = true;
                    }

                    layout.setChildAlpha(false);
                }	
            }
            
            
            mLastMotionY = y;
        }else {
			handleInterceptTouchMove(ev);
		}
    }

    /**
     * 释放处理
     * @param event
     */
    protected void handleTouchUp() {
        mGestureDetector.release();
        if(!mDisablePullPause){
        	if( getChildAt(getCurrentScreen()) instanceof WorkspaceCellLayout) {
                WorkspaceCellLayout layout = (WorkspaceCellLayout) getChildAt(getCurrentScreen());
                if(mTouchState == TOUCH_STATE_SWIPE_DOWN_GESTURE && layout.getScrollY() < -(getHeight() / 3) ||
            		    mTouchState == TOUCH_STATE_SWIPE_UP_GESTURE && layout.getScrollY() > 0) {
            	    layout.springBack();
                }
            }
        }
        
        
        mTouchState = TOUCH_STATE_REST;
        mTouchDownAbort = false;
        mTouchDownAbortScroll = false;
        // 设置滑动状态
        RuntimeConfig.sLauncherInScrolling = false;
        mIgnoreFireSwipeUp = false;
    }

    /**
     * 取消处理
     * @param event
     */
    protected void handleTouchCancel() {
        if (mTouchState == TOUCH_STATE_SCROLLING) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            final int screenWidth = mWidth;
            final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
            scrollToScreen(whichScreen, 0, false, false);
        }

        mGestureDetector.release();

        mTouchState = TOUCH_STATE_REST;
        mTouchDownAbort = false;
        mTouchDownAbortScroll = false;
        // 设置滑动状态
        RuntimeConfig.sLauncherInScrolling = false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /* if (LOGD_ENABLED) {
            XLog.d(TAG, "onInterceptTouchEvent action=" + ev.getAction() + ", x=" + ev.getX() + ", y=" + ev.getY());
        }*/

        if (filterTouchEvent()) {
            return false; // We don't want the events. Let them fall through to the child view.
        }

        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging state
         * and he is moving his finger. We want to intercept this motion.
         */
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_MOVE) {
            if (mTouchState == TOUCH_STATE_VIEW_SCROLLING_GESTURE) {
                return false;
            } else if (mTouchState != TOUCH_STATE_REST) {
                return true;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                handleInterceptTouchDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                handleInterceptTouchMove(ev);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleInterceptTouchUpOrCancel();
                break;

            default:
                break;
        }

        /* if (LOGD_ENABLED) {
            XLog.d(TAG, "onInterceptTouchEvent touch state=" + mTouchState);
        }*/

        if (mTouchState == TOUCH_STATE_SCROLLING) {
            for (PageSwitchListener listener : mScrollListeners) {
                listener.onPageFrom(mCurrentScreen);
            }
        }

        return mTouchState != TOUCH_STATE_REST && mTouchState != TOUCH_STATE_VIEW_SCROLLING_GESTURE;
    }

    /**
     * 按下处理
     * @param event
     */
    protected void handleInterceptTouchDown(final MotionEvent ev) {
        mDownMotionX = mLastMotionX = ev.getX();
        mLastMotionY = ev.getY();
        mLastMotionRawY = ev.getRawY();

        mAllowLongPress = true;

        final boolean isScrollerStoped = mScroller.isFinished();
        boolean isFinishedScroll = isScrollerStoped || Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) < mTouchSlop / 2;
        if (!isScrollerStoped) {
//        	setScrollXReflect(mScroller.getCurrX());
            setScrollX(mScroller.getCurrX());
            mScroller.abortAnimation();
        }

        long now = System.currentTimeMillis();
        mTouchDownAbort = (now - mDownTime > 500);
        mTouchDownAbortScroll = !isFinishedScroll && (now - mDownTime > 500);
        mDownTime = now;

        mTouchState = isFinishedScroll ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;

        mGestureDetector.forwardTouchEvent(ev);
    }

    /**
     * 评估，并（立即或跟踪）到
     * @param event
     */
    protected void handleInterceptTouchMove(final MotionEvent ev) {
        final int currentScreen = mCurrentScreen;
        final float x = ev.getX();
        final float y = ev.getY();
        final int xDiff = (int) Math.abs(x - mLastMotionX);
        final int yDiff = (int) Math.abs(y - mLastMotionY);
        final int yRawDiff = (int) (ev.getRawY() - mLastMotionRawY);

        final int touchSlop = mTouchSlop;
        final boolean xMoved = xDiff > touchSlop;
        final boolean yMoved = Math.abs(yRawDiff) > touchSlop;

        if (xMoved || yMoved) {
            final boolean mTouchedScrollableWidget = isViewAtLocationScrollable((int) mLastMotionX, (int) mLastMotionY);

            if (xDiff > yDiff && (mTouchedScrollableWidget ? !yMoved : true)) {
                mTouchState = TOUCH_STATE_SCROLLING;
                mLastMotionX = x;
                mLastMotionY = y;

                mTouchX = getScrollX();
                mSmoothingTime = System.nanoTime() / NANOTIME_DIV;

                // 更新缓存
                enableChildrenCache(currentScreen - 1, currentScreen + 1, false);
            } else {
                if (!mTouchedScrollableWidget) {
                    if (!xMoved) {
                        // Only y axis movement. So may be a Swipe down
                        // or up gesture
                        if (yRawDiff > 0) {
                            if (yRawDiff > (touchSlop * 2)) {
                                mTouchState = TOUCH_STATE_SWIPE_DOWN_GESTURE;
                            }
                        } else {
                            if (-yRawDiff > (touchSlop * 2)) {
                                mTouchState = TOUCH_STATE_SWIPE_UP_GESTURE;
                            }
                        }
                    }

                    if (mTouchState == TOUCH_STATE_REST && xMoved) {
                        mTouchState = TOUCH_STATE_SCROLLING;
                        mLastMotionX = x;
                        mLastMotionY = y;

                        mTouchX = getScrollX();
                        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;

                        // 更新缓存
                        enableChildrenCache(currentScreen - 1, currentScreen + 1, false);
                    }
                } else {
                    mTouchState = TOUCH_STATE_VIEW_SCROLLING_GESTURE;
                }
            }

            // Either way, cancel any pending longpress
            if (mAllowLongPress) {
                mAllowLongPress = false;
                // Try canceling the long press. It could also have been
                // scheduled by a distant descendant, so use the
                // mAllowLongPress
                // flag to block everything
                final View currentScreenChild = getChildAt(currentScreen);
                if (currentScreenChild != null) {
                    currentScreenChild.cancelLongPress();
                }
            }
        }

        mGestureDetector.forwardTouchEvent(ev);
    }

    /**
     * 释放处理
     * @param event
     */
    protected void handleInterceptTouchUpOrCancel() {
        clearChildrenCache(false, false, false);

        mTouchState = TOUCH_STATE_REST;

        mAllowLongPress = false;

        mGestureDetector.release();

        // 设置滑动状态
        RuntimeConfig.sLauncherInScrolling = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        /*if (LOGD_ENABLED) {
            XLog.d(TAG, "onTouchEvent action=" + ev.getAction() + ", x=" + ev.getX() + ", y=" + ev.getY());
        }*/

        if (filterTouchEvent()) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            scrollToScreen(mCurrentScreen);
            return false; // We don't want the events. Let them fall through to the children view.
        }

        mGestureDetector.forwardTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                handleTouchMove(ev);
                break;

            case MotionEvent.ACTION_UP:
                handleTouchUp();
                break;

            case MotionEvent.ACTION_CANCEL:
                handleTouchCancel();
                break;
        }

        /*if (LOGD_ENABLED) {
            XLog.d(TAG, "onTouchEvent touch state=" + mTouchState);
        }*/

        return true;
    }

    public float getMotionYRadio() {
        float radioY = (mLastMotionY - this.getTop()) / this.getHeight();
        return radioY > 1F ? 1F : radioY;
    }

    /**
     * 页面切换监听者
     * @author songzhaochun
     *
     */
    public interface PageSwitchListener {
        /**
         * 切换到下一页
         * @param currentPage
         */
        void onPageFrom(int currentPage);
        /**
         * 切换到下一页
         * @param currentPage
         */
        void onPageTo(int currentPage);
        /**
         * 开始切换到下一�
         * @param currentPage
         * @param nextPage
         */
        void onPageMoving(int orignalPage, int currentPage, int direction);

        /**
         * 切换完成
         * @param previousPage
         * @param currentPage
         */
        void onPageSwitched(int previousPage, int currentPage);
    }

    /**
     * 缓存实现策略
     * @author songzhaochun
     *
     */
    public interface CacheHandler {
        void enableCache(int childIndex);
        void clearCache(boolean immediately, boolean all, boolean destroyCache);
    }

    public void handleSecondFingerGusture(final MotionEvent ev) {
        if (ev.getPointerCount() != 2 || !ableToHandleSecondFinger()) {
            return;
        }

        if (mSecondGestureDetector == null) {
            final ViewConfiguration configuration = ViewConfiguration.get(getContext());
            mSecondGestureDetector = new GestureDetector(configuration,
                    new cc.snser.launcher.ui.components.pagedsv.GestureDetector.OnGestureListener() {

                        @Override
                        public void onFling(int directionX, int directionY, int velocityX, int velocityY) {
                            if (mSecondFingerState == TOUCH_STATE_SCROLLING) {
                                handleFling(directionX, velocityX, velocityY, true);
                            }
                        }
                    });
        }

        mSecondGestureDetector.forwardSecondeTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastSecondMotionX = ev.getX(1);
                mLastSecondMotionY = ev.getY(1);
                mSecondFingerState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;
            case MotionEvent.ACTION_MOVE:
                final float x = ev.getX(1);
                final float y = ev.getY(1);
                final int xDiff = (int) Math.abs(x - mLastSecondMotionX);
                final int yDiff = (int) Math.abs(y - mLastSecondMotionY);

                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;
                if (xMoved || yMoved) {
                    if (xDiff > yDiff) {
                        mSecondFingerState = TOUCH_STATE_SCROLLING;
                    }
                }

                mLastSecondMotionX = x;
                mLastSecondMotionY = y;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mLastSecondMotionX = 0;
                mLastSecondMotionY = 0;
                mSecondFingerState = TOUCH_STATE_REST;
                mSecondGestureDetector.release();
                break;
            default:
                break;
        }
    }

    protected boolean ableToHandleSecondFinger() {
        return false;
    }

    /**
     * 只为{@link PagedScrollStrategy}}使用
     */
    protected void setPagedScrollX(int scrollX) {
    	setScrollX(scrollX);
        mTouchX = getScrollX();

        if(RuntimeConfig.sLauncherInScrolling && mNotifyMovingInScrolling){
            if (!mScrollListeners.isEmpty()) {
                final int whichScreen = (int) FloatMath.floor((getScrollX() + (mWidth / 2)) * 1.0F / mWidth);
                for (PageSwitchListener listener : mScrollListeners) {
                    listener.onPageMoving(mCurrentScreen, whichScreen, mMoveDirection);
                }
            }
        }
    }

    private Field mGroupFlagsField;

    private void clearAnimationGroupFlags() {
        try {
        	int groupFlags = getFieldValue(FIELD_GROUP_FLAGS);
        	groupFlags &= ~0x4;
        	setFieldValue(FIELD_GROUP_FLAGS, groupFlags);
            
        } catch (Throwable t) {
            try {
                if (mGroupFlagsField == null) {
                    mGroupFlagsField = ViewGroup.class.getDeclaredField("mGroupFlags");
                }
                boolean access = mGroupFlagsField.isAccessible();
                mGroupFlagsField.setAccessible(true);
                int flag = mGroupFlagsField.getInt(this);
                flag &= ~0x4;
                mGroupFlagsField.setInt(this, flag);
                mGroupFlagsField.setAccessible(access);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void invalidateIfAnimationGroupFlagsChanged() {
        try {
            if ((getFieldValue(FIELD_GROUP_FLAGS) & 0x4) == 0x4) {
                invalidate();
            }
        } catch (Throwable t) {
            try {
                if (mGroupFlagsField == null) {
                    mGroupFlagsField = ViewGroup.class.getDeclaredField("mGroupFlags");
                }
                boolean access = mGroupFlagsField.isAccessible();
                mGroupFlagsField.setAccessible(true);
                int flag = mGroupFlagsField.getInt(this);
                mGroupFlagsField.setAccessible(access);
                if ((flag & 0x4) == 0x4) {
                    invalidate();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        mChildCount++;
        if (DeviceUtils.isJellyBean() && !DeviceUtils.isMiOne()) {
            ViewUtils.setLayerType(child, ViewUtils.LAYER_TYPE_HARDWARE);
        }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        mChildCount--;
        if (DeviceUtils.isJellyBean() && !DeviceUtils.isMiOne()) {
            ViewUtils.destroyHardwareLayer(child);
        }
    }
    
    protected void resetScreenVerticalScroller() {
    	if(!mDisablePullPause){
    		if( getChildAt(getCurrentScreen()) instanceof WorkspaceCellLayout) {
                WorkspaceCellLayout layout = (WorkspaceCellLayout) getChildAt(getCurrentScreen());
                if(layout != null) {
                    layout.scrollTo(0, 0);
                }
        	}	
    	}
    }

    public void setPageMovingInScrolling(boolean notifyMovingInScrolling){
        mNotifyMovingInScrolling = notifyMovingInScrolling;
    }
    
    
    private final String FIELD_GROUP_FLAGS = "mGroupFlags";
    private final String FIELD_SCROLL_X = "mScrollX";
    
    
    protected int getFieldValue(final String fieldName){
    	try {
    		return (int)(Integer)FieldUtils.readField(this, fieldName);
		} catch (Exception e) {
		}
    	return 0;
    }
    
    protected void setFieldValue(final String fieldName,int value) {
    	try {
    		FieldUtils.writeField(this, fieldName, value);
		} catch (Exception e) {
		}
	}
}