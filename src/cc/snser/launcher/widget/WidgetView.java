
package cc.snser.launcher.widget;

import cc.snser.launcher.themes.widget.IWidgetTheme;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * <p>
 * Abstract class for the widget view.
 * </p>
 *
 * @author huangninghai
 * @version 1.0
 */
public abstract class WidgetView extends LinearLayout {

    private static final int MESSAGE_SCREEN_OUT = 1;
    private static final int MESSAGE_SCREEN_IN = 2;
    
    private static final long SCREEN_OUT_DELAY = 60 * 1000;
    private static final long SCREEN_IN_DELAY = 400;

    protected long mWidgetId;
    private ILauncherWidgetViewInfo info;
    private IWidgetContext mWidgetContext;

    private int mOriginalWindowAttachCount;

    private boolean mHasPerformedLongPress;

    private CheckForLongPress mPendingCheckForLongPress;

    private final boolean mScrollable;

    protected static long mCustomScreenOutDelay = -1;
    protected static long mCustomScreenInDelay = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SCREEN_OUT:
                    if (mWidgetContext != null) {
                        if (mWidgetContext.isScrolling()) {
                            mHandler.sendEmptyMessageDelayed(MESSAGE_SCREEN_OUT, SCREEN_OUT_DELAY);
                            return;
                        }
                    }
                    onScreenOut();
                    break;
                case MESSAGE_SCREEN_IN:
                	if (mWidgetContext != null) {
                        if (mWidgetContext.isScrolling()) {
                            mHandler.sendEmptyMessageDelayed(MESSAGE_SCREEN_IN, SCREEN_IN_DELAY);
                            return;
                        }
                    }
                    onScreenIn();
                    break;
            }
        };
    };

    private OnClickListener mWidgetOnClickListener;
    private OnClickListener mOnClickListenerProxy = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (isContainerInEditMode() && blockClickInEditMode()) {
				return;
			}

			if (mWidgetOnClickListener != null) {
				mWidgetOnClickListener.onClick(v);
			}
		}
	};

    public WidgetView(Activity context) {
        this(context, null);
    }

    public WidgetView(Activity context, AttributeSet attrs) {
        this(context, attrs, false);
    }

    public WidgetView(Activity context, AttributeSet attrs, boolean scrollable) {
        super(context, attrs);
        this.mScrollable = scrollable;
        super.setOnClickListener(mOnClickListenerProxy);
    }
    
    public boolean scrollable() {
        return mScrollable;
    }
    
    public void setWidgetContext(IWidgetContext widgetContext){
    	mWidgetContext = widgetContext;
    }
    
    public IWidgetContext getWidgetContext() {
    	return mWidgetContext;
    }
    
    public abstract String getLabel();

    public abstract void onLauncherPause();

    public abstract void onLauncherResume();
    
    public abstract void onLauncherLoadingFinished();

    public abstract void onAdded(boolean newInstance);

    public abstract void onRemoved(boolean permanent);

    public abstract void onDestroy();

    public abstract void onScreenOn();

    public abstract void onScreenOff();

    public abstract void onCloseSystemDialogs();
    
    public abstract void onActivityResult(int requestCode, int resultCode, Intent data);
    
    public void  onHomePressed(){
    	
    }

    public void screenOut() {
        if (!handleOnScreenOutEvent()) {
            return;
        }

        mHandler.removeMessages(MESSAGE_SCREEN_OUT);
        mHandler.removeMessages(MESSAGE_SCREEN_IN);

        mHandler.sendEmptyMessageDelayed(MESSAGE_SCREEN_OUT, mCustomScreenOutDelay >= 0 ?
                mCustomScreenOutDelay : SCREEN_OUT_DELAY);
    }

    public void screenIn() {
        if (!handleOnScreenInEvent()) {
            return;
        }

        mHandler.removeMessages(MESSAGE_SCREEN_OUT);
        mHandler.removeMessages(MESSAGE_SCREEN_IN);

        mHandler.sendEmptyMessageDelayed(MESSAGE_SCREEN_IN, mCustomScreenInDelay >= 0 ?
                mCustomScreenInDelay : SCREEN_IN_DELAY);
    }

    protected boolean handleOnScreenOutEvent() {
        return false;
    }

    protected boolean handleOnScreenInEvent() {
        return false;
    }

    protected void onScreenOut() {
        // do nothing now
    }

    protected void onScreenIn() {
        // do nothing now
    }

    public void onOpenFolder() {
        // do nothing now
    }

    public void onCloseFolder() {
        // do nothing now
    }

    public void onUpdate() {
        // do nothing now
    }
    
    public void onTransparentActivityLaunched() {
        // do nothing now
    }

    public void handleClickMainVew(View v) {

    }

    public void init(ILauncherWidgetViewInfo itemInfo) {
        this.info = itemInfo;
        this.mWidgetId = itemInfo.getWidgetId();
    }

    public long getWidgetId() {
        return mWidgetId;
    }

    public String getWidgetViewType() {
        return null;
    }

    public ILauncherWidgetViewInfo getInfo() {
        return info;
    }

    public int getSpanX() {
        return 1;
    }

    public int getSpanY() {
        return 1;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after long-press is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }

        // Watch for long-press events at this level to make sure
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
        return isContainerInEditMode() && blockClickInEditMode();
    }

    protected boolean checkForLongClick() {
        return true;
    }

    private boolean isContainerInEditMode() {
        //return Workspace.sInEditMode;
    	return false;
    }

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

        @Override
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
    public void setOnClickListener(OnClickListener l) {
        mWidgetOnClickListener = l;
    }

    protected boolean blockClickInEditMode() {
        return true;
    }

    public boolean acceptByFolder() {
        return false;
    }

    public boolean acceptByDockbar() {
        return false;
    }

    public Drawable getIconDrawable() {
        return null;
    }

    public void applyTheme(IWidgetTheme theme) {
    }

    public boolean onBackPressed() {
        return false;
    }
}
