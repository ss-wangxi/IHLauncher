package com.btime.launcher.app;

import com.btime.launcher.util.SignatureUtils;
import com.btime.launcher.app.IAppService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.text.TextUtils;

public class AppService extends Service {
    private IAppService.Stub mBinder = new IAppService.Stub() {
        @Override
        public AppType getAppPage(AppType type) throws RemoteException {
            return AppController.getInstance().getAppPage(type);
        }
        
        @Override
        public boolean startApp(AppType type) throws RemoteException {
            return AppController.getInstance().startApp(type);
        }
        
        @Override
        public boolean stopApp(AppType type) throws RemoteException {
            return AppController.getInstance().stopApp(type);
        }
        
        @Override
        public boolean notifyStartApp(AppType type) throws RemoteException {
            return AppController.getInstance().notifyStartApp(type, null);
        }
        
        @Override
        public boolean notifyStartAppWithExtra(AppType type, String extra) throws RemoteException {
            return AppController.getInstance().notifyStartApp(type, extra);
        }
        
        @Override
        public int checkSupportScrollPage(int direction) throws RemoteException {
            return AppController.getInstance().checkSupportScrollPage(direction);
        }

        @Override
        public int scrollPage(int direction) throws RemoteException {
            return AppController.getInstance().scrollPage(direction);
        }
        
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String packageName = null;
            PackageManager pm = AppService.this.getPackageManager();
            String[] packages = pm.getPackagesForUid(getCallingUid());
            
            if(packages != null && packages.length > 0){
                packageName = packages[0];
            }
            
            String md5Calling = SignatureUtils.getSignatureMD5(AppService.this, packageName);
            String md5Self = SignatureUtils.getSignatureMD5(AppService.this, getPackageName());
            if (TextUtils.isEmpty(md5Calling) || (!md5Calling.equalsIgnoreCase(md5Self))) {
                throw new SecurityException("AppService Check Signature Failed!");
            }
            
            return super.onTransact(code, data, reply, flags);
        }

		@Override
		public boolean isAgreeUserServiceProtocol() throws RemoteException {
			SharedPreferences sharedPreferences = getSharedPreferences("FRIST_SHOW_SERVICE_PROTOCAL", Context.MODE_PRIVATE);
	    	boolean isFirstShowServuceProtocal = sharedPreferences.getBoolean("FirstShowUserServiceProtocal", true);
	    	if(isFirstShowServuceProtocal){
	    		return true;
	    	}else{
	    		return false;
	    	}
			
		}
    };   
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
