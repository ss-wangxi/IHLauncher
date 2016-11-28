package cc.snser.launcher.style;


import cc.snser.launcher.App;
import cc.snser.launcher.support.settings.GestureSettings;
import cc.snser.launcher.ui.utils.SettingsConstants;

import com.btime.launcher.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class SettingPreferences {
	private static HomePreferences sThunderPreferences;
	private static HomeCommonPreferences sCommonPreferences;
	
	private static final String SUFFIX_THUNDER = "_preferences_thunder";
	private static final String SUFFIX_COMMON = "_preferences_common";
	

	
	private static String[] sLayouts;
	private static int sHomeScreenTransformationType = Integer.MIN_VALUE;
	private static HomePreferences getHomePreferences(){
		if(sThunderPreferences == null){
			sThunderPreferences = new HomePreferences(getSharedPreferences(SUFFIX_THUNDER));
		}

		return sThunderPreferences;
	}
	
	public static HomePreferences getHomePreferences(int style){
		if(sThunderPreferences == null){
			sThunderPreferences = new HomePreferences(getSharedPreferences(SUFFIX_THUNDER));
		}
		return sThunderPreferences;
	}
	
	public static HomeCommonPreferences getCommonPreferences(){
		if(sCommonPreferences == null){
			sCommonPreferences = new HomeCommonPreferences(getSharedPreferences(SUFFIX_COMMON));
		}
		
		return sCommonPreferences;
	}
	
	private static synchronized SharedPreferences getSharedPreferences(String suffix){
		Context context = App.getApp();
		return context.getSharedPreferences(context.getPackageName() + suffix, Context.MODE_PRIVATE);
	}
	
	public static synchronized int getHomeScreen(){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.getHomeScreen();
		}
		return 0;
	}
	
	public static synchronized int getHomeScreen(int defaultValue){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.getHomeScreen(defaultValue);
		}
		
		return 0;
	}
	
	public static void setHomeScreen(int homeScreen){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			homePreferences.setHomeScreen(homeScreen);
		}	
	}
	
	public static void setScreenNumber(int number){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			homePreferences.setScreenNumber(number);
		}
	}
	
	public static synchronized int getScreenNumber(int defaultValue){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.getScreenNumber(defaultValue);
		}
		
		return 1;
	}

	
	public static int getHomeLayoutType(Context context){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.getHomeLayoutType(context);
		}
		
		return 0;
	}

	public static void setHomeLayoutType(int homeLayoutType) {
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			homePreferences.setHomeLayoutType(homeLayoutType);
		}
	}
	
	public static int getHomeLayoutTypeSecondLayer(Context context){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.getHomeLayoutTypeSecondLayer(context);
		}
		
		return 0;
	}

	public static void setHomeLayoutTypeSecondLayer(int homeLayoutType) {
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			homePreferences.setHomeLayoutTypeSecondLayer(homeLayoutType);
		}
	}
	
	public static boolean isFirstLoad(){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.isFirstLoad();
		}
		
		return false;
	}
	
	public static synchronized boolean isLoopHomeScreen() {
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.isLoopHomeScreen();
		}
		
		return DefaultPreferences.getLoopHomeScreenDefaultValue();
	}
	
	public static synchronized void setIsLoopHomeScreen(boolean loopHomeScreen){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			homePreferences.setIsLoopHomeScreen(loopHomeScreen);
		}
	}
	
	public static synchronized GestureSettings getWorkspaceGestureUpAction(Context context) {
		HomeCommonPreferences commonPreferences = getCommonPreferences();
		if(commonPreferences != null){
			return commonPreferences.getWorkspaceGestureUpAction(context);
		}
		return null;
	}

	public static synchronized GestureSettings getWorkspaceGestureDownAction(Context context) {
		HomeCommonPreferences commonPreferences = getCommonPreferences();
		if(commonPreferences != null){
			return commonPreferences.getWorkspaceGestureDownAction(context);
		}
		
		return null;
	}
	
	public static synchronized int[] getHomeLayout(Context context) {
		
		if(sLayouts == null){
			sLayouts = context.getResources().getStringArray(R.array.home_layout_values);
		}
		
        int cellNumVertical = context.getResources().getInteger(R.integer.workspace_default_cell_num_vertical);
        int cellNumHorizontal = context.getResources().getInteger(R.integer.workspace_default_cell_num_horizontal);
        int[] homeLayout = new int[]{cellNumVertical, cellNumHorizontal};
/*      int homeLayoutType = getHomeLayoutType(context);
        homeLayout = Utils.getLayout(sLayouts[homeLayoutType]);*/
		
		return homeLayout;
    }
	
	 public static synchronized int getHomeScreenTransformationType() {
	        if (sHomeScreenTransformationType == Integer.MIN_VALUE) {
	            SharedPreferences sharedPreferences = getSharedPreferences(SUFFIX_COMMON);

	            sHomeScreenTransformationType = sharedPreferences.getInt(
	                    SettingsConstants.KEY_HOME_SCREEN_TRANSFORMATION_TYPE, DefaultPreferences.getHomeScreenTransformationDefaultType());
	        }
	        return sHomeScreenTransformationType;
	    }

	  public static synchronized void setHomeScreenTransformationType(int screenTransformationType) {
	        SharedPreferences sharedPreferences = getSharedPreferences(SUFFIX_COMMON);
	        sharedPreferences.edit().putInt(SettingsConstants.KEY_HOME_SCREEN_TRANSFORMATION_TYPE, screenTransformationType).commit();

	        sHomeScreenTransformationType = screenTransformationType;
	  }
	  
	  
	  public static synchronized int getWallpaperStyle(){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  return homePreferences.getWallpaperStyle();
		  }
		  
		  return -1;
	  }
	  
	  public static synchronized void setWallpaperStyle(int style){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  homePreferences.setWallpaperStyle(style);
		  }
	  }
	  
	  public static synchronized String getPathWallpaper(){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  return homePreferences.getPathWallpaper();
		  }
		  
		  return null;
	  }
	  
	  public static synchronized void setPathWallpaper(String name){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  homePreferences.setPathWallpaper(name);
		  }
	  }
	  public static synchronized String getDefaultWallpaperId(){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  return homePreferences.getDefaultWallpaperId();
		  }
		  
		  return null;
	  }
	  
	  public static synchronized void setDefaultWallpaperId(String id){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  homePreferences.setDefaultWallpaperId(id);
		  }
	  }
	  
	  public static synchronized void setLiveWallpaper(ComponentName cn){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  homePreferences.setLiveWallpaper(cn);
		  }
	  }
	  
	  public static synchronized ComponentName getLiveWallpaper(){
		  HomePreferences homePreferences = getHomePreferences();
		  if(homePreferences != null){
			  return homePreferences.getLiveWallpaper();
		  }
		  
		  return null;
	  }
	  
	  
	 public static SharedPreferences getPreferences(){
		 return getSharedPreferences(SUFFIX_COMMON);
	 }
	
	public static synchronized void setWallpaperDimensions(int width, int height){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			homePreferences.setWallpaperDimensions(width, height);
		}
	}
	
	public static synchronized int[] getWallpaperDimensions(){
		HomePreferences homePreferences = getHomePreferences();
		if(homePreferences != null){
			return homePreferences.getWallpaperDimensions();
		}
		
		return null;
	}
}
