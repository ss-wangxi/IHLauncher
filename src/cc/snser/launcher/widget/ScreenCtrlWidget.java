package cc.snser.launcher.widget;

import cc.snser.launcher.widget.anim.WidgetAnimator;

import com.btime.launcher.adapter.ChannelLayoutAdapter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;

public class ScreenCtrlWidget extends WidgetView implements IScreenCtrlWidget {
    public static final String TAG = "ScreenCtrlWidget";
    
    protected Rect mInitRegion = new Rect();
    protected int mScreen;
    protected Rect mWidget2x3Size = new Rect();
    protected Rect mWidget2x3Margin = new Rect();
    
    protected WidgetAnimator mAnimator;

    public ScreenCtrlWidget(Activity context) {
        super(context);
        if (ChannelLayoutAdapter.isSupportScrollAnim(context)) {
            mAnimator = new WidgetAnimator(this);
        }
    }
    
    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public void onAdded(boolean newInstance) {
    }

    @Override
    public void onRemoved(boolean permanent) {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onScreenOn() {
    }

    @Override
    public void onScreenOff() {
    }

    @Override
    public void onCloseSystemDialogs() {
    }
    
    @Override
    protected boolean checkForLongClick() {
        return true;
    }

    @Override
    public boolean acceptByFolder() {
        return false;
    }
    
    @Override
    public void onLauncherPause() {
    }

    @Override
    public void onLauncherResume() {
    }
    
    @Override
    public void onLauncherLoadingFinished() {
    }
    
    @Override
    public void onHomePressed() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
    
    @Override
    public void setWorkspaceInfo(int workspaceWidth, int minScrollX, int maxScrollX) {
        if (mAnimator != null) {
            mAnimator.setWorkspaceInfo(workspaceWidth, minScrollX, maxScrollX);
        }
    }
    
    @Override
    public void setWidget2x3Info(Rect size, Rect margin) {
        mWidget2x3Size.set(size);
        mWidget2x3Margin.set(margin);
        if (mAnimator != null) {
            mAnimator.setWidget2x3Info(size, margin);
        }
    }
    
    @Override
    public void setVisibleRegion(Rect region, int screen) {
        mInitRegion.set(region);
        mScreen = screen;
        if (mAnimator != null) {
            mAnimator.setVisibleRegion(region, screen);
        }
    }
    
    @Override
    public void onWorkspaceScroll(int x, int y) {
        if (mAnimator != null) {
            mAnimator.onWorkspaceScroll(x, y);
        }
    }
    
    @Override
    public void onWorkspaceScrollStart(int currentScreen, int scrollX, int scrollY) {
        if (mAnimator != null) {
            mAnimator.onWorkspaceScrollStart(currentScreen, scrollX, scrollY);
        }
    }
    
    @Override
    public void onWorkspaceScrollStop(int currentScreen, int scrollX, int scrollY) {
        if (mAnimator != null) {
            mAnimator.onWorkspaceScrollStop(currentScreen, scrollX, scrollY);
        }
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
        if (mAnimator != null) {
            mAnimator.onActionDown(currentScreen);
        }
    }
    
    @Override
    public void onActionUp(int currentScreen) {
        if (mAnimator != null) {
            mAnimator.onActionUp(currentScreen);
        }
    }
    
    @Override
    public void onAccOn() {
    }

    @Override
    public void onAccOff() {
    }
    
}
