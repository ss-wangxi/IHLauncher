/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.snser.launcher.ui.dragdrop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import cc.snser.launcher.CellLayout;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.RuntimeConfig;
import cc.snser.launcher.Utils;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.ui.view.ShadingBackgroundView;
import cc.snser.launcher.widget.AppWidgetResizeFrame;
import cc.snser.launcher.widget.LauncherAppWidgetHostView;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.AnimationUtils;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.DeviceUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A ViewGroup that coordinated dragging across its dscendants
 */
@SuppressLint("NewApi")
public class DragLayer extends android.widget.FrameLayout implements OnHierarchyChangeListener {
	private static final String TAG = "DragLayer";
    private DragController mDragController;

    private Launcher mLauncher;

    private MultiTouchHandler mMultiTouchHandler;

    private ShadingBackgroundView mShadingView;
    private View mFadeScreen;
    private boolean isLastHideWithAnimation;

    private View mTopView = null;
    private int mTopViewVisibility;
    
    private int mXDown, mYDown;
    
    // Variables relating to resizing widgets
    private final ArrayList<AppWidgetResizeFrame> mResizeFrames = new ArrayList<AppWidgetResizeFrame>();
    private AppWidgetResizeFrame mCurrentResizeFrame;
    private Context mContext;

    /**
     * Used to create a new DragLayer from XML.
     *
     * @param context The application's context.
     * @param attrs The attribtues set containing the Workspace's customization values.
     */
    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mMultiTouchHandler = new MultiTouchHandler();

        setOnHierarchyChangeListener(this);
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
        mMultiTouchHandler.setLauncher(launcher);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!DeviceUtils.isHuaWeiP6() || oldh == 0 || oldw == 0) { // for bugfix 270743
            return;
        }

        ScreenDimensUtils.onConfigurationChanged();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    public void setDragController(DragController controller) {
        mDragController = controller;
        mMultiTouchHandler.setDragController(controller);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mDragController.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    Rect hitRectTemp = new Rect();
    
    private boolean handleTouchDown(MotionEvent ev, boolean intercept) {
    	XLog.d(TAG, "DragLayer:handleTouchDown");
    	int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
        	for (AppWidgetResizeFrame child: mResizeFrames) {
                child.getHitRect(hitRectTemp);
                if (hitRectTemp.contains(x, y)) {
                    if (child.beginResizeIfPointInRegion(x - child.getLeft(), y - child.getTop())) {
                    	XLog.d(TAG, "DragLayer:handleTouchDown,set the current resize frame!");
                    	
                        mCurrentResizeFrame = child;
                        mXDown = x;
                        mYDown = y;
                        requestDisallowInterceptTouchEvent(true);
                        return true;
                    }else{
                    	XLog.d(TAG, "DragLayer:handleTouchDown,not begin resize pointer in region");
                    }
                }else{
                	XLog.d(TAG, "DragLayer:handleTouchDown,not hit");
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	if (ev.getAction() == MotionEvent.ACTION_DOWN) {
    		XLog.d(TAG, "DragLayer:onInterceptTouchEvent,go to handleTouchDown");
            if (handleTouchDown(ev, true)) {
                return true;
            }
        }
    	
    	XLog.d(TAG, "DragLayer:onInterceptTouchEvent,clear all resize frames");
    	
        clearAllResizeFrames();
    	mMultiTouchHandler.handleTouchEvent(ev);
        boolean result = mDragController.onInterceptTouchEvent(ev);
        if(result == false ) {
            checkEmptyAreaInEditMode(ev);
        }
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	XLog.d(TAG, "DragLayer:onTouchEvent");
    	
    	boolean handled = false;
        int action = ev.getAction();

        int x = (int) ev.getX();
        int y = (int) ev.getY();

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (handleTouchDown(ev, false)) {
                return true;
            }
        }
        
        
        
        if (mCurrentResizeFrame != null) {
            handled = true;
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    mCurrentResizeFrame.visualizeResizeForDelta(x - mXDown, y - mYDown);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mCurrentResizeFrame.visualizeResizeForDelta(x - mXDown, y - mYDown);
                    mCurrentResizeFrame.onTouchUp();
                    mCurrentResizeFrame = null;
            }
        }else{
        	XLog.d(TAG, "mCurrentResizeFrame == NULL");
        }
        if (handled) return true;
    	
        return mDragController.onTouchEvent(ev);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return mDragController.dispatchUnhandledMove(focused, direction);
    }

    private boolean mCheckActionUp = false;
    private void checkEmptyAreaInEditMode(MotionEvent ev){
        if(ev == null) return;

        if(ev.getAction() == MotionEvent.ACTION_DOWN){
            if (mLauncher.getWorkspace().isInEditMode()){
                mCheckActionUp = true;
            }
            return;
        }

        if(mCheckActionUp && ev != null && mLauncher.getWorkspace().isInEditMode() && ev.getAction() == MotionEvent.ACTION_UP) {
            mCheckActionUp = false;
            if(!mLauncher.getWorkspace().isScrolling()){
                if(mLauncher.getCurrentWorkspaceScreen() == 0 ||
                        mLauncher.getCurrentWorkspaceScreen() == mLauncher.getWorkspace().getChildCount()-1){
                    int realWdith = ScreenDimensUtils.getScreenRealWidth(getContext());
                    int centerWidth = (int) (Workspace.sEditModeScaleRatio * realWdith);
                    if (ev.getRawX() < (realWdith - centerWidth) / 2 && mLauncher.getCurrentWorkspaceScreen() == 0 
                    		&& ev.getRawY() < ScreenDimensUtils.getScreenRealHeight(getContext()) * Workspace.sEditModeScaleRatio) {
                    } else if (ev.getRawX() > centerWidth + (realWdith - centerWidth) / 2 && mLauncher.getCurrentWorkspaceScreen() == mLauncher.getWorkspace().getChildCount()-1 && 
                    		ev.getRawY() < ScreenDimensUtils.getScreenRealHeight(getContext()) * Workspace.sEditModeScaleRatio){
                    }
                }
            }
        }
    }

    private WeakReference<Bitmap> mForegroundReference = null;

    private final HashMap<View, Integer> mVisibilityBackup = new HashMap<View, Integer>();


    /**********************************
     * 屏幕最左右的指示条
     *********************************/
    private ImageView mMoveLeftScreenBar;
    private ImageView mMoveRightScreenBar;

    private boolean mRemoveLeftBarRunning = false;

    private boolean mRemoveRightBarRunning = false;

    public void hideMoveLeftScreenBar() {
        if (mMoveLeftScreenBar != null) {
            mMoveLeftScreenBar.setVisibility(View.GONE);

            if (RuntimeConfig.sLauncherInTouching || mRemoveLeftBarRunning) {
                return;
            }

            mRemoveLeftBarRunning = true;
            postDelayed(new Runnable() {

                @Override
                public void run() {
                    mRemoveLeftBarRunning = false;

                    if (mMoveLeftScreenBar.getVisibility() != View.GONE) {
                        return;
                    }

                    DragLayer.this.removeView(mMoveLeftScreenBar);
                    mMoveLeftScreenBar = null;
                }
            }, 500);
        }
    }

    public void hideMoveRightScreenBar() {
        if (mMoveRightScreenBar != null) {
            mMoveRightScreenBar.setVisibility(View.GONE);

            if (RuntimeConfig.sLauncherInTouching || mRemoveRightBarRunning) {
                return;
            }

            mRemoveRightBarRunning = true;
            postDelayed(new Runnable() {

                @Override
                public void run() {
                    mRemoveRightBarRunning = false;

                    if (mMoveRightScreenBar.getVisibility() != View.GONE) {
                        return;
                    }

                    DragLayer.this.removeView(mMoveRightScreenBar);
                    mMoveRightScreenBar = null;
                }
            }, 500);
        }
    }

    public void showMoveLeftScreenBar() {
        if (mMoveLeftScreenBar == null) {
             View.inflate(mContext, R.layout.move_to_left_screen_bar, this);
             mMoveLeftScreenBar = (ImageView) findViewById(R.id.move_to_left_screen_bar);
        }
        updateMoveScreenBarLayoutParam(mMoveLeftScreenBar, true);

        mMoveLeftScreenBar.setVisibility(View.VISIBLE);
    }

    public void showMoveRightScreenBar() {
        if (mMoveRightScreenBar == null) {
            View.inflate(mContext, R.layout.move_to_right_screen_bar, this);
            mMoveRightScreenBar = (ImageView) findViewById(R.id.move_to_right_screen_bar);
        }
        updateMoveScreenBarLayoutParam(mMoveRightScreenBar, false);

        mMoveRightScreenBar.setVisibility(View.VISIBLE);
    }

    private void updateMoveScreenBarLayoutParam(View view, boolean isLeft) {
    	FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, (isLeft ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP);
            view.setLayoutParams(layoutParams);
        }
        if (Workspace.sInEditMode) {
            int centerY = CellLayout.getSmartTopPadding();
            layoutParams.height = (int) Utils.scaleTo(getHeight(), centerY, Workspace.sEditModeScaleRatio);
        } else {
            layoutParams.height = LayoutParams.FILL_PARENT;
        }
    }

    /**********************************
     * 屏幕最左右的指示条 end
     *********************************/

    public void clearCache() {
        if (mForegroundReference == null) {
            return;
        }

        Bitmap b = mForegroundReference.get();
        if (b != null) {
            BitmapUtils.recycleBitmap(b);
            mForegroundReference.clear();
            mForegroundReference = null;
        }
    }

    public void clearAllResizeFrames() {
        if (mResizeFrames.size() > 0) {
            for (AppWidgetResizeFrame frame: mResizeFrames) {
                frame.commitResize();
                removeView(frame);
            }
            mResizeFrames.clear();
        }
    }

    public boolean hasResizeFrames() {
        return mResizeFrames.size() > 0;
    }

    public boolean isWidgetBeingResized() {
        return mCurrentResizeFrame != null;
    }

    public void addResizeFrame(LauncherAppWidgetHostView widget,
            CellLayout cellLayout) {
        AppWidgetResizeFrame resizeFrame = new AppWidgetResizeFrame(getContext(), widget, cellLayout, this);

        LayoutParams lp = new LayoutParams(-1, -1);
        //lp.customPosition = true;

        addView(resizeFrame, lp);
        mResizeFrames.add(resizeFrame);

        resizeFrame.snapToWidget(false);
    }

    @Override
    public void addView(View child) {
        this.addView(child, -1, null, false, true, false);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        this.addView(child, -1, params, false, true, false);
    }

    public void addView(View child, boolean hasPadding, boolean hasMargin, boolean needWrapper) {
        this.addView(child, -1, null, hasPadding, hasMargin, needWrapper);
    }

    public void addView(View child, int index, boolean hasPadding, boolean hasMargin, boolean needWrapper) {
        this.addView(child, index, null, hasPadding, hasMargin, needWrapper);
    }

    public void addView(View child, ViewGroup.LayoutParams params, boolean hasPadding, boolean hasMargin, boolean needWrapper) {
        this.addView(child, -1, params, hasPadding, hasMargin, needWrapper);
    }

    private void addView(View child, int index, ViewGroup.LayoutParams params, boolean hasPadding, boolean hasMargin, boolean needWrapper) {
        if (RuntimeConfig.sGlobalBottomPadding != 0) {
            if (hasPadding) {
                if (params == null) {
                    super.addView(child, index);
                } else {
                    super.addView(child, index, params);
                }

                child.setPadding(child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(), child.getPaddingBottom() + RuntimeConfig.sGlobalBottomPadding);
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) child.getLayoutParams();
                if (layoutParams.height != FrameLayout.LayoutParams.MATCH_PARENT && layoutParams.height != FrameLayout.LayoutParams.WRAP_CONTENT) {
                    layoutParams.height = layoutParams.height + RuntimeConfig.sGlobalBottomPadding;
                }
            } else if (hasMargin) {
                if (needWrapper) {
                    child = ViewWrapper.generateNewView(this.getContext(), child, params);

                    super.addView(child, index);
                } else {
                    if (params == null) {
                        super.addView(child, index);
                    } else {
                        super.addView(child, index, params);
                    }

                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) child.getLayoutParams();
                    layoutParams.bottomMargin = layoutParams.bottomMargin + RuntimeConfig.sGlobalBottomPadding;
                }
            } else {
                if (params == null) {
                    super.addView(child, index);
                } else {
                    super.addView(child, index, params);
                }
            }
        } else {
            if (params == null) {
                super.addView(child, index);
            } else {
                super.addView(child, index, params);
            }
        }
    }

    public void addViewWithAnim(View view, boolean withAnim) {
        final View child = view;
        if (withAnim) {
            Animation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
            alphaAnim.setAnimationListener(new AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    addView(child);
                }
            });
            view.startAnimation(alphaAnim);
        } else {
            addView(child);
        }
    }

    @Override
    public void removeView(View view) {
        if (view.getParent() instanceof ViewWrapper) {
            super.removeView((ViewWrapper) view.getParent());
        } else {
            super.removeView(view);
        }
    }

    public void removeViewWithAnim(View view, boolean withAnim) {
        final View child = view;
        if (withAnim) {
            Animation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
            alphaAnim.setAnimationListener(new AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    removeView(child);
                }
            });
            view.startAnimation(alphaAnim);
        } else {
            removeView(child);
        }
    }

    public void restoreCurrentScreen(boolean animation) {
        restoreCurrentScreen(animation, 300);
    }

    public void restoreCurrentScreen(boolean animation, int dura)  {
        if (mLauncher == null) {
            return;
        }

        int duration = animation && isLastHideWithAnimation ? dura : 0;

        View v = mLauncher.getBottomArea();
        if (v != null) {
            v.setVisibility(View.VISIBLE);
            AnimationUtils.playAlphaAnimation(v, 0, 1, duration, null);
        }

        if (mFadeScreen != null) {
            mFadeScreen.setVisibility(View.VISIBLE);
            AnimationUtils.playAlphaAnimation(mFadeScreen, 0, 1, duration, null);
            mFadeScreen = null;
        }

        v = mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentScreen());
        if (v != null) {
            v.setVisibility(View.VISIBLE);
            AnimationUtils.playAlphaAnimation(v, 0, 1, duration, null);
        }

        if (mShadingView != null) {
            final View shading = mShadingView;
            Runnable endAction = new Runnable() {
                public void run() {
                    shading.clearAnimation();
                    removeView(shading);
                }
            };
            AnimationUtils.playAlphaAnimation(shading, 1f, 0, duration, endAction);
            // ensure removing shading view
            postDelayed(endAction, duration);
            mShadingView = null;
        }

        isLastHideWithAnimation = false;
        mVisibilityBackup.clear();

    }

    public void setTopView(View topView) {
        if (topView == null || topView.getParent() == this) {
            mTopView = topView;
        }
    }

    public void clearTopView(View topView) {
        if (topView == mTopView) {
            mTopView = null;
        }
    }

    private void setTopViewVisibilityBeforeBlur() {
        View topView = getTopView();
        if (topView != null) {
            mTopViewVisibility = topView.getVisibility();
            if (topView.getVisibility() == View.VISIBLE) {
                topView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setTopViewVisibilityAfterBlur() {
        View topView = getTopView();
        if (topView != null) {
            topView.setVisibility(mTopViewVisibility);
        }
    }

    public View getShadingView() {
        return mShadingView;
    }

    @Override
    public void bringChildToFront(View child) {
        super.bringChildToFront(child);
        bringTopViewToFront(mTopView);
        bringDragViewToFront(child);
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        bringTopViewToFront(mTopView);
        bringDragViewToFront(child);
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        if (child == mTopView) {
            mTopView = null;
        }
    }

    public View getTopView() {
        return mTopView;
    }

    private void bringTopViewToFront(View topView) {
        if (topView != null && topView.getParent() == this) {
            super.bringChildToFront(topView);
        }
    }

    private void bringDragViewToFront(View child) {
        if (mDragController != null && mDragController.isDragging() && mDragController.getDragObject() != null) {
            DragView v = mDragController.getDragObject().dragView;
            if (v != null && v.getParent() == this && v != child && v.getType() == DragView.TYPE_DRAG_VIEW) {
                super.bringChildToFront(v);
            }
        }
    }

    public boolean isHidingCurrentScreen() {
        return mFadeScreen != null;
    }

    private static class ViewWrapper extends FrameLayout {
        public ViewWrapper(Context context) {
            super(context);
        }

        public static ViewWrapper generateNewView(Context context, View child, ViewGroup.LayoutParams params) {
            ViewWrapper wrapper = new ViewWrapper(context);

            if (params == null) {
                wrapper.addView(child);
            } else {
                wrapper.addView(child, params);
            }

            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) child.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.bottomMargin + RuntimeConfig.sGlobalBottomPadding;

            View view = new View(context);
            view.setBackgroundColor(0xff000000);

            layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, RuntimeConfig.sGlobalBottomPadding);
            layoutParams.gravity = Gravity.BOTTOM;
            view.setLayoutParams(layoutParams);

            wrapper.addView(view);

            return wrapper;
        }
    }
    
    public static class LayoutParams extends FrameLayout.LayoutParams {
        //private int x, y;
        public boolean customPosition = false;

        public LayoutParams(int width, int height) {
            super(width, height);
        }
        
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.leftMargin = x;
        }

        public int getX() {
            return this.leftMargin;
        }

        public void setY(int y) {
            this.topMargin = y;
        }

        public int getY() {
            return this.topMargin;
        }
    }
}