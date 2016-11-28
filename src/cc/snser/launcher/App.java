/*

 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.snser.launcher;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cc.snser.launcher.iphone.model.LauncherModelIphone;
import cc.snser.launcher.model.LauncherModel;
import cc.snser.launcher.model.LauncherModel.Callbacks;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.btime.launcher.CarOSLauncherBase;
import com.btime.launcher.util.XLog;


public class App extends Application {

    public static final String EXTRA_PACKAGE_NAME = "package_name";

    private LauncherModel mModel;
    private Launcher mLauncher;

    private DeferredHandler mHandler;

    private static App sLauncherApplication;

    private String mCurrentTheme;
    private Executor mExecutor = new ThreadPoolExecutor(10, 200, 6,  
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    public String getCurrentTheme(){
    	return mCurrentTheme;
    }
    
    public void setCurrentTheme(String theme){
    	mCurrentTheme = theme;
    }
    
    public static long TIME_APP_ONCREATE = 0;
    public static long TIME_LAUNCHER_ONCREATE = 0;
    public static long TIME_LAUNCHER_ONSTART = 0;
    public static long TIME_LAUNCHER_ONRESUME = 0;
    public static long TIME_LAUNCHER_FINISHBIND = 0;

    @Override
    public void onCreate() {
        
        if (App.TIME_APP_ONCREATE == 0) {
            App.TIME_APP_ONCREATE = System.currentTimeMillis();
        }
        //VMRuntime.getRuntime().setMinimumHeapSize(4 * 1024 * 1024);

        super.onCreate();
        
        sLauncherApplication = this;	

        mHandler = new DeferredHandler();
        
        XLog.init(this);
        XLog.e("LSTAT", "Launcher.App.onCreate nanoTime=" + String.format(Locale.getDefault(), "%.3fs", System.nanoTime() / 1000000000.0f) 
                        + " pid=" + android.os.Process.myPid());
        
        CarOSLauncherBase.getInstance().init(this);
	}
    
    public static App getApp() {
        return sLauncherApplication;
    }
    
    public static Context getAppContext() {
        return sLauncherApplication.getApplicationContext();
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        CarOSLauncherBase.getInstance().unInit(this);
        
        if (mModel != null) {
            mModel.onTerminate(this);
        }
    }

    LauncherModel setLauncher(Launcher launcher) {
    	mLauncher = launcher;
        LauncherModel model = getModel();
        model.initialize(launcher);
        return model;
    }
    
    
    
    public Launcher getLauncher(){
    	return mLauncher;
    }

    private IconCache getIconCache() {
        return IconCache.getInstance(this);
    }

    public WeakReference<Callbacks> getModelCallbacks() {
        return mModel == null ? null : mModel.mCallbacks;
    }

    public DeferredHandler getDeferredHandler() {
        return mHandler;
    }

    public LauncherModel getModel() {
        if (mModel == null) {
            mModel = new LauncherModelIphone(this, getIconCache(), mHandler);
        }
        return mModel;
    }
    
    

    /***********************以下与外置语言包有关******************************/
    public Resources getSystemResources() {
        return super.getResources();
    }


    public Typeface getFont(Context context) {
        return WorkspaceIconUtils.getWorkspaceIconTypeface(context);
    }
    /***********************以上与外置语言包有关******************************/
    
    public void sendLocalBroadcast(Intent intent){
    	LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    public void registerLocalReceiver(BroadcastReceiver receiver ,IntentFilter intentFilter){
    	LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);	
    }
    
    public void unRegisterLocalReceiver(BroadcastReceiver receiver){
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
    
    public Executor getExecutor(){
    	return mExecutor;
    }
	
	public static boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	
    private static Handler sUIHandler = new Handler();
    public void runOnUiThread(Runnable action) {
        runOnUiThread(action, 0);
    }
    public void runOnUiThread(Runnable action, long delayMillis) {
        if (action != null) {
            sUIHandler.postDelayed(action, delayMillis);
        }
    }
    public void cancelRunOnUiThread(Runnable action) {
        if (action != null) {
            sUIHandler.removeCallbacks(action);
        }
    }
    
    /**
     * 当前是否处于横屏状态
     * @return
     */
    public boolean isScreenLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
	
}
