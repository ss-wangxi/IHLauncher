package cc.snser.launcher.widget.traffic2x3;

import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import cc.snser.launcher.widget.ScreenCtrlWidget;

import com.btime.launcher.app.AppController;
import com.btime.launcher.app.AppType;
import com.btime.launcher.util.XLog;
import com.btime.netmonsrv.service.NetmonServiceHelper;
import com.btime.netmonsrv.service.NetmonServiceHelper.OnTrafficUpdateListenser;
import com.btime.netmonsrv.service.NetmonServiceHelper.Size;
import com.btime.launcher.R;

public class Traffic2x3Widget extends ScreenCtrlWidget implements View.OnClickListener, OnTrafficUpdateListenser {
    
    public static final int SPANX = 2;
    public static final int SPANY = 3;
    
    private Traffic2x3View mWidgetView;
/*    private TextView mTxtFree;
    private TextView mTxtFreeUnit;
    private TextView mTxtTotal;
    private TextView mTxtTotalUnit;
    private SuperProgressWheel mProgress;*/
    private TextView mTxtStatus;
    private SIMStateBroadcastReceiver mReceiver;
	private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    public Traffic2x3Widget(Activity context) {   	
        super(context);   
        setGravity(Gravity.CENTER);
        mWidgetView = (Traffic2x3View)inflate(context, R.layout.widget_traffic_2x3_view, null);
        mWidgetView.setHost(this);
        mWidgetView.findViewById(R.id.widget_traffic2x3_btn).setOnClickListener(this);
/*        mTxtFree = (TextView)mWidgetView.findViewById(R.id.widget_traffic2x3_free);
        mTxtFreeUnit = (TextView)mWidgetView.findViewById(R.id.widget_traffic2x3_free_unit);
        mTxtTotal = (TextView)mWidgetView.findViewById(R.id.widget_traffic2x3_total);
        mTxtTotalUnit = (TextView)mWidgetView.findViewById(R.id.widget_traffic2x3_total_unit);
        mProgress = (SuperProgressWheel)mWidgetView.findViewById(R.id.widget_traffic2x3_progress);*/
        mTxtStatus = (TextView)mWidgetView.findViewById(R.id.widget_traffic2x3_status);
        mWidgetView.findViewById(R.id.widget_traffic2x3_base).setOnClickListener(this);
        addView(mWidgetView); 
        simStateChanged();
        registerBroadcastRecerver(context);
        onUpdate(new Size(0), new Size(0), 0);
        NetmonServiceHelper.getInstance().addOnTrafficUpdateListenser(this);
        NetmonServiceHelper.getInstance().refreshTraffic(getContext().getApplicationContext());
    }
    private void registerBroadcastRecerver(Activity context) {
        if (mReceiver == null) {
            mReceiver = new SIMStateBroadcastReceiver();
        }
        if (mReceiver != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_SIM_STATE_CHANGED);
            intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            context.registerReceiver(mReceiver, intentFilter);
        }
    }
    private void unregisterBroadcastReceiver() {
		if (mReceiver != null) {
			getContext().unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}
    private class SIMStateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(intent.getAction().equals(ACTION_SIM_STATE_CHANGED)){
        		  simStateChanged();
        }else if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)){
        	SharedPreferences sharedPreferences = getContext().getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE);
        	boolean isAccOnandHunderdFlow = sharedPreferences.getBoolean("ACCONHundredFlowMessage", false);
        	if(isAccOnandHunderdFlow){
        		showWarnDialog(100);	
        	}
        }
    }		
 }
    @Override
    public int getSpanX() {
        return SPANX;
    }
    
    @Override
    public int getSpanY() {
        return SPANY;
    }
    
    @Override
    public void onClick(View v) {
        AppController.getInstance().startApp(AppType.TYPE_TRAFFIC);
        simStateChanged();
    }
    
    @Override
    public void onScreenOn() {
    	 simStateChanged();
    }
    
    @Override
    protected void onScreenIn() {
    	 simStateChanged();
    }
    
    @Override
    public void onLauncherResume() {
        super.onLauncherResume(); 
    	simStateChanged();
    } 
    
    
    public final static String getImei(TelephonyManager tm) {
        try {
            if (tm != null && !TextUtils.isEmpty(tm.getDeviceId())) {
                return tm.getDeviceId();
            }
        } catch (Exception e) {
        }
        return "null";
    }
    
    private void simStateChanged() {
   	 TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		int simState = tm.getSimState();
		XLog.e("LSTAT", "simStateChanged state=" + simState + " imei=" + getImei(tm) + " nanoTime=" + String.format(Locale.getDefault(), "%.3fs", System.nanoTime() / 1000000000.0f));
		 switch (simState) {
      case TelephonyManager.SIM_STATE_UNKNOWN:
    	  NetmonServiceHelper.getInstance().unBindService(getContext());
    	  onUpdate(new Size(0), new Size(0),0);
    	  break;
      case TelephonyManager.SIM_STATE_ABSENT:
    	  NetmonServiceHelper.getInstance().unBindService(getContext());
    	  onUpdate(new Size(0), new Size(0),0);
    	  break;
      case TelephonyManager.SIM_STATE_READY:
    	  NetmonServiceHelper.getInstance().refreshTraffic(getContext().getApplicationContext()); 
    	  break;  
      default:
          break;
	   }
		
	}
	@Override
    protected boolean handleOnScreenInEvent() {
        return true;
    }
    @Override
    public void onRemoved(boolean permanent) {
    	super.onRemoved(permanent);
    	if (permanent) {
    		NetmonServiceHelper.getInstance().removeOnTrafficUpdateListenser(this);
    		unregisterBroadcastReceiver();
    	}
    }

    @Override
    public void onUpdate(Size freeSize, Size totalSize, int freePersent) {
        Log.d("Snser", "Traffic2x3Widget onUpdate this=" + this.hashCode() + " free=" + freeSize.cntDisp + freeSize.cntDispUnit + " total=" + totalSize.cntDisp + totalSize.cntDispUnit + " freePersent=" + freePersent);
        if (/*mTxtFree != null && mTxtTotal != null && */mTxtStatus != null/* && mProgress != null*/) {
/*            mTxtFree.setText(freeSize.cntDisp);
            mTxtFreeUnit.setText(freeSize.cntDispUnit);
            mTxtTotal.setText(totalSize.cntDisp);
            mTxtTotalUnit.setText(totalSize.cntDispUnit);
            mProgress.setProgress(freePersent);*/
            mTxtStatus.setText(String.format(Locale.getDefault(), "剩余 %s%s", freeSize.cntDisp, freeSize.cntDispUnit));
        } else {
        }
    }
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	onRemoved(true);
    }
    
	@Override
	public void showWarnDialog(int usetotalFlow) {
		Intent intent = new Intent();//
		intent.setAction("com.caros.openflowdialog");
		final String flowMsg;
    	if(usetotalFlow<90){
			flowMsg = "流量已使用超过套餐的80%，请注意流量使用情况，并考虑尽快进行充值!";
		}else if(usetotalFlow == 100){
			flowMsg = "流量已用光，请立即进行流量充值，以保证功能正常使用!";
		}else{
			flowMsg = "您的流量即将用完，请注意流量使用情况，并考虑尽快进行充值!";
		}
		intent.putExtra("FLOWMESSAGEREMINDER", flowMsg);
		getContext().startService(intent);
		getContext().stopService(intent);
		SharedPreferences.Editor editor = getContext().getSharedPreferences("FLOW_MESSAGE_SHOW", Context.MODE_PRIVATE).edit();
		editor.putBoolean("FirstShowFlowMessage", false);
		editor.commit();	
		
	}
}
