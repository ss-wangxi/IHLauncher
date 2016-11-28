
package cc.snser.launcher.widget;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import cc.snser.launcher.apps.components.AppIcon;
import cc.snser.launcher.apps.components.IconPressAnimation;
import cc.snser.launcher.apps.components.IconView;

import com.btime.launcher.R;

public abstract class IconWidgetView extends WidgetView implements AppIcon, OnClickListener, OnLongClickListener {

    protected IconView mIcon;

    public IconWidgetView(Activity context) {
        this(context, null);
    }

    public IconWidgetView(Activity context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.iconview, this);

        mIcon = (IconView) findViewById(R.id.btn_icon);
        mIcon.setEnableIconPressAnimation(true);

        if (mIcon != null) {
            mIcon.setOnClickListener(this);
            mIcon.setOnLongClickListener(this);
            mIcon.setTouchEnabled(false);
        }
    }

    @Override
    public IconView getMainView() {
        return mIcon;
    }

    @Override
    public Drawable getIconDrawable() {
        return mIcon.getIcon();
    }

    @Override
    public void onClick(View v) {
        handleClickMainVew(this);
    }

    @Override
    public boolean onLongClick(View v) {
        return performLongClick();
    }



    public boolean acceptByFolder() {
        return true;
    }

    public boolean acceptByDockbar() {
        return true;
    }

    /**************************************************
     * 拦截touch，在这里做动画，而不是iconview中，避免清除缓存
     *****************************************************/
    private boolean blockUpAnimation = false;
    private boolean isPressed = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = super.dispatchTouchEvent(ev);
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (IconView.isShowIconPressAnimation) {
                    startIconPressAnimation(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                cancelLongPress();
                if (IconView.isShowIconPressAnimation) {
                    startIconPressAnimation(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelLongPress();
                blockUpAnimation = false;
                clearAnimation();
                // 动画是 fill after，因此需要再刷一次界面
                invalidate();
                break;
        }
        return handled;
    }

    private void startIconPressAnimation(boolean pressed) {
        if (pressed) {
            isPressed = true;
        } else {
            if (!isPressed || blockUpAnimation) {
                blockUpAnimation = false;
                return;
            } else {
                isPressed = false;
            }
        }
        clearAnimation();
        startAnimation(IconPressAnimation.obtain(pressed));
    }

    @Override
    public boolean performLongClick() {
        blockUpAnimation = true;
        clearAnimation();
        return super.performLongClick();
    }
}
