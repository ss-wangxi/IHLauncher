/*
 * Copyright (C) 2009 The Android Open Source Project
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

package cc.snser.launcher.widget;

import cc.snser.launcher.apps.model.workspace.LauncherAppWidgetInfo;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.widget.anim.WidgetAnimator;

import com.btime.launcher.Constants;
import com.btime.launcher.app.PackageEventReceiver;
import com.btime.launcher.app.PackageEventReceiver.IPackageEventCallback;
import com.btime.launcher.util.XLog;
import com.btime.launcher.R;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

/**
 * {@inheritDoc}
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView implements IScreenCtrlWidget, IPackageEventCallback {
    private boolean mHasPerformedLongPress;

    private CheckForLongPress mPendingCheckForLongPress;

    private LayoutInflater mInflater;

    private OnClickListener mWidgetOnClickListener;

    private Workspace mWorkspace;
    
    private String mPkgName;
    private Rect mLastRegion;
    private int mLastScreen;

    private OnClickListener mOnClickListenerProxy = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (isContainerInEditMode()) {
                if (mWorkspace == null) {
                    return;
                }
                return;
            }
            if (mWidgetOnClickListener != null) {
                mWidgetOnClickListener.onClick(v);
            }
        }
    };
    
    private WidgetAnimator mAnimator = new WidgetAnimator(this);
    
    private static boolean isWorkspaceScrolling = false;
    private static boolean isWorkspaceDraging = false;
    private static boolean isWidgetScreenIn = false;
    
    public LauncherAppWidgetHostView(Context context) {
        super(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        super.setOnClickListener(mOnClickListenerProxy);
        PackageEventReceiver.registerPackageEventCallback(this);
    }
    
    public void onRemoved() {
        PackageEventReceiver.unregisterPackageEventCallback(this);
    }
    
    @Override
    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
    	mWidgetOnClickListener = l;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }

        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (checkForLongClick()) {
                    postCheckForLongClick();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return isContainerInEditMode();
    }

    private boolean isContainerInEditMode() {
        if (Workspace.sInEditMode) {
            return true;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float x = event.getX();
                final float y = event.getY();
                if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
                    if (mPendingCheckForLongPress != null) {
                        removeCallbacks(mPendingCheckForLongPress);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if ((getParent() != null) && hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }
    
    protected boolean checkForLongClick() {
        return true;
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWorkspace = null;
        ViewParent p = getParent();
        while (p != null && p instanceof View) {
            if (p instanceof Workspace) {
                mWorkspace = (Workspace) p;
                break;
            }
            p = p.getParent();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow(); 
        mWorkspace = null;
    }
    
    @Override
    public void setTag(Object tag) {
        super.setTag(tag);
        if (tag instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo)tag;
            if (info.appWidgetCn != null && !TextUtils.isEmpty(info.appWidgetCn.getPackageName())) {
                mPkgName = info.appWidgetCn.getPackageName();
            }
        }
    }
    
    
    @Override
    public void setWorkspaceInfo(int workspaceWidth, int minScrollX, int maxScrollX) {
        mAnimator.setWorkspaceInfo(workspaceWidth, minScrollX, maxScrollX);
    }
    
    @Override
    public void setWidget2x3Info(Rect size, Rect margin) {
        mAnimator.setWidget2x3Info(size, margin);
    }
    
    @Override
    public void setVisibleRegion(Rect region, int screen) {
        mAnimator.setVisibleRegion(region, screen);
        if (!region.equals(mLastRegion) || screen != mLastScreen) {
            mLastRegion = region;
            mLastScreen = screen;
        }
        XLog.d("LauncherAppWidgetHostView", "My notify setVisibleRegion loc=" + region);
        Intent intent = new Intent(Constants.ACTION_WIDGET_LOCATION);
        intent.setPackage(mPkgName);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra("loc", new RectF(region));
        getContext().sendBroadcast(intent);
    }
    
    @Override
    public void onWorkspaceScroll(int x, int y) {
        mAnimator.onWorkspaceScroll(x, y);
    }
    
    @Override
    public void onWorkspaceScrollStart(int currentScreen, int scrollX, int scrollY) {
        synchronized (LauncherAppWidgetHostView.class) {
            if (!isWorkspaceScrolling && !isWorkspaceDraging) {
                getContext().sendBroadcast(new Intent(Constants.ACTION_WORKSPACE_OPERATE_START).setPackage(mPkgName));
                mAnimator.onWorkspaceScrollStart(currentScreen, scrollX, scrollY);
            }
            isWorkspaceScrolling = true;
        }
    }
    
    @Override
    public void onWorkspaceScrollStop(int currentScreen, int scrollX, int scrollY) {
        synchronized (LauncherAppWidgetHostView.class) {
            if (isWorkspaceScrolling && !isWorkspaceDraging) {
                getContext().sendBroadcast(new Intent(Constants.ACTION_WORKSPACE_OPERATE_STOP).setPackage(mPkgName));
                mAnimator.onWorkspaceScrollStop(currentScreen, scrollX, scrollY);
            }
            isWorkspaceScrolling = false;
        }
    }
    
    @Override
    public void onWorkspaceDragStart() {
        synchronized (LauncherAppWidgetHostView.class) {
            if (!isWorkspaceScrolling && !isWorkspaceDraging) {
                getContext().sendBroadcast(new Intent(Constants.ACTION_WORKSPACE_OPERATE_START).setPackage(mPkgName));
            }
            isWorkspaceDraging = true;
        }
    }
    
    @Override
    public void onWorkspaceDragStop() {
        synchronized (LauncherAppWidgetHostView.class) {
            if (!isWorkspaceScrolling && isWorkspaceDraging) {
                getContext().sendBroadcast(new Intent(Constants.ACTION_WORKSPACE_OPERATE_STOP).setPackage(mPkgName));
            }
            isWorkspaceDraging = false;
        }
    }
    
    @Override
    public void onWidgetScreenIn() {
        onWidgetScreenIn(false);
    }
    
    private void onWidgetScreenIn(boolean forceNotify) {
        XLog.d("LauncherAppWidgetHostView", "My notify onWidgetScreenIn forceNotify=" + forceNotify);
        if (forceNotify || !isWidgetScreenIn) {
            final Intent intent = new Intent(Constants.ACTION_WIDGET_SCREEN_IN);
            intent.setPackage(mPkgName);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            getContext().sendBroadcast(intent);
        }
        isWidgetScreenIn = true;
    }
    
    @Override
    public void onWidgetScreenOut() {
        onWidgetScreenOut(false);
    }
    
    private void onWidgetScreenOut(boolean forceNotify) {
        XLog.d("LauncherAppWidgetHostView", "My notify onWidgetScreenOut");
        if (forceNotify || isWidgetScreenIn) {
            final Intent intent = new Intent(Constants.ACTION_WIDGET_SCREEN_OUT);
            intent.setPackage(mPkgName);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            getContext().sendBroadcast(intent);
        }
        isWidgetScreenIn = false;
    }
    
    @Override
    public void onActionDown(int currentScreen) {
        getContext().sendBroadcast(new Intent(Constants.ACTION_WORKSPACE_OPERATE_BEFORE).setPackage(mPkgName));
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

    @Override
    public void onPackageEvent(Context context, String action, String pkgname) {
        if (pkgname != null && pkgname.equals(mPkgName)) {
            if (Intent.ACTION_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || PackageEventReceiver.ACTION_PACKAGE_DIED.equals(action)) {
                XLog.d("LauncherAppWidgetHostView", "My notify onPackageEvent action=" + action);
                //AppWidget进程挂掉后，再发个位置通知广播，顺便把他拉起来
                if (mLastRegion != null && mLastScreen >= 0) {
                    setVisibleRegion(mLastRegion, mLastScreen);
                }
                if (isWidgetScreenIn) {
                    onWidgetScreenIn(true);
                } else {
                    onWidgetScreenOut(true);
                }
            }
        }
    }

}
