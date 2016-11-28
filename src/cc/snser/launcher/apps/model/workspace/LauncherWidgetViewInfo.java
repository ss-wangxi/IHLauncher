
package cc.snser.launcher.apps.model.workspace;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.screens.DeleteZone.Deletable;
import cc.snser.launcher.widget.ILauncherWidgetViewInfo;
import cc.snser.launcher.widget.WidgetView;

import java.io.Serializable;

/**
 * <p>
 * 自带Widget
 * Container for the widget view infomation.
 * </p>
 *
 * @author huangninghai
 * @version 1.0
 */
public class LauncherWidgetViewInfo extends HomeItemInfo implements Deletable, ILauncherWidgetViewInfo {

    public int widgetViewType;

    public Serializable identity;

    public WidgetView hostView = null;

    private boolean isDragging;

    /**
     * The intent used to find plugged in widget.
     */
    public Intent intent;

    public LauncherWidgetViewInfo(int widgetViewType, Serializable identity) {
        this.widgetViewType = widgetViewType;
        this.identity = identity;
        this.itemType = LauncherSettings.Favorites.ITEM_TYPE_WIDGET_VIEW;
    }
    
    public LauncherWidgetViewInfo(LauncherWidgetViewInfo src) {
    	super(src);
    	
    	this.widgetViewType = src.widgetViewType;
    	this.identity = src.identity;
    	this.hostView = src.hostView;
    	this.isDragging = src.isDragging;
    	this.intent = src.intent;
    }

    @Override
    public void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.APPWIDGET_ID, widgetViewType);
        String uri = intent != null ? intent.toUri(0) : null;
        values.put(LauncherSettings.Favorites.INTENT, uri);
    }

    public long getId() {
        return id;
    }
    
    @Override
    public long getWidgetId(){
    	return getId();
    }
    
    @Override
    public int getScreenIndex(){
    	return screen;
    }
    
    @Override
    public WidgetView getHostView() {
        return hostView;
    }

    @Override
    public String toString() {
        return "LauncherWidgetViewInfo(type=" + Integer.toString(widgetViewType) + ")";
    }

    @Override
    public void unbind() {
        super.unbind();
        hostView = null;
    }


    public Intent getIntent() {
        return intent;
    }


    public boolean isShortcut() {
        return false;
    }


    @Override
    public boolean isDeletable(Context context) {
        return true;
    }

    @Override
    public String getLabel(Context context) {
        return "";
    }

    @Override
    public int getDeleteZoneIcon(Context context) {
        return 0;
    }

    @Override
    public boolean onDelete(Context context) {
        return false;
    }
    
    @Override
    public LauncherWidgetViewInfo cloneSelf() {
        return new LauncherWidgetViewInfo(this);
    }
}
