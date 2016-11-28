package cc.snser.launcher.widget;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.snser.launcher.widget.calendar1x1.Calendar1x1Widget;
import cc.snser.launcher.widget.clock1x1.Clock1x1Widget;
import cc.snser.launcher.widget.setting2x3.Setting2x3Widget;
import cc.snser.launcher.widget.test2x3.Test2x3Widget;
import cc.snser.launcher.widget.traffic2x3.Traffic2x3Widget;
import cc.snser.launcher.widget.transparent1x1.Transparent1x1Widget;

import com.btime.launcher.R;
import com.btime.launcher.widget.btime1x1.BTime1x1Widget;
import com.btime.launcher.widget.camera1x1.Camera1x1Widget;
import com.btime.launcher.widget.gallery1x1.Gallery1x1Widget;
import com.btime.launcher.widget.settings1x1.Settings1x1Widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.SparseArray;

/**
 * Built-in widget.
 * <p>
 * Store information of local built-in widget.</p>
 *
 * @author GuoLin
 *
 */
public class BuiltinWidgetMgr extends BuiltinWidget {
	
    public BuiltinWidgetMgr(Context context,
			Class<? extends WidgetView> viewClass, int type, int label,
			int preview, int spanX, int spanY) {
		super(context, viewClass, type, label, preview, spanX, spanY);
	}

	private static final Map<Integer, BuiltinWidget> ALL = new LinkedHashMap<Integer, BuiltinWidget>();
	private static final SparseArray<BuiltinWidget> EXTRA = new SparseArray<BuiltinWidget>();
	
    public static BuiltinWidget get(Context context, int type) {
    	BuiltinWidget ret = getAll(context).get(type);
    	
    	if ( ret == null )
    		ret = get_switcher(context, type);
    	
    	return ret;
    }
    
    public static BuiltinWidget getExtra(Context context,int type){
    	return EXTRA.get(type);
    }

    public static List<Widget> all(Context context) {
        return new ArrayList<Widget>(getAll(context).values());
    }
    
    // 开关相关的widget
 	private static final Map<Integer, BuiltinWidget> All_Switcher = new LinkedHashMap<Integer, BuiltinWidget>();
 	public static BuiltinWidget get_switcher(Context context, int type) {
        return All_Switcher.get(type);
    }
 	
    /**
     * Get all built-in widgets.
     * <p>
     * <strong>NOTE:</strtong> Once add a built-in widget add it here.</p>
     * @param context Application context
     * @return All built-in widgets in map
     */
    public static Map<Integer, BuiltinWidget> getAll(final Context context) {
        if (ALL.isEmpty()) {
            Resources resources = context.getResources();
            int type = 0;
            
            //Transparent1x1Widget
            type = resources.getInteger(R.integer.widget_view_type_transparent1x1);
            ALL.put(type, new BuiltinWidget(context, Transparent1x1Widget.class, type,
                    R.string.widget_name_transparent1x1, R.drawable.icon_in_loading,
                    Transparent1x1Widget.SPANX, Transparent1x1Widget.SPANY));
            
            //Traffic2x3Widget
            type = resources.getInteger(R.integer.widget_view_type_traffic2x3);
            ALL.put(type, new BuiltinWidget(context, Traffic2x3Widget.class, type,
                    R.string.widget_name_traffic2x3, R.drawable.icon_in_loading,
                    Traffic2x3Widget.SPANX, Traffic2x3Widget.SPANY));

            //Setting2x3Widget
            type = resources.getInteger(R.integer.widget_view_type_setting2x3);
            ALL.put(type, new BuiltinWidget(context, Setting2x3Widget.class, type,
                    R.string.widget_name_setting2x3, R.drawable.icon_in_loading,
                    Setting2x3Widget.SPANX, Setting2x3Widget.SPANY));
            
            //Test2x3Widget
            type = resources.getInteger(R.integer.widget_view_type_test2x3);
            ALL.put(type, new BuiltinWidget(context, Test2x3Widget.class, type,
                    R.string.widget_name_test2x3, R.drawable.icon_in_loading,
                    Test2x3Widget.SPANX, Test2x3Widget.SPANY));
            
            //clock1x1_analog
            type = resources.getInteger(R.integer.widget_view_type_clock1x1);
            ALL.put(type, new BuiltinWidget(context, Clock1x1Widget.class, type,
                    R.string.widget_name_clock1x1, R.drawable.icon_in_loading,
                    Clock1x1Widget.SPANX, Clock1x1Widget.SPANY));
            
            //calendar1x1Widget
            type = resources.getInteger(R.integer.widget_view_type_calendar1x1);
            ALL.put(type, new BuiltinWidget(context, Calendar1x1Widget.class, type,
                    R.string.widget_name_calendar1x1, R.drawable.icon_in_loading,
                    Calendar1x1Widget.SPANX, Calendar1x1Widget.SPANY));
            
            //Camera1x1Widget
            type = resources.getInteger(R.integer.widget_view_type_camera1x1);
            ALL.put(type, new BuiltinWidget(context, Camera1x1Widget.class, type,
                    R.string.widget_camera1x1_name, R.drawable.icon_in_loading,
                    Camera1x1Widget.SPANX, Camera1x1Widget.SPANY));
            
            //Gallery1x1Widget
            type = resources.getInteger(R.integer.widget_view_type_gallery1x1);
            ALL.put(type, new BuiltinWidget(context, Gallery1x1Widget.class, type,
                    R.string.widget_gallery1x1_name, R.drawable.icon_in_loading,
                    Gallery1x1Widget.SPANX, Gallery1x1Widget.SPANY));
            
            //BTime1x1Widget
            type = resources.getInteger(R.integer.widget_view_type_btime1x1);
            ALL.put(type, new BuiltinWidget(context, BTime1x1Widget.class, type,
                    R.string.widget_btime1x1_name, R.drawable.icon_in_loading,
                    BTime1x1Widget.SPANX, BTime1x1Widget.SPANY));
            
            //Settings1x1Widget
            type = resources.getInteger(R.integer.widget_view_type_settings1x1);
            ALL.put(type, new BuiltinWidget(context, Settings1x1Widget.class, type,
                    R.string.widget_settings1x1_name, R.drawable.icon_in_loading,
                    Settings1x1Widget.SPANX, Settings1x1Widget.SPANY));
        }
        
        return ALL;
    }

    public static Class<? extends WidgetView> sBuiltinClockweatherViewClass;
    public static Class<? extends WidgetView> sBuiltinClockViewClass;

    static {
        try {
//            sBuiltinClockweatherViewClass = (Class<? extends WidgetView>) Class.forName("cc.snser.launcher.widget.clockweather.IntegrateClockWeatherWidget");
//            sBuiltinClockViewClass = (Class<? extends WidgetView>) Class.forName("cc.snser.launcher.widget.clock.IntegrateClockView");
        } catch (Exception e) {
            //Ignore
        }
    }

    public static boolean hasBuiltinClockweatherView() {
        return sBuiltinClockweatherViewClass != null;
    }

    public static Class<? extends WidgetView> getBuiltinClockweatherViewClass() {
        return sBuiltinClockweatherViewClass;
    }
    
    public static boolean hasBuiltinClockView(){
    	return sBuiltinClockViewClass != null;
    }
    
    public static Class<? extends WidgetView> getBuiltinClockViewClass(){
    	return sBuiltinClockViewClass;
    }
}
