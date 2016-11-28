/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.btime.launcher.util.XLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class HomeWatcher {  
  
    static final String TAG = "HomeWatcher";  
    private Context mContext;  
    private IntentFilter mFilter;  
    private OnHomePressedListener mListener;  
    private InnerRecevier mRecevier;  
  

    public interface OnHomePressedListener {  
        public void onHomePressed();  
  
        public void onHomeLongPressed();  
    }  
  
    public HomeWatcher(Context context) {  
        mContext = context;  
        mFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);  
        mFilter.addAction(Intent.ACTION_USER_PRESENT);
    }  
  
    public void setOnHomePressedListener(OnHomePressedListener listener) {  
        mListener = listener;  
        mRecevier = new InnerRecevier();  
    }  

    public void startWatch() {  
        if (mRecevier != null) {  
            mContext.registerReceiver(mRecevier, mFilter);  
        }  
    }  
  
    public void stopWatch() {  
        if (mRecevier != null) {  
            mContext.unregisterReceiver(mRecevier);
            mRecevier = null;
        }  
    }  
   
    class InnerRecevier extends BroadcastReceiver {  
        final String SYSTEM_DIALOG_REASON_KEY = "reason";  
        final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";  
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";  
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";  
  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            String action = intent.getAction();  
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {  
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);  
                if (reason != null) {  
                    XLog.e(TAG, "action:" + action + ",reason:" + reason);
                    if (mListener != null) {  
                        if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {  
                            mListener.onHomePressed();  
                        } else if (reason  
                                .equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {  
                            mListener.onHomeLongPressed();  
                        }  
                    }  
                }  
            }  
        }  
    }  
}  