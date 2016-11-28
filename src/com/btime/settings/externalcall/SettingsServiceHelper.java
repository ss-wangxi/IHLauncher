package com.btime.settings.externalcall;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.btime.launcher.app.AppController;
import com.btime.launcher.app.AppType;
import com.btime.launcher.app.SettingsPage;
import com.btime.launcher.util.XLog;
import com.btime.settings.externalcall.IExternalCallService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

public class SettingsServiceHelper {
    
    private static final int SERVICE_STATE_CONNECTING = 0;
    private static final int SERVICE_STATE_CONNECTED = 1;
    private static final int SERVICE_STATE_DISCONNECTED = 2;
    
    private SettingsServiceHelper() {
    }
    
    private static class SingletonHolder {
        public static SettingsServiceHelper sInstance = new SettingsServiceHelper();
    }
    
    public static SettingsServiceHelper getIntance() {
        return SingletonHolder.sInstance;
    }
    
    private IExternalCallService mService;
    private int mServiceState = SERVICE_STATE_DISCONNECTED;
    
    private Handler mHandler = new Handler();
    private ArrayList<Runnable> mPendingTasks = new ArrayList<Runnable>();
    
    private WeakReference<Context> mAppContext;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            XLog.i("Snser", "SettingsServiceHelper onServiceConnected");
            mService = IExternalCallService.Stub.asInterface(service);
            mServiceState = SERVICE_STATE_CONNECTED;
            runPendingTasks();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            XLog.i("Snser", "SettingsServiceHelper onServiceDisconnected");
            mService = null;
            mServiceState = SERVICE_STATE_DISCONNECTED;
        }
    };
    
    public void init(Context context) {
        XLog.i("Snser", "SettingsServiceHelper init");
        mAppContext = new WeakReference<Context>(context.getApplicationContext());
        bindService(mAppContext.get());
    }
    
    private void bindService(Context context) {
        Intent intent = new Intent("com.caros.settings.externalcall.ExternalCallServiceFilter");   
        intent.setPackage("com.caros.settings");
        boolean ret = context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mServiceState = ret ? SERVICE_STATE_CONNECTING : SERVICE_STATE_DISCONNECTED;
    }
    
    private IExternalCallService getServiceAutoBind() {
        if (mService != null) {
            return mService;
        } else {
            if (mAppContext != null) {
                Context appContext = mAppContext.get();
                if (appContext != null) {
                    bindService(appContext);
                }
            }
            return null;
        }
    }
    
    private void runPendingTasks() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mPendingTasks) {
                    for (Runnable task : mPendingTasks) {
                        task.run();
                    }
                    mPendingTasks.clear();
                }
            }
        });
    }
    
    public boolean callVoiceUpDownFunction(final boolean bShow, final boolean bUp, final float percent, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callVoiceUpDownFunction(bShow, bUp, percent);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callVoiceUpDownFunction(bShow, bUp, percent, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callVoiceMinFunction(final boolean bShow, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callVoiceMinFunction(bShow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callVoiceMinFunction(bShow, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callVoiceSilentFunction(final boolean bShow, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callVoiceSilentFunction(bShow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callVoiceSilentFunction(bShow, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callVoiceMaxFunction(final boolean bShow, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callVoiceMaxFunction(bShow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callVoiceMaxFunction(bShow, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callVoiceReset(final boolean bShow, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callVoiceReset(bShow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callVoiceReset(bShow, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callBringhtnessUpDownFunction(final boolean bShow, final boolean bUp, final float percent, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callBringhtnessUpDownFunction(bShow, bUp, percent);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callBringhtnessUpDownFunction(bShow, bUp, percent, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callBringhtnessMinFunction(final boolean bShow, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callBringhtnessMinFunction(bShow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callBringhtnessMinFunction(bShow, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callBringhtnessMaxFunction(final boolean bShow, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callBringhtnessMaxFunction(bShow);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callBringhtnessMaxFunction(bShow, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callHotspotOpen(final boolean bShow, final boolean bOpen, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callHotspotOpen(bShow, bOpen);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callHotspotOpen(bShow, bOpen, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callGPRSOpen(final boolean bShow, final boolean bOpen, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callGPRSOpen(bShow, bOpen);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callGPRSOpen(bShow, bOpen, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callFMOpen(final boolean bShow, final boolean bOpen, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callFMOpen(bShow, bOpen);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callFMOpen(bShow, bOpen, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callBluetoothOpen(final boolean bShow, final boolean bOpen, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callBluetoothOpen(bShow, bOpen);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callBluetoothOpen(bShow, bOpen, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean callWLANOpen(final boolean bShow, final boolean bOpen, final boolean bWait) {
        final IExternalCallService service = getServiceAutoBind();
        if (service != null) {
            try {
                service.callWLANOpen(bShow, bOpen);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (bWait && mServiceState == SERVICE_STATE_CONNECTING) {
            synchronized (mPendingTasks) {
                mPendingTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        callWLANOpen(bShow, bOpen, false);
                    }
                });
            }
            return true;
        }
        return false;
    }
    
    public boolean filterStartIntent(AppType type) {
        final int value = type.value();
        if (value == SettingsPage.PAGE_SETTINGS_CAROS_WLAN.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_WLAN_ON.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_WLAN_OFF.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH_ON.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH_OFF.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC_ON.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC_OFF.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_FM.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_FM_ON.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_FM_OFF.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_AP.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_AP_ON.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_AP_OFF.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_UP.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_DOWN.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_ON.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_OFF.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_MAX.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_MIN.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_UP.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_DOWN.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_MAX.value()
            || value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_MIN.value()) {
            return true;
        }
        return false;
    }
    
    public boolean startApp(AppType type) {
        final int value = type.value();
        boolean ret = false;
        boolean needCtrlLauncher = false;
        if (value == SettingsPage.PAGE_SETTINGS_CAROS_WLAN.value()) {
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_WLAN_ON.value()) {
            ret = callWLANOpen(true, true, true);
            needCtrlLauncher = true;
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_WLAN_OFF.value()) {
            ret = callWLANOpen(true, false, true);
            needCtrlLauncher = true;
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH.value()) {
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH_ON.value()) {
            ret = callBluetoothOpen(true, true, true);
            needCtrlLauncher = true;
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BLUETOOTH_OFF.value()) {
            ret = callBluetoothOpen(true, false, true);
            needCtrlLauncher = true;
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC.value()) {
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC_ON.value()) {
            ret = callGPRSOpen(true, true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_TRAFFIC_OFF.value()) {
            ret = callGPRSOpen(true, false, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_FM.value()) {
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_FM_ON.value()) {
            //ret = callFMOpen(true, true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_FM_OFF.value()) {
            //ret = callFMOpen(true, false, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_AP.value()) {
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_AP_ON.value()) {
            ret = callHotspotOpen(true, true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_AP_OFF.value()) {
            ret = callHotspotOpen(true, false, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_UP.value()) {
            ret = callVoiceUpDownFunction(true, true, 0.2f, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_DOWN.value()) {
            ret = callVoiceUpDownFunction(true, false, 0.2f, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_ON.value()) {
            ret = callVoiceReset(true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_OFF.value()) {
            ret = callVoiceSilentFunction(true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_MAX.value()) {
            ret = callVoiceMaxFunction(true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_VOLUME_MIN.value()) {
            ret = callVoiceMinFunction(true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_UP.value()) {
            ret = callBringhtnessUpDownFunction(true, true, 0.2f, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_DOWN.value()) {
            ret = callBringhtnessUpDownFunction(true, false, 0.2f, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_MAX.value()) {
            ret = callBringhtnessMaxFunction(true, true);
        } else if (value == SettingsPage.PAGE_SETTINGS_CAROS_BRIGHTNESS_MIN.value()) {
            ret = callBringhtnessMinFunction(true, true);
        }
        if (needCtrlLauncher && ret) {
            AppController.getInstance().handleTrafficCtrl(AppController.PKGNAME_SETTINGS_CAROS);
            AppController.getInstance().handleCtrlLauncher(AppController.PKGNAME_SETTINGS_CAROS);
        }
        return ret;
    }
    
}
