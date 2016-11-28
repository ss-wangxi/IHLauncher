package cc.snser.launcher.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.List;

/**
 * Widget.
 * <p>
 * This is a light class of {@link WidgetView}, it only stores widget information so it can be
 * instantiated to deal with non-view logic.</p>
 *
 * @author GuoLin
 * @see BuiltinWidget
 * @see PluginWidget
 *
 */
public abstract class Widget {
    protected static final String TAG = "Launcher.Widget";

    protected Context mContext;

    /** The widget view type. */
    public final int type;

    protected Widget(Context context, int type) {
        this.mContext = context;
        this.type = type;
    }

    /**
     * Get label (name) of widget.
     * @return Widget name
     */
    public abstract String getLabel();
    
    /**
     * Get icon of widget (shows on choice list).
     * @return Widget icon
     */
    public abstract Drawable getPreview();

    /**
     * How many cells on X coordinate the wiget will occupied.
     * @return Number of cells
     */
    public abstract int getSpanX();
    public abstract void setSpanX(int spanX);

    /**
     * How many cells on Y coordinate the wiget will occupied.
     * @return Number of cells
     */
    public abstract int getSpanY();
    public abstract void setSpanY(int spanY);

    /**
     * Get widget view which used to show on workspace.
     * @param activity Activity to create widget view
     * @return Widget view
     */
    public abstract WidgetView getWidgetView(Activity activity);

    /**
     * Factory method to get widget by its type.
     * @param context The application context
     * @param type Widget view type to find
     * @return Widget instance if found, or <code>null</code> for not
     */
    //public static Widget get(Context context, int type) {
      //  return BuiltinWidget.get(context, type);
    //}

    /**
     * Get all usable widgets, including either built-in and plugin widgets.
     * @param context The application context
     * @return Widget list
     */
    //public static List<Widget> loadAll(Context context) {
      //  return BuiltinWidget.all(context);
    //}
}
