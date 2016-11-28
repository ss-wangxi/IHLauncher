
package cc.snser.launcher.apps.components.workspace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import cc.snser.launcher.App;
import cc.snser.launcher.apps.components.AppIcon;
import cc.snser.launcher.apps.components.IconView;
import cc.snser.launcher.apps.model.workspace.LauncherWidgetViewInfo;
import cc.snser.launcher.screens.Workspace;

import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.ui.view.SlipSwitchBitmapDrawable;

public class Shortcut extends IconView implements AppIcon {
    private static final long DELAY_HANDLE_RESUME = 200;
    private Handler mHandler;

    public Shortcut(Context context) {
        super(context);
        mHandler = new Handler();
    }

    public Shortcut(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler();
    }

    @Override
    protected boolean enableCellImagePool() {
        return !ViewUtils.isHardwareAccelerated(this);
    }

    @Override
    protected boolean enableIconPressAnimation() {
        return App.getApp().getLauncher().getDragController().isDragging() == false;
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (!Workspace.sInEditMode) {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    public View getMainView() {
        return this;
    }

    @Override
    protected void afterDrawMaskFolder(Canvas canvas) {
        drawIconOnly(canvas);
    }

    public void onResume() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateIcon();
                updateLabel();
            }
        }, DELAY_HANDLE_RESUME);
    }

    public void onPause() {
        //do nothing
    }

    public void updateIcon() {
        Object obj = getTag();
        if (obj instanceof LauncherWidgetViewInfo) {
        	if ( ((LauncherWidgetViewInfo) obj).hostView != null ){
	            Drawable drawable = ((LauncherWidgetViewInfo) obj).hostView.getIconDrawable();
	            if (drawable != getIcon()) {
	                if (drawable instanceof SlipSwitchBitmapDrawable) {
	                    ((SlipSwitchBitmapDrawable) drawable).setHostView(this);
	                    setIcon(drawable);
	                    ((SlipSwitchBitmapDrawable) drawable).startTransition();
	                } else{
	                	setIcon(drawable);
	                }
	            } 
        	}
        }
    }
    
    public void updateLabel() {
    	Object obj = getTag();
    	if (obj instanceof LauncherWidgetViewInfo) {
    		if ( ((LauncherWidgetViewInfo) obj).hostView != null ){
	    		String label = ((LauncherWidgetViewInfo) obj).hostView.getLabel();
	    		if (label != null && !label.equals(getText())) {
	    			setText(label);
	    		}
    		}
    	}
    }
}
