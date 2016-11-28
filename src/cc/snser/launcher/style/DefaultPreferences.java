package cc.snser.launcher.style;

import android.content.Context;
import cc.snser.launcher.ui.effects.EffectFactory;

import com.btime.launcher.R;

public class DefaultPreferences {
	
	private static int sDefaultHomeLayoutType = Integer.MIN_VALUE;
	public static int getDefaultHomeScreen(int style){
		return 0;
	}
	
	public static synchronized int getDefaultHomeLayoutType(Context context) {
		if (sDefaultHomeLayoutType == Integer.MIN_VALUE) {
			sDefaultHomeLayoutType = context.getResources().getInteger(R.integer.default_home_layout_type);
		}
	    return sDefaultHomeLayoutType;
	}
	
	public static boolean getLoopHomeScreenDefaultValue() {
		return false;
	}
	
	public static int getHomeScreenTransformationDefaultType() {
        return EffectFactory.TYPE_CLASSIC;
    }
}
