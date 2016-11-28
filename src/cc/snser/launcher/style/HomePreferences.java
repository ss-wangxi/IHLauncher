package cc.snser.launcher.style;

import cc.snser.launcher.ui.utils.SettingsConstants;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class HomePreferences {
	private SharedPreferences mSharedPreferences;
	private static final String KEY_CURRENT_THEME = "pref_k_current_theme_overall";
	private static final String KEY_WALLPAPER_STYLE = "pref_k_wallpaper_style";
	private static final String KEY_LIVE_WALLPAPER_COMPONENT = "pref_k_live_wallpaper_component";
	private static final String KEY_PATH_WALLPAPER_FILE = "pref_k_wallpaper_file";
	private static final String KEY_DEFAULT_WALLPAPER_ID = "pref_k_wallpaper_id";
	private static final String KEY_FIRST_LOAD_WORKSPACE = "pref_k_first_load";
	private static final String KEY_WALLPAPER_WIDTH = "pref_k_wallpaper_width";
	private static final String KEY_WALLPAPER_HEIGHT = "pref_k_wallpaper_height";
	public HomePreferences(SharedPreferences preferences){
		mSharedPreferences = preferences;
	}
	public int getHomeScreen(){
		return mSharedPreferences.getInt(SettingsConstants.KEY_DEFAULT_SCREEN, 0);
	}
	
	public int getHomeScreen(int defaultValue){
		return mSharedPreferences.getInt(SettingsConstants.KEY_DEFAULT_SCREEN, defaultValue);
	}
	
	public void setHomeScreen(int homeScreen){
		mSharedPreferences.edit().putInt(SettingsConstants.KEY_DEFAULT_SCREEN, homeScreen).commit();
	}
	
	public void setScreenNumber(int number){
		mSharedPreferences.edit().putInt(SettingsConstants.KEY_SCREEN_NUMBER, number).commit();
	}
	
	public void setSecondLayerScreen(int number){
		mSharedPreferences.edit().putInt(SettingsConstants.KEY_SECONDLAYER_SCREEN_NUMBER, number).commit();
	}
	
	public int getSecondLayerScreen(){
		return mSharedPreferences.getInt(SettingsConstants.KEY_SECONDLAYER_SCREEN_NUMBER, 1);
	}
	
	public int getScreenNumber(){
		return mSharedPreferences.getInt(SettingsConstants.KEY_SCREEN_NUMBER, 1);
	}
	
	public int getScreenNumber(int defaultValue){
		return mSharedPreferences.getInt(SettingsConstants.KEY_SCREEN_NUMBER, defaultValue);
	}
	
	public int getHomeLayoutType(Context context){
	    int defaultValue = DefaultPreferences.getDefaultHomeLayoutType(context);
	    return mSharedPreferences.getInt(SettingsConstants.KEY_HOME_LAYOUT_TYPE, defaultValue);
	}

	public void setHomeLayoutType(int homeLayoutType) {
        mSharedPreferences.edit().putInt(SettingsConstants.KEY_HOME_LAYOUT_TYPE, homeLayoutType).commit();
    }
	
	public int getHomeLayoutTypeSecondLayer(Context context){
	    int defaultValue = DefaultPreferences.getDefaultHomeLayoutType(context);
	    return mSharedPreferences.getInt(SettingsConstants.KEY_HOME_LAYOUT_TYPE_SECONDLAYER, defaultValue);
	}

	public void setHomeLayoutTypeSecondLayer(int homeLayoutType) {
        mSharedPreferences.edit().putInt(SettingsConstants.KEY_HOME_LAYOUT_TYPE_SECONDLAYER, homeLayoutType).commit();
    }
	
	public boolean isFirstLoad(){
		boolean first = mSharedPreferences.getBoolean(KEY_FIRST_LOAD_WORKSPACE, true);
		if(first){
			mSharedPreferences.edit().putBoolean(KEY_FIRST_LOAD_WORKSPACE, false).commit();
		}
		
		return first;
	}
	

	
	public boolean isLoopHomeScreen() {
        return mSharedPreferences.getBoolean(SettingsConstants.KEY_LOOP_HOME_SCREEN, 
        		DefaultPreferences.getLoopHomeScreenDefaultValue());
    }
    
    public void setIsLoopHomeScreen(boolean loopHomeScreen) {
    	mSharedPreferences.edit().putBoolean(SettingsConstants.KEY_LOOP_HOME_SCREEN, loopHomeScreen).commit();
    }
    
    public int getWallpaperStyle(){
    	return mSharedPreferences.getInt(KEY_WALLPAPER_STYLE, 1);
    }
    
    public void setWallpaperStyle(int style){
    	mSharedPreferences.edit().putInt(KEY_WALLPAPER_STYLE, style).commit();
    }
    
    public String getPathWallpaper(){
    	return mSharedPreferences.getString(KEY_PATH_WALLPAPER_FILE, null);
    }
    
    public void setPathWallpaper(String name){
    	mSharedPreferences.edit().putString(KEY_PATH_WALLPAPER_FILE, name).commit();
    }
    
    public String getDefaultWallpaperId(){
    	return mSharedPreferences.getString(KEY_DEFAULT_WALLPAPER_ID, null);
    }
    
    public void setDefaultWallpaperId(String id){
    	mSharedPreferences.edit().putString(KEY_DEFAULT_WALLPAPER_ID, id).commit();
    }
    
    public void setLiveWallpaper(ComponentName cn){
    	mSharedPreferences.edit().putString(KEY_LIVE_WALLPAPER_COMPONENT, cn.flattenToString()).commit();
    }
    
    public ComponentName getLiveWallpaper(){
    	String ret = mSharedPreferences.getString(KEY_LIVE_WALLPAPER_COMPONENT, null);
    	if(ret == null){
    		return null;
    	}
    	
    	return ComponentName.unflattenFromString(ret);
    }
    
    public void setWallpaperDimensions(int width, int height){
    	mSharedPreferences.edit().putInt(KEY_WALLPAPER_WIDTH, width).commit();
    	mSharedPreferences.edit().putInt(KEY_WALLPAPER_HEIGHT, height).commit();
    
    }
    
    public int[] getWallpaperDimensions(){
    	int[] dimensions = new int[2];
    	dimensions[0] = mSharedPreferences.getInt(KEY_WALLPAPER_WIDTH, 0);
    	dimensions[1] = mSharedPreferences.getInt(KEY_WALLPAPER_HEIGHT, 0);
    	
    	return dimensions;
    }
}
