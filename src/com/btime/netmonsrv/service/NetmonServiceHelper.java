package com.btime.netmonsrv.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import cc.snser.launcher.App;

import com.btime.launcher.util.XLog;
import com.btime.netmonsrv.service.INetmonService;
import com.btime.netmonsrv.service.INetmonServiceCallback;

public class NetmonServiceHelper {
	private final static String TAG = "NetmonServiceHelper";
    private INetmonService mService;
    private ArrayList<OnTrafficUpdateListenser> mListensers = new ArrayList<OnTrafficUpdateListenser>();
    private Context mcontext;
    private List<String> mDispAppList;
    private HashMap<String, Boolean> mAppNetworkCtrlState = new HashMap<String, Boolean>();
    
    private NetmonServiceHelper() {
    }
    
    private static class SingletonHolder {
        public static NetmonServiceHelper sInstance = new NetmonServiceHelper();
    }
    
    public static NetmonServiceHelper getInstance() {
        return SingletonHolder.sInstance;
    }
    
    public static class Size {
        private static final long KB = 1024;
        private static final long MB = KB * 1024;
        private static final long GB = MB * 1024;
        
        public long cntByte = 0;
        public String cntDisp = "0";
        public String cntDispUnit = "MB";
        
        public Size(long sizeByte) {
            if (sizeByte >= 0) {
                this.cntByte = sizeByte;
                parseDispSize();
            }
        }
        
        private void parseDispSize() {
            if (this.cntByte >= GB) {
                this.cntDisp = String.format("%.1f", this.cntByte * 1.0f / GB);
                this.cntDispUnit = "GB";
            } else {
                this.cntDisp = String.format("%.1f", this.cntByte * 1.0f / MB);
                this.cntDispUnit = "MB";
            }
            if (this.cntDisp.endsWith(".0")) {
                this.cntDisp = this.cntDisp.substring(0, this.cntDisp.length() - 2);
            }
        }
    }
    
    private INetmonServiceCallback mCallback = new INetmonServiceCallback.Stub() {
        @Override
        public void onUpdate(final Flux flux) throws RemoteException {
        	XLog.d(TAG,"total flow is " + flux._totalFlux);
			if( null != flux._flux){
				for( AppFlux appFlow : flux._flux){
				    if (mDispAppList != null && mDispAppList.contains(appFlow._pkgname)) {
				        mAppNetworkCtrlState.put(appFlow._pkgname, (appFlow._netState == 1));
				    }
				}
			}
            if (flux._comboPercent >= 0 && flux._comboPercent <= 100) {
                App.getApp().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (OnTrafficUpdateListenser listenser : mListensers) {
                        	SharedPreferences sharedPreferences = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE);				      
        					boolean isFirstShoemessage =  sharedPreferences.getBoolean("FirstShowFlowMessage", true);
        					boolean isFirstEightyMessage = sharedPreferences.getBoolean("EightytFlowMessage", false);
        					boolean isFirstNinetytyMessag = sharedPreferences.getBoolean("NinetyFlowMessage", false);
        					boolean isFiratHundredMessage = sharedPreferences.getBoolean("HundredFlowMessage", false);
        					//boolean isAccOnandHunderdFlow = sharedPreferences.getBoolean("ACCONHundredFlowMessage", false);
        					int usetotalFlow = flux._comboPercent;					
        					long historyTime = sharedPreferences.getLong("TheHistoryTime", 0);
        					long currentTime = System.currentTimeMillis();
        					long daysLeft = flux._daysLeft*24*60*60*1000;
        					if(usetotalFlow == 100){
        						SharedPreferences.Editor editor = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
        						editor.putBoolean("ACCONHundredFlowMessage", true);
        						editor.commit();
        					}else{
        						SharedPreferences.Editor editor = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
        						editor.putBoolean("ACCONHundredFlowMessage", false);
        						editor.commit();
        					}
        					if(usetotalFlow>= 80){
        						if(isFirstShoemessage){
        							listenser.showWarnDialog(usetotalFlow);
        							SharedPreferences.Editor editor = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
        							editor.putLong("TheHistoryTime", currentTime);
        							editor.commit();
        						}else{
        							if(historyTime+daysLeft<currentTime){
        								//还在同一计费周期内
        							}else{//不在同一计费周期内显示弹框提示
        								if(usetotalFlow <90 && !isFirstEightyMessage){
        									SharedPreferences.Editor editor = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
        									editor.putBoolean("EightytFlowMessage", true);
        									editor.putLong("TheHistoryTime", currentTime);
        									editor.commit();
        									listenser.showWarnDialog(usetotalFlow);
        								}
        								if(usetotalFlow>=90 && usetotalFlow<100 && !isFirstNinetytyMessag){
        									SharedPreferences.Editor editor = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
        									editor.putBoolean("NinetyFlowMessage", true);
        									editor.putLong("TheHistoryTime", currentTime);
        									editor.commit();
        									listenser.showWarnDialog(usetotalFlow);
        								}
        								if(usetotalFlow == 100 && !isFiratHundredMessage){
        									SharedPreferences.Editor editor = mcontext.getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
        									editor.putBoolean("HundredFlowMessage", true);
        									editor.putLong("TheHistoryTime", currentTime);
        									editor.commit();
        									listenser.showWarnDialog(usetotalFlow);							
        								}
        																					
        							}							
        						}						
        					}
                         	
                            listenser.onUpdate(new Size(flux._comboUnused_n), new Size(flux._combo_n), 100 - flux._comboPercent);
                        }
                    }
                });
            }
        }
    };
    
    private ServiceConnection mConnection = new ServiceConnection() { 
        public void onServiceConnected(ComponentName className, IBinder service) { 
            mService = INetmonService.Stub.asInterface(service);
            try { 
                mService.registerCallback(mCallback);
                mDispAppList = mService.getDispAppList();
                mService.getFlux();
            } catch (RemoteException e) { 
                e.printStackTrace();
            } 
        }
        
        public void onServiceDisconnected(ComponentName className) { 
            mService = null; 
        } 
    };
    
    private void bindService(Context context) {
    	mcontext = context;
        Intent intent = new Intent("com.caros.netmonsrv.service.AIDL");
        intent.setPackage("com.caros.netmonsrv");
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    
    public static interface OnTrafficUpdateListenser {
        public void onUpdate(Size freeSize, Size totalSize, int freePersent);
        public void showWarnDialog(int usetotalFlow);
    }
    
    public void addOnTrafficUpdateListenser(OnTrafficUpdateListenser listenser) {
        if (listenser != null) {
            mListensers.add(listenser);
        }
    }
    
    public void removeOnTrafficUpdateListenser(OnTrafficUpdateListenser listenser) {
        if (listenser != null) {
            mListensers.remove(listenser);
        }
    }
    
    public void refreshTraffic(Context context) {
        try {
            if (mService != null && mService.isVaild()) {  
                mService.getFlux();
            } else {
                mService = null;
                bindService(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void unBindService(Context context) {
        try {
            if (mService != null) {
            	 mService.unregisterCallback(mCallback);
            	 context.unbindService(mConnection); 
            } else {           	
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mService = null;
        }
    }
    
    public boolean isAppNetworkDisabled(String pkgname) {
        if (mAppNetworkCtrlState.containsKey(pkgname) && mAppNetworkCtrlState.get(pkgname)) {
            return true;
        }
        return false;
    }
    
}
