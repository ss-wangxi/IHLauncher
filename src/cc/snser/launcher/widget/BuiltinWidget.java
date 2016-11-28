package cc.snser.launcher.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in widget.
 * <p>
 * Store information of local built-in widget.</p>
 *
 * @author GuoLin
 *
 */
public class BuiltinWidget extends Widget {
    private final int mLabel;

    private final int mPreview;

    private int mSpanX;
    private int mSpanY;

    private final Class<? extends WidgetView> mViewClass;

    public BuiltinWidget(Context context, Class<? extends WidgetView> viewClass,
            int type, int label, int preview, int spanX, int spanY) {
        super(context.getApplicationContext(), type);
        this.mViewClass = viewClass;
        this.mLabel = label;
        this.mPreview = preview;
        this.mSpanX = spanX;
        this.mSpanY = spanY;
    }

    @Override
    public String getLabel() {
        return mContext.getString(mLabel);
    }

    @Override
    public Drawable getPreview() {
        return mContext.getResources().getDrawable(mPreview);
    }

    @Override
    public int getSpanX() {
        return mSpanX;
    }

    @Override
    public int getSpanY() {
        return mSpanY;
    }

    @Override
    public WidgetView getWidgetView(Activity activity) {
        try {
            Constructor<?> constructor = mViewClass.getConstructor(Activity.class);
            return (WidgetView) constructor.newInstance(activity);
        } catch (Exception e) {
            //XLog.e(TAG, "Create widget failed.", e);
        }
        return null;
    }

	@Override
	public void setSpanX(int spanX) {
		mSpanX = spanX;
	}

	@Override
	public void setSpanY(int spanY) {
		mSpanY = spanY;
	}
}
